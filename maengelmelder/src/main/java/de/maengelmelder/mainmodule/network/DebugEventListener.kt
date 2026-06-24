package de.maengelmelder.mainmodule.network

import android.util.Log
import okhttp3.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * For debugging network request through API calls
 */
class DebugEventListener(debugSign: String = "MMAPI") : EventListener() {

    private var mCallStartNS = 0L
    private val mDebugSign = debugSign

    private fun getDifferenceMs(): Long {
        return (System.nanoTime() - mCallStartNS) / 1000000L
    }

    override fun callStart(call: Call) {
        mCallStartNS = System.nanoTime()
        Log.d(mDebugSign, "call started!")
    }

    override fun dnsStart(call: Call, domainName: String) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] dns started!")
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] dns ended!")
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] connect started!")
    }

    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] connect ended!")
    }

    override fun secureConnectStart(call: Call) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] secure connect started!")
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] secure connect ended!")
    }

    override fun requestBodyStart(call: Call) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] request body started!")
    }

    override fun requestBodyEnd(call: Call, byteCount: Long) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] request body ended!")
    }

    override fun responseBodyStart(call: Call) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] response body started!")
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] response body ended!")
    }

    override fun connectionAcquired(call: Call, connection: Connection) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] connection acquired!")
    }

    override fun connectionReleased(call: Call, connection: Connection) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] connection released!")
    }

    override fun requestHeadersStart(call: Call) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] request headers started!")
    }

    override fun requestHeadersEnd(call: Call, request: Request) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] request headers ended!")
    }

    override fun responseHeadersStart(call: Call) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] response headers started!")
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] response headers ended!")
    }

    override fun callEnd(call: Call) {
        Log.d(mDebugSign, "[${getDifferenceMs()}] call ended!")
    }
}