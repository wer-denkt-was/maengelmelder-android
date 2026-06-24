package de.maengelmelder.mainmodule.customviews

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.*
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.MMInitiator
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.activities.*
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Logout
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.utils.UserData
import java.lang.Exception

class OverviewSlidingMenu(c: Context, a: Activity) : View.OnClickListener {

    private val mCtx = c
    private var mExtClick: ((Int, Any) -> Unit)? = null
    private val mActivity = a

    private val mDrawerLayout: DrawerLayout
    private val mMenuView: View

    private var mLogo: ImageView? = null
    private var mBtnLogin: TextView? = null
    private var mBtnLogout: TextView? = null
    private var mTxtUsername: TextView? = null
    private var mTxtMyMessages: TextView? = null
    private var mTxtMyActivities: TextView? = null
    private var mOfflineMapLayout: LinearLayout? = null
    private var mTxtVersion: TextView? = null

    private var mTxtStartPage: TextView? = null
    private var mTxtSettings: TextView? = null
    private var mTxtWelcome: TextView? = null
    private var mTxtAbout: TextView? = null
    private var mTxtDataProt: TextView? = null
    private var mTxtHelp: TextView? = null
    private var mTxtImpressum: TextView? = null
    private var mTxtNewMessage: TextView? = null
    private var mTxtNewMessageQR: TextView? = null

    private var mCurrentDomain: List<Domain>? = null
    private var mCurrentSystem: SystemInfo? = null
    private var mCurrentLatitude: Double = 0.0
    private var mCurrentLongitude: Double = 0.0

    private var mMode = MessageProcessActivity.TYPE_DEFECT_REPORT

    init {
        // Insert DrawerLayout into the activity's content view, wrapping existing content.
        // This replicates SlidingMenu's attachToActivity(SLIDING_WINDOW) behaviour.
        val contentParent = a.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val existingContent = contentParent.getChildAt(0)
        contentParent.removeView(existingContent)

        mDrawerLayout = DrawerLayout(c)
        mDrawerLayout.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)

        if (existingContent != null) {
            mDrawerLayout.addView(existingContent, DrawerLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

        mMenuView = LayoutInflater.from(c).inflate(R.layout.mm_layout_sliding_menu_overview, mDrawerLayout, false)
        val widthPx = (360 * c.resources.displayMetrics.density + 0.5f).toInt()
        mDrawerLayout.addView(mMenuView, DrawerLayout.LayoutParams(widthPx, MATCH_PARENT, GravityCompat.START))

        // Drawer opens only via toggleMenu(), not by swipe gesture.
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        contentParent.addView(mDrawerLayout)

        // View references inside the menu layout
        mBtnLogin = mMenuView.findViewById(R.id.btn_login)
        mBtnLogout = mMenuView.findViewById(R.id.btn_logout)
        mTxtUsername = mMenuView.findViewById(R.id.username)
        mTxtMyMessages = mMenuView.findViewById(R.id.menu_my_messages)
        mTxtMyActivities = mMenuView.findViewById(R.id.menu_my_activities)
        mOfflineMapLayout = mMenuView.findViewById(R.id.offlineMapLayout)
        mLogo = mMenuView.findViewById(R.id.banner)
        mTxtStartPage = mMenuView.findViewById(R.id.menu_startpage)
        mTxtSettings = mMenuView.findViewById(R.id.menu_settings)
        mTxtWelcome = mMenuView.findViewById(R.id.menu_welcome)
        mTxtAbout = mMenuView.findViewById(R.id.menu_about)
        mTxtDataProt = mMenuView.findViewById(R.id.menu_datenschutz)
        mTxtHelp = mMenuView.findViewById(R.id.menu_help)
        mTxtImpressum = mMenuView.findViewById(R.id.menu_impressum)
        mTxtVersion = mMenuView.findViewById(R.id.appversion)
        mTxtNewMessage = mMenuView.findViewById(R.id.menu_new_message)
        mTxtNewMessageQR = mMenuView.findViewById(R.id.menu_new_message_qr)

        // Click listeners
        mTxtStartPage?.setOnClickListener(this)
        if (MMConstants.ShowWelcomeMenuItem) {
            mTxtWelcome?.visibility = View.VISIBLE
            mTxtWelcome?.setOnClickListener(this)
        } else {
            mTxtWelcome?.visibility = View.GONE
        }
        mTxtDataProt?.setOnClickListener(this)
        mTxtHelp?.setOnClickListener(this)
        mTxtImpressum?.setOnClickListener(this)
        mTxtUsername?.setOnClickListener(this)
        mTxtMyActivities?.setOnClickListener(this)
        mTxtMyMessages?.setOnClickListener(this)
        mLogo?.setOnClickListener(this)

        if (MMConstants.HideAboutAppMenu) {
            mTxtAbout?.visibility = View.GONE
        } else {
            mTxtAbout?.visibility = View.VISIBLE
            mTxtAbout?.setOnClickListener(this)
        }

        if (MMInitiator.hasMode("idea")) {
            mTxtStartPage?.visibility = View.VISIBLE
        } else {
            mTxtStartPage?.visibility = View.GONE
        }

        if (MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.AppSettings] == true) {
            mTxtSettings?.visibility = View.VISIBLE
            mTxtSettings?.setOnClickListener(this)
        } else {
            mTxtSettings?.visibility = View.GONE
            mTxtSettings?.setOnClickListener(null)
        }

        if (MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.UserLogin] == true) {
            mBtnLogin?.setOnClickListener(this)
            mBtnLogout?.setOnClickListener(this)
            refreshAccount(c)
        } else {
            mBtnLogin?.visibility = View.GONE
            mBtnLogout?.visibility = View.GONE
            if (mTxtUsername?.text?.isEmpty() == true) {
                mTxtUsername?.visibility = View.GONE
            } else {
                mTxtUsername?.visibility = View.VISIBLE
            }
        }

