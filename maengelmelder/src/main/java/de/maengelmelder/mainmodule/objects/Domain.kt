package de.maengelmelder.mainmodule.objects

import android.os.Parcel
import android.os.Parcelable
import de.maengelmelder.mainmodule.MMConstants
import org.json.JSONObject
import java.io.Serializable
import java.lang.Exception

class Domain (domId: String? = null) : Serializable {
    companion object {
        const val DescTextLimit = "bmsTextLimit"
        const val DescTextLimitWarning = "bmsLimitWarning"

        fun createDefault(): Domain = Domain(MMConstants.DefaultDomainId.toString()).apply {
            name = MMConstants.DefaultDomainName
        }
    }

    var id: String? = domId
    var systemId: String = ""
    var uri: String? = null
    var name: String? = null
    var isDefault: Boolean = false
    var isDefaultRecipient: Boolean = false
    var settings = hashMapOf<String, Any>()
    var bmsUrl = "" // TODO not saved in DB yet since it's only required for login page

    private var mCategories: ArrayList<Category> = ArrayList()

    fun settingsToJson(): JSONObject {
        return JSONObject().apply {
            settings.forEach { entry -> put(entry.key, entry.value) }
        }
    }

    fun settingsFromJson(jsonString: String) {
        (try { JSONObject(jsonString) } catch (e: Exception) { null })?.let { json ->
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                settings[key] = json.opt(key)
            }
        }
    }

    fun addCategory(cat: Category) = mCategories.add(cat)

    fun addCategories(vararg cat: Category) = mCategories.addAll(cat)

    fun iterateCategories(func: (Category) -> Unit) = mCategories.forEach(func)

    fun hasCategories(): Boolean = mCategories.isNotEmpty()

    fun categoriesAsArray(): Array<Category> = mCategories.toTypedArray()
}
