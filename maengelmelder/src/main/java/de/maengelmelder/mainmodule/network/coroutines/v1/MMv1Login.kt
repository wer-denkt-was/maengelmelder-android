package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.UserCred
import org.json.JSONObject
import java.lang.Exception

class MMv1Login(c: Context, username: String, pass: String, domain: Domain? = null) :
    MMNetworkRepository<UserCred, BaseResponse>(c, domain?.id?.toInt()?: MMConstants.DefaultDomainId, "login")  {

    companion object {
        const val RESP_SERVER_ERROR = -2
        const val RESP_WRONG_CRED = -1
    }

    private val mDomain = domain

    init {
        multipartFormAddString("email", username)
        multipartFormAddString("password", pass)

        var staticDomainId = MMConstants.getStaticDomainId(context)
        if (staticDomainId == 0) {
            staticDomainId = domainId
        }
        multipartFormAddString("domainid", staticDomainId.toString())
    }

    private fun getDomainName(): String? {
        return if (externalSystemInfo == null) {
            if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
        } else externalSystemInfo?.domainName
    }

    override fun getUrl(): String {
        val url = StringBuilder()
        val domainName = getDomainName()
        url.append(domainName)
        url.append(MMConstants.V1ApiPath)
        url.append("/login")
        return url.toString()
    }

    override fun getQueryParameters(): Map<String, String>? = null

    override fun parseResponse(resp: BaseResponse): UserCred {
        val userCred = UserCred()
        userCred.systemInfo = extSystemInfo
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
                    userCred.firstname = user.optString("firstname", "")
                    userCred.lastname = user.optString("lastname", "")
                    // userCred.systemUrl = getDomainName()?: (if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl)
                    userCred.type = user.optString("type", "")
                }
            }
        }

        return userCred
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val body = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (body == null) return BaseResponse(RESP_SERVER_ERROR, "")
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
}