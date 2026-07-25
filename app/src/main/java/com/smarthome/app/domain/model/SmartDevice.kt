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
        val channels: List<SwitchChannel>,
    ) : DeviceConfiguration {
        val channelCount: Int
            get() = channels.size
    }

    data class SafetyOutlet(
        val maxOnDurationSeconds: Int,
    ) : DeviceConfiguration

    data class Light(
        val scheduleEnabled: Boolean = false,
        val startLocalTime: String = "18:00",
        val endLocalTime: String = "22:00",
        val timezone: String = "Asia/Colombo",
    ) : DeviceConfiguration

    data class Camera(
        val mediaUri: String,
    ) : DeviceConfiguration
}

data class SwitchChannel(
    val id: String,
    val name: String,
    val desiredStatus: PowerState,
    val reportedStatus: DeviceStatus,
    val requestId: String?,
) {
    val isPending: Boolean
        get() = reportedStatus.acceptsPowerCommands && desiredStatus.name != reportedStatus.name
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
