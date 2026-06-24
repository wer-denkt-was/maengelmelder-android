package de.maengelmelder.mainmodule.network.collectives.coroutines

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Bms
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1CreateBundle
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1SendMessage
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1UploadFileToBundle
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.CreateMessageResponse
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.UploadedFileInfo
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.service.receivers.BroadcastFilterList
import de.maengelmelder.mainmodule.service.util.Broadcaster
import de.maengelmelder.mainmodule.utils.AccessibilityUtil
import de.maengelmelder.mainmodule.utils.UserData
import de.maengelmelder.mainmodule.utils.images.ImageManipulator
import de.maengelmelder.mainmodule.utils.images.ImageOrientatorCoroutine
import de.maengelmelder.mainmodule.utils.images.PhotoSelector
import de.maengelmelder.mainmodule.utils.notification.NotifUtility
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.*

class APIUploadMessage(c: Context,
                       msgBuilder: MessageBuilder,
                       msgStartTS: Long,
                       notifId: Int) : IOCoroutine<CreateMessageResponse>(c, listOf()) {


    companion object {
        val STATUS = "de.mengelmelder.uploadmessage.status"
        val STATUS_SUCCESS = "success"
        val STATUS_FAILED = "failed"
        val MESSAGE = "de.mengelmelder.uploadmessage.message"

        private val STATUS_IMAGE_UPLOAD_FAILED = -2
    }

    // Text
    private val mTextUploadNotifTitle = c.getString(R.string.message_upload_notif_title)

    // Notification
    private val mNotifId = notifId
    private val mChannelId = c.getString(R.string.notif_channel_id)
    private val mNotifWiz =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                NotifUtility.getWizard(c,
                        mChannelId, c.getString(R.string.app_name),
                        "", NotificationManager.IMPORTANCE_LOW)
            else NotifUtility.getWizardOld(c, mChannelId)
    private val builder = mNotifWiz.getBuilder()

    // Message builder and other parameters
    private val mMessageB = msgBuilder
    private val mMsgStartTS = msgStartTS

    // Get user cred
    private val userCred = UserData.getUserCred(c)

    // Database instance
    private val mDB = MMDB.instance(c)

    override fun beforeDispatcherIO() {
        // Notify start of service
        builder.setSmallIcon(R.drawable.upload_notif).setContentTitle(mTextUploadNotifTitle)
        mNotifWiz.notify(mNotifId, builder.build())

        // Update entry to database (status + log)
        mDB.updateMessage(mMessageB.messageId, Pair(mDB.constants.COL_UPLOAD_STATUS, mDB.constants.STATUS_UPLOADING))
        mMessageB.addAdditionalData(Message.DATA_UPDATELOG, mTextUploadNotifTitle)
        mDB.updateMessage(mMessageB.messageId, Pair(mDB.constants.COL_ADDITIONAL, mMessageB.message.additionalDataToJSON().toString()))

        // Broadcast to UI and announce for accessibility
        broadcast(mDB.constants.STATUS_UPLOADING, mTextUploadNotifTitle)
        AccessibilityUtil.announce(context, mTextUploadNotifTitle)
    }

    override fun insideDispatcherIO(scope: CoroutineScope): Pair<CreateMessageResponse?, BaseResponse?> {
        // Grab system info and domainid
        val sysinfo = mDB.getSystem(mMessageB.category.systemId)
        val domainid = mMessageB.category.domainId.toInt()

        // Contains the list of all uploaded files
        var uploadedFiles = listOf<UploadedFileInfo>()
        var allFilesUploaded = true

        val userDomainMatchesCategory = userCred?.isUserValid() == true && userCred.domain?.id == domainid.toString()
        var msgUploadAPI = MMv1SendMessage(context, mMessageB, mMsgStartTS).apply {
            attachUserCred = userDomainMatchesCategory
        }

        // Don't upload any photo if the category doesn't need photo
        val categoryDoesntNeedPhoto = mMessageB.category.photoReq == Category.PHOTO_NEVER
        if (mMessageB.hasImage() && !categoryDoesntNeedPhoto) {
            // Use bundle to upload images
            val createBundle = MMv1CreateBundle(context, domainid).apply {
                sysinfo?.let { s -> externalSystemInfo = s }
                attachUserCred = userDomainMatchesCategory
            }
            val respBundle = createBundle.doExecuteAPI()?: BaseResponse(-1, "")
            val bundle = createBundle.parseResponse(respBundle)
            if (bundle.isEmpty()) {
                // Failed to create bundle
                val fail = BaseResponse(STATUS_IMAGE_UPLOAD_FAILED, "")
                return Pair(null, fail)
            } else {
                // Bundle is successfully created and token is available
                // Iterate each image and upload them to bundle
                mMessageB.iterateImagePaths { p ->
                    // Fix image rotation
                    val imageBmp = getFixedRotationBitmap(p)
                    val filename = if (p.startsWith("content://")) {
                        PhotoSelector.getImagePathFromURI(context, Uri.parse(p))
                    } else {
                        File(p).name
                    }
                    imageBmp?.let { img ->
                        // Upload the image
                        val uploadFileToBundle = MMv1UploadFileToBundle(
                                context, domainid, bundle, filename?: "image.jpg", img
                        ).apply {
                            externalSystemInfo = sysinfo
                            attachUserCred = userDomainMatchesCategory
                        }
                        val uploadResp = uploadFileToBundle.doExecuteAPI(null)
                        if (uploadResp != null) {
                            val files = uploadFileToBundle.parseResponse(uploadResp)
                            if (files.isNotEmpty()) {
                                // Store the list of the uploaded files for later checking
                                uploadedFiles = files
                            } else {
                                allFilesUploaded = false
                            }
                        } else {
                            allFilesUploaded = false
                        }
                    }
                }

                if (!allFilesUploaded) {
                    // 1 or more files failed to upload
                    val fail = BaseResponse(STATUS_IMAGE_UPLOAD_FAILED, "")
                    return Pair(null, fail)
                } else {
                    // All files are uploaded.
                    val filenames = arrayListOf<String>()
                    uploadedFiles.forEach { file -> filenames.add(file.filename) }
                    msgUploadAPI = MMv1SendMessage(context, mMessageB, mMsgStartTS, bundle, filenames.toTypedArray())
                }
            }
        }

        // Retrieve BmsSettings first
        val bmsAPI = MMv1Bms(context, mMessageB.category.domainId.toInt()).apply {
            if (sysinfo != null) externalSystemInfo = sysinfo
        }
        val bmsSettingsResp = bmsAPI.doExecuteAPI(null)?: BaseResponse(-1, "")
        if (bmsSettingsResp.isSuccess()) {
            // If user is admin, we can set the responsible id ahead depending on the bms settings
            bmsAPI.parseResponse(bmsSettingsResp)?.let { bms ->

                if (    // Can send responsible ID along with message
                        bms.responsibleSettings["show"] == 1 &&
                        // Responsibility should fall to user
                        bms.responsibleSettings["default_responsible"] == "current_user" &&
                        // User exists
                        userCred != null && userCred.isUserValid()) {

                    msgUploadAPI.addParameter("responsibleid", userCred.id)
                }
            }
        }

        // Retrieve the correct system info for uploading message
        if (sysinfo != null) msgUploadAPI.externalSystemInfo = sysinfo

        // Parent call to execute the API
        val resp = msgUploadAPI.doExecuteAPI(null)?: BaseResponse(-1, "")

        // Parse the result and return them
        if (resp.isSuccess()) {
            return Pair(msgUploadAPI.parseResponse(resp), null)
        } else {
            return Pair(null, msgUploadAPI.parseError(resp))
        }
    }

    override fun onSuccess(data: CreateMessageResponse) {
        when (data.code) {

            // Successful upload
            1, in 200..299 -> {
                // Update the message's server ID (not local ID) with the retrieved ID
                mDB.updateMessage(mMessageB.messageId,
                        mDB.constants.COL_SERVER_ID to data.msgId,
                        mDB.constants.COL_UPLOAD_STATUS to mDB.constants.STATUS_FINISHED,
                        mDB.constants.COL_UPLOADED_AT to Date().time.toString()
                )
                // Prepare notification
                val uploadFinishedContent = context.getString(R.string.message_upload_done_notif_content)
                builder.setSmallIcon(R.drawable.upload_notif_success)
                        .setColorized(true)
                        .setColor(Color.GREEN)
                        .setProgress(1, 1, false)
                        .setContentTitle(uploadFinishedContent)

                // Notify through notification
                mNotifWiz.notify(mNotifId, builder.build())

                // Tell UI and announce for screenreader
                broadcast(STATUS_SUCCESS, uploadFinishedContent)
                AccessibilityUtil.announce(context, uploadFinishedContent)
            }

            // Fail
            else -> onError(BaseResponse(data.code, data.msg))
        }
    }

    override fun onError(err: BaseResponse) {
        val initialMsg = context.getString(R.string.message_upload_fail_notif_title)
        var contentMsg = if (err.body.isEmpty()) initialMsg else err.body

        if (contentMsg.contains("apikey is not valid for")) {
            var categoryDomain = mMessageB.category.domainText
            if (categoryDomain.isEmpty()) {
                categoryDomain = context.getString(R.string.other_domain)
            }
            // Authentication error (e.g user tries to upload bms message of a category belonging to a domain
            // while being logged in to other domains
            contentMsg = context.getString(R.string.error_user_cannot_be_authenticated, categoryDomain)
        }

        if (err.code == STATUS_IMAGE_UPLOAD_FAILED) {
            contentMsg = context.getString(R.string.message_upload_fail_reason_image)
        }

        // Prepare notification
        builder.setSmallIcon(R.drawable.upload_notif_failed)
                .setColorized(true)
                .setColor(Color.RED)
                .setContentTitle(initialMsg)
                .setContentText(contentMsg)
        mNotifWiz.notify(mNotifId, builder.build())

        // Update the logs with the error message
        mMessageB.addAdditionalData(Message.DATA_UPDATELOG, contentMsg)
        mDB.updateMessage(mMessageB.messageId,
                mDB.constants.COL_ADDITIONAL to mMessageB.message.additionalDataToJSON().toString(),
                mDB.constants.COL_UPLOAD_STATUS to mDB.constants.STATUS_UPLOAD_FAILED)

        // Tell UI and announce for accessibility
        broadcast(STATUS_FAILED, contentMsg)
        AccessibilityUtil.announce(context, "$initialMsg, $contentMsg")
    }

    /**
     * Broadcast status and message to UI
     */
    private fun broadcast(status: String, msg: String) {
        Broadcaster.cast(context, BroadcastFilterList.MESSAGE_UPLOAD, mapOf(STATUS to status, MESSAGE to msg))
    }

    /**
     * Returns a bitmap from the given file with rotation fixed depending on the EXIF data.
     * This method is not thread-safe and should be executed in a thread
     */
    fun getFixedRotationBitmap(imageFilePath: String, maxWidth: Int = 0): Bitmap? {
        var bitmap : Bitmap? = null
        if (imageFilePath.startsWith("content://")) {
            bitmap = ImageManipulator.getScaledBitmapNonThread(context, Uri.parse(imageFilePath), maxWidth)
        } else {
            bitmap = if (maxWidth > 0) {
                ImageManipulator.getScaledBitmap(File(imageFilePath), maxWidth)
            } else {
                BitmapFactory.decodeFile(imageFilePath)
            }
        }

        return if (bitmap != null) {
            try {
                ImageOrientatorCoroutine(bitmap, imageFilePath).executeUnsafe()
            } catch (e: Exception) {
                bitmap
            }
        } else {
            null
        }
    }
}