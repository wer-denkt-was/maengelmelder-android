package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Category
import org.json.JSONObject
import java.lang.Exception

/**
 * api/v1/bmsapp/<appid>/category/<catid>
 */
class MMv1BmsAppCategory (c: Context, val id: Long) : MMNetworkRepository<Category?, BaseResponse>(c, -1, "category") {

    override fun getUrl(): String {
        val url = StringBuilder()
        val domainName = if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
        url.append(domainName)
        url.append(MMConstants.V1ApiPath)
        url.append("${MMConstants.BmsAppApiPath}/$appId/$methodName/$id")
        return url.toString()
    }

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