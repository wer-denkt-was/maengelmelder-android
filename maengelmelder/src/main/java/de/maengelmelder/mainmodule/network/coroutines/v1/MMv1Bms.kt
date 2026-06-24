package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.BmsDomain
import org.json.JSONObject
import java.lang.Exception

class MMv1Bms(c: Context, domId: Int, private val fetchCategory: Boolean = true) : MMNetworkRepository<BmsDomain?, BaseResponse>(c, domId, "bms") {

    init {
        // Don't use bearer token
        addBearerTokenWhenAvailable = false
    }

    override fun getQueryParameters(): Map<String, String>? {
        return if (fetchCategory) {
            null
        } else {
            mapOf("category_fieldset" to "wizard")
        }
    }

    override fun parseResponse(resp: BaseResponse): BmsDomain? {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return null

        val data = json.optJSONObject("data")
        return if (data != null) BmsDomain.fromJSON(data) else null
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return BaseResponse(resp.code, "")

        return BaseResponse(resp.code, json.optString("message"))
    }
}