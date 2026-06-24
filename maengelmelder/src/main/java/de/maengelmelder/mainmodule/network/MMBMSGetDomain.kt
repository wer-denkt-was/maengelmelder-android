package de.maengelmelder.mainmodule.network

import android.content.Context
import android.util.Log
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Attribute
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.utils.UserData
import org.json.JSONArray
import org.json.JSONObject

/**
 * Created by christian on 04.08.17.
 */

@Deprecated(message = "Use MMv1Domain instead!!")
class MMBMSGetDomain(c: Context, lang: String, lat: Double, lon: Double) : MMBMS<Domain, BaseResponse>(c, "get_domain") {

    var mLang: String = lang
    var mLat: Double = lat
    var mLon: Double = lon

    fun setCoordinate(lat: Double, lon: Double) {
        mLat = lat
        mLon = lon
    }

    override fun parseResponse(resp: BaseResponse): Domain {
        val dom = Domain()
        val json = JSONObject(resp.body)
        dom.id = json.optString("domainid")
        dom.name = json.optString("name")
        dom.isDefault = json.optInt("isDefault") == 1
        dom.isDefaultRecipient = json.optInt("isDefaultRecipient") == 1
        dom.uri = json.optString("domainuri")
        dom.systemId = externalSystemInfo?.generateId()?: ""

        // Categories
        val types = json.optJSONArray("types")
        (0..(types.length()-1)).forEach { i->
            val type = types.optJSONArray(i)
            val cat = Category()
            cat.systemId = externalSystemInfo?.generateId()?: ""
            cat.markerId = type.optString(0)
            cat.name = type.optString(1)
            cat.displayedName = cat.name
            cat.domainText = dom.name?: ""
            cat.isPrivate = type.optInt(2) == 1
            cat.needsIdentification = type.optInt(3) == 1
            cat.typeId = type.optLong(4)
            // index-5 is for attributes
            cat.hasTitle = type.optInt(6) == 1

            if (type.length() > 7) {
                cat.domainId = type.optString(7)
                cat.photoReq = type.optString(8, Category.PHOTO_REQ)
                cat.description = type.optString(9, "")
            }

            if (cat.name.contains(">")) {
                val idx = cat.name.indexOf(">")
                cat.group = cat.name.substring(0, idx)
            }

            // Attributes
            val attribs = type.optJSONArray(5)
            (0..(attribs.length()-1)).forEach {j ->
                val attr = Attribute()
                val jsonAttr = attribs.optJSONObject(j)
                attr.systemId = externalSystemInfo?.generateId()?: ""
                attr.domainId = dom.id?: ""
                attr.localId = jsonAttr.optString("id")
                attr.type = jsonAttr.optString("type")
                attr.name = jsonAttr.optString("name", "")
                attr.code = jsonAttr.optString("code")

                attr.errorText = jsonAttr.optString("error")
                attr.helpText = jsonAttr.optString("help")

                attr.visibleIfCode = jsonAttr.optString("visible_if_code")
                attr.visibleIfValue = jsonAttr.optString("visible_if_value")
                attr.requiredIfCode = jsonAttr.optString("required_if_code")
                attr.requiredIfValue = jsonAttr.optString("required_if_value")
                attr.required = jsonAttr.optInt("required", 0) == 1
                attr.shouldCache = jsonAttr.optInt("cached", 0) == 1

                if (attr.type.equals(Attribute.TYPE_VALUELIST)) {
                    val values = jsonAttr.optJSONArray("values")
                    (0..(values.length()-1)).forEach { k ->
                        val valuePair = values.getJSONObject(k)
                        attr.addValuePair(
                                valuePair.optString("id"),
                                valuePair.optString("text")
                        )
                    }
                }
                attr.id = attr.generateId()
                cat.addAttribute(attr)
            }
            dom.addCategory(cat)
        }

        return dom
    }

    override fun parseError(resp: BaseResponse): BaseResponse = resp

    override fun getUrlParam(): Map<String, String?>? {
        val map = hashMapOf(
                "appid" to appId,
                "phone" to phoneId,
                "lat" to "$mLat",
                "long" to "$mLon"
        )
        externalSystemInfo?.let { info -> map.put("appid", info.appId) }
        userCred?.let { uc -> if (uc.token.isNotEmpty()) map["authorization"] = uc.token }
        return map
    }
}
