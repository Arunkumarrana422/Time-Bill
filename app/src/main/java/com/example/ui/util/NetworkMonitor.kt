package com.example.ui.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * Fast and accurate internet reachability probe.
 * When a SIM card has no recharge / data pack, cellular carriers typically:
 * 1. Drop TCP packets (timeout)
 * 2. Or intercept HTTP requests and redirect (HTTP 302/301) to a recharge portal page.
 *
 * By calling Google's generate_204 endpoint with instanceFollowRedirects = false,
 * an uncharged SIM will return 302 (redirect to recharge portal) or fail/timeout,
 * correctly proving there is NO real internet connection!
 */
suspend fun checkActualInternetAccess(): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://connectivitycheck.gstatic.com/generate_204")
        val connection = url.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 2500
        connection.readTimeout = 2500
        connection.requestMethod = "GET"
        connection.useCaches = false
        val responseCode = connection.responseCode
        connection.disconnect()
        // HTTP 204 No Content is only returned when real, open internet is accessible
        if (responseCode == 204) {
            return@withContext true
        }
    } catch (e: Exception) {
        // Fallback to secondary probe
    }

    try {
        // Secondary backup check using raw socket to Google DNS (8.8.8.8:53)
        Socket().use { socket ->
            socket.connect(InetSocketAddress("8.8.8.8", 53), 2000)
            true
        }
    } catch (e: Exception) {
        false
    }
}

/**
 * Check initial network status based on Android OS capabilities.
 * NET_CAPABILITY_VALIDATED is false when a SIM has no data balance / recharge.
 */
fun isInitialNetworkConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    return hasInternet && isValidated
}

fun observeNetworkConnectivity(context: Context): Flow<Boolean> = callbackFlow {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun hasBasicCapabilities(network: Network? = connectivityManager.activeNetwork): Boolean {
        if (network == null) return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return hasInternet && isValidated
    }

    // Emit initial status based on validation check
    val initialBasic = hasBasicCapabilities()
    if (!initialBasic) {
        trySend(false)
    } else {
        launch(Dispatchers.IO) {
            val real = checkActualInternetAccess()
            trySend(real)
        }
    }

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            launch(Dispatchers.IO) {
                if (hasBasicCapabilities(network)) {
                    val real = checkActualInternetAccess()
                    trySend(real)
                } else {
                    trySend(false)
                }
            }
        }

        override fun onLost(network: Network) {
            trySend(false)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (!hasInternet || !isValidated) {
                trySend(false)
            } else {
                launch(Dispatchers.IO) {
                    val real = checkActualInternetAccess()
                    trySend(real)
                }
            }
        }
    }

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    try {
        connectivityManager.registerNetworkCallback(request, callback)
    } catch (e: Exception) {
        // Fallback if permission or system issue
    }

    awaitClose {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            // ignore
        }
    }
}.distinctUntilChanged()

