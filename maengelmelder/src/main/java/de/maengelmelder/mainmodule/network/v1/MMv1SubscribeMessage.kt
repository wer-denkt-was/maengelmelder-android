package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.MessageSubsResponse
import org.json.JSONObject

/**
 * api/v1/domain/{domainid}/message/{messageid}/subscribe - POST
 * email inside body
 */
@Deprecated(
        message = "AsyncTask is deprecated since SDK 30",
        replaceWith = ReplaceWith("coroutines.v1.MMv1SubscribeMessage")
)
class MMv1SubscribeMessage(c: Context, val messageId: String, val email: String, val domainid: String) :
        MMv1Api<MessageSubsResponse, BaseResponse>(c, "message/$messageId/subscribe", domainid) {

    init {
        addContent("email", email)
    }

    override fun parseResponse(resp: BaseResponse): MessageSubsResponse {
        var msgId = ""
        var email = ""
        var userId = ""
        try { JSONObject(resp.body) } catch (e: Exception) { null }?.let { json ->
            json.optJSONObject("data")?.let { data ->
                msgId = data.optString("message-id", "")
                email = data.optString("email", "")
                userId = data.optString("userid", "")
            }
        }
        return MessageSubsResponse(true, msgId, email, userId)
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val err = try { JSONObject(resp.body) } catch (e: Exception) { null }?.let { json ->
            val msg = json.optJSONObject("data")?.optString("message")
            BaseResponse(resp.code, msg?: "")
        }
        return err?: resp
    }

    override fun getUrlParam(): Map<String, String?>? = null
}