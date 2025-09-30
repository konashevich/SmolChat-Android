package io.shubham0204.smollmandroid.subscription

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

@Single
class NetworkStatusMonitor(context: Context) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isOnline = MutableStateFlow(checkNow())
    val isOnline: StateFlow<Boolean> = _isOnline

    private fun checkNow(): Boolean {
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private val cb = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { _isOnline.value = checkNow() }
        override fun onLost(network: Network) { _isOnline.value = checkNow() }
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) { _isOnline.value = checkNow() }
    }

    init { cm.registerDefaultNetworkCallback(cb) }
}
