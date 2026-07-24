package com.smarthome.app.domain.model

enum class PowerState {
    ON,
    OFF,
}

enum class CommandState {
    IDLE,
    PENDING,
    APPLIED,
    REJECTED,
}

data class OutletDevice(
    val id: String,
    val name: String,
    val desiredStatus: PowerState,
    val reportedStatus: DeviceStatus,
    val commandState: CommandState,
    val desiredRequestId: String?,
    val reportedRequestId: String?,
) {
    val isCommandPending: Boolean
        get() = commandState == CommandState.PENDING

    val acceptsPowerCommands: Boolean
        get() = reportedStatus.acceptsPowerCommands && !isCommandPending
}