package de.maengelmelder.mainmodule.network

import android.content.Context
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Message

/**
 * Created by christian on 07.09.17.
 */

@Deprecated("Use MMv1Duplicates instead!")
class MMBMSGetDuplicates(c: Context, categoryId: String, domainId: String?, lat: Double, lon: Double)
    : MMBMS<ArrayList<Message>, BaseResponse>(c, "find_duplicates") {

    private val mCategoryId = categoryId
    private val mDomainId = domainId
    private val mLat = lat
    private val mLon = lon
    private val mNearestMessage = MMBMSGetNearestMessages(c, mLat, mLon, domainId!!, domainOnly = true)

    override fun parseResponse(resp: BaseResponse): ArrayList<Message> = mNearestMessage.parseResponse(resp)

    override fun parseError(resp: BaseResponse): BaseResponse = mNearestMessage.parseError(resp)

    override fun getUrlParam(): Map<String, String?>? {
        val map = hashMapOf<String, String?>(
                "categoryid" to mCategoryId,
                "lat" to mLat.toString(),
                "long" to mLon.toString()
        )
        userCred?.let { uc -> if (uc.token.isNotEmpty()) map["authorization"] = uc.token }

        if (mDomainId != null) {
            map["domainid"] = mDomainId
        }
        return map
    }
}