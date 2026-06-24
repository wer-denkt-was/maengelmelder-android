package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.utils.UserData
import org.json.JSONObject
import java.lang.Exception

class MMv1Logout(c: Context) : MMNetworkRepository<BaseResponse, BaseResponse>(c, -1, "logout") {

    companion object {
        val RESP_SUCCESS = 1
        val RESP_WRONG_TOKEN = -1
    }

    private val mToken = UserData.getUserCred(c)?.token?: ""

    override fun getUrl(): String {
        val url = StringBuilder()
        val domainName = if (externalSystemInfo == null) {
            if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
        } else externalSystemInfo?.domainName

        url.append(domainName)
        url.append(MMConstants.V1ApiPath)
        url.append("/logout")
        return url.toString()
    }

    override fun getQueryParameters(): Map<String, String>? = mapOf("authorization" to mToken)

    override fun parseResponse(resp: BaseResponse): BaseResponse {
        val body = JSONObject(resp.body)
        if (body.optBoolean("success", false)) {
            return BaseResponse(RESP_SUCCESS, "")
        }
        return BaseResponse(RESP_WRONG_TOKEN, "")
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val body = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (body == null) return BaseResponse(resp.code, "")
        val message = body.optString("message")
        val code =
                if (message == "unable to logout user") {
                    RESP_WRONG_TOKEN
                } else {
                    resp.code
                }
        val msg = body.optString("message", "")
        return BaseResponse(code, msg)
    }
}