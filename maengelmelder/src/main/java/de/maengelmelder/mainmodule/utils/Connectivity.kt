package de.maengelmelder.mainmodule.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.NetworkRequest


/**
 *
 * ## Overview
 * Used to get information of the current network condition (status, SSID, type, etc.)
 */
internal object Connectivity {

    /**
     *
     * Gets the type of the network the device is currently connected to
     *
     * @param c Context
     * @return [NetworkInfo] object of the currently connected network
     */
    @Deprecated(message = "Class NetworkInfo itself is deprecated")
    fun getNetworkType(c: Context): NetworkInfo? {
        val cm = c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo
    }

    /**
     * Returns currently active network
     */
    private fun getActiveNetwork(c: Context): Network? {
        val conManager = c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return conManager.activeNetwork
    }

    /**
     * Check whether the device has active wifi / cellular connection
     */
    fun hasActiveConnection(c: Context) : Boolean {
        val conManager = c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNet = getActiveNetwork(c)
        val activeNetCap = conManager.getNetworkCapabilities(activeNet)
        return activeNetCap?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                || activeNetCap?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    }

    /**
     * Enables monitoring connection
     */
    fun enableConnectivityMonitor(c: Context, cb: ConnectivityManager.NetworkCallback) {
        val conManager = c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        conManager.registerNetworkCallback(
                NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build(),
                cb
        )
    }

    /**
     * Disables connection monitoring
     */
    fun disableConnectivityMonitor(c: Context, cb: ConnectivityManager.NetworkCallback) {
        val conManager = c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        conManager.unregisterNetworkCallback(cb)
    }

    /**
     * Basic network callback that checks how many active connections the device has (WIFI / CELLULAR).
     */
    fun getNetworkCallback(listener: NetworkAvailability?) : ConnectivityManager.NetworkCallback{
        val networkCallback: ConnectivityManager.NetworkCallback = object : ConnectivityManager.NetworkCallback() {

            /**
             * Map of [Network] -> [NetworkCapabilities]
             */
            private val mConnectionMap: HashMap<Network, NetworkCapabilities> = hashMapOf()

            fun connectionSize(): Int {
                return mConnectionMap.size
            }
            override fun onUnavailable() {
                listener?.onAvailabilityChanged(0)
            }
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                mConnectionMap[network] = networkCapabilities
                listener?.onAvailabilityChanged(connectionSize())
            }
            override fun onLost(network: Network) {
                mConnectionMap.remove(network)
                listener?.onAvailabilityChanged(connectionSize())
            }
        }

        return networkCallback
    }

    interface NetworkAvailability {
        fun onAvailabilityChanged(activeNetworkCount: Int)
    }

}