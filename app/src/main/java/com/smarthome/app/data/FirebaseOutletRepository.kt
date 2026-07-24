package com.smarthome.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.smarthome.app.domain.model.CommandState
import com.smarthome.app.domain.model.DeviceStatus
import com.smarthome.app.domain.model.OutletDevice
import com.smarthome.app.domain.model.PowerState
import com.smarthome.app.domain.repository.OutletRepository
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseOutletRepository(
    private val authentication: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : OutletRepository {

    override val hasAuthenticatedUser: Boolean
        get() = authentication.currentUser != null

    override suspend fun signIn(
        email: String,
        password: String,
    ) {
        authentication
            .signInWithEmailAndPassword(email, password)
            .await()
    }

    override fun signOut() {
        authentication.signOut()
    }

    override fun observeOutlet(
        homeId: String,
        deviceId: String,
    ): Flow<OutletDevice> = callbackFlow {
        val registration = outletDocument(homeId, deviceId)
            .addSnapshotListener { snapshot, exception ->
                when {
                    exception != null -> close(exception)

                    snapshot == null || !snapshot.exists() -> {
                        close(IllegalStateException("Outlet does not exist."))
                    }

                    else -> {
                        runCatching(snapshot::toOutletDevice)
                            .onSuccess { outlet -> trySend(outlet) }
                            .onFailure { mappingError -> close(mappingError) }
                    }
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun requestPowerState(
        homeId: String,
        deviceId: String,
        powerState: PowerState,
    ) {
        val userId = authentication.currentUser?.uid
            ?: throw IllegalStateException("Authentication is required.")

        outletDocument(homeId, deviceId)
            .update(
                mapOf(
                    "desired.status" to powerState.name,
                    "desired.requestId" to UUID.randomUUID().toString(),
                    "desired.requestedBy" to userId,
                    "desired.requestedAt" to FieldValue.serverTimestamp(),
                    "commandState" to CommandState.PENDING.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    private fun outletDocument(
        homeId: String,
        deviceId: String,
    ) = firestore
        .collection("homes")
        .document(homeId)
        .collection("devices")
        .document(deviceId)
}

private fun DocumentSnapshot.toOutletDevice(): OutletDevice {
    return OutletDevice(
        id = id,
        name = getString("name")
            ?: throw IllegalStateException("Outlet name is missing."),
        desiredStatus = parsePowerState(getString("desired.status")),
        reportedStatus = parseDeviceStatus(getString("reported.status")),
        commandState = parseCommandState(getString("commandState")),
        desiredRequestId = getString("desired.requestId"),
        reportedRequestId = getString("reported.requestId"),
    )
}

private fun parsePowerState(value: String?): PowerState {
    return PowerState.entries.firstOrNull { state -> state.name == value }
        ?: throw IllegalStateException("Invalid desired status: $value")
}

private fun parseDeviceStatus(value: String?): DeviceStatus {
    return DeviceStatus.entries.firstOrNull { status -> status.name == value }
        ?: throw IllegalStateException("Invalid reported status: $value")
}

private fun parseCommandState(value: String?): CommandState {
    return CommandState.entries.firstOrNull { state -> state.name == value }
        ?: throw IllegalStateException("Invalid command state: $value")
}