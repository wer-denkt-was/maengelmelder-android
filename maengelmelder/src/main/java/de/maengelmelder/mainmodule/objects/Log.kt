package de.maengelmelder.mainmodule.objects

import org.json.JSONObject

class Log (val id: Int, val timestamp: Long, val data: HashMap<String, String>) {

    companion object {
        val KEY_TYPE = "logtype"
        val TYPE_MSG_CREATED = "logtype.msg_created"
        val TYPE_MSG_EDITED = "logtype.msg_edited"
        val TYPE_MSG_REMOVED = "logtype.msg_removed"
        val TYPE_MSG_UPDATED = "logtype.msg_updated"
        val TYPE_MSG_UPDATE_FAILED = "logtype.msg_update_failed"
        val TYPE_MSG_VIEWED = "logtype.msg_viewed"
        val TYPE_LOGIN = "logtype.login"
        val TYPE_LOGOUT = "logtype.logout"
        val TYPE_MSG_UPLOAD_SUCCESS = "logtype.msg_upload_success"
        val TYPE_IMG_UPLOAD_SUCCESS = "logtype.img_upload_success"
        val TYPE_MSG_UPLOAD_FAIL = "logtype.msg_upload_fail"
        val TYPE_IMG_UPLOAD_FAIL = "logtype.img_upload_fail"
        val TYPE_MSG_UPLOAD = "logtype.msg_upload"
        val TYPE_IMG_UPLOAD = "logtype.img_upload"

        val KEY_MSG_ID = "msg_id"
        val KEY_REF_ID = "ref_id"
        val KEY_USERNAME = "username"
        val KEY_REASON = "reason"
        val KEY_IMG_PATH = "img_path"

        val KEY_TITLE = "title"

        fun mapToJson(data: Map<String, String>): JSONObject {
            val json = JSONObject()
            data.forEach { e -> json.put(e.key, e.value) }
            return json
        }

        fun jsonToMap(json: String): HashMap<String, String> {
            val map = hashMapOf<String, String>()
            try {
                val jsonObject = JSONObject(json)
                jsonObject.keys().forEach { key -> map[key] = jsonObject.optString(key, "") }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return map
        }
    }

}