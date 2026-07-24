package com.smarthome.app.ui.outlet

import com.smarthome.app.domain.model.CommandState
import com.smarthome.app.domain.model.DeviceStatus
import com.smarthome.app.domain.model.FloorPlan
import com.smarthome.app.domain.model.OutletDevice
import com.smarthome.app.domain.model.PowerState
import com.smarthome.app.domain.model.RoomLayout
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

    override suspend fun deleteFloor(
        homeId: String,
        floorId: String,
    ) = Unit

    override suspend fun deleteRoom(
        homeId: String,
        floorId: String,
        roomId: String,
    ) = Unit
}

private class FakeOutletRepository(
    override var hasAuthenticatedUser: Boolean = false,
) : OutletRepository {
    private val outlets = MutableSharedFlow<OutletDevice>(replay = 1)

    var signedInEmail: String? = null
        private set

    var requestedPowerState: PowerState? = null
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

    override suspend fun requestPowerState(
        homeId: String,
        deviceId: String,
        powerState: PowerState,
    ) {
        requestedPowerState = powerState
    }

    suspend fun emit(outlet: OutletDevice) {
        outlets.emit(outlet)
    }
}
