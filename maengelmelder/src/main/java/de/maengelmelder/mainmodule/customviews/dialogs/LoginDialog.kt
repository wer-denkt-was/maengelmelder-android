package de.maengelmelder.mainmodule.customviews.dialogs

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.utils.UserData

class LoginDialog(c: Context, domain: Domain, systemInfo: SystemInfo? = null) : Dialog(c) {

    val mUserCred = UserData.getUserCred(c)

    init {
        val view = LayoutInflater.from(c).inflate(R.layout.mm_activity_login, null)
        setContentView(view)

        // Hide toolbar and domain dropdown
        view.findViewById<Toolbar>(R.id.toolbar)?.apply {
            visibility = View.GONE
        }
        view.findViewById<Toolbar>(R.id.domain_choices)?.apply {
            visibility = View.GONE
        }

        // Show forced domain login + warning if any
        view.findViewById<TextView>(R.id.forced_domain_login)?.apply {
            visibility = View.VISIBLE
            text = c.getString(R.string.override_domain_login, domain.name)
        }
        view.findViewById<TextView>(R.id.warn_log_out_from_previous_account)?.apply {
            if (mUserCred != null && mUserCred.isUserValid()) {
                visibility = View.VISIBLE
                text = c.getString(R.string.warn_previous_account_auto_logout, mUserCred.domain?.name, domain.name)
            } else {
                visibility = View.GONE
            }
        }

        val loadingIcon = view.findViewById<ProgressBar>(R.id.loading)
        val btnLogin = view.findViewById<Button>(R.id.btn_login)
        val btnRegister = view.findViewById<Button>(R.id.btn_register)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)

        // login and cancel
        btnRegister?.visibility = View.GONE
        btnCancel?.setOnClickListener {
            dismiss()
        }
        btnLogin?.setOnClickListener {

        }

        // TODO LoginDialog. Maybe useful later????
    }
}