package de.maengelmelder.mainmodule.utils.images

import android.app.Activity
import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import de.maengelmelder.mainmodule.utils.ActivityUtil
import java.io.File

/**
 * ## Overview
 * This object handles picture selection using camera and gallery. It also handles the required permissions and it's compatible from Android 4.0 - 8.0
 *
 * ## Permissions
 * - Accessing + writing external file storage (Gallery)
 * - Camera
 */
object PhotoSelector {

    /**
     * @property AUTH_EXT_STORAGE_DOC Authority for external storage access (Android 7.0+)
     * @property AUTH_DOWNLOADS Authority for downloaded documents (Android 7.0+)
     * @property AUTH_MEDIA Authority for gallery (Android 7.0+)
     * @property AUTH_GOOGLE_PHOTOS Authority for google photos
     * @property URI_SCHEME_CONTENT Uri schema for arbitrary content. Usually for Google Photos
     * @property URI_SCHEME_FILE Uri schema for file
     *
     * @property PHOTO_PERM_REQ_CODE Request code for camera permission
     * @property GALLERY_PERM_REQ_CODE Request code for accessing gallery
     * @property PHOTO_RESULT_CODE Result code for approved camera access
     * @property GALLERY_RESULT_CODE Result code for approved gallery access
     */
    val AUTH_EXT_STORAGE_DOC    = "com.android.externalstorage.documents"
    val AUTH_DOWNLOADS          = "com.android.providers.downloads.documents"
    val AUTH_MEDIA              = "com.android.providers.media.documents"
    val AUTH_GOOGLE_PHOTOS      = "com.google.android.apps.photos.content"
    val URI_SCHEME_CONTENT      = "content"
    val URI_SCHEME_FILE         = "file"

    val PHOTO_PERM_REQ_CODE = 1
    val GALLERY_PERM_REQ_CODE = 2
    val PHOTO_RESULT_CODE = 101
    val GALLERY_RESULT_CODE = 102

