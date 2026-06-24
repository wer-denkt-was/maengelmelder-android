package de.maengelmelder.mainmodule.activities

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.databinding.MmActivityInternalWebBinding
import de.maengelmelder.mainmodule.utils.ResourceProxy
import io.github.inflationx.viewpump.ViewPumpContextWrapper

/**
 * Used to open webpages internally inside the app. Uses [de.maengelmelder.mainmodule.customviews.CustomWebView]
 */
class InternalWebActivity : AppCompatActivity() {

    /**
     * Type of data being displayed
     * - URL means that the data sent is a valid URL
     * - HTML text means that the data sent is an HTML text
     * - AssetHtml means that the data sent is a path to the HTML page in assets folder (only path starting from assets folder)
     */
    enum class DataType {
        URL, HTMLText, AssetHtml
    }

    companion object {
        val BUNDLE_DATA = "mm.internalweb.data"
        val BUNDLE_TYPE = "mm.internalweb.type"
        val BUNDLE_PAGE_TITLE = "mm.internalweb.pagetitle"
    }

    private lateinit var mBinding: MmActivityInternalWebBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = MmActivityInternalWebBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        setSupportActionBar(findViewById(R.id.toolbar))

        val title = intent.getStringExtra(BUNDLE_PAGE_TITLE)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (title != null) {
            supportActionBar?.title = title
        }

        mBinding.internalWeb.actionBar = supportActionBar

        // top right back button to navigate the webpages
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val data = intent.getStringExtra(BUNDLE_DATA)
        val type = ResourceProxy.getSerializeableExtra(intent, BUNDLE_TYPE, DataType::class.java)

        // Load the given data
        if (data != null && data.isNotEmpty()) {
            with (mBinding) {
                when (type) {
                    DataType.URL -> internalWeb.loadUrl(data)
                    DataType.HTMLText -> internalWeb.loadData(data)
                    DataType.AssetHtml -> internalWeb.loadUrl("file:///android_asset/$data")
                    else -> internalWeb.loadUrl(data)
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                with (mBinding) {
                    if (internalWeb.canGoBack()) {
                        internalWeb.back()
                    } else {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}