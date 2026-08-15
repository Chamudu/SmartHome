package com.smarthome.app.domain.model

/** Source category of a recorded device event. */
enum class EventOrigin {
    ANDROID,
    SIMULATOR,
    AUTOMATION,
    SYSTEM,
}

/**
 * Append-only activity record for a single device state transition. Events are
 * written by trusted backend automation and are read-only to clients.
 */
data class DeviceEvent(
    val id: String,
    val type: String,
    val fromStatus: DeviceStatus?,
    val toStatus: DeviceStatus?,
    val origin: EventOrigin,
    val actorId: String?,
    val requestId: String?,
    val reason: String?,
    val occurredAtMillis: Long,
    val channelId: String?,
)
