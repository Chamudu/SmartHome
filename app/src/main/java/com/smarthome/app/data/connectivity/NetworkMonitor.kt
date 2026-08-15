package com.smarthome.app.data.connectivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Reports whether the device currently has network connectivity for Firestore operations. */
interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}

/**
 * Fallback used before an application registers a real monitor and in JVM tests, so consumers never
 * observe a null monitor.
 */
object AlwaysOnlineNetworkMonitor : NetworkMonitor {
    private val online = MutableStateFlow(true)

    override val isOnline: StateFlow<Boolean> = online.asStateFlow()
}