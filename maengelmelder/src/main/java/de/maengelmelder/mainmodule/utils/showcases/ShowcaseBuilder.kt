package de.maengelmelder.mainmodule.utils.showcases

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.erkutaras.showcaseview.ShowcaseManager
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.utils.AccessibilityUtil

/**
 * ShowcaseBuilder wraps [ShowcaseManager] to provide one-time tutorial overlays.
 * Sequential tutorials are chained via [onClose] lambdas; dismissal is received
 * through [handleActivityResult] which must be called from the host Activity's
 * onActivityResult.
 */
internal object ShowcaseBuilder {

    enum class ButtonPosition {
        BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT
    }

    private var pendingCallback: (() -> Unit)? = null

    fun hasShown(c: Context, prefKey: String): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(c).getBoolean(prefKey, false)
    }

    private fun markShown(c: Context, prefKey: String) {
        PreferenceManager.getDefaultSharedPreferences(c).edit().putBoolean(prefKey, true).apply()
    }

    /**
     * Show a spotlight tutorial for [target].
     *
     * @param prefKey         When non-null the tutorial is shown at most once (SharedPreferences).
     *                        If already shown, the function returns without invoking [onClose].
     * @param marginFocusDp   Extra margin in dp added around the target view to enlarge the
     *                        spotlight circle beyond the view's own bounds (default 32 dp).
     * @param onClose         Invoked after the tutorial is dismissed.
     *                        Call the next [show] from here to chain tutorials in sequence.
     */
    fun show(
        c: Context,
        a: Activity,
        target: Target,
        title: String,
        content: String,
        hideOnTouchOutside: Boolean = true,
        prefKey: String? = null,
        buttonPos: ButtonPosition = ButtonPosition.BOTTOM_RIGHT,
        marginFocusDp: Int = 32,
        onClose: (() -> Unit)? = null
    ) {
        if (prefKey != null && hasShown(c, prefKey)) {
            Log.i("ShowcaseBuilder", "tutorial '$prefKey' already shown – skipping")
            return
        }

        if (target !is ViewTarget) {
            // ShowcaseManager only supports View targets; mark as shown and proceed.
            if (prefKey != null) markShown(c, prefKey)
            onClose?.invoke()
            return
        }

        pendingCallback = {
            if (prefKey != null) markShown(c, prefKey)
            onClose?.invoke()
        }

        ShowcaseManager.Builder()
            .context(a)
            .key(prefKey ?: "showcase_${System.currentTimeMillis()}")
            .view(target.view)
            .descriptionTitle(title)
            .descriptionText(content)
            .alphaBackground(255)
            .colorBackground(ContextCompat.getColor(a, R.color.mmcolor_showcase_bg))
            .colorFocusArea(Color.TRANSPARENT)
            .marginFocusArea(marginFocusDp)
            .colorDescText(ContextCompat.getColor(a, R.color.mmcolor_showcase_content_text))
            .buttonText(a.getString(android.R.string.ok))
            .buttonVisibility(true)
            .cancelButtonVisibility(false)
            .add()
            .build()
            .show()

        AccessibilityUtil.announce(c, "$title. $content")
    }

    /**
     * Call from every host Activity's onActivityResult to fire the pending [onClose] chain.
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == ShowcaseManager.REQUEST_CODE_SHOWCASE) {
            pendingCallback?.invoke()
            pendingCallback = null
        }
    }
}
