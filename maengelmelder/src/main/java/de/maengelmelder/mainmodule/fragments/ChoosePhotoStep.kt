package de.maengelmelder.mainmodule.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.preference.PreferenceManager
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.customviews.dialogs.ImageSelectionDialog
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.utils.AccessibilityUtil
import de.maengelmelder.mainmodule.utils.ActivityUtil
import de.maengelmelder.mainmodule.utils.ResourceProxy
import de.maengelmelder.mainmodule.utils.images.ImageManipulator
import de.maengelmelder.mainmodule.utils.images.ImageOrientatorCoroutine
import de.maengelmelder.mainmodule.utils.images.PhotoSelector
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import kotlin.Exception

/**
 *
 * This fragment shows the multiple photos selection from both gallery and camera.
 * Picked photos will have their [File.getAbsolutePath] be saved in the [MessageBuilder]
 */
class ChoosePhotoStep : BaseMessageStepFragment(), View.OnClickListener {

    /**
     * normal scaling for image preview
     */
    private val NORMAL_IMG_SCALE = 800

    /**
     * Image scaling for preview when memory is low
     */
    private val LOWMEM_IMG_SCALE = 320

    /**
     * Loaded image that are taken either from gallery or camera and is currently displayed
     */
    private var mLoadedImage: File? = null

    /**
     * Default scale for previewing image
     */
    private var mPreviewImgScale = NORMAL_IMG_SCALE

    /**
     * Button to remove displayed image.
     */
    private var mBtnRemoveImage: ImageView? = null

    /**
     * No image text
     */
    private var mTxtNoImage: TextView? = null

    /**
     * Progress bar when loading the image into preview
     */
    private var mImgLoadProgress: ProgressBar? = null

    /**
     * Imageview that displays the chosen image
     */
    private var mImage: ImageView? = null

    /**
     * Thumbnail of the image being displayed
     */
    private var mDisplayedImageThumbnail: ImageView? = null

    /**
     * List of images that are loaded (which are also saved to the database)
     */
    private var mImageList: LinearLayout? = null

    /**
     * Button to add new image
     */
    private var mBtnNewImage: ImageView? = null

    /**
     * Gallery containing all loaded images
     */
    private var mImagesGallery: HorizontalScrollView? = null

