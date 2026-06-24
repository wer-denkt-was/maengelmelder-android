package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import android.util.Log
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Attribute
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import org.json.JSONObject
import java.lang.Exception

@Deprecated("AsyncTask is deprecated starting from SDK 20", ReplaceWith("coroutines.v1.MMv1Categories"))
class MMv1Categories(c: Context, domId: String) :
        MMv1Api<Domain?, BaseResponse>(c, "category", domId) {

    companion object {
        const val RESP_NO_DATA = -1
    }

    private val mDomId = domId

    override fun parseResponse(resp: BaseResponse): Domain? {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return null

        val domain = Domain(mDomId)
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
        if (json == null) return BaseResponse(RESP_NO_DATA, "")

        return BaseResponse(resp.code, json.optString("message"))
    }

    override fun getUrlParam(): Map<String, String?>? = null
}