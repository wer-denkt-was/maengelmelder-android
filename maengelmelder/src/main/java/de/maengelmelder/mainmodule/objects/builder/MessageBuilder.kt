package de.maengelmelder.mainmodule.objects.builder

import android.content.Context
import android.net.Uri
import android.util.Patterns
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.objects.Attribute
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Log
import de.maengelmelder.mainmodule.objects.Message
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.Serializable
import java.util.*

/**
 *
 * [MessageBuilder] is used to construct new [Message] object or attach existing one.
 */
class MessageBuilder(msg: Message? = null) : Serializable {

    /**
     * Enumeration used to describe the steps needed in order to complete a message
     */
    enum class STEP {
        PHOTO, LOCATION, CATEGORY, ATTRIBUTE
    }

    /**
     * @property mMessage object
     */
    private var mMessage = Message()

    /**
     * @property mappedAttributeValues Values of the attributes filled by the user.
     * The key represents the [de.maengelmelder.mainmodule.objects.Attribute.id]
     */
    private var mappedAttributeValues: HashMap<String, Any?> = HashMap()

    init {
        // Initialize the message by setting its ID
        if (msg == null) {
            mMessage.id = UUID.randomUUID().toString().replace("-", "").uppercase()
        } else {
            mMessage = msg
        }
    }

    /**
     * Local property that shows true when a new position from [de.maengelmelder.mainmodule.fragments.ChooseLocationStep] is recorded
     */
    var isLocationChanged: Boolean = false

    /**
     * Setter-Getter for message Id
     */
    var messageId: String
        get() = mMessage.id
        private set(value) { mMessage.id = value }

    /**
     * Setter-Getter for title
     */
    var title: String
        get() = mMessage.title
        set(value) { mMessage.title = value }

    /**
     * Setter-getter for description
     */
    var description: String
        get() = mMessage.desc
        set(value) { mMessage.desc = value }

    /**
     * Setter-getter for Category.
     */
    var category: Category
        get() = message.category
        set(value) { message.category = value }

    /**
     * Setter-getter for Message object
     */
    var message: Message
        get() = mMessage
        set(value) { mMessage = value }

    /**
     * Adds attribute key-value pairs to the message. These are the answers to the form by the user
     *
     * @param key [de.maengelmelder.mainmodule.objects.Attribute.id]
     * @param value the answer to the question related to the attribute
     */
    fun addAttributeValue(key: String, value: Any?) = mappedAttributeValues.put(key, value)

    /**
     * Get the attribute value with the defined key
     *
     * @param key [de.maengelmelder.mainmodule.objects.Attribute.id]
     * @return the value of the given key
     * @see HashMap.get
     */
    fun getAttributeValue(key: String): Any? = mappedAttributeValues.get(key)

    /**
     * Returns true if there are no records inside the map
     *
     * @return true if it is empty, false otherwise
     * @see HashMap.isEmpty
     */
    fun areAttributeValuesEmpty(): Boolean = mappedAttributeValues.isEmpty()

    /**
     * parse a given [JSONArray] to populate the key-values Attributes.
     * The [JSONArray] should have the structure like "[{id:'...', value: '...'}, ...]"
     *
     * @param arr [JSONArray]
     */
    fun attributeValuesFromJson(arr: JSONArray) {
        for (i in 0..arr.length()-1) {
            val item = arr.getJSONObject(i)
            val value = item.opt("value")
            if (value is JSONArray) {
                val arrayedValue = arrayListOf<String>()
                (0 until value.length()).forEach { x ->
                    arrayedValue.add(value.optString(x));
                }
                mappedAttributeValues[item.optString("id")] = arrayedValue.toTypedArray()
            } else {
                mappedAttributeValues[item.optString("id")] = value
            }
        }
    }

    /**
     * Transform the map of attributes to JSONObject
     * The format matches the one required for uploading
     */
    fun attributeToJsonObject(): JSONObject {
        val obj = JSONObject()

        attributes.forEach { entry ->
            val id = entry.key

            val casted = when(entry.value) {
                is String -> JSONArray().put(entry.value.toString())
                is Int -> JSONArray().put(entry.value.toString().toInt())
                is Boolean -> JSONArray().put(entry.value.toString().toBoolean())
                else -> JSONArray(entry.value)
            }

            obj.put(id, casted)
        }

        return obj
    }

