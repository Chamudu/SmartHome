package com.smarthome.app.ui.outlet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.SmartHomeApplication
import com.smarthome.app.data.FirebaseFloorRepository
import com.smarthome.app.data.FirebaseOutletRepository
import com.smarthome.app.data.connectivity.NetworkMonitor
import com.smarthome.app.data.recovery.FirestoreErrorClassifier
import com.smarthome.app.data.recovery.commandBackoffMillis
import com.smarthome.app.data.recovery.retryWithBackoff
import com.smarthome.app.domain.model.FloorPlan
import com.smarthome.app.domain.model.LayoutViolation
import com.smarthome.app.domain.model.DeviceProfile
import com.smarthome.app.domain.model.DeviceConfiguration
import com.smarthome.app.domain.model.CommandState
import com.smarthome.app.domain.model.NewDevice
import com.smarthome.app.domain.model.OutletDevice
import com.smarthome.app.domain.model.PowerState
import com.smarthome.app.domain.model.RoomLayout
import com.smarthome.app.domain.model.SmartDevice
import com.smarthome.app.domain.model.HomeAlert
import com.smarthome.app.domain.model.DeviceEvent
import com.smarthome.app.domain.model.defaultFloorName
import com.smarthome.app.domain.repository.FloorRepository
import com.smarthome.app.domain.repository.FloorContainsDevicesException
import com.smarthome.app.domain.repository.RoomContainsDevicesException
import com.smarthome.app.domain.repository.OutletRepository
import com.smarthome.app.domain.validation.FloorLayoutValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OutletUiState(
    val email: String = "",
    val password: String = "",
    val isAuthenticated: Boolean = false,
    val accountEmail: String? = null,
    val isSigningIn: Boolean = false,
    val isLoadingOutlet: Boolean = false,
    val isSendingCommand: Boolean = false,
    val outlet: OutletDevice? = null,
    val devices: List<SmartDevice> = emptyList(),
    val isLoadingDevices: Boolean = false,
    val isCreatingDevice: Boolean = false,
    val switchCommandsInFlight: Set<String> = emptySet(),
    val deviceCommandsInFlight: Set<String> = emptySet(),
    val scheduleUpdatesInFlight: Set<String> = emptySet(),
    val alerts: List<HomeAlert> = emptyList(),
    val isLoadingAlerts: Boolean = false,
    val eventsByDevice: Map<String, List<DeviceEvent>> = emptyMap(),
    val isLoadingEvents: Boolean = false,
    val eventsErrorMessage: String? = null,
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
    val isRecovering: Boolean = false,
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
    private val networkMonitor: NetworkMonitor = SmartHomeApplication.networkMonitor,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(
        OutletUiState(
            isAuthenticated = repository.hasAuthenticatedUser,
            accountEmail = repository.authenticatedUserEmail,
        ),
    )

    val uiState: StateFlow<OutletUiState> = mutableUiState.asStateFlow()

    private var outletObservation: Job? = null
    private var deviceObservation: Job? = null
    private var alertObservation: Job? = null
    private var floorObservation: Job? = null
    private var roomObservation: Job? = null
    private val deviceEventJobs: MutableMap<String, Job> = mutableMapOf()
    private var authenticationObservation: Job? = null
    private var networkObservation: Job? = null
    private var userInitiatedSignOut = false
    private val pendingCommands: MutableMap<String, PendingCommand> = mutableMapOf()

    init {
        observeAuthentication()
        observeNetwork()
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
                        accountEmail = repository.authenticatedUserEmail ?: email,
                        isSigningIn = false,
                    )
                }
                observeOutlet()
                observeDevices()
                observeAlerts()
                observeFloors()
            }.onFailure { cause ->
                mutableUiState.update {
                    it.copy(
                        isSigningIn = false,
                        errorMessage = if (cause.isRecoverable()) {
                            "Sign-in could not be completed. Check your connection and try again."
                        } else {
                            "Sign-in failed. Check your credentials and connection."
                        },
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

        mutableUiState.update { it.copy(errorMessage = null) }

        executeCommand(
            key = OUTLET_ID,
            setInFlight = { value ->
                mutableUiState.update { it.copy(isSendingCommand = value) }
            },
            action = {
                repository.requestPowerState(
                    homeId = HOME_ID,
                    deviceId = OUTLET_ID,
                    powerState = powerState,
                )
            },
            failureMessage = "The outlet command could not be sent.",
        )
    }

    fun requestSwitchChannelState(
        deviceId: String,
        channelId: String,
        powerState: PowerState,
    ) {
        val key = "$deviceId:$channelId"
        val device = mutableUiState.value.devices.firstOrNull { it.id == deviceId }
        val configuration = device?.configuration as? DeviceConfiguration.MultiSwitch
        val channel = configuration?.channels?.firstOrNull { it.id == channelId }
        if (device == null || channel == null ||
            !device.reportedStatus.acceptsPowerCommands ||
            key in mutableUiState.value.switchCommandsInFlight
        ) return

        mutableUiState.update { it.copy(errorMessage = null) }

        executeCommand(
            key = key,
            setInFlight = { value ->
                mutableUiState.update {
                    it.copy(
                        switchCommandsInFlight = if (value) {
                            it.switchCommandsInFlight + key
                        } else {
                            it.switchCommandsInFlight - key
                        },
                    )
                }
            },
            action = {
                repository.requestSwitchChannelState(HOME_ID, deviceId, channelId, powerState)
            },
            failureMessage = "The switch channel command could not be sent.",
        )
    }

    fun requestDevicePowerState(
        deviceId: String,
        powerState: PowerState,
    ) {
        val state = mutableUiState.value
        val device = state.devices.firstOrNull { it.id == deviceId } ?: return
        val supportsPower = device.profile in setOf(
            DeviceProfile.OUTLET,
            DeviceProfile.SAFETY_OUTLET,
            DeviceProfile.LIGHT,
        )
        if (!supportsPower || !device.reportedStatus.acceptsPowerCommands ||
            device.commandState == CommandState.PENDING ||
            deviceId in state.deviceCommandsInFlight
        ) return

        mutableUiState.update { it.copy(errorMessage = null) }

        executeCommand(
            key = deviceId,
            setInFlight = { value ->
                mutableUiState.update {
                    it.copy(
                        deviceCommandsInFlight = if (value) {
                            it.deviceCommandsInFlight + deviceId
                        } else {
                            it.deviceCommandsInFlight - deviceId
                        },
                    )
                }
            },
            action = {
                repository.requestPowerState(HOME_ID, deviceId, powerState)
            },
            failureMessage = "The device command could not be sent.",
        )
    }

    fun updateLightSchedule(
        deviceId: String,
        enabled: Boolean,
        startLocalTime: String,
        endLocalTime: String,
        timezone: String,
    ) {
        val device = mutableUiState.value.devices.firstOrNull { it.id == deviceId }
        if (device?.profile != DeviceProfile.LIGHT ||
            deviceId in mutableUiState.value.scheduleUpdatesInFlight
        ) return

        val start = startLocalTime.trim()
        val end = endLocalTime.trim()
        val zone = timezone.trim()
        if (!TIME_PATTERN.matches(start) || !TIME_PATTERN.matches(end)) {
            mutableUiState.update { it.copy(errorMessage = "Use 24-hour time in HH:mm format.") }
            return
        }
        if (start == end) {
            mutableUiState.update { it.copy(errorMessage = "Start and end times must be different.") }
            return
        }
        if (zone.isBlank() || zone.length > 64) {
            mutableUiState.update { it.copy(errorMessage = "Enter a valid timezone.") }
            return
        }

        mutableUiState.update { it.copy(errorMessage = null) }

        executeCommand(
            key = "schedule:$deviceId",
            setInFlight = { value ->
                mutableUiState.update {
                    it.copy(
                        scheduleUpdatesInFlight = if (value) {
                            it.scheduleUpdatesInFlight + deviceId
                        } else {
                            it.scheduleUpdatesInFlight - deviceId
                        },
                    )
                }
            },
            action = {
                repository.updateLightSchedule(HOME_ID, deviceId, enabled, start, end, zone)
            },
            failureMessage = "The light schedule could not be saved.",
        )
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
        val resolvedName = name.trim().ifBlank { defaultFloorName(level) }
        val violations = FloorLayoutValidator.validateFloor(
            name = resolvedName,
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

        var createdFloorId: String? = null
        executeCommand(
            key = "create-floor",
            setInFlight = { value ->
                mutableUiState.update { it.copy(isSavingLayout = value) }
            },
            action = {
                createdFloorId = floorRepository.createFloor(
                    homeId = HOME_ID,
                    name = resolvedName,
                    level = level,
                    gridColumns = gridColumns,
                    gridRows = gridRows,
                )
            },
            failureMessage = "The floor could not be created.",
            onSuccess = {
                val floorId = createdFloorId
                if (floorId != null) {
                    mutableUiState.update { state ->
                        state.copy(
                            selectedFloorId = floorId,
                            rooms = emptyList(),
                            layoutMessage = "Floor created.",
                        )
                    }
                    observeRooms(floorId)
                }
            },
        )
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

        saveLayout(
            action = {
                floorRepository.createRoom(
                    homeId = HOME_ID,
                    floorId = floor.id,
                    room = room,
                )
            },
            successMessage = "Room created.",
            failureMessage = "The room could not be created.",
        )
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

        executeCommand(
            key = "create-device",
            setInFlight = { value ->
                mutableUiState.update { it.copy(isCreatingDevice = value) }
            },
            action = {
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
            },
            failureMessage = "The device could not be created.",
            onSuccess = {
                mutableUiState.update { it.copy(layoutMessage = "Device created.") }
            },
        )
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

        saveLayout(
            action = { floorRepository.deleteFloor(HOME_ID, floorId) },
            successMessage = "Floor deleted.",
            failureMessage = "The floor could not be deleted.",
            failureDetails = { exception ->
                (exception as? FloorContainsDevicesException)?.message
            },
        )
    }

    fun deleteRoom(roomId: String) {
        val floorId = mutableUiState.value.selectedFloorId ?: return
        if (mutableUiState.value.isSavingLayout) return

        saveLayout(
            action = { floorRepository.deleteRoom(HOME_ID, floorId, roomId) },
            successMessage = "Room deleted.",
            failureMessage = "The room could not be deleted.",
            failureDetails = { exception ->
                (exception as? RoomContainsDevicesException)?.message
            },
        )
    }

    fun signOut() {
        userInitiatedSignOut = true
        cancelObservations()
        pendingCommands.clear()
        repository.signOut()

        mutableUiState.value = OutletUiState(
            email = mutableUiState.value.email,
        )
        userInitiatedSignOut = false
    }

    private fun observeAuthentication() {
        authenticationObservation?.cancel()

        authenticationObservation = viewModelScope.launch {
            repository
                .observeAuthentication()
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid == null &&
                        mutableUiState.value.isAuthenticated &&
                        !userInitiatedSignOut
                    ) {
                        resetToSignedOut()
                    }
                }
        }
    }

    private fun observeNetwork() {
        networkObservation?.cancel()

        networkObservation = viewModelScope.launch {
            var wasOnline = networkMonitor.isOnline.value
            networkMonitor.isOnline
                .collect { online ->
                    mutableUiState.update { it.copy(isOffline = !online) }
                    if (online && !wasOnline) {
                        flushPendingCommands()
                    }
                    wasOnline = online
                }
        }
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
                .retryWithBackoff(
                    isRetryable = { cause -> cause.isRecoverable() },
                    onRetry = { _, _ ->
                        mutableUiState.update { it.copy(isRecovering = true) }
                    },
                )
                .catch { cause ->
                    when {
                        cause.isSessionRevoked() -> resetToSignedOut()
                        else -> mutableUiState.update { state ->
                            state.copy(
                                isLoadingOutlet = false,
                                errorMessage = "The outlet could not be loaded.",
                            )
                        }
                    }
                }
                .collect { outlet ->
                    mutableUiState.update { state ->
                        state.copy(
                            isLoadingOutlet = false,
                            outlet = outlet,
                            errorMessage = null,
                            isRecovering = false,
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
                .retryWithBackoff(
                    isRetryable = { cause -> cause.isRecoverable() },
                    onRetry = { _, _ ->
                        mutableUiState.update { it.copy(isRecovering = true) }
                    },
                )
                .catch { cause ->
                    when {
                        cause.isSessionRevoked() -> resetToSignedOut()
                        else -> mutableUiState.update {
                            it.copy(
                                isLoadingDevices = false,
                                errorMessage = "Devices could not be loaded.",
                            )
                        }
                    }
                }
                .collect { devices ->
                    mutableUiState.update {
                        it.copy(
                            devices = devices,
                            isLoadingDevices = false,
                            errorMessage = null,
                            isRecovering = false,
                        )
                    }
                    observeDeviceEvents(devices)
                }
        }
    }

    private fun observeDeviceEvents(devices: List<SmartDevice>) {
        val wanted = devices.map { it.id }.toSet()
        if (wanted.isNotEmpty() && deviceEventJobs.isEmpty()) {
            mutableUiState.update { it.copy(isLoadingEvents = true) }
        }
        deviceEventJobs.keys.filterNot { it in wanted }.forEach { deviceId ->
            deviceEventJobs.remove(deviceId)?.cancel()
            mutableUiState.update { state ->
                state.copy(eventsByDevice = state.eventsByDevice - deviceId)
            }
        }
        wanted.forEach { deviceId ->
            if (deviceEventJobs.containsKey(deviceId)) return@forEach
            val job = viewModelScope.launch {
                repository.observeDeviceEvents(HOME_ID, deviceId)
                    .retryWithBackoff(
                        isRetryable = { cause -> cause.isRecoverable() },
                        onRetry = { _, _ ->
                            mutableUiState.update { it.copy(isRecovering = true) }
                        },
                    )
                    .catch { cause ->
                        when {
                            cause.isSessionRevoked() -> resetToSignedOut()
                            else -> mutableUiState.update { state ->
                                state.copy(
                                    isLoadingEvents = false,
                                    eventsErrorMessage = "Usage history could not be loaded.",
                                )
                            }
                        }
                    }
                    .collect { events ->
                        mutableUiState.update { state ->
                            state.copy(
                                eventsByDevice = state.eventsByDevice + (deviceId to events),
                                isLoadingEvents = false,
                                eventsErrorMessage = null,
                                isRecovering = false,
                            )
                        }
                    }
            }
            deviceEventJobs[deviceId] = job
        }
    }

    private fun observeAlerts() {
        alertObservation?.cancel()
        mutableUiState.update { it.copy(isLoadingAlerts = true) }
        alertObservation = viewModelScope.launch {
            repository.observeAlerts(HOME_ID)
                .retryWithBackoff(
                    isRetryable = { cause -> cause.isRecoverable() },
                    onRetry = { _, _ ->
                        mutableUiState.update { it.copy(isRecovering = true) }
                    },
                )
                .catch { cause ->
                    when {
                        cause.isSessionRevoked() -> resetToSignedOut()
                        else -> mutableUiState.update {
                            it.copy(
                                isLoadingAlerts = false,
                                errorMessage = "Safety alerts could not be loaded.",
                            )
                        }
                    }
                }
                .collect { alerts ->
                    mutableUiState.update {
                        it.copy(
                            alerts = alerts,
                            isLoadingAlerts = false,
                            errorMessage = null,
                            isRecovering = false,
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
                .retryWithBackoff(
                    isRetryable = { cause -> cause.isRecoverable() },
                    onRetry = { _, _ ->
                        mutableUiState.update { it.copy(isRecovering = true) }
                    },
                )
                .catch { cause ->
                    when {
                        cause.isSessionRevoked() -> resetToSignedOut()
                        else -> mutableUiState.update { state ->
                            state.copy(
                                isLoadingFloors = false,
                                layoutMessage = "Floor plans could not be loaded.",
                            )
                        }
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
                            isRecovering = false,
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
                .retryWithBackoff(
                    isRetryable = { cause -> cause.isRecoverable() },
                    onRetry = { _, _ ->
                        mutableUiState.update { it.copy(isRecovering = true) }
                    },
                )
                .catch { cause ->
                    when {
                        cause.isSessionRevoked() -> resetToSignedOut()
                        else -> mutableUiState.update { state ->
                            state.copy(
                                rooms = emptyList(),
                                layoutMessage = "Rooms could not be loaded.",
                            )
                        }
                    }
                }
                .collect { rooms ->
                    if (mutableUiState.value.selectedFloorId == floorId) {
                        mutableUiState.update { state ->
                            state.copy(rooms = rooms, isRecovering = false)
                        }
                    }
                }
        }
    }

    private fun executeCommand(
        key: String,
        setInFlight: (Boolean) -> Unit,
        action: suspend () -> Unit,
        failureMessage: String,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = { _ ->
            mutableUiState.update { it.copy(errorMessage = failureMessage) }
        },
    ) {
        viewModelScope.launch {
            setInFlight(true)
            var attempt = 0
            while (true) {
                val cause = runCatching { action() }.exceptionOrNull()

                if (cause == null) {
                    pendingCommands.remove(key)
                    setInFlight(false)
                    onSuccess()
                    mutableUiState.update {
                        it.copy(isRecovering = pendingCommands.isNotEmpty())
                    }
                    return@launch
                }

                when {
                    cause.isSessionRevoked() -> {
                        pendingCommands.remove(key)
                        setInFlight(false)
                        mutableUiState.update { it.copy(isRecovering = false) }
                        resetToSignedOut()
                        return@launch
                    }

                    !cause.isRecoverable() -> {
                        pendingCommands.remove(key)
                        setInFlight(false)
                        onFailure(cause)
                        mutableUiState.update {
                            it.copy(isRecovering = pendingCommands.isNotEmpty())
                        }
                        return@launch
                    }

                    !networkMonitor.isOnline.value -> {
                        pendingCommands[key] = PendingCommand(
                            key = key,
                            setInFlight = setInFlight,
                            action = action,
                            failureMessage = failureMessage,
                            onSuccess = onSuccess,
                            onFailure = onFailure,
                        )
                        mutableUiState.update { it.copy(isRecovering = true) }
                        return@launch
                    }

                    attempt >= MAX_COMMAND_RETRIES -> {
                        pendingCommands.remove(key)
                        setInFlight(false)
                        onFailure(cause)
                        mutableUiState.update {
                            it.copy(isRecovering = pendingCommands.isNotEmpty())
                        }
                        return@launch
                    }

                    else -> {
                        mutableUiState.update { it.copy(isRecovering = true) }
                        attempt += 1
                        delay(
                            commandBackoffMillis(
                                attempt,
                                baseDelayMillis = COMMAND_RETRY_BASE_MILLIS,
                                maxDelayMillis = COMMAND_RETRY_MAX_MILLIS,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun flushPendingCommands() {
        val commands = pendingCommands.values.toList()
        if (commands.isEmpty()) return

        pendingCommands.clear()
        commands.forEach { command ->
            executeCommand(
                key = command.key,
                setInFlight = command.setInFlight,
                action = command.action,
                failureMessage = command.failureMessage,
                onSuccess = command.onSuccess,
                onFailure = command.onFailure,
            )
        }
        mutableUiState.update { it.copy(isRecovering = pendingCommands.isNotEmpty()) }
    }

    private fun resetToSignedOut(message: String? = null) {
        userInitiatedSignOut = true
        cancelObservations()
        pendingCommands.clear()
        repository.signOut()

        mutableUiState.value = OutletUiState(
            email = mutableUiState.value.email,
            errorMessage = message ?: SESSION_EXPIRED_MESSAGE,
        )
        userInitiatedSignOut = false
    }

    private fun cancelObservations() {
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
        deviceEventJobs.values.forEach(Job::cancel)
        deviceEventJobs.clear()
    }

    private fun saveLayout(
        action: suspend () -> Unit,
        successMessage: String,
        failureMessage: String,
        failureDetails: (Throwable) -> String? = { null },
    ) {
        if (mutableUiState.value.isSavingLayout) return

        executeCommand(
            key = "layout",
            setInFlight = { value ->
                mutableUiState.update { it.copy(isSavingLayout = value) }
            },
            action = action,
            failureMessage = failureMessage,
            onSuccess = {
                mutableUiState.update { it.copy(layoutMessage = successMessage) }
            },
            onFailure = { cause ->
                val message = failureDetails(cause) ?: failureMessage
                mutableUiState.update { it.copy(layoutMessage = message) }
            },
        )
    }

    private companion object {
        const val HOME_ID = "demo-home"
        const val OUTLET_ID = "main-outlet"
        const val MAX_COMMAND_RETRIES = 4
        const val COMMAND_RETRY_BASE_MILLIS = 500L
        const val COMMAND_RETRY_MAX_MILLIS = 8_000L
        const val SESSION_EXPIRED_MESSAGE = "Your session has expired. Please sign in again."
        val TIME_PATTERN = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")
    }
}

private class PendingCommand(
    val key: String,
    val setInFlight: (Boolean) -> Unit,
    val action: suspend () -> Unit,
    val failureMessage: String,
    val onSuccess: () -> Unit,
    val onFailure: (Throwable) -> Unit,
)

private fun Throwable.isRecoverable(): Boolean = FirestoreErrorClassifier.isRecoverable(this)

private fun Throwable.isSessionRevoked(): Boolean = FirestoreErrorClassifier.isSessionRevoked(this)

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