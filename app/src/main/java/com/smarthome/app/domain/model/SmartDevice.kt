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
        val mediaType: String = "SNAPSHOT",
        val mediaUri: String,
        val capturedAtMillis: Long? = null,
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
    val mediaUri: String = "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=640&q=80",
)

enum class CameraConnectivity(val displayName: String) {
    ONLINE("ONLINE"),
    OFFLINE("OFFLINE"),
    ERROR("ERROR"),
}

fun DeviceStatus.toCameraConnectivity(): CameraConnectivity = when (this) {
    DeviceStatus.ON, DeviceStatus.OFF -> CameraConnectivity.ONLINE
    DeviceStatus.ERROR -> CameraConnectivity.ERROR
    DeviceStatus.DISCONNECTED -> CameraConnectivity.OFFLINE
}