    /**
     * Starts the camera. Also handles required permission
     *
     * @param a Activity
     * @param c Context
     * @param outputFile The file where the result of the captured image will be saved. Provide the path along with filename and extension. Make sure the folder pointing to the file is already created.
     */
    fun startCamera(a: Activity, c: Context, outputFile: File?) {
        if (ActivityUtil.isPermissionGranted(c, android.Manifest.permission.CAMERA)) {
            val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            if (outputFile != null) {
                // Avoid UriExposedException on Android N and above
                val uri = getUriFromFile(c, outputFile)
                i.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                i.clipData = ClipData.newRawUri("", uri)
                i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            a.startActivityForResult(i, PHOTO_RESULT_CODE)
        }
    }

    /**
     * Starts image capture without camera permission
     */
    fun startCameraNoPerm(a: Activity, c: Context, outputFile: File?) {
        val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (outputFile != null) {
            // Avoid UriExposedException on Android N and above
            val uri = getUriFromFile(c, outputFile)
            i.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            i.clipData = ClipData.newRawUri("", uri)
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        a.startActivityForResult(i, PHOTO_RESULT_CODE)
    }

    /**
     * Returns the absolute path to an image file from the provided URI
     *
     * @param c Context
     * @param uri the file URI.
     *
     * @return the path to the image file, or null if the uri is invalid or pointed to a non-existant file
     */
    fun getImagePathFromURI(c: Context, uri: Uri): String? {
        var path: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) { // Below API 19
            val cLoader = androidx.loader.content.CursorLoader(
                c,
                uri,
                proj,
                null,
                null,
                null
            )
            val cursor = cLoader.loadInBackground()

            if (cursor != null) {
                cursor.moveToFirst()

                path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                cursor.close()
            }

        } else { // API 19 and above

            if (DocumentsContract.isDocumentUri(c, uri)) {
                when (uri.authority) {
                    // Uri is a document in storage
                    AUTH_EXT_STORAGE_DOC -> {
                        val docid = DocumentsContract.getDocumentId(uri)
                        val split = docid.split(":")
                        return Environment.getExternalStorageDirectory().absolutePath + "/" + split[1]
                    }
                    // URI leads to downloaded file
                    AUTH_DOWNLOADS -> {
                        val docid = DocumentsContract.getDocumentId(uri)
                        val contentURI = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), docid.toLong())
                        return getDataColumn(c, contentURI)
                    }
                    // URI is a media file
                    AUTH_MEDIA -> {
                        val wholeid = DocumentsContract.getDocumentId(uri)
                        val split = wholeid.split(":")
                        val cUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        val sel = MediaStore.Images.Media._ID+"=?"
                        val selArgs = arrayOf(split[1])
                        return getDataColumn(c, cUri, sel, selArgs)
                    }
                }
            } else if (uri.scheme == URI_SCHEME_CONTENT) {
                if (uri.authority == AUTH_GOOGLE_PHOTOS) {
                    return uri.lastPathSegment
                }
                return getDataColumn(c, uri, null, null)
            } else if (uri.scheme == URI_SCHEME_FILE) {
                return uri.path
            }
        }
        return path
    }

    /**
     * Get the content of _data column from a URI. The content of _data column contains the absolute path to the file pointed by the URI
     * @see getImagePathFromURI
     *
     * @param c Context
     * @param uri URI of the file
     * @param sel the selected columns for the WHERE clause. If null, no where clause
     * @param selArgs the values for columns matched by the 'sel' parameter. Ignored if the "sel" parameter is null
     */
    private fun getDataColumn(c: Context, uri: Uri, sel: String? = null, selArgs: Array<String>? = null): String? {
        var cursor: Cursor? = null
        val dataCol = "_data"
        val proj = arrayOf(dataCol)
        var result : String? = null

        try {
            cursor = c.contentResolver.query(uri, proj, sel, selArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idx = cursor.getColumnIndexOrThrow(dataCol)
                result = cursor.getString(idx)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return result
    }

    /**
     * Get the URI from the file. The file provider authority is the package name concatenated by ".fileprovider", as seen from the manifest file
     * @see FileProvider.getUriForFile
     *
     * @param c Context
     * @param f File
     * @return the URI of the file
     *
     */
    fun getUriFromFile(c: Context, f: File): Uri = FileProvider.getUriForFile(c, c.packageName + ".fileprovider", f)

    /**
     * Copies resource from the old URI to a new image [Uri].
     * This is handy when getting an image URI from a different application (e.g. Google Photos) since other applications may remove permission after caller app is closed
     */
    fun copyImageAndGetUri(c: Context, oldUri: Uri): Uri {
        if (oldUri.authority != null) {
            val imagePath = getImagePathFromURI(c, oldUri)?: "image.jpg"
            val filename = if (imagePath.contains("/")) {
                imagePath.substring(imagePath.lastIndexOf("/"))
            } else {
                imagePath
            }
            val inpStream = c.contentResolver.openInputStream(oldUri)
            val bmp = BitmapFactory.decodeStream(inpStream)
            val newUriPath = MediaStore.Images.Media.insertImage(c.contentResolver, bmp, filename, "")
            return Uri.parse(newUriPath)
        }
        return oldUri
    }

    /**
     * Open the gallery to let user choose a photo from it. Also handles permission
     *
     * @param a Activity
     * @param c Context
     * @param title The title when the prompt shows up for user to choose which app that handles the gallery
     */
    fun startGallery(a: Activity, c: Context, title: String) {
        if (ActivityUtil.isPermissionGranted(c, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
            a.startActivityForResult(Intent.createChooser(intent, title), GALLERY_RESULT_CODE)
        }
    }

    /**
     * Image from gallery. This also enables picking images from other providers such as Google Photos/Dropbox/etc
     */
    fun startGallery2(a: Activity, title: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        a.startActivityForResult(Intent.createChooser(intent, title), GALLERY_RESULT_CODE)
    }
}