package de.maengelmelder.mainmodule.network

import android.content.Context
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.responses.*
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.*

/**
 * Created by christian on 28.08.17.
 */
@Deprecated("use MMv1SendMessage instead!")
open class MMBMSSendMessage(c: Context, mb: MessageBuilder, msgStartTS: Long)
    : MMBMS<CreateMessageResponse, BaseResponse>(c, "create_message") {

    private var mMessageB = mb
    private var mTSStart = msgStartTS

    init {
        if (mb.hasImage()) {
            val img = File(mb.getImagePath(0))
            addContent("picture", img)
        } else if (MMConstants.BypassImageReq && mb.category.photoReq == Category.PHOTO_REQ) {
            addContent("picture", getBlankImage())
        }
    }

    override fun parseResponse(resp: BaseResponse): CreateMessageResponse {
        val json = JSONObject(resp.body)
        val code = json.optInt("result")
        val msg = json.optString("message")
        val domId = json.optInt("domainid", -1)
        val msgId = json.optString("id")

        return CreateMessageResponse(code, msg, msgId, domId.toString())
    }

    override fun parseError(resp: BaseResponse): BaseResponse = resp

    override fun getUrlParam(): Map<String, String?>? {
        val cal = Calendar.getInstance()
        cal.time = Date()
        val map = hashMapOf(
                "domainid"      to mMessageB.category.domainId,
                "description"   to mMessageB.description,
                "type"          to mMessageB.category.name,
                "typeid"        to mMessageB.category.typeId.toString(),
                "lat"           to mMessageB.getLocation().second.toString(),
                "long"          to mMessageB.getLocation().first.toString(),
                "answers"       to mMessageB.getAttributesAsJson().toString(),
                "reportStart"   to mTSStart.toString(),
                "reportSent"    to cal.timeInMillis.toString(),
                "via"           to "android",
                "adr"           to mMessageB.message.street
        )
        val apikey = if (BuildConfig.debug) MMConstants.OverridingApiKey_Test else MMConstants.OverridingApiKey
        if (!apikey.isEmpty()) {
            map["apikey"] = apikey
        }
        if (mMessageB.category.hasTitle) {
            map["title"] = mMessageB.title
        }

        userCred?.let { uc -> if (uc.token.isNotEmpty()) map["authorization"] = uc.token }

        return map
    }

}