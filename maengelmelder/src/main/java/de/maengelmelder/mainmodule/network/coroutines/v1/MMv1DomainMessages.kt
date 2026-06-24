package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Message
import org.json.JSONObject
import java.lang.Exception

class MMv1DomainMessages(c: Context,
                         domainId: Int,
                         params: HashMap<String, String>? = null,
                         useUserCred: Boolean = false,
                         saveToDb: Boolean = false) :
    MMNetworkRepository<List<Message>, BaseResponse>(c, domainId, "message") {

    val mParams = params
    val bUseUserCred = useUserCred
    val bSaveToDb = saveToDb
    val mDomainId = domainId

    override fun getUrl(): String {
        if (!bUseUserCred) return super.getUrl()
        return if (userCred != null && userCred.isUserValid() && userCred.systemInfo != null) {
            userCred.systemInfo?.domainName + MMConstants.V1ApiPath + "/domain/" + mDomainId + "/" + methodName
        } else {
            super.getUrl()
        }
    }

    override fun getQueryParameters(): Map<String, String>? {
        mParams?.put("fields", "id,title,text,state_german,state,marker_uri,created,domain,message_type,marker_color,lat,lon")
        return mParams
    }

    override fun parseResponse(resp: BaseResponse): List<Message> {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        val msgs = arrayListOf<Message>()
        val db = MMDB.instance(context)
        json?.run {
            optJSONArray("data")?.let { messages ->
                (0 until messages.length()).forEach { i ->
                    messages.optJSONObject(i)?.let { m ->
                        val message = Message.fromJson(m, extSystemInfo)
                        msgs.add(message)
                        // Save to DB
                        if (bSaveToDb) {
                            db.addMessageFromServer(message)
                        }
                    }
                }
            }
        }
        return msgs
    }

    override fun parseError(resp: BaseResponse): BaseResponse = resp
}