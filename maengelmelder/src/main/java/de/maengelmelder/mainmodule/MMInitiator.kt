package de.maengelmelder.mainmodule

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.core.app.ActivityCompat
import de.maengelmelder.mainmodule.activities.InternalWebActivity
import de.maengelmelder.mainmodule.activities.SplashscreenActivity
import de.maengelmelder.mainmodule.activities.TermsActivity
import de.maengelmelder.mainmodule.service.ForegroundLocationService
import de.maengelmelder.mainmodule.utils.images.ImageManipulator
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import java.lang.Exception

/**
 *
 *
 * ## Overview
 * This class is used to start the Mängelmelder module and save / load any initial, universal configuration for Mängelmelder
 */

class MMInitiator (c: Context) {

    /**
     * @property mPrefPrefix Prefix for saving configuration values. This prefix is appended before [Config] enumeration
     * @property mContext the Context for initializing [PreferenceManager]
     * @property mPref Preferences for storing config values
     */

    companion object {
        private val mPrefPrefix: String = "mm.config."

        private var mOriginActivity: Class<out Activity>? = null

        private var mInsider: MMInsider? = null

        private var mAdditionalModes: List<String>? = null

        /**
         * Initialize the required instances to start Mängelmelder (e.g. Picasso's default config, etc.).
         * Call this method in your [Application] class
         *
         * @param c Context
         */
        fun init(c: Context) {
            ImageManipulator.initPicasso(c)

            // init custom fonts if any
            val customFontPath = c.getString(R.string.mm_custom_font_normal_path)
            if (customFontPath.isNotEmpty()) {
                val config = CalligraphyConfig.Builder()
                        .setDefaultFontPath(customFontPath)
                        .setFontAttrId(io.github.inflationx.calligraphy3.R.attr.fontPath)
                        .build()
                ViewPump.init(ViewPump.builder()
                        .addInterceptor(CalligraphyInterceptor(config))
                        .build())
            }

            // init additional modes
            val modes = c.getString(R.string.mm_app_modes)
            if (modes.isNotEmpty()) {
                mAdditionalModes = modes.split(",")
            }
        }

        /**
         * Get the value of a configuration defined by the [Config] key
         * @see android.content.SharedPreferences.getString
         *
         * @param c Context
         * @param key the key from [Config] enumeration
         * @return The value of the configuration in String, or null if none
         *
         */
        fun getConfig(c: Context, key: Config): String? {
            val pref = PreferenceManager.getDefaultSharedPreferences(c)
            val pkey = mPrefPrefix + key.name
            return pref.getString(pkey, null)
        }


        /**
         * Set the origin activity's class
         */
        fun setOriginActivity(act: Class<out Activity>) {
            mOriginActivity = act
        }

        /**
         *  This will finish the current activity in module and go back to the caller activity
         *
         *  @param c Context
         *  @param finishOnUnknown If the origin activity is not set and this param is set to true, then it will instead exit the app
         */
        fun returnToOrigin(c: Context, a: Activity, finishOnUnknown: Boolean = true) {
            // Remove all possible running services
            triggerAppStopped(c)

            // Quit
            if (mOriginActivity != null) {
                a.finish()
                /*
                val i = Intent(c, mOriginActivity)
                i.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                try {
                    c.startActivity(i)
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (finishOnUnknown) {
                        ActivityCompat.finishAffinity(a)
                    }
                }
                */
            } else {
                if (finishOnUnknown) {
                    ActivityCompat.finishAffinity(a)
                }
            }
        }

        /**
         * To be trigged when app is stopped or removed from background
         */
        fun triggerAppStopped(c: Context) {
            // Stop location foreground service
            try {
                val intent = Intent(c, ForegroundLocationService::class.java).apply {
                    putExtra(ForegroundLocationService.EXTRA_CANCEL_SERVICE, true)
                }
                c.startService(intent)
            } catch (e: IllegalStateException) {
                // App is in background. Service cannot be started while the app is in background
            }
        }

        /**
         * Returns true if the app enables the mode given. false otherwise
         */
        fun hasMode(mode: String) : Boolean {
            return mAdditionalModes?.contains(mode)?: false
        }
        /**
         * Returns true if the app contains any mode
         */
        fun containsAnyMode(): Boolean {
            return mAdditionalModes?.isNotEmpty()?: false
        }

        /**
         * Opens [InternalWebActivity] with the given url.
         *
         * @param url the url to be opened
         * @param title the title of the activity using [Activity.setTitle]
         * @param type web resource type ([InternalWebActivity.DataType] enum)
         **/
        fun openURL(ctx: Context,
                            url: String,
                            title: String = "",
                            type: InternalWebActivity.DataType = InternalWebActivity.DataType.URL) {
            val i = Intent(ctx, InternalWebActivity::class.java)
            i.putExtra(InternalWebActivity.BUNDLE_DATA, url)
            i.putExtra(InternalWebActivity.BUNDLE_TYPE, type)
            i.putExtra(InternalWebActivity.BUNDLE_PAGE_TITLE, title)
            ctx.startActivity(i)
        }
    }

    private val mContext: Context = c

    private val mPref = PreferenceManager.getDefaultSharedPreferences(c)

    /**
     * Used to define the type of configuration that can be saved
     */
    enum class Config { APP_TITLE, APP_SUBTITLE }

    /**
     * Set the configuration key and its value. Builder-Style call
     *
     * @param c [Config] enum
     * @param value The value in String
     */
    fun setConfig(c: Config, value: String): MMInitiator {
        val key = mPrefPrefix + c.name
        mPref.edit().putString(key, value).apply()
        return this
    }

    /**
     * Return instance of [MMInsider] object
     */
    fun getInsider(): MMInsider {
        if (mInsider == null) mInsider = MMInsider(mContext)
        return mInsider!!
    }

    /**
     * Start Mängelmelder
     */
    fun start() {
        val i = Intent(mContext, SplashscreenActivity::class.java)
        mContext.startActivity(i)
    }

}