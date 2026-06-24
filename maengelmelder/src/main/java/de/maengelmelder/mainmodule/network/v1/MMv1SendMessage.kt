package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.preference.PreferenceManager
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.responses.*
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.utils.UserData
import de.maengelmelder.mainmodule.utils.images.PhotoSelector
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

/**
 * Created by christian on 28.08.17.
 *
 * The class utilizes the v1 MM API instead of the old implementation.
 * It is recommended to use this API as the old implementation has a problem on uploading image (and the old API is deprecated)
 *
 * Since 24.08.2020, this class doesn't attach the image in [MessageBuilder] automatically.
 * You will need to call [attachImage] or [addSingleImageBitmap] explicitly before executing the call
 */
@Deprecated(
        message = "AsyncTask is deprecated since SDK 30",
        replaceWith = ReplaceWith("coroutines.v1.MMv1SendMessage")
)
open class MMv1SendMessage(c: Context, mb: MessageBuilder, msgStartTS: Long,
                           bundleID: String? = null, filenames: Array<String>? = null)
    : MMv1Api<CreateMessageResponse, BaseResponse>(c, "message", mb.category.domainId) {

    companion object {
        val ERR_INVALID_JSON = "ERR1"
        val ERR_MISSING_INVALID_TYPE = "ERR2"
        val ERR_POSITION = "ERR3"
        val ERR_MISSING_INVALID_ATTR = "ERR4"
        val ERR_AUTH = "ERR5"
        val ERR_MISSING_BUNDLE_INFO = "ERR7"
        val ERR_BUNDLE_NOT_EXIST = "ERR8"
        val ERR_FILES_MISMATCH = "ERR9"
    }

    private var mMessageB = mb
    private var mTSStart = msgStartTS
    private var mDB = MMDB.instance(c)

    // Preferences
    private val mPref = PreferenceManager.getDefaultSharedPreferences(c)
    private val bShouldSaveLog = mPref.getBoolean(c.getString(R.string.mm_prefkey_should_log), true)

    init {
        // Get the attributes filled by the user from the message creation
        mb.attributeValuesFromJson(mDB.getExtrasJSON(mb.messageId))

        // Payload for data
        val jsonPayload = JSONObject()
        jsonPayload.put("typeid", mb.category.typeId)
        jsonPayload.put("lat", mb.message.lat)
        jsonPayload.put("lon", mb.message.lon)
        jsonPayload.put("title", mb.title)
        jsonPayload.put("description", mb.description)

        jsonPayload.put("reportStart", mTSStart)
        jsonPayload.put("reportSent", System.currentTimeMillis())
        jsonPayload.put("via", "android")
        jsonPayload.put("phone", phoneId)
        jsonPayload.put("attribute_values", extrasToJSONObject(mb.attributes))

        bundleID?.let { id ->
            jsonPayload.put("bundle_token", id)
        }
        filenames?.let { fs ->
            val jsonArr = JSONArray()
            fs.forEach { f -> jsonArr.put(f) }
            jsonPayload.put("expected_filenames", jsonArr)
        }

        addContent("data", jsonPayload.toString())
    }

    fun attachImage(key: String = "picture") {
        // Payload for image, if exists
        if (mMessageB.hasImage()) {
            val first = mMessageB.getImagePath(0)
            if (first.startsWith("content://")) {
                context?.let { c ->
                    val uri = Uri.parse(first)
                    addContent(c, key, PhotoSelector.getImagePathFromURI(c, uri), uri)
                }
            } else {
                val img = File(first)
                try {
                    addContent(key, img, 50)
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }
        } else if (MMConstants.BypassImageReq && mMessageB.category.photoReq == Category.PHOTO_REQ) {
            addContent(key, getBlankImage())
        }
    }

    fun addSingleImageBitmap(bmp: Bitmap, filename: String, key: String = "picture") {
        addContent(key, filename, bmp, 50)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        if (bShouldSaveLog) {
            mDB.addLog(de.maengelmelder.mainmodule.objects.Log.TYPE_MSG_UPLOAD,
                    hashMapOf(
                            de.maengelmelder.mainmodule.objects.Log.KEY_MSG_ID to mMessageB.messageId,
                            de.maengelmelder.mainmodule.objects.Log.KEY_TITLE to mMessageB.title
                    ))
        }
    }

    @Deprecated(
            message = "Moved to MessageBuilder",
            replaceWith = ReplaceWith("MessageBuilder.attributeToJsonObject")
    )
    private fun extrasToJSONObject(attrs: Map<String, Any?>): JSONObject {
        val obj = JSONObject()

        attrs.forEach { entry ->
            val id = entry.key

            val casted = when(entry.value) {
                is String -> entry.value.toString()
                is Int -> entry.value.toString().toInt()
                is Boolean -> entry.value.toString().toBoolean()
                else -> entry.value
            }

            val array = JSONArray()
            array.put(casted)
            obj.put(id, array)
        }

        return obj
    }

    override fun parseResponse(resp: BaseResponse): CreateMessageResponse {
        val json = JSONObject(resp.body)
        val data = json.optJSONObject("data")
        val success = json.optBoolean("success")

        var newMsgID = data.optString("id", null)
        if (newMsgID == null) {
            // Retrieve it from "links" object. This is for when the user is not logged in
            val links = data.optJSONObject("links").optJSONObject("self").optString("href")
            if (links != null) {
                newMsgID = links.substring(links.lastIndexOf("/") + 1, links.length)
            }
        }

        // LOG: successful message upload
        if (bShouldSaveLog) {
            mDB.addLog(de.maengelmelder.mainmodule.objects.Log.TYPE_MSG_UPLOAD_SUCCESS,
                    hashMapOf(
                            de.maengelmelder.mainmodule.objects.Log.KEY_MSG_ID to mMessageB.messageId,
                            de.maengelmelder.mainmodule.objects.Log.KEY_TITLE to mMessageB.title,
                            de.maengelmelder.mainmodule.objects.Log.KEY_REF_ID to newMsgID
                    ))
        }

        val domain = data.optString("domain")

        return CreateMessageResponse(if (success) 1 else -1, "", newMsgID, domain)
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        var error: JSONObject? = null
        try {
            error = JSONObject(resp.body)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (error == null) return resp
        var errMsg = error.optString("message", "")
        if (errMsg.isNotEmpty()) {
            val openBracket = errMsg.indexOf("[")
            val closingBracket = errMsg.indexOf("]")
            if (openBracket != -1 && closingBracket != -1 && closingBracket > openBracket) {
                errMsg.substring(openBracket+1, closingBracket).let { errCode ->
                    errMsg = when (errCode) {
                        ERR_MISSING_INVALID_ATTR -> context?.getString(R.string.err_msgcreation_invalid_attr)?: errMsg
                        ERR_MISSING_INVALID_TYPE -> context?.getString(R.string.err_msgcreation_invalid_type)?: errMsg
                        ERR_INVALID_JSON -> context?.getString(R.string.err_msgcreation_invalid_json)?: errMsg
                        ERR_POSITION -> context?.getString(R.string.err_msgcreation_invalid_pos)?: errMsg
                        ERR_AUTH -> context?.getString(R.string.err_msgcreation_need_auth)?: errMsg
                        else -> errMsg
                    }
                }
            }
        }
        return BaseResponse(resp.code, errMsg)
    }

    override fun getUrlParam(): Map<String, String?>? {
        val map = hashMapOf<String, String?>()
        context?.let { ctx ->
            UserData.getUserCred(ctx)?.let { cred ->
                if (cred.isUserValid() && cred.domain?.id == mMessageB.category.domainId) {
                    // same domain Id, put the credentials
                    map["authorization"] = cred.token
                }
            }
        }

        val apikey = if (BuildConfig.debug) MMConstants.OverridingApiKey_Test else MMConstants.OverridingApiKey
        if (!apikey.isEmpty()) {
            map["apikey"] = apikey
        }

        return map
    }

}