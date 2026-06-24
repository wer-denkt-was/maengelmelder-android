package de.maengelmelder.mainmodule.utils.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.ImageView
import com.squareup.picasso.Picasso
import de.maengelmelder.mainmodule.BuildConfig
import java.io.File
import java.io.FileOutputStream

/**
 * ## Overview
 * This object handles manipulation of image, including
 * - Scaling image to reduce memory usage on loading
 * - Handling image download and caching to an [ImageView]
 *
 * ## External Library
 * - Picasso (https://github.com/square/picasso)
 *
 */
object ImageManipulator {

    /**
     * Initialize Picasso instance and configuration. Call this on your Application's onCreate() method. Indicator is provided when on debug build
     *
     * @param c Context. Use the application's baseContext or applicationContext
     */
    fun initPicasso(c: Context) {
        val pic =
            if (BuildConfig.BUILD_TYPE == "debug") {
                Picasso.Builder(c)
                    .indicatorsEnabled(true)
                    .loggingEnabled(true)
                    .build()
            } else {
                Picasso.Builder(c).build()
            }
        try {
            Picasso.setSingletonInstance(pic)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    /**
     * Set an image downloaded from a URL to an [ImageView]. Also handles caching
     *
     * @param c Context
     * @param iv [ImageView] used to display the image
     * @param imgURL the url where the image is located
     * @param placeholder the resource id for placeholder when the image is being downloaded. Use 0 to not use it
     * @param errorImg The resource id of the displayed image when the image fails to download. Use 0 to display nothing
     */
    fun setImage(c: Context, iv: ImageView, imgURL: String, placeholder: Int = 0, errorImg: Int = 0) {
        if (imgURL.isEmpty()) {
            return
        }

        var req = Picasso.get().load(imgURL)
        if (placeholder != 0) {
            req = req.placeholder(placeholder)
        }
        if (errorImg != 0) {
            req = req.error(errorImg)
        }
        req.into(iv)
    }

    /**
     * Returns a [Bitmap] from the given file path or URI (content://...)
     */
    fun getBitmapFromUriOrPath(c: Context, uriOrPath: String, size: Int, onResult: (Bitmap?) -> Unit) {
        if (uriOrPath.startsWith("content://")) {
            val uri = Uri.parse(uriOrPath)
            getScaledBitmap(c, uri, size, onResult)
        } else {
            val file = File(uriOrPath)
            onResult(getScaledBitmap(file, size))
        }
    }

    /**
     * Save a [Bitmap] object to a JPEG file
     *
     * @param b Bitmap object
     * @param format image format
     * @param q quality level. 100 is the highest, 0 is the lowest
     * @param f the target file where the Bitmap will be saved. Make sure the folder containing the file is already created
     * @exception Exception if the file or the bitmap is invalid, or the folder has not been created yet
     */
    fun compress(b: Bitmap, format: Bitmap.CompressFormat, q: Int, f: File) {
        try {
            val fos = FileOutputStream(f)
            b.compress(format, q, fos)
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Scale a [Bitmap] object by the provided width
     *
     * @param b Bitmap
     * @param size the width of the new [Bitmap]. The height will be scaled with the ratio of the width
     * @return the scaled Bitmap. The returned Bitmap will have the width of the "size" parameter.
     *          If OutOfMemory happens, it will return the original bitmap
     */
    fun getScaledBitmap(b: Bitmap, size: Int): Bitmap {
        var outWidth = 0
        var outHeight = 0
        if (b.width > b.height) {
            outWidth = size
            outHeight = (b.height * size) / b.width
        } else  {
            outHeight = size
            outWidth = (b.width * size) / b.height
        }

        try {
            return Bitmap.createScaledBitmap(b, outWidth, outHeight, false)
        } catch (e: OutOfMemoryError) {
            return b
        }
    }

    /**
     * Get a scaled bitmap from an URI. This operation is done inside a thread
     *
     * @param c Context for contentDescriptor
     * @param uri the uri object of the image
     * @param size the resulting width. If 0 is passed, then it will return original bitmap without scaling
     * @param onResult The resulting bitmap will be provided here
     */
    fun getScaledBitmap(c: Context, uri: Uri, size: Int, onResult: (Bitmap?) -> Unit) {
        val thread = Thread(Runnable {
            onResult(getScaledBitmapNonThread(c, uri, size))
        })
        thread.start()
    }

    /**
     * Non-thread version of [getScaledBitmap]
     */
    fun getScaledBitmapNonThread(c: Context, uri: Uri, size: Int): Bitmap? {
        val descriptor = try { c.contentResolver.openFileDescriptor(uri, "r") } catch (e: Exception) { null }
        if (descriptor == null) return null
        val fileDescriptor = descriptor.fileDescriptor
        val imageBmp = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        descriptor.close()
        return if (size == 0) {
            imageBmp
        } else {
            getScaledBitmap(imageBmp, size)
        }
    }

    /**
     * Rotate a bitmap with the given angle. It creates a new bitmap rather than modifying source
     *
     * @param bmp source bitmap
     * @param angle angle in degree
     *
     * @return new rotated bitmap or the source bitmap if it hits OOM
     */
    fun rotateBitmap(bmp: Bitmap, angle: Float): Bitmap =
        try {
            val mat = Matrix()
            mat.postRotate(angle)
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, mat, true)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            bmp
        }

    /**
     * Get a scaled bitmap from the image file
     *
     * @param f the file where the image file is located
     * @param size the width of the returned [Bitmap]
     * @return the scaled Bitmap with the width the same value as the supplied "size" parameter
     */
    fun getScaledBitmap(f: File, size: Int): Bitmap {
        val opt = BitmapFactory.Options()
        opt.inJustDecodeBounds = true
        BitmapFactory.decodeFile(f.absolutePath, opt)

        opt.inJustDecodeBounds = false
        opt.inSampleSize = opt.outWidth / size
        opt.inPreferredConfig = Bitmap.Config.ARGB_8888

        return BitmapFactory.decodeFile(f.absolutePath, opt)
    }
}