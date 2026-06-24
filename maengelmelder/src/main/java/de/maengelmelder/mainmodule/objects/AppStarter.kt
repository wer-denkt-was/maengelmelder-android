package de.maengelmelder.mainmodule.objects

import android.content.Context
import android.content.Intent
import android.widget.Toast
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.activities.OverviewActivity
import de.maengelmelder.mainmodule.utils.UserData

object AppStarter {

    fun startOverview(c: Context,
                        onStartLoggingIn: ((String) -> Unit)? = null,
                        onFinishedLogin: ((UserCred?) -> Unit)? = null) {
        // check autologin first
        val username = if (BuildConfig.debug) MMConstants.AutoLoginCred_Test.first else MMConstants.AutoLoginCred.first
        val pass = if (BuildConfig.debug) MMConstants.AutoLoginCred_Test.second else MMConstants.AutoLoginCred.second

        if (username.isNotEmpty() && pass.isNotEmpty()) {
            onStartLoggingIn?.invoke(username)
            handleAutoLogin (c, username, pass) {
                if (it != null && it.isUserValid()) {
                    Toast.makeText(c, c.getString(R.string.info_autologin_success, it.publicName), Toast.LENGTH_LONG).show()
                }
                onFinishedLogin?.invoke(it)
                doStartOverviewActivity(c)
            }
        } else {
            doStartOverviewActivity(c)
        }
    }

    private fun doStartOverviewActivity(c: Context) {
        val i = Intent(c, OverviewActivity::class.java)
        c.startActivity(i)
    }

    private fun handleAutoLogin(ctx: Context, username: String, pass: String, resp: (UserCred?) -> Unit) {
        val token = UserData.getUserCred(ctx)?.token?: ""

        // Not empty, do autologin
        if (!username.isEmpty() && !pass.isEmpty()) {
            if (!token.isEmpty()) {
                UserData.logout(ctx) {
                    UserData.login(ctx, username, pass, resp)
                }
            } else {
                UserData.login(ctx, username, pass, resp)
            }
        } else {
            resp.invoke(null)
        }
    }

}