    private val bAllowMultipleImages = MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.MultipleImages]?: false
    private val mMaxImages = MMConstants.MaxImageUploadOnReportCreation

    // perm request handlers
    private lateinit var mCameraPermRequest: ActivityResultLauncher<String>

    override fun getLayoutId(): Int = R.layout.mm_fragment_choosephoto

    private var mImageSelDialog: ImageSelectionDialog? = null

    override fun onViewInflated(v: View?) {
        // Get references to the views
        mImage = v?.findViewById(R.id.image)
        mImageList = v?.findViewById(R.id.imagesscroll)
        mBtnRemoveImage = v?.findViewById(R.id.btn_remove_img)
        mTxtNoImage = v?.findViewById(R.id.txt_no_image)
        mImgLoadProgress = v?.findViewById(android.R.id.secondaryProgress)
        mImagesGallery = v?.findViewById(R.id.imagescrolllayout)
        mBtnNewImage = v?.findViewById(R.id.add_new_image)
        mBtnRemoveImage?.setOnClickListener(this)

        // Permission request handlers
        mCameraPermRequest = ActivityUtil.requestPermission(this) {
            if (it) launchCamera()
        }

        if (bAllowMultipleImages) {
            mImagesGallery?.visibility = View.VISIBLE
        } else {
            mImagesGallery?.visibility = View.GONE
        }

        // Explicitly ask users for permission to read external storage. Should be handled in Overview, but just to be sure
        context?.let { c ->
            mTxtNoImage?.setOnClickListener {
                val numImages = builder?.getNumOfImages()?: 0
                if (numImages < mMaxImages) {
                    getImageSelectionDialog(c).show()
                }
            }

            mBtnNewImage?.setOnClickListener {
                getImageSelectionDialog(c).show()
            }
        }

    }

    // In onResume(), the fragment will populate the area with saved photos if any
    override fun onResume() {
        super.onResume()

        builder?.let { b ->
            if (b.hasImage()) {
                mBtnRemoveImage?.visibility = View.VISIBLE
                mTxtNoImage?.visibility = View.GONE

                // Clear the gallery first and reload the saved photos
                mImageList?.removeAllViews()
                var idx = 0

                b.iterateImagePaths { path ->
                    context?.let { c ->
                        // Since Android 30, you will not be able to grab image from gallery using its image path
                        // Only from Uri
                        ImageManipulator.getBitmapFromUriOrPath(c, path, mPreviewImgScale) {
                            ActivityUtil.runOnUiThread(c) {
                                if (it != null) {
                                    try {
                                        addToList(it, path, idx == 0)
                                        idx += 1
                                    } catch (e: Exception) {
                                        // Can fail if image is already removed from the device
                                    }
                                }
                            }
                        }
                    }
                }

                toggleNewImageButton(b.getNumOfImages() < mMaxImages)
            } else {
                mBtnRemoveImage?.visibility = View.GONE
                mTxtNoImage?.visibility = View.VISIBLE
                toggleNewImageButton(true)
            }
        }

        context?.let { c ->
            mBtnNewImage?.setOnClickListener {
                getImageSelectionDialog(c).show()
            }

            // Show help first time only
            val pref = PreferenceManager.getDefaultSharedPreferences(c)
            val showHelpOnceOnly = pref.getBoolean("mmv2.step.photo.show_help", false)
            val helpText = getString(R.string.photo_step_onetime_info)
            if (!showHelpOnceOnly && helpText.isNotEmpty()) {
                pref.edit().putBoolean("mmv2.step.photo.show_help", true).apply()
                activity?.let { act ->
                    AlertDialog.Builder(act)
                        .setMessage(helpText)
                        .setCancelable(true)
                        .setPositiveButton(R.string.ok) { d: DialogInterface, which: Int ->
                            d.dismiss()
                        }.show()
                }
            }
        }
    }

    private fun getImageSelectionDialog(c: Context): ImageSelectionDialog {
        if (mImageSelDialog == null) {
            mImageSelDialog = ImageSelectionDialog(c,
                {
                    if (ActivityUtil.isPermissionGranted(c, Manifest.permission.CAMERA)) {
                        launchCamera()
                    } else {
                        mCameraPermRequest.launch(Manifest.permission.CAMERA)
                    }
                },
                {
                    launchGallery()
                })
        }
        return mImageSelDialog!!
    }

    override fun getTitle(): String = mContext?.getString(R.string.step_choose_photo)?: ""

    // Handles the image obtained after camera shot or selecting from gallery
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (Activity.RESULT_CANCELED == resultCode) {
            return
        }

        var savedImage: Bitmap? = null
        var origin = ""
        if (requestCode == PhotoSelector.PHOTO_RESULT_CODE && mLoadedImage != null) {
            // Load the image taken from camera. Scale it so it does not load too long when displaying
            val loaded = mLoadedImage
            origin = context?.getString(R.string.camera)?: "Camera"
            if (loaded != null) savedImage = ImageManipulator.getScaledBitmap(loaded, mPreviewImgScale)

            mLoadedImage?.let {file ->
                savedImage?.let { image ->
                    loadImageToPreview(file.path, image, origin)
                }
            }
            context?.let { c ->
                loaded?.let { l ->
                    try {
                        ResourceProxy.saveImageToMediaStore(c, l)
                    } catch (e: IOException) {
                        // Cannot create uri, cannot save the image
                        e.printStackTrace()
                    } catch (e: FileNotFoundException) {
                        // Image does not exist
                        e.printStackTrace()
                    } catch (e: IllegalArgumentException) {
                        // Image cannot be decoded
                        e.printStackTrace()
                    }
                }

                val announce = c.getString(R.string.acc_announce_photo_from_camera_taken,
                    ((builder?.getNumOfImages()?: 0) + 1).toString())
                AccessibilityUtil.announce(c, announce)
            }

        } else if (requestCode == PhotoSelector.GALLERY_RESULT_CODE) {
            // Load image taken from gallery
            val ctx = context
            if (ctx != null && data != null && data.data != null) {
                // This is an Uri, not file path
                /**
                 * If the uri has authority (e.g. taken from Google Photos or other non-default gallery app),
                 * then the given file located on the uri needs to be copied to a normal, non-restrictive uri.
                 *
                 * Apps like Google Photos only retain permissions temporarily.
                 * After the calling activity is closed, the uri becomes invalid and will throw an exception if accessed again
                 * This is, unfortunately, what Google has intended for Android 11+.
                 */
                val uriData = data.data
                if (uriData != null) {
                    val imgData = try {
                        PhotoSelector.copyImageAndGetUri(ctx, uriData)
                    } catch (e: Exception) {
                        uriData
                    }
                    origin = ctx.getString(R.string.gallery)
                    imgData.let { d ->
                        PhotoSelector.getImagePathFromURI(ctx, d)?.let { path ->
                            mLoadedImage = File(path)
                        }
                        // We limit the width of the preview image to 800px so it doesn't crash on big images
                        ImageManipulator.getScaledBitmap(ctx, d, 800) { bitmap ->
                            savedImage = bitmap
                            ActivityUtil.runOnUiThread(ctx) {
                                savedImage?.let { image ->
                                    loadImageToPreview(d.toString(), image, origin)
                                }
                            }
                        }
                    }

                    val announce = ctx.getString(R.string.acc_announce_photo_from_gallery_taken,
                        ((builder?.getNumOfImages()?: 0) + 1).toString())
                    AccessibilityUtil.announce(ctx, announce)
                }
            }
        }
    }

    fun loadImageToPreview(uriPath: String, image: Bitmap, origin: String) {
        // Add to Message
        if (!bAllowMultipleImages) {
            builder?.clearImages()
            mImageList?.removeAllViews()
        }

        builder?.addImagePath(uriPath)
        addToList(image, uriPath, true)
        if (mBtnRemoveImage?.visibility != View.VISIBLE) {
            mBtnRemoveImage?.visibility = View.VISIBLE
        }
        // Hide "no-image" text
        mTxtNoImage?.visibility = View.GONE

        val numImages = builder?.getNumOfImages()?: 0

        toggleNewImageButton(numImages < mMaxImages)
        context?.let { c ->
            AccessibilityUtil.announce(c, c.getString(R.string.acc_announce_foto_taken, origin))
        }
    }

    /**
     * Add a given Bitmap to the list of photos and display it if necessary
     *
     * @param bmp Bitmap
     * @param path absolute path to the image that contains the Bitmap
     * @param display whether to display this image or not
     */
    private fun addToList(bmp: Bitmap, path: String, display: Boolean = true) {
        // Set thumbnail image
        val imgView = ImageView(context)
        imgView.maxHeight = 90
        imgView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        imgView.setPadding(5,5,15,5)

        if (BuildConfig.debug) imgView.setBackgroundColor(Color.GREEN)

        // Scale the image to max 256px-width
        val bitmap = ImageManipulator.getScaledBitmap(bmp, 256)
        imgView.setImageBitmap(bitmap)

        // Set the listener to the thumbnail. When it is clicked, it will be previewed in bigger scale
        val imgSelected = OnImageSelected(bmp, path)
        imgView.setOnClickListener(imgSelected)
        imgView.tag = imgSelected

        // Set content description for image
        imgView.contentDescription = getString(R.string.acc_cd_fotostep_image, path)

        // Add to the list of thumbnails and display it if necessary
        mImageList?.addView(imgView)
        if (display) imgSelected.onClick(imgView)
    }

    // This step is complete if the build has at least one image picked. In practice, image picking is not mandatory
    override fun isStepComplete(): Boolean = when (builder?.category?.photoReq) {
        Category.PHOTO_REQ -> builder?.hasImage() == true
        Category.PHOTO_NEVER -> builder?.hasImage() == false
        else -> true
    }

    override fun onClick(p0: View?) {
        when (p0) {
            // Remove previewed image
            mBtnRemoveImage -> {
                mImage?.tag?.let {
                    val imgSelListener = it as OnImageSelected
                    // Remove from builder
                    builder?.removeImagePath(imgSelListener.imagePath)
                    // Remove the thumbnail image
                    mImageList?.removeView(mDisplayedImageThumbnail)
                    // Remove preview
                    mImage?.setImageResource(0)
                    // Remove reference to the thumbnail
                    mDisplayedImageThumbnail = null
                    // Check if builder still has an image
                    if (builder?.hasImage() == false) {
                        mBtnRemoveImage?.visibility = View.GONE
                        mTxtNoImage?.visibility = View.VISIBLE
                    } else {
                        mBtnRemoveImage?.visibility = View.VISIBLE
                        mTxtNoImage?.visibility = View.GONE
                    }
                    // Enable adding more image
                    toggleNewImageButton(true)
                }
            }
        }
    }

    override fun isLoading(): Boolean = false

    /**
     * Method to launch camera. it also handles the naming convention of the resulting image
     */
    private fun launchCamera() {
        activity?.let { act ->
            context?.let { ctx ->
                val curTime = Date().time
                // The captured photo will be named photo_<message Id>_<timestamp> and has .jpg extension
                mLoadedImage = ResourceProxy.getPhotoFilePath(ctx,
                        "photo_${builder?.messageId}_$curTime",
                        "jpg")
                PhotoSelector.startCamera(act, ctx, mLoadedImage)
            }
        }
    }

    /**
     * Method to launch gallery
     */
    private fun launchGallery() {
        activity?.let { act -> PhotoSelector.startGallery2(act, getString(R.string.select_photo)) }
    }

    override fun shouldPromptBeforeChange(): Boolean = builder?.hasImage() == false

    override fun executeBeforeChange() {
        builder?.let { b ->
            context?.let { ctx ->
                // Update the photos column in database
                val db = MMDB.instance(ctx)
                db.updateMessage(b.message.id,
                        db.constants.COL_PHOTO_PATH to (b.getPhotoPathsAsString())
                )
            }
        }
    }

    // Show prompt about missing photo to the user before changing to another fragment
    override fun promptBeforeChange(f: (Boolean) -> Unit) {
        activity?.let { act ->
            val b = AlertDialog.Builder(act)
                    .setTitle(R.string.warn_no_photo_chosen_title)
                    .setMessage(R.string.warn_no_photo_chosen)
                    .setPositiveButton(R.string.ok) {
                        dialog: DialogInterface, which: Int -> f(true)
                    }
            b.show()
        }
    }

    private fun toggleNewImageButton(toggle: Boolean) {
        if (toggle) {
            mBtnNewImage?.visibility = View.VISIBLE
            mBtnNewImage?.setOnClickListener { _ ->
                context?.let { c ->
                    getImageSelectionDialog(c).show()
                }
            }
        } else {
            mBtnNewImage?.visibility = View.GONE
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // When the memory is low, the preview images will have lower scale
        mPreviewImgScale = LOWMEM_IMG_SCALE
    }

    /**
     * Listener to the thumbnail images.
     */
    inner class OnImageSelected(bmp: Bitmap, path: String) : ImageOrientatorCoroutine.ExecutionListener, View.OnClickListener {

        /**
         * @property mRotatedBmp resulting bitmap from [mBitmap] that has been properly rotated according to the given EXIF's orientation matadata
         * @property mPath path to the file that contains [mBitmap]. This path is used to load the EXIF metadata
         */
        private val mBitmap = bmp
        private var mRotatedBmp: Bitmap? = null
        private val mPath = path

        val imagePath: String get() = mPath

        /**
         * Clean the loaded bitmaps. After this call, referenced bitmaps in this instance will be unusable and the instance
         * should not be used any longer
         */
        fun clean() {
            mBitmap.recycle()
            mRotatedBmp?.recycle()
            mRotatedBmp = null
        }

        override fun onClick(v: View?) {
            // Set the thumbnail as the displayed one
            if (v is ImageView) {
                mDisplayedImageThumbnail = v
            }

            // Rotate the bitmap based on the given EXIF data
            if (mRotatedBmp != null) {
                mImage?.setImageBitmap(mRotatedBmp)
            } else {
                mImage?.let { ImageOrientatorCoroutine(mBitmap, mPath).setListener(this).execute() }
            }

            // Sets the big preview's tag to this one
            mImage?.tag = this@OnImageSelected
        }

        override fun beforeExecuted() {
            // Show loading indicator
            mImgLoadProgress?.visibility = View.VISIBLE
        }

        override fun afterOrientation(bmp: Bitmap?, iv: Any?) {
            // Disable loading indicator and show the rotated bitmap
            mRotatedBmp = bmp
            mRotatedBmp?.let { mImage?.setImageBitmap(it) }
            mImgLoadProgress?.visibility = View.GONE
        }
    }
}