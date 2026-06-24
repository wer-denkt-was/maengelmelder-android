package de.maengelmelder.mainmodule.network

import android.content.Context
import android.util.Log
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.LoginWithTokenResponse
import de.maengelmelder.mainmodule.utils.UserData
import org.json.JSONObject
import java.lang.Exception

/**
 * Created by christian on 29.09.17.
 */

@Deprecated("Use MMv1Logout instead")
class MMBMSLogout(c: Context) : MMBMS<LoginWithTokenResponse, BaseResponse>(c, "logout") {

    private val mToken = UserData.getUserCred(c)?.token?: ""

    override fun parseResponse(resp: BaseResponse): LoginWithTokenResponse {
        val body = JSONObject(resp.body)
        val token = body.optString("token", "")
        if (token == null || token.isEmpty()) {
            // wrong email address / password
            return LoginWithTokenResponse(resp.code, "", "", "", "")
        }

        return LoginWithTokenResponse(resp.code, "","", "", "")
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val body = try { JSONObject(resp.body) } catch (e: Exception) { null }
        return BaseResponse(resp.code, body?.optString("error", "")?: "")
    }

    override fun getUrlParam(): Map<String, String?>? = hashMapOf("authorization" to mToken)
}