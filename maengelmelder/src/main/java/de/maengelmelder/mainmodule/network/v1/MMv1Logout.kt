package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import android.util.Log
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.LoginWithTokenResponse
import de.maengelmelder.mainmodule.utils.UserData
import org.json.JSONObject
import java.lang.Exception

@Deprecated(
        message = "AsyncTask is deprecated since SDK 30",
        replaceWith = ReplaceWith("coroutines.v1.MMv1Logout")
)
class MMv1Logout (c: Context) : MMv1Api<LoginWithTokenResponse, BaseResponse>(c, "logout") {

    companion object {
        val RESP_SUCCESS = 1
        val RESP_WRONG_TOKEN = -1
    }

    private val mToken = UserData.getUserCred(c)?.token?: ""

    override fun getURL(): String {
        val url = StringBuilder()
        val domainName = if (externalSystemInfo == null) {
            if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
        } else externalSystemInfo?.domainName

        url.append(domainName)
        url.append(MMConstants.V1ApiPath)
        url.append("/logout")
        return url.toString()
    }

    override fun parseResponse(resp: BaseResponse): LoginWithTokenResponse {
        val body = JSONObject(resp.body)
        if (body.optBoolean("success", false)) {
            return LoginWithTokenResponse(RESP_SUCCESS, "", "", "", "")
        }
        return LoginWithTokenResponse(RESP_WRONG_TOKEN, "", "", "", "")
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

    override fun getUrlParam(): Map<String, String?>? = mapOf("authorization" to mToken)
}