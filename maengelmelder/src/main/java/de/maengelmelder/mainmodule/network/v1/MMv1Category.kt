package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import android.util.Log
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import org.json.JSONObject
import java.lang.Exception

@Deprecated("Asynctask is deprecated since SDK 30", ReplaceWith("coroutines.v1.MMv1Category"))
class MMv1Category(c: Context, domId: String, catId: String) :
        MMv1Api<Category?, BaseResponse>(c, "category/$catId", domId) {

    companion object {
        const val RESP_NO_DATA = -1
    }

    override fun parseResponse(resp: BaseResponse): Category? {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return null

        val data = json.optJSONObject("data")
        var category: Category? = null
        data?.let { d ->
            category = Category.fromJSON(data)
        }
        return category
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return BaseResponse(RESP_NO_DATA, "")

        return BaseResponse(resp.code, json.optString("message"))
    }

    override fun getUrlParam(): Map<String, String?>? = null
}