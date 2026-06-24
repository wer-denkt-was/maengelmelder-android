package de.maengelmelder.mainmodule.objects

import android.util.Log
import org.json.JSONObject
import java.lang.NumberFormatException

class BmsDomain {

    companion object {
        fun fromJSON(json: JSONObject): BmsDomain {
            return BmsDomain().apply {
                id = json.optInt("id", 0)
                useGroups = json.optInt("use_groups", 0) == 1
                name = json.optString("name", "")
                iconSet = json.optString("icon_set", "")

                json.optJSONObject("settings")?.let { s ->
                    s.keys().forEach { k ->
                        if (k == "responsible") {
                            s.optJSONObject("responsible")?.let {
                                responsibleSettings = jsonObjToMap(it)
                            }
                        } else {
                            settings[k] = s.get(k)
                        }
                    }
                }

                settings["anon_questions"] = json.optInt("anon_questions", 0) == 1
            }
        }

        private fun jsonObjToMap(obj: JSONObject): HashMap<String, Any> {
            val hashmap = hashMapOf<String, Any>()
            obj.keys().forEach { k ->
                hashmap[k] = obj.get(k)
            }
            return hashmap
        }
    }

    var id: Int = 0
    var name: String = ""
    var useGroups: Boolean = false
    var iconSet: String = ""
    var settings: HashMap<String, Any> = hashMapOf()
    var responsibleSettings: HashMap<String, Any> = hashMapOf()

    fun getSetting(key: String, default: Int = -1) : Int {
        val value = settings[key] ?: return default
        return try { value.toString().toInt() } catch (e: NumberFormatException) { return default }
    }

    fun getSetting(key: String, default: String = "") : String {
        val value = settings[key] ?: return default
        return value.toString()
    }

    fun debugSettings(): String {
        var text = "Length = ${settings.size}\n"
        settings.keys.forEach { k ->
            text += "$k = ${settings[k]}\n"
        }
        return text
    }
}