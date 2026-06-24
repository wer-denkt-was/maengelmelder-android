package de.maengelmelder.mainmodule.customviews.dialogs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.utils.ActivityUtil

/**
 * Notifies user that GPS is not turned on. Provides a way to go to the GPS settings page
 *
 * [GPS_SETTINGS_REQ_CODE] can be used by the calling activity to get feedback on the GPS status
 */
class GPSAlertDialog(c: Context, a: Activity, permRequester: ActivityResultLauncher<String>, onDismiss: (() -> Unit)? = null) : AlertDialog(c) {

    companion object {
        const val GPS_SETTINGS_REQ_CODE = 69
        const val PREF_KEY_DONT_SHOW =  "wdw.mmv2.gpsalert.dontshow"
    }

    init {
        val v = LayoutInflater.from(c).inflate(R.layout.mm_gps_alert_dialog, null, false)
        setView(v)

        v.findViewById<Button>(R.id.btn_gps_activate)?.let { btn ->
            btn.setOnClickListener {
                dismiss()
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).also { i ->
                    a.startActivityForResult(i, GPS_SETTINGS_REQ_CODE)
                }
            }
        }
        v.findViewById<Button>(R.id.btn_cancel)?.let { btn ->
            btn.setOnClickListener {
                dismiss()
                onDismiss?.invoke()
            }
        }

        v.findViewById<CheckBox>(R.id.chk_dont_show)?.let { chk ->
            chk.setOnCheckedChangeListener { _, value ->
                val pref = PreferenceManager.getDefaultSharedPreferences(c)
                pref.edit().putBoolean(PREF_KEY_DONT_SHOW, value).apply()
            }
        }

        v.findViewById<Button>(R.id.btn_loc_perm)?.let { btn ->
            if (ActivityUtil.isPermissionGranted(c, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                btn.visibility = View.GONE
            } else {
                btn.visibility = View.VISIBLE
            }

            btn.setOnClickListener {
                dismiss()
                permRequester.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

}