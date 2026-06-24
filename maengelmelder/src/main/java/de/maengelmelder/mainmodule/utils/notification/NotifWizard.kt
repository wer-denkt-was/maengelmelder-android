package de.werdenktwas.modules.android.notificationutil

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * This class manages notification and its builders. Each instance of this class is tied with its channel Id
 */
class NotifWizard(c: Context, channelId: String) {
    /**
     * @property mChannelId channel Id
     * @property mCtx context
     */
    private val mChannelId = channelId
    private val mCtx = c

    /**
     * Get an instance of [NotificationCompat.Builder]
     *
     * @see NotificationCompat.Builder
     */
    fun getBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(mCtx, mChannelId)
    }

    /**
     * Shows the notification using the supplied notif Id
     * Requires POST_NOTIFICATION permission
     *
     * @see NotificationManagerCompat.notify
     */
    fun notify(notifId: Int, notif: Notification) {
        with(NotificationManagerCompat.from(mCtx)) {
            notify(notifId, notif)
        }
    }
}