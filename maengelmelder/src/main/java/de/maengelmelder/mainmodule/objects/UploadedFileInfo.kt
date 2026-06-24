package de.maengelmelder.mainmodule.objects

import org.json.JSONObject

class UploadedFileInfo {

    companion object {
        fun fromJSON(jsonObj: JSONObject): UploadedFileInfo = UploadedFileInfo().apply {
            id = jsonObj.optString("id", "")
            url = jsonObj.optString("url", "")
            sizeByte = jsonObj.optLong("size", 0)
            type = jsonObj.optString("type", "")
            filename = jsonObj.optString("filename", "")
        }
    }

    var id: String = ""
    var url: String = ""
    var type: String = ""
    var filename: String = ""
    var sizeByte: Long = 0

}