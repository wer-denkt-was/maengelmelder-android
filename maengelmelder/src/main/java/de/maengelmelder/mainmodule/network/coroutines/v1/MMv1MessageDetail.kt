package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.MessageDetail
import de.maengelmelder.mainmodule.utils.UserData
import org.json.JSONObject
import java.lang.Exception

class MMv1MessageDetail(c: Context, msg: Message, attachPhoneId: Boolean = false)
    : MMNetworkRepository<MessageDetail?, BaseResponse>(
        c,
        if (MMConstants.UseDefaultDomainWhenPossible || msg.category.domainId.isEmpty())
            MMConstants.DefaultDomainId
        else msg.category.domainId.toInt(),
        "message/${msg.serverId}"){

    private val bAttachPhoneId = attachPhoneId

    override fun getQueryParameters(): Map<String, String>? {
        return if (bAttachPhoneId) mapOf(Pair("phone", phoneId)) else null
    }

    override fun parseResponse(resp: BaseResponse): MessageDetail? {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        return if (json != null) MessageDetail.fromJSON(json, domainId.toString()) else null
    }

    override fun parseError(resp: BaseResponse): BaseResponse = resp
}