package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import android.graphics.Bitmap
import androidx.preference.PreferenceManager
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.CreateMessageResponse
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.utils.UserData
import org.json.JSONArray
import org.json.JSONObject

class MMv1SendMessage(c: Context,
                      mb: MessageBuilder,
                      msgStartTS: Long,
                      bundleID: String? = null,
                      filenames: Array<String>? = null) :
    MMNetworkRepository<CreateMessageResponse, BaseResponse>(c, mb.category.domainId.toInt(), "message") {

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

    private val mDb = MMDB.instance(c)
    private val mMessageB = mb
    // Preferences
    private val mPref = PreferenceManager.getDefaultSharedPreferences(c)
    private val bShouldSaveLog = mPref.getBoolean(c.getString(R.string.mm_prefkey_should_log), true)

    private val mPayload = JSONObject()

    init {
        // Get the attributes filled by the user from the message creation
        mb.attributeValuesFromJson(mDb.getExtrasJSON(mb.messageId))

        // Payload for data
        mPayload.apply {
            put("typeid", mb.category.typeId)
            put("lat", mb.message.lat)
            put("lon", mb.message.lon)
            put("title", mb.title)
            put("description", mb.description)
            put("attribute_values", mb.attributeToJsonObject())

            put("reportStart", msgStartTS)
            put("reportSent", System.currentTimeMillis())

            put("via", "android")
            put("phone", phoneId)

            bundleID?.let { id -> put("bundle_token", id) }
            filenames?.let { fs ->
                val jsonArr = JSONArray()
                fs.forEach { f -> jsonArr.put(f) }
                put("expected_filenames", jsonArr)
            }
        }

        multipartFormAddString("data", mPayload.toString())
    }

    fun addSingleImageBitmap(bmp: Bitmap, filename: String, key: String = "picture") {
        multipartFormAddImage(key, filename, bmp, 50)
    }

    fun addParameter(key: String, value: Any) {
        mPayload.put(key, value)
        multipartFormAddString("data", mPayload.toString())
    }

    override fun getQueryParameters(): Map<String, String>? {
        val map = hashMapOf<String, String>()
        UserData.getUserCred(context)?.let { cred ->
            if (cred.isUserValid() && cred.domain?.id == mMessageB.category.domainId) {
                // same domain Id, put the credentials
                map["authorization"] = cred.token
            }
        }

        val apikey = if (BuildConfig.debug) MMConstants.OverridingApiKey_Test else MMConstants.OverridingApiKey
        if (!apikey.isEmpty()) {
            map["apikey"] = apikey
        }

        return map
    }

    override fun parseResponse(resp: BaseResponse): CreateMessageResponse {
        val json = JSONObject(resp.body)
        val data = json.optJSONObject("data")
        val success = json.optBoolean("success")

        if (data == null) {
            return CreateMessageResponse(-1, "", "", "")
        }

        // Try to get the new message id
        var newMsgID = data.optString("id")
        if (newMsgID.isNotEmpty()) {
            // Retrieve it from "links" object. This is for when the user is not logged in
            val links = data.optJSONObject("links")?.optJSONObject("self")?.optString("href")
            if (links != null && links.isNotEmpty()) {
                newMsgID = links.substring(links.lastIndexOf("/") + 1, links.length)
            }
        }

        // LOG: successful message upload
        if (bShouldSaveLog) {
            mDb.addLog(de.maengelmelder.mainmodule.objects.Log.TYPE_MSG_UPLOAD_SUCCESS,
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
                        ERR_MISSING_INVALID_ATTR -> context.getString(R.string.err_msgcreation_invalid_attr)
                        ERR_MISSING_INVALID_TYPE -> context.getString(R.string.err_msgcreation_invalid_type)
                        ERR_INVALID_JSON -> context.getString(R.string.err_msgcreation_invalid_json)
                        ERR_POSITION -> context.getString(R.string.err_msgcreation_invalid_pos)
                        ERR_AUTH -> context.getString(R.string.err_msgcreation_need_auth)
                        else -> errMsg
                    }
                }
            }
        }
        return BaseResponse(resp.code, errMsg)
    }
}