package com.smarthome.app.domain.repository

import com.smarthome.app.domain.model.OutletDevice
import com.smarthome.app.domain.model.PowerState
import com.smarthome.app.domain.model.SmartDevice
import com.smarthome.app.domain.model.NewDevice
import com.smarthome.app.domain.model.HomeAlert
import com.smarthome.app.domain.model.DeviceEvent
import kotlinx.coroutines.flow.Flow

interface OutletRepository {
    val hasAuthenticatedUser: Boolean
    val authenticatedUserEmail: String?

    suspend fun signIn(
        email: String,
        password: String,
    )

    fun signOut()

    /**
     * Emits the current authenticated user id, or null once the session ends (sign-out, expiry, or
     * revocation). Used to route the user back to the authentication flow.
     */
    fun observeAuthentication(): Flow<String?>

    fun observeOutlet(
        homeId: String,
        deviceId: String,
    ): Flow<OutletDevice>

    fun observeDevices(homeId: String): Flow<List<SmartDevice>>

    fun observeAlerts(homeId: String): Flow<List<HomeAlert>>

    fun observeDeviceEvents(
        homeId: String,
        deviceId: String,
    ): Flow<List<DeviceEvent>>

    suspend fun createDevice(
        homeId: String,
        device: NewDevice,
    ): String

    suspend fun requestPowerState(
        homeId: String,
        deviceId: String,
        powerState: PowerState,
    )

    suspend fun requestSwitchChannelState(
        homeId: String,
        deviceId: String,
        channelId: String,
        powerState: PowerState,
    )

    suspend fun updateLightSchedule(
        homeId: String,
        deviceId: String,
        enabled: Boolean,
        startLocalTime: String,
        endLocalTime: String,
        timezone: String,
    )

    suspend fun placeOutlet(
        homeId: String,
        deviceId: String,
        floorId: String,
        roomId: String?,
        column: Int,
        row: Int,
    )

    suspend fun placeDevice(
        homeId: String,
        deviceId: String,
        floorId: String,
        roomId: String?,
        column: Int,
        row: Int,
    )

    suspend fun deleteDevice(homeId: String, deviceId: String)
}
