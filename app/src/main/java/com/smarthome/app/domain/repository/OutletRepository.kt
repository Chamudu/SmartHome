package com.smarthome.app.domain.repository

import com.smarthome.app.domain.model.OutletDevice
import com.smarthome.app.domain.model.PowerState
import com.smarthome.app.domain.model.SmartDevice
import com.smarthome.app.domain.model.NewDevice
import com.smarthome.app.domain.model.HomeAlert
import kotlinx.coroutines.flow.Flow

interface OutletRepository {
    val hasAuthenticatedUser: Boolean

    suspend fun signIn(
        email: String,
        password: String,
    )

    fun signOut()

    fun observeOutlet(
        homeId: String,
        deviceId: String,
    ): Flow<OutletDevice>

    fun observeDevices(homeId: String): Flow<List<SmartDevice>>

    fun observeAlerts(homeId: String): Flow<List<HomeAlert>>

    suspend fun createDevice(
        homeId: String,
        device: NewDevice,
    ): String

    suspend fun requestPowerState(
        homeId: String,
        deviceId: String,
        powerState: PowerState,
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
