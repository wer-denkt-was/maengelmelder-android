package de.maengelmelder.mainmodule.network.utils

import android.content.Context
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.utils.UserData
import java.net.URLEncoder

object NetworkUtil {

    /**
     * Encodes [Map] into query parameter. It already adds "?" at the beginning
     */
    fun paramFromMap(map: Map<String, String?>): String {
        val param = StringBuilder()
        param.append("?")
        val it = map.iterator()

        while (it.hasNext()) {
            val entry = it.next()
            param.append(entry.key)
            param.append("=")
            param.append(URLEncoder.encode(entry.value, "utf-8"))
            if (it.hasNext()) param.append("&")
        }

        return param.toString()
    }

    /**
     * returns string for Bearer authentication
     */
    fun getBearerAuthentication(c: Context): String? {
        return UserData.getForBearerAuth(c)
    }

    /**
     * Returns default basic auth
     */
    fun getBasicAuthentication(c: Context): String? {
        return UserData.getForBasicAuth(c, BuildConfig.debug)
    }

}