package com.example.lightweightyoutube.data.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Checks if the device currently has an active network connection.
 *
 * This function accesses [ConnectivityManager] to determine if there's an active network
 * and if that network has capabilities for internet transport (Wi-Fi, Cellular, Ethernet, Bluetooth).
 *
 * @param context The [Context] used to access system services.
 *                It's recommended to use application context to avoid leaks if stored.
 * @return `true` if a network connection is available, `false` otherwise.
 */
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Get the currently active network.
    val network = connectivityManager.activeNetwork ?: return false

    // Get the capabilities of the active network.
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

    // Check if the active network has one of the common internet transport types.
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true // For internet over Bluetooth
        else -> false
    }
}
