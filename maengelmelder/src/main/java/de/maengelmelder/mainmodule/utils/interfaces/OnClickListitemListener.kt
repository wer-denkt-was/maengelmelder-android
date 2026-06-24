package de.maengelmelder.mainmodule.utils.interfaces

import android.view.View

interface OnClickListitemListener {
    fun onItem(v: View, pos: Int, id: Long)
}