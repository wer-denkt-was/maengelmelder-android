package de.maengelmelder.mainmodule.utils.showcases

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.widget.TextView
import com.erkutaras.showcaseview.ShowcaseActivity
import de.maengelmelder.mainmodule.utils.FontUtil

/**
 * [ShowcaseActivity] belongs to the third-party ShowcaseView library and is launched as its
 * own Activity outside of our own activities, so it never goes through
 * `ViewPumpContextWrapper`/Calligraphy like the rest of the app. Apply the host app's custom
 * font to it explicitly via the activity lifecycle instead.
 */
internal object ShowcaseFontFix {

    fun registerIn(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is ShowcaseActivity) {
                    // onActivityCreated fires as soon as ShowcaseActivity.onCreate() calls
                    // super.onCreate() - before its own onCreate() body runs setContentView()
                    // and populates the description/button text. Defer to the next message
                    // loop iteration so those views actually exist by the time we look them up.
                    activity.window.decorView.post { applyFont(activity) }
                }
            }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun applyFont(activity: Activity) {
        FontUtil.applyCustomFont(activity.findViewById<TextView>(com.erkutaras.showcaseview.R.id.textView_description_title))
        FontUtil.applyCustomFont(activity.findViewById<TextView>(com.erkutaras.showcaseview.R.id.textView_description))
        FontUtil.applyCustomFont(activity.findViewById<TextView>(com.erkutaras.showcaseview.R.id.button_done))
    }
}
