package com.smarthome.app.ui.outlet

import com.smarthome.app.domain.model.CommandState
import com.smarthome.app.domain.model.DeviceStatus
import com.smarthome.app.domain.model.DeviceProfile
import com.smarthome.app.domain.model.FloorPlan
import com.smarthome.app.domain.model.OutletDevice
import com.smarthome.app.domain.model.NewDevice
import com.smarthome.app.domain.model.PowerState
import com.smarthome.app.domain.model.RoomLayout
import com.smarthome.app.domain.model.SmartDevice
import com.smarthome.app.domain.model.HomeAlert
import com.smarthome.app.domain.model.AlertSeverity
import com.smarthome.app.domain.model.DeviceConfiguration
import com.smarthome.app.domain.model.DeviceEvent
import com.smarthome.app.domain.model.EventOrigin
import com.smarthome.app.domain.model.SwitchChannel
import com.smarthome.app.domain.repository.FloorRepository
import com.smarthome.app.domain.repository.OutletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OutletViewModelTest {

    @Test
    fun `backend safety alert is exposed in realtime UI state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val repository = FakeOutletRepository(hasAuthenticatedUser = true)
            val viewModel = OutletViewModel(repository, FakeFloorRepository())
            advanceUntilIdle()

            repository.emitAlerts(
                listOf(
                    HomeAlert(
                        id = "alert-1",
                        deviceId = "iron",
                        type = "SAFETY_CUTOFF",
                        severity = AlertSeverity.CRITICAL,
                        message = "Iron was switched off.",
                        createdAtMillis = 1234L,
                    ),
                ),
            )
            advanceUntilIdle()

            assertEquals("alert-1", viewModel.uiState.value.alerts.single().id)
            assertFalse(viewModel.uiState.value.isLoadingAlerts)

            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `recorded device events are exposed in realtime UI state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val repository = FakeOutletRepository(hasAuthenticatedUser = true)
            val viewModel = OutletViewModel(repository, FakeFloorRepository())
            advanceUntilIdle()

            repository.emitDevices(listOf(lightDevice()))
            advanceUntilIdle()

            repository.emitEvents(
                listOf(
                    DeviceEvent(
                        id = "state-request-1",
                        type = "STATE_REPORTED",
                        fromStatus = DeviceStatus.OFF,
                        toStatus = DeviceStatus.ON,
                        origin = EventOrigin.ANDROID,
                        actorId = "owner",
                        requestId = "request-1",
                        reason = null,
                        occurredAtMillis = 1234L,
                        channelId = null,
                    ),
                ),
            )
            advanceUntilIdle()

            assertEquals(
                "state-request-1",
                viewModel.uiState.value.eventsByDevice["porch-light"]?.single()?.id,
            )
            assertFalse(viewModel.uiState.value.isLoadingEvents)

            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `successful sign in starts outlet observation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val repository = FakeOutletRepository()
            val viewModel = OutletViewModel(
                repository = repository,
                floorRepository = FakeFloorRepository(),
            )

            viewModel.updateEmail(" owner@smarthome.test ")
            viewModel.updatePassword("secret-password")
            viewModel.signIn()
            advanceUntilIdle()

            assertEquals("owner@smarthome.test", repository.signedInEmail)
            assertTrue(viewModel.uiState.value.isAuthenticated)
            assertEquals("owner@smarthome.test", viewModel.uiState.value.accountEmail)
            assertEquals("", viewModel.uiState.value.password)

            repository.emit(outlet())
            advanceUntilIdle()

            assertEquals("main-outlet", viewModel.uiState.value.outlet?.id)
            assertFalse(viewModel.uiState.value.isLoadingOutlet)

            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `power request is forwarded for controllable outlet`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val repository = FakeOutletRepository(
                hasAuthenticatedUser = true,
            )
            val viewModel = OutletViewModel(
                repository = repository,
                floorRepository = FakeFloorRepository(),
            )
            advanceUntilIdle()

            repository.emit(outlet())
            advanceUntilIdle()

            viewModel.requestPowerState(PowerState.ON)
            advanceUntilIdle()

            assertEquals(PowerState.ON, repository.requestedPowerState)
            assertFalse(viewModel.uiState.value.isSendingCommand)

            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `power request is blocked while outlet reports error`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val repository = FakeOutletRepository(
                hasAuthenticatedUser = true,
            )
            val viewModel = OutletViewModel(
                repository = repository,
                floorRepository = FakeFloorRepository(),
            )
            advanceUntilIdle()

            repository.emit(
                outlet(reportedStatus = DeviceStatus.ERROR),
            )
            advanceUntilIdle()

            viewModel.requestPowerState(PowerState.ON)
            advanceUntilIdle()

            assertNull(repository.requestedPowerState)

            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `multi-switch command targets one channel`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeOutletRepository(hasAuthenticatedUser = true)
            val viewModel = OutletViewModel(repository, FakeFloorRepository())
            advanceUntilIdle()
            repository.emitDevices(
                listOf(
                    SmartDevice(
                        id = "hall-switch",
                        name = "Hall switch",
                        profile = DeviceProfile.MULTI_SWITCH,
                        floorId = "ground-floor",
                        roomId = "hall",
                        column = 1,
                        row = 1,
                        desiredStatus = PowerState.OFF,
                        reportedStatus = DeviceStatus.OFF,
                        commandState = CommandState.IDLE,
                        configuration = DeviceConfiguration.MultiSwitch(
                            channels = listOf(
                                SwitchChannel("channel-1", "Lamp", PowerState.OFF, DeviceStatus.OFF, null),
                                SwitchChannel("channel-2", "Fan", PowerState.OFF, DeviceStatus.OFF, null),
                            ),
                        ),
                    ),
                ),
            )
            advanceUntilIdle()

            viewModel.requestSwitchChannelState("hall-switch", "channel-2", PowerState.ON)
            advanceUntilIdle()

            assertEquals(
                SwitchChannelRequest("hall-switch", "channel-2", PowerState.ON),
                repository.switchChannelRequest,
            )
            assertTrue(viewModel.uiState.value.switchCommandsInFlight.isEmpty())
            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `generic light power control routes to selected device`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeOutletRepository(hasAuthenticatedUser = true)
            val viewModel = OutletViewModel(repository, FakeFloorRepository())
            advanceUntilIdle()
            repository.emitDevices(
                listOf(
                    SmartDevice(
                        id = "porch-light",
                        name = "Porch light",
                        profile = DeviceProfile.LIGHT,
                        floorId = "ground-floor",
                        roomId = "porch",
                        column = 2,
                        row = 2,
                        desiredStatus = PowerState.OFF,
                        reportedStatus = DeviceStatus.OFF,
                        commandState = CommandState.IDLE,
                        configuration = DeviceConfiguration.Light(scheduleEnabled = false),
                    ),
                ),
            )
            advanceUntilIdle()

            viewModel.requestDevicePowerState("porch-light", PowerState.ON)
            advanceUntilIdle()

            assertEquals("porch-light", repository.requestedDeviceId)
            assertEquals(PowerState.ON, repository.requestedPowerState)
            assertTrue(viewModel.uiState.value.deviceCommandsInFlight.isEmpty())
            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `valid overnight light schedule is forwarded`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeOutletRepository(hasAuthenticatedUser = true)
            val viewModel = OutletViewModel(repository, FakeFloorRepository())
            advanceUntilIdle()
            repository.emitDevices(listOf(lightDevice()))
            advanceUntilIdle()

            viewModel.updateLightSchedule(
                "porch-light", true, "18:00", "06:00", "Asia/Colombo",
            )
            advanceUntilIdle()

            assertEquals(
                LightScheduleRequest("porch-light", true, "18:00", "06:00", "Asia/Colombo"),
                repository.lightScheduleRequest,
            )
            assertTrue(viewModel.uiState.value.scheduleUpdatesInFlight.isEmpty())
            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `invalid light schedule is rejected before repository call`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeOutletRepository(hasAuthenticatedUser = true)
            val viewModel = OutletViewModel(repository, FakeFloorRepository())
            advanceUntilIdle()
            repository.emitDevices(listOf(lightDevice()))
            advanceUntilIdle()

            viewModel.updateLightSchedule(
                "porch-light", true, "25:00", "06:00", "Asia/Colombo",
            )
            advanceUntilIdle()

            assertNull(repository.lightScheduleRequest)
            assertEquals("Use 24-hour time in HH:mm format.", viewModel.uiState.value.errorMessage)
            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `placing outlet infers containing room and forwards logical coordinate`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        try {
            val outletRepository = FakeOutletRepository(hasAuthenticatedUser = true)
            val floorRepository = FakeFloorRepository()
            val viewModel = OutletViewModel(outletRepository, floorRepository)

            floorRepository.emitFloors(listOf(floor()))
            advanceUntilIdle()
            floorRepository.emitRooms(
                listOf(RoomLayout("kitchen", "Kitchen", 0, 0, 4, 4)),
            )
            advanceUntilIdle()

            viewModel.placeOutlet(column = 2, row = 3)
            advanceUntilIdle()

            assertEquals(
                DevicePlacement("ground-floor", "kitchen", 2, 3),
                outletRepository.placement,
            )
            assertEquals("Outlet placed.", viewModel.uiState.value.layoutMessage)

            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `creating safety outlet converts minutes and infers room`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val outletRepository = FakeOutletRepository(hasAuthenticatedUser = true)
            val floorRepository = FakeFloorRepository()
            val viewModel = OutletViewModel(outletRepository, floorRepository)
            floorRepository.emitFloors(listOf(floor()))
            advanceUntilIdle()
            floorRepository.emitRooms(
                listOf(RoomLayout("utility", "Utility", 0, 0, 4, 4)),
            )
            advanceUntilIdle()

            viewModel.createDevice(
                name = "Iron",
                profile = DeviceProfile.SAFETY_OUTLET,
                column = 1,
                row = 2,
                channelCount = 2,
                maxOnDurationMinutes = 15,
                mediaUri = "",
            )
            advanceUntilIdle()

            assertEquals("utility", outletRepository.createdDevice?.roomId)
            assertEquals(900, outletRepository.createdDevice?.maxOnDurationSeconds)
            assertEquals("Device created.", viewModel.uiState.value.layoutMessage)
            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `creating camera forwards configured media uri and infers room`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val outletRepository = FakeOutletRepository(hasAuthenticatedUser = true)
            val floorRepository = FakeFloorRepository()
            val viewModel = OutletViewModel(outletRepository, floorRepository)
            floorRepository.emitFloors(listOf(floor()))
            advanceUntilIdle()
            floorRepository.emitRooms(
                listOf(RoomLayout("office", "Office", 0, 0, 4, 4)),
            )
            advanceUntilIdle()

            val mediaUri = "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=640&q=80"
            viewModel.createDevice(
                name = "Living room camera",
                profile = DeviceProfile.CAMERA,
                column = 2,
                row = 3,
                channelCount = 2,
                maxOnDurationMinutes = 15,
                mediaUri = mediaUri,
            )
            advanceUntilIdle()

            assertEquals("office", outletRepository.createdDevice?.roomId)
            assertEquals(DeviceProfile.CAMERA, outletRepository.createdDevice?.profile)
            assertEquals(mediaUri, outletRepository.createdDevice?.mediaUri)
            assertEquals("Device created.", viewModel.uiState.value.layoutMessage)
            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `creating camera rejects non-https media uri`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val outletRepository = FakeOutletRepository(hasAuthenticatedUser = true)
            val floorRepository = FakeFloorRepository()
            val viewModel = OutletViewModel(outletRepository, floorRepository)
            floorRepository.emitFloors(listOf(floor()))
            advanceUntilIdle()
            floorRepository.emitRooms(
                listOf(RoomLayout("office", "Office", 0, 0, 4, 4)),
            )
            advanceUntilIdle()

            viewModel.createDevice(
                name = "Bad camera",
                profile = DeviceProfile.CAMERA,
                column = 2,
                row = 3,
                channelCount = 2,
                maxOnDurationMinutes = 15,
                mediaUri = "javascript:alert(1)",
            )
            advanceUntilIdle()

            assertNull(outletRepository.createdDevice)
            assertEquals(
                "Camera media must use an HTTPS URI.",
                viewModel.uiState.value.layoutMessage,
            )
            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `moving device validates destination and infers room`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val outletRepository = FakeOutletRepository(hasAuthenticatedUser = true)
            val floorRepository = FakeFloorRepository()
            val viewModel = OutletViewModel(outletRepository, floorRepository)
            floorRepository.emitFloors(listOf(floor()))
            advanceUntilIdle()
            floorRepository.emitRooms(listOf(RoomLayout("kitchen", "Kitchen", 0, 0, 4, 4)))
            advanceUntilIdle()

            viewModel.moveDevice("other-device", 2, 2)
            advanceUntilIdle()

            assertEquals(DevicePlacement("ground-floor", "kitchen", 2, 2), outletRepository.placement)
            assertEquals("Device moved.", viewModel.uiState.value.layoutMessage)
            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `primary synchronized outlet cannot be deleted`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeOutletRepository(hasAuthenticatedUser = true)
            val viewModel = OutletViewModel(repository, FakeFloorRepository())

            viewModel.deleteDevice("main-outlet")
            advanceUntilIdle()

            assertNull(repository.deletedDeviceId)
            assertEquals(
                "The primary synchronized outlet cannot be deleted yet.",
                viewModel.uiState.value.layoutMessage,
            )
            viewModel.signOut()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun floor() = FloorPlan(
        id = "ground-floor",
        name = "Ground floor",
        level = 0,
        gridColumns = 12,
        gridRows = 16,
    )

    private fun outlet(
        reportedStatus: DeviceStatus = DeviceStatus.OFF,
    ) = OutletDevice(
        id = "main-outlet",
        name = "Main outlet",
        desiredStatus = PowerState.OFF,
        reportedStatus = reportedStatus,
        commandState = CommandState.IDLE,
        desiredRequestId = null,
        reportedRequestId = null,
        floorId = "ground-floor",
        roomId = "utility",
        column = 1,
        row = 1,
    )
}

private class FakeFloorRepository : FloorRepository {
    private val floors = MutableSharedFlow<List<FloorPlan>>(replay = 1)
    private val rooms = MutableSharedFlow<List<RoomLayout>>(replay = 1)

    override fun observeFloors(homeId: String): Flow<List<FloorPlan>> = floors

    override fun observeRooms(
        homeId: String,
        floorId: String,
    ): Flow<List<RoomLayout>> = rooms

    override suspend fun createFloor(
        homeId: String,
        name: String,
        level: Int,
        gridColumns: Int,
        gridRows: Int,
    ): String = "created-floor"

    override suspend fun createRoom(
        homeId: String,
        floorId: String,
        room: RoomLayout,
    ): String = "created-room"

    override suspend fun updateFloor(
        homeId: String,
        floor: FloorPlan,
    ) = Unit

    override suspend fun updateRoom(
        homeId: String,
        floorId: String,
        room: RoomLayout,
    ) = Unit

    override suspend fun deleteFloor(
        homeId: String,
        floorId: String,
    ) = Unit

    override suspend fun deleteRoom(
        homeId: String,
        floorId: String,
        roomId: String,
    ) = Unit

    suspend fun emitFloors(value: List<FloorPlan>) {
        floors.emit(value)
    }

    suspend fun emitRooms(value: List<RoomLayout>) {
        rooms.emit(value)
    }
}

private data class DevicePlacement(
    val floorId: String,
    val roomId: String?,
    val column: Int,
    val row: Int,
)

private data class SwitchChannelRequest(
    val deviceId: String,
    val channelId: String,
    val powerState: PowerState,
)

private data class LightScheduleRequest(
    val deviceId: String,
    val enabled: Boolean,
    val startLocalTime: String,
    val endLocalTime: String,
    val timezone: String,
)

private class FakeOutletRepository(
    override var hasAuthenticatedUser: Boolean = false,
) : OutletRepository {
    override val authenticatedUserEmail: String?
        get() = signedInEmail
    private val outlets = MutableSharedFlow<OutletDevice>(replay = 1)
    private val devices = MutableSharedFlow<List<SmartDevice>>(replay = 1)
    private val alerts = MutableSharedFlow<List<HomeAlert>>(replay = 1)
    private val events = MutableSharedFlow<List<DeviceEvent>>(replay = 1)

    var signedInEmail: String? = null
        private set

    var requestedPowerState: PowerState? = null
        private set

    var requestedDeviceId: String? = null
        private set

    var switchChannelRequest: SwitchChannelRequest? = null
        private set

    var lightScheduleRequest: LightScheduleRequest? = null
        private set

    var placement: DevicePlacement? = null
        private set

    var createdDevice: NewDevice? = null
        private set

    var deletedDeviceId: String? = null
        private set

    override suspend fun signIn(
        email: String,
        password: String,
    ) {
        signedInEmail = email
        hasAuthenticatedUser = true
    }

    override fun signOut() {
        hasAuthenticatedUser = false
    }

    override fun observeOutlet(
        homeId: String,
        deviceId: String,
    ): Flow<OutletDevice> = outlets

    override fun observeDevices(homeId: String): Flow<List<SmartDevice>> = devices

    override fun observeAlerts(homeId: String): Flow<List<HomeAlert>> = alerts

    override fun observeDeviceEvents(homeId: String, deviceId: String): Flow<List<DeviceEvent>> = events

    override suspend fun createDevice(homeId: String, device: NewDevice): String {
        createdDevice = device
        return "created-device"
    }

    override suspend fun requestPowerState(
        homeId: String,
        deviceId: String,
        powerState: PowerState,
    ) {
        requestedDeviceId = deviceId
        requestedPowerState = powerState
    }

    override suspend fun requestSwitchChannelState(
        homeId: String,
        deviceId: String,
        channelId: String,
        powerState: PowerState,
    ) {
        switchChannelRequest = SwitchChannelRequest(deviceId, channelId, powerState)
    }

    override suspend fun updateLightSchedule(
        homeId: String,
        deviceId: String,
        enabled: Boolean,
        startLocalTime: String,
        endLocalTime: String,
        timezone: String,
    ) {
        lightScheduleRequest = LightScheduleRequest(
            deviceId, enabled, startLocalTime, endLocalTime, timezone,
        )
    }

    override suspend fun placeOutlet(
        homeId: String,
        deviceId: String,
        floorId: String,
        roomId: String?,
        column: Int,
        row: Int,
    ) {
        placement = DevicePlacement(floorId, roomId, column, row)
    }

    override suspend fun placeDevice(
        homeId: String,
        deviceId: String,
        floorId: String,
        roomId: String?,
        column: Int,
        row: Int,
    ) {
        placement = DevicePlacement(floorId, roomId, column, row)
    }

    override suspend fun deleteDevice(homeId: String, deviceId: String) {
        deletedDeviceId = deviceId
    }

    suspend fun emit(outlet: OutletDevice) {
        outlets.emit(outlet)
    }

    suspend fun emitAlerts(value: List<HomeAlert>) {
        alerts.emit(value)
    }

    suspend fun emitDevices(value: List<SmartDevice>) {
        devices.emit(value)
    }

    suspend fun emitEvents(value: List<DeviceEvent>) {
        events.emit(value)
    }
}

private fun lightDevice() = SmartDevice(
    id = "porch-light",
    name = "Porch light",
    profile = DeviceProfile.LIGHT,
    floorId = "ground-floor",
    roomId = "porch",
    column = 2,
    row = 2,
    desiredStatus = PowerState.OFF,
    reportedStatus = DeviceStatus.OFF,
    commandState = CommandState.IDLE,
    configuration = DeviceConfiguration.Light(),
)
