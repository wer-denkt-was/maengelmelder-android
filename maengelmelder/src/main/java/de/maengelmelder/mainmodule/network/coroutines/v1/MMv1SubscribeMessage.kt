package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.MessageSubsResponse
import org.json.JSONObject

class MMv1SubscribeMessage(c: Context, val messageId: String, val email: String, val domainid: Int)
    : MMNetworkRepository<MessageSubsResponse, BaseResponse>(c, domainid, "message/$messageId/subscribe"){

    init {
        multipartFormAddString("email", email)
    }

    override fun getQueryParameters(): Map<String, String>? = null

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
}