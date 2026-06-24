package de.maengelmelder.mainmodule.utils.interfaces

import android.view.View
import de.maengelmelder.mainmodule.objects.Message

interface OnMessageSelected {
    fun onStartEdit(m: Message, v: View?)
    fun onRemove(m: Message, v: View?)
    fun onUpload(m: Message, v: View?)
    fun onUploadImages(m: Message, v: View?)
    fun onDetail(m: Message, v: View?)
}