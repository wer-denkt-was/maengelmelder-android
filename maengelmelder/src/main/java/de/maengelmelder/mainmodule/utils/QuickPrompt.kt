package de.maengelmelder.mainmodule.utils

import android.content.Context
import android.os.Build
import com.google.android.material.snackbar.Snackbar
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import de.maengelmelder.mainmodule.R

object QuickPrompt {

    /**
     * Uses either [Toast] or [Snackbar] (SDK >= 24) to show short message to the user. The [Snackbar] will have
     * an OK button to close it. Pass the application context
     *
     * @see Toast.makeText
     * @see Snackbar.make
     */
    fun inform(ctx: Context, parentLayout: View, msg: String,
               marginBottom: Float = 0f) {
        if (Build.VERSION.SDK_INT >= 24) {
            val sb = Snackbar.make(parentLayout, msg, Snackbar.LENGTH_INDEFINITE).apply {
                // Make the snackbar bigger
                val snView = view as ViewGroup
                var textElem: TextView? = null
                (0..snView.childCount).forEach { idx ->
                    val v = snView.getChildAt(idx)
                    if (v is TextView) {
                        textElem = v
                    }
                }
                textElem?.maxLines = 5
            }
            sb.setAction(R.string.ok) { sb.dismiss() }
            if (marginBottom > 0) {
                val lp = sb.view.layoutParams as ViewGroup.MarginLayoutParams
                lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin,
                        lp.bottomMargin + marginBottom.toInt())
                sb.view.layoutParams = lp
            }
            sb.show()
        } else {
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
        }
    }

}