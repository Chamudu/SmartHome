package com.smarthome.app.domain.repository

import com.smarthome.app.domain.model.OutletDevice
import com.smarthome.app.domain.model.PowerState
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

    suspend fun requestPowerState(
        homeId: String,
        deviceId: String,
        powerState: PowerState,
    )
}