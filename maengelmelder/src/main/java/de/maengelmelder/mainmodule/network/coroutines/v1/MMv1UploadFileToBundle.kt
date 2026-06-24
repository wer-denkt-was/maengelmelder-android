package de.maengelmelder.mainmodule.network.coroutines.v1

import android.content.Context
import android.graphics.Bitmap
import de.maengelmelder.mainmodule.network.coroutines.MMNetworkRepository
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.UploadedFileInfo
import org.json.JSONObject
import java.lang.Exception

/**
 * upload file to bundle
 */
class MMv1UploadFileToBundle(c: Context,
                             domId: Int,
                             bundleId: String,
                             filename: String,
                             bitmap: Bitmap) :
    MMNetworkRepository<List<UploadedFileInfo>, BaseResponse>(c, domId, "bundle/${bundleId}/file") {

    init {
        try {
            multipartFormAddImage("picture", filename, bitmap, 50, 1280)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun getQueryParameters(): Map<String, String>? = null

    override fun parseResponse(resp: BaseResponse): List<UploadedFileInfo> {
        val uploadedFiles = arrayListOf<UploadedFileInfo>()
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return uploadedFiles

        json.optJSONObject("data")?.let { data ->
            data.optJSONArray("files")?.let { files ->
                (0 until (files.length())).forEach { idx ->
                    val jsonObj = files.getJSONObject(idx)
                    val info = UploadedFileInfo.fromJSON(jsonObj)
                    uploadedFiles.add(info)
                }
            }
        }

        return uploadedFiles
    }

    override fun parseError(resp: BaseResponse): BaseResponse {
        val json = try { JSONObject(resp.body) } catch (e: Exception) { null }
        if (json == null) return BaseResponse(-1, "")
        return BaseResponse(resp.code, json.optString("message"))
    }
}