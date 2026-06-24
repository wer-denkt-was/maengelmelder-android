package de.maengelmelder.mainmodule.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.adapters.DomainsSpinnerAdapter
import de.maengelmelder.mainmodule.customviews.dialogs.GPSAlertDialog
import de.maengelmelder.mainmodule.customviews.dialogs.LoadingDialog
import de.maengelmelder.mainmodule.databinding.MmActivityLoginBinding
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.collectives.coroutines.APIGetDomains
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Login
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1System
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.objects.UserCred
import de.maengelmelder.mainmodule.utils.AccessibilityUtil
import de.maengelmelder.mainmodule.utils.ActivityUtil
import de.maengelmelder.mainmodule.utils.UserData
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.coroutines.selects.select
import java.lang.Exception

class LoginActivity : AppCompatActivity(), View.OnClickListener, MMBMS.BMSListener<UserCred, BaseResponse> {

    companion object {
        const val REQ_CODE = 120
        const val BUNDLE_USER_LAT = "wdw.login.user_lat"
        const val BUNDLE_USER_LON = "wdw.login.user_lon"
        const val BUNDLE_FORCE_SYSTEM = "wdw.login.systeminfo"
        const val BUNDLE_FORCE_DOMAIN_ID = "wdw.login.domainid"
        const val BUNDLE_FORCE_DOMAIN_NAME = "wdw.login.domainname"
    }

    private var mDomains: Array<Domain>? = null
    private var mSystemHost: SystemInfo? = null
    private var mForcedDomainId: Int = 0

    private var mLoadingDialog: LoadingDialog? = null
    private lateinit var mBinding: MmActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = MmActivityLoginBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mForcedDomainId = intent.getIntExtra(BUNDLE_FORCE_DOMAIN_ID, 0)
        val forcedDomainName = intent.getStringExtra(BUNDLE_FORCE_DOMAIN_NAME)?: ""
        val forcedSystemInfo = ActivityUtil.getIntentSerializeableExtra(intent, BUNDLE_FORCE_SYSTEM, SystemInfo::class.java)

        with (mBinding) {
            btnLogin.setOnClickListener(this@LoginActivity)
            btnRegister.setOnClickListener(this@LoginActivity)
        }

        if (MMConstants.UseDefaultDomainWhenPossible) {
            // Fixed domain id? Just use the default domain. No need to query system info
            val defDomain = Domain().apply {
                id = MMConstants.DefaultDomainId.toString()
                name = MMConstants.DefaultDomainName
                systemId = mSystemHost?.appId?: "1"
            }
            mDomains = arrayOf(defDomain)
        } else if (mForcedDomainId > 0 && forcedDomainName.isNotEmpty()) {
            // Force user to log into this domain instead
            // Hide the dropdown
            val currentUserCred = UserData.getUserCred(this)
            mSystemHost = forcedSystemInfo
            mBinding.apply {
                domainChoices.visibility = View.GONE
                forcedDomainLogin.visibility = View.VISIBLE
                forcedDomainLogin.text = getString(R.string.override_domain_login, forcedDomainName)
                if (currentUserCred != null && currentUserCred.isUserValid()) {
                    warnLogOutFromPreviousAccount.visibility = View.VISIBLE
                    warnLogOutFromPreviousAccount.text = getString(R.string.warn_previous_account_auto_logout, currentUserCred.domain?.name, forcedDomainName)
                } else {
                    warnLogOutFromPreviousAccount.visibility = View.GONE
                }
            }
            val forcedDomain = Domain().apply {
                id = mForcedDomainId.toString()
                name = forcedDomainName
                systemId = mSystemHost?.appId?: "1"
            }
            mDomains = arrayOf(forcedDomain)
        } else {
            val lat = intent.getDoubleExtra(BUNDLE_USER_LAT, 0.0)
            val lon = intent.getDoubleExtra(BUNDLE_USER_LON, 0.0)
            toggleDomainLoading(true)
            if ((lat > -0.1 && lat < 1.0) || (lon > -0.1 && lon < 1.0)) {
                if (MMConstants.NoLocationFallbackMessage.isNotEmpty()) {
                    val defDomain = Domain().apply {
                        id = MMConstants.DefaultDomainId.toString()
                        name = MMConstants.DefaultDomainName
                        systemId = "1"
                    }
                    toggleDomainLoading(false)
                    mBinding.noLocationMessage.visibility = View.VISIBLE
                    mBinding.noLocationMessage.text = MMConstants.NoLocationFallbackMessage
                    showLoginForm(listOf(defDomain))
                } else {
                    showFailedDomainRetrieval()
                }
            } else {
                MMv1System(this, lat, lon).apply {
                    listener = object : MMBMS.BMSListener<List<SystemInfo>, BaseResponse> {
                        override fun onData(data: List<SystemInfo>) {
                            // Systeminfo found
                            val extOnly = if (data.size == 1) data else data.filter { d -> d.isExternal }
                            mSystemHost = try { extOnly.first() } catch (e: Exception) { null }
                            if (mSystemHost != null) {
                                doRetrieveDomains(mSystemHost!!, lat, lon)
                            } else {
                                // Failed domain retrieval
                                showFailedDomainRetrieval()
                            }
                        }

                        override fun onFail(err: BaseResponse) {
                            showFailedDomainRetrieval()
                        }
                    }
                }.execute()
            }
        }
    }

    fun doRetrieveDomains(systemInfo: SystemInfo, lat: Double, lon: Double) {
        APIGetDomains(this, listOf(systemInfo), lat, lon).apply {
            success = {
                if (it.isEmpty()) {
                    // No domains found
                    showFailedDomainRetrieval()
                } else {
                    toggleDomainLoading(false)
                    showLoginForm(it)
                }
            }
            error = {
                // Failed to retrieve domains
                showFailedDomainRetrieval()
            }
        }.execute()
    }

    fun showLoginForm(domains: List<Domain>) {
        mBinding.domainChoices.adapter = DomainsSpinnerAdapter(this@LoginActivity, domains)
        mBinding.domainChoices.visibility = View.VISIBLE

        val savedUser = UserData.getUsername(this)
        if (savedUser.isNotEmpty()) {
            mBinding.username.setText(savedUser)
        }
    }

    fun showFailedDomainRetrieval() {
        toggleDomainLoading(false)
        AlertDialog.Builder(this)
            .setMessage(R.string.login_failed_to_retrieve_domain)
            .setNegativeButton(R.string.dialog_ok2) { dialog, _ ->
                onBackPressed()
                dialog.dismiss()
            }.show()
    }

    fun toggleDomainLoading(toggle: Boolean) {
        if (mLoadingDialog == null) mLoadingDialog = LoadingDialog(this, getString(R.string.loading_domains))
        if (toggle) {
            if (mLoadingDialog?.isShowing == false) mLoadingDialog?.show()
        } else {
            if (mLoadingDialog?.isShowing == true) mLoadingDialog?.dismiss()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    override fun onClick(v: View?) {
        when (v) {
            mBinding.btnLogin -> {
                with (mBinding) {
                    val email = username.text.toString()
                    val pass = password.text.toString()

                    if (email.isEmpty() || pass.isEmpty()) {
                        error.visibility = View.VISIBLE
                        error.text = getString(R.string.error_empty_user_pass)
                        AccessibilityUtil.focus(error)
                    } else {
                        error.visibility = View.INVISIBLE
                        loading.visibility = View.VISIBLE
                        UserData.saveUsername(this@LoginActivity, email)
                        btnLogin.isEnabled = false
                        btnRegister.isEnabled = false

                        val selDomain: Domain? = if (mForcedDomainId > 0) {
                            mDomains?.get(0)
                        } else {
                            try {
                                domainChoices.selectedItem as Domain?
                            } catch (e: Exception) {
                                null
                            }
                        }

                        // Logout first
                        UserData.logout(this@LoginActivity) {
                            // Then login
                            if (selDomain != null) {
                                MMv1Login(this@LoginActivity, email, pass, selDomain).apply {
                                    externalSystemInfo = mSystemHost
                                    listener = this@LoginActivity
                                }.execute()
                            } else {
                                // TODO no domain selected (should not be possible anyway)
                            }
                        }
                    }
                }
            }

            mBinding.btnRegister -> {
                val selectedDomain = mBinding.domainChoices.selectedItem as Domain?
                var registrationUrl = if (BuildConfig.debug) MMConstants.RegistrationPageUrl_Test else MMConstants.RegistrationPageUrl
                if (selectedDomain != null && selectedDomain.bmsUrl.isNotEmpty()) {
                    registrationUrl = selectedDomain.bmsUrl.removeSuffix("/bms") + "/login"
                }

                val i = Intent(this, InternalWebActivity::class.java)
                i.putExtra(InternalWebActivity.BUNDLE_PAGE_TITLE, getString(R.string.register))
                i.putExtra(InternalWebActivity.BUNDLE_TYPE, InternalWebActivity.DataType.URL)
                i.putExtra(InternalWebActivity.BUNDLE_DATA, registrationUrl)
                startActivity(i)
            }
        }
    }

    override fun onData(data: UserCred) {
        with (mBinding) {
            loading.visibility = View.GONE
            btnLogin.isEnabled = true
            btnRegister.isEnabled = true
        }

        if (data.isUserValid()) {
            UserData.saveUserCred(ctx = this, userCred = data)
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            onFail(BaseResponse(MMv1Login.RESP_WRONG_CRED, ""))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return true
            }
        }

        return false
    }

    override fun onFail(err: BaseResponse) {
        with (mBinding) {
            loading.visibility = View.GONE
            btnLogin.isEnabled = true
            btnRegister.isEnabled = true
        }
        UserData.removeUserCred(this)
        val appCtx = applicationContext
        when (err.code) {
            403, MMv1Login.RESP_WRONG_CRED -> {
                with (mBinding) {
                    error.visibility = View.VISIBLE
                    error.text = getString(R.string.error_wrong_user_pass)
                }
            }
            404 -> {
                Toast.makeText(appCtx, R.string.err_not_found, Toast.LENGTH_SHORT).show()
            }
            408 -> {
                Toast.makeText(appCtx, R.string.err_server_no_respond, Toast.LENGTH_SHORT).show()
            }
            500, MMv1Login.RESP_SERVER_ERROR -> {
                Toast.makeText(appCtx, R.string.err_server_500, Toast.LENGTH_SHORT).show()
            }
            503 -> {
                Toast.makeText(appCtx, R.string.err_server_503, Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(appCtx, getString(R.string.err_general, err.code.toString()), Toast.LENGTH_SHORT).show()
            }
        }
    }
}