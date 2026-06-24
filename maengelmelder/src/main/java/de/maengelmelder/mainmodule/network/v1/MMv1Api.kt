package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.utils.UserData

abstract class MMv1Api<T, K>(c: Context, method: String, domId: String? = null) : MMBMS<T, K>(c, method) {

    private var mDomainId = domId

    protected val domainId get() = mDomainId

    override fun getURL(): String {
        val url = StringBuilder()
        var domainName = if (BuildConfig.debug) MMConstants.ServerUrl_Test else MMConstants.ServerUrl
        externalSystemInfo?.let { info -> domainName = info.domainName }

        return url.append(domainName).append(MMConstants.V1ApiPath).
                append("/domain").append("/$mDomainId").append("/$mMethod").toString()
    }

}