package de.maengelmelder.mainmodule.objects

import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.objects.interfaces.IdGenerable
import org.json.JSONObject
import java.io.Serializable
import java.lang.Exception

/**
 * Class that holds information about MM-system, internal or external.
 *
 * - Refer to entries in table [de.maengelmelder.mainmodule.database.MMDBConstants.TBL_DOMAINS]
 * - The id may also refer to [de.maengelmelder.mainmodule.database.MMDBConstants.COL_SYSTEM_ID] in any tables
 */
class SystemInfo: IdGenerable, Serializable {

    companion object {
        fun getDefaultSystemInfo(): SystemInfo {
            return SystemInfo().apply {
                appId = "1"
                domainName = if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
            }
        }
    }

    var appId = ""
    var title = ""
    var domainName = ""
    var isExternal = false

    override fun generateId(): String = "$appId-$domainName"

    override fun toString(): String {
        return "System[appId:$appId][domain:$domainName][external:$isExternal]"
    }

}