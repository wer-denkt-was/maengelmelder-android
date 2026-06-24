package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.LoginResponse
import org.json.JSONObject

/**
 * This class executes v1/check_login API to check whether the submitted username and password are correct.
 * The class does not handle caching, tokens, or credentials management, but exists to solely check whether the given username and password
 * are registered in the MM system
 */
@Deprecated(message = "Not used anymore. MMv1Login returns the login token. You can instead check if there is a login token from UserCred class")
class MMv1CheckLogin(c: Context) :
        MMv1Api<LoginResponse, LoginResponse>(c, "check_login", MMConstants.DefaultDomainId.toString()){

    private val mTextFailedParsing = c.getString(R.string.error_parse_login_resp)

    override fun parseResponse(resp: BaseResponse): LoginResponse = parse(resp)

    override fun parseError(resp: BaseResponse): LoginResponse = parse(resp)

    override fun getUrlParam(): Map<String, String?>? = null

    private fun parse(resp: BaseResponse): LoginResponse {
        var json: JSONObject? = null
        try {
            json = JSONObject(resp.body)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        if (json == null) return LoginResponse(false, mTextFailedParsing)

        return LoginResponse(json.optBoolean("success"), json.optJSONObject("data").optString("message"))
    }
}