    /**
     * Returns the list of Attribute values as a [Map]
     */
    val attributes: Map<String, Any?> get() = mappedAttributeValues.toMap()

    /**
     * Adds additional metadata to the [Message]. These metadata can be used to add / take extra information without
     * changing the database structure
     *
     * @param key key
     * @param value value
     */
    fun addAdditionalData(key: String, value: String) {
        message.additional[key] = value
    }

    /**
     * Check if message contain additional data by key
     */
    fun hasAdditionalData(key: String) : Boolean {
        return message.additional.containsKey(key)
    }

    /**
     * Returns the additional data saved by key
     */
    fun getAdditionalData(key: String): String? {
        return message.additional[key]
    }

    /**
     * Removes a metadata by its given key
     *
     * @param key key
     */
    fun removeAdditionalData(key: String) {
        message.additional.remove(key)
    }

    fun isAttributeForced(attrId: Long): Boolean {
        return message.additional["${attrId}_force"]?.toBoolean()?: false
    }

    fun debugAdditionalData() {
        message.additional.forEach { pair ->
            android.util.Log.d("MessageBuilder", "key: ${pair.key}, value: ${pair.value}")
        }
    }

    fun isLocationLocked(): Boolean {
        return message.isLocationLocked()
    }

    fun isCategoryLocked(): Boolean {
        return message.isCategoryLocked()
    }

    fun isMessageFromQR(): Boolean {
        return getAdditionalData("from_qr_code")?.toBoolean()?: false
    }

    /**
     * Encode the list of attribute values as [JSONArray]. Reverse from [attributeValuesFromJson]
     *
     * @return The [JSONArray] containing the attribute values.
     */
    fun getAttributesAsJson(): JSONArray {
        val json = JSONArray()
        for ((key, value) in mappedAttributeValues) {
            val item = JSONObject()
                    .put("id", key)
                    .put("value", if (value is Array<*>) JSONArray(value) else value)
            json.put(item)
        }
        return json
    }

    /**
     * Check whether all the required attributes are filled out.
     * It requires [category] to not be null and contains the list of [de.maengelmelder.mainmodule.objects.Attribute]s related to that
     * category. Non-mandatory attributes ([de.maengelmelder.mainmodule.objects.Attribute.required] = false) are skipped
     *
     * @return true if all required attributes have non-null values. False otherwise
     */
    fun areAttributeValuesFilled() : Boolean {
        if (!category.isValid()) return false
        var filled = true
        category.iterateAttributes { attr ->
            if (filled && attr.required) {
                if (category.attrIdsMessage.isNotEmpty() && !category.attrIdsMessage.contains(attr.localId.toInt())) {
                    // Since attributes that aren't included in message creation don't need checks, we return true immediately
                    filled = true
                } else {
                    val value = mappedAttributeValues[attr.localId]
                    filled = when (value) {
                        null -> {
                            false
                        }
                        is String -> {
                            val v = value.toString()
                            if (attr.type == Attribute.TYPE_EMAIL) {
                                !v.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(v).matches()
                            } else {
                                !v.isEmpty()
                            }
                        }
                        else -> {
                            true
                        }
                    }
                }
            }
        }
        return filled
    }

    /**
     * Sets the location property of the message
     * @param lat latitude
     * @param lon longitude
     * @see [Message.lat]
     * @see [Message.lon]
     */
    fun setLocation(lat: Double, lon: Double) {
        mMessage.lat = lat
        mMessage.lon = lon
    }

    /**
     * Returns true if description is not empty. Will be ignored if [MMConstants.HideDescripton] is set to true
     */
    fun isDescriptionValid(): Boolean = MMConstants.HideDescripton || description != ""

    /**
     * Returns true if the [type] and [typeid] are not null or empty. Both these properties are needed to be submitted through the API
     */
    fun isCategoryValid() : Boolean = category.isValid()

    /**
     * Returns the location in a [Pair] or longitude-latitude
     */
    fun getLocation(): Pair<Double, Double> = Pair(mMessage.lon, mMessage.lat)

