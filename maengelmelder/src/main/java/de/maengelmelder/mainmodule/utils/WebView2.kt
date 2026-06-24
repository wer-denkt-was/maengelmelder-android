package de.maengelmelder.mainmodule.utils

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

internal class WebView2(ctx: Context, ast: AttributeSet) : WebView(ctx, ast) {

    private var mOnUrlLoaded: ((String) -> Unit)? = null

    var onUrlLoaded: ((String) -> Unit)?
        get() = mOnUrlLoaded
        set(value) { mOnUrlLoaded = value }

    fun invokeUrl(url: String) {
        onUrlLoaded?.invoke(url)
    }



}