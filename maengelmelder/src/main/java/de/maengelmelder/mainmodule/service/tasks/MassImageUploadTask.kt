package de.maengelmelder.mainmodule.service.tasks

import android.app.NotificationManager
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import androidx.preference.PreferenceManager
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1UpdateMessage
import de.maengelmelder.mainmodule.network.responses.MessageUpdateResponse
import de.maengelmelder.mainmodule.objects.Log
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.service.receivers.BroadcastFilterList
import de.maengelmelder.mainmodule.service.util.Broadcaster
import de.maengelmelder.mainmodule.utils.notification.NotifUtility
import java.io.File
import java.lang.ref.WeakReference

/**
 * This task is used to execute more than 1 [MMv1UpdateMessage] task to upload images in FIFO order. Useful for mass image upload on messages.
 * Note that [MessageCreateTask] implements a more primitive, anti-threading implementation of this, although with the same algorithm and base API.
 * Since this task runs in background, the chained API calls on [MessageCreateTask] cannot be replaced with this implementation.
 */
@Deprecated(message = "Not used anymore. Replaced with bundle call. See [network.coroutines.v1.MMv1UploadFileToBundle] and [network.coroutines.v1.MMv1CreateBundle]")
internal class MassImageUploadTask(c: Context, notifId: Int, mb: MessageBuilder) :
        AsyncTask<Void, Int, Array<String>>() {

    /**
     * Reference to context
     */
    private val mRefCtx = WeakReference<Context>(c)

    private val mMessage = mb
    private val mTotalImages = mb.getNumOfImages()
    private val mServerMsgId = mb.message.serverId
    private val mLocalMsgId = mb.messageId

    private val mNotifId = notifId
    private val mNotifWiz =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                NotifUtility.getWizard(c,
                        c.getString(R.string.notif_channel_id),
                        c.getString(R.string.app_name),
                        "",
                        NotificationManager.IMPORTANCE_LOW)
            else NotifUtility.getWizardOld(c, c.getString(R.string.notif_channel_id))

    private val builder = mNotifWiz.getBuilder()

    private val mImageUploaded = arrayListOf<String>()

    private val mDB = MMDB.instance(c)

    private val mTextUploadingImages = c.getString(R.string.message_upload_images)
    private val mTextUploadImagesFailed = c.getString(R.string.message_upload_images_fail)
    private val mTextUploadImagesSuccess = c.getString(R.string.message_imageupload_success)

    // Configs
    private val mPref = PreferenceManager.getDefaultSharedPreferences(c)
    private val bDeleteImageAfterSubmit = mPref.getBoolean(c.getString(R.string.mm_prefkey_del_images_after_upload), false)
    private val bDeleteMsgAfterSubmit = mPref.getBoolean(c.getString(R.string.mm_prefkey_del_msg_after_upload), false)
    private val bShouldSaveLog = mPref.getBoolean(c.getString(R.string.mm_prefkey_should_log), true)

    override fun onPreExecute() {
        // Prepare notification for user
        builder.setSmallIcon(R.drawable.white)
        builder.setContentTitle(mTextUploadingImages)
        builder.setContentText("(${mImageUploaded.size} / $mTotalImages)")
        mNotifWiz.notify(mNotifId, builder.build())

        // Update entry to database
        mDB.updateMessage(mLocalMsgId, Pair(mDB.constants.COL_UPLOAD_STATUS, mDB.constants.STATUS_UPLOADING_IMAGES))

        // Update log
        mMessage.addAdditionalData(Message.DATA_UPDATELOG, "(0 / ${mMessage.getNumOfImages()})")
        mDB.updateMessage(mMessage.messageId,
                Pair(mDB.constants.COL_ADDITIONAL, mMessage.message.additionalDataToJSON().toString()))

        // Tell UI
        broadcast(mDB.constants.STATUS_UPLOADING_IMAGES, mTextUploadingImages)
    }

    override fun doInBackground(vararg params: Void?): Array<String> {
        // Upload each image
        val sys = mDB.getSystem(mMessage.category.systemId)

        mMessage.iterateImagePaths { path ->
            val ctx = mRefCtx.get()
            if (ctx != null) {
                // create the API with the correct system info
                val api = MMv1UpdateMessage(ctx,
                        mMessage.category.domainId.toInt(),
                        mServerMsgId,
                        "",
                        path,
                        false).apply { externalSystemInfo = sys }

                // Execute in the same thread as this one, to ensure FIFO order
                val resp = api.doExecuteAPI(null)

                // Parse the response
                val result =
                        if (resp != null) api.parseResponse(resp)
                        else MessageUpdateResponse(-1, mMessage.messageId, null, "")

                // If successful upload, update uploaded images list
                if (result.code == 200) {
                    mImageUploaded.add(path)

                    // Update log
                    mMessage.addAdditionalData(Message.DATA_UPDATELOG, "(${mImageUploaded.size} / ${mMessage.getNumOfImages()})")
                    mDB.updateMessage(mMessage.messageId,
                            Pair(mDB.constants.COL_ADDITIONAL, mMessage.message.additionalDataToJSON().toString()))

                    // Notify user
                    builder.setContentText("(${mImageUploaded.size} / $mTotalImages)")
                    mNotifWiz.notify(mNotifId, builder.build())
                }
            }
        }

        // Return the list of successfully uploaded image (might not be the same content as the list of previous images)
        return mImageUploaded.toTypedArray()
    }

    override fun onPostExecute(result: Array<String>?) {
        if (result == null) {
            sendFailNotice()
            return
        }

        // Delete the uploaded image from local disk if config allows
        if (bDeleteImageAfterSubmit) {
            mImageUploaded.forEach { img ->
                try {
                    val f = File(img)
                    if (f.exists()) f.delete()
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        // Some images fail to upload
        if (mImageUploaded.size < mMessage.getNumOfImages()) {
            mMessage.removeImagePaths(mImageUploaded.toTypedArray())

            // Filter the successfully uploaded images and save them to database
            mDB.updateMessage(mLocalMsgId,
                    mDB.constants.COL_PHOTO_PATH to mMessage.getPhotoPathsAsString())
            mDB.updateMessage(mLocalMsgId,
                    mDB.constants.COL_UPLOAD_STATUS to mDB.constants.STATUS_IMAGE_UPLOAD_FAIL)
            sendFailNotice()
        } else {
            // Delete the message if the config allows
            if (bDeleteMsgAfterSubmit) {
                mDB.deleteMessage(mMessage.messageId)

                // LOG: logged when the message is removed
                if (bShouldSaveLog) {
                    mDB.addLog(Log.TYPE_MSG_REMOVED, hashMapOf(
                            Log.KEY_MSG_ID to mMessage.messageId,
                            Log.KEY_TITLE to (mMessage.title)
                    ))
                }
            } else {
                // Update status to finished
                mDB.updateMessage(mLocalMsgId,
                        mDB.constants.COL_UPLOAD_STATUS to mDB.constants.STATUS_FINISHED)
                // Remove upload logs
                mMessage.removeAdditionalData(Message.DATA_UPDATELOG)
                mDB.updateMessage(mMessage.messageId,
                        Pair(mDB.constants.COL_ADDITIONAL, mMessage.message.additionalDataToJSON().toString()))
            }

            // Notify user
            sendSuccessNotice()
        }
    }

    private fun sendFailNotice() {
        builder.setSmallIcon(R.drawable.red).setContentTitle(mTextUploadImagesFailed)
        mNotifWiz.notify(mNotifId, builder.build())
        broadcast(mDB.constants.STATUS_IMAGE_UPLOAD_FAIL, mTextUploadImagesFailed)
    }

    private fun sendSuccessNotice() {
        builder.setSmallIcon(R.drawable.green).setContentTitle(mTextUploadImagesSuccess).setContentText("")
        mNotifWiz.notify(mNotifId, builder.build())
        broadcast(mDB.constants.STATUS_FINISHED, mTextUploadImagesSuccess)
    }

    private fun broadcast(status: String, msg: String) {
        val ctx = mRefCtx.get()
        if (ctx == null) return

        Broadcaster.cast(ctx, BroadcastFilterList.MESSAGE_UPLOAD, mapOf(
                MessageCreateTask.STATUS to status,
                MessageCreateTask.MESSAGE to msg
        ))
    }
}