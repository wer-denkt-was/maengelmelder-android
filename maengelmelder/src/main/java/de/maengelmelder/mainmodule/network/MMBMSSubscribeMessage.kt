package de.maengelmelder.mainmodule.network

import android.content.Context
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import org.json.JSONObject

/**
 * Created by christian on 29.09.17.
 */
@Deprecated("Use MMv1SubscribeMessage instead!!")
class MMBMSSubscribeMessage(c: Context, email: String, msgId: String) : MMBMS<BaseResponse, BaseResponse>(c, "subscribe_message") {

    private val mEmail = email
    private val mID = msgId

    override fun parseResponse(resp: BaseResponse): BaseResponse {
        val json = JSONObject(resp.body)
        val code = json.optInt("result", -1)
        return BaseResponse(code, "")
    }

    override fun parseError(resp: BaseResponse): BaseResponse = resp

    override fun getUrlParam(): Map<String, String?>? {
        val map = hashMapOf("email" to mEmail, "id" to mID)
        userCred?.let { uc -> if (uc.token.isNotEmpty()) map["authorization"] = uc.token }
        return map
    }
}