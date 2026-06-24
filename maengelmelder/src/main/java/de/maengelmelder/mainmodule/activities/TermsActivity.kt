package de.maengelmelder.mainmodule.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.MMInitiator
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.databinding.MmActivityTermsBinding
import de.maengelmelder.mainmodule.objects.AppStarter
import de.maengelmelder.mainmodule.utils.AccessibilityUtil
import io.github.inflationx.viewpump.ViewPumpContextWrapper

/**
 * Shows user the terms of service that they need to accept before using the app
 */
class TermsActivity : AppCompatActivity() {

    companion object {
        /**
         * Preference keys for accepted terms and policy. If both keys have value "true", then the user can skip this activity
         */
        val PREF_OLD_VER_NUMBER = "pref.mm.oldVerNumber"
        val PREF_TERMS_ACCEPTED = "pref.mm.terms.accepted"
        val PREF_POLICY_ACCEPTED = "pref.mm.policy.accepted"
    }

    /**
     * State of the page display
     * [INTRO] is the introduction page (1st page). assets/mm-html/intro.html
     * [TERMS] is the page containing terms of use (2nd page). assets/mm-html/terms.html
     * [POLICY] is the page containing Data protection (3rd page). assets/mm-html/policy.html
     */
    private enum class State {
        INTRO, TERMS, POLICY
    }

    private var mState = State.INTRO
    private lateinit var mBinding: MmActivityTermsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set up content view and support action bar
        supportActionBar?.hide()

        mBinding = MmActivityTermsBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        with (mBinding) {
            webviewForWelcometext.setDefaultTextEncoding("utf-8")
            webviewForTerms.setDefaultTextEncoding("utf-8")
            webviewForPolicy.setDefaultTextEncoding("utf-8")
            webviewForTerms.enableHistoryBackButton = true
            webviewForPolicy.enableHistoryBackButton = true
        }
        // When the user rejects the terms and condition
        mBinding.reject.setOnClickListener{
            AccessibilityUtil.unfocus(mBinding.termsLayout)
            val dialog = AlertDialog.Builder(this)
                    .setMessage(R.string.warn_not_accepting_terms_and_policy)
                    .setPositiveButton(R.string.dialog_ok) { dialog, _ -> dialog.dismiss() }
                    .setNegativeButton(R.string.dialog_exit) { _, _ -> finishAffinity() }
            dialog.show()
        }

        // Moves to the next terms and condition. Terms -> Policy -> App
        mBinding.accept.setOnClickListener{
            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            when (mState) {
                State.INTRO -> {
                    mState = State.TERMS
                    show()
                }
                State.TERMS -> {
                    pref.edit().putBoolean(PREF_TERMS_ACCEPTED, true).apply()
                    mState = State.POLICY
                    show()
                }
                State.POLICY -> {
                    pref.edit().putBoolean(PREF_POLICY_ACCEPTED, true).apply()
                    AccessibilityUtil.announce(this, getString(R.string.acc_cd_app_starting))

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
            }
        }

        show()
    }

    /**
     * Display the page depending on the state
     */
    private fun show() {
        when (mState) {
            State.INTRO -> {
                with (mBinding) {
                    webviewForWelcometext.visibility = View.VISIBLE
                    webviewForTerms.visibility = View.GONE
                    webviewForPolicy.visibility = View.GONE
                    accept.setText(R.string.next)
                    supportActionBar?.title = getString(R.string.title_welcome)

                    val extUrl = MMConstants.ExternalURLsMap[MMConstants.ExternalURL.AboutApp]
                    if (!extUrl.isNullOrEmpty()) {
                        webviewForWelcometext.loadUrl(extUrl)
                    } else {
                        webviewForWelcometext.loadUrl("file:///android_asset/mm-html/intro.html")
                    }
                }
                AccessibilityUtil.announce(this, getString(R.string.title_welcome))
            }

            State.TERMS -> {
                with (mBinding) {
                    webviewForWelcometext.visibility = View.GONE
                    webviewForTerms.visibility = View.VISIBLE
                    webviewForPolicy.visibility = View.GONE
                    accept.setText(R.string.terms_accept)
                    supportActionBar?.title = getString(R.string.title_terms)

                    val extUrl = MMConstants.ExternalURLsMap[MMConstants.ExternalURL.Usage]
                    if (!extUrl.isNullOrEmpty()) {
                        webviewForTerms.loadUrl(extUrl)
                    } else {
                        webviewForTerms.loadUrl("file:///android_asset/mm-html/terms.html")
                    }
                }
                AccessibilityUtil.announce(
                    this,
                    getString(R.string.acc_cd_terms_nutzungsbedingungen_loaded)
                )
            }

            State.POLICY -> {
                with (mBinding) {
                    webviewForWelcometext.visibility = View.GONE
                    webviewForTerms.visibility = View.GONE
                    webviewForPolicy.visibility = View.VISIBLE
                    accept.setText(R.string.terms_accept)
                    supportActionBar?.title = getString(R.string.title_policy)

                    val extUrl = MMConstants.ExternalURLsMap[MMConstants.ExternalURL.DataProtection]
                    if (!extUrl.isNullOrEmpty()) {
                        webviewForPolicy.loadUrl(extUrl)
                    } else {
                        webviewForPolicy.loadUrl("file:///android_asset/mm-html/policy.html")
                    }
                }
                AccessibilityUtil.announce(this, getString(R.string.acc_cd_terms_datenschutz_loaded))
            }
        }
    }

    override fun onBackPressed() {
        AccessibilityUtil.unfocus(mBinding.termsLayout)
        val dialog = AlertDialog.Builder(this)
                .setMessage(R.string.warn_not_accepting_terms_and_policy)
                .setPositiveButton(R.string.dialog_ok) { dialog, _ -> dialog.dismiss() }
                .setNegativeButton(R.string.dialog_exit) { _, _ -> MMInitiator.returnToOrigin(this, this) }
        dialog.show()
    }

    private fun setStatus(statusText: String?) {
        with(mBinding) {
            if (statusText != null) {
                termsStatus.loadingLayout.visibility = View.VISIBLE
                termsStatus.loadingText.text = statusText
            } else {
                termsStatus.loadingLayout.visibility = View.GONE
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

}