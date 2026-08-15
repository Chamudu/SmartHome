package com.smarthome.app

import android.app.Application
import com.smarthome.app.data.connectivity.AlwaysOnlineNetworkMonitor
import com.smarthome.app.data.connectivity.ConnectivityNetworkMonitor
import com.smarthome.app.data.connectivity.NetworkMonitor

/**
 * Registers a device-wide network monitor so repositories and the ViewModel can react to internet
 * loss without holding a [android.content.Context] themselves.
 */
class SmartHomeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        networkMonitor = ConnectivityNetworkMonitor(this)
    }

    companion object {
        @Volatile
        var networkMonitor: NetworkMonitor = AlwaysOnlineNetworkMonitor
            private set
    }
}