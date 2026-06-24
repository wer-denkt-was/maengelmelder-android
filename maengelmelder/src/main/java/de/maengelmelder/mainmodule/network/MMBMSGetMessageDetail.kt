package de.maengelmelder.mainmodule.network

import android.content.Context
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.MessageDetailErrorResponse
import de.maengelmelder.mainmodule.objects.MessageDetail
import de.maengelmelder.mainmodule.objects.MessageHistory
import de.maengelmelder.mainmodule.objects.MessageImageUri
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by christian on 11.09.17.
 */
@Deprecated("Use MMv1MessageDetail instead!")
class MMBMSGetMessageDetail(c: Context, id: String) : MMBMS<MessageDetail, MessageDetailErrorResponse>(c, "get_message_detail") {

    private val mId = id

    override fun parseResponse(resp: BaseResponse): MessageDetail {
        val md = MessageDetail()
        val json = JSONObject(resp.body)

        val firstImage = MessageImageUri().apply {
            originalUri = json.optString("pictureUrl")
            thumbnailUri = originalUri
        }
        md.images = listOf(firstImage)
        md.id = json.optString("messageid")
        md.lat = json.optDouble("lat", Double.MAX_VALUE)
        md.lon = json.optDouble("lon", Double.MAX_VALUE)


        val detail = json.optJSONArray("details")
        var i = 0
        val history = arrayListOf<MessageHistory>()
        while (i < (detail?.length()?: 0) - 1) {
            val key = detail.get(i) as String
            val value = detail.get(i + 1) as String
            val msgHist = MessageHistory().apply {
                text = key
                manualText = value
            }
            history.add(msgHist)
            i += 2
        }
        md.details = history

        return md
    }

    override fun parseError(resp: BaseResponse): MessageDetailErrorResponse {
        val json = try { JSONObject(resp.body) } catch (e: JSONException) { null }
        return MessageDetailErrorResponse(
                json?.optString("error")?: "",
                json?.optString("message", "")?: ""
        )
    }

    override fun getUrlParam(): Map<String, String?>? {
        val map = hashMapOf("id" to mId, "appid" to appId)
        userCred?.let { uc -> if (uc.token.isNotEmpty()) map["authorization"] = uc.token }
        return map
    }
}