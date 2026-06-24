package de.maengelmelder.mainmodule.adapters

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.objects.Log
import de.maengelmelder.mainmodule.service.util.Time

class LogsAdapter(c: Context, logs: Array<Log>) : BaseAdapter() {

    private val mContext = c
    private val mLayoutInf = LayoutInflater.from(c)
    private val mLogs = logs

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView?: mLayoutInf.inflate(R.layout.mm_listitem_logs, parent, false)

        val vh = {
            if (view.tag == null) { view.tag = ViewHolder(view) }
            view.tag as ViewHolder
        }.invoke()

        val item = getItem(position) as Log
        vh.time.text = Time.getRelativeSpanFromToday(mContext, item.timestamp)

        val text =
            when (item.data[Log.KEY_TYPE]) {
                Log.TYPE_LOGIN -> {
                    fromHtml(mContext.getString(R.string.loginfo_login, "<b>${item.data[Log.KEY_USERNAME]}</b>"))
                }

                Log.TYPE_LOGOUT -> {
                    fromHtml(mContext.getString(R.string.loginfo_logout, "<b>${item.data[Log.KEY_USERNAME]}</b>"))
                }

                Log.TYPE_MSG_CREATED -> {
                    fromHtml(mContext.getString(R.string.loginfo_new_message, "<b>${item.data[Log.KEY_MSG_ID]}</b>"))
                }

                Log.TYPE_MSG_REMOVED -> {
                    fromHtml(mContext.getString(R.string.loginfo_remove_message,
                            "<b>${item.data[Log.KEY_TITLE]}</b>",
                            "<b>${item.data[Log.KEY_MSG_ID]}</b>"))
                }

                Log.TYPE_MSG_EDITED -> {
                    fromHtml(mContext.getString(R.string.loginfo_edit_message,
                            "<b>${item.data[Log.KEY_TITLE]}</b>",
                            "<b>${item.data[Log.KEY_MSG_ID]}</b>"))
                }

                Log.TYPE_MSG_UPDATED -> {
                    fromHtml(mContext.getString(R.string.loginfo_update_message,
                            "<b>${item.data[Log.KEY_TITLE]}</b>",
                            "<b>${item.data[Log.KEY_REF_ID]}</b>"))
                }

                Log.TYPE_MSG_UPDATE_FAILED -> {
                    fromHtml(mContext.getString(R.string.loginfo_update_message_failed,
                            "<b>${item.data[Log.KEY_TITLE]}</b>",
                            "<b>${item.data[Log.KEY_REF_ID]}</b>",
                            "<i>${item.data[Log.KEY_REASON]}</i>"))
                }

                Log.TYPE_MSG_UPLOAD -> {
                    fromHtml(mContext.getString(R.string.loginfo_message_upload,
                            "<b>${item.data[Log.KEY_TITLE]}</b>",
                            "<b>${item.data[Log.KEY_MSG_ID]}</b>"))
                }

                Log.TYPE_MSG_UPLOAD_SUCCESS -> {
                    fromHtml(mContext.getString(R.string.loginfo_image_upload_success,
                            "<b>${item.data[Log.KEY_TITLE]}</b>",
                            "<b>${item.data[Log.KEY_MSG_ID]}</b>",
                            "<b>${item.data[Log.KEY_REF_ID]}</b>"))
                }

                Log.TYPE_MSG_UPLOAD_FAIL -> {
                    fromHtml(mContext.getString(R.string.loginfo_message_upload_failed,
                            "<b>${item.data[Log.KEY_TITLE]}</b>",
                            "<b>${item.data[Log.KEY_MSG_ID]}</b>",
                            "<i>${item.data[Log.KEY_REASON]}</i>"))
                }

                Log.TYPE_IMG_UPLOAD -> {
                    fromHtml(mContext.getString(R.string.loginfo_image_upload,
                            "<b>${item.data[Log.KEY_IMG_PATH]}</b>",
                            "<b>${item.data[Log.KEY_TITLE]}</b>",
                            "<b>${item.data[Log.KEY_REF_ID]}</b>"))
                }

                Log.TYPE_IMG_UPLOAD_SUCCESS -> {
                    fromHtml(mContext.getString(R.string.loginfo_image_upload_success,
                            "<b>${item.data[Log.KEY_IMG_PATH]}</b>",
                            "<b>${item.data[Log.KEY_TITLE]}</b>",
                            "<b>${item.data[Log.KEY_REF_ID]}</b>"))
                }

                Log.TYPE_IMG_UPLOAD_FAIL -> {
                    fromHtml(mContext.getString(R.string.loginfo_image_upload_fail,
                            "<b>${item.data[Log.KEY_IMG_PATH]}</b>",
                            "<b>${item.data[Log.KEY_TITLE]}</b>",
                            "<b>${item.data[Log.KEY_REF_ID]}</b>",
                            "<i>${item.data[Log.KEY_REASON]}</i>"))
                }

                Log.TYPE_MSG_VIEWED -> {
                    fromHtml(mContext.getString(R.string.loginfo_msg_viewed,
                            "<b>${item.data[Log.KEY_TITLE]}</b>",
                            "<b>${item.data[Log.KEY_REF_ID]}</b>"))
                }


                else -> ""
            }
        vh.log.text = text

        return view
    }

    @SuppressWarnings("deprecation")
    private fun fromHtml(html: String): Spanned =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            Html.fromHtml(html)
        }

    override fun getItem(position: Int): Any = mLogs[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = mLogs.size

    inner class ViewHolder(v: View) {
        val time = v.findViewById<TextView>(R.id.time)
        val log = v.findViewById<TextView>(R.id.log)
    }
}