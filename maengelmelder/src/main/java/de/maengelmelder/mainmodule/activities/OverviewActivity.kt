package de.maengelmelder.mainmodule.activities

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import de.maengelmelder.mainmodule.utils.showcases.ViewTarget
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.MMInitiator
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.customviews.OverviewSlidingMenu
import de.maengelmelder.mainmodule.customviews.dialogs.AppRatingDialog
import de.maengelmelder.mainmodule.customviews.dialogs.FilterMsgDialog
import de.maengelmelder.mainmodule.customviews.dialogs.GPSAlertDialog
import de.maengelmelder.mainmodule.customviews.dialogs.MessageCreationOptionDialog
import de.maengelmelder.mainmodule.customviews.dialogs.PermissionDialog
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.databinding.MmActivityOverviewBinding
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.collectives.coroutines.APIUploadMessage
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Message
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1System
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.MessageFilterParam
import de.maengelmelder.mainmodule.objects.MessageGroup
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.service.ForegroundLocationService
import de.maengelmelder.mainmodule.service.receivers.BroadcastFilterList
import de.maengelmelder.mainmodule.utils.*
import de.maengelmelder.mainmodule.utils.interfaces.IMapHelper
import de.maengelmelder.mainmodule.utils.showcases.ShowcaseBuilder
import io.github.inflationx.viewpump.ViewPumpContextWrapper

/**
 * The first view the user will see. Contains map and navigations to other pages
 */
class OverviewActivity: AppCompatActivity(), View.OnClickListener {

    /**
     * @property BUNDLE_MSG_ID bundle key containing message Id. If value exists, the map will pan to the location of the message
     * @property TUT_HOME_PREFKEY preference key for tutorial for left menu button
     * @property TUT_NEW_MSG_PREFKEY preference key for tutorial on how to create new message
     * @property DELAY_TWICE_PRESS_MS window delay in ms for exiting the app, by clicking back button twice
     * @property MAX_ZOOM Maximum allowed zoom level. This is also used to determine whether the map should put group markers or not
     * @property DELAY_MESSAGE_API_MS adds delay in between message API requests.
     *
     * @property mMapHelper map helper
     * @property mGetNearestMessageReq the request object for getting the list of messages nearby
     * @property mGetNearestMessageResp the response object for list of nearby messages
     *
     * @property mDefaultDomain default domain. Only contains the domain Id 32 or as described in BuildConfig
     * @property mSelectedMsgId id of the selected message from the map
     * @property mSelectedMessageGroup instance of selected [MessageGroup] on the map
     * @property mSlidingMenu instance of [OverviewSlidingMenu], the menu from the left side of the page
     * @property bClickTwiceToExit if true, the next back button press will close MM
     * @property mAnimPopupMsgShown Animation for popup panel when a message on the map is clicked
     * @property mMessageUploadComplete Broadcast receiver for message upload status
     *
     * @property mLastMessagesList List containing the last downloaded [Message]s. This list is purged every time the map is panned
     * @property mMarkerDrawingThread thread for drawing map markers
     * @property mMarkerGroupDistThreshold The max distance between markers to put them in 1 group. Calculated based on the dimension of the marker image
     * @property mMessageDbMgmtThread thread for saving messages to local database
     *
     * @property mV1SystemApi instance of [MMv1System]
     * @property mApiNearestMsg instance of [APIGetNearestMessages]
     */
    companion object {
        const val BUNDLE_MSG_ID = "wdw.maengelmelder2.overview.msgId"
        const val MAX_INVALID_POSITION_COUNT_FOR_GPS_WARNING = 3
    }

    private val TUT_QRCODE_PREFKEY = "wdw.mm.tut_qr_code.shown"
    private val TUT_NEW_MSG_PREFKEY = "wdw.mm.tut_new_msg.shown"
    private val TUT_HOME_PREFKEY = "wdw.mm.tut_home.shown"
    private val PREF_LAST_LAT = "overview.last_lat"
    private val PREF_LAST_LON = "overview.last_lon"
    private val PREF_INSTALL_INFO = "wdw.mm.install_info"

    private val MAX_ZOOM = 20

    val DELAY_TWICE_PRESS_MS = 3000L
    val DELAY_MESSAGE_API_MS = 3000L

    private var mCurrentMsgAPITs = System.currentTimeMillis()
    private lateinit var mMapHelper: GoogleMapHelper
    private lateinit var mGPSAlertDialog: GPSAlertDialog
    private var mCountInvalidPosition = 0

    var mLastMessagesList: ArrayList<Message> = arrayListOf()
    var mGetNearestMessageReq: MMv1Message? = null
    var mGetNearestMessageResp: NearestMessagesRequestListener? = null
    private val mDefaultDomain = Domain(MMConstants.DefaultDomainId.toString()).apply {
        name = MMConstants.DefaultDomainName
    }

    var mV1SysApiRef: MMv1System? = null
    var mV1MsgApiRefs: ArrayList<MMv1Message> = arrayListOf()

    var mSelectedMsgId: String? = null
    var mSelectedMessageGroup: MessageGroup? = null

    var mSlidingMenu: OverviewSlidingMenu? = null
    private var mTutorialsStarted = false

    private var bClickTwiceToExit = false

    private var mAnimPopupMsgShown: Animation? = null
    private val mMessageUploadComplete = MessageUploadBroadcastReceiver()
    private val mLocationUpdateReceiver = LocationUpdateBroadcastReceiver()

    private var mMarkerGroupDistThreshold = 0.0
    private var mMarkerDrawingThread: MarkerDrawingThread? = null
    private var mMessageDbMgmtThread: DBMessageManagementThread? = null

    private val mHandler = Handler()

    private var mCurrentSystem: SystemInfo? = null
    private var mCurrentDomains: List<Domain>? = null

    private var mLastDownloadedMsgIds = arrayListOf<String>()
    private var mLocationRetrieveProgressDialog: ProgressDialog? = null

    // Activity results
    private lateinit var mAfterActivityCenterMap: ActivityResultLauncher<Intent>
    private lateinit var mAllPermRequest: ActivityResultLauncher<Array<String>>
    private lateinit var mLocServicePermRequest: ActivityResultLauncher<String>
    private lateinit var mMyLocPermRequest: ActivityResultLauncher<String>

