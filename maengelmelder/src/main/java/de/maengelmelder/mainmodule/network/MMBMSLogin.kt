package de.maengelmelder.mainmodule.network

import android.content.Context
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.LoginWithTokenResponse
import org.json.JSONObject
import java.lang.Exception

/**
 * Created by christian on 29.09.17.
 */
@Deprecated("Use MMv1Login instead")
class MMBMSLogin(c: Context, username: String, pass: String) : MMBMS<LoginWithTokenResponse, BaseResponse>(c, "login") {

    companion object {
        const val CODE_SERVERFAIL = -2
        const val CODE_WRONG_USERNAME_PASS = -1
        const val CODE_SUCCESS = 1
    }

    private val mUser = username
    private val mPass = pass

    init {
        addContent("email", mUser)
        addContent("password", mPass)
    }

    override fun parseResponse(resp: BaseResponse): LoginWithTokenResponse {
        val body = JSONObject(resp.body)
        val token = body.optString("token", "")
        if (token == null || token.isEmpty()) {
            // wrong email address / password
            return LoginWithTokenResponse(CODE_WRONG_USERNAME_PASS, "", mUser, "", "")
        }

        return LoginWithTokenResponse(CODE_SUCCESS, "",mUser, token, "")
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val body = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (body == null) return BaseResponse(CODE_SERVERFAIL, "")
        val code =
            if (body.has("error")) {
                CODE_WRONG_USERNAME_PASS
            } else {
                resp.code
            }
        val msg = body.optString("error", "")
        return BaseResponse(code, msg)
    }

    override fun getUrlParam(): Map<String, String?>? = null
}