package de.maengelmelder.mainmodule.utils.images

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.ref.WeakReference

class ImageOrientatorCoroutine(bmp: Bitmap, imgPath: String, reference: Any? = null) : ViewModel() {

    /**
     * @property mPath image path with the EXIF data
     * @property mBitmap the bitmap
     * @property mExecListener listener for the process, both before and after execution
     *
     * @property bitmap getter for mBitmap
     * @property imagePath getter for mPath
     */
    private val mPath = imgPath
    private val mBitmap = bmp
    private var mExecListener: ExecutionListener? = null

    private val imageViewRef = WeakReference<Any>(reference)
    val bitmap: Bitmap get() = mBitmap
    val imagePath: String get() = mPath

    /**
     * Sets the listener
     * @param l instance of [ExecutionListener]
     * @return this instance of [ImageOrientatorAsyncTask]
     */
    fun setListener(l: ExecutionListener): ImageOrientatorCoroutine {
        mExecListener = l
        return this
    }

    /**
     * Run the coroutine in IO-thread
     */
    fun execute() {
        mExecListener?.beforeExecuted()
        viewModelScope.launch {
            val newBitmap = withContext(Dispatchers.IO) { executeUnsafe() }
            mExecListener?.afterOrientation(newBitmap, imageViewRef)
        }
    }

    /**
     * Run the coroutine in blocking-fashion. Only use this inside a worker thread
     *
     * Fix the image orientation according to its EXIF metadata
     */
    fun executeUnsafe(): Bitmap {
        // Obtain the EXIF data from the image path
        val exifInt = try {
            androidx.exifinterface.media.ExifInterface(mPath)
        } catch (e: IOException) { null }

        // Exception from getting EXIF data from the given path. Return the original bitmap
        if (exifInt == null) return bitmap

        // Get the orientation value
        val orientation = exifInt.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED)

        // Rotate the bitmap depending on the given orientation
        return when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> {
                ImageManipulator.rotateBitmap(mBitmap, 90f)
            }
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> {
                ImageManipulator.rotateBitmap(mBitmap, 180f)
            }
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> {
                ImageManipulator.rotateBitmap(mBitmap, 270f)
            }
            else -> mBitmap
        }
    }

    /**
     * The methods outlined here are not thread-safe
     */
    interface ExecutionListener {
        fun beforeExecuted()
        fun afterOrientation(bmp: Bitmap?, iv: Any?)
    }
}