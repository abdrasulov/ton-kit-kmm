package io.horizontalsystems.tonkit

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

actual class ConnectionManager(context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnectedFlow = MutableStateFlow(calculateIsConnected())
    actual val isConnectedFlow: StateFlow<Boolean>
        get() = _isConnectedFlow.asStateFlow()

    private var callback = ConnectionStatusCallback()

    actual fun start() {
        refreshState()
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
    }

    actual fun stop() {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            //already unregistered
        }
    }

    private fun refreshState() {
        _isConnectedFlow.update { calculateIsConnected() }
    }

    private fun calculateIsConnected(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities != null &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    inner class ConnectionStatusCallback : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            if (connectivityManager.activeNetwork == null || connectivityManager.activeNetwork == network) {
                refreshState()
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            if (connectivityManager.activeNetwork == network) {
                refreshState()
            }
        }

        override fun onAvailable(network: Network) {
            if (connectivityManager.activeNetwork == network) {
                refreshState()
            }
        }
    }
}
