package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import android.net.Uri
import de.maengelmelder.mainmodule.network.responses.*
import de.maengelmelder.mainmodule.utils.images.PhotoSelector
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Created by christian on 28.08.17.
 *
 * api/v1/domain/<domainid>/message/<messageid>/update
 *
 * - imagePath can be path to image or [Uri]
 */
@Deprecated(
        message = "AsyncTask is deprecated since SDK 30",
        replaceWith = ReplaceWith("coroutines.v1.MMv1UpdateMessage")
)
open class MMv1UpdateMessage(private val c: Context,
                             domId: String,
                             private val msgId: String,
                             commentText: String,
                             private val imagePath: String? = null,
                             solved: Boolean = false,
                             attributeValues: Map<String, Any?>? = null)
    : MMv1Api<MessageUpdateResponse, MessageUpdateResponse>(c, "update", domId) {

    companion object {
        val SUCCESS = 200
        val ERR_SERVER = -1
        val ERR_INVALID_AUTH = -2
    }

    init {
        addContent("data", JSONObject().apply {
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

        if (imagePath != null && imagePath.isNotEmpty()) {
            try {
                if (imagePath.startsWith("content://")) {
                    val uri = Uri.parse(imagePath)
                    val filename = PhotoSelector.getImagePathFromURI(c, uri)?: "images/image.jpg"
                    addContent(c, "picture", filename.substring(filename.lastIndexOf("/")), uri)
                } else {
                    addContent("picture", File(imagePath), 100)
                }
            } catch (e: IllegalArgumentException) {
                // Not an image. Ignoring...
            }
        }
    }

    override fun parseResponse(resp: BaseResponse): MessageUpdateResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) {
            return MessageUpdateResponse(ERR_SERVER, msgId, null, "")
        } else {
            if (json.optBoolean("success", false)) {
                return MessageUpdateResponse(SUCCESS, msgId, imagePath, "")
            } else {
                return parseError(resp)
            }
        }
    }

    override fun parseError(resp: BaseResponse): MessageUpdateResponse {
        val error = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (error == null) {
            return MessageUpdateResponse(ERR_SERVER, msgId, null, "")
        } else {
            val msg = error.optString("message", "")
            val openBrace = msg.indexOf("[")
            val closeBrace = msg.indexOf("]")
            val errorCode = if (openBrace > -1 && closeBrace > -1 && closeBrace > openBrace) {
                try { msg.substring(openBrace+1, closeBrace).removePrefix("ERR").toInt() } catch (e: Exception) { ERR_SERVER }
            } else if (msg == "authentication token invalid") {
                ERR_INVALID_AUTH
            } else {
                ERR_SERVER
            }

            return MessageUpdateResponse(errorCode, msgId, null, msg)
        }
    }

    override fun getUrlParam(): Map<String, String?>? {
        if (userCred != null && userCred?.isUserValid() == true) {
            return mapOf("authorization" to userCred?.token)
        }
        return null
    }

}