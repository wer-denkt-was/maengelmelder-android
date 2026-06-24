package de.maengelmelder.mainmodule.database

import android.content.Context
import androidx.core.content.ContextCompat
import de.maengelmelder.mainmodule.R

/**
 * DB table names, version number, fields, etc
 */
internal class MMDBConstants(c: Context) {
    val context: Context = c

    val DB_NAME = c.getString(R.string.mm_db_name)
    val DB_VERSION = c.resources.getInteger(R.integer.mm_db_version)

    val ORIGIN_SELF = "self"
    val ORIGIN_SERVER = "server"

    val COL_CREATEDAT = "created_at"
    val COL_UPLOADED_AT = "uploaded_at"
    val COL_ADDITIONAL = "additional"

    val COL_ID = "_id"
    val COL_SERVER_ID = "server_id"
    val COL_SYSTEM_ID = "system_id"
    val COL_TITLE = "title"
    val COL_TYPE = "type"
    val COL_TYPEID = "typeid"
    val COL_STATE = "state"
    val COL_STATE_EN = "state_en"
    val COL_COLOR = "color"
    val COL_DISTANCE = "distance"
    val COL_MARKER_ID = "markerid"
    val COL_DESC = "description"
    val COL_TEXT = "msg_text"
    val COL_ORIGIN = "origin"
    val COL_EXTRASJSON = "extras"
    val COL_DOMAIN_ID = "domain_id"
    val COL_PHOTO_PATH = "photofilepath"
    val COL_MARKER_URL = "marker_url"
    val COL_LAT = "latitude"
    val COL_LON = "longitude"
    val COL_UPLOAD_STATUS = "upload_status"
    val COL_ORDERING = "ordering"
    val COL_MARK_FAV = "marked_fav"
    val COL_GROUP = "grp_name"
    val COL_INTERNAL_TYPE = "message_internal_type"
    val COL_DOMAIN_URL = "domain_url"

    // Upload status
    val STATUS_UPLOADING = "uploading"
    val STATUS_UPLOADING_IMAGES = "uploading_images"
    val STATUS_IMAGE_UPLOAD_FAIL = "upload_images_failed"
    val STATUS_FINISHED = "upload_successful"
    val STATUS_UPLOAD_FAILED = "upload_failed"

    // Category (+COL_ID, +COL_MARKER_ID)
    val COL_NAME = "name"
    val COL_DOMAIN_NAME = "domain_name"
    val COL_DISPNAME = "displayname"
    val COL_HASTITLE = "hastitle"
    val COL_ISPRIVATE = "private"
    val COL_ISDEFAULT = "is_default"
    val COL_ISDEFAULT_RECIPIENT = "is_default_recipient"
    val COL_ISSEEN = "is_seen"
    val COL_REQUIRE_PHOTO = "require_photo"
    val COL_REQUIRE_POSITION = "require_position"
    val COL_NEEDSID = "needsidentification"
    val COL_EXTERNAL_URL = "external_url"
    val COL_ATTRIDS_FOR_CREATION = "message_creation_attr_ids"
    val COL_DIRECTLINK_ONLY = "create_directlink_only"

    // Category - Attribute *-* relationship
    val COL_CAT_ID = "cat_id"
    val COL_ATTR_ID = "attr_id"
    val COL_RUBRIK = "rubric"

    // Attribute (+COL_ID, +COL_NAME, +COL_TYPE)
    val COL_CODE = "code"
    val COL_ISPUBLIC = "is_public"
    val COL_ISREQ = "is_required"
    val COL_MULTISELECT = "multiselect"
    val COL_SHOULDCACHE = "should_cache"
    val COL_MAXLENGTH = "max_length"
    val COL_REQ_IF_CODE = "req_if_code"
    val COL_REQ_IF_VALUE = "req_if_val"
    val COL_VIS_IF_CODE = "vis_if_code"
    val COL_VIS_IF_VALUE = "vis_if_val"
    val COL_ERRTEXT = "errtext"
    val COL_HELPTEXT = "helptext"
    val COL_VALUELIST = "valueslist"

    val TBL_MESSAGES = "tbl_messages"
    val TBL_CATEGORIES = "tbl_categories"
    val TBL_ATTRIBUTES = "tbl_attributes"
    val TBL_CAT_ATTR = "tbl_cats_attrs"
    val TBL_LOGS = "tbl_logs"
    val TBL_DOMAINS = "tbl_domains"
    val TBL_SYSTEM = "tbl_systems"
    val TBL_GEOMETRY = "tbl_geometries"

    // Geometry table
    val COL_GEOMID = "geomid"
    val COL_GEOMPARTID = "geompartid"
    val COL_WKT = "wkt"
    val COL_MIN_LAT = "min_lat";
    val COL_MIN_LON = "min_lon";
    val COL_MAX_LAT = "max_lat";
    val COL_MAX_LON = "max_lon";

    // System
    val COL_SYS_NAME = "system_name"
    val COL_URLS = "urls"
    val COL_APP_ID = "appid"
}