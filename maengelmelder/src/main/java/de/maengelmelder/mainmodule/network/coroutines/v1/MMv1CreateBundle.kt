package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import org.json.JSONObject
import java.lang.Exception

class MMv1CreateBundle(c: Context, domId: Int) :
    MMNetworkRepository<String, BaseResponse>(c, domId, "bundle") {

    init {
        val jsonPayload = JSONObject().apply {
            put("phone", phoneId)
        }
        setJsonBody(jsonPayload.toString())
    }

    override fun getQueryParameters(): Map<String, String>? = null

    override fun parseResponse(resp: BaseResponse): String {
        var token = ""
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return token
        json.optJSONObject("data")?.let { data ->
            token = data.optString("token", "")
        }
        return token
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return BaseResponse(-1, "")
        return BaseResponse(resp.code, json.optString("message"))
    }
}