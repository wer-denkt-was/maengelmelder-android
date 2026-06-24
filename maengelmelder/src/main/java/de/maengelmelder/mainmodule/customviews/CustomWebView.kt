package de.maengelmelder.mainmodule.customviews

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.webkit.*
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.utils.AccessibilityUtil
import de.maengelmelder.mainmodule.utils.WebView2
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Created by christian on 06.10.17.
 *
 * Customized web view with extra widgets, triggers for errors, and progressbar
 */
internal class CustomWebView(c: Context, attribs: AttributeSet? = null) : RelativeLayout(c, attribs) {

    /**
     * @property mView the inflated view
     * @property mWebView the [WebView] inside the inflated view
     * @property mErr Error text
     * @property mProgress horizontal progress bar indicating webpage loading
     * @property mAnimOnLoad animation used when loading a webpage
     */

    private val LOG = "CustomWebView"

    private var mView: View? = null
    private var mWebView: WebView2? = null
    private var mHistoryBackBtn: FloatingActionButton? = null
    private var mErr: TextView? = null
    private var mProgress: ProgressBar? = null
    private var mAnimOnLoad: Animation? = null

    private var mActionBar: ActionBar? = null
    var actionBar: ActionBar?
        get() = mActionBar
        set(value) { mActionBar = value }

    private var bEnableHistoryBackBtn = false
    var enableHistoryBackButton
        get() = bEnableHistoryBackBtn
        set(value) {
            bEnableHistoryBackBtn = value
            if (value && mWebView?.canGoBack() == true) {
                mHistoryBackBtn?.visibility = View.VISIBLE
            } else {
                mHistoryBackBtn?.visibility = View.GONE
            }
        }

