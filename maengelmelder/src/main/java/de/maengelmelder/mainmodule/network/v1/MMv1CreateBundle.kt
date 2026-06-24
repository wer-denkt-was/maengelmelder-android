package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import org.json.JSONObject
import java.lang.Exception

/**
 * POST api/v1/domain/<domainid>/bundle
 */
@Deprecated(
        message = "AsyncTask is deprecated since SDK 30",
        replaceWith = ReplaceWith("coroutines.v1.MMv1Duplicates")
)
class MMv1CreateBundle(c: Context, domId: String, phone: String? = null) : MMv1Api<String, BaseResponse>(c, "bundle", domId) {

    companion object {
        const val RESP_NO_DATA = -1
    }

    private val mPhone = phone

    init {
        val jsonPayload = JSONObject()
        mPhone?.let { jsonPayload.put("phone", it) }
        setRawJsonBody(jsonPayload.toString())
    }

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
        if (json == null) return BaseResponse(RESP_NO_DATA, "")
        return BaseResponse(resp.code, json.optString("message"))
    }

    override fun getUrlParam(): Map<String, String?>? = null
}