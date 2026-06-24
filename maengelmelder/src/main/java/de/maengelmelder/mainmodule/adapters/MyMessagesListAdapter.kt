package de.maengelmelder.mainmodule.adapters

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.service.tasks.ThumbnailLoadingCoroutine
import de.maengelmelder.mainmodule.service.util.Time
import de.maengelmelder.mainmodule.utils.MapMarkerFromURLCoroutine
import de.maengelmelder.mainmodule.utils.ResourceProxy
import de.maengelmelder.mainmodule.utils.interfaces.OnMessageMenuItemClicked
import de.maengelmelder.mainmodule.utils.interfaces.OnMessageSelected
import kotlin.collections.ArrayList

class MyMessagesListAdapter(c: Context, messageBuilders: ArrayList<MessageBuilder>) : BaseAdapter() {

    /**
     * @property mContext context
     * @property mDB instance of [MMDB]
     * @property mInflater for inflating child view
     * @property mBuilders list of messages
     * @property mPopup popup menu for actions
     * @property mSelectedListener listener when the message is selected
     */

    private val mContext = c
    private val mDB = MMDB.instance(mContext)
    private val mInflater = LayoutInflater.from(c)
    private var mBuilders = messageBuilders
    private var mPopup: PopupMenu? = null
    private var mSelectedListener: OnMessageSelected? = null
    private val mColorStepComplete = ContextCompat.getColor(mContext, R.color.mmcolor_step_completed_icontint)
    private val mColorStepIncomplete = ContextCompat.getColor(mContext, R.color.mmcolor_step_incomplete_icontint)

    private val mPref = PreferenceManager.getDefaultSharedPreferences(c)
    private val bCanDisplayImg = mPref.getBoolean(c.getString(R.string.mm_prefkey_show_image_on_list), true)

    fun setMessages(list: ArrayList<MessageBuilder>) {
        mBuilders = list
        notifyDataSetChanged()
    }

    override fun getView(idx: Int, v: View?, vg: ViewGroup?): View {
        // Inflate child view
        val view = v?: mInflater.inflate(R.layout.mm_listitem_mymessage, vg, false)

        // Get the message item
        val msg = getItem(idx) as MessageBuilder

        // Obtain the ViewHolder (getting references)
        if (view.tag == null) {
            view.tag = ViewHolder(view)
        }
        val vh = view.tag as ViewHolder
        val msgDesc = if (msg.title == "" || msg.title == "null") msg.description else msg.title

        vh.text1.text = "${msg.message.state} | $msgDesc"

        if (msg.message.markerUrl.isNotEmpty()) {
            // Try to use message marker from Url if possible
            MapMarkerFromURLCoroutine.run(msg.message.markerUrl) {
                if (it == null) {
                    // May happen to self-created messages that are not uploaded yet.
                    vh.icon.setImageDrawable(ResourceProxy.getMarker(mContext, msg.message.colorString, msg.category.markerId))
                } else {
                    vh.icon.setImageBitmap(it)
                }
            }
        } else {
            vh.icon.setImageDrawable(ResourceProxy.getMarker(mContext, msg.message.colorString, msg.category.markerId))
        }

        // Image if it has one
        if (msg.hasImage() && bCanDisplayImg) {
            vh.thumbnailLayout.visibility = View.GONE
            vh.image.alpha = 0F
            ThumbnailLoadingCoroutine(mContext, vh, msg.messageId, msg.getImagePath(0)).apply {
                listener = object : ThumbnailLoadingCoroutine.OnThumbnail {
                    override fun onThumbnailReady(ref: Any?, bitmap: Bitmap) {
                        ref?.let { r ->
                            if (r is ViewHolder) {
                                r.thumbnailLayout.visibility = View.VISIBLE
                                r.image.setImageBitmap(bitmap)
                                r.image.animate().alpha(1f).setDuration(150).start()
                            }
                        }
                    }
                }
            }.execute()

            if (msg.getNumOfImages() == 1) {
                vh.moreImageIndicator.visibility = View.INVISIBLE
            } else {
                vh.moreImageIndicator.visibility = View.VISIBLE
                vh.moreImageIndicator.text = mContext.getString(R.string.more_image_indicator,
                        (msg.getNumOfImages()-1).toString())
            }
        } else {
            vh.thumbnailLayout.visibility = View.INVISIBLE
        }

        // If the message has serverId, it's uploaded already
        val uploadStat = if (msg.message.serverId.isNotEmpty()) mDB.constants.STATUS_FINISHED else mDB.getUploadStatus(msg.messageId)
        var statusSpeak = ""
        // Get the upload status of the message
        when (uploadStat) {
            // Report has not been fully filled or ready for upload
            "", null -> {
                statusSpeak = mContext.getString(R.string.acc_cd_mymsglistitem_state_incomplete)
                vh.uploadStatusLayout.visibility = View.GONE
                vh.statusLayout.visibility = View.VISIBLE

                // get the attribute values to check whether the message is ready to be uploaded
                msg.attributeValuesFromJson(mDB.getExtrasJSON(msg.messageId))
                val status = msg.getStatus()
                status.asIterable().forEach { kv ->
                    val icon = vh.statusIcons[kv.first]
                    icon?.setColorFilter (if (kv.second) mColorStepComplete else mColorStepIncomplete)
                }
            }

            // Report is being uploaded to the server
            mDB.constants.STATUS_UPLOADING -> {
                vh.statusLayout.visibility = View.INVISIBLE
                vh.uploadStatusLayout.visibility = View.VISIBLE
                vh.uploadStatusText.text = mContext.getString(R.string.message_upload_notif_title)
                vh.uploadStatusText.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.upload_notif, 0,0,0)
                vh.uploadStatusText.setCompoundDrawablesWithIntrinsicBounds(0, 0,0,0)
            }

            // Extra images for the uploaded message are being uploaded
            // This is a possible next status after STATUS_UPLOADING
            mDB.constants.STATUS_UPLOADING_IMAGES -> {
                vh.statusLayout.visibility = View.INVISIBLE
                vh.uploadStatusLayout.visibility = View.VISIBLE
                vh.uploadStatusText.setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.ic_menu_report_image, 0,0,0)
                val sb = StringBuilder()
                        .append(mContext.getString(R.string.message_upload_images))
                        .append(msg.message.additional[Message.DATA_UPDATELOG])
                vh.uploadStatusText.text = sb.toString()
            }

