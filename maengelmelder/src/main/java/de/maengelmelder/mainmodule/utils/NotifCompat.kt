package de.maengelmelder.mainmodule.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import de.maengelmelder.mainmodule.R

/**
 *
 * This object is used to reliably build notifications across different SDK versions
 *
 */
@Deprecated("Use library ':notificationutil' instead")
internal object NotifCompat {

    /**
     * @property mNotifMgr The notification manager
     * @property mNotifId the notification Id, which is incremented by 1 for every notification being made
     */
    private var mNotifMgr: NotificationManager? = null

    private var mNotifId: Int = 0

    /**
     * Returns the notification id, then increment it by 1
     *
     * @return notification id in Integer
     */
    fun getIncNotifId(): Int = ++mNotifId

    /**
     * Returns the notification manager that is compatible with any SDK versions
     *
     * @param c Context
     * @return [NotificationManager]
     */
    fun getNotifMgr(c: Context): NotificationManager {
        if (mNotifMgr == null) {
            mNotifMgr = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                val channelId = c.getString(R.string.notif_channel_id)
                if (mNotifMgr?.getNotificationChannel(channelId) == null) {
                    val channel = NotificationChannel(
                            channelId,
                            c.getString(R.string.app_name),
                            NotificationManager.IMPORTANCE_DEFAULT)
                    channel.enableVibration(true)
                    channel.lightColor = Color.GREEN
                    mNotifMgr?.createNotificationChannel(channel)
                }
            }
        }
        return mNotifMgr!!
    }

    /**
     * Returns the notification builder that is compatible across different SDK versions
     *
     * @param c Context
     * @return [NotificationCompat.Builder]
     */
    fun getBuilder(c: Context): NotificationCompat.Builder {
        val channelId = c.getString(R.string.notif_channel_id)
        val builder = NotificationCompat.Builder(c, channelId)
        return if (Build.VERSION.SDK_INT < 26) builder else builder.setChannelId(channelId)
    }

}