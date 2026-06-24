package de.maengelmelder.mainmodule.objects

import android.util.Log
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.activities.MessageProcessActivity
import de.maengelmelder.mainmodule.objects.interfaces.IdGenerable
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * BMS Message object.
 */
class Message : Serializable, IdGenerable {

    var id: String = "" // Either auto-generated through generateId() if it is from server or a generated id if self-made
    var systemId: String = ""
    var serverId: String = ""

    var category: Category = Category()

    var title: String = ""
    var state: String = ""
    var state_en: String = ""
    var colorString: String = "white"
    var distance: Double = -1.0
    var desc: String = ""
    var text: String = ""
    var createdAt: Long = -1
    var uploadedAt: Long = -1
    var street: String = ""
    var imagePaths = arrayListOf<String>()
    var uploadStatus: String = ""
    var markerUrl: String = ""
    var additional = hashMapOf<String, String>()
    var isFavorite: Boolean = false
    var internalType: String = MessageProcessActivity.TYPE_DEFECT_REPORT

    var lat: Double = Double.MAX_VALUE
    var lon: Double = Double.MAX_VALUE

    override fun generateId(): String {
        return "$systemId-${category.domainId}-$serverId"
    }

    fun getDescriptionOnly(): String {
        val idx = desc.indexOf(":")
        return if (idx == -1) desc else try { desc.substring(idx + 1, desc.length) } catch (e: Exception) { desc }
    }

    fun filterImagePaths(other: Array<String>) {
        imagePaths.removeAll(other)
    }

    fun additionalDataToJSON(): JSONObject {
        val json = JSONObject()
        additional.forEach { e -> try { json.put(e.key, e.value) } catch (e: JSONException) { } }
        return json
    }

    fun passFilter(it: MessageFilterParam): Boolean {
        // Desc
        var shouldAdd = it.text.isEmpty() || desc.lowercase().contains(it.text.lowercase())
        // Title (if any)
        if (title.isNotEmpty() && title != "null") {
            shouldAdd = shouldAdd && (it.text.isEmpty() || title.lowercase().contains(it.text.lowercase()))
        }
        // fav only (user defined settings)
        shouldAdd = shouldAdd && (!it.favoriteOnly || isFavorite)
        // category
        shouldAdd = shouldAdd && (it.category.isEmpty() || category.name.lowercase().contains(it.category.lowercase()))
        // status
        shouldAdd = shouldAdd && (it.statuses.isEmpty() || it.statuses.contains(state))
        return shouldAdd
    }

    fun isCategoryLocked(): Boolean {
        val locked = additional["force_typeid"]?: "false"
        return locked.toBoolean()
    }

    fun isLocationLocked(): Boolean {
        val locked = additional["force_loc"]?: "false"
        return locked.toBoolean()
    }

    companion object {
        val DATA_UPDATELOG = "message.updatelog"

        fun parseAdditionalData(data: String): HashMap<String, String> {
            val map = hashMapOf<String, String>()
            val json = try { JSONObject(data) } catch (e: JSONException) { null }
            json?.keys()?.forEach { k -> map[k] = json.optString(k, "") }
            return map
        }

        fun fromJson(m: JSONObject,
                     extSystemInfo: SystemInfo? = null,
        ): Message {
            val dtFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val msg = Message()
            msg.id = m.optString("id")?: ""
            msg.serverId = msg.id
            msg.systemId = extSystemInfo?.generateId()?: ""
            msg.title = m.optString("title")
            msg.desc = m.optString("text")
            msg.state = m.optString("state_german")
            msg.state_en = m.optString("state")
            msg.markerUrl = m.optString("marker_uri", "")

            val dtString = m.optString("created", "")
            msg.createdAt = try { dtFormatter.parse(dtString).time } catch (e: java.lang.Exception) { -1 }

            // Domain
            val domObj = Domain.createDefault()
            domObj.systemId = msg.systemId
            // Get the domain information
            m.optJSONObject("domain")?.let { domain ->
                val id = domain.optString("id", MMConstants.DefaultDomainId.toString())
                domObj.id = id
                domObj.name = domain.optString("title", "")
            }

            // Category
            val cat = Category()
            cat.domainId = domObj.id?: ""
            cat.systemId = msg.systemId
            m.optJSONObject("message_type")?.let { mtype ->
                cat.typeId = mtype.optLong("id")
                cat.name = mtype.optString("name")
                cat.description = mtype.optString("description")
                cat.markerId = m.optString("marker_id")
                msg.category = cat
            }

            msg.colorString = m.optString("marker_color")
            msg.lat = m.optDouble("lat", 0.0)
            msg.lon = m.optDouble("lon", 0.0)
            return msg
        }

    }

}