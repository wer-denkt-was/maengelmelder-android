package de.maengelmelder.mainmodule.utils.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import de.werdenktwas.modules.android.notificationutil.NotifWizard

/**
 * Handy utility used to build [NotifWizard] and determine suitable notification Id
 */
object NotifUtility {

    /**
     * @property STARTING_NOTIF_ID starting notification Id.
     * @property PREF_KEY_NOTIF_ID preference key for storing incremental notification Id
     */

    val STARTING_NOTIF_ID = 1

    private val PREF_KEY_NOTIF_ID = "de.wdw.notifutil.incrementid"

    /**
     * Returns an auto incremented notification Id. It will always start from [STARTING_NOTIF_ID] and incremented by 1
     * every time this method is called
     *
     * @param c Context
     *
     * @return incremented notification Id
     */
    fun getIncrementalNotifId(c: Context): Int {
        val pref = PreferenceManager.getDefaultSharedPreferences(c)
        val value = pref.getInt(PREF_KEY_NOTIF_ID, STARTING_NOTIF_ID)
        pref.edit().putInt(PREF_KEY_NOTIF_ID, value.inc()).apply()
        return value
    }

    /**
     * Returns an instance of [NotifWizard]. This method also creates the notification channel based on supplied
     * parameters, if needed. Only usable with Android O or above
     *
     *  @param c Context object
     *  @param channelId channel Id as string.
     *  @param channelName channel name
     *  @param desc channel's description
     *  @param importance importance level
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getWizard(c: Context,
                  channelId: String,
                  channelName: String,
                  desc: String = "",
                  importance: Int = NotificationManager.IMPORTANCE_DEFAULT): NotifWizard {

        val mgr = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = desc
        }
        mgr.createNotificationChannel(channel)
        return NotifWizard(c, channelId)
    }

    /**
     *  The legacy method to create a [NotifWizard] (Below Android O)
     *
     *  @param c Context object
     *  @param channelId channel Id as string. Will most likely be ignored by legacy version
     */
    fun getWizardOld(c: Context, channelId: String) : NotifWizard {
        return NotifWizard(c, channelId)
    }

}