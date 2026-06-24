package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import android.util.Log
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.SystemInfo
import org.json.JSONObject
import java.lang.Exception

/**
 * api/v1/bmsapp/<appid>/system/by_location
 */
@Deprecated("AsyncTask is deprecated", ReplaceWith("network.coroutines.v1.MMv1System"))
class MMv1System(ctx: Context, lat: Double, lon: Double, saveToDb: Boolean = true):
        MMv1Api<List<SystemInfo>, BaseResponse>(ctx, "", "") {

    private val mLatitude = lat
    private val mLongitude = lon
    private val mSaveToDb = saveToDb

    override fun getURL(): String {
        val url = StringBuilder()
        val domainName = if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
        url.append(domainName)
        url.append(MMConstants.V1ApiPath)
        url.append("${MMConstants.BmsAppApiPath}/$appId/system/by_location")
        return url.toString()
    }

    override fun parseResponse(resp: BaseResponse): List<SystemInfo> {
        val systems = arrayListOf<SystemInfo>()

        if (BuildConfig.DEBUG && MMConstants.ForceHost_Test) {
            // Create a dummy test system using test server host if ForceHost_Test is applied
            val baseInfo = SystemInfo().apply {
                appId = "1"
                title = MMConstants.ServerUrl_Test
                isExternal = true
                domainName = MMConstants.ServerUrl_Test
            }
            systems.add(baseInfo)
        } else {

            val jsonString = try {
                JSONObject(resp.body)
            } catch (e: Exception) {
                null
            }

            jsonString?.let { body ->
                body.optJSONObject("data")?.let { data ->
                    data.optJSONArray("systems")?.let { sys ->
                        (0 until sys.length()).forEach { i ->
                            sys.optJSONObject(i)?.let { item ->
                                SystemInfo().apply {
                                    appId = item.optString("appid", "1")
                                    title = item.optString("name", "")
                                    isExternal = item.optBoolean("external", false)
                                    domainName = item.optString("host", "").removeSuffix("/")
                                }.also { info -> systems.add(info) }
                            }
                        }
                    }
                }
            }
        }

        // Save to dat
        if (mSaveToDb) {
            context?.let { c ->
                val db = MMDB.instance(c)
                systems.forEach { s -> db.addSystem(s) }
            }
        }

        return systems
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return BaseResponse(-1, "")
        return BaseResponse(resp.code, json.optString("message"))
    }

    override fun getUrlParam(): Map<String, String?>? = mapOf("lat" to mLatitude.toString(), "lon" to mLongitude.toString())
}