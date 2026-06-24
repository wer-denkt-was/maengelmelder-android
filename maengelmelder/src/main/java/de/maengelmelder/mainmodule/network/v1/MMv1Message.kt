package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import android.util.Log
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.responses.*
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.Message
import org.json.JSONObject
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by christian on 28.08.17.
 *
 * This is similar to [MMv1SendMessage], but is a GET method
 * and used to retrieve a list of messages with the given domainId and bounding box
 *
 * /api/v1/bmsapp/<appid>/message?top=<>&left=<>&bottom=<>&right=<>&fieldset=mmv2
 */
@Deprecated("AsyncTask is deprecated starting Android 10", ReplaceWith("network.coroutines.v1.MMv1Message"))
open class MMv1Message(c: Context,
                       topLat: Double, leftLon: Double, bottomLat: Double, rightLon: Double,
                       zoomLvl: Int = 15, optionalDomainId: String = "")
    : MMv1Api<List<Message>, BaseResponse>(c, "message", optionalDomainId) {

    private val mDtFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    private val mOptionalDomainId = optionalDomainId
    private val mTopLat = topLat
    private val mBottomLat = bottomLat
    private val mLeftLon = leftLon
    private val mRightLon = rightLon
    private val mZoomLvl = zoomLvl

    private val mDB = MMDB.instance(c)

    private var mDomains: List<Domain>? = null

    val domains: List<Domain>? get() = mDomains

    override fun getURL(): String {
        if (mOptionalDomainId.isNotEmpty()) {
            return super.getURL()
        }

        var overrideAppId = externalSystemInfo?.appId
        if (MMConstants.ForceUseOverriddenAppId) {
            overrideAppId = MMConstants.OverrideAppId
        }

        val url = StringBuilder()
        val domainName = if (externalSystemInfo == null) {
            if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
        } else externalSystemInfo?.domainName

        url.append(domainName)
        url.append(MMConstants.V1ApiPath)
        url.append("${MMConstants.BmsAppApiPath}/$overrideAppId/message")

        return url.toString()
    }

    override fun parseResponse(resp: BaseResponse): List<Message> {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        val msgs = arrayListOf<Message>()

        val otherDomains = arrayListOf<Domain>()

        json?.run {
            optJSONArray("data")?.let { messages ->
                (0 until messages.length()).forEach { i ->
                    messages.optJSONObject(i)?.let { m ->
                        val msg = Message()
                        msg.id = m.optString("id")?: ""
                        msg.serverId = msg.id
                        msg.systemId = externalSystemInfo?.generateId()?: ""
                        msg.title = m.optString("title")
                        msg.desc = m.optString("text")
                        msg.state = m.optString("state_german")
                        msg.state_en = m.optString("state")

                        val dtString = m.optString("created")
                        msg.createdAt = try { mDtFormatter.parse(dtString).time } catch (e: Exception) { -1 }

                        // prepare category and domain
                        val cat = Category()
                        val domObj = Domain.createDefault()
                        domObj.systemId = msg.systemId

                        // Get the domain information
                        m.optJSONObject("domain")?.let { domain ->
                            val id = domain.optString("id", MMConstants.DefaultDomainId.toString())
                            domObj.id = id
                            domObj.name = domain.optString("title", "")
                        }

                        cat.domainId = domObj.id?: ""

                        // Save the domain to DB if not recorded yet
                        if (otherDomains.find { dom -> dom.id == domObj.id } == null) {
                            mDB.addDomain(domObj)
                            otherDomains.add(domObj)
                        }

                        cat.systemId = msg.systemId
                        m.optJSONObject("message_type")?.let { mtype ->
                            cat.typeId = mtype.optLong("id")
                            cat.name = mtype.optString("name")
                            cat.description = mtype.optString("description")
                            cat.markerId = m.optString("marker_id")
                            msg.category = cat
                        }

                        msg.colorString = m.optString("marker_color")
                        msg.lat = m.optDouble("lat", 0.0)
                        msg.lon = m.optDouble("lon", 0.0)

                        msgs.add(msg)
                    }
                }
            }
        }

        this.mDomains = otherDomains

        return msgs
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        return resp
    }

    override fun getUrlParam(): Map<String, String?>? = mapOf(
            Pair("top", mTopLat.toString()),
            Pair("left", mLeftLon.toString()),
            Pair("bottom", mBottomLat.toString()),
            Pair("right", mRightLon.toString()),
            Pair("fieldset", "mmv2_map"),
            Pair("sort", "-created"),
            Pair("visible_map", "1"),
            Pair("zoom", "$mZoomLvl")
    )

}