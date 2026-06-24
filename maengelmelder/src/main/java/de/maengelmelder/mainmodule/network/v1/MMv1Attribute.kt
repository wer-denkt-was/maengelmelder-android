package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Attribute
import org.json.JSONObject
import java.lang.Exception

@Deprecated("AsyncTask is deprecated since SDK 30", ReplaceWith("coroutines.v1.MMv1Attribute"))
class MMv1Attribute(ctx: Context, domId: String, categoryId: String):
        MMv1Api<List<Attribute>, BaseResponse>(ctx, "attribute", domId) {

    companion object {
        const val RESP_NO_DATA = -1
    }

    private val mDomId = domId

    override fun parseResponse(resp: BaseResponse): List<Attribute> {
        val attrs = arrayListOf<Attribute>()
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        val data = json?.optJSONArray("data")

        data?.run {
            (0 until length()).forEach {
                val jsonAttr = optJSONObject(it)
                attrs.add(Attribute.fromJSON(jsonAttr, mDomId.toInt()))
            }

        }

        return attrs
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return BaseResponse(RESP_NO_DATA, "")
        return BaseResponse(resp.code, json.optString("message"))
    }

    // TODO get params for individual categoryId
    override fun getUrlParam(): Map<String, String?>? = null
}