    private var mFilterMsgDialog: FilterMsgDialog? = null
    private var mMessageFilterParam: MessageFilterParam? = null // Null = no filter

    private lateinit var mBinding: MmActivityOverviewBinding

    // Connectivity
    private var mDeviceHasConnection = true
    private val mNetworkCb = Connectivity.getNetworkCallback(object: Connectivity.NetworkAvailability {
        override fun onAvailabilityChanged(activeNetworkCount: Int) {
            runOnUiThread {
                if (activeNetworkCount > 0) {
                    mDeviceHasConnection = true
                    mBinding.connectivityStatus.visibility = View.GONE
                } else {
                    mDeviceHasConnection = false
                    mBinding.connectivityStatus.visibility = View.VISIBLE
                }
            }
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.mm_activity_overview)
        mBinding = MmActivityOverviewBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Title and subtitle, also setting up toolbar as actionbar
        setSupportActionBar(findViewById(R.id.toolbar))
        val title = MMInitiator.getConfig(this, MMInitiator.Config.APP_TITLE)
        val subtitle = MMInitiator.getConfig(this, MMInitiator.Config.APP_SUBTITLE)
        supportActionBar?.title = title
        supportActionBar?.subtitle = subtitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
        supportActionBar?.setHomeActionContentDescription(R.string.acc_cd_overview_home_icon)

        mLocationRetrieveProgressDialog = ProgressDialog(this).apply {
            setMessage(getString(R.string.loading_retrieving_location))
        }

        // activity results
        mAfterActivityCenterMap = ActivityUtil.startActivityForResult(this) {
            getLastCachedPosition().let { pos ->
                setCenter(pos.second, pos.first, 16)
            }
            mBinding.newMessage.isEnabled = true
            showRatingWindow()
        }

        // Permission results
        mAllPermRequest = ActivityUtil.requestPermissions(this) {
            if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                navigateToOwnLoc()
            }
        }
        mLocServicePermRequest = ActivityUtil.requestPermission(this) {
            if (it) startLocationForegroundService()
        }
        mMyLocPermRequest = ActivityUtil.requestPermission(this) {
            if (it) navigateToOwnLoc()
        }

        // GPS alert dialog
        mGPSAlertDialog = GPSAlertDialog(this, this, mMyLocPermRequest)

        // Obtain the map fragment and initialize
        val fragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        fragment.getMapAsync(object: OnMapReadyCallback {
            override fun onMapReady(map: GoogleMap) {
                // Setup the map helper
                mMapHelper = GoogleMapHelper(this@OverviewActivity, map)

                // Move to the last cached position
                getLastCachedPosition().let { pos ->
                    val movePos = if (pos.first == 0.0 && pos.second == 0.0) {
                        MMConstants.DefaultLatLon
                    } else pos
                    // Low zoom for 0,0
                    setCenter(movePos.second, movePos.first,
                            if (movePos.first == 0.0 && movePos.second == 0.0) 2 else 16)
                    // Immediately retrieve messages
                    retrieveMessage(movePos.second, movePos.first)
                }

                // Pan listener. It also triggers get_domain
                mMapHelper.setMapPanListener(this@OverviewActivity, { /* Map starts moving */ }) { lon: Double, lat: Double ->
                    retrieveMessage(lat, lon)
                }

                // Upon tapping the map, hide the detail panel
                mMapHelper.setMapClickListener(this@OverviewActivity) { _, _ ->
                    if (mBinding.detail.visibility == View.VISIBLE) {
                        mBinding.detail.visibility = View.GONE
                    }
                    mMapHelper.clearHighlights()
                }

                // When a marker is clicked
                mMapHelper.setOnMarkerClickListener(this@OverviewActivity) { id, data ->
                    if (id?.startsWith(MessageGroup.PREFIX_ID) == true) {
                        // Grouped marker. Click and zoom
                        val group = data as MessageGroup
                        displaySelectedMessageGroup(group)
                    } else {
                        displaySelectedMessage(id, MAX_ZOOM)
                    }
                }

                // enable user location
                if (ActivityUtil.isPermissionGranted(this@OverviewActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    enableLocation()
                }

                // Preference for default map display
                val pref = PreferenceManager.getDefaultSharedPreferences(this@OverviewActivity)
                if (pref.getBoolean(getString(R.string.mm_prefkey_satasdefaulttile), false)) {
                    mMapHelper.changeDisplayTo(IMapHelper.Display.SATELLITE)
                }

                mMapHelper.loadGeoJSONLayers(mBinding.layerswitcher)
            }
        })

        // Set up connectivity indicator
        mDeviceHasConnection = Connectivity.hasActiveConnection(this)
        mBinding.connectivityStatus.visibility = if (mDeviceHasConnection) View.GONE else View.VISIBLE
        // TODO mBinding.connectivityStatus should show information on offline mode for user when clicked

        // Qr code button
        mBinding.scanQr.visibility = if (MMConstants.EnableMessageCreationFromQRCode) View.VISIBLE else View.GONE

        // Listener
        mBinding.newMessage.setOnClickListener(this)
        mBinding.detail.setOnClickListener(this)
        mBinding.myLocation.setOnClickListener(this)
        mBinding.messageList.setOnClickListener(this)
        mBinding.messageFilter.setOnClickListener(this)
        mBinding.scanQr.setOnClickListener(this)

        // Sliding menu
        mSlidingMenu = OverviewSlidingMenu(this, this).apply {
            setOnNewMessageClicked { v -> this@OverviewActivity.onClick(mBinding.newMessage) }
            if (MMConstants.EnableMessageCreationFromQRCode) {
                setOnNewMessageQRClicked { v -> this@OverviewActivity.onClick(mBinding.scanQr) }
            }
        }

        // Build config settings applied
        if (MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.ActivityHistory] == false) {
            val key = getString(R.string.mm_prefkey_should_log)
            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            pref.edit().putBoolean(key, false).apply()
        }

        // Build config settings for message list
        if (MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.MessageList] == true) {
            mBinding.messageList.visibility = View.VISIBLE
        } else {
            mBinding.messageList.visibility = View.GONE
        }

