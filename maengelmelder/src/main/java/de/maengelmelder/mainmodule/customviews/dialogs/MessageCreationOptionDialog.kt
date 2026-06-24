package de.maengelmelder.mainmodule.customviews.dialogs

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.cardview.widget.CardView
import de.maengelmelder.mainmodule.R

class MessageCreationOptionDialog(c: Context) : Dialog(c) {

    companion object {
        val OPTION_STANDARD = "standard"
        val OPTION_QR = "qrcode"
    }

    private var mOnOptionPicked: ((String) -> Unit)? = null
    var onOptionPicked
        get() = mOnOptionPicked
        set(value) { mOnOptionPicked = value }

    init {
        val view = LayoutInflater.from(c).inflate(R.layout.mm_dialog_message_create_options, null)
        setContentView(view)

        view.findViewById<CardView>(R.id.btn_create_message_qr)?.apply {
            setOnClickListener { _ ->
                dismiss()
                mOnOptionPicked?.invoke(OPTION_QR)
            }
        }
        view.findViewById<CardView>(R.id.btn_create_message_standard)?.apply {
            setOnClickListener { _ ->
                dismiss()
                mOnOptionPicked?.invoke(OPTION_STANDARD)
            }
        }
    }

}