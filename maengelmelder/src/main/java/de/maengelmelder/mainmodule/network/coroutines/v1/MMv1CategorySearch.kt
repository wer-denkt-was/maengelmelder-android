package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.SystemInfo
import org.json.JSONObject
import java.lang.Exception

class MMv1CategorySearch(
    c: Context,
    domId: Int,
    keyword: String
) : MMNetworkRepository<List<Category>, BaseResponse>(c, domId, "category/keyword_search") {

    private val mKeyword = keyword

    override fun getQueryParameters(): Map<String, String> = mapOf("q" to mKeyword)

    override fun parseResponse(resp: BaseResponse): List<Category> {
        val categoryList = arrayListOf<Category>()
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return categoryList

        val data = json.optJSONArray("data")
        val system = externalSystemInfo?: SystemInfo.getDefaultSystemInfo()
        if (data != null) {
            for (i in 0 until data.length()) {
                // Parse
                val category = Category.fromJSON(data.getJSONObject(i))

                // External system is set to default system (id 1, original MM system) if it doesn't exist
                category.systemId = system.generateId()

                // Add to list
                categoryList.add(category)
            }
        }
        return categoryList
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return BaseResponse(-1, "")

        return BaseResponse(resp.code, json.optString("message"))
    }
}