        // Explicitly ask users for permission to read external storage and location
        // New after Android 13: Notification requires permission as well
        if (PermissionDialog.canShow(this)) {
            val locPermAllowed = ActivityUtil.isPermissionGranted(this, Manifest.permission.ACCESS_FINE_LOCATION)
            val camPermAllowed = ActivityUtil.isPermissionGranted(this, Manifest.permission.CAMERA)
            val notifPermAllowed = ActivityUtil.isPermissionGranted(this, Manifest.permission.POST_NOTIFICATIONS)
            if (!locPermAllowed || !camPermAllowed || !notifPermAllowed) {
                val permList = arrayListOf<String>()
                if (!locPermAllowed) {
                    permList.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                if (!camPermAllowed) {
                    permList.add(Manifest.permission.CAMERA)
                }
                if (!notifPermAllowed) {
                    permList.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                PermissionDialog(this, this, permList, mAllPermRequest).show()
            }
        }

        // One time warning on install
        if (MMConstants.ShowWarningNotEmergencyService) {
            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            val canShowInfo = pref.getBoolean(PREF_INSTALL_INFO, true)
            if (canShowInfo) {
                AlertDialog.Builder(this)
                        .setMessage(R.string.warn_firsttime_not_emergency_app)
                        .setPositiveButton(R.string.ok) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                        .create()
                        .show()
                pref.edit().putBoolean(PREF_INSTALL_INFO, false).apply()
            }
        }

        mBinding.messageFilterIndicator.visibility = View.GONE
    }

    override fun onStart() {
        // register broadcast receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mMessageUploadComplete, IntentFilter(BroadcastFilterList.MESSAGE_UPLOAD), RECEIVER_EXPORTED)
            registerReceiver(mLocationUpdateReceiver, IntentFilter(ForegroundLocationService.BCAST_FILTER), RECEIVER_EXPORTED)
        } else {
            registerReceiver(mMessageUploadComplete, IntentFilter(BroadcastFilterList.MESSAGE_UPLOAD))
            registerReceiver(mLocationUpdateReceiver, IntentFilter(ForegroundLocationService.BCAST_FILTER))
        }

        super.onStart()
    }

