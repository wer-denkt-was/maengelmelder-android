package de.maengelmelder.mainmodule.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.preference.PreferenceManager
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.activities.MessageProcessActivity
import de.maengelmelder.mainmodule.objects.*
import org.json.JSONArray
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Local database used to store messages, attributes, categories, etc. It uses native SQlite API
 *
 * To alter table structure (adding/removing columns, etc.):
 * 1. Add/remove the column in the table inside [onCreate]
 * 2. Increment the database version in main/res/values/settings.xml (mm_db_version)
 * 3. in [MMDBMigrator], add another index to [MMDBMigrator.mPatchList] with the SQL-command required to alter the table. See the previous migrations
 *
 */
internal class MMDB(const: MMDBConstants) : SQLiteOpenHelper(const.context, const.DB_NAME, null, const.DB_VERSION) {

    companion object {
        /**
         * Singleton instance of the database
         */
        private var instance: MMDB? = null

        @Synchronized
        fun instance(c: Context): MMDB {
            if (instance == null) {
                instance = MMDB(MMDBConstants(c))
            }
            return instance!!
        }
    }

    /**
     * Constants containing the names and columns for the tables
     */
    val constants = const

    override fun onCreate(db: SQLiteDatabase) {
        // Systems table
        MMDBUtils.createTable(db, constants.TBL_SYSTEM, true,
                constants.COL_ID to TEXT + PRIMARY_KEY,
                constants.COL_APP_ID to INTEGER + DEFAULT("1"),
                constants.COL_NAME to TEXT,
                constants.COL_DOMAIN_URL to TEXT,
                constants.COL_ISPRIVATE to INTEGER + DEFAULT("1")
        )

        // Domain table. 1 system may have multiple domains
        MMDBUtils.createTable(db, constants.TBL_DOMAINS, true,
                constants.COL_ID to TEXT + PRIMARY_KEY, // Domain Id
                constants.COL_SYSTEM_ID to TEXT, // System Id
                constants.COL_NAME to TEXT,
                constants.COL_DOMAIN_URL to TEXT,
                constants.COL_ISDEFAULT to INTEGER + DEFAULT("0"),
                constants.COL_ISDEFAULT_RECIPIENT to INTEGER + DEFAULT("0"),
                constants.COL_EXTRASJSON to TEXT
        )

        // Message table
        MMDBUtils.createTable(db, constants.TBL_MESSAGES, true,
                constants.COL_ID to TEXT + PRIMARY_KEY, // Either auto-generated for local msg or the result of generateId() if it comes from server
                constants.COL_SERVER_ID to TEXT, // Actual id from server, specific to a domain and a system
                constants.COL_DOMAIN_ID to TEXT, // Domain specific to a system
                constants.COL_SYSTEM_ID to TEXT,
                constants.COL_CAT_ID to TEXT, // foreign key to Category table

                constants.COL_LAT to REAL,
                constants.COL_LON to REAL,
                constants.COL_STATE to TEXT,
                constants.COL_STATE_EN to TEXT,
                constants.COL_COLOR to TEXT,
                constants.COL_TITLE to TEXT,
                constants.COL_DESC to TEXT,
                constants.COL_TEXT to TEXT,
                constants.COL_ORIGIN to TEXT,
                constants.COL_EXTRASJSON to TEXT,
                constants.COL_PHOTO_PATH to TEXT,
                constants.COL_MARKER_URL to TEXT + DEFAULT("''"),
                constants.COL_UPLOAD_STATUS to TEXT,
                constants.COL_INTERNAL_TYPE to TEXT + DEFAULT("defect_report"), // defect report or idea
                constants.COL_ADDITIONAL to TEXT, // This column should contain a JSON structure for any kind of additional data without breaking schema
                constants.COL_UPLOADED_AT to TEXT, // Need to be converted to long for timestamp
                constants.COL_CREATEDAT to TEXT, // Need to be converted to long for timestamp
                constants.COL_MARK_FAV to INTEGER + DEFAULT("0")
        )

        // Logs
        MMDBUtils.createTable(db, constants.TBL_LOGS, true,
                constants.COL_ID to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                constants.COL_CREATEDAT to TEXT,
                constants.COL_ADDITIONAL to TEXT // This column should contain a JSON structure
        )

        // Categories
        MMDBUtils.createTable(db, constants.TBL_CATEGORIES, true,
                constants.COL_ID to TEXT + PRIMARY_KEY, // Generated id since 2 categories can have the same domain and id
                constants.COL_TYPEID to TEXT, // The actual id specific to a system and a domain
                constants.COL_DOMAIN_ID to TEXT, // The domain Id specific to a system
                constants.COL_SYSTEM_ID to TEXT + DEFAULT("''"),

                constants.COL_NAME to TEXT + DEFAULT("''"),
                constants.COL_GROUP to TEXT + DEFAULT("''"),
                constants.COL_DESC to TEXT + DEFAULT("''"),
                constants.COL_DISPNAME to TEXT + DEFAULT("''"),
                constants.COL_MARKER_ID to TEXT + DEFAULT("''"),
                constants.COL_HASTITLE to INTEGER + DEFAULT("''"), // 1 is true, 0 is false. Default = 0
                constants.COL_ISPRIVATE to INTEGER + DEFAULT("''"), // 1 is true, 0 is false. Default = 0
                constants.COL_NEEDSID to INTEGER + DEFAULT("''"), // 1 is true, 0 is false. Default = 0
                constants.COL_REQUIRE_PHOTO to TEXT + DEFAULT("'${Category.PHOTO_OPTIONAL}'"),
                constants.COL_REQUIRE_POSITION to TEXT + DEFAULT("'${Category.POS_REQ}'"),
                constants.COL_ISSEEN to TEXT + DEFAULT("0"),
                constants.COL_EXTERNAL_URL to TEXT + DEFAULT("''"),
                constants.COL_RUBRIK to TEXT + DEFAULT("''"),
                constants.COL_ATTRIDS_FOR_CREATION to TEXT + DEFAULT("''"),
                constants.COL_DIRECTLINK_ONLY to INTEGER + DEFAULT("0")
        )

        // Attributes
        MMDBUtils.createTable(db, constants.TBL_ATTRIBUTES, true,
                constants.COL_ID to TEXT + PRIMARY_KEY, // Type ID of the Category. Just convert to Long
                constants.COL_ATTR_ID to TEXT, // The actual attribute Id local to a domain and system
                constants.COL_SYSTEM_ID to TEXT, // System id where this attribute belongs to
                constants.COL_DOMAIN_ID to TEXT, // Domain id where this attribute belongs to

                constants.COL_NAME to TEXT,
                constants.COL_TYPE to TEXT,
                constants.COL_CODE to TEXT,
                constants.COL_ORDERING to INTEGER + DEFAULT("0"),
                constants.COL_MULTISELECT to INTEGER + DEFAULT("0"),
                constants.COL_ISPUBLIC to INTEGER, // 1 is true, 0 is false. Default = 0
                constants.COL_ISREQ to INTEGER, // 1 is true, 0 is false. Default = 0
                constants.COL_SHOULDCACHE to INTEGER, // 1 is true, 0 is false. Default = 0
                constants.COL_MAXLENGTH to INTEGER + DEFAULT("0"), // default = 0
                constants.COL_REQ_IF_CODE to TEXT,
                constants.COL_REQ_IF_VALUE to TEXT,
                constants.COL_VIS_IF_CODE to TEXT,
                constants.COL_VIS_IF_VALUE to TEXT,
                constants.COL_ERRTEXT to TEXT,
                constants.COL_HELPTEXT to TEXT,
                constants.COL_VALUELIST to TEXT + DEFAULT("''") // Values list in JSON array (spinner)
        )

        // Relationship table between category and attribute
        MMDBUtils.createTable(db, constants.TBL_CAT_ATTR, true,
                constants.COL_CAT_ID to TEXT,
                constants.COL_ATTR_ID to TEXT
        )

        // Geometry
        MMDBUtils.createTable(db, constants.TBL_GEOMETRY, true,
                constants.COL_SYSTEM_ID to TEXT,
                constants.COL_DOMAIN_ID to TEXT,
                constants.COL_GEOMID to INTEGER,
                constants.COL_GEOMPARTID to INTEGER,
                constants.COL_WKT to TEXT,
                constants.COL_MIN_LAT to REAL,
                constants.COL_MAX_LAT to REAL,
                constants.COL_MIN_LON to REAL,
                constants.COL_MAX_LON to REAL,
                constants.COL_CAT_ID to TEXT // Comma-separated category IDs
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        var completeAllPatches = true
        // Apply patches for each version. If one patch does not exists
        (oldV until newV).forEach { version ->
            val patch = MMDBMigrator.getPatch(version)
            if (patch == null || patch.shouldRebuildTables()) {
                completeAllPatches = false
                return@forEach
            } else {
                patch.apply(db)
            }
        }

        if (!completeAllPatches) {
            dropAllTables(db)
            onCreate(db)
        }
    }

    /**
     * Drop all tables and the contents in it
     */
    private fun dropAllTables(db: SQLiteDatabase?) {
        db?.let { dbobj ->
            MMDBUtils.dropTable(dbobj, constants.TBL_SYSTEM, true)
            MMDBUtils.dropTable(dbobj, constants.TBL_DOMAINS, true)
            MMDBUtils.dropTable(dbobj, constants.TBL_MESSAGES, true)
            MMDBUtils.dropTable(dbobj, constants.TBL_CATEGORIES, true)
            MMDBUtils.dropTable(dbobj, constants.TBL_ATTRIBUTES, true)
            MMDBUtils.dropTable(dbobj, constants.TBL_CAT_ATTR, true)
            MMDBUtils.dropTable(dbobj, constants.TBL_LOGS, true)
            MMDBUtils.dropTable(dbobj, constants.TBL_GEOMETRY, true)
        }
    }

    /**
     * Add logs
     *
     * @param type log type. Refer to constants from [Log] class
     * @param data Extra data. you can use pre-defined constants from [Log] class or use your own.
     * @see Log
     */
    fun addLog(type: String, data: HashMap<String, String>) {
        data[Log.KEY_TYPE] = type
        writableDatabase.let {
            MMDBUtils.insert(
                it,
                constants.TBL_LOGS,
                constants.COL_CREATEDAT to System.currentTimeMillis().toString(),
                constants.COL_ADDITIONAL to Log.mapToJson(data).toString()
            )
        }
    }

    /**
     * Get the list of logs
     *
     * @param minTS the minimum time. The retrieved logs will have timestamps after the provided one. Value lower than 0 will be ignored
     *
     * @return list of logs
     */
    fun getLogs(minTS: Long = -1L): List<Log> {
        val logs = arrayListOf<Log>()
        readableDatabase.let {
            val cursor = it.rawQuery("SELECT * FROM ${constants.TBL_LOGS}", null)
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    val l = cursorToLog(cursor)
                    val canAdd = if (minTS > 0) l.timestamp > minTS else true
                    if (canAdd) logs.add(l)
                    cursor.moveToNext()
                }
            }
            cursor.close()
        }
        return logs
    }

    /**
     * Add [Category] entry. It will replace existing one
     *
     * @param cat Category
     */
    fun addCategory(cat: Category, db: SQLiteDatabase? = null) {
        val dbObj = db?: writableDatabase
        val id = cat.generateId()
        val existing = getCategory(id)
        val values = arrayOf(
                constants.COL_ID to id,
                constants.COL_DOMAIN_ID to cat.domainId,
                constants.COL_SYSTEM_ID to cat.systemId,
                constants.COL_TYPEID to cat.typeId,
                constants.COL_NAME to cat.name,
                constants.COL_GROUP to cat.group,
                constants.COL_DESC to cat.description,
                constants.COL_DISPNAME to cat.displayedName,
                constants.COL_RUBRIK to cat.rubric,
                constants.COL_MARKER_ID to cat.markerId,
                constants.COL_HASTITLE to if (cat.hasTitle) "1" else "0",
                constants.COL_ISPRIVATE to if (cat.isPrivate) "1" else "0",
                constants.COL_REQUIRE_PHOTO to cat.photoReq,
                constants.COL_REQUIRE_POSITION to cat.posReq,
                constants.COL_NEEDSID to if (cat.needsIdentification) "1" else "0",
                constants.COL_DIRECTLINK_ONLY to if (cat.createDirectLinkOnly) "1" else "0",
                constants.COL_ATTRIDS_FOR_CREATION to cat.attrIdsMessageToJSONString()
        )
        if (existing == null) {
            dbObj.let {
                MMDBUtils.replace(it, constants.TBL_CATEGORIES, *values)
            }
        } else {
            updateCategory(dbObj, id, *values)
        }
    }

    /**
     * update existing [Category] object or insert if it does not exist. Since REPLACE will remove the existing record,
     * fields that were present before would be wiped back to default if it is not included in the updated values.
     *
     * Similar to [addCategory] with [Category] as parameter, but this allows partial column values
     */
    fun addCategory(catId: String, vararg members: Pair<String, String>) {
        val cat = getCategory(catId)
        if (cat == null) {
            writableDatabase.let {
                MMDBUtils.insert(it, constants.TBL_CATEGORIES, *members)
            }
        } else {
            updateCategory(null, catId, *members)
        }
    }

    /**
     * Update the entry of Category by identified its [MMDBConstants.COL_ID]
     *
     * @param catId category Id
     * @param updated column and values to be updated
     *
     */
    fun updateCategory(db: SQLiteDatabase? = null,
                       catId: String,
                       vararg updated: Pair<String, Any>,) {
        val useDB = db?: writableDatabase
        useDB.let {
            it.update(
                constants.TBL_CATEGORIES,
                updated.toContentValues(),
                "${constants.COL_ID} = '$catId'",
                null
            )
        }
    }

    /**
     * Add entry to attribute-category relational table
     *
     * @param attrId attribute Id in string
     * @param carID category ID in string
     */
    fun addAttrCatRelation(attrId: String, catId: String, db: SQLiteDatabase? = null) {
        val dbObj = db?: writableDatabase
        dbObj.let {
            MMDBUtils.replace(
                it,
                constants.TBL_CAT_ATTR,
                constants.COL_CAT_ID to catId,
                constants.COL_ATTR_ID to attrId
            )
        }
    }

    /**
     * Insert attribute. Handles duplicate
     *
     * @param attr Attribute
     */
    fun addAtribute(attr: Attribute, db: SQLiteDatabase? = null) {
        val dbObj = db?: writableDatabase
        dbObj.let {
            MMDBUtils.replace(
                it,
                constants.TBL_ATTRIBUTES,
                constants.COL_ID to attr.generateId(),
                constants.COL_SYSTEM_ID to attr.systemId,
                constants.COL_DOMAIN_ID to attr.domainId,
                constants.COL_ATTR_ID to attr.localId,
                constants.COL_NAME to attr.name,
                constants.COL_TYPE to attr.type,
                constants.COL_CODE to attr.code,
                constants.COL_ORDERING to attr.ordering,
                constants.COL_ISPUBLIC to if (attr.public) "1" else "0",
                constants.COL_ISREQ to if (attr.required) "1" else "0",
                constants.COL_SHOULDCACHE to if (attr.shouldCache) "1" else "0",
                constants.COL_REQ_IF_CODE to attr.requiredIfCode,
                constants.COL_REQ_IF_VALUE to attr.requiredIfValue,
                constants.COL_VIS_IF_CODE to attr.visibleIfCode,
                constants.COL_VIS_IF_VALUE to attr.visibleIfValue,
                constants.COL_MULTISELECT to if (attr.multiselect) "1" else "0",
                constants.COL_ERRTEXT to attr.errorText,
                constants.COL_HELPTEXT to attr.helpText,
                constants.COL_VALUELIST to attr.choicesToJsonArray().toString()
            )
        }
    }

    /**
     * Get category with the given ID. You can also include the list of attributes associated with the category by setting
     * [withAttribs] to true
     *
     * @param id id of the Category
     * @param withAttribs if true, it will also retrieve the attributes associated with the category
     *
     * @return [Category] object or null if it does not exist
     */
    fun getCategory(id: String, withAttribs: Boolean = false): Category? {
        var cat : Category? = null

        readableDatabase.let {
            val cursor = it.rawQuery(
                "SELECT * FROM ${constants.TBL_CATEGORIES} WHERE ${constants.COL_ID} = '$id'",
                null
            )
            if (cursor.moveToFirst()) {
                cat = cursorToCategory(cursor)
                if (withAttribs) {
                    cat?.addAllAttributes(getAttributesByCategoryId(id, it))
                }
            }
            cursor.close()
        }
        return cat
    }

    /**
     * Retrieves all categories from the given system ID and domain ID
     */
    fun getCategoriesFromDomain(systemId: String,
                                domainId: String,
                                withAttribs: Boolean = false,
                                db: SQLiteDatabase? = null): List<Category> {
        val dbObject = db?: readableDatabase
        val categories: ArrayList<Category> = arrayListOf()
        dbObject.let {
            val cursor = it.rawQuery(
                "SELECT * FROM ${constants.TBL_CATEGORIES} WHERE ${constants.COL_SYSTEM_ID} = '$systemId' AND ${constants.COL_DOMAIN_ID} = '$domainId'",
                null
            )
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    val cat = cursorToCategory(cursor)
                    if (withAttribs) {
                        val attrs = getAttributesByCategoryId(cat.generateId(), it)
                        cat.addAllAttributes(attrs)
                    }
                    categories.add(cat)
                    cursor.moveToNext()
                }
            }
            cursor.close()
        }
        return categories
    }

    /**
     * Deletes all categories, attributes, and their relations from the database with the given domain and system ID
     */
    fun removeCategoriesAndAttributesFromDomain(
        systemId: String,
        domainId: String,
        db: SQLiteDatabase? = null) {

        val dbObj = db?: writableDatabase
        dbObj.let {
            val categories = getCategoriesFromDomain(systemId, domainId, true, it)
            categories.forEach { cat ->
                val catId = cat.generateId()
                cat.iterateAttributes { attr ->
                    val attrId = attr.generateId()

                    // Remove the relation first
                    MMDBUtils.delete(it, constants.TBL_CAT_ATTR,
                        "${constants.COL_CAT_ID} = \'$catId\' "+
                                "AND ${constants.COL_ATTR_ID} = \'$attrId\'")

                    // Remove the attribute
                    MMDBUtils.delete(it, constants.TBL_ATTRIBUTES, "${constants.COL_ID} = \'$attrId\'")
                }
                // Remove the category
                MMDBUtils.delete(it, constants.TBL_CATEGORIES, "${constants.COL_ID} = \'$catId\'")
            }
        }
    }

    /**
     * Returns true if the category is dirty (the description has been seen by the user)
     */
    fun isCategorySeen(catId: String): Boolean {
        var isSeen = false
        writableDatabase.let {
            val cursor = it.rawQuery(
                "SELECT ${constants.COL_ISSEEN} FROM ${constants.TBL_CATEGORIES} WHERE ${constants.COL_ID} = '$catId'",
                null
            )
            if (cursor.moveToFirst()) {
                isSeen = cursor.getInt(cursor.getColumnIndex(constants.COL_ISSEEN)) == 1
            }
            cursor.close()
        }
        return isSeen
    }

    /**
     * Get all attributes related to the category defined by [catId]
     *
     * @param catId id of Category
     *
     * @return Array of [Attribute] related to the category.
     *          Empty array if not attributes found
     */
    fun getAttributesByCategoryId(catId: String, db: SQLiteDatabase? = null): Array<Attribute> {
        val sb = StringBuilder()
        val attrs = ArrayList<Attribute>()
        val dbObject = db?: readableDatabase
        dbObject.let {
            val cursor = it.rawQuery(
                "SELECT ${constants.COL_ATTR_ID} FROM ${constants.TBL_CAT_ATTR} WHERE ${constants.COL_CAT_ID} = '$catId'",
                null
            )
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    sb.append("\'${cursor.getString(0)}\'")
                    if (!cursor.isLast) sb.append(",")
                    cursor.moveToNext()
                }
                cursor.close()
            }

            if (sb.isNotEmpty()) {
                val attrCursor = it.rawQuery(
                    "SELECT * FROM ${constants.TBL_ATTRIBUTES} WHERE ${constants.COL_ID} IN ($sb)",
                    null
                )
                if (attrCursor.moveToFirst()) {
                    while (!attrCursor.isAfterLast) {
                        attrs.add(cursorToAttribute(attrCursor))
                        attrCursor.moveToNext()
                    }
                }
                attrCursor.close()
            }
        }

        return attrs.toTypedArray()
    }

    /**
     * Adds a new, self-created message to table. This method should only be used when the user
     * creates a new message through the app. It also takes care of default values from
     * desc, title, etc.
     *
     * @param ctx Context
     *
     * @param msg [Message] to be saved
     */
    fun addSelfCreatedMessage(ctx: Context,
                              internalType: String = MessageProcessActivity.TYPE_DEFECT_REPORT): Message {
        val cal = Calendar.getInstance()
        cal.time = Date()

        val msg = Message()
        msg.id = UUID.randomUUID().toString().replace("-", "").uppercase()
        msg.createdAt = cal.timeInMillis
        msg.internalType = internalType
        msg.desc = ctx.getString(R.string.default_msg_description)
        msg.title = ctx.getString(R.string.default_msg_title)

        val varargs = arrayOf(constants.COL_ID to msg.id,
                constants.COL_ORIGIN to constants.ORIGIN_SELF,
                constants.COL_CREATEDAT to msg.createdAt,
                constants.COL_DESC to msg.desc,
                constants.COL_TITLE to msg.title,
                constants.COL_INTERNAL_TYPE to internalType)

        writableDatabase.let {
            MMDBUtils.insert(it, constants.TBL_MESSAGES, *varargs)
        }

        // LOG: logged when a new message is created
        val pref = PreferenceManager.getDefaultSharedPreferences(ctx)
        val canLog = pref.getBoolean(ctx.getString(R.string.mm_prefkey_should_log), false)
        if (MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.ActivityHistory] == true && canLog) {
            addLog(Log.TYPE_MSG_CREATED, hashMapOf(Log.KEY_MSG_ID to msg.id))
        }
        return msg
    }

    fun addDomain(domain: Domain, db: SQLiteDatabase? = null) {
        val dbObj = db?: writableDatabase
        val values = arrayOf(
                constants.COL_ID to domain.id,
                constants.COL_NAME to domain.name,
                constants.COL_SYSTEM_ID to domain.systemId,
                constants.COL_DOMAIN_URL to domain.uri,
                constants.COL_ISDEFAULT to if (domain.isDefault) 1 else 0,
                constants.COL_ISDEFAULT_RECIPIENT to if (domain.isDefaultRecipient) 1 else 0,
                constants.COL_EXTRASJSON to domain.settingsToJson().toString()
        )
        dbObj.let {
            MMDBUtils.replace(it, constants.TBL_DOMAINS, *values)
        }
    }

    fun getDomain(domainId: String): Domain? {
        var dom: Domain? = null
        readableDatabase.let {
            val cursor = it.rawQuery("SELECT * FROM ${constants.TBL_DOMAINS} WHERE ${constants.COL_ID} = '$domainId'", null)
            if (cursor.moveToNext()) {
                dom = cursorToDomain(cursor)
            }
            cursor.close()
        }
        return dom
    }

    /**
     * Add system information to system table
     *
     * @param sysId system Id (for primary key)
     * @param appId system's app id
     * @param sysName name of the system
     * @param domainUrl domain URL
     * @param isPrivate true if it is wdw system, false otherwise
     */
    fun addSystem(sysId: String, appId: Int, sysName: String, domainUrl: String, isPrivate: Boolean) {
        val values = arrayOf(
                constants.COL_ID to sysId,
                constants.COL_APP_ID to appId,
                constants.COL_NAME to sysName,
                constants.COL_DOMAIN_URL to domainUrl,
                constants.COL_ISPRIVATE to if (isPrivate) 1 else 0
        )
        writableDatabase.let {
            MMDBUtils.replace(it, constants.TBL_SYSTEM, *values)
        }
    }

    /**
     * Calls [addSystem]
     *
     * @param sysInfo [SystemInfo] instance
     */
    fun addSystem(sysInfo: SystemInfo) {
        addSystem(sysInfo.generateId(), sysInfo.appId.toInt(), sysInfo.title, sysInfo.domainName, !sysInfo.isExternal)
    }

    /**
     * Retrieves a single [SystemInfo]
     *
     * @param sysId the system's id
     */
    fun getSystem(sysId: String): SystemInfo? {
        var sys: SystemInfo? = null
        readableDatabase.let {
            val cursor = it.rawQuery("SELECT * FROM ${constants.TBL_SYSTEM} WHERE ${constants.COL_ID} = '$sysId'", null)
            if (cursor.moveToNext()) {
                sys = cursorToSystemInfo(cursor)
            }
            cursor.close()
        }
        return sys
    }

    /**
     *  Save message to database. This method should only be used when saving messages
     *  coming from the server (e.g. through [de.maengelmelder.mainmodule.network.MMBMSGetNearestMessages]).
     *
     *  It handles duplicate, and keeping local columns (e.g. favorite) intact when updating messages
     *
     *  @param msg [Message] to be saved
     *
     */
    fun addMessageFromServer(msg: Message) {
        val varargs = arrayOf(constants.COL_ID to msg.id, // Since a message can have the same domain and id, it should be generated
                constants.COL_SERVER_ID to msg.serverId,
                constants.COL_SYSTEM_ID to msg.systemId,
                constants.COL_DOMAIN_ID to msg.category.domainId,
                constants.COL_CAT_ID to msg.category.generateId(),
                constants.COL_ORIGIN to constants.ORIGIN_SERVER,
                constants.COL_TITLE to msg.title,
                constants.COL_STATE to msg.state,
                constants.COL_COLOR to msg.colorString,
                constants.COL_LAT to msg.lat,
                constants.COL_LON to msg.lon,
                constants.COL_TEXT to msg.text,
                constants.COL_DESC to msg.desc,
                constants.COL_UPLOADED_AT to "-1",
                constants.COL_MARKER_URL to msg.markerUrl,
                constants.COL_CREATEDAT to msg.createdAt
        )

        if (messageExists(msg.id)) {
            updateMessage(msg.id, *varargs)
        } else {
            writableDatabase.let {
                MMDBUtils.replace(it, constants.TBL_MESSAGES, *varargs)
            }
        }
    }

    /**
     * Check whether the message with the given ID exists in the table.
     *
     * @return true if exists, false otherwise
     */
    private fun messageExists(msgId: String): Boolean {
        val whereArgs = "${constants.COL_ID} = \'$msgId\'"
        var exists = false
        readableDatabase.let {
            val cursor = it.rawQuery("SELECT ${constants.COL_ID} FROM ${constants.TBL_MESSAGES} WHERE $whereArgs", null)
            exists = cursor.count > 0
            cursor.close()
        }
        return exists
    }

    /**
     * Returns a message with the given ID along with the related category.
     *
     * @param id id of the message
     * @param isServerId whether the given Id is the one that comes from server or client
     *
     * @return [Message] with the given ID or null if it does not exist
     */
    fun getMessage(id: String, isServerId: Boolean = false): Message? {
        var m: Message? = null
        val whereArgs = if (!isServerId) "${constants.COL_ID} = \'$id\'" else "${constants.COL_SERVER_ID} = \'$id\'"

        readableDatabase.let {
            val cursor = it.rawQuery("SELECT * FROM ${constants.TBL_MESSAGES} WHERE $whereArgs", null)
            if (cursor.moveToFirst()) {
                m = cursorToMessage(cursor)
                val catId = cursor.getString(cursor.getColumnIndexOrThrow(constants.COL_CAT_ID))
                if (catId != null) {
                    val cat = getCategory(catId)
                    cat?.let { c -> m?.category = c }
                }
            }
            cursor.close()
        }
        return m
    }

    /**
     * Get all saved messages, both locally created and from the server
     *
     * @return [ArrayList] of [Message].
     */
    fun getMessages(): ArrayList<Message> {
        val msgs = ArrayList<Message>()

        readableDatabase.let {
            val cursor = it.rawQuery("SELECT * FROM ${constants.TBL_MESSAGES}", null)
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    val msg = cursorToMessage(cursor)
                    // Get the category also
                    val catId = cursor.getString(cursor.getColumnIndexOrThrow(constants.COL_CAT_ID))
                    if (catId != null) {
                        val cat = getCategory(catId)
                        cat?.let { c ->  msg.category = c }
                    }
                    msgs.add(msg)
                    cursor.moveToNext()
                }
            }
            cursor.close()
        }
        return msgs
    }

    /**
     * Get the upload status of a message (the value of column [MMDBConstants.COL_UPLOAD_STATUS])
     *
     * @param msgId id of the message
     *
     * @return upload status or null if the message does not exist
     */
    fun getUploadStatus(msgId: String): String? {
        var uploadStatus: String? = null

        readableDatabase.let {
            val cursor = it.rawQuery("SELECT ${constants.COL_UPLOAD_STATUS} FROM ${constants.TBL_MESSAGES} WHERE ${constants.COL_ID} = '$msgId'", null)
            if (cursor.moveToFirst()) {
                uploadStatus = cursor.getString(cursor.getColumnIndexOrThrow(constants.COL_UPLOAD_STATUS))
            }
            cursor.close()
        }

        return uploadStatus
    }

    /**
     * Get the value of column [MMDBConstants.COL_EXTRASJSON] from message table with
     * the given message ID. The value is in String and will be converted to [JSONArray] immediately
     *
     * @param msgId id of the [Message]
     *
     * @return [JSONArray] containing the value of the column
     */
    fun getExtrasJSON(msgId: String) : JSONArray {
        var arr = JSONArray()

        readableDatabase.let {
            val cursor = it.rawQuery("SELECT ${constants.COL_EXTRASJSON} FROM ${constants.TBL_MESSAGES} WHERE ${constants.COL_ID} = '$msgId'", null)
            if (cursor.moveToFirst()) {
                arr = JSONArray(cursor.getString(0)?: "[]")
            }
            cursor.close()
        }
        return arr
    }

    /**
     * Get a list of messages that match with the given parameter [andQueries]. The parameter [andQueries]
     * should have the first element as String that matches the given column name at [MMDBConstants] and the second element
     * as [Any].
     *
     * @param andQueries the filter that will be combined by AND
     *
     * @return [ArrayList] of [Message] matching given queries
     */
    fun getMessages(vararg andQueries: Pair<String, Any>): ArrayList<Message> {
        val msgs = ArrayList<Message>()
        var where = ""
        val whereArgs = arrayListOf<String>()
        andQueries.forEachIndexed {
            idx, pair ->
                where += "${pair.first} = ?"
                whereArgs.add(pair.second.toString())
                if (idx < andQueries.size - 1) where += " AND "
        }

        // * is a spread operator to pass array instead of varargs
        readableDatabase.let {
            val cursor = it.rawQuery(
                "SELECT * FROM ${constants.TBL_MESSAGES} WHERE $where",
                whereArgs.toTypedArray()
            )
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    val msg = cursorToMessage(cursor)
                    // Get the category also
                    val catId = cursor.getString(cursor.getColumnIndexOrThrow(constants.COL_CAT_ID))
                    if (catId != null) {
                        val cat = getCategory(catId)
                        cat?.let { c ->  msg.category = c }
                    }
                    msgs.add(msg)
                    cursor.moveToNext()
                }
            }
            cursor.close()
        }

        return msgs
    }

    /**
     * Return the number of messages that satisfy the given queries.
     *
     * @param andQueries filter
     *
     * @return the number of messages that satisfy the given "AND" query.
     */
    fun getMessagesCount(vararg andQueries: Pair<String, Any>): Int {
        var where = ""
        val whereArgs = arrayListOf<String>()
        andQueries.forEachIndexed {
                idx, pair ->
            where += "${pair.first} = ?"
            whereArgs.add(pair.second.toString())
            if (idx < andQueries.size - 1) where += " AND "
        }

        var msgCount = 0
        readableDatabase.let {
            val cursor = it.rawQuery(
                "SELECT ${constants.COL_ID} FROM ${constants.TBL_MESSAGES} WHERE $where",
                whereArgs.toTypedArray()
            )
            msgCount = cursor.count
            cursor.close()
        }
        return msgCount
    }

    /**
     * Update the message defined by [msgId] with the given [updated] values. The [updated] values
     * should follow the rule given in [getMessages]
     *
     * @param msgId id of the message
     * @param updated the changes
     *
     * @see [MMDB.getMessages]
     */
    fun updateMessage(msgId: String, vararg updated: Pair<String, Any>) {
        writableDatabase.let {
            it.update(
                constants.TBL_MESSAGES,
                updated.toContentValues(),
                "${constants.COL_ID} = '$msgId'",
                null
            )
        }
    }

    /**
     * Delete a [Message] with the given ID
     *
     * @param id the ID of [Message] that will be deleted
     */
    fun deleteMessage(id: String) {
        writableDatabase.let {
            MMDBUtils.delete(it, constants.TBL_MESSAGES, "${constants.COL_ID} = \'$id\'")
        }
    }

    /**
     * Clear all records in a table from the given name
     *
     * @param tblName name of the table
     */
    fun truncate(tblName: String) {
        writableDatabase.let {
            MMDBUtils.delete(it, tblName, "1 = 1")
        }
    }

    /**
     * Transform [Cursor] to [Log] object
     *
     * @param c [Cursor]
     */
    private fun cursorToLog(c: Cursor) : Log =
        Log (
                id = c.getInt(c.getColumnIndex(constants.COL_ID)),
                timestamp = c.getString(c.getColumnIndex(constants.COL_CREATEDAT)).toLong(),
                data = Log.jsonToMap(c.getString(c.getColumnIndex(constants.COL_ADDITIONAL)))
        )

    /**
     * Transform [Cursor] to [Message]
     *
     * @param c [Cursor]
     */
    private fun cursorToMessage(c: Cursor): Message {
        val m = Message()
        m.id = c.getString(c.getColumnIndex(constants.COL_ID))?: ""
        m.systemId = c.getString(c.getColumnIndex(constants.COL_SYSTEM_ID))?: ""
        m.serverId = c.getString(c.getColumnIndex(constants.COL_SERVER_ID))?: ""
        m.title = c.getString(c.getColumnIndex(constants.COL_TITLE))?: m.title
        m.lat = c.getDouble(c.getColumnIndex(constants.COL_LAT))
        m.lon = c.getDouble(c.getColumnIndex(constants.COL_LON))
        m.state = c.getString(c.getColumnIndex(constants.COL_STATE))?: m.state
        m.state_en = c.getString(c.getColumnIndex(constants.COL_STATE))?: m.state_en
        m.colorString = c.getString(c.getColumnIndex(constants.COL_COLOR))?: m.colorString
        m.desc = c.getString(c.getColumnIndex(constants.COL_DESC))?: m.desc
        m.text = c.getString(c.getColumnIndex(constants.COL_TEXT))?: m.text
        m.createdAt = c.getString(c.getColumnIndex(constants.COL_CREATEDAT))?.toLong()?: -1
        m.uploadedAt = c.getString(c.getColumnIndex(constants.COL_UPLOADED_AT))?.toLong()?: -1
        m.uploadStatus = c.getString(c.getColumnIndex(constants.COL_UPLOAD_STATUS))?: ""
        m.isFavorite = c.getInt(c.getColumnIndex(constants.COL_MARK_FAV)) == 1
        m.internalType = c.getString(c.getColumnIndex(constants.COL_INTERNAL_TYPE))
        m.markerUrl = c.getString(c.getColumnIndex(constants.COL_MARKER_URL))
        m.additional = Message.parseAdditionalData(c.getString(c.getColumnIndex(constants.COL_ADDITIONAL))?: "")

        try {
            val imagePaths = c.getString(c.getColumnIndex(constants.COL_PHOTO_PATH))
            if (imagePaths != null && imagePaths.isNotEmpty()) {
                m.imagePaths.addAll(imagePaths.split(";"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return m
    }

    /**
     * Transform [Cursor] to [Attribute]
     *
     * @param c [Cursor]
     */
    private fun cursorToAttribute(c: Cursor): Attribute {
        val attr = Attribute()
        attr.id = c.getString(c.getColumnIndex(constants.COL_ID))
        attr.localId = c.getString(c.getColumnIndex(constants.COL_ATTR_ID))
        attr.domainId = c.getString(c.getColumnIndex(constants.COL_DOMAIN_ID))
        attr.systemId = c.getString(c.getColumnIndex(constants.COL_SYSTEM_ID))
        attr.name = c.getString(c.getColumnIndex(constants.COL_NAME))
        attr.code = c.getString(c.getColumnIndex(constants.COL_CODE))
        attr.type = c.getString(c.getColumnIndex(constants.COL_TYPE))
        attr.public = c.getInt(c.getColumnIndex(constants.COL_ISPUBLIC)) == 1
        attr.required = c.getInt(c.getColumnIndex(constants.COL_ISREQ)) == 1
        attr.shouldCache = c.getInt(c.getColumnIndex(constants.COL_SHOULDCACHE)) == 1
        attr.requiredIfCode = c.getString(c.getColumnIndex(constants.COL_REQ_IF_CODE))
        attr.requiredIfValue = c.getString(c.getColumnIndex(constants.COL_REQ_IF_VALUE))
        attr.visibleIfCode = c.getString(c.getColumnIndex(constants.COL_VIS_IF_CODE))
        attr.visibleIfValue= c.getString(c.getColumnIndex(constants.COL_VIS_IF_VALUE))
        attr.errorText = c.getString(c.getColumnIndex(constants.COL_ERRTEXT))
        attr.helpText= c.getString(c.getColumnIndex(constants.COL_HELPTEXT))
        attr.ordering = c.getInt(c.getColumnIndex(constants.COL_ORDERING))
        attr.maxLength = c.getInt(c.getColumnIndex(constants.COL_MAXLENGTH))
        attr.multiselect = c.getInt((c.getColumnIndex(constants.COL_MULTISELECT))) == 1
        attr.choicesFromJson(c.getString(c.getColumnIndex(constants.COL_VALUELIST)))
        return attr
    }

    /**
     * Transform [Cursor] to [Category]
     *
     * @param c [Cursor]
     */
    private fun cursorToCategory(c: Cursor): Category {
        val cat = Category()
        cat.systemId = c.getString(c.getColumnIndex(constants.COL_SYSTEM_ID))
        cat.domainId = c.getString(c.getColumnIndex(constants.COL_DOMAIN_ID))
        cat.typeId = c.getString(c.getColumnIndex(constants.COL_TYPEID)).toLong()
        cat.name = c.getString(c.getColumnIndex(constants.COL_NAME))
        cat.group = c.getString(c.getColumnIndex(constants.COL_GROUP))
        cat.rubric = c.getString(c.getColumnIndex(constants.COL_RUBRIK))
        cat.displayedName = c.getString(c.getColumnIndex(constants.COL_DISPNAME))
        cat.markerId = c.getString(c.getColumnIndex(constants.COL_MARKER_ID))
        cat.hasTitle = c.getInt(c.getColumnIndex(constants.COL_HASTITLE)) == 1
        cat.isPrivate = c.getInt(c.getColumnIndex(constants.COL_ISPRIVATE)) == 1
        cat.needsIdentification = c.getInt(c.getColumnIndex(constants.COL_NEEDSID)) == 1
        cat.description = c.getString(c.getColumnIndex(constants.COL_DESC))
        cat.description = c.getString(c.getColumnIndex(constants.COL_DESC))
        cat.photoReq = try { c.getString(c.getColumnIndex(constants.COL_REQUIRE_PHOTO)) } catch (e: java.lang.Exception) { Category.PHOTO_OPTIONAL }
        cat.posReq = try { c.getString(c.getColumnIndex(constants.COL_REQUIRE_POSITION)) } catch (e: java.lang.Exception) { Category.POS_REQ }
        cat.isSeen = c.getInt(c.getColumnIndex(constants.COL_ISSEEN)) == 1
        cat.createDirectLinkOnly = c.getInt(c.getColumnIndex(constants.COL_DIRECTLINK_ONLY)) == 1
        cat.attrIdsMessageFromJSONString(c.getString(c.getColumnIndex(constants.COL_ATTRIDS_FOR_CREATION)))
        return cat
    }

    /**
     * Transform [Cursor] to [Domain]
     *
     * @param c [Cursor]
     */
    private fun cursorToDomain(c: Cursor): Domain = Domain().apply {
        id = c.getString(c.getColumnIndex(constants.COL_ID))
        systemId = c.getString(c.getColumnIndex(constants.COL_SYSTEM_ID))
        name = c.getString(c.getColumnIndex(constants.COL_NAME))
        uri = c.getString(c.getColumnIndex(constants.COL_DOMAIN_URL))
        isDefault = c.getInt(c.getColumnIndex(constants.COL_ISDEFAULT)) == 1
        isDefaultRecipient = c.getInt(c.getColumnIndex(constants.COL_ISDEFAULT_RECIPIENT)) == 1
        settingsFromJson(c.getString(c.getColumnIndex(constants.COL_EXTRASJSON)))
    }

    /**
     * Transform [Cursor] to [SystemInfo]
     *
     * @param c [Cursor]
     */
    private fun cursorToSystemInfo(c: Cursor): SystemInfo = SystemInfo().apply {
        appId = c.getString(c.getColumnIndex(constants.COL_APP_ID))
        domainName = c.getString(c.getColumnIndex(constants.COL_DOMAIN_URL))
        title = c.getString(c.getColumnIndex(constants.COL_NAME))
        isExternal = c.getInt(c.getColumnIndex(constants.COL_ISPRIVATE)) == 0
    }

    /**
     * Transform [Cursor] to [Geometry]
     */
    private fun cursorToGeometry(c: Cursor): Geometry = Geometry().apply {
        systemId = c.getString(c.getColumnIndex(constants.COL_SYSTEM_ID)).toInt()
        domainId = c.getString(c.getColumnIndex(constants.COL_DOMAIN_ID)).toInt()
        id = c.getInt(c.getColumnIndex(constants.COL_GEOMID))
        partId = c.getInt(c.getColumnIndex(constants.COL_GEOMPARTID))
        minLat = c.getDouble(c.getColumnIndex(constants.COL_MIN_LAT))
        maxLat = c.getDouble(c.getColumnIndex(constants.COL_MAX_LAT))
        minLon = c.getDouble(c.getColumnIndex(constants.COL_MIN_LON))
        maxLon = c.getDouble(c.getColumnIndex(constants.COL_MAX_LON))
        val catIds = c.getString(c.getColumnIndex(constants.COL_CAT_ID))?: ""
        if (catIds.isNotEmpty()) {
            categoryIds = catIds.split(",")
        }
    }

}