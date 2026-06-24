package de.maengelmelder.mainmodule.network.v1

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.UploadedFileInfo
import org.json.JSONObject
import java.io.File
import java.lang.Exception

/**
 * POST api/v1/domain/<domainid>/bundle/<bundleid>/file
 *
 */
@Deprecated(
        message = "AsyncTask is deprecated since SDK 30",
        replaceWith = ReplaceWith("coroutines/v1/MMv1UploadFileToBundle")
)
class MMv1UploadFileToBundle(
        c: Context,
        domId: String,
        bundleId: String,
        filename: String,
        bitmap: Bitmap) :

        MMv1Api<List<UploadedFileInfo>, BaseResponse>(c, "bundle/${bundleId}/file", domId) {

    companion object {
        const val RESP_NO_DATA = -1
    }

    init {
        try {
            addContent("picture", filename, bitmap, 50)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun parseResponse(resp: BaseResponse): List<UploadedFileInfo> {
        val uploadedFiles = arrayListOf<UploadedFileInfo>()
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return uploadedFiles

        json.optJSONObject("data")?.let { data ->
            data.optJSONArray("files")?.let { files ->
                (0 until (files.length())).forEach { idx ->
                    val jsonObj = files.getJSONObject(idx)
                    val info = UploadedFileInfo().apply {
                        id = jsonObj.optString("id", "")
                        url = jsonObj.optString("url", "")
                        sizeByte = jsonObj.optLong("size", 0)
                        type = jsonObj.optString("type", "")
                        filename = jsonObj.optString("filename", "")
                    }
                    uploadedFiles.add(info)
                }
            }
        }

        return uploadedFiles
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return BaseResponse(RESP_NO_DATA, "")
        return BaseResponse(resp.code, json.optString("message"))
    }

    override fun getUrlParam(): Map<String, String?>? = null
}