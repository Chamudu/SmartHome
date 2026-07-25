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

private class FakeOutletRepository(
    override var hasAuthenticatedUser: Boolean = false,
) : OutletRepository {
    private val outlets = MutableSharedFlow<OutletDevice>(replay = 1)
    private val devices = MutableSharedFlow<List<SmartDevice>>(replay = 1)

    var signedInEmail: String? = null
        private set

    var requestedPowerState: PowerState? = null
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

    override suspend fun createDevice(homeId: String, device: NewDevice): String {
        createdDevice = device
        return "created-device"
    }

    override suspend fun requestPowerState(
        homeId: String,
        deviceId: String,
        powerState: PowerState,
    ) {
        requestedPowerState = powerState
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
}
