package com.smarthome.app.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks internet connectivity through [ConnectivityManager] and exposes it as a [StateFlow] so the
 * ViewModel can pause command retries while offline and flush queued commands on reconnection.
 */
class ConnectivityNetworkMonitor(
    context: Context,
) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val mutableIsOnline = MutableStateFlow(isCurrentlyOnline())

    override val isOnline: StateFlow<Boolean> = mutableIsOnline.asStateFlow()

    private val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            mutableIsOnline.value = true
        }

        override fun onLost(network: Network) {
            mutableIsOnline.value = isCurrentlyOnline()
        }

        override fun onUnavailable() {
            mutableIsOnline.value = false
        }
    }

    init {
        connectivityManager.registerNetworkCallback(request, callback)
    }

    fun close() {
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
    }

    private fun isCurrentlyOnline(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(
            connectivityManager.activeNetwork,
        )
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}