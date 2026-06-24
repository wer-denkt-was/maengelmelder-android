package de.maengelmelder.mainmodule.network.collectives

import android.content.Context
import de.maengelmelder.mainmodule.network.MMAPI
import de.maengelmelder.mainmodule.network.MMBMSGetNearestMessages
import de.maengelmelder.mainmodule.network.v1.MMv1Domain
import de.maengelmelder.mainmodule.network.v1.MMv1Message
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.service.tasks.CategoriesAndAttributesThread

/**
 * Calls [MMv1Domain] and [MMBMSGetNearestMessages] sequentially for each given [SystemInfo]
 */
@Deprecated(message = "Bad idea to use this as some requests can be made parallel. Just use individual, chained requests")
class APIGetNearestMessages(context: Context,
                            systems: List<SystemInfo>,
                            userLoc: Pair<Double, Double>,
                            topLat: Double, leftLon: Double, bottomLat: Double, rightLon: Double,
                            overrideDomainId: String? = null) : BaseCollectiveAPI<List<Message>>(context, systems) {

    /**
     * @property mOverrideDomainId if not null, it will skip to [MMBMSGetNearestMessages] for the given domain Id
     * @property mUserLoc user's location (Longitude-latitude), used to determine the domain
     * @property mDomains will be filled with [Domain] objects from the [MMv1Domain] call
     */

    companion object {
        private val TAG = "CollAPIMsgs"
    }

    private val mOverrideDomainId = overrideDomainId
    private val mUserLoc = userLoc
    private val mTopLat = topLat
    private val mLeftLon = leftLon
    private val mBottomLat = bottomLat
    private val mRightLon = rightLon
    private val mDomains = arrayListOf<Domain>()
    private var mCalls = arrayListOf<MMAPI>()

    val domains: List<Domain> get() = mDomains

    override fun onPreExecute() {
        mCalls = arrayListOf()
    }

    override fun doInBackground(vararg params: Void?): List<Message> {
        val ctx = mContext.get()
        if (ctx == null) return arrayListOf()

        // Prepare arraylist to contain messages and clear the previous domains
        val messages = arrayListOf<Message>()
        mDomains.clear()

        if (isCancelled) return messages

        if (mOverrideDomainId != null) {
            // Client-exclusive MM
            doGetMessages(ctx, mOverrideDomainId)?.also { msgs -> messages.addAll(msgs) }
        } else {
            // general MM or unrestricted client MM
            mSystems.forEach { info ->
                if (isCancelled) return messages

                // Get the domain from each system
                val domainApi = MMv1Domain(ctx, mUserLoc.second, mUserLoc.first).apply {
                    system = info
                }
                mCalls.add(domainApi)

                val domain = domainApi.executeInThread().let { resp ->
                    if (resp.code in 200..299) domainApi.parseResponse(resp)
                    else null
                }
                if (isCancelled) return messages

                // Get nearest messages from the given domain and boundary
                if (domain != null) {
                    // Add to domain list
                    mDomains.addAll(domain)

                    // Save the domain (Still in the same thread). System id for each domain is also saved here
                    CategoriesAndAttributesThread(ctx, domain).run()

                    if (isCancelled) return messages

                    domain.forEach { d ->
                        doGetMessages(ctx, d.id?: "", info)?.also { msgs ->
                            messages.addAll(msgs)
                        }
                    }

                }
            }
        }

        return messages
    }

    override fun cancelRequest() {
        mCalls.forEach { api -> if (!api.isRequestCancelled()) api.cancelRequest() }
        mCalls.clear()
        super.cancelRequest()
    }

    /**
     * Executes [MMv1Message] API synchronously.
     */
    private fun doGetMessages(ctx: Context, domainId: String, systemInfo: SystemInfo? = null): List<Message>? {
        if (isCancelled) return null

        val msgAPI = MMv1Message(ctx, mTopLat, mLeftLon, mBottomLat, mRightLon, 15, domainId).apply {
            system = systemInfo
        }
        mCalls.add(msgAPI)

        return msgAPI.executeInThread().let { resp ->
            if (resp.code in 200..299) msgAPI.parseResponse(resp)
            else null
        }
    }

}