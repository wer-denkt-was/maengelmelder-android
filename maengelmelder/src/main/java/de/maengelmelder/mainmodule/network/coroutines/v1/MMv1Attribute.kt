package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Attribute
import org.json.JSONObject
import java.lang.Exception

class MMv1Attribute(ctx: Context, domId: Int, categoryId: Int = -1)
    : MMNetworkRepository<List<Attribute>, BaseResponse>(ctx, domId, "attribute") {

    private val mCatId = categoryId

    override fun getQueryParameters(): Map<String, String>? {
        return if (mCatId == -1) null else mapOf("type" to mCatId.toString())
    }

    override fun parseResponse(resp: BaseResponse): List<Attribute> {
        val attrs = arrayListOf<Attribute>()
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        json?.optJSONArray("data")?.run {
            (0 until length()).forEach {
                val jsonAttr = optJSONObject(it)
                attrs.add(Attribute.fromJSON(jsonAttr, domainId))
            }
        }

        return attrs
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return BaseResponse(-1, "")
        return BaseResponse(resp.code, json.optString("message"))
    }
}