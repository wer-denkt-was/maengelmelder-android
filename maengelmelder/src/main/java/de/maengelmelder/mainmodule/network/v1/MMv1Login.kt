package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import android.util.Log
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.LoginWithTokenResponse
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.UserCred
import org.json.JSONObject
import java.lang.Exception

@Deprecated(
        message = "AsyncTask is deprecated since SDK 30",
        replaceWith = ReplaceWith("coroutines.v1.MMv1Login")
)
class MMv1Login (c: Context, username: String, pass: String, domain: Domain? = null)
    : MMv1Api<UserCred, BaseResponse>(c, "login", domain?.id?: MMConstants.DefaultDomainId.toString()) {

    companion object {
        const val RESP_SERVERFAIL = -2
        const val RESP_WRONG_CRED = -1
        const val RESP_SUCCESS = 1
    }

    private val mUser = username
    private val mPass = pass
    private val mDomain = domain

    val username: String get() = mUser
    val password: String get() = mPass

    init {
        addContent("email", mUser)
        addContent("password", mPass)
        addContent("domainid", domainId?: MMConstants.DefaultDomainId.toString())
    }

    override fun getURL(): String {
        val url = StringBuilder()
        val domainName = if (externalSystemInfo == null) {
            if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
        } else externalSystemInfo?.domainName

        url.append(domainName)
        url.append(MMConstants.V1ApiPath)
        url.append("/login")
        return url.toString()
    }

    override fun parseResponse(resp: BaseResponse): UserCred {
        val userCred = UserCred()
        JSONObject(resp.body).let { json ->
            json.optJSONObject("data")?.let { data ->
                userCred.token = data.optString("token", "")
                data.optJSONObject("user")?.let { user ->
                    userCred.status = user.optString("status", UserCred.STATUS_ENABLED)
                    userCred.id = user.optString("id", "")
                    userCred.email = user.optString("email", "")
                    userCred.avatarUri = user.optString("avatar_uri", "")
                    userCred.publicName = user.optString("public_name", "")
                    userCred.domain = mDomain?: Domain(MMConstants.DefaultDomainId.toString()).apply {
                        name = MMConstants.DefaultDomainName
                    }
                }
            }
        }
        return userCred
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val body = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (body == null) return BaseResponse(RESP_SERVERFAIL, "")
        val message = body.optString("message")
        val code =
                if (message == "unable to authenticate") {
                    RESP_WRONG_CRED
                } else {
                    resp.code
                }
        val msg = body.optString("message", "")
        return BaseResponse(code, msg)
    }

    override fun getUrlParam(): Map<String, String?>? = null
}