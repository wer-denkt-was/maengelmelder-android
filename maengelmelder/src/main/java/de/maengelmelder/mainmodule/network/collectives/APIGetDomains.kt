package de.maengelmelder.mainmodule.network.collectives

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.network.v1.MMv1Domain
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.service.tasks.CategoriesAndAttributesThread
import java.lang.ref.WeakReference

/**
 * Used to retrieve the list of domains from the given list of [System]
 */
@Deprecated("AsnyTask is deprecated starting from SDK 30", ReplaceWith("coroutines.APIGetDomains"))
class APIGetDomains(context: Context,
                       systems: List<SystemInfo>,
                       userLat: Double, userLon: Double) : BaseCollectiveAPI<List<Domain>>(context, systems) {

    companion object {
        private val TAG = "CollAPI-Domains"
    }

    private val mUserLoc = Pair(userLon, userLat)

    override fun doInBackground(vararg params: Void?): List<Domain> {
        val ctx = mContext.get()
        if (ctx == null) return arrayListOf()

        val domains = arrayListOf<Domain>()

        if (mSystems.isEmpty()) {
            // Client-exclusive MM
            doGetDomain(ctx)?.also { dom ->
                CategoriesAndAttributesThread(ctx, dom).run()
                domains.addAll(dom)
            }
        } else {
            // general MM or unrestricted client MM
            mSystems.forEach { info ->

                // Get the domain from each system
                val domain = doGetDomain(ctx, info)

                if (domain != null) {
                    // Save the domain (Still in the same thread). System id for each domain is also saved here
                    CategoriesAndAttributesThread(ctx, domain).run()
                    domains.addAll(domain)
                }
            }
        }

        return domains
    }

    private fun doGetDomain(ctx: Context, systemInfo: SystemInfo? = null): List<Domain>? {
        val domainApi = MMv1Domain(ctx, mUserLoc.second, mUserLoc.first).apply { system = systemInfo }
        return domainApi.executeInThread().let { resp ->
            if (resp.code in 200..299) domainApi.parseResponse(resp)
            else null
        }
    }
}