package de.maengelmelder.mainmodule.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.content.res.Configuration
import android.location.Location
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.*
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.utils.notification.NotifUtility
import de.werdenktwas.modules.android.notificationutil.NotifWizard

/**
 * this foreground service will query user's location when they are available
 */
class ForegroundLocationService : Service() {

    companion object {
        /**
         * used for broadcasting user's location to GUI
         */
        val BCAST_FILTER = "de.maengelmelder.mainmodule.service.ForegroundLocationService"

        /**
         * key for the latitude value on broadcast
         */
        val RESULT_LAT = "foregroundlocationservice.result.latitude"

        /**
         * Key for longitude value on broadcast
         */
        val RESULT_LON = "foregroundlocationservice.result.longitude"

        /**
         * debug tag
         */
        private val TAG = ForegroundLocationService::class.java.simpleName

        /**
         * Key used to close the running service
         */
        val EXTRA_CANCEL_SERVICE = "foregroundlocationservice.cancel_service"
    }

    /**
     * For foreground service notification
     */
    private var mNotifWiz: NotifWizard? = null

    /**
     * Location service provider
     */
    private var mFusedLocService: FusedLocationProviderClient? = null

    /**
     * Location request
     */
    private var mLocReq: LocationRequest? = null

    /**
     * Called when new location is available
     */
    private var mLocCb: LocationCallback? = null

    /**
     * Service handler
     */
    private var mServiceHandler: Handler? = null
    private var bConfigChanged = false

    /**
     * Last user location
     */
    private var mLastLocation: Location? = null

    private val mBinder = LocalBinder()

    override fun onCreate() {
        // Notification
        val mChannelId = getString(R.string.notif_channel_id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotifWiz = NotifUtility.getWizard(this,
                    mChannelId, getString(R.string.app_name),
                    "", NotificationManager.IMPORTANCE_LOW)
        } else {
            mNotifWiz = NotifUtility.getWizardOld(this, mChannelId)
        }

        // Location service, request and callback
        mFusedLocService = LocationServices.getFusedLocationProviderClient(this)
        mLocCb = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                super.onLocationResult(res)
                mLastLocation = res.lastLocation
                broadcastLocation()
            }
        }

        mLocReq = LocationRequest.Builder(1000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        try {
            mFusedLocService?.requestLocationUpdates(mLocReq!!, mLocCb!!, Looper.myLooper()!!)
        } catch (e: SecurityException) {
            // No permission
            e.printStackTrace()
        }
        getLastLocation()

        val ht = HandlerThread(TAG)
        ht.start()
        mServiceHandler = Handler(ht.looper)

        // Assign the notification to the foreground service
        try {
            ServiceCompat.startForeground(
                this,
                NotifUtility.getIncrementalNotifId(this),
                getNotification()!! ,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                })
        } catch (e: Exception) {
            e.printStackTrace()
            // Service is possibly started from invalid state (e.g. from bgtask)
        }
    }

    fun getLastLocation() {
        try {
            mFusedLocService?.lastLocation?.addOnCompleteListener {
                if (it.isSuccessful) {
                    mLastLocation = it.result
                    broadcastLocation()
                }
            }
        } catch (ex: SecurityException) {
            // Should not appear if the permission is granted
            // Should not appear even if it's not granted since app will not fire the service
        }
    }

    /**
     * Broadcast user's last known location to GUI
     */
    fun broadcastLocation() {
        mLastLocation?.let { loc ->
            val bcast = Intent(BCAST_FILTER)
            bcast.putExtra(RESULT_LAT, loc.latitude)
            bcast.putExtra(RESULT_LON, loc.longitude)
            sendBroadcast(bcast)
        }
    }

    fun getNotification(): Notification? {
        val cancelIntent = Intent(this, ForegroundLocationService::class.java).apply {
            putExtra(EXTRA_CANCEL_SERVICE, true)
        }

        val servicePendingIntent = if (Build.VERSION.SDK_INT >= 31) {
            PendingIntent.getService(this,
                    0,
                    cancelIntent,
                    PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this,
                    0,
                    cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val builder = mNotifWiz?.getBuilder()
                ?.setContentTitle(getString(R.string.location_update_title))
                ?.setContentText(getString(R.string.location_update_content))
                ?.setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                ?.setSmallIcon(android.R.drawable.ic_menu_mylocation)
                ?.setStyle(
                        NotificationCompat.BigTextStyle()
                                .bigText(getString(R.string.location_update_content)))
                ?.setWhen(System.currentTimeMillis())
                ?.setOngoing(true)
                // Button to cancel the location update
                ?.addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.cancel_location_update),
                        servicePendingIntent)

        return builder?.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val fromNotif = intent?.getBooleanExtra(EXTRA_CANCEL_SERVICE,
                false)?: false
        if (fromNotif) {
            mLocCb?.let { loccb ->
                mFusedLocService?.removeLocationUpdates(loccb)
            }
            stopForeground(true)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        // Called when a client comes to the foreground and binds with this service.
        // The service should cease to be a foreground service when that happens.
        stopForeground(true)
        bConfigChanged = false
        return mBinder
    }

    override fun onRebind(intent: Intent?) {
        // Called when a client returns to the foreground and binds once again with this service.
        // The service should cease to be a foreground service when that happens.
        stopForeground(true)
        bConfigChanged = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // Called when the last client unbinds from this service.
        // If this method is called due to a configuration change in MainActivity, we do nothing.
        // Otherwise, we make this service a foreground service.
        if (!bConfigChanged) {
            try {
                ServiceCompat.startForeground(
                    this,
                    NotifUtility.getIncrementalNotifId(this),
                    getNotification()!! ,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        FOREGROUND_SERVICE_TYPE_LOCATION
                    } else {
                        0
                    })
            } catch (e: Exception) {
                e.printStackTrace()
                // Service is possibly started from invalid state (e.g. from bgtask)
            }
        }

        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        bConfigChanged = true
    }

    override fun onDestroy() {
        mServiceHandler?.removeCallbacksAndMessages(null)
    }

    class LocalBinder : Binder() { }

}