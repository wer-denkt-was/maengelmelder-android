package de.maengelmelder.mainmodule.network

import android.content.Context
import android.util.Log
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Message
import org.json.JSONArray
import java.lang.Exception

/**
 * Created by christian on 10.08.17.
 */

@Deprecated(message = "Use MMv1Message instead!")
class MMBMSGetNearestMessages(c: Context, lat: Double, lon: Double, domainid: String, domainOnly: Boolean)
    : MMBMS<ArrayList<Message>, BaseResponse>(c, "get_nearest_messages"){

    private var mLat = lat
    private var mLon = lon
    private var mDomainId = domainid
    private var bDomainOnly = domainOnly

    fun setCoordinate(lat: Double, lon: Double) {
        mLat = lat
        mLon = lon
    }

    override fun parseResponse(resp: BaseResponse): ArrayList<Message> {
        val json = JSONArray(resp.body)
        val messages = ArrayList<Message>()
        (0..(json.length()-1)).forEach { i ->
            val item = json.getJSONObject(i)
            val m = Message()
            val cat = Category()
            cat.domainId = mDomainId
            cat.typeId = item.optLong("typeid")
            cat.name = item.optString("type")
            cat.markerId = item.optString("markerid")
            cat.systemId = externalSystemInfo?.generateId()?: ""

            m.category = cat
            m.systemId = externalSystemInfo?.generateId()?: ""
            m.serverId = item.optString("messageid")
            m.title = item.optString("title", "")
            m.state = item.optString("state")
            m.state_en = item.optString("state_en")
            m.colorString = item.optString("color")
            m.distance = item.optDouble("distance")
            m.lat = item.optDouble("lat")
            m.lon = item.optDouble("long")
            m.desc = item.optString("description")
            m.text = item.optString("text")
            if (item.has("pictureUrl")) {
                m.imagePaths.add(item.optString("pictureUrl"))
            }
            m.id = m.generateId()
            messages.add(m)
        }
        return messages
    }

    override fun parseError(resp: BaseResponse): BaseResponse = resp

    override fun getUrlParam(): Map<String, String?>? {
        val domOnly: String = if (bDomainOnly) "1" else "0"
        val map = hashMapOf(
                "appid" to appId,
                "phone" to phoneId,
                "lang" to "de",
                "domainid" to mDomainId,
                "lat" to "$mLat",
                "long" to "$mLon",
                "domain_only" to domOnly
        )
        userCred?.let { uc -> if (uc.token.isNotEmpty()) map["authorization"] = uc.token }
        return map
    }
}