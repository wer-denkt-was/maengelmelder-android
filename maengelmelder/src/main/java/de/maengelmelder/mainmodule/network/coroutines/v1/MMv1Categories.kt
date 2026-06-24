package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.coroutines.MMOkHttpClient
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import org.json.JSONObject
import java.lang.Exception

class MMv1Categories (c: Context, domId: Int)
    : MMNetworkRepository<Domain?, BaseResponse>(c, domId, "category") {

    private val mDomainId = domId

    override fun getQueryParameters(): Map<String, String>? = null

    override fun parseResponse(resp: BaseResponse): Domain? {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return null

        val domain = Domain(mDomainId.toString())
        val data = json.optJSONArray("data")
        if (data != null) {
            (0 until data.length()).forEach { i ->
                val catJSON = data.getJSONObject(i)
                val category = Category.fromJSON(catJSON)
                domain.addCategory(category)
            }
        }

        return domain
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return BaseResponse(-1, "")
        return BaseResponse(resp.code, json.optString("message"))
    }
}