    init {
        // Inflate view from R.layout.mm_custom.webview
        mView = LayoutInflater.from(c).inflate(R.layout.mm_custom_webview, null)
        addView(mView)

        // References
        mWebView = mView?.findViewById(R.id.webview)
        mErr = mView?.findViewById(R.id.err_not_loading)
        mProgress = mView?.findViewById(R.id.loadingBar)
        mHistoryBackBtn = mView?.findViewById(R.id.btn_history_back)
        mHistoryBackBtn?.setOnClickListener { v ->
            if (mWebView?.canGoBack() == true) {
                mWebView?.goBack()
            }
            resetHistoryBackButton()
        }

        // Add customized chrome and webview client
        mWebView?.webChromeClient = MyWebChromeClient()
        mWebView?.webViewClient = MyWebViewClient()
        mWebView?.settings?.javaScriptEnabled = true
        mWebView?.onUrlLoaded = { url ->
            if (url.startsWith("http://") || url.startsWith("https://")) {
                val browseIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                c.startActivity(browseIntent)
            } else if (url.startsWith("wdw-wv://")) {
                val newUrl = url.removePrefix("wdw-wv://")
                mWebView?.loadUrl(newUrl)
            } else if (url.startsWith("wdw://")) {
                // Local HTML file in mm-html
                val nameAndQuery = url.removePrefix("wdw://").split("?");
                loadUrl("file:///android_asset/mm-html/" + nameAndQuery[0])

                if (nameAndQuery.size > 1) {
                    val queryParams = nameAndQuery[1].split("&")
                    queryParams.forEach { p ->
                        val keyVal = p.split("=")
                        if (keyVal.size > 1) {
                            when (keyVal[0]) {
                                "title" -> {
                                    mActionBar?.title = URLDecoder.decode(keyVal[1], "UTF-8")
                                }
                            }
                        }
                    }
                }
            } else if (url.startsWith("file:///")) {
                mWebView?.loadUrl(url)
            } else if (url.startsWith("mailto:")) {
                c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse((url))));
            }
        }

        if (BuildConfig.debug) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    private fun resetHistoryBackButton () {
        if (!bEnableHistoryBackBtn) return
        if (mWebView?.canGoBack() == true) {
            mHistoryBackBtn?.visibility = View.VISIBLE
        } else {
            mHistoryBackBtn?.visibility = View.GONE
        }
    }

    /**
     * Closure that is triggered when a URL is being loaded
     * IMPORTANT: the webview itself has already got built-in url handler. It handles url as follow:
     * - if it's a valid URL, it will be opened in the browser app
     * -
     */
    var onUrlCalled: ((String) -> Unit)?
        get() = mWebView?.onUrlLoaded
        set(value) { mWebView?.onUrlLoaded = value }

    /**
     * Set load animation that is played when a webpage finishes loading
     * @param anim [Animation]
     */
    fun setOnLoadAnimation(anim: Animation) {
        mAnimOnLoad = anim
    }

    /**
     * Loads a URL with additional HTTP headers
     */
    fun loadUrl(url: String) {
        mWebView?.invokeUrl(url)
    }

    /**
     * @see [WebView.loadDataWithBaseURL]
     */
    fun loadDataWithBaseUrl(baseUrl: String, data: String) {
        mWebView?.loadDataWithBaseURL(baseUrl, data, "text/html; charset=utf-8", "UTF-8", null)
    }

    /**
     * @see [WebSettings.setDefaultTextEncodingName]
     */
    fun setDefaultTextEncoding(encoding: String) {
        mWebView?.settings?.defaultTextEncodingName = encoding
    }

    /**
     * @see [WebView.loadData]
     */
    fun loadData(htmlText: String, mimeType: String = "text/html", encoding: String = "UTF-8") {
        mWebView?.loadData(htmlText, mimeType, encoding)
    }

    /**
     * Get the previously loaded URL
     *
     * @see [WebView.copyBackForwardList]
     * @return previous URL or null if it doesn't exist
     */
    fun getPreviousUrl(): String? {
        val urlStack = mWebView?.copyBackForwardList()
        if (urlStack == null || urlStack.currentIndex == 0) return null
        return urlStack.getItemAtIndex(urlStack.currentIndex-1).originalUrl
    }

    /**
     * Get the currently loaded URL
     *
     * @see [WebView.getUrl]
     */
    fun getUrl(): String = mWebView?.url?: ""

    /**
     * Returns true if there is a previously loaded URL. False otherwise
     */
    fun canGoBack(): Boolean = mWebView?.canGoBack()?: false

    /**
     * Same as Back button in browser
     *
     * @see [WebView.goBack]
     */
    fun back() {
        if (mWebView?.canGoBack() == true) {
            mWebView?.goBack()
        }
    }

    fun accessibilityFocus() {
        mWebView?.let {
            AccessibilityUtil.focus(it)
        }
    }

    inner class MyWebViewClient : WebViewClient() {
        /**
         * Shows loading progress
         */
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            mErr?.visibility = View.INVISIBLE
            mProgress?.visibility = View.VISIBLE
        }

        /**
         * If it is opening MM's test server page, add authentication
         */
        override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?) {
            val serverPage = if (BuildConfig.debug) MMConstants.RegistrationPageUrl_Test else MMConstants.RegistrationPageUrl
            host?.let {
                if (serverPage.contains(it)) {
                    handler?.proceed(MMConstants.AuthCred_Test.first, MMConstants.AuthCred_Test.second)
                }
            }
        }

        /**
         * Hide progress and play loading animation, if any
         */
        override fun onPageFinished(view: WebView?, url: String?) {
            // show/hide back button when enabled
            resetHistoryBackButton()
            mProgress?.visibility = View.GONE
            mAnimOnLoad?.let { anim -> mWebView?.startAnimation(anim) }
            super.onPageFinished(view, url)
        }

        /**
         * For Android N
         */
        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            request?.url?.let { url -> mWebView?.onUrlLoaded?.invoke(url.toString()) }
            return false
        }

        /**
         * For android lower than N
         */
        @SuppressWarnings("deprecation")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            url?.let { u -> mWebView?.onUrlLoaded?.invoke(u) }
            return false
        }
    }

    inner class MyWebChromeClient : WebChromeClient() {
        /**
         * Listen to loading progress while updating the progress bar
         */
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            mProgress?.progress = newProgress
            if (newProgress == 100) {
                mProgress?.visibility = View.INVISIBLE
            }
        }
    }

}