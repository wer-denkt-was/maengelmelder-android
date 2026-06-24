package de.maengelmelder.mainmodule.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.utils.ResourceProxy
import de.maengelmelder.mainmodule.utils.images.ImageManipulator
import java.text.SimpleDateFormat
import java.util.*

class DuplicateListAdapter(c: Context, msgs: Array<Message>) : BaseAdapter() {

    companion object {
        private val DateDisplay = SimpleDateFormat("dd. MMM yyyy", Locale.getDefault())
    }

    private val mContext = c
    private val mMessages = msgs
    private val mInflater = LayoutInflater.from(c)

    @SuppressLint("SetTextI18n")
    override fun getView(idx: Int, v: View?, vg: ViewGroup?): View {
        val view = v?: mInflater.inflate(R.layout.mm_listitem_duplicate_messages, vg, false)
        if (view.tag == null) {
            view.tag = ViewHolder(view)
        }

        val vh = view.tag as ViewHolder
        val msg = getItem(idx) as Message
        vh.icon.setImageDrawable(ResourceProxy.getMarker(mContext, msg.colorString, msg.category.markerId))
        vh.text.text = msg.desc
        vh.status.text = msg.state
        vh.subStatus.text = DateDisplay.format(Date(msg.createdAt))

        view.contentDescription = "${msg.state} von ${vh.subStatus.text}. ${msg.desc}"

        // thumbnail
        if (msg.imagePaths.size > 0) {
            ImageManipulator.setImage(mContext, vh.image, msg.imagePaths[0], 0, 0)
        } else {
            vh.image.setImageResource(0)
        }

        return view
    }

    override fun getItem(p0: Int): Any = mMessages[p0]

    override fun getItemId(p0: Int): Long = p0.toLong()

    override fun getCount(): Int = mMessages.size

    inner class ViewHolder(v: View) {
        val subStatus = v.findViewById<TextView>(R.id.txt_sub)
        val status = v.findViewById<TextView>(R.id.txt_status)
        val text = v.findViewById<TextView>(R.id.duplicate_text)
        val icon = v.findViewById<ImageView>(R.id.duplicate_message_icon)
        val image = v.findViewById<ImageView>(R.id.duplicate_image_icon)
    }
}