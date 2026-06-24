package de.maengelmelder.mainmodule.customviews.dialogs

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import de.maengelmelder.mainmodule.R

class LoadingDialog(c: Context, text: String = "", onCancel: ((Dialog)->Unit)? = null) : Dialog(c) {

    init {
        setCanceledOnTouchOutside(false)
        val view = LayoutInflater.from(c).inflate(R.layout.mm_dialog_loading, null)
        setContentView(view)

        val textV = view.findViewById<TextView>(R.id.text)
        if (text.isNotEmpty()) textV.text = text

        val cancelBtn = view.findViewById<TextView>(R.id.btn_cancel)
        if (onCancel != null) {
            cancelBtn.visibility = View.VISIBLE
            cancelBtn.setOnClickListener { _ -> onCancel.invoke(this) }
        } else {
            cancelBtn.visibility = View.GONE
        }
    }
}