    /**
     * Returns true if the latitude and longitude values are not [Double.MAX_VALUE]
     */
    fun isLocationValid(): Boolean =
            mMessage.lat != Double.MAX_VALUE && mMessage.lon != Double.MAX_VALUE &&
                    mMessage.lon != 0.0 && mMessage.lat != 0.0

    /**
     * Adds an image path
     *
     * @param filePath valid, absolute image path
     */
    fun addImagePath(filePath: String?) {
        if (filePath != null) mMessage.imagePaths.add(filePath)
    }

    /**
     * Remove all images, optionally remove them also from storage
     */
    fun clearImages(removeFromDisk: Boolean = true) {
        if (removeFromDisk) {
            mMessage.imagePaths.forEach { path ->
                try { File(path).delete() } catch (e: Exception) { }
            }
        }
        mMessage.imagePaths.clear()
    }

    /**
     * Remove an image path by its Id
     *
     * @param idx index
     */
    fun removeImagePath(idx: Int): String = mMessage.imagePaths.removeAt(idx)

    /**
     * Remove an image path by its name
     *
     * @param path the image path. It must match one of the entries in order to be removed
     */
    fun removeImagePath(path: String) = mMessage.imagePaths.remove(path)

    /**
     * Remove an array of image paths. Similar rule to [removeImagePath]
     *
     * @param paths array of image paths to be removed
     */
    fun removeImagePaths(paths: Array<String>) = mMessage.filterImagePaths(paths)

    /**
     * Get an image path by its index
     *
     * @param idx index
     */
    fun getImagePath(idx: Int): String = try { mMessage.imagePaths[idx] } catch (e: Exception) { "" }

    /**
     * Get the number of image paths stored
     *
     * @return number of image paths
     */
    fun getNumOfImages(): Int = mMessage.imagePaths.size

    /**
     * Iterate through the list of image paths and apply the given method
     *
     * @param f function to be executed on every image path
     */
    fun iterateImagePaths(f: (String) -> Unit) = mMessage.imagePaths.forEach(f)

    /**
     * Returns true if it has at least 1 image path.
     */
    fun hasImage(): Boolean =
            try { mMessage.imagePaths.size > 0 } catch (e: Exception) { false }

    /**
     * Returns the list of image paths as string of image paths separated by ';'
     */
    fun getPhotoPathsAsString(): String = pathListToString(mMessage.imagePaths.toTypedArray())

    /**
     * Returns an array of pair [STEP] and [Boolean].
     * If the boolean value is true, that means that user has completed the [STEP] for this message
     * false means that there is missing information needed for that [STEP]
     */
    fun getStatus(): Array<Pair<STEP, Boolean>> {
        return arrayOf(
                Pair(STEP.PHOTO, when (category.photoReq) {
                    Category.PHOTO_REQ -> hasImage()
                    else -> true
                }),
                Pair(STEP.LOCATION, isLocationValid()),
                Pair(STEP.CATEGORY, isCategoryValid()),
                Pair(STEP.ATTRIBUTE, areAttributeValuesFilled() && isDescriptionValid())
        )
    }

    /**
     * Returns the total size of files uploaded for this message
     * This ONLY WORKS for local files. It will return 0 for server messages
     */
    fun getTotalPhotoSizeMB(c: Context): Float {
        if (!hasImage()) return 0.0f
        return mMessage.imagePaths.fold(0.0f) { init, value ->
            if (value.startsWith("content://")) {
                try {
                    val descriptor = c.contentResolver.openAssetFileDescriptor(Uri.parse(value), "r")
                    val size = descriptor?.length?: 0
                    descriptor?.close()
                    init + size.toFloat() / (1024f * 1024f)
                } catch (e: Exception) {
                    init
                }
            } else {
                val file = File(value)
                if (file.exists()) {
                    init + file.length().toFloat() / (1024f * 1024f)
                } else {
                    init
                }
            }
        }
    }

    companion object {
        fun pathListToString(paths: Array<String>): String =
            when (paths.size) {
                0 -> ""
                1 -> paths[0]
                else -> {
                    val totalPath = StringBuilder()
                    paths.forEachIndexed { idx, path ->
                        totalPath.append(path)
                        if (idx < paths.size - 1) totalPath.append(";")
                    }
                    totalPath.toString()
                }
            }
    }
}