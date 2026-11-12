package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.SystemInfo
import org.json.JSONObject
import java.lang.Exception

/**
 * api/v1/bmsapp/<appid>/category/<catid>
 *
 * If [externalSystemInfo] is set, the category is requested from that system
 */
class MMv1BmsAppCategory (c: Context, val id: Long) : MMNetworkRepository<Category?, BaseResponse>(c, -1, "category") {

    companion object {
        /**
         * Builds the request URL. An external system overrides the default host and, unless
         * [forceOverriddenAppId] is set, the app id
         */
        internal fun buildUrl(defaultHost: String, appId: String, forceOverriddenAppId: Boolean,
                              system: SystemInfo?, methodName: String, id: Long): String {
            val host = system?.domainName?.takeIf { it.isNotEmpty() } ?: defaultHost
            val currentAppId = if (forceOverriddenAppId) appId
                else system?.appId?.takeIf { it.isNotEmpty() } ?: appId
            return "$host${MMConstants.V1ApiPath}${MMConstants.BmsAppApiPath}/$currentAppId/$methodName/$id"
        }
    }

    override fun getUrl(): String = buildUrl(
        if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl,
        appId, MMConstants.ForceUseOverriddenAppId, externalSystemInfo, methodName, id)

    override fun getQueryParameters(): Map<String, String>? = mapOf("fieldset" to "all")

    override fun parseResponse(resp: BaseResponse): Category? {
        val jsonString = try {
            JSONObject(resp.body)
        } catch (e: Exception) {
            null
        }

        var category: Category? = null
        jsonString?.let { body ->
            body.optJSONObject("data")?.let { data ->
                category = Category.fromJSON(data)
            }
        }
        return category
    }

    override fun parseError(resp: BaseResponse): BaseResponse = resp
}