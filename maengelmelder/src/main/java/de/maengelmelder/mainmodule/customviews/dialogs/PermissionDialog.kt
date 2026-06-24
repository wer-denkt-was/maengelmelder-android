package de.maengelmelder.mainmodule.customviews.dialogs

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import androidx.activity.result.ActivityResultLauncher
import androidx.preference.PreferenceManager
import de.maengelmelder.mainmodule.R

class PermissionDialog (c: Context, a: Activity,
                        permissionsList: List<String>,
                        arl: ActivityResultLauncher<Array<String>>) : Dialog(c) {

    companion object {
        val PREFKEY_DONT_SHOW = "wdw.mmv2.permdialog.canshow"
        fun canShow(c: Context): Boolean {
            val pref = PreferenceManager.getDefaultSharedPreferences(c)
            return pref.getBoolean(PREFKEY_DONT_SHOW, true)
        }
    }

    private var mView: View

    init {
        setCancelable(true)
        setTitle(R.string.dialog_permission_title)
        mView = LayoutInflater.from(context).inflate(R.layout.mm_dialog_permissions, null)
        setContentView(mView)

        val chkDontShow = mView.findViewById<CheckBox>(R.id.chk_dont_show)
        chkDontShow.setOnCheckedChangeListener { _, b ->
            val pref = PreferenceManager.getDefaultSharedPreferences(c)
            pref.edit().putBoolean(PREFKEY_DONT_SHOW, !b).apply()
        }

        val permStorageLayout = mView.findViewById<View>(R.id.perm_storage)
        val needsStoragePerm = permissionsList.contains(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                permissionsList.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permStorageLayout.visibility = if (needsStoragePerm) View.VISIBLE else View.GONE

        val permLocationLayout = mView.findViewById<View>(R.id.perm_location)
        val needsLocPerm = permissionsList.contains(Manifest.permission.ACCESS_FINE_LOCATION) ||
                permissionsList.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
        permLocationLayout.visibility = if (needsLocPerm) View.VISIBLE else View.GONE

        val permCamLayout = mView.findViewById<View>(R.id.perm_camera)
        val needsCamPerm = permissionsList.contains(Manifest.permission.CAMERA)
        permCamLayout.visibility = if (needsCamPerm) View.VISIBLE else View.GONE

        val permNotifLayout = mView.findViewById<View>(R.id.perm_notif)
        val needsPermNotif = permissionsList.contains(Manifest.permission.POST_NOTIFICATIONS)
        permNotifLayout.visibility = if (needsPermNotif) View.VISIBLE else View.GONE

        val btnAskPerm = mView.findViewById<Button>(R.id.btn_ask_permission)
        btnAskPerm.setOnClickListener { v ->
            arl.launch(permissionsList.toTypedArray())
            cancel()
        }
        val btnCancel = mView.findViewById<Button>(R.id.btn_cancel)
        btnCancel.setOnClickListener { v -> cancel() }

    }

}