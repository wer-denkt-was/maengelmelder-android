package de.maengelmelder.mainmodule.customviews.dialogs

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import de.maengelmelder.mainmodule.R


/**
 * Notifies user to rate app if they want to
 */
class AppRatingDialog(c: Context) : AlertDialog(c) {

    companion object {
        val PREF_HAS_SHOWN_APPRATING = "wdw.mm.has_shown_app_rating_window"
    }

    init {
        val v = LayoutInflater.from(c).inflate(R.layout.mm_dialog_apprating, null, false)
        setView(v)

        v.findViewById<Button>(R.id.btn_go_to_app_store)?.let { btn ->
            btn.setOnClickListener {
                dismiss()
                // GO to app store
                val uri = Uri.parse("market://details?id=" + c.packageName)
                val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
                try {
                    startActivity(c, myAppLinkToMarket, null)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
        v.findViewById<Button>(R.id.btn_cancel)?.let { btn ->
            btn.setOnClickListener {
                dismiss()
            }
        }

        v.findViewById<CheckBox>(R.id.chk_dont_show)?.let { chk ->
            chk.setOnCheckedChangeListener { _, value ->
                val pref = PreferenceManager.getDefaultSharedPreferences(c)
                pref.edit().putBoolean(PREF_HAS_SHOWN_APPRATING, value).apply()
            }
        }
    }

}