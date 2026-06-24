package de.maengelmelder.mainmodule.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.MMInitiator
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.databinding.MmSplashscreenBinding
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Categories
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.AppStarter
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.service.tasks.CategoriesAndAttributesThread
import de.maengelmelder.mainmodule.utils.ResourceProxy

class SplashscreenActivity : AppCompatActivity() {

    /**
     * Artificial splashscreen delay
     */
    private val DEFAULT_SPLASHSCREEN_DELAY_MS = 3000L

    private lateinit var mBinding: MmSplashscreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        mBinding = MmSplashscreenBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Load app configs
        MMConstants.load(applicationContext)

        // Delay the splashscreen, then check whether the user has accepted terms and conditions
        val checkTermsAndPrivacyRunnable = Runnable {
            if (!ResourceProxy.hasUserAcceptedTermsAndCondition(this) || isNewVersion()) {
                gotoTerms()
            } else {
                startApp()
            }
        }
        if (MMConstants.SkipSplashscreen) {
            mBinding.splashscreen.visibility = View.GONE
            checkTermsAndPrivacyRunnable.run()
        } else {
            Handler(Looper.getMainLooper()).postDelayed(
                checkTermsAndPrivacyRunnable,
                DEFAULT_SPLASHSCREEN_DELAY_MS
            )
        }

        // If offline mode is supported, we need to pre-download default categories/attributes in the background
        if (BuildConfig.DEBUG) {
            Log.d(
                "OfflineMode",
                "Offline mode feature is active: ${MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.OfflineMode]}"
            )
        }
        if (MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.OfflineMode] == true) {
            MMv1Categories(this, 32).apply {
                // Don't attach logged in credentials
                attachUserCred = false
                listener = object : MMBMS.BMSListener<Domain?, BaseResponse> {
                    override fun onData(data: Domain?) {
                        // save it to DB
                        data?.let { domain ->
                            CategoriesAndAttributesThread(
                                applicationContext,
                                listOf(domain)
                            ) {
                                if (BuildConfig.DEBUG) {
                                    Log.d("MMNetworkRepo",
                                        "saved domain info ${data.id} to DB. Num categories: ${data.categoriesAsArray().size}. ")
                                }
                            }.start()
                        }
                    }
                    override fun onFail(err: BaseResponse) {
                        // Failure possibly due to missing connection.
                        // Ignore it since this is supposed to run in bg
                    }
                }
            }.execute()
        }
    }

    private fun startApp() {
        if (MMInitiator.hasMode("idea")) {
            Intent(this, IdeaOrDefectReportActivity::class.java).also {
                startActivity(it)
            }
        } else {
            AppStarter.startOverview(this,
                    {
                        setStatus(getString(R.string.status_autologin_user, it))
                    },
                    {
                        setStatus(null)
                    }
            )
        }
    }

    private fun gotoTerms() {
        val i = Intent(this, TermsActivity::class.java)
        startActivity(i)
        finish()
    }

    /**
     * Compare version code and returns true if the current version number is higher than the old one or
     * if there's no record of the old one being present. False otherwise.
     *
     * @param revertPref If true, it will remove the preferences flag for accepting terms and policy
     */
    private fun isNewVersion(revertPref: Boolean = true) : Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val appVersion = BuildConfig.VERSION_CODE
        val oldVersion = pref.getInt(TermsActivity.PREF_OLD_VER_NUMBER, 0)
        val isNew = appVersion > oldVersion
        if (appVersion > oldVersion) {
            if (revertPref) {
                pref.edit()
                        .remove(TermsActivity.PREF_TERMS_ACCEPTED)
                        .remove(TermsActivity.PREF_POLICY_ACCEPTED)
                        .putInt(TermsActivity.PREF_OLD_VER_NUMBER, appVersion)
                        .apply()
            }
        }
        return isNew
    }

    private fun setStatus(statusText: String?) {
        with(mBinding) {
            if (statusText != null) {
                splashcreenStatus.loadingLayout.visibility = View.VISIBLE
                splashcreenStatus.loadingText.text = statusText
            } else {
                splashcreenStatus.loadingLayout.visibility = View.GONE
            }
        }
    }

}