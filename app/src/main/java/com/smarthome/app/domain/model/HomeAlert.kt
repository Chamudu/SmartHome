package com.smarthome.app.domain.model

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL,
}

data class HomeAlert(
    val id: String,
    val deviceId: String,
    val type: String,
    val severity: AlertSeverity,
    val message: String,
    val createdAtMillis: Long,
)
