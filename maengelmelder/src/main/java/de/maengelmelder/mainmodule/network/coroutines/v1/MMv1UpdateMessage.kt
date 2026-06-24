package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import android.net.Uri
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.MessageUpdateResponse
import de.maengelmelder.mainmodule.utils.UserData
import de.maengelmelder.mainmodule.utils.images.PhotoSelector
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MMv1UpdateMessage(private val c: Context,
                        domId: Int,
                        private val msgId: String,
                        commentText: String,
                        private val imagePath: String? = null,
                        solved: Boolean = false,
                        attributeValues: Map<String, Any?>? = null) :
    MMNetworkRepository<MessageUpdateResponse, MessageUpdateResponse>(c, domId, "update") {

    init {
        multipartFormAddString("data", JSONObject().apply {
            put("messageid", msgId)
            put("text", commentText)
            put("solved", if (solved) 1 else 0)
            put("phone", phoneId)

            if (attributeValues != null) {
                val attr = JSONObject()
                for ((key, value) in attributeValues) {
                    attr.putOpt(key, JSONArray(arrayOf(value)))
                }
                put("attribute_values", attr)
            }

        }.toString())

        if (!imagePath.isNullOrEmpty()) {
            try {
                if (imagePath.startsWith("content://")) {
                    val uri = Uri.parse(imagePath)
                    val filename = PhotoSelector.getImagePathFromURI(c, uri)?: "images/image.jpg"
                    multipartFormAddUri(c, "picture", filename.substring(filename.lastIndexOf("/")), uri)
                } else {
                    multipartFormAddImage("picture", File(imagePath), 100)
                }
            } catch (e: IllegalArgumentException) {
                // Not an image. Ignoring...
            }
        }
    }

    override fun getQueryParameters(): Map<String, String>? {
        val userCred = UserData.getUserCred(context)
        if (userCred != null && userCred.isUserValid() && addBearerTokenWhenAvailable) {
            return mapOf("authorization" to userCred.token)
        }
        return null
    }

    override fun parseResponse(resp: BaseResponse): MessageUpdateResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) {
            return MessageUpdateResponse(-1, msgId, null, "")
        } else {
            if (json.optBoolean("success", false)) {
                return MessageUpdateResponse(200, msgId, imagePath, "")
            } else {
                return parseError(resp)
            }
        }
    }

    override fun parseError(resp: BaseResponse): MessageUpdateResponse {
        val error = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (error == null) {
            return MessageUpdateResponse(-1, msgId, null, "")
        } else {
            val msg = error.optString("message", "")
            val openBrace = msg.indexOf("[")
            val closeBrace = msg.indexOf("]")
            val errorCode = if (openBrace > -1 && closeBrace > -1 && closeBrace > openBrace) {
                try { msg.substring(openBrace+1, closeBrace).removePrefix("ERR").toInt() }
                catch (e: Exception) { -1 }
            } else if (msg == "authentication token invalid") {
                -2
            } else {
                -1
            }

            return MessageUpdateResponse(errorCode, msgId, null, msg)
        }
    }
}