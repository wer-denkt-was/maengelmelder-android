package de.maengelmelder.mainmodule.service.tasks

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1CreateBundle
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1SendMessage
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1UploadFileToBundle
import de.maengelmelder.mainmodule.network.responses.*
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.UploadedFileInfo
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.service.receivers.BroadcastFilterList
import de.maengelmelder.mainmodule.service.util.Broadcaster
import de.maengelmelder.mainmodule.utils.AccessibilityUtil
import de.maengelmelder.mainmodule.utils.images.ImageManipulator
import de.maengelmelder.mainmodule.utils.images.ImageOrientatorCoroutine
import de.maengelmelder.mainmodule.utils.images.PhotoSelector
import de.maengelmelder.mainmodule.utils.notification.NotifUtility
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by christian on 07.09.17.
 *
 * This task encapsulates [MMv1SendMessage] to provide more insights/info/utility to the upload process. These include:
 * - Notification
 * - broadcasts to UI
 * - Automatically upload remaining images after the message is successfully uploaded
 * - Auto-deletion of remaining images/messages after successful upload (configurable)
 *
 */
@Deprecated(
        message = "AsyncTask is deprecated since SDK 30",
        replaceWith = ReplaceWith("network.collectives.coroutines.APIUploadMessage")
)
internal class MessageCreateTask(c: Context, messageB: MessageBuilder, msgStartTS: Long,
                        notifId: Int)
    : MMBMS<CreateMessageResponse, BaseResponse>(c, ""),
        MMBMS.BMSListener<CreateMessageResponse, BaseResponse> {

    companion object {
        val STATUS = "de.mengelmelder.uploadmessage.status"
        val STATUS_SUCCESS = "success"
        val STATUS_FAILED = "failed"

        val MESSAGE = "de.mengelmelder.uploadmessage.message"
    }

    // Reference to context. Useful to keep away context leaking. Have to check for null, tho
    private val mRefCtx = WeakReference(c)

    // Text
    private val mTextUploadNotifTitle = c.getString(R.string.message_upload_notif_title)

    // Notification elements
    private val mNotifId = notifId
    private val mChannelId = c.getString(R.string.notif_channel_id)
    private val mNotifWiz =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                NotifUtility.getWizard(c,
                    mChannelId, c.getString(R.string.app_name),
                    "", NotificationManager.IMPORTANCE_LOW)
            else NotifUtility.getWizardOld(c, mChannelId)
    private val builder = mNotifWiz.getBuilder()
    private val mMessageB = messageB
    private var mEmail: String? = null

    // Database instance
    private val mDB = MMDB.instance(c)

    private val mMessageStartTS = msgStartTS

    private lateinit var mMsgSendAPI: MMv1SendMessage
    /**
     * Set the email for message subscription
     */
    fun setSubscription(email: String) {
        mEmail = email
    }

    /**
     * Initiate and inform user that the upload service is starting
     */
    override fun onPreExecute() {
        // Notify start of service
        builder.setSmallIcon(R.drawable.upload_notif)
                .setContentTitle(mTextUploadNotifTitle)
        mNotifWiz.notify(mNotifId, builder.build())

        // Update entry to database (status + log)
        mDB.updateMessage(mMessageB.messageId, Pair(mDB.constants.COL_UPLOAD_STATUS, mDB.constants.STATUS_UPLOADING))
        mMessageB.addAdditionalData(Message.DATA_UPDATELOG, mTextUploadNotifTitle)
        mDB.updateMessage(mMessageB.messageId, Pair(mDB.constants.COL_ADDITIONAL, mMessageB.message.additionalDataToJSON().toString()))

        // Broadcast to UI
        broadcast(mDB.constants.STATUS_UPLOADING, mTextUploadNotifTitle)
        mRefCtx.get()?.let {
            AccessibilityUtil.announce(it, mTextUploadNotifTitle)
        }

        super.onPreExecute()
    }

    /**
     * The method overrides the [MMv1SendMessage.doInBackground] method to intercept the result. This is useful since we are still in the
     * background thread and we do not want to end the thread and reach [onPostExecute] before we upload the rest of the remaining image
     */
    override fun doInBackground(vararg p0: Any?): BaseResponse {
        val context = mRefCtx.get()
        val sysinfo = mDB.getSystem(mMessageB.category.systemId)
        val domainid = mMessageB.category.domainId

        var uploadedFiles = listOf<UploadedFileInfo>()
        var allFilesUploaded = true

        mMsgSendAPI = MMv1SendMessage(context!!, mMessageB, mMessageStartTS)

        if (mMessageB.hasImage()) {
            if (mMessageB.getNumOfImages() == 1) {
                // Only 1 image -> upload normally with picture
                val path = mMessageB.getImagePath(0)
                val imgPath = if (path.startsWith("content://")) {
                    PhotoSelector.getImagePathFromURI(context, Uri.parse(path))
                } else {
                    path
                }
                val imageBmp = getFixedRotationBitmap(path)
                imageBmp?.let { b -> mMsgSendAPI.addSingleImageBitmap(b, imgPath?: "data/image.jpg") }
            } else {
                // Multiple images -> use bundle
                val createBundle = MMv1CreateBundle(context, domainid.toInt()).apply {
                    sysinfo?.let { s -> system = s }
                }

                val respBundle = createBundle.doExecuteAPI()?: BaseResponse(-1, "")
                val bundle = createBundle.parseResponse(respBundle)
                if (bundle.isEmpty()) {
                    // Empty bundle. Error somewhere in server
                    val fail = BaseResponse(-1, "")
                    onFail(fail)
                    return fail
                } else {
                    // begin uploading image to bundle
                    mMessageB.iterateImagePaths { p ->
                        // Fix image rotation first, then upload
                        val imageBmp = getFixedRotationBitmap(p)
                        val filename = if (p.startsWith("content://")) {
                            PhotoSelector.getImagePathFromURI(context, Uri.parse(p))
                        } else {
                            File(p).name
                        }
                        imageBmp?.let { img ->
                            val uploadFileToBundle = MMv1UploadFileToBundle(
                                    context, domainid.toInt(), bundle, filename?: "image.jpg", img
                            ).apply {
                                externalSystemInfo = sysinfo
                            }
                            val uploadResp = uploadFileToBundle.doExecuteAPI(null)
                            if (uploadResp != null) {
                                val files = uploadFileToBundle.parseResponse(uploadResp)
                                if (files.isNotEmpty()) {
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
                        val fail = BaseResponse(-1, "")
                        onFail(fail)
                        return fail
                    } else {
                        // All files are uploaded
                        val filenames = arrayListOf<String>()
                        uploadedFiles.forEach { file -> filenames.add(file.filename) }
                        mMsgSendAPI = MMv1SendMessage(mRefCtx.get()!!,
                                mMessageB, mMessageStartTS,
                                bundle, filenames.toTypedArray())
                    }
                }
            }
        }

        // Retrieve the correct system info for uploading message
        if (sysinfo != null) mMsgSendAPI.externalSystemInfo = sysinfo

        // Parent call to execute the API
        val resp = mMsgSendAPI.doExecuteAPI(null)?: BaseResponse(-1, "")

        // Parse the result. It reaches exception if the message upload is not successful
        var result: CreateMessageResponse? = null
        var failResult: BaseResponse? = null
        if (resp.isSuccess()) {
            try {
                result = parseResponse(resp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                failResult = parseError(resp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Check result from POST api/v1/domain/<domainid>/message
        if (result != null) {
            when (result.code) {

                // Successful upload
                1, in 200..299 -> {
                    // Update the message's server ID (not local ID) with the retrieved ID
                    mDB.updateMessage(mMessageB.messageId,
                            mDB.constants.COL_SERVER_ID to result.msgId,
                            mDB.constants.COL_UPLOAD_STATUS to mDB.constants.STATUS_FINISHED,
                            mDB.constants.COL_UPLOADED_AT to Date().time.toString()
                    )
                    result.run { onData(this) }
                }

                // Fail
                else -> failResult?.let { f -> onFail(f) }
            }
        } else {
            failResult?.let { f -> onFail(f) }
        }

        return resp
    }

    /**
     * Returns a bitmap from the given file with rotation fixed depending on the EXIF data.
     * This method is not thread-safe and should be executed in a thread
     */
    fun getFixedRotationBitmap(imageFilePath: String, maxWidth: Int = 0): Bitmap? {
        var bitmap : Bitmap? = null
        if (imageFilePath.startsWith("content://")) {
            context?.let { c ->
                bitmap = ImageManipulator.getScaledBitmapNonThread(c, Uri.parse(imageFilePath), maxWidth)
            }
        } else {
            bitmap = if (maxWidth > 0) {
                ImageManipulator.getScaledBitmap(File(imageFilePath), maxWidth)
            } else {
                BitmapFactory.decodeFile(imageFilePath)
            }
        }

        if (bitmap != null) {
            return try {
                ImageOrientatorCoroutine(bitmap!!, imageFilePath).executeUnsafe()
            } catch (e: Exception) {
                bitmap
            }
        } else {
            return null
        }
    }

    override fun parseResponse(resp: BaseResponse): CreateMessageResponse = mMsgSendAPI.parseResponse(resp)

    override fun parseError(resp: BaseResponse): BaseResponse = mMsgSendAPI.parseError(resp)

    override fun getUrlParam(): Map<String, String?>? = mMsgSendAPI.getQueryParameters()

    override fun onData(data: CreateMessageResponse) {
        when (data.msgId) {

            // Failed due to problem with app / server (e.g. bypassed timeout, endpoint expired (IOExc), etc.)
            "-1" -> onFail(BaseResponse(-1, data.msg))

            // Successful upload
            else -> {

                // Prepare notification
                val uploadFinishedContent = mRefCtx.get()?.getString(R.string.message_upload_done_notif_content)?: ""
                builder.setSmallIcon(R.drawable.upload_notif_success)
                        .setColorized(true)
                        .setColor(Color.GREEN)
                        .setProgress(1, 1, false)
                        .setContentTitle(uploadFinishedContent)

                // Notify through notification
                mNotifWiz.notify(mNotifId, builder.build())

                // Tell UI
                broadcast(STATUS_SUCCESS, uploadFinishedContent)

                // Announce for screenreader
                mRefCtx.get()?.let {
                    AccessibilityUtil.announce(it, uploadFinishedContent)
                }
            }
        }
    }

    override fun onFail(err: BaseResponse) {
        val initialMsg = mRefCtx.get()?.getString(R.string.message_upload_fail_notif_title)?: ""
        val contentMsg = when (err.code) {
            // Endpoint is no longer valid
            RESPSTATUS_IOEXC -> mRefCtx.get()?.getString(R.string.error_parse_login_resp)?: ""
            // Timeout reached. Either server takes too long to respond or the app does not have sufficient connection.
            RESPSTATUS_TIMEOUT -> mRefCtx.get()?.getString(R.string.message_upload_fail_timeout)?: ""
            // Failed to connect. Possibly due to connection being cut when uploading
            RESPSTATUS_CONNECTION_FAILED -> mRefCtx.get()?.getString(R.string.message_upload_fail_connect)?: ""
            // Error retrieved from the server. Can be message-related or functionality-related (e.g. parsing error, etc.)
            else -> if (err.body.isEmpty()) initialMsg else err.body
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

        // Tell UI
        broadcast(STATUS_FAILED, contentMsg)

        // Announce for screenreader
        mRefCtx.get()?.let {
            AccessibilityUtil.announce(it, "$initialMsg, $contentMsg")
        }
    }

    private fun broadcast(status: String, msg: String) {
        val ctx = mRefCtx.get()
        if (ctx == null) return

        Broadcaster.cast(ctx, BroadcastFilterList.MESSAGE_UPLOAD, mapOf(
                STATUS to status,
                MESSAGE to msg
        ))
    }
}