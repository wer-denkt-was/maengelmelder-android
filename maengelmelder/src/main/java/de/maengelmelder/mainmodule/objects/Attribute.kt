package de.maengelmelder.mainmodule.objects

import de.maengelmelder.mainmodule.objects.interfaces.IdGenerable
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.lang.Exception

class Attribute : Serializable, IdGenerable {

    companion object {
        val TYPE_TEXT = "text"
        val TYPE_TEXTAREA = "textarea"
        val TYPE_EMAIL = "email"
        val TYPE_NUMBER = "number"
        val TYPE_VALUELIST = "valuelist"
        val TYPE_CHECKBOX = "checkbox"

        fun fromJSON(jsonAttr: JSONObject, domainId: Int = -1) : Attribute {
            return Attribute().apply {
                val attr = this
                attr.localId = jsonAttr.optString("id")
                attr.domainId = domainId.toString()
                attr.type = jsonAttr.optString("type")
                attr.name = jsonAttr.optString("name", "")
                attr.code = jsonAttr.optString("code")
                attr.multiselect = jsonAttr.optInt("multiselect", 0) == 1

                attr.errorText = jsonAttr.optString("error")
                attr.helpText = jsonAttr.optString("help")

                attr.ordering = jsonAttr.optInt("ordering", 0)
                attr.visibleIfCode = jsonAttr.optString("visible_if_code")
                attr.visibleIfValue = jsonAttr.optString("visible_if_value")
                attr.requiredIfCode = jsonAttr.optString("required_if_code")
                attr.requiredIfValue = jsonAttr.optString("required_if_value")
                attr.required = jsonAttr.optInt("required", 0) == 1
                attr.shouldCache = jsonAttr.optInt("cached", 0) == 1
                attr.maxLength = jsonAttr.optInt("max_length", 0)

                if (attr.type == Attribute.TYPE_VALUELIST) {
                    jsonAttr.optJSONArray("values")?.let { values ->
                        (0 until values.length()).forEach { k ->
                            val valuePair = values.getJSONObject(k)
                            attr.addValuePair(
                                valuePair.optString("id"),
                                valuePair.optString("text")
                            )
                        }
                    }

                }
                attr.id = attr.generateId()
            }
        }
    }

    var systemId = ""
    var domainId = ""
    var localId = "" // Use this one to submit messages to the system
    var id: String? = null // Use this for local DB primary key. Should have the same value as generateId()

    var name: String? = null
    var type: String? = null
    var code: String? = null
    private var values: ArrayList<Pair<String, String>> = ArrayList()

    var choices: ArrayList<Pair<String, String>>
        get() = values
        set(value) { values = value }

    var public: Boolean = false
    var multiselect: Boolean = false
    var required: Boolean = false
    var shouldCache: Boolean = false
    var requiredIfCode: String? = null
    var requiredIfValue: String? = null
    var visibleIfValue: String? = null
    var visibleIfCode: String? = null
    var maxLength: Int? = null
    var ordering: Int = 0

    var errorText: String? = null
    var helpText: String? = null

    fun addValuePair(key: String, value: String) {
        values.add(Pair(key, value))
    }

    fun choicesNameToStringArray(): Array<String> {
        return values.map { pair -> pair.second }.toTypedArray()
    }

    fun choicesValueToStringArray(): Array<String> {
        return values.map { pair -> pair.first }.toTypedArray()
    }

    fun choicesToJsonArray(): JSONArray {
        return JSONArray().apply {
            values.forEach { v ->
                put(JSONObject().apply {
                    put("id", v.first)
                    put("value", v.second)
                })
            }
        }
    }

    fun choicesFromJson(json: String) {
        values = ArrayList()
        (try { JSONArray(json) } catch (e: Exception) { null })?.let { j ->
            (0 until j.length()).forEach { idx ->
                j.optJSONObject(idx)?.let { item ->
                    val id = item.optString("id")
                    val value = item.optString("value")
                    values.add(Pair(id, value))
                }
            }
        }
    }

    override fun generateId(): String {
        // Should be used for primary key inside database
        return "$systemId-$domainId-$localId"
    }
}