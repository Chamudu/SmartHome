package com.smarthome.app.domain.model

enum class DeviceProfile(val displayName: String) {
    OUTLET("Outlet"),
    MULTI_SWITCH("Multi-switch"),
    SAFETY_OUTLET("Safety outlet"),
    LIGHT("Light"),
    CAMERA("Camera"),
}

sealed interface DeviceConfiguration {
    data object Outlet : DeviceConfiguration

    data class MultiSwitch(
        val channelCount: Int,
    ) : DeviceConfiguration

    data class SafetyOutlet(
        val maxOnDurationSeconds: Int,
    ) : DeviceConfiguration

    data class Light(
        val scheduleEnabled: Boolean = false,
    ) : DeviceConfiguration

    data class Camera(
        val mediaUri: String,
    ) : DeviceConfiguration
}

data class SmartDevice(
    val id: String,
    val name: String,
    val profile: DeviceProfile,
    val floorId: String,
    val roomId: String?,
    val column: Int,
    val row: Int,
    val desiredStatus: PowerState,
    val reportedStatus: DeviceStatus,
    val commandState: CommandState,
    val configuration: DeviceConfiguration,
)

data class NewDevice(
    val name: String,
    val profile: DeviceProfile,
    val floorId: String,
    val roomId: String?,
    val column: Int,
    val row: Int,
    val channelCount: Int = 2,
    val maxOnDurationSeconds: Int = 900,
    val mediaUri: String = "https://placehold.co/640x360",
)