            // Extra images failed to upload
            // This is a possible next status after STATUS_UPLOADING_IMAGES
            mDB.constants.STATUS_IMAGE_UPLOAD_FAIL -> {
                vh.statusLayout.visibility = View.INVISIBLE
                vh.uploadStatusLayout.visibility = View.VISIBLE
                vh.uploadStatusText.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_drwtext_warn, 0,0,0
                )
                vh.uploadStatusText.text = mContext.getText(R.string.notif_image_failed_to_upload)
            }

            // Message upload is failed
            // This is a possible next status after STATUS_UPLOADING
            mDB.constants.STATUS_UPLOAD_FAILED -> {
                vh.statusLayout.visibility = View.INVISIBLE
                vh.uploadStatusLayout.visibility = View.VISIBLE
                vh.uploadStatusText.setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.ic_delete, 0,0,0
                )
                vh.uploadStatusText.text = msg.message.additional[Message.DATA_UPDATELOG]
            }

            // Message and their extra images (if any) have been successfully uploaded
            // This is a possible next status after STATUS_UPLOADING or STATUS_UPLOADING_IMAGES
            mDB.constants.STATUS_FINISHED -> {
                val initialTS = if (msg.message.createdAt > 0) msg.message.createdAt else msg.message.uploadedAt
                val text = mContext.getString(R.string.message_already_uploaded,
                        Time.getRelativeSpanFromToday(mContext, initialTS)
                )
                statusSpeak = text
                vh.uploadStatusLayout.visibility = View.VISIBLE
                vh.statusLayout.visibility = View.GONE
                vh.uploadStatusText.text = text
                vh.uploadStatusText.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_done, 0,0,0
                )
            }
        }

        view.contentDescription = mContext.getString(R.string.acc_cd_mymsglistitem, statusSpeak)

        return view
    }

    /**
     * Remove a message from the list by its message id
     */
    fun remove(msgid: String) {
        val filtered = mBuilders.filter { mb -> mb.messageId != msgid }
        mBuilders = ArrayList(filtered)
        notifyDataSetChanged()
    }

    /**
     * Listener for the context menu on each message (upload, edit, delete)
     */
    fun setContextMenuListener(l: OnMessageSelected) {
        mSelectedListener = l
    }

    override fun getItem(p0: Int): Any = mBuilders[p0]

    override fun getItemId(p0: Int): Long = p0.toLong()

    override fun getCount(): Int = mBuilders.size

    /**
     * Get a popup depending on the upload status of the message
     * E.g. message that is not uploaded successfully will get an option to re-upload
     */
    fun getPopup(msg: Message, v: View, listener: OnMessageMenuItemClicked): PopupMenu {
        mPopup = PopupMenu(mContext, v)

        when (msg.uploadStatus) {
            "", mDB.constants.STATUS_UPLOAD_FAILED, mDB.constants.STATUS_UPLOADING, mDB.constants.STATUS_UPLOADING_IMAGES -> {
                mPopup?.menuInflater?.inflate(R.menu.menu_message, mPopup?.menu)
            }

            mDB.constants.STATUS_IMAGE_UPLOAD_FAIL -> {
                mPopup?.menuInflater?.inflate(R.menu.menu_failed_imageupload, mPopup?.menu)
            }

            mDB.constants.STATUS_FINISHED -> {
                mPopup?.menuInflater?.inflate(R.menu.menu_message_uploaded, mPopup?.menu)
            }

        }

        mPopup?.setOnMenuItemClickListener(listener)
        return mPopup!!
    }

    inner class ViewHolder(v: View) {
        val text1 = v.findViewById<TextView>(R.id.text1)
        val icon = v.findViewById<ImageView>(R.id.icon)
        val image = v.findViewById<ImageView>(R.id.image)
        val thumbnailLayout = v.findViewById<RelativeLayout>(R.id.thumbnail)
        val moreImageIndicator = v.findViewById<TextView>(R.id.more_indicator)
        val statusLayout = v.findViewById<LinearLayout>(R.id.statuses)
        val uploadStatusLayout = v.findViewById<LinearLayout>(R.id.upload_status)
        val uploadStatusText = v.findViewById<TextView>(R.id.uploadText)
        val statusIcons = hashMapOf(
                MessageBuilder.STEP.PHOTO to v.findViewById<ImageView>(R.id.status_photo),
                MessageBuilder.STEP.LOCATION to v.findViewById<ImageView>(R.id.status_location),
                MessageBuilder.STEP.CATEGORY to v.findViewById<ImageView>(R.id.status_category),
                MessageBuilder.STEP.ATTRIBUTE to v.findViewById<ImageView>(R.id.status_attributes)
        )
    }
}