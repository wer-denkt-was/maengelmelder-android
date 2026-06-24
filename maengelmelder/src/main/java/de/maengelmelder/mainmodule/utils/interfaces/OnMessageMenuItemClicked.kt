package de.maengelmelder.mainmodule.utils.interfaces

import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.objects.Message

/**
 *
 * [PopupMenu] listener for message list
 */
class OnMessageMenuItemClicked(v: View?, m: Message, listener: OnMessageSelected?) : PopupMenu.OnMenuItemClickListener {
    private val message = m
    private val view = v
    private val mListener = listener

    override fun onMenuItemClick(mi: MenuItem?): Boolean {
        when (mi?.itemId) {
            R.id.menu_edit -> mListener?.onStartEdit(message, view)
            R.id.menu_remove -> mListener?.onRemove(message, view)
            R.id.menu_upload -> mListener?.onUpload(message, view)
            R.id.menu_upload_images -> mListener?.onUploadImages(message, view)
            R.id.menu_detail -> mListener?.onDetail(message, view)
        }
        return true
    }
}