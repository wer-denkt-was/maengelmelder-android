package de.maengelmelder.mainmodule.service.tasks

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import de.maengelmelder.mainmodule.network.collectives.coroutines.IOCoroutine
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.utils.images.ImageManipulator
import de.maengelmelder.mainmodule.utils.images.ImageOrientatorCoroutine
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.lang.Exception

class ThumbnailLoadingCoroutine(ctx: Context,
                                referenceView: Any?,
                                id: String,
                                photoPath: String,
                                thumbnailSize: Long = 128):
        IOCoroutine<Bitmap>(ctx, listOf()) {

    companion object {
        private val thumbnailMap = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024).toInt() / 8) {
            override fun sizeOf(key: String?, value: Bitmap?): Int {
                return value?.byteCount?: 0 / 1024
            }
        }
    }
    private val mThumbnailSize = thumbnailSize
    private val mRefView = referenceView
    private val identifier = id
    private val path = photoPath
    private var mThumbnailListener: OnThumbnail? = null

    var listener: OnThumbnail?
        get() = mThumbnailListener
        set(value) {
            mThumbnailListener = value
        }

    override fun insideDispatcherIO(scope: CoroutineScope): Pair<Bitmap?, BaseResponse?> {
        var cached = thumbnailMap.get(identifier)
        if (cached == null) {
            try {
                val bitmap = if (path.startsWith("content://")) {
                    ImageManipulator.getScaledBitmapNonThread(context, Uri.parse(path), mThumbnailSize.toInt())
                } else {
                    ImageManipulator.getScaledBitmap(File(path), mThumbnailSize.toInt())
                }

                cached = if (bitmap != null) {
                    ImageOrientatorCoroutine(bitmap, path).executeUnsafe()
                } else {
                    bitmap
                }
            } catch (e: Exception) {
                // Maybe path is null?
            }
        }

        return Pair(cached, null)
    }

    override fun onSuccess(data: Bitmap) {
        thumbnailMap.put(identifier, data)
        mThumbnailListener?.onThumbnailReady(mRefView, data)

    }

    override fun onError(err: BaseResponse) {
        // This should not happen
    }

    interface OnThumbnail {
        fun onThumbnailReady(ref: Any?, bitmap: Bitmap)
    }
}