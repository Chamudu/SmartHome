package com.smarthome.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.smarthome.app.domain.model.CommandState
import com.smarthome.app.domain.model.DeviceStatus
import com.smarthome.app.domain.model.OutletDevice
import com.smarthome.app.domain.model.PowerState
import com.smarthome.app.domain.model.DeviceConfiguration
import com.smarthome.app.domain.model.DeviceProfile
import com.smarthome.app.domain.model.NewDevice
import com.smarthome.app.domain.model.SmartDevice
import com.smarthome.app.domain.model.SwitchChannel
import com.smarthome.app.domain.model.AlertSeverity
import com.smarthome.app.domain.model.HomeAlert
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

    override val authenticatedUserEmail: String?
        get() = authentication.currentUser?.email

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

    override fun observeDevices(homeId: String): Flow<List<SmartDevice>> = callbackFlow {
        val registration = devicesCollection(homeId)
            .addSnapshotListener { snapshot, exception ->
                when {
                    exception != null -> close(exception)
                    snapshot == null -> close(IllegalStateException("Device snapshot is missing."))
                    else -> runCatching {
                        snapshot.documents
                            .map(DocumentSnapshot::toSmartDevice)
                            .sortedBy(SmartDevice::name)
                    }.onSuccess(::trySend).onFailure(::close)
                }
            }

        awaitClose { registration.remove() }
    }

    override fun observeAlerts(homeId: String): Flow<List<HomeAlert>> = callbackFlow {
        val registration = firestore
            .collection("homes")
            .document(homeId)
            .collection("alerts")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, exception ->
                when {
                    exception != null -> close(exception)
                    snapshot == null -> close(IllegalStateException("Alert snapshot is missing."))
                    else -> runCatching {
                        snapshot.documents.map(DocumentSnapshot::toHomeAlert)
                    }.onSuccess(::trySend).onFailure(::close)
                }
            }

        awaitClose { registration.remove() }
    }

    override suspend fun createDevice(
        homeId: String,
        device: NewDevice,
    ): String {
        val configuration = when (device.profile) {
            DeviceProfile.OUTLET -> emptyMap<String, Any>()
            DeviceProfile.MULTI_SWITCH -> mapOf(
                "channels" to (1..device.channelCount).map { number ->
                    mapOf(
                        "id" to "channel-$number",
                        "name" to "Switch $number",
                        "desiredStatus" to PowerState.OFF.name,
                        "reportedStatus" to DeviceStatus.OFF.name,
                        "requestId" to null,
                    )
                },
            )
            DeviceProfile.SAFETY_OUTLET -> mapOf(
                "maxOnDurationSeconds" to device.maxOnDurationSeconds,
                "activatedAt" to null,
                "cutoffDueAt" to null,
            )
            DeviceProfile.LIGHT -> mapOf(
                "schedule" to mapOf(
                    "enabled" to false,
                    "startLocalTime" to "18:00",
                    "endLocalTime" to "22:00",
                    "timezone" to "Asia/Colombo",
                    "lastEvaluatedAt" to null,
                ),
            )
            DeviceProfile.CAMERA -> mapOf(
                "mediaType" to "SNAPSHOT",
                "mediaUri" to device.mediaUri.trim(),
                "capturedAt" to FieldValue.serverTimestamp(),
            )
        }

        return devicesCollection(homeId).add(
            mapOf(
                "name" to device.name.trim(),
                "profile" to device.profile.name,
                "floorId" to device.floorId,
                "roomId" to device.roomId,
                "position" to mapOf("column" to device.column, "row" to device.row),
                "desired" to mapOf(
                    "status" to PowerState.OFF.name,
                    "requestId" to null,
                    "requestedBy" to null,
                    "requestedAt" to null,
                ),
                "reported" to mapOf(
                    "status" to DeviceStatus.OFF.name,
                    "requestId" to null,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "errorCode" to null,
                ),
                "commandState" to CommandState.IDLE.name,
                "config" to configuration,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await().id
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

    override suspend fun requestSwitchChannelState(
        homeId: String,
        deviceId: String,
        channelId: String,
        powerState: PowerState,
    ) {
        check(authentication.currentUser != null) { "Authentication is required." }
        val reference = outletDocument(homeId, deviceId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            check(snapshot.getString("profile") == DeviceProfile.MULTI_SWITCH.name) {
                "Device is not a multi-switch."
            }
            val channels = (snapshot.get("config.channels") as? List<*>)
                ?.map { raw ->
                    (raw as? Map<*, *>)?.entries?.associate { (key, value) -> key.toString() to value }
                        ?: error("Invalid switch channel.")
                }
                ?: error("Switch channels are missing.")
            check(channels.any { channel -> channel["id"] == channelId }) {
                "Switch channel does not exist."
            }
            val nextChannels = channels.map { channel ->
                if (channel["id"] != channelId) channel else channel + mapOf(
                    "desiredStatus" to powerState.name,
                    "requestId" to UUID.randomUUID().toString(),
                )
            }
            transaction.update(reference, mapOf(
                "config.channels" to nextChannels,
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
        }.await()
    }

    override suspend fun updateLightSchedule(
        homeId: String,
        deviceId: String,
        enabled: Boolean,
        startLocalTime: String,
        endLocalTime: String,
        timezone: String,
    ) {
        outletDocument(homeId, deviceId).update(
            mapOf(
                "config.schedule.enabled" to enabled,
                "config.schedule.startLocalTime" to startLocalTime,
                "config.schedule.endLocalTime" to endLocalTime,
                "config.schedule.timezone" to timezone,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    override suspend fun placeOutlet(
        homeId: String,
        deviceId: String,
        floorId: String,
        roomId: String?,
        column: Int,
        row: Int,
    ) {
        placeDevice(homeId, deviceId, floorId, roomId, column, row)
    }

    override suspend fun placeDevice(
        homeId: String,
        deviceId: String,
        floorId: String,
        roomId: String?,
        column: Int,
        row: Int,
    ) {
        outletDocument(homeId, deviceId)
            .update(
                mapOf(
                    "floorId" to floorId,
                    "roomId" to roomId,
                    "position.column" to column,
                    "position.row" to row,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    override suspend fun deleteDevice(homeId: String, deviceId: String) {
        outletDocument(homeId, deviceId).delete().await()
    }

    private fun outletDocument(
        homeId: String,
        deviceId: String,
    ) = devicesCollection(homeId)
        .document(deviceId)

    private fun devicesCollection(homeId: String) = firestore
        .collection("homes")
        .document(homeId)
        .collection("devices")
}

private fun DocumentSnapshot.toSmartDevice(): SmartDevice {
    val profile = DeviceProfile.entries.firstOrNull { it.name == getString("profile") }
        ?: throw IllegalStateException("Invalid device profile.")
    val config = get("config") as? Map<*, *> ?: emptyMap<String, Any>()
    val configuration = when (profile) {
        DeviceProfile.OUTLET -> DeviceConfiguration.Outlet
        DeviceProfile.MULTI_SWITCH -> DeviceConfiguration.MultiSwitch(
            channels = (config["channels"] as? List<*>)?.map { rawChannel ->
                val channel = rawChannel as? Map<*, *>
                    ?: throw IllegalStateException("Invalid switch channel.")
                SwitchChannel(
                    id = channel["id"] as? String
                        ?: throw IllegalStateException("Switch channel ID is missing."),
                    name = channel["name"] as? String
                        ?: throw IllegalStateException("Switch channel name is missing."),
                    desiredStatus = parsePowerState(channel["desiredStatus"] as? String),
                    reportedStatus = parseDeviceStatus(channel["reportedStatus"] as? String),
                    requestId = channel["requestId"] as? String,
                )
            } ?: throw IllegalStateException("Switch channels are missing."),
        )
        DeviceProfile.SAFETY_OUTLET -> DeviceConfiguration.SafetyOutlet(
            maxOnDurationSeconds = config.requiredInt("maxOnDurationSeconds"),
        )
        DeviceProfile.LIGHT -> DeviceConfiguration.Light(
            scheduleEnabled = (config["schedule"] as? Map<*, *>)?.get("enabled") as? Boolean ?: false,
            startLocalTime = (config["schedule"] as? Map<*, *>)?.get("startLocalTime") as? String ?: "18:00",
            endLocalTime = (config["schedule"] as? Map<*, *>)?.get("endLocalTime") as? String ?: "22:00",
            timezone = (config["schedule"] as? Map<*, *>)?.get("timezone") as? String ?: "Asia/Colombo",
        )
        DeviceProfile.CAMERA -> DeviceConfiguration.Camera(
            mediaUri = config["mediaUri"] as? String ?: "",
        )
    }
    return SmartDevice(
        id = id,
        name = getString("name") ?: throw IllegalStateException("Device name is missing."),
        profile = profile,
        floorId = getString("floorId") ?: throw IllegalStateException("Device floor is missing."),
        roomId = getString("roomId"),
        column = requiredInt("position.column"),
        row = requiredInt("position.row"),
        desiredStatus = parsePowerState(getString("desired.status")),
        reportedStatus = parseDeviceStatus(getString("reported.status")),
        commandState = parseCommandState(getString("commandState")),
        configuration = configuration,
    )
}

private fun DocumentSnapshot.toHomeAlert(): HomeAlert {
    val severityValue = getString("severity")
    val severity = AlertSeverity.entries.firstOrNull { it.name == severityValue }
        ?: throw IllegalStateException("Invalid alert severity: $severityValue")
    return HomeAlert(
        id = id,
        deviceId = getString("deviceId")
            ?: throw IllegalStateException("Alert device is missing."),
        type = getString("type")
            ?: throw IllegalStateException("Alert type is missing."),
        severity = severity,
        message = getString("message")
            ?: throw IllegalStateException("Alert message is missing."),
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time
            ?: throw IllegalStateException("Alert creation time is missing."),
    )
}

private fun Map<*, *>.requiredInt(field: String): Int {
    val value = this[field] as? Number ?: throw IllegalStateException("$field is missing.")
    return value.toInt()
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
        floorId = getString("floorId")
            ?: throw IllegalStateException("Outlet floor is missing."),
        roomId = getString("roomId"),
        column = requiredInt("position.column"),
        row = requiredInt("position.row"),
    )
}

private fun DocumentSnapshot.requiredInt(field: String): Int {
    val value = getLong(field)
        ?: throw IllegalStateException("$field is missing.")
    check(value in Int.MIN_VALUE..Int.MAX_VALUE) { "$field is outside the supported range." }
    return value.toInt()
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
