package de.maengelmelder.mainmodule.utils

import android.widget.TextView
import de.maengelmelder.mainmodule.R
import io.github.inflationx.calligraphy3.TypefaceUtils

/**
 * Font-related utilities
 */
object FontUtil {

    /**
     * Applies the host app's custom font (configured via [R.string.mm_custom_font_normal_path],
     * see [de.maengelmelder.mainmodule.MMInitiator]) to [view].
     *
     * Calligraphy/ViewPump applies this font automatically to views inflated from XML, but it does
     * not reach buttons nested directly inside an [androidx.appcompat.widget.Toolbar] used as a
     * plain container (e.g. the bottom action bars in the create-message flow), so those need to
     * have the font applied explicitly.
     */
    fun applyCustomFont(view: TextView?) {
        view ?: return
        val customFontPath = view.context.getString(R.string.mm_custom_font_normal_path)
        if (customFontPath.isNotEmpty()) {
            TypefaceUtils.load(view.context.assets, customFontPath)?.let {
                view.typeface = it
            }
        }
    }
}
