package de.maengelmelder.mainmodule.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.MMInitiator
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.customviews.OverviewSlidingMenu
import de.maengelmelder.mainmodule.databinding.MmActivityIdeaOrDefectReportBinding
import de.maengelmelder.mainmodule.objects.AppStarter
import de.maengelmelder.mainmodule.utils.showcases.ShowcaseBuilder

/**
 * This activity is shown before [OverviewActivity] if [R.string.mm_app_modes] contains "idea"
 */
class IdeaOrDefectReportActivity : AppCompatActivity() {

    private var bClickTwiceToExit = false
    private val DELAY_TWICE_PRESS_MS = 3000L

    var mSlidingMenu: OverviewSlidingMenu? = null

    private lateinit var mBinding: MmActivityIdeaOrDefectReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MMInitiator.setOriginActivity(this::class.java)

        mBinding = MmActivityIdeaOrDefectReportBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
        supportActionBar?.setHomeActionContentDescription(R.string.acc_cd_overview_home_icon)

        mSlidingMenu = OverviewSlidingMenu(this, this).apply {
            setMode(MessageProcessActivity.TYPE_IDEA)
        }

        mBinding.btnPickDefectreport.setOnClickListener {
            AppStarter.startOverview(this,
                    {
                        Toast.makeText(this, getString(R.string.status_autologin_user, it), Toast.LENGTH_LONG).show()
                    }
            )
        }

        mBinding.btnPickIdea.setOnClickListener {
            Intent(this, MessageProcessActivity::class.java).also { i ->
                i.putExtra(MessageProcessActivity.BUNDLE_TYPE, MessageProcessActivity.TYPE_IDEA)
                i.putExtra(MessageProcessActivity.BUNDLE_INITIAL_LAT, MMConstants.DefaultLatLon.second)
                i.putExtra(MessageProcessActivity.BUNDLE_INITIAL_LON, MMConstants.DefaultLatLon.first)
                startActivity(i)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ShowcaseBuilder.handleActivityResult(requestCode, resultCode)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Home button to close sliding menu
            android.R.id.home -> mSlidingMenu?.toggleMenu()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        // Close the sliding menu first
        if (mSlidingMenu?.isClosed == false) {
            mSlidingMenu?.close()
            return
        }
        // Click twice to exit
        if (!bClickTwiceToExit) {
            bClickTwiceToExit = true
            Toast.makeText(applicationContext, R.string.back_twice_to_exit, Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({ bClickTwiceToExit = false }, DELAY_TWICE_PRESS_MS)
        } else {
            ActivityCompat.finishAffinity(this)
        }
    }
}