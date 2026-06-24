package de.maengelmelder.mainmodule.network.utils

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.IOException
import okio.source

/**
 * Used to embed resources such as images or other files into a multipart request body
 */
class ContentUriRequestBody(
        private val contentResolver: ContentResolver,
        private val contentUri: Uri
) : RequestBody() {

    override fun contentType(): MediaType? {
        val contentType = contentResolver.getType(contentUri)
        return contentType?.toMediaTypeOrNull()
    }

    override fun writeTo(sink: BufferedSink) {
        val inputStream = contentResolver.openInputStream(contentUri)
                ?: throw IOException("Couldn't open content URI for reading")
        inputStream.source().use { source ->
            sink.writeAll(source)
        }
        try { inputStream.close() } catch (e: Exception) { e.printStackTrace() }
    }
}