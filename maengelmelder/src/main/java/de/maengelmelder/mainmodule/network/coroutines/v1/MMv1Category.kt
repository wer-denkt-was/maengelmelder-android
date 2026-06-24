package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.UserCred
import de.maengelmelder.mainmodule.utils.UserData
import org.json.JSONObject
import java.lang.Exception

class MMv1Category(c: Context, domId: Int, catId: String) : MMNetworkRepository<Category?, BaseResponse>(c, domId, "category/$catId") {

    init {
        val userCred = UserData.getUserCred(c)
        addBearerTokenWhenAvailable = userCred != null && userCred.isUserValid() && userCred.domain?.id == domId.toString()
    }

    override fun getQueryParameters(): Map<String, String>? = null

    override fun parseResponse(resp: BaseResponse): Category? {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return null

        val data = json.optJSONObject("data")
        var category: Category? = null
        data?.let { d -> category = Category.fromJSON(d) }
        return category
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return BaseResponse(-1, "")

        return BaseResponse(resp.code, json.optString("message"))
    }
}