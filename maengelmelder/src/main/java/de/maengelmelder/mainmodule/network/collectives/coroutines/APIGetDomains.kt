package de.maengelmelder.mainmodule.network.collectives.coroutines

import android.content.Context
import android.util.Log
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Domain
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.service.tasks.CategoriesAndAttributesThread
import kotlinx.coroutines.CoroutineScope

class APIGetDomains(context: Context,
                    systems: List<SystemInfo>,
                    userLat: Double, userLon: Double,
                    disableUserAuth: Boolean = false)
    : IOCoroutine<List<Domain>>(context, systems) {

    private val mUserLoc = Pair(userLon, userLat)
    private var mOnSuccess: ((domains: List<Domain>) -> Unit)? = null
    private var mOnError: ((resp: BaseResponse) -> Unit)? = null
    private val bDisableUserAuth = disableUserAuth

    var success: ((domains: List<Domain>) -> Unit)?
        get() = mOnSuccess
        set(value) { mOnSuccess = value }

    var error: ((resp: BaseResponse) -> Unit)?
        get() = mOnError
        set(value) { mOnError = value }

    override fun insideDispatcherIO(scope: CoroutineScope): Pair<List<Domain>?, BaseResponse?> {
        val domains = arrayListOf<Domain>()

        if (systemInfos.isEmpty()) {
            // Client-exclusive MM
            doGetDomain(context, null, scope, bDisableUserAuth)?.also { dom ->
                CategoriesAndAttributesThread(context, dom).run()
                domains.addAll(dom)
            }
        } else {
            // general MM or unrestricted client MM
            systemInfos.forEach { info ->

                // Get the domain from each system
                val domain = doGetDomain(context, info, scope, bDisableUserAuth)

                if (domain != null) {
                    domains.addAll(domain)
                }
            }
        }

        return Pair(domains, null)
    }

    override fun onSuccess(data: List<Domain>) {
        success?.invoke(data)
    }

    override fun onError(err: BaseResponse) {
        error?.invoke(err)
    }

    private fun doGetDomain(ctx: Context,
                            systemInfo: SystemInfo? = null,
                            coroutineScope: CoroutineScope? = null,
                            disableUserAuth: Boolean = false): List<Domain>? {

        val domainApi = MMv1Domain(ctx, mUserLoc.second, mUserLoc.first).apply {
            this.attachUserCred = !disableUserAuth
            this.externalSystemInfo = systemInfo
        }
        return domainApi.doExecuteAPI(coroutineScope).let { resp ->
            if (resp != null) {
                if (resp.code in 200..299) domainApi.parseResponse(resp)
                else null
            } else {
                null
            }
        }
    }
}