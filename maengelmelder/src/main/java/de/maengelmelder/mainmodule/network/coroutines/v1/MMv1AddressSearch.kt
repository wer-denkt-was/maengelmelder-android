package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import android.util.Log
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.AddressSearchResult
import org.json.JSONObject

/**
 * returns Pair of doubles (lon-lat)
 */
class MMv1AddressSearch(
    c: Context,
    domId: Int?,
    private val keyword: String
) : MMNetworkRepository<AddressSearchResult?, BaseResponse>(c, domId?: 0, "search_address") {

    override fun getUrl(): String {
        return if (domainId != 0) {
            super.getUrl()
        } else {
            super.getUrl().replace("/0", "")
        }
    }

    override fun getQueryParameters(): Map<String, String> = mapOf("q" to keyword)

    override fun parseResponse(resp: BaseResponse): AddressSearchResult? {
        var lonLat: Pair<Double, Double>? = null
        var formattedAddress = ""
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json != null) {
            val result = json.getJSONObject("data").optJSONArray("results")
            if (result != null && result.length() > 0) {
                // Get first result
                val first = result.getJSONObject(0)

                // Get formatted address
                formattedAddress = first.optString("formatted_address", "")

                // get the actual coordinate
                val geom = first.optJSONObject("geometry")
                if (geom != null && geom.has("location")) {
                    val lonLatObj = geom.getJSONObject("location")
                    lonLat = Pair(lonLatObj.optDouble("lng", 0.0), lonLatObj.optDouble("lat", 0.0))
                }
            }
        }
        return if (lonLat != null) AddressSearchResult(lonLat.second, lonLat.first, formattedAddress) else null
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        return resp
    }
}