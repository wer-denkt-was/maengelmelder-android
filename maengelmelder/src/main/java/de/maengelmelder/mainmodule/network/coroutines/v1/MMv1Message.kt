package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.Message
import org.json.JSONObject
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/**
 * If domainid is given, we revert to base URL.
 * If not, we use bmsapp path (/api/v1/bmsapp/<appid>/message?top=<>&left=<>&bottom=<>&right=<>&fieldset=mmv2)
 */
class MMv1Message(c: Context,
                  topLat: Double, leftLon: Double, bottomLat: Double, rightLon: Double,
                  zoomLvl: Int = 15, optionalDomainId: Int = 0)
    : MMNetworkRepository<List<Message>, BaseResponse>(c, optionalDomainId, "message") {

    private val mDtFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    private val mOptDomainid = optionalDomainId
    private val mTopLat = topLat
    private val mBottomLat = bottomLat
    private val mLeftLon = leftLon
    private val mRightLon = rightLon
    private val mZoomLvl = zoomLvl

    private val mDB = MMDB.instance(c)

    private var mDomains: List<Domain>? = null

    val domains: List<Domain>? get() = mDomains

    override fun getUrl(): String {
        var overrideAppId = externalSystemInfo?.appId
        if (MMConstants.ForceUseOverriddenAppId) {
            overrideAppId = MMConstants.OverrideAppId
        }

        val url = StringBuilder()
        var domainName = if (extSystemInfo == null) {
            if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
        } else extSystemInfo?.domainName

        // if ForceHost_Test is active, then we use the test URL no matter what
        if (MMConstants.ForceHost_Test && BuildConfig.debug) {
            domainName = MMConstants.ServerUrl_Test
        }

        url.append(domainName)
        url.append(MMConstants.V1ApiPath)
        if (mOptDomainid <= 0) {
            url.append(MMConstants.BmsAppApiPath)
            url.append("/$overrideAppId")
        } else {
            url.append("/domain")
            url.append("/$mOptDomainid")
        }
        url.append("/message")

        return url.toString()
    }

    override fun getQueryParameters(): Map<String, String>? {
        return mapOf(
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

    override fun parseResponse(resp: BaseResponse): List<Message> {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        val msgs = arrayListOf<Message>()

        val otherDomains = arrayListOf<Domain>()

        json?.run {
            optJSONArray("data")?.let { messages ->
                (0 until messages.length()).forEach { i ->
                    messages.optJSONObject(i)?.let { m ->
                        val message = Message.fromJson(m, extSystemInfo)
                        msgs.add(message)

                        val domObj = Domain.createDefault()
                        domObj.systemId = message.systemId

                        // Get the domain information
                        m.optJSONObject("domain")?.let { domain ->
                            val id = domain.optString("id", MMConstants.DefaultDomainId.toString())
                            domObj.id = id
                            domObj.name = domain.optString("title", "")
                        }

                        // Save the domain to DB if not recorded yet
                        if (otherDomains.find { dom -> dom.id == domObj.id } == null) {
                            mDB.addDomain(domObj)
                            otherDomains.add(domObj)
                        }
                    }
                }
            }
        }

        this.mDomains = otherDomains

        return msgs
    }

    override fun parseError(resp: BaseResponse): BaseResponse = resp
}