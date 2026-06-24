package de.maengelmelder.mainmodule.service.util

import android.content.Context
import android.content.Intent

/**
 * Simple object for broadcasting message from services / background tasks to UI
 */
object Broadcaster {

    /**
     * broadcast a message
     * @param c context
     * @param filter the broadcast filter
     * @param data additional data broadcasted. Put empty map if you don't use any
     */
    fun cast(c: Context, filter: String, data: Map<String, String>) {
        val bcast = Intent(filter)
        data.forEach { entry -> bcast.putExtra(entry.key, entry.value) }
        c.sendBroadcast(bcast)
    }

}