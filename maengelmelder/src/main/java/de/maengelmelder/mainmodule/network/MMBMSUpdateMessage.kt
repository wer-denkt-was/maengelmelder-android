package de.maengelmelder.mainmodule.network

import android.content.Context
import android.net.Uri
import android.util.Log
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.MessageUpdateResponse
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder

/**
 * Created by christian on 25.09.17.
 */
@Deprecated(message = "Use MMv1 counterpart instead")
open class MMBMSUpdateMessage(c: Context, msgId: String, text: String, solved: Boolean, imgFile: File? = null)
    : MMBMS<MessageUpdateResponse, BaseResponse>(c, "update_message") {

    private val mId = msgId
    private val mText = text
    private val bSolved = solved
    private val mImg = imgFile

    private var textSuccess = c.getString(R.string.update_msg_success)
    private var textNotExists = c.getString(R.string.update_msg_not_exist)
    private var textNotAllowed = c.getString(R.string.update_msg_not_allowed)
    private var textNoContent = c.getString(R.string.update_msg_no_text)

    init {
        if (mImg != null) {
            addContent("picture", mImg)
        }
    }

    override fun parseResponse(resp: BaseResponse): MessageUpdateResponse {
        var json: JSONObject? = null
        try {
            json = JSONObject(resp.body)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val code = json?.optInt("result", -1)?: -1
        val msgId = json?.optString("messageId", "")?: ""
        val msg = when (code) {
            1 -> textSuccess
            2 -> textNotExists
            3 -> textNotAllowed
            5 -> textNoContent
            else -> ""
        }

        return MessageUpdateResponse(code, msgId, mImg?.path, msg)
    }

    override fun parseError(resp: BaseResponse): BaseResponse = resp

    override fun getUrlParam(): Map<String, String?>? {
        val map = hashMapOf(
                "appid" to appId,
                "id" to mId,
                "text" to mText,
                "solved" to if (bSolved) "1" else "0"
        )
        userCred?.let { uc -> if (uc.token.isNotEmpty()) map["authorization"] = uc.token }
        return map
    }
}