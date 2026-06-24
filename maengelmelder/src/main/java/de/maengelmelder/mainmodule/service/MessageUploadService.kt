package de.maengelmelder.mainmodule.service

import android.app.Service
import android.content.Intent
import android.os.AsyncTask
import android.os.Binder
import android.os.IBinder
import de.maengelmelder.mainmodule.network.collectives.coroutines.APIUploadMessage
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.service.tasks.MassImageUploadTask
import de.maengelmelder.mainmodule.utils.ResourceProxy
import de.maengelmelder.mainmodule.utils.notification.NotifUtility

class MessageUploadService : Service() {

    companion object {
        val KEY_SERVICE_TYPE = "uploadservice.type"
        val KEY_TIMESTAMP_CREATED = "uploadservice.createdTS"
        val KEY_MESSAGE = "uploadservice.message"
        val KEY_EMAIL_SUBSCRIPTION = "uploadservice.email"
        val TYPE_MESSAGE = 1
        @Deprecated("No longer used since we have bundle upload")
        val TYPE_MASS_UPDATE_IMAGES = 2
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // Check the service type
        when(intent?.getIntExtra(KEY_SERVICE_TYPE, 0)) {
            // Message upload
            TYPE_MESSAGE -> {
                val mb = ResourceProxy.getSerializeableExtra(intent, KEY_MESSAGE, MessageBuilder::class.java) // The message
                if (mb != null) {
                    val task = APIUploadMessage(this, mb,
                            intent.getLongExtra(KEY_TIMESTAMP_CREATED, System.currentTimeMillis()),
                            NotifUtility.getIncrementalNotifId(this))
                    task.execute()
                }
            }

            // Images upload
            TYPE_MASS_UPDATE_IMAGES -> {
                /**
                 * IMPORTANT: Since api/v1/bundle exists, this is not needed anymore. Do not use them
                 */
                val mb = intent.getSerializableExtra(KEY_MESSAGE) as MessageBuilder // Message
                val task = MassImageUploadTask(this, NotifUtility.getIncrementalNotifId(this), mb)
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }

        }

        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder = Binder()
}