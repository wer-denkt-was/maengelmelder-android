package de.maengelmelder.mainmodule.network.coroutines

import com.facebook.stetho.okhttp3.StethoInterceptor
import de.maengelmelder.mainmodule.network.DebugEventListener
import de.maengelmelder.mainmodule.network.MMAPI
import okhttp3.ConnectionPool
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Default OKHTTP client
 */
object MMOkHttpClient {
    /**
     * Request is aborted, particularly due to [cancel] call.
     */
    val RESPSTATUS_ABORTED              = -2

    /**
     * Failed to connect (No internet connection)
     */
    val RESPSTATUS_CONNECTION_FAILED    = -100

    /**
     * Endpoint is unknown or has expired. Ask server admin about this error
     */
    val RESPSTATUS_IOEXC                = -101

    /**
     * The task is already executed by [execute] call
     */
    val RESPSTATUS_ALREADY_EXEC         = -102

    /**
     * Timeout, probably due to slow internet connection
     */
    val RESPSTATUS_TIMEOUT              = -103

    /**
     * Timeout, probably due to slow internet connection
     */
    val RESPSTATUS_INVALID_CERT         = -104

    /**
     * Default builder for OkHttpClient
     */
    fun generateDefaultBuilder(debugging: Boolean = false,
                                       debugCallsign: String? = null): OkHttpClient.Builder {
        val b = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                // No timeout for read/write
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                // No redirections
                .followRedirects(false)
                .followSslRedirects(false)
                // basic connection pool that keeps max 5 connections and 5 mins timeout
                .connectionPool(ConnectionPool())
                // Use modern TLS wherever possible
                .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                // Use 2.0, then fallback to 1.1 when not available
                .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))

        if (debugging) {
            b.addNetworkInterceptor(StethoInterceptor())
                    .eventListener(DebugEventListener(debugCallsign?: "MMAPI"))
        }

        return b
    }
}