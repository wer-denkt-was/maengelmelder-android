package de.maengelmelder.mainmodule.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.material.tabs.TabLayout
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.view.get
import de.maengelmelder.mainmodule.utils.showcases.ViewTarget
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.adapters.StepViewPagerAdapter
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.databinding.MmActivityProcessMessageBinding
import de.maengelmelder.mainmodule.fragments.*
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.utils.Connectivity
import de.maengelmelder.mainmodule.utils.ResourceProxy
import de.maengelmelder.mainmodule.utils.showcases.ShowcaseBuilder
import io.github.inflationx.viewpump.ViewPumpContextWrapper

/**
 * This activity houses different fragments required to initiate message creation steps.
 * The fragments, and future ones, should inherit from [BaseMessageStepFragment].
 *
 * It handles:
 * - Message creation process (assigning ID, default values)
 * - Saving and loading existing messages
 * - Navigation in-between fragments
 * - fragment UI loading
 */
class MessageProcessActivity : AppCompatActivity(), TabLayout.OnTabSelectedListener, androidx.viewpager.widget.ViewPager.OnPageChangeListener {

    companion object {
        val BUNDLE_MESSAGE_ID   = "MessageProcessActivity._id"
        val BUNDLE_NO_LOC       = "MessageProcessActivity.no_location"
        val BUNDLE_TYPE         = "MessageProcessActivity.type"
        val BUNDLE_INITIAL_LAT  = "MessageProcessActivity.initLat"
        val BUNDLE_INITIAL_LON  = "MessageProcessActivity.initLon"
        val BUNDLE_INITIAL_CATEGORY  = "MessageProcessActivity.initCategory"
        val BUNDLE_INITIAL_QPARAM = "MessageProcessActivity.qParams" // Usually used for attribute values
        val BUNDLE_SOURCE  = "MessageProcessActivity.activityStartSource"

        val REQ_CODE            = 4541
        // position, foto, att
        val JUMP_TO_STEP        = "MessageProcessActivity.jump_to_step"

        val TYPE_DEFECT_REPORT  = "defect_report"
        val TYPE_IDEA           = "idea"

        val SOURCE_NORMAL       = "normal"
        val SOURCE_DEEPLINK     = "deeplink"
    }

    /**
     * Database
     */
    private lateinit var mDB: MMDB

    /**
     * Specialized builder for new and existing messages
     */
    private lateinit var mMessageBuilder: MessageBuilder

    /**
     * The fragment which is currently being shown to the user
     */
    private var mCurrentFragment: BaseMessageStepFragment? = null

    /**
     * Array containing the titles connected to each Fragment
     */
    private var mSteps: Array<StepInfo>? = null

    /**
     * Current position of the shown fragment
     */
    private var mPos: Int = 0

    /**
     * Report or Idea
     */
    protected var mCreationType = TYPE_DEFECT_REPORT

    /**
     * normal or through QR Code
     */
    protected var mActivitySource = SOURCE_NORMAL

    private var mNoInitialLoc = false

    private lateinit var mBinding: MmActivityProcessMessageBinding

    /**
     * Internet connection monitoring
     */
    protected var bHasInternetConn = true
    private val mNetworkCb = Connectivity.getNetworkCallback(object: Connectivity.NetworkAvailability {
        override fun onAvailabilityChanged(activeNetworkCount: Int) {
            runOnUiThread {
                if (activeNetworkCount > 0) {
                    bHasInternetConn = true
                    mBinding.txtConnectionMonitor.visibility = View.GONE
                    mBinding.connectivityStatus.visibility = View.GONE
                } else {
                    bHasInternetConn = false
                    mBinding.txtConnectionMonitor.visibility = View.VISIBLE
                    mBinding.connectivityStatus.visibility = View.VISIBLE
                }
            }
        }
    })

    /**
     * Preference key for showing navigation tutorial
     */
    private val PREF_TUT_NAVI = "wdw.mm.tut_message.navigation"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = MmActivityProcessMessageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        setSupportActionBar(findViewById(R.id.toolbar))

        // Get instance of local database
        mDB = MMDB.instance(this)

        // Retrieve creation mode
        mCreationType = intent.getStringExtra(BUNDLE_TYPE)?: TYPE_DEFECT_REPORT
        if (mCreationType == TYPE_IDEA) {
            // Adds back button here
            supportActionBar?.let { ab ->
                ab.setDisplayHomeAsUpEnabled(true)
                ab.setDisplayShowHomeEnabled(true)
            }
        }

        // Retrieve the message ID from previous activity (if the user is trying to edit saved message)
        val msgId = intent.getStringExtra(BUNDLE_MESSAGE_ID)
        // Get source
        mActivitySource = intent.getStringExtra(BUNDLE_SOURCE)?: SOURCE_NORMAL

        // Initiate a new builder
        mMessageBuilder = MessageBuilder()
        var shouldSaveNew = true

        // Message ID is passed, retrieve the message from local DB and add it to the builder
        if (msgId != null && this::mDB.isInitialized) {
            // Existing
            val m = mDB.getMessage(msgId)
            if (m != null) {
                mMessageBuilder.message = m
                mCreationType = m.internalType
                mMessageBuilder.attributeValuesFromJson(mDB.getExtrasJSON(msgId))
                shouldSaveNew = false
            }
        }

        // No message ID means new message will be created
        if (shouldSaveNew) {
            val msg = mDB.addSelfCreatedMessage(this, mCreationType)
            mMessageBuilder.message = msg
        }

        // initialize the array
        mSteps = if (mCreationType == TYPE_IDEA) {
            arrayOf(
                    StepInfo(this, getString(R.string.step_choose_category), R.drawable.ic_step_category, ChooseCategoryStep()),
                    StepInfo(this, getString(R.string.step_choose_location), R.drawable.ic_step_standort, ChooseLocationStep()),
                    StepInfo(this, getString(R.string.step_choose_photo), R.drawable.ic_step_photo, ChoosePhotoStep()),
                    StepInfo(this, getString(R.string.step_edit_attributes), R.drawable.ic_step_edit, FillAttributesStep()),
                    StepInfo(this, getString(R.string.step_review), R.drawable.ic_step_review, ReviewStep())
            )}
            else if (MMConstants.CategoryBeforePosition) {
            arrayOf(
                    StepInfo(this, getString(R.string.step_choose_category), R.drawable.ic_step_category, ChooseCategoryStep()),
                    StepInfo(this, getString(R.string.step_choose_location), R.drawable.ic_step_standort, ChooseLocationStep()),
                    StepInfo(this, getString(R.string.step_choose_photo), R.drawable.ic_step_photo, ChoosePhotoStep()),
                    StepInfo(this, getString(R.string.step_edit_attributes), R.drawable.ic_step_edit, FillAttributesStep()),
                    StepInfo(this, getString(R.string.step_review), R.drawable.ic_step_review, ReviewStep())
            )}
            else {
            arrayOf(
                    StepInfo(this, getString(R.string.step_choose_location), R.drawable.ic_step_standort, ChooseLocationStep()),
                    StepInfo(this, getString(R.string.step_choose_photo), R.drawable.ic_step_photo, ChoosePhotoStep()),
                    StepInfo(this, getString(R.string.step_choose_category), R.drawable.ic_step_category, ChooseCategoryStep()),
                    StepInfo(this, getString(R.string.step_edit_attributes), R.drawable.ic_step_edit, FillAttributesStep()),
                    StepInfo(this, getString(R.string.step_review), R.drawable.ic_step_review, ReviewStep())
            )}

        // Retrieve the latitude-longitude values from the previous activity
        mNoInitialLoc = intent.getBooleanExtra(BUNDLE_NO_LOC, false)
        if (!mNoInitialLoc) {
            val initLat = intent.getDoubleExtra(BUNDLE_INITIAL_LAT, Double.MAX_VALUE)
            val initLon = intent.getDoubleExtra(BUNDLE_INITIAL_LON, Double.MAX_VALUE)
            if (initLat != Double.MAX_VALUE && initLon != Double.MAX_VALUE) {
                mMessageBuilder.setLocation(initLat, initLon)
            }
        }

        // Retrieve type if it exists
        val type = ResourceProxy.getSerializeableExtra(intent, BUNDLE_INITIAL_CATEGORY, Category::class.java)
        if (type != null) {
            mMessageBuilder.category = type
        }

        // BUNDLE_INITIAL_QPARAM is given through deeplink (e.g. URL or QR-Code). See [DeepLinkDetectActivity]
        // set attribute if it exists (BUNDLE_INITIAL_QPARAM), also other possible settings
        val qParam = intent.getStringExtra(BUNDLE_INITIAL_QPARAM)
        if (qParam != null) {
            val queryHash = ResourceProxy.parseQueryParameter(qParam)
            val attrHash = try { queryHash["attribute_values"] as Map<String, Any?> } catch (e: Exception) { null }
            if (!attrHash.isNullOrEmpty()) {
                // attribute values
                attrHash.forEach { entry ->
                    if (entry.key.endsWith("_force")) {
                        // Store attribute editable setting
                        // boolean-string (true|false)
                        mMessageBuilder.addAdditionalData(entry.key, entry.value?.toString()?: "false")
                    } else {
                        // Store the rest of values
                        if (entry.value != null) {
                            mMessageBuilder.addAttributeValue(entry.key, entry.value)
                        }
                    }
                }
            }

            // Store any "forced" variables. It will be stored as string-boolean
            if (!mMessageBuilder.hasAdditionalData("force_loc")
                && queryHash.containsKey("force_loc")) {
                // force_loc=1|0
                val forceLocParam = (queryHash["force_loc"] as String) == "1"
                mMessageBuilder.addAdditionalData("force_loc", forceLocParam.toString())
            }
            if (!mMessageBuilder.hasAdditionalData("force_typeid")
                && queryHash.containsKey("force_typeid")) {
                val forceTypeid = try { (queryHash["force_typeid"] as String).toLong() } catch (e: Exception) { 0 }
                val forceType = forceTypeid > 0L
                mMessageBuilder.addAdditionalData("force_typeid", forceType.toString())
            }

            // Mark this message as being created from QR
            mMessageBuilder.addAdditionalData("from_qr_code", true.toString())
        }
        // Save the additional data to DB
        mDB.updateMessage(mMessageBuilder.messageId,
            Pair(
                mDB.constants.COL_ADDITIONAL,
                mMessageBuilder.message.additionalDataToJSON().toString()))

        // mMessageBuilder.debugAdditionalData();

        // Check if for this message, some of the steps are disabled
        if (mMessageBuilder.isCategoryLocked()) {
            // Type id is forced
            val step = mSteps?.find { s -> s.stepName == getString(R.string.step_choose_category) }
            step?.let { s -> s.isLocked = true }
        }
        if (mMessageBuilder.isLocationLocked()) {
            // location is forced
            val step = mSteps?.find { s -> s.stepName == getString(R.string.step_choose_location) }
            step?.let { s -> s.isLocked = true }
        }
        // Get to the nearest tab that isn't locked
        mPos = mSteps?.indexOfFirst { x -> !x.isLocked }?: 0

        // Viewpager
        val vpAdapter = StepViewPagerAdapter(supportFragmentManager, mSteps!!)

        with (mBinding) {
            viewpager.adapter = vpAdapter
            viewpager.offscreenPageLimit = mSteps?.size ?: 5 / 2
            tabs.setupWithViewPager(viewpager)
            tabs.addOnTabSelectedListener(this@MessageProcessActivity)

            if (MMConstants.ForcePositionConfirmation) {
                // If this settings is enabled, user cannot move from position tab until they confirm the position
                tabs.visibility = View.GONE
            }
            mCurrentFragment = mSteps?.get(mPos)?.fragment

            // Since the first fragment is a map, we disable it first
            viewpager.canSwipe = false
            setupBottomNavi()

            // Set viewpager listener
            viewpager.addOnPageChangeListener(this@MessageProcessActivity)

            // Jump to the given step if given
            var jumpToStep = intent.getStringExtra(JUMP_TO_STEP)
            if (mPos > 0 && jumpToStep == null) {
                // if mPos is bigger than 0 (means the first fragment is possibly locked / inaccessible)
                jumpToStep = mSteps?.get(mPos)?.name
            }
            if (jumpToStep != null) {
                moveToByName(jumpToStep)
            } else {
                // Update title
                supportActionBar?.title = mSteps?.get(0)?.name?: getString(R.string.activity_new_message)
            }

            // Check connectivity
            val hasConnection = Connectivity.hasActiveConnection(this@MessageProcessActivity)
            if (hasConnection) {
                mBinding.connectivityStatus.visibility = View.GONE
                txtConnectionMonitor.visibility = View.GONE
            } else {
                mBinding.connectivityStatus.visibility = View.VISIBLE
                txtConnectionMonitor.visibility = View.VISIBLE
            }
        }
    }

    override fun onPageScrollStateChanged(state: Int) { }
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { }

    override fun onPageSelected(position: Int) {
        doChangeFragment(position)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    override fun onResume() {
        Connectivity.enableConnectivityMonitor(this, mNetworkCb)
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        Connectivity.disableConnectivityMonitor(this, mNetworkCb)
    }

    fun fromDeeplink(): Boolean {
        return mActivitySource == SOURCE_DEEPLINK
    }

    fun hadNoInitialPosition(): Boolean {
        return mNoInitialLoc
    }

    /**
     * Go to the next designated fragment
     */
    fun nextStep() {
        moveToByIdx(mPos + 1)
    }

    fun nextStepAfterSelectingCategory(category: Category) {
        if (category.posReq == Category.POS_OPTIONAL || category.posReq == Category.POS_NEVER) {
            val nextFragment = try { mSteps?.get(mPos+1) } catch (e: Exception) { null }
            if (nextFragment != null && nextFragment.fragment is ChooseLocationStep) {
                // If the next fragment is location step and position is not mandatory
                // We can skip it to the next fragment
                moveToByIdx(mPos + 2)
            } else {
                nextStep()
            }
        } else {
            nextStep()
        }
    }

    /**
     * Go to the previous fragment
     */
    fun previousStep() {
        moveToByIdx(mPos - 1)
    }

    /**
     * Switch to fragment indicated by position
     */
    private fun doChangeFragment(position: Int) {
        // Set the current position
        mPos = position
        val step = mSteps?.get(position)
        // Get the fragment for the position
        val fragment = step?.fragment
        // Change title
        supportActionBar?.title = step?.name
        // Determine if the fragment can be swiped to move to the next / previous fragment
        // The fragment containing maps should not be swipeable since it will interfere with panning
        mBinding.viewpager.canSwipe = fragment?.canBeSwipedInsideViewPager()?: true
        // Hide the soft keyboard if it is open
        try { hideSoftKeyboard() } catch (e: Exception) { }
        // refreshOfflineMap the bottom navigation to reflect the changes in selected / unselected fragment
        setupBottomNavi()
        // Invoke onViewBroughtUp()
        fragment?.onViewBroughtUp()
    }

    /**
     * Sets the bottom navigation so it will show the correct layout for selected / unselected tabs
     */
    private fun setupBottomNavi() {
        if (mSteps?.size != mBinding.tabs.tabCount) return

        val tabChild = mBinding.tabs.getChildAt(0) as ViewGroup
        (0 until mBinding.tabs.tabCount).forEach { i ->
            val step = mSteps?.get(i)
            val tab = mBinding.tabs.getTabAt(i)
            if (i == mPos) {
                // Selected tab
                step?.isSelected = true
            } else if (i == mPos + 1) {
                // 1 after selected tab
                step?.isAfterSelected = true
            } else {
                // other tabs
                step?.isSelected = false
                step?.isAfterSelected = false
            }

            if (tab?.contentDescription?.isEmpty() == true) {
                tab.contentDescription = getString(R.string.acc_cd_messageprocess_tabitem, step?.name)
            }

            // Disable the tab if the step has pre-defined value and is locked
            if (step?.isLocked == true) {
                tabChild.getChildAt(i).isEnabled = false
            }

            // Modify the view so it shows the correct background
            val customView = tab?.customView
            if (customView == null) {
                tab?.customView = step?.modifyView(null, i)
            } else {
                step?.modifyView(customView , i)
            }
        }
    }

    /**
     * Move to a fragment identified by its name. It uses [mSteps] mapping
     * if no location is defined, user cannot move to the next step before setting up the location
     */
    fun moveToByName(stepName: String) {
        if (mNoInitialLoc && !mMessageBuilder.isLocationValid()) {
            return
        }

        if ((mMessageBuilder.getAdditionalData("force_typeid")?:"false").toBoolean()
            && stepName == getString(R.string.step_choose_category)) {
            return
        }

        if ((mMessageBuilder.getAdditionalData("force_loc")?:"false").toBoolean()
            && stepName == getString(R.string.step_choose_location)) {
            return
        }

        if (mSteps == null) return
        var idx = -1
        mSteps?.forEachIndexed { index, pair ->
            if (pair.name == stepName) {
                idx = index
                return@forEachIndexed
            }
        }

        if (idx != -1) {
            moveToByIdx(idx)
        }
    }

    /**
     * Move to a fragment by the index in [mSteps] array
     * if no location is defined, user cannot move to the next step before setting up the location
     */
    fun moveToByIdx(idx: Int) {
        if (mNoInitialLoc && !mMessageBuilder.isLocationValid()) {
            return
        }

        // don't move if it's locked
        val step = try { mSteps?.get(idx) } catch (e: Exception) { null }
        if (step == null || step.isLocked) {
            return
        }

        if (mMessageBuilder.isLocationValid() && mBinding.tabs.visibility == View.GONE) {
            // Re-init the tab selection listener once location is valid
            // So the user can navigate with tabs again
            mBinding.tabs.visibility = View.VISIBLE
        }

        if (idx >= 0 && idx < (mSteps?.size ?: 0)) {
            mBinding.viewpager.currentItem = idx
        }
    }

    fun getBuilder(): MessageBuilder = mMessageBuilder

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Save the content of the message before going back
        saveMessage()
        // Inform the user that the message is already saved
        val textId = if (mCreationType == TYPE_IDEA) R.string.save_idea else R.string.save_message
        Toast.makeText(applicationContext, textId, Toast.LENGTH_LONG).show()

        // if this came from deeplink, back button should start the overview map activity
        if (fromDeeplink()) {
            startActivity(Intent(this, OverviewActivity::class.java))
        } else {
            // Finish the activity
            setResult(Activity.RESULT_OK)
        }
        finish()
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) { }
    override fun onTabReselected(tab: TabLayout.Tab?) { }

    /**
     * When a tab is selected
     */
    override fun onTabSelected(tab: TabLayout.Tab?) {
        if (mNoInitialLoc && !mMessageBuilder.isLocationValid()) {
            return
        }

        // Hide the soft keyboard
        try {
            hideSoftKeyboard()
        } catch (e: Exception) { /* Probably due to keyboard not being shown at the first place */ }

        // This is called on the current fragment. Usually it is for saving the content inside the fragment
        mCurrentFragment?.executeBeforeChange()

        // Check if previous fragment needs to display prompt before moving
        val prevFragment = mSteps?.get(mPos)?.fragment
        if (prevFragment != null && prevFragment.shouldPromptBeforeChange()) {
            prevFragment.promptBeforeChange { res -> }
        }

        if (tab == null) return
        // Change the fragment according to the tab position
        mCurrentFragment = mSteps?.get(tab.position)?.fragment
    }

    /**
     * Hide the soft keyboard
     *
     * @see InputMethodManager.hideSoftInputFromWindow
     */
    private fun hideSoftKeyboard() {
        val inpMgr = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { v ->
            inpMgr.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    /**
     * Forwards the permission callback to the underlying current fragment
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        mCurrentFragment?.onRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * Forwards activity callback to the underlying current fragment
     */
    private var mTutorialStarted = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || mTutorialStarted) return
        mTutorialStarted = true

        mBinding.root.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            if (!hasWindowFocus()) {
                mTutorialStarted = false
                return@postDelayed
            }
            ShowcaseBuilder.show(
                this, this,
                ViewTarget(mBinding.tabs),
                getString(R.string.tut_navigation_title),
                getString(R.string.tut_navigation_content),
                true,
                PREF_TUT_NAVI,
                ShowcaseBuilder.ButtonPosition.BOTTOM_LEFT
            )
        }, 600L)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        ShowcaseBuilder.handleActivityResult(requestCode, resultCode)
        mCurrentFragment?.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_message_process, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.menu_save -> {
                // Save message and quit the activity
                saveMessage()
                Toast.makeText(applicationContext, R.string.info_message_saved, Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }

            R.id.menu_cancel -> {
                // Delete current message and quit the activity
                mDB.deleteMessage(mMessageBuilder.messageId)
                Toast.makeText(applicationContext, R.string.info_message_cancelled, Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }

            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Save the content the user has filled in for the message. This includes image file paths, filled forms, position, chosen categories, etc.
     */
    private fun saveMessage() {
        // Iterate through all steps and save all data provided inside the fragment
        mSteps?.forEach { step ->
            try {
                step.fragment.executeBeforeChange()
            } catch (e: Exception) {
                e.printStackTrace() /* Fragment might not have been created or accessed yet */
            }
        }

        // Update the message status
        mDB.updateMessage(mMessageBuilder.messageId, Pair(mDB.constants.COL_UPLOAD_STATUS, ""))

        // LOG: logged when message is saved
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val canSaveLog = pref.getBoolean(getString(R.string.mm_prefkey_should_log), true)
        if (canSaveLog) {
            mDB.addLog(de.maengelmelder.mainmodule.objects.Log.TYPE_MSG_EDITED,
                    hashMapOf(
                            de.maengelmelder.mainmodule.objects.Log.KEY_MSG_ID to (mMessageBuilder.messageId),
                            de.maengelmelder.mainmodule.objects.Log.KEY_TITLE to (mMessageBuilder.title)
                    )
            )
        }
    }

    /**
     * Clear the rubrik selection in Category step
     */
    fun clearRubricSelection() {
        mSteps?.find { step -> step.fragment is ChooseCategoryStep }?.let { step ->
            try {
                (step.fragment as ChooseCategoryStep).clearRubricSelection()
            } catch (e: Exception) {
                // Should not happen
            }
        }
    }

    /**
     * Holds information of the steps needed to complete a message (photo, category, etc.)
     */
   inner class StepInfo(val context: Context, val name: String, val drawableRes: Int, val fragment: BaseMessageStepFragment) {
        /**
         * @property bIsSelected whether the step is currently being selected / shown
         * @property bIsAfterSelected whether the step is the one after the selected. This is used so the tabview can use the proper
         * background
         * @property mColorSelected The color for selected step
         * @property mColorNormal The color for normal, unselected step
         * @property bIsLocked affects tab display
         */

        private var bIsSelected = false
        private var bIsAfterSelected = true
        private var mColorSelected = ContextCompat.getColor(context, R.color.mmcolor_tab_selected_text)
        private var mColorNormal = ContextCompat.getColor(context, R.color.mmcolor_tab_normal_text)

        private var bIsLocked = false

        private var mDrawableLocked = R.drawable.ic_lock

        var isSelected: Boolean
            get() = bIsSelected
            set(value) {
                bIsSelected = value
                if (bIsSelected) bIsAfterSelected = false
            }

        var isLocked: Boolean
            get() = bIsLocked
            set(value) {
                bIsLocked = value
            }

        var isAfterSelected: Boolean
            get() = bIsAfterSelected
            set(value) {
                bIsAfterSelected = value
                if (bIsAfterSelected) bIsSelected = false
            }

        val stepName get() = name

        /**
         * Modify the tabview and populate them with proper and background
         *
         * @param view existing tabview. Put null to create a new one
         * @param idx index of the tab. used due to the first step using different set of bg images than others
         *
         * @return the modified view or a new one
         */
        fun modifyView(view: View? = null, idx: Int): View {
            val v = view?: LayoutInflater.from(context).inflate(R.layout.mm_tabitem_step, null)
            val vh = try { v.tag as TabViewHolder } catch (e: Exception) { TabViewHolder(v) }

            vh.text.text = if (bIsLocked) "" else name
            vh.icon.setImageResource(if (bIsLocked) mDrawableLocked else drawableRes)
            v.contentDescription = getString(R.string.acc_cd_messageprocess_tabitem, name)

            if (isSelected) {
                vh.parent.setBackgroundResource(if (idx == 0) R.drawable.mm_bg_step_first_selected else R.drawable.mm_bg_step_selected)
                vh.text.setTextColor(mColorSelected)
                vh.icon.setColorFilter(mColorSelected)
            } else if (isAfterSelected) {
                vh.parent.setBackgroundResource(R.drawable.mm_bg_step_normal_after_selected)
                vh.text.setTextColor(mColorNormal)
                vh.icon.setColorFilter(mColorNormal)
            } else {
                vh.parent.setBackgroundResource(if (idx == 0) R.drawable.mm_bg_step_first_normal else R.drawable.mm_bg_step_normal)
                vh.text.setTextColor(mColorNormal)
                vh.icon.setColorFilter(mColorNormal)
            }
            return v
        }
   }

    inner class TabViewHolder(v: View) {
        val parent = v.findViewById<RelativeLayout>(R.id.parent)
        val icon = v.findViewById<ImageView>(R.id.icon)
        val text = v.findViewById<AppCompatTextView>(R.id.txt)
    }
}