    override fun onStop() {
        // Unregister
        unregisterReceiver(mMessageUploadComplete)
        unregisterReceiver(mLocationUpdateReceiver)
        super.onStop()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Build options menu for test version to display Test environment
        if (BuildConfig.debug) {
            menuInflater.inflate(R.menu.menu_test, menu)
        } else {
            menuInflater.inflate(R.menu.menu_overview, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Home button to close sliding menu
            android.R.id.home -> mSlidingMenu?.toggleMenu()
            // test environment menu for test version only
            R.id.menu_testinfo -> {
                val intent = Intent(this, TestInfoActivity::class.java)
                startActivity(intent)
            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(p0: View?) {
        when (p0) {
            // Go to message detail page
            mBinding.detail -> toDetail()

            // Pan map to current location
            mBinding.myLocation -> {
                val locPermGranted = ActivityUtil.isPermissionGranted(
                        this, Manifest.permission.ACCESS_FINE_LOCATION)
                if (::mMapHelper.isInitialized ) {
                    if (locPermGranted) {
                        startLocationForegroundService()
                    } else {
                        mLocServicePermRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            }

            // Message filter
            mBinding.messageFilter -> {
                showFilterDialog()
            }

            // message list
            mBinding.messageList -> {
                cacheLastPosition()
                val i = Intent(this, MessagesListActivity::class.java).apply {
                    putExtra(MessagesListActivity.BUNDLE_MESSAGE_IDS, mLastDownloadedMsgIds)
                }
                mAfterActivityCenterMap.launch(i)
            }

            // create new message with map center as the location
            mBinding.newMessage -> {
                startNewMessage()
            }

            // Create new message from QR scanning
            mBinding.scanQr -> {
                if (!MMConstants.EnableMessageCreationFromQRCode) {
                    return
                }
                // Start QR activity
                startActivity(Intent(
                    this@OverviewActivity,
                    QRCodeActivity::class.java)
                )
            }
        }
    }

    /**
     * Starts new message process
     */
    private fun startNewMessage() {
        if (::mMapHelper.isInitialized) {
            // disable button to avoid spam click
            mBinding.newMessage.isEnabled = false
            // Cache the last position of the user
            cacheLastPosition()
            mLocationRetrieveProgressDialog?.show()

            val i = Intent(this, MessageProcessActivity::class.java)
            // For some reason, the MessageProcessActivity can be launched twice even with 1 button press
            i.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            mMapHelper.getMyLocation { lon, lat ->
                mLocationRetrieveProgressDialog?.hide()
                var latitude = lat
                var longitude = lon
                if (latitude == 0.0 && longitude == 0.0) {
                    if (MMConstants.ForcePositionConfirmation) {
                        // show GPS position not available
                        AlertDialog.Builder(this)
                            .setTitle(R.string.warn_position_not_found_title)
                            .setMessage(R.string.warn_position_not_found_desc)
                            .setPositiveButton(R.string.warn_position_not_found_btn_yes) { dialog, which ->
                                // Move to message creation but don't put the location
                                // Warn process that user has no location predefined and has to put it manually
                                dialog.dismiss()
                                i.putExtra(MessageProcessActivity.BUNDLE_NO_LOC, true)
                                mAfterActivityCenterMap.launch(i)
                            }.setNegativeButton(R.string.warn_position_not_found_btn_no) { dialog, which ->
                                // Re-call location
                                mBinding.newMessage.isEnabled = true
                                dialog.dismiss()
                            }.show()
                    } else {
                        // get the center position of the map
                        mMapHelper.getCenter().also { center ->
                            latitude = center.second
                            longitude = center.first
                        }
                        i.putExtra(MessageProcessActivity.BUNDLE_INITIAL_LAT, latitude)
                        i.putExtra(MessageProcessActivity.BUNDLE_INITIAL_LON, longitude)
                        mAfterActivityCenterMap.launch(i)
                    }
                } else {
                    // GPS coordinate received. Proceed as usual
                    i.putExtra(MessageProcessActivity.BUNDLE_INITIAL_LAT, latitude)
                    i.putExtra(MessageProcessActivity.BUNDLE_INITIAL_LON, longitude)
                    mAfterActivityCenterMap.launch(i)
                }
            }
        }
    }

    /**
     * Starts location tracking. Requires location permission
     */
    private fun startLocationForegroundService() {
        val i = Intent(this, ForegroundLocationService::class.java)
        ContextCompat.startForegroundService(this, i)

        mMapHelper.getMyLocation { longitude, latitude ->
            if (longitude == 0.0 && latitude == 0.0) {
                // Don't center location on 0,0
                // isFinished is required because dialog is sometimes showing when activity is finished
                showGpsDialog()
            } else {
                setCenter(latitude, longitude, 16)
            }
        }
    }

    private fun showGpsDialog() {
        mCountInvalidPosition += 1
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val dontShow = pref.getBoolean(GPSAlertDialog.PREF_KEY_DONT_SHOW, false)
        if (!mGPSAlertDialog.isShowing && !isFinishing && !dontShow && mCountInvalidPosition >= MAX_INVALID_POSITION_COUNT_FOR_GPS_WARNING) {
            // Only show GPS alert dialog after 3 invalid position delivered by GPS
            // Sometimes the GPS shows alert dialog at the first time due to GPS calibration
            mGPSAlertDialog.show()
            mCountInvalidPosition = 0
        }
    }

    private fun showFilterDialog() {
        if (mFilterMsgDialog == null) {
            mFilterMsgDialog = FilterMsgDialog(this)
        }

        mFilterMsgDialog?.setStatuses(getListOfMessageStates())
        if (mMessageFilterParam !== null) {
            val previousFilteredStatuses = mMessageFilterParam?.statuses
            mFilterMsgDialog?.setExistingStatus(
                previousFilteredStatuses?.toTypedArray() ?: arrayOf()
            )
        }

        mFilterMsgDialog?.setListener(object: FilterMsgDialog.Listener {
            override fun onCancel() {}
            override fun onFilter(
                desc: String,
                cat: String,
                favOnly: Boolean,
                statuses: Array<String>,
                isDefaultFilter: Boolean
            ) {
                if (!isDefaultFilter) {
                    if (mMessageFilterParam == null) {
                        mMessageFilterParam = MessageFilterParam()
                    }
                    mMessageFilterParam?.apply {
                        text = desc
                        category = cat
                        favoriteOnly = favOnly
                    }
                    mMessageFilterParam?.statuses?.clear()
                    mMessageFilterParam?.statuses?.addAll(statuses)
                } else {
                    mMessageFilterParam = null
                }

                mBinding.messageFilterIndicator.visibility = if (mMessageFilterParam == null) View.GONE else View.VISIBLE
                // Re-retrieve the messages
                val lastPos = mMapHelper.getCenter()
                retrieveMessage(lastPos.second, lastPos.first)
            }
        })
        mFilterMsgDialog?.show()
    }

    private fun getListOfMessageStates(): Set<Pair<String, Int>> {
        val set = HashSet<Pair<String, Int>>()
        mLastMessagesList.forEach { m ->
            set.add(Pair(m.state, Color.BLACK))
        }
        return set
    }

    /**
     * Shows app-rating window
     *
     * TODO might be good if we could replace it with Google Android's default in-app review window
     * https://developer.android.com/guide/playcore/in-app-review
     */
    private fun showRatingWindow() {
        if (MMConstants.ShowAppRatingWindow) {
            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            val canShowInfo = pref.getBoolean(AppRatingDialog.PREF_HAS_SHOWN_APPRATING, false)
            val db = MMDB.instance(this)
            val numCreatedMessages = db.getMessagesCount(
                db.constants.COL_ORIGIN to db.constants.ORIGIN_SELF,
                db.constants.COL_UPLOAD_STATUS to db.constants.STATUS_FINISHED
            )

            if (!canShowInfo && numCreatedMessages >= MMConstants.ShowAppRatingWindowAfterNMessages && mDeviceHasConnection) {
                Handler(Looper.getMainLooper()).postDelayed({
                    // Show app rating window
                    if (!isFinishing) {
                        AppRatingDialog(this).show()
                    }
                }, 5000)
            }
        }
    }

    /**
     * Go to message detail. It requires a valid [mSelectedMsgId]
     */
    private fun toDetail() {
        cacheLastPosition()
        if (mSelectedMsgId != null) {
            // Individual message
            val i = Intent(this, MessageDetailActivity::class.java)
            i.putExtra(MessageDetailActivity.BUNDLE_MSG_ID, mSelectedMsgId)
            mAfterActivityCenterMap.launch(i)
        } else if (mSelectedMessageGroup != null) {
            // Grouped message
            val i = Intent(this, MessagesListActivity::class.java)
            val messageIDs = ArrayList(mSelectedMessageGroup?.messages?.map { msg -> msg.generateId() }?: listOf())
            i.putExtra(MessagesListActivity.BUNDLE_MESSAGE_IDS, messageIDs)
            mAfterActivityCenterMap.launch(i)
        }
    }

    /**
     * Utilize systems API to retrieve all messages from the given systems
     */
    private fun getNearestMessages(lat: Double, lon: Double) {
        // Cancel currently running one if any
        mV1SysApiRef?.cancel("Map is moved by user")
        if (mV1MsgApiRefs.isNotEmpty()) {
            mV1MsgApiRefs.forEach { r -> r.cancel("Map is moved by user") }
        }
        mV1MsgApiRefs.clear()

        // System API
        mV1SysApiRef = MMv1System(this, lat, lon).apply {
            listener = (object: MMBMS.BMSListener<List<SystemInfo>, BaseResponse> {
                override fun onData(data: List<SystemInfo>) {
                    val extOnly = if (data.size == 1) data else data.filter { d -> d.isExternal }
                    mCurrentSystem = try { extOnly.first() } catch (e: Exception) { null }

                    mSlidingMenu?.systemInfo = mCurrentSystem

                    // Directly calls nearest messages
                    // mMetaQueue.resolve(sys)
                    // Get map extents
                    val ext = mMapHelper.getMapExtent()
                    val zoom = mMapHelper.getZoomLevel()
                    val ctx = this@OverviewActivity

                    // For each external systems, call message APIs in parallel
                    extOnly.forEach { extSystem ->
                        val msgAPI = MMv1Message(ctx,
                                ext.first.second, ext.first.first,
                                ext.second.second, ext.second.first,
                                zoom).apply {

                            externalSystemInfo = extSystem

                            listener = (object : MMBMS.BMSListener<List<Message>, BaseResponse> {
                                override fun onData(data: List<Message>) {

                                    mLastMessagesList.addAll(data)

                                    domains?.let { d ->
                                        synchronized(this@OverviewActivity) {
                                            if (d.isNotEmpty()) {
                                                mCurrentDomains = d
                                                mSlidingMenu?.domain = mCurrentDomains
                                            }
                                        }
                                    }

                                    doProcessMessagesList(data)
                                }
                                override fun onFail(err: BaseResponse) {
                                    // Fail silently
                                    hideLoading()
                                }
                            })
                        }
                        msgAPI.execute()
                        mV1MsgApiRefs.add(msgAPI)
                    }

                }
                override fun onFail(err: BaseResponse) {
                    // Just fail silently
                    hideLoading()
                }
            })
        }

        mV1SysApiRef?.execute()
    }

    private fun doProcessMessagesList(list: List<Message>) {
        // Hide the loading part
        hideLoading()

        // Hide focused message detail panel
        mBinding.detail.visibility = View.GONE

        // Filter the messages with given messagefilterparam first
        var filteredList = list

        mMessageFilterParam?.let {
            filteredList = list.filter { m -> m.passFilter(it) }
        }

        // Use worker thread to save messages to DB
        mMessageDbMgmtThread?.stopThread()
        mMessageDbMgmtThread = DBMessageManagementThread(this@OverviewActivity, filteredList)
        mMessageDbMgmtThread?.start()

        // Do not use non-UI thread for this
        mMarkerDrawingThread = MarkerDrawingThread(this@OverviewActivity, filteredList)
        mMarkerDrawingThread?.run()

        // Keep the last downloaded message IDs for list viewing
        mLastDownloadedMsgIds.clear()
        mLastDownloadedMsgIds.addAll(filteredList.map { m -> m.generateId() })

        AccessibilityUtil.announce(this, getString(R.string.acc_announce_messages_on_map, filteredList.size.toString()))
    }

    /**
     * Get the nearest messages with provided domain. The domain's id is the one used to determine the
     * origin of messages
     */
    private fun getNearestMessages(dom: Domain) {
        mCurrentDomains = arrayListOf(dom)
        mSlidingMenu?.domain = mCurrentDomains

        if(mGetNearestMessageReq != null)
            mGetNearestMessageReq?.cancel("Map is moved by user")

        if (mGetNearestMessageResp == null)
            mGetNearestMessageResp = NearestMessagesRequestListener(this)

        val ext = mMapHelper.getMapExtent()

        mGetNearestMessageReq = MMv1Message(this,
                ext.first.second, ext.first.first,
                ext.second.second, ext.second.first,
                mMapHelper.getZoomLevel(),
                dom.id?.toInt()?: 0).apply {

            listener = (object : MMBMS.BMSListener<List<Message>, BaseResponse> {
                override fun onData(data: List<Message>) {
                    mLastMessagesList.addAll(data)
                    domains?.let { d ->
                        synchronized(this@OverviewActivity) {
                            if (d.isNotEmpty()) {
                                mCurrentDomains = d
                                mSlidingMenu?.domain = mCurrentDomains
                            }
                        }
                    }
                    doProcessMessagesList(data)
                }
                override fun onFail(err: BaseResponse) {
                    // Fail silently
                    hideLoading()
                }
            })
        }.also { api ->
            api.execute()
        }
    }

    /**
     * Show statusbar when API call is running
     */
    private fun showLoading() {
        mBinding.statusbar.visibility = View.VISIBLE
    }

    /**
     * Hides the status bar for loading message
     */
    private fun hideLoading() {
        mBinding.statusbar.visibility = View.GONE
    }

    /**
     * Display group information
     */
    private fun displaySelectedMessageGroup(group: MessageGroup?) {
        mSelectedMessageGroup = group
        mSelectedMsgId = null

        if (mSelectedMessageGroup == null) {
            with(mBinding) {
                detail.visibility = View.INVISIBLE
                detail.contentDescription = ""
            }
        } else {
            with(mBinding) {
                detail.visibility = View.VISIBLE
                detailLine1.text = getString(
                    R.string.selected_message_group_line1,
                    (group?.messages?.size ?: 0).toString()
                )
                detailLine2.text = getString(R.string.selected_message_group_line2)
            }

            mSelectedMessageGroup?.getCenter()?.let { center ->
                setCenter(center.second, center.first, MAX_ZOOM - 1)
            }

            // Animation
            if (mAnimPopupMsgShown == null) {
                mAnimPopupMsgShown = AnimationUtils.loadAnimation(this, R.anim.anim_msgpopup_enter)
                mAnimPopupMsgShown?.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {}
                    override fun onAnimationStart(animation: Animation?) {
                        mBinding.detail.visibility = View.VISIBLE
                    }
                })
            }
            mAnimPopupMsgShown?.reset()
            with(mBinding) {
                detail.startAnimation(mAnimPopupMsgShown)
                detail.contentDescription = getString(
                    R.string.acc_cd_markergroup_click,
                    (group?.messages?.size ?: 0).toString()
                )
                AccessibilityUtil.focus(detail)
            }
            cacheLastPosition()
        }
    }

    /**
     * Display short info about the selected message, also pans the map to the message location
     */
    private fun displaySelectedMessage(id: String?, zoomLvl: Int = -1) {

        mSelectedMsgId = id
        mSelectedMessageGroup = null
        if (id == null) {
            mBinding.detail.visibility = View.INVISIBLE
        } else {
            mMapHelper.highlightMarker(id)
            val db = MMDB.instance(this)
            val msg = db.getMessage(id)
            if (msg != null) {
                mBinding.detail.visibility = View.VISIBLE

                var line1 = msg.getDescriptionOnly()
                if (line1.isEmpty()) line1 = msg.category.name

                mBinding.detailLine1.text = line1
                mBinding.detailLine2.text = msg.state
                setCenter(msg.lat, msg.lon, zoomLvl)

                // Animation
                if (mAnimPopupMsgShown == null) {
                    mAnimPopupMsgShown = AnimationUtils.loadAnimation(this, R.anim.anim_msgpopup_enter)
                    mAnimPopupMsgShown?.setAnimationListener(object: Animation.AnimationListener {
                        override fun onAnimationRepeat(animation: Animation?) { }
                        override fun onAnimationEnd(animation: Animation?) { }
                        override fun onAnimationStart(animation: Animation?) {
                            mBinding.detail.visibility = View.VISIBLE
                        }
                    })
                }
                mAnimPopupMsgShown?.reset()
                with(mBinding) {
                    detail.startAnimation(mAnimPopupMsgShown)
                    detail.contentDescription = getString(R.string.acc_cd_overview_selected_marker,
                        msg.id, msg.state, msg.getDescriptionOnly())
                    AccessibilityUtil.focus(detail)
                }
            } else {
                with(mBinding) {
                    detail.contentDescription = ""
                    detail.visibility = View.INVISIBLE
                }
            }

            cacheLastPosition()
        }
    }

    /**
     * move the map's center to the given latitude-longitude and optional zoomLvl
     */
    private fun setCenter(lat: Double, lon: Double, zoomLvl: Int = -1) {
        // Check whether lateinit prop is already initialized
        if (!::mMapHelper.isInitialized) return

        if (zoomLvl == -1) mMapHelper.moveTo(lat, lon)
        else mMapHelper.moveTo(lat, lon, zoomLvl)

        cacheLastPosition()
    }

    /**
     * Enable user location. Also handles permission for location and external storage
     */
    private fun enableLocation(askForPerm: Boolean = true) {
        if (::mMapHelper.isInitialized) {
            if (ActivityUtil.isPermissionGranted(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                navigateToOwnLoc()
            } else {
                if (askForPerm && ::mMyLocPermRequest.isInitialized) {
                    mMyLocPermRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }
    }

    /**
     * Navigate to the user's current location, or the app's default location defined in settings.xml
     */
    private fun navigateToOwnLoc() {
        if (!::mMapHelper.isInitialized) return

        // Check if preference allows to track user's location
        val shouldTrack = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.mm_prefkey_enableownloc), true)

        if (shouldTrack) {
            // Turn on foreground location updates
            startLocationForegroundService();
        } else {
            getLastCachedPosition().let { pos ->
                if (pos.second != 0.0 && pos.first != 0.0) {
                    setCenter(pos.second, pos.first, 16)
                    mMapHelper.addMyLocationMarker(R.drawable.ic_marker_mylocation, pos.second, pos.first)
                    mSlidingMenu?.let {
                        it.currentLatitude = pos.second
                        it.currentLongitude = pos.first
                    }
                }
            }
        }
    }

    /**
     * Retrieve nearest messages from the given lat-lon. It chain-calls get_domain and get_nearest_message
     * or just the get_nearest_message if the app is configured to only display domain-specific messages
     *
     * @see de.maengelmelder.mainmodule.MMConstants.ShowDomainOnlyMessage
     */
    private fun retrieveMessage(lat: Double, lon: Double) {
        var canCall = false
        if (System.currentTimeMillis() - mCurrentMsgAPITs >= DELAY_MESSAGE_API_MS) {
            canCall = true
            mCurrentMsgAPITs = System.currentTimeMillis()
        }

        if (canCall) {
            showLoading()
            mLastMessagesList = arrayListOf()
            val staticDomainId = MMConstants.getStaticDomainId(this)
            if (staticDomainId > 0 || MMConstants.ShowDomainOnlyMessage) {
                // directly execute get_nearest_message with specified domainID
                if (staticDomainId > 0) mDefaultDomain.id = staticDomainId.toString()
                getNearestMessages(mDefaultDomain)
            } else {
                // use the systems-API to retrieve every messages from every system
                getNearestMessages(lat, lon)
            }
        }
    }

    /**
     * Refresh the map's tile source between satellite and street.
     */
    private fun refreshMapDisplay() {
        if (!this::mMapHelper.isInitialized) return
        mHandler.postDelayed({
            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            if (pref.getBoolean(getString(R.string.mm_prefkey_satasdefaulttile), false)) {
                mMapHelper.changeDisplayTo(IMapHelper.Display.SATELLITE)
            } else {
                mMapHelper.changeDisplayTo(IMapHelper.Display.STREET)
            }
        }, 500)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || mTutorialsStarted) return
        mTutorialsStarted = true

        // Delay slightly so Maps-SDK overlays and any startup UI settle before the
        // showcase captures view positions and starts its translucent Activity.
        // If a system dialog (e.g. location permission) steals focus during the delay,
        // we reset the flag so the showcase retries once focus returns.
        mBinding.root.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            if (!hasWindowFocus()) {
                // A system popup appeared while we were waiting – try again next focus gain.
                mTutorialsStarted = false
                return@postDelayed
            }
            ShowcaseBuilder.show(this, this, ViewTarget(mBinding.newMessage),
                getString(R.string.tut_new_msg_title), getString(R.string.tut_new_msg_content),
                true, TUT_NEW_MSG_PREFKEY, ShowcaseBuilder.ButtonPosition.BOTTOM_LEFT) {

                if (MMConstants.EnableMessageCreationFromQRCode) {
                    ShowcaseBuilder.show(this, this, ViewTarget(mBinding.scanQr),
                        getString(R.string.tut_qr_title), getString(R.string.tut_qr_content),
                        true, TUT_QRCODE_PREFKEY, ShowcaseBuilder.ButtonPosition.BOTTOM_LEFT) {

                        ShowcaseBuilder.show(this, this, ViewTarget(findViewById(R.id.toolbar)),
                            getString(R.string.tut_home_title), getString(R.string.tut_home_content),
                            true, TUT_HOME_PREFKEY, ShowcaseBuilder.ButtonPosition.BOTTOM_RIGHT)
                    }
                } else {
                    ShowcaseBuilder.show(this, this, ViewTarget(findViewById(R.id.toolbar)),
                        getString(R.string.tut_home_title), getString(R.string.tut_home_content),
                        true, TUT_HOME_PREFKEY, ShowcaseBuilder.ButtonPosition.BOTTOM_RIGHT)
                }
            }
        }, 600L)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        ShowcaseBuilder.handleActivityResult(requestCode, resultCode)
        try {
            when (requestCode) {
                // Returned from Login activity.
                LoginActivity.REQ_CODE -> {
                    val uc = UserData.getUserCred(this)
                    if (uc != null && uc.isUserValid() && ::mBinding.isInitialized) {
                        QuickPrompt.inform(
                            applicationContext,
                            mBinding.overviewLayout,
                            getString(R.string.info_login_success, uc.publicName)
                        )
                    }
                    mSlidingMenu?.refreshAccount(this)
                }

                // Returned from Profile Activity
                ProfileActivity.REQ_CODE -> {
                    if (resultCode == Activity.RESULT_OK && mSlidingMenu != null) {
                        mSlidingMenu?.refreshAccount(this)
                    }
                }

                // If GPS is turned on, try getting user location
                GPSAlertDialog.GPS_SETTINGS_REQ_CODE -> {
                    if (DeviceUtil.isGPSEnabled(this) && ::mMapHelper.isInitialized) {
                        mMapHelper.getMyLocation { lon, lat ->
                            if (lat != 0.0 && lon != 0.0) {
                                setCenter(lat, lon)
                                mMapHelper.addMyLocationMarker(
                                    R.drawable.ic_marker_mylocation,
                                    lat,
                                    lon
                                )
                            } else {
                                getLastCachedPosition().let { pos ->
                                    setCenter(pos.second, pos.first, 16)
                                    mMapHelper.addMyLocationMarker(
                                        R.drawable.ic_marker_mylocation,
                                        pos.second, pos.first
                                    )
                                }
                            }
                        }
                    }
                }

                // Returning from msg process act
                MyMessagesActivity.REQ_CODE, MessageDetailActivity.REQ_CODE, MessageProcessActivity.REQ_CODE -> {
                    getLastCachedPosition().let { pos -> setCenter(pos.second, pos.first, 16) }
                }

                // Returned from Settings activity
                SettingsActivity.REQ_CODE -> {
                    // toggle tracking based on value
                    refreshMapDisplay()
                    getLastCachedPosition().let { pos ->
                        if (pos.first != 0.0 && pos.second != 0.0) {
                            setCenter(pos.second, pos.first, 16)
                        }
                    }
                }

                // Returned from message list activity
                MessagesListActivity.REQ_CODE -> {
                    // Navigate to the chosen message
                    if (resultCode == Activity.RESULT_OK && ::mMapHelper.isInitialized) {
                        data?.let {
                            data.getStringExtra(BUNDLE_MSG_ID)?.let { msgid ->
                                val msg = MMDB.instance(this).getMessage(msgid, true)
                                msg?.let { m ->
                                    // We refresh the marker for it
                                    // It is possible that the marker is not drawn on the map
                                    val marker = ResourceProxy.getMarker(
                                        this,
                                        m.colorString,
                                        m.category.markerId,
                                        true
                                    )
                                    val descriptor = getString(R.string.acc_cd_marker_click, m.id)
                                    if (MMConstants.UseMarkerUri) {
                                        mMapHelper.addMarkerFromImageUrl(
                                            m.id,
                                            m.markerUrl,
                                            m.lat,
                                            m.lon,
                                            m,
                                            descriptor
                                        )
                                    } else {
                                        mMapHelper.addMarker(
                                            m.id,
                                            marker,
                                            m.lat,
                                            m.lon,
                                            m,
                                            descriptor
                                        )
                                    }

                                }
                                displaySelectedMessage(msgid, MAX_ZOOM)
                                mMapHelper.highlightMarker(msgid)
                            }
                        }
                    }
                }
            }

            // Automatically re-enable location tracking if permission allows.
            // but don't ask for permission if it is not allowed
            enableLocation(false)
        } catch (e: Exception) {
            // Handles kotlin.UninitializedPropertyAccessException ()
            e.printStackTrace()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        if (MMInitiator.hasMode("idea")) {
            MMInitiator.returnToOrigin(this, this)
        } else {
            // Close the sliding menu first
            if (mSlidingMenu?.isClosed == false) {
                mSlidingMenu?.close()
                return
            }

            // Click twice to exit
            if (!bClickTwiceToExit) {
                // Save the user's position
                cacheLastPosition()
                bClickTwiceToExit = true
                Toast.makeText(applicationContext, R.string.back_twice_to_exit, Toast.LENGTH_SHORT).show()
                mHandler.postDelayed({ bClickTwiceToExit = false }, DELAY_TWICE_PRESS_MS)
            } else {
                MMInitiator.returnToOrigin(this, this)
            }
        }
    }

    /**
     * Cache the last position of the map's center in preference
     */
    fun cacheLastPosition() {
        if (!::mMapHelper.isInitialized) return
        val center = mMapHelper.getCenter()
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putFloat(PREF_LAST_LAT, center.second.toFloat())
                .putFloat(PREF_LAST_LON, center.first.toFloat())
                .apply()

    }

    /**
     * Returns the last position cached by [cacheLastPosition]
     */
    private fun getLastCachedPosition(): Pair<Double, Double> {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val lastLat = pref.getFloat(PREF_LAST_LAT, 0.0f)
        val lastLon = pref.getFloat(PREF_LAST_LON, 0.0f)
        return Pair(lastLon.toDouble(), lastLat.toDouble())
    }

    override fun onResume() {
        super.onResume()
        Connectivity.enableConnectivityMonitor(this, mNetworkCb)
        mSlidingMenu?.refreshAccount(this)
    }

    override fun onPause() {
        super.onPause()
        Connectivity.disableConnectivityMonitor(this, mNetworkCb)
    }

    /**
     * Handles response from the API that retrieves the list of [Message]s
     */
    inner class NearestMessagesRequestListener(c: Context) : MMBMS.BMSListener<List<Message>, BaseResponse> {

        val context = c

        override fun onData(data: List<Message>) {
            mLastMessagesList = ArrayList(data)

            // Save the msg to DB in a separate thread
            mMessageDbMgmtThread?.stopThread()
            mMessageDbMgmtThread = DBMessageManagementThread(context, data)
            mMessageDbMgmtThread?.start()

            // Draw the message as markers on the map on UI thread
            runOnUiThread {
                mMarkerDrawingThread?.stopThread()
                mMarkerDrawingThread = MarkerDrawingThread(context, data)
                mMarkerDrawingThread?.run()
            }

            mBinding.statusbar.visibility = View.INVISIBLE
        }

        override fun onFail(err: BaseResponse) {
            mBinding.statusbar.visibility = View.INVISIBLE
        }
    }

    /**
     * Marker drawing thread. Also handles grouping
     */
    inner class MarkerDrawingThread(c: Context, arr: List<Message>) : Thread() {

        private val context = c
        private val mMsgs = arr
        private var bIsStopped = false

        override fun run() {
            if (bIsStopped) return

            // Clear all markers on map
            mMapHelper.clearMarkers()
            // mMapHelper.clear()

            // Setup properties
            val defaultDomId = MMConstants.DefaultDomainId.toString() // default domain Id
            val zoomLevel = mMapHelper.getZoomLevel() // current zoom level of the map
            val isZoomStopped = zoomLevel >= (MAX_ZOOM - 1) // If zoom level is high enough to warrant grouping

            // Compute distance threshold depending on bitmap size (It will only do them once)
            if (mMarkerGroupDistThreshold <= 0) {
                val baseBitmap = BitmapFactory.decodeResource(resources, R.drawable.white)
                mMarkerGroupDistThreshold = baseBitmap.height.coerceAtLeast(baseBitmap.width).toDouble()
                baseBitmap.recycle()
            }

            // Group messages to its own MessageGroup
            val messageGroups = arrayListOf<MessageGroup>().apply {
                addAll(mMsgs.filter { m ->
                    // Filter messages first through its domain id and app settings
                    val domainId = m.category.domainId
                    if (!MMConstants.ShowDomainOnlyMessage) true else domainId == defaultDomId
                }.map { m ->
                    // Map to its own group
                    MessageGroup(m).apply { isSmall = isZoomStopped }
                })
            }

            if (bIsStopped) return

            // List of dead groups (Ones that overlap other groups)
            val deadGroupsList = arrayListOf<MessageGroup>()
            var isDone = false

            // Keep concating marker groups to the nearest ones if they are close enough
            while (!isDone) {
                isDone = true
                for (i in 0 until messageGroups.size) {
                    if (bIsStopped) return
                    val group = messageGroups[i]
                    if (group.isAlive) {
                        for (j in (i+1) until messageGroups.size) {
                            if (bIsStopped) return
                            val group2 = messageGroups[j]
                            if (group2.isAlive && group.isOverlappingWith(group2, mMarkerGroupDistThreshold, mMapHelper)) {
                                group.concat(group2)
                                group2.isAlive = false
                                deadGroupsList.add(group2)
                                isDone = false
                            }
                        }
                    }
                }
                // Remove the dead groups
                messageGroups.removeAll(deadGroupsList)
                deadGroupsList.clear()
            }

            if (bIsStopped) return

            // Draw the groups
            messageGroups.forEach { group ->
                if (bIsStopped) return
                if (group.messages.size > 1) {
                    // Draw group
                    ResourceProxy.getGroupMarker(context, group) {
                        val center = group.getCenter()
                        val descriptor = getString(
                            R.string.acc_cd_markergroup_click,
                            group.messages.size.toString()
                        )
                        mMapHelper.addMarker(
                            group.groupId,
                            it,
                            center.second,
                            center.first,
                            group,
                            descriptor
                        )
                    }
                } else {
                    // Draw individual
                    val item = group.messages[0]
                    val marker = ResourceProxy.getMarker(context, item.colorString, item.category.markerId, mSelectedMsgId == item.id)
                    val descriptor = getString(R.string.acc_cd_marker_click, item.id)
                    if (MMConstants.UseMarkerUri) {
                        mMapHelper.addMarkerFromImageUrl(item.id, item.markerUrl, item.lat, item.lon, item, descriptor)
                    } else {
                        mMapHelper.addMarker(item.id, marker, item.lat, item.lon, item, descriptor)
                    }
                }
            }
        }

        fun stopThread() {
            bIsStopped = true
        }
    }

    inner class DBMessageManagementThread(c: Context, arr: List<Message>) : ObjectListOperationThread<Message>(arr) {

        private val db = MMDB.instance(c)
        private val defaultDomId = MMConstants.DefaultDomainId.toString()

        override fun forEveryObject(item: Message) {
            // Only put messages with correct domain ID if restricted
            if (!MMConstants.ShowDomainOnlyMessage || item.category.domainId == defaultDomId) {
                val catId = item.category.generateId()
                db.addCategory(catId,
                        Pair(db.constants.COL_ID, catId),
                        Pair(db.constants.COL_NAME, item.category.name),
                        Pair(db.constants.COL_TYPEID, item.category.typeId.toString()),
                        Pair(db.constants.COL_DOMAIN_ID, item.category.domainId),
                        Pair(db.constants.COL_MARKER_ID, item.category.markerId))
                db.addMessageFromServer(item)
            }
        }
    }

    inner class MessageUploadBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val msg = intent?.getStringExtra(APIUploadMessage.MESSAGE) ?: ""
            QuickPrompt.inform(applicationContext, mBinding.overviewLayout, msg)
        }
    }

    inner class LocationUpdateBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra(ForegroundLocationService.RESULT_LAT, 0.0)?: 0.0
            val lon = intent?.getDoubleExtra(ForegroundLocationService.RESULT_LON, 0.0)?: 0.0
            if (::mMapHelper.isInitialized) {
                mMapHelper.addMyLocationMarker(R.drawable.ic_marker_mylocation, lat, lon)
            }
            mSlidingMenu?.let {
                it.currentLatitude = lat
                it.currentLongitude = lon
            }
        }
    }

}