package com.smarthome.app.ui.outlet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.data.FirebaseFloorRepository
import com.smarthome.app.data.FirebaseOutletRepository
import com.smarthome.app.domain.model.FloorPlan
import com.smarthome.app.domain.model.LayoutViolation
import com.smarthome.app.domain.model.DeviceProfile
import com.smarthome.app.domain.model.NewDevice
import com.smarthome.app.domain.model.OutletDevice
import com.smarthome.app.domain.model.PowerState
import com.smarthome.app.domain.model.RoomLayout
import com.smarthome.app.domain.model.SmartDevice
import com.smarthome.app.domain.model.HomeAlert
import com.smarthome.app.domain.repository.FloorRepository
import com.smarthome.app.domain.repository.FloorContainsDevicesException
import com.smarthome.app.domain.repository.RoomContainsDevicesException
import com.smarthome.app.domain.repository.OutletRepository
import com.smarthome.app.domain.validation.FloorLayoutValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OutletUiState(
    val email: String = "",
    val password: String = "",
    val isAuthenticated: Boolean = false,
    val isSigningIn: Boolean = false,
    val isLoadingOutlet: Boolean = false,
    val isSendingCommand: Boolean = false,
    val outlet: OutletDevice? = null,
    val devices: List<SmartDevice> = emptyList(),
    val isLoadingDevices: Boolean = false,
    val isCreatingDevice: Boolean = false,
    val alerts: List<HomeAlert> = emptyList(),
    val isLoadingAlerts: Boolean = false,
    val errorMessage: String? = null,
    val floors: List<FloorPlan> = emptyList(),
    val selectedFloorId: String? = null,
    val rooms: List<RoomLayout> = emptyList(),
    val isLoadingFloors: Boolean = false,
    val isSavingLayout: Boolean = false,
    val layoutMessage: String? = null,
) {
    val selectedFloor: FloorPlan?
        get() = floors
            .firstOrNull { floor -> floor.id == selectedFloorId }
            ?.copy(rooms = rooms)
}

class OutletViewModel(
    private val repository: OutletRepository = FirebaseOutletRepository(),
    private val floorRepository: FloorRepository = FirebaseFloorRepository(),
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(
        OutletUiState(
            isAuthenticated = repository.hasAuthenticatedUser,
        ),
    )

    val uiState: StateFlow<OutletUiState> = mutableUiState.asStateFlow()

    private var outletObservation: Job? = null
    private var deviceObservation: Job? = null
    private var alertObservation: Job? = null
    private var floorObservation: Job? = null
    private var roomObservation: Job? = null

    init {
        if (repository.hasAuthenticatedUser) {
            observeOutlet()
            observeDevices()
            observeAlerts()
            observeFloors()
        }
    }

    fun updateEmail(email: String) {
        mutableUiState.update { state ->
            state.copy(
                email = email,
                errorMessage = null,
            )
        }
    }

    fun updatePassword(password: String) {
        mutableUiState.update { state ->
            state.copy(
                password = password,
                errorMessage = null,
            )
        }
    }

    fun signIn() {
        val state = mutableUiState.value
        val email = state.email.trim()
        val password = state.password

        if (email.isBlank() || password.isBlank()) {
            mutableUiState.update {
                it.copy(errorMessage = "Enter both email and password.")
            }
            return
        }

        if (state.isSigningIn) return

        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isSigningIn = true,
                    errorMessage = null,
                )
            }

            runCatching {
                repository.signIn(email, password)
            }.onSuccess {
                mutableUiState.update {
                    it.copy(
                        password = "",
                        isAuthenticated = true,
                        isSigningIn = false,
                    )
                }
                observeOutlet()
                observeDevices()
                observeAlerts()
                observeFloors()
            }.onFailure {
                mutableUiState.update {
                    it.copy(
                        isSigningIn = false,
                        errorMessage = "Sign-in failed. Check your credentials and connection.",
                    )
                }
            }
        }
    }

    fun requestPowerState(powerState: PowerState) {
        val state = mutableUiState.value

        if (state.isSendingCommand ||
            state.outlet?.acceptsPowerCommands != true
        ) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isSendingCommand = true,
                    errorMessage = null,
                )
            }

            runCatching {
                repository.requestPowerState(
                    homeId = HOME_ID,
                    deviceId = OUTLET_ID,
                    powerState = powerState,
                )
            }.onSuccess {
                mutableUiState.update {
                    it.copy(isSendingCommand = false)
                }
            }.onFailure {
                mutableUiState.update {
                    it.copy(
                        isSendingCommand = false,
                        errorMessage = "The outlet command could not be sent.",
                    )
                }
            }
        }
    }

    fun selectFloor(floorId: String) {
        if (mutableUiState.value.floors.none { floor -> floor.id == floorId }) {
            return
        }

        if (mutableUiState.value.selectedFloorId == floorId) {
            return
        }

        mutableUiState.update { state ->
            state.copy(
                selectedFloorId = floorId,
                rooms = emptyList(),
                layoutMessage = null,
            )
        }
        observeRooms(floorId)
    }

    fun createFloor(
        name: String,
        level: Int,
        gridColumns: Int,
        gridRows: Int,
    ) {
        val violations = FloorLayoutValidator.validateFloor(
            name = name,
            level = level,
            gridColumns = gridColumns,
            gridRows = gridRows,
            existingFloors = mutableUiState.value.floors,
        )

        if (violations.isNotEmpty()) {
            mutableUiState.update { state ->
                state.copy(layoutMessage = violations.toMessage())
            }
            return
        }

        if (mutableUiState.value.isSavingLayout) return

        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    isSavingLayout = true,
                    layoutMessage = null,
                )
            }

            runCatching {
                floorRepository.createFloor(
                    homeId = HOME_ID,
                    name = name,
                    level = level,
                    gridColumns = gridColumns,
                    gridRows = gridRows,
                )
            }.onSuccess { floorId ->
                mutableUiState.update { state ->
                    state.copy(
                        selectedFloorId = floorId,
                        rooms = emptyList(),
                        isSavingLayout = false,
                        layoutMessage = "Floor created.",
                    )
                }
                observeRooms(floorId)
            }.onFailure {
                mutableUiState.update { state ->
                    state.copy(
                        isSavingLayout = false,
                        layoutMessage = "The floor could not be created.",
                    )
                }
            }
        }
    }

    fun createRoom(
        name: String,
        column: Int,
        row: Int,
        width: Int,
        height: Int,
    ) {
        val floor = mutableUiState.value.selectedFloor
        if (floor == null) {
            mutableUiState.update { state ->
                state.copy(layoutMessage = "Select a floor before adding a room.")
            }
            return
        }

        val room = RoomLayout(
            id = "",
            name = name,
            column = column,
            row = row,
            width = width,
            height = height,
        )
        val violations = FloorLayoutValidator.validateRoom(floor, room)

        if (violations.isNotEmpty()) {
            mutableUiState.update { state ->
                state.copy(layoutMessage = violations.toMessage())
            }
            return
        }

        if (mutableUiState.value.isSavingLayout) return

        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    isSavingLayout = true,
                    layoutMessage = null,
                )
            }

            runCatching {
                floorRepository.createRoom(
                    homeId = HOME_ID,
                    floorId = floor.id,
                    room = room,
                )
            }.onSuccess {
                mutableUiState.update { state ->
                    state.copy(
                        isSavingLayout = false,
                        layoutMessage = "Room created.",
                    )
                }
            }.onFailure {
                mutableUiState.update { state ->
                    state.copy(
                        isSavingLayout = false,
                        layoutMessage = "The room could not be created.",
                    )
                }
            }
        }
    }

    fun updateSelectedFloor(
        name: String,
        level: Int,
        gridColumns: Int,
        gridRows: Int,
    ) {
        val current = mutableUiState.value.selectedFloor ?: return
        val updated = current.copy(
            name = name,
            level = level,
            gridColumns = gridColumns,
            gridRows = gridRows,
        )
        val violations = FloorLayoutValidator.validateFloor(
            name = name,
            level = level,
            gridColumns = gridColumns,
            gridRows = gridRows,
            existingFloors = mutableUiState.value.floors,
            editingFloorId = current.id,
        ).toMutableSet()

        current.rooms.forEach { room ->
            violations += FloorLayoutValidator.validateRoom(updated, room)
        }
        mutableUiState.value.devices
            .filter { device -> device.floorId == current.id }
            .forEach { device ->
                violations += FloorLayoutValidator.validateDevicePosition(
                    updated,
                    device.column,
                    device.row,
                )
            }

        if (violations.isNotEmpty()) {
            mutableUiState.update { it.copy(layoutMessage = violations.toMessage()) }
            return
        }

        saveLayout(
            action = { floorRepository.updateFloor(HOME_ID, updated) },
            successMessage = "Floor updated.",
            failureMessage = "The floor could not be updated.",
        )
    }

    fun updateRoom(
        roomId: String,
        name: String,
        column: Int,
        row: Int,
        width: Int,
        height: Int,
    ) {
        val floor = mutableUiState.value.selectedFloor ?: return
        val room = RoomLayout(roomId, name, column, row, width, height)
        val violations = FloorLayoutValidator.validateRoom(floor, room)
            .toMutableSet()
        mutableUiState.value.devices
            .filter { device -> device.roomId == roomId }
            .filterNot { device ->
                device.column in room.column until room.right &&
                    device.row in room.row until room.bottom
            }
            .forEach {
                violations += LayoutViolation.DEVICE_POSITION_OUTSIDE_ROOM
            }
        if (violations.isNotEmpty()) {
            mutableUiState.update { it.copy(layoutMessage = violations.toMessage()) }
            return
        }

        saveLayout(
            action = { floorRepository.updateRoom(HOME_ID, floor.id, room) },
            successMessage = "Room updated.",
            failureMessage = "The room could not be updated.",
        )
    }

    fun placeOutlet(
        column: Int,
        row: Int,
    ) {
        val floor = mutableUiState.value.selectedFloor ?: return
        val violations = FloorLayoutValidator.validateDevicePosition(floor, column, row)
        if (violations.isNotEmpty()) {
            mutableUiState.update { it.copy(layoutMessage = violations.toMessage()) }
            return
        }

        val roomId = floor.rooms.firstOrNull { room ->
            column in room.column until room.right &&
                row in room.row until room.bottom
        }?.id

        saveLayout(
            action = {
                repository.placeOutlet(
                    homeId = HOME_ID,
                    deviceId = OUTLET_ID,
                    floorId = floor.id,
                    roomId = roomId,
                    column = column,
                    row = row,
                )
            },
            successMessage = "Outlet placed.",
            failureMessage = "The outlet could not be placed.",
        )
    }

    fun createDevice(
        name: String,
        profile: DeviceProfile,
        column: Int,
        row: Int,
        channelCount: Int,
        maxOnDurationMinutes: Int,
        mediaUri: String,
    ) {
        val floor = mutableUiState.value.selectedFloor ?: return
        val message = when {
            name.isBlank() -> "Enter a device name."
            FloorLayoutValidator.validateDevicePosition(floor, column, row).isNotEmpty() ->
                "The device position is outside the floor grid."
            mutableUiState.value.devices.any { device ->
                device.floorId == floor.id && device.column == column && device.row == row
            } -> "That grid cell already contains a device."
            profile == DeviceProfile.MULTI_SWITCH && channelCount !in setOf(2, 3, 5) ->
                "A multi-switch must have 2, 3, or 5 channels."
            profile == DeviceProfile.SAFETY_OUTLET && maxOnDurationMinutes !in 1..240 ->
                "Safety duration must be between 1 and 240 minutes."
            profile == DeviceProfile.CAMERA && !mediaUri.startsWith("https://") ->
                "Camera media must use an HTTPS URI."
            else -> null
        }
        if (message != null) {
            mutableUiState.update { it.copy(layoutMessage = message) }
            return
        }

        val roomId = floor.rooms.firstOrNull { room ->
            column in room.column until room.right && row in room.row until room.bottom
        }?.id

        if (mutableUiState.value.isCreatingDevice) return
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(isCreatingDevice = true, layoutMessage = null)
            }
            runCatching {
                repository.createDevice(
                    HOME_ID,
                    NewDevice(
                        name = name,
                        profile = profile,
                        floorId = floor.id,
                        roomId = roomId,
                        column = column,
                        row = row,
                        channelCount = channelCount,
                        maxOnDurationSeconds = maxOnDurationMinutes * 60,
                        mediaUri = mediaUri,
                    ),
                )
            }.onSuccess {
                mutableUiState.update {
                    it.copy(isCreatingDevice = false, layoutMessage = "Device created.")
                }
            }.onFailure {
                mutableUiState.update {
                    it.copy(
                        isCreatingDevice = false,
                        layoutMessage = "The device could not be created.",
                    )
                }
            }
        }
    }

    fun moveDevice(deviceId: String, column: Int, row: Int) {
        val floor = mutableUiState.value.selectedFloor ?: return
        val message = when {
            FloorLayoutValidator.validateDevicePosition(floor, column, row).isNotEmpty() ->
                "The device position is outside the floor grid."
            mutableUiState.value.devices.any { device ->
                device.id != deviceId && device.floorId == floor.id &&
                    device.column == column && device.row == row
            } -> "That grid cell already contains a device."
            else -> null
        }
        if (message != null) {
            mutableUiState.update { it.copy(layoutMessage = message) }
            return
        }
        val roomId = floor.rooms.firstOrNull { room ->
            column in room.column until room.right && row in room.row until room.bottom
        }?.id
        saveLayout(
            action = { repository.placeDevice(HOME_ID, deviceId, floor.id, roomId, column, row) },
            successMessage = "Device moved.",
            failureMessage = "The device could not be moved.",
        )
    }

    fun deleteDevice(deviceId: String) {
        if (deviceId == OUTLET_ID) {
            mutableUiState.update {
                it.copy(layoutMessage = "The primary synchronized outlet cannot be deleted yet.")
            }
            return
        }
        saveLayout(
            action = { repository.deleteDevice(HOME_ID, deviceId) },
            successMessage = "Device deleted.",
            failureMessage = "The device could not be deleted.",
        )
    }

    fun deleteSelectedFloor() {
        val floorId = mutableUiState.value.selectedFloorId ?: return
        if (mutableUiState.value.isSavingLayout) return

        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(isSavingLayout = true, layoutMessage = null)
            }

            runCatching {
                floorRepository.deleteFloor(HOME_ID, floorId)
            }.onSuccess {
                mutableUiState.update { state ->
                    state.copy(
                        isSavingLayout = false,
                        layoutMessage = "Floor deleted.",
                    )
                }
            }.onFailure { exception ->
                val message = if (exception is FloorContainsDevicesException) {
                    exception.message
                } else {
                    "The floor could not be deleted."
                }
                mutableUiState.update { state ->
                    state.copy(
                        isSavingLayout = false,
                        layoutMessage = message,
                    )
                }
            }
        }
    }

    fun deleteRoom(roomId: String) {
        val floorId = mutableUiState.value.selectedFloorId ?: return
        if (mutableUiState.value.isSavingLayout) return

        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(isSavingLayout = true, layoutMessage = null)
            }

            runCatching {
                floorRepository.deleteRoom(HOME_ID, floorId, roomId)
            }.onSuccess {
                mutableUiState.update { state ->
                    state.copy(
                        isSavingLayout = false,
                        layoutMessage = "Room deleted.",
                    )
                }
            }.onFailure { exception ->
                val message = if (exception is RoomContainsDevicesException) {
                    exception.message
                } else {
                    "The room could not be deleted."
                }
                mutableUiState.update { state ->
                    state.copy(
                        isSavingLayout = false,
                        layoutMessage = message,
                    )
                }
            }
        }
    }

    fun signOut() {
        outletObservation?.cancel()
        outletObservation = null
        deviceObservation?.cancel()
        deviceObservation = null
        alertObservation?.cancel()
        alertObservation = null
        floorObservation?.cancel()
        floorObservation = null
        roomObservation?.cancel()
        roomObservation = null
        repository.signOut()

        mutableUiState.value = OutletUiState(
            email = mutableUiState.value.email,
        )
    }

    private fun observeOutlet() {
        outletObservation?.cancel()

        mutableUiState.update {
            it.copy(
                isLoadingOutlet = true,
                errorMessage = null,
            )
        }

        outletObservation = viewModelScope.launch {
            repository
                .observeOutlet(
                    homeId = HOME_ID,
                    deviceId = OUTLET_ID,
                )
                .catch {
                    mutableUiState.update { state ->
                        state.copy(
                            isLoadingOutlet = false,
                            errorMessage = "The outlet could not be loaded.",
                        )
                    }
                }
                .collect { outlet ->
                    mutableUiState.update { state ->
                        state.copy(
                            isLoadingOutlet = false,
                            outlet = outlet,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private fun observeDevices() {
        deviceObservation?.cancel()
        mutableUiState.update { it.copy(isLoadingDevices = true) }
        deviceObservation = viewModelScope.launch {
            repository.observeDevices(HOME_ID)
                .catch {
                    mutableUiState.update {
                        it.copy(
                            isLoadingDevices = false,
                            errorMessage = "Devices could not be loaded.",
                        )
                    }
                }
                .collect { devices ->
                    mutableUiState.update {
                        it.copy(
                            devices = devices,
                            isLoadingDevices = false,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private fun observeAlerts() {
        alertObservation?.cancel()
        mutableUiState.update { it.copy(isLoadingAlerts = true) }
        alertObservation = viewModelScope.launch {
            repository.observeAlerts(HOME_ID)
                .catch {
                    mutableUiState.update {
                        it.copy(
                            isLoadingAlerts = false,
                            errorMessage = "Safety alerts could not be loaded.",
                        )
                    }
                }
                .collect { alerts ->
                    mutableUiState.update {
                        it.copy(
                            alerts = alerts,
                            isLoadingAlerts = false,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private fun observeFloors() {
        floorObservation?.cancel()

        mutableUiState.update { state ->
            state.copy(
                isLoadingFloors = true,
                layoutMessage = null,
            )
        }

        floorObservation = viewModelScope.launch {
            floorRepository
                .observeFloors(HOME_ID)
                .catch {
                    mutableUiState.update { state ->
                        state.copy(
                            isLoadingFloors = false,
                            layoutMessage = "Floor plans could not be loaded.",
                        )
                    }
                }
                .collect { floors ->
                    val previousFloorId = mutableUiState.value.selectedFloorId
                    val selectedFloorId = previousFloorId
                        ?.takeIf { floorId ->
                            floors.any { floor -> floor.id == floorId }
                        }
                        ?: floors.firstOrNull()?.id

                    mutableUiState.update { state ->
                        state.copy(
                            floors = floors,
                            selectedFloorId = selectedFloorId,
                            isLoadingFloors = false,
                        )
                    }

                    if (selectedFloorId != previousFloorId) {
                        if (selectedFloorId == null) {
                            roomObservation?.cancel()
                            mutableUiState.update { state ->
                                state.copy(rooms = emptyList())
                            }
                        } else {
                            observeRooms(selectedFloorId)
                        }
                    }
                }
        }
    }

    private fun observeRooms(floorId: String) {
        roomObservation?.cancel()

        roomObservation = viewModelScope.launch {
            floorRepository
                .observeRooms(
                    homeId = HOME_ID,
                    floorId = floorId,
                )
                .catch {
                    mutableUiState.update { state ->
                        state.copy(
                            rooms = emptyList(),
                            layoutMessage = "Rooms could not be loaded.",
                        )
                    }
                }
                .collect { rooms ->
                    if (mutableUiState.value.selectedFloorId == floorId) {
                        mutableUiState.update { state ->
                            state.copy(rooms = rooms)
                        }
                    }
                }
        }
    }

    private fun saveLayout(
        action: suspend () -> Unit,
        successMessage: String,
        failureMessage: String,
    ) {
        if (mutableUiState.value.isSavingLayout) return

        viewModelScope.launch {
            mutableUiState.update { it.copy(isSavingLayout = true, layoutMessage = null) }
            runCatching { action() }
                .onSuccess {
                    mutableUiState.update {
                        it.copy(isSavingLayout = false, layoutMessage = successMessage)
                    }
                }
                .onFailure {
                    mutableUiState.update {
                        it.copy(isSavingLayout = false, layoutMessage = failureMessage)
                    }
                }
        }
    }

    private companion object {
        const val HOME_ID = "demo-home"
        const val OUTLET_ID = "main-outlet"
    }
}

private fun Set<LayoutViolation>.toMessage(): String {
    return when {
        LayoutViolation.FLOOR_NAME_BLANK in this -> "Floor name is required."
        LayoutViolation.FLOOR_LEVEL_DUPLICATE in this -> "Another floor already uses this level."
        LayoutViolation.GRID_COLUMNS_OUT_OF_RANGE in this ||
            LayoutViolation.GRID_ROWS_OUT_OF_RANGE in this -> {
            "Grid dimensions must be between ${FloorLayoutValidator.MIN_GRID_SIZE} and " +
                "${FloorLayoutValidator.MAX_GRID_SIZE}."
        }

        LayoutViolation.ROOM_NAME_BLANK in this -> "Room name is required."
        LayoutViolation.ROOM_ORIGIN_NEGATIVE in this -> "Room coordinates cannot be negative."
        LayoutViolation.ROOM_SIZE_NOT_POSITIVE in this -> "Room width and height must be positive."
        LayoutViolation.ROOM_OUTSIDE_FLOOR in this -> "The room extends beyond the floor grid."
        LayoutViolation.ROOM_OVERLAPS_EXISTING in this -> "The room overlaps an existing room."
        LayoutViolation.DEVICE_POSITION_OUTSIDE_FLOOR in this -> "The device position is outside the floor grid."
        LayoutViolation.DEVICE_POSITION_OUTSIDE_ROOM in this -> "The assigned outlet must remain inside this room."
        else -> "The layout values are invalid."
    }
}
