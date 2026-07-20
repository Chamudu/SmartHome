package com.smarthome.app.domain.model

/** The last operational state confirmed by a device. */
enum class DeviceStatus {
    ON,
    OFF,
    ERROR,
    DISCONNECTED,
    ;

    val acceptsPowerCommands: Boolean
        get() = this == ON || this == OFF
}
