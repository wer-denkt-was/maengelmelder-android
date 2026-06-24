package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Attribute
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import org.json.JSONObject
import java.lang.Exception

@Deprecated("AsyncTask is deprecated starting from SDK 30", ReplaceWith("coroutines.v1.MMv1Domain"))
class MMv1Domain(ctx: Context, lat: Double, lon: Double):
        MMv1Api<List<Domain>, BaseResponse>(ctx, "", "") {

    companion object {
        const val RESP_NO_DATA = -1
    }

    private val mLat = lat
    private val mLon = lon
    private val mAppId = MMConstants.OverrideAppId

    private var bCheckOriginalSystemOnly = false
    var checkOriginalSystemOnly
        get() = bCheckOriginalSystemOnly
        set(value) { bCheckOriginalSystemOnly = value }

    override fun getURL(): String {
        val url = StringBuilder()
        var domainName = MMConstants.ServerUrl
        var currentAppId = appId
        if (checkOriginalSystemOnly) {
            currentAppId = "1"
        } else {
            currentAppId = if (externalSystemInfo == null) mAppId else externalSystemInfo?.appId?: "1"
            if (MMConstants.ForceUseOverriddenAppId) {
                currentAppId = mAppId
            }
            domainName = if (externalSystemInfo == null) {
                if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
            } else externalSystemInfo?.domainName?: MMConstants.ServerUrl
        }
        url.append(domainName)
        url.append(MMConstants.V1ApiPath)
        url.append("${MMConstants.BmsAppApiPath}/$currentAppId/domain")
        return url.toString()
    }

    override fun parseResponse(resp: BaseResponse): List<Domain> {
        val jsonBody = try { JSONObject(resp.body) } catch (e: Exception) { null }
        val domains = arrayListOf<Domain>()
        // Primary domain
        jsonBody?.optJSONObject("data")?.run {
            val pd = Domain()
            optJSONObject("primary_domain")?.let { primaryDom ->
                pd.id = primaryDom.optInt("id", -1).toString()
                pd.name = primaryDom.optString("name", "")
                pd.systemId = externalSystemInfo?.generateId()?: ""
                primaryDom.optJSONObject("settings")?.let { settings ->
                    pd.settingsFromJson(settings.toString())
                }
                domains.add(pd)
            }

            // Categories
            optJSONArray("types")?.let { types ->
                (0 until types.length()).forEach { i ->
                    val catJSON = types.getJSONObject(i)
                    val cat = Category.fromJSON(catJSON).apply {
                        systemId = externalSystemInfo?.generateId()?: ""
                    }
                    var selDomain = pd
                    catJSON.optJSONObject("domain")?.let { catDom ->
                        val domId = catDom.optInt("id", -1)
                        if (domId != -1) {
                            val domain = domains.find { d -> d.id == domId.toString() }
                            if (domain == null) {
                                // new domain, different from the primary one. Make a new domain entry
                                val newDom = Domain().apply {
                                    this.id = domId.toString()
                                    this.name = catDom.optString("title", "")
                                    this.systemId = externalSystemInfo?.generateId() ?: ""
                                }
                                selDomain = newDom
                                domains.add(newDom)
                            } else {
                                selDomain = domain
                            }
                        }
                    }

                    // attributes
                    catJSON.optJSONArray("attributes")?.let { attrs ->
                        (0 until attrs.length()).forEach { j ->
                            val attrJSON = attrs.getJSONObject(j)
                            val attr = Attribute.fromJSON(attrJSON, selDomain.id?.toInt()!!).apply {
                                systemId = externalSystemInfo?.generateId()?: ""
                                id = generateId()
                            }
                            cat.addAttribute(attr)
                        }
                    }
                    selDomain.addCategory(cat)
                }
            }
        }
        return domains
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return BaseResponse(MMv1Domain.RESP_NO_DATA, "")
        return BaseResponse(resp.code, json.optString("message"))
    }

    override fun getUrlParam(): Map<String, String?>? {
        val params = hashMapOf(Pair("lat", mLat.toString()), Pair("lon", mLon.toString()))
        userCred?.let { cred ->
            if (cred.isUserValid() && cred.token.isNotEmpty()) params["authorization"] = cred.token
        }
        return params
    }
}