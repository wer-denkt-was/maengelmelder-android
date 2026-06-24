package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.responses.*
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.MessageDetail
import de.maengelmelder.mainmodule.objects.MessageHistory
import de.maengelmelder.mainmodule.objects.MessageImageUri
import org.json.JSONObject
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by christian on 28.08.17.
 *
 * api/v1/domain/<domain-id>/message/<message-id>
 */
@Deprecated(
        message = "AsyncTask is deprecated since SDK 30",
        replaceWith = ReplaceWith("coroutines.v1.MMv1MessageDetail")
)
open class MMv1MessageDetail(c: Context, msg: Message, attachPhoneId: Boolean = false)
    : MMv1Api<MessageDetail?, BaseResponse>(
        c,
        "message/${msg.serverId}",
        if (MMConstants.UseDefaultDomainWhenPossible && msg.category.domainId.isEmpty())
            MMConstants.DefaultDomainId.toString()
        else msg.category.domainId) {

    private val bAttachPhoneId = attachPhoneId

    override fun parseResponse(resp: BaseResponse): MessageDetail? {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        return if (json != null) MessageDetail.fromJSON(json, domainId) else null
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        return resp
    }

    override fun getUrlParam(): Map<String, String?>? =
            if (bAttachPhoneId) mapOf(Pair("phone", phoneId)) else null
}