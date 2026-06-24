package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.SystemInfo
import org.json.JSONObject
import java.lang.Exception

class MMv1System(ctx: Context, lat: Double, lon: Double, saveToDb: Boolean = true)
    : MMNetworkRepository<List<SystemInfo>, BaseResponse>(ctx, -1, "system/by_location") {

    private val mLatitude = lat
    private val mLongitude = lon
    private val mSaveToDb = saveToDb

    init {
        addBearerTokenWhenAvailable = false
    }

    override fun getUrl(): String {
        val url = StringBuilder()
        val domainName = if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
        url.append(domainName)
        url.append(MMConstants.V1ApiPath)
        url.append("${MMConstants.BmsAppApiPath}/$appId/$methodName")
        return url.toString()
    }

    override fun getQueryParameters(): Map<String, String>? {
        return mapOf("lat" to mLatitude.toString(), "lon" to mLongitude.toString())
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
                                }.also { info ->
                                    // For testing, since the URL being given from System API is always Live URL
                                    // We have to manually change it to our test url
                                    if (info.appId == "1" && BuildConfig.DEBUG && !MMConstants.ForceHost_Test) {
                                        info.domainName = MMConstants.ServerUrl_Test
                                    }

                                    systems.add(info)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Save to dat
        if (mSaveToDb) {
            val db = MMDB.instance(context)
            systems.forEach { s -> db.addSystem(s) }
        }

        return systems
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return BaseResponse(resp.code, "")
        return BaseResponse(resp.code, json.optString("message"))
    }
}