        if (MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.ActivityHistory] == true) {
            mTxtMyActivities?.visibility = View.VISIBLE
        } else {
            mTxtMyActivities?.visibility = View.GONE
        }

        mTxtNewMessageQR?.visibility = if (MMConstants.EnableMessageCreationFromQRCode) View.VISIBLE else View.GONE

        c.packageManager.getPackageInfo(c.packageName, 0).apply {
            mTxtVersion?.text = versionName
        }
    }

    var systemInfo: SystemInfo?
        get() = mCurrentSystem
        set(value) { mCurrentSystem = value }

    var domain: List<Domain>?
        get() = mCurrentDomain
        set(value) { mCurrentDomain = value }

    var currentLatitude: Double
        get() = mCurrentLatitude
        set(value) { mCurrentLatitude = value }

    var currentLongitude: Double
        get() = mCurrentLongitude
        set(value) { mCurrentLongitude = value }

    fun setMode(mode: String) {
        mMode = mode
    }

    fun setOnNewMessageClicked(listener: ((View) -> Unit)?) {
        close()
        mTxtNewMessage?.setOnClickListener(listener)
    }

    fun setOnNewMessageQRClicked(listener: ((View) -> Unit)?) {
        close()
        mTxtNewMessageQR?.setOnClickListener(listener)
    }

    override fun onClick(v: View?) {
        doCacheLastPosition()
        when (v?.id) {
            R.id.btn_login -> {
                val i = Intent(mCtx, LoginActivity::class.java)
                i.putExtra(LoginActivity.BUNDLE_USER_LAT, mCurrentLatitude)
                i.putExtra(LoginActivity.BUNDLE_USER_LON, mCurrentLongitude)
                mActivity.startActivityForResult(i, LoginActivity.REQ_CODE)
                close()
            }
            R.id.btn_logout -> {
                mBtnLogout?.isEnabled = false
                MMv1Logout(mCtx).apply {
                    listener = mLogoutListener
                }.execute()
            }
            R.id.menu_help -> {
                if (!MMConstants.UseStaticToS) {
                    MMInitiator.openURL(mCtx,
                        MMConstants.ExternalURLsMap[MMConstants.ExternalURL.Usage] ?: "",
                        mCtx.getString(R.string.mm_title_usage),
                        InternalWebActivity.DataType.URL)
                } else {
                    MMInitiator.openURL(mCtx,
                        "mm-html/terms.html",
                        mCtx.getString(R.string.mm_title_usage),
                        InternalWebActivity.DataType.AssetHtml)
                }
            }
            R.id.menu_datenschutz -> {
                if (!MMConstants.UseStaticPrivacyPolicy) {
                    MMInitiator.openURL(mCtx,
                        MMConstants.ExternalURLsMap[MMConstants.ExternalURL.DataProtection] ?: "",
                        mCtx.getString(R.string.mm_title_data_protection),
                        InternalWebActivity.DataType.URL)
                } else {
                    MMInitiator.openURL(mCtx,
                        "mm-html/policy.html",
                        mCtx.getString(R.string.mm_title_data_protection),
                        InternalWebActivity.DataType.AssetHtml)
                }
            }
            R.id.menu_about -> {
                if (!MMConstants.UseStaticAbout) {
                    MMInitiator.openURL(mCtx,
                        MMConstants.ExternalURLsMap[MMConstants.ExternalURL.AboutApp] ?: "",
                        mCtx.getString(R.string.mm_title_about_app),
                        InternalWebActivity.DataType.URL)
                } else {
                    MMInitiator.openURL(mCtx,
                        "mm-html/aboutapp.html",
                        mCtx.getString(R.string.mm_title_about_app),
                        InternalWebActivity.DataType.AssetHtml)
                }
            }
            R.id.menu_impressum -> {
                if (!MMConstants.UseStaticImpressum) {
                    MMInitiator.openURL(mCtx,
                        MMConstants.ExternalURLsMap[MMConstants.ExternalURL.Impressum] ?: "",
                        mCtx.getString(R.string.mm_title_impressum),
                        InternalWebActivity.DataType.URL)
                } else {
                    MMInitiator.openURL(mCtx,
                        "mm-html/impressum.html",
                        mCtx.getString(R.string.mm_title_impressum),
                        InternalWebActivity.DataType.AssetHtml)
                }
            }
            R.id.menu_welcome -> {
                MMInitiator.openURL(mCtx,
                    "mm-html/intro.html",
                    mCtx.getString(R.string.mm_title_welcome),
                    InternalWebActivity.DataType.AssetHtml)
            }
            R.id.username -> {
                val i = Intent(mCtx, ProfileActivity::class.java)
                mActivity.startActivityForResult(i, ProfileActivity.REQ_CODE)
                close()
            }
            R.id.menu_my_messages -> {
                Intent(mCtx, MyMessagesActivity::class.java).also { i ->
                    i.putExtra(MyMessagesActivity.BUNDLE_MODE, mMode)
                    mActivity.startActivity(i)
                }
            }
            R.id.menu_my_activities -> {
                val i = Intent(mCtx, LogsActivity::class.java)
                mActivity.startActivity(i)
            }
            R.id.menu_settings -> {
                val i = Intent(mCtx, SettingsActivity::class.java)
                mActivity.startActivityForResult(i, SettingsActivity.REQ_CODE)
            }
            R.id.menu_startpage -> {
                if (mActivity !is IdeaOrDefectReportActivity) {
                    Intent(mCtx, IdeaOrDefectReportActivity::class.java).also { i ->
                        mActivity.startActivity(i)
                    }
                } else {
                    close()
                }
            }
            R.id.banner -> close()
        }
    }

    private fun doCacheLastPosition() {
        try {
            (mActivity as OverviewActivity).cacheLastPosition()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val mLogoutListener = object : MMBMS.BMSListener<BaseResponse, BaseResponse> {
        override fun onData(data: BaseResponse) {
            mBtnLogout?.isEnabled = true
            UserData.removeUserCred(mCtx)
            refreshAccount(mCtx)
            Toast.makeText(mCtx, R.string.info_logout_success, Toast.LENGTH_LONG).show()
        }

        override fun onFail(err: BaseResponse) {
            mBtnLogout?.isEnabled = true
            UserData.removeUserCred(mCtx)
            refreshAccount(mCtx)
        }
    }

    fun refreshAccount(ctx: Context) {
        if (MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.UserLogin] != true) return

        val user = UserData.getUserCred(ctx)

        if (user == null || !user.isUserValid()) {
            mTxtUsername?.visibility = View.GONE
            mBtnLogin?.visibility = View.VISIBLE
            mBtnLogout?.visibility = View.GONE
            mTxtUsername?.text = ""
        } else {
            mTxtUsername?.visibility = View.VISIBLE
            mTxtUsername?.text = user.publicName
            mBtnLogin?.visibility = View.GONE
            mBtnLogout?.visibility = View.VISIBLE
        }
    }

    fun toggleMenu() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START)
        } else {
            mDrawerLayout.openDrawer(GravityCompat.START)
        }
    }

    fun close() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    fun open() {
        if (!mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.openDrawer(GravityCompat.START)
        }
    }

    val isClosed: Boolean get() = !mDrawerLayout.isDrawerOpen(GravityCompat.START)
}
