package de.maengelmelder.mainmodule.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Load image from URL to be used as map marker. Also handles caching and scaling
 */
object MapMarkerFromURLCoroutine : ViewModel() {

    /**
     * Bitmap cache for marker images, keyed by URL
     */
    private var mBitmapCache: LruCache<String, Bitmap>? = null

    fun run(url: String, bitmapWidth: Int = 120, onDone: (Bitmap?) -> Unit) {
        initCache()
        viewModelScope.launch {
            onDone(doGetBitmap(url, bitmapWidth))
        }
    }

    fun runMultiple(urls: List<String>, bitmapWidth: Int = 120, onDone: (List<Bitmap?>) -> Unit) {
        initCache()
        viewModelScope.launch {
            val bitmaps = urls.map { u -> doGetBitmap(u, bitmapWidth) }
            onDone(bitmaps)
        }
    }

    private fun initCache() {
        if (mBitmapCache == null) {
            val maxMem = (Runtime.getRuntime().maxMemory() / 1024).toInt()
            // Allocate 1/8th of max memory for storing bitmap
            mBitmapCache = object : LruCache<String, Bitmap>(maxMem / 8) {
                override fun sizeOf(key: String?, value: Bitmap?): Int {
                    return (value?.byteCount ?: 0) / 1024
                }
            }
        }
    }

    private suspend fun doGetBitmap(url: String, bitmapWidth: Int): Bitmap? {
        if (url == "" || !url.startsWith("http://") || !url.startsWith("https://")) {
            return null
        } else {
            val bitmap = getBitmap(url, bitmapWidth)
            return bitmap
        }
    }

    /**
     * Returns [Bitmap] from the given Url and resize it to the given bitmapWidth
     * @param url Image url
     * @param bitmapWidth width of the exported bitmap in pixel
     * @return [Bitmap] image or null if URL is invalid or cannot be resolved
     */
    private suspend fun getBitmap(url: String, bitmapWidth: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            val existingBitmap = mBitmapCache?.get(url)
            if (existingBitmap != null) {
                existingBitmap
            } else {
                // Grab the image from url
                var bmp: Bitmap?
                try {
                    val markerUrl = URL(url)
                    val inpStream = markerUrl.openConnection().getInputStream()
                    bmp = BitmapFactory.decodeStream(inpStream)
                    inpStream.close()
                } catch (e: Exception) {
                    bmp = null
                }
                // Scale it to the given width
                if (bmp != null) {
                    val finalBmp = Bitmap.createScaledBitmap(
                        bmp,
                        bitmapWidth,
                        (bitmapWidth / bmp.width * bmp.height),
                        false
                    )
                    // Save to cache
                    mBitmapCache?.put(url, finalBmp)
                    finalBmp
                } else {
                    // Malformed URL, invalid image, etc.
                    null
                }
            }
        }
    }
}