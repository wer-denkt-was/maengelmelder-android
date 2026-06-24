package de.maengelmelder.mainmodule.objects

import android.util.Log
import de.maengelmelder.mainmodule.objects.interfaces.IdGenerable
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import kotlin.Exception

/**
 *
 * Holds category object, which is being chosen by the user in [de.maengelmelder.mainmodule.fragments.ChooseCategoryStep]
 */
class Category : Serializable, IdGenerable {

    companion object {
        /**
         * Should match the string returned by the API [de.maengelmelder.mainmodule.network.MMBMSGetDomain]
         */
        val PHOTO_REQ = "required"
        val PHOTO_OPTIONAL = "optional"
        val PHOTO_NEVER = "never"

        val POS_REQ = "required"
        val POS_OPTIONAL = "optional"
        val POS_NEVER = "never"

        fun fromJSON(catJSON: JSONObject): Category {
            return Category().apply {
                markerId = catJSON.optInt("markerid", 0).toString()
                name = catJSON.optString("name", "")
                group = catJSON.optString("group", "").let { cat ->
                    if (cat == "null") "" else cat
                }
                rubric = catJSON.optString("rubric", "")
                displayedName = name
                ordering = catJSON.optInt("ordering", 0)
                typeId = catJSON.optInt("id").toLong()
                photoReq = catJSON.optString("photo", PHOTO_OPTIONAL)
                posReq = catJSON.optString("position", POS_REQ)
                isPrivate = catJSON.optInt("private") == 1
                hasTitle = catJSON.optInt("has_title") == 1
                description = catJSON.optString("description", "")
                externalURL = catJSON.optString("external_uri", null)
                createDirectLinkOnly = catJSON.optInt("create_directlink_only", 0) == 1

                val domJSON = catJSON.optJSONObject("domain")
                domJSON?.let {
                    domainId = it.optString("id")
                    domainText = it.optString("title")
                }
                val bmsJSON = catJSON.optJSONObject("bms")
                bmsJSON?.let {
                    domainText = bmsJSON.optString("name", domainText)
                }

                val attrJSON = catJSON.optJSONArray("attributes")
                attrJSON?.let {attrs ->
                    (0 until attrs.length()).forEach { i ->
                        val singleAttr = attrs.getJSONObject(i)
                        addAttribute(Attribute.fromJSON(singleAttr, domainId.toInt()))
                    }
                }

                catJSON.optJSONArray("attributeids")?.let { attrids ->
                    (0 until attrids.length()).forEach { i ->
                        val id = attrids.optInt(i, -1)
                        if (id > 0) {
                            attrIds.add(id)
                        }
                    }
                }
                catJSON.optJSONArray("attributeids_message")?.let { attrids ->
                    (0 until attrids.length()).forEach { i ->
                        val id = attrids.optInt(i, -1)
                        if (id > 0) {
                            attrIdsMessage.add(id)
                        }
                    }
                }
                catJSON.optJSONArray("attributeids_update")?.let { attrids ->
                    (0 until attrids.length()).forEach { i ->
                        val id = attrids.optInt(i, -1)
                        if (id > 0) {
                            attrIdsUpdate.add(id)
                        }
                    }
                }
            }
        }
    }

    /**
     * @property typeId the type Id of the category, which is the actual ID that should be sent to the server
     * @property systemId the system Id of the category where it belongs
     * @property domainId the domain Id of the category where it belongs
     * @property markerId the id of the map marker
     * @property name the name of the category. It may contain ">" from [de.maengelmelder.mainmodule.network.MMBMSGetDomain]
     * @property group the name of the category group. The category list in [de.maengelmelder.mainmodule.fragments.ChooseCategoryStep] is grouped by this variable
     * @property description description of the category.
     * @property displayedName contains the category name that should be displayed in the UI
     * @property domainText contains the domain description
     * @property hasTitle whether the category requires the title of the message to be filled in or not
     * @property isPrivate whether it is a private category or not
     * @property needsIdentification whether the user has to log in before uploading a message under this category
     * @property photoReq photo requirements. It conforms to either [PHOTO_REQ], [PHOTO_OPTIONAL], or [PHOTO_NEVER]
     * @property posReq position requirements. It conforms to either [POS_REQ], [POS_NEVER], or [POS_OPTIONAL]
     * @property requirePosition whether the message under this category requires a position or not
     * @property ordering ordering for sorting
     * @property isSeen local variable. Whether the description of the category has already been seen by the user or not
     * @property attributes list of [Attribute]s under this category.
     *
     * @property attrIds list of attribute Ids
     * @property attrIdsMessage list of attribute Ids used for message creation. It's always a subset of [attrIds]
     */
    var typeId: Long = 0L
    var systemId: String = ""
    var domainId: String = ""
    var markerId: String = ""
    var name: String = ""
    var group: String = ""
    var rubric: String = ""
    var description: String = ""
    var displayedName: String = ""
    var domainText: String = ""
    var hasTitle: Boolean = false
    var isPrivate: Boolean = false
    var needsIdentification: Boolean = false
    var photoReq: String = PHOTO_OPTIONAL
    var posReq: String = POS_REQ
    var requirePosition: Boolean = true
    var ordering: Int = 0
    var isSeen: Boolean = false
    var externalURL: String? = null
    var attrIds: ArrayList<Int> = arrayListOf()
    var attrIdsMessage: ArrayList<Int> = arrayListOf()
    var attrIdsUpdate: ArrayList<Int> = arrayListOf()
    var createDirectLinkOnly: Boolean = false
    private var attributes: ArrayList<Attribute> = ArrayList()

    /**
     * The category requires at least the typeId and domainId since both of them are needed for submission
     */
    fun isValid(): Boolean = typeId != 0L && !domainId.isEmpty()

    /**
     * Match either the whole object or if both categories have the same id and domainid
     */
    fun equalToCategory(cat: Category): Boolean {
        return this == cat || (this.domainId == cat.domainId && this.typeId == cat.typeId)
    }

    /**
     * Returns the list of attribute Ids for message Update
     */
    fun getAttributeIdsForUpdate(): List<Int> {
        return attrIdsUpdate
    }

    /**
     * As long as it has systemId, domainId, and typeId, id generation should suffice
     */
    override fun generateId(): String {
        return "$systemId-$domainId-$typeId"
    }

    override fun toString(): String = "Kategorie: $name[domain=$domainId,marker=$markerId,type=$typeId]"

    fun setAttribute(attr: Array<Attribute>) {
        attributes.clear()
        attributes.addAll(attr)
    }
    fun addAttribute(attr: Attribute) { attributes.add(attr) }
    fun addAllAttributes(attr: Array<Attribute>) { attributes.addAll(attr) }
    fun iterateAttributes(f: (Attribute) -> Unit) { attributes.forEach(f) }
    fun getAttributes(): Array<Attribute> {
        return attributes.toTypedArray()
    }

    fun hasAttribute(attrId: String): Boolean = attributes.count { attr -> attr.localId == attrId } >= 1

    fun getActualName(): String {
        if (name.isEmpty() || !name.contains(">")) return name
        val idx = name.lastIndexOf(">")
        return try { name.substring(idx+1, name.length) } catch (e: Exception) { "" }
    }

    fun hasRubric(): Boolean {
        return rubric !== null && rubric !== "null" && rubric?.isNotEmpty() == true
    }

    fun hasEmailField(): Boolean {
        attributes.forEach { attr ->
            if (attr.code == "email") return true
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Category) return false
        return this.name == other.name && this.domainId == other.domainId && this.typeId == other.typeId
    }

    fun attrIdsMessageToJSONString(): String {
        val arr = JSONArray()
        attrIdsMessage.forEach { i -> arr.put(i) }
        return arr.toString()
    }

    fun attrIdsMessageFromJSONString(jsonString: String) {
        attrIdsMessage = arrayListOf()
        val jsonArr: JSONArray? = try {
            JSONArray(jsonString)
        } catch (e: Exception) {
            null
        }
        jsonArr?.let { arr ->
            (0 until arr.length()).forEach { i ->
                attrIdsMessage.add(arr.getInt(i))
            }
        }
    }
}