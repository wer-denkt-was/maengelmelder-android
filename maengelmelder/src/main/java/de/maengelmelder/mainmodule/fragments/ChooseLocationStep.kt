package de.maengelmelder.mainmodule.fragments

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.number.Scale
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.SupportMapFragment
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.activities.MessageProcessActivity
import de.maengelmelder.mainmodule.customviews.dialogs.LatLonInputDialog
import de.maengelmelder.mainmodule.customviews.dialogs.LoadingDialog
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1AddressSearch
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Domain
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.AddressSearchResult
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.utils.AccessibilityUtil
import de.maengelmelder.mainmodule.service.ForegroundLocationService
import de.maengelmelder.mainmodule.utils.ActivityUtil
import de.maengelmelder.mainmodule.utils.FontUtil
import de.maengelmelder.mainmodule.utils.GoogleMapHelper
import de.maengelmelder.mainmodule.utils.ResourceProxy
import de.maengelmelder.mainmodule.utils.interfaces.IMapHelper
import java.util.Timer
import java.util.TimerTask

/**
 *
 * This fragment enables user to pinpoint a location on the map for the bms message
 */
class ChooseLocationStep : BaseMessageStepFragment(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    val REQ_CODE_OWN_LOC_PERM = 1

    /**
     * @property mMapHelper map helper
     * @property mCurrentDisplay current map display, between Street and Satellite
     * @property mBtnMapDisplay button to change map source between street and satellite.
     * @property mBtnLockPosition button to update the position of the message to the map's center
     * @property mChkOfflineMap Checkbox to toggle offline map display
     * @property mBtnMyLoc button to pan the map to user's current location if available
     * @property mMapCenterLat value of the latitude of the center position of the map
     * @property mMapCenterLon value of the longitude of the center position of the map
     */
    private lateinit var mMapHelper: GoogleMapHelper
    private val mLocationUpdateReceiver = LocationUpdateBroadcastReceiver()
    private var mCurrentDisplay = IMapHelper.Display.STREET
    private var mLocText: TextView? = null
    private var mTextPosNotNeeded: TextView? = null
    private var mBtnMapDisplay: ImageView? = null
    private var mBtnManualLatLonEntry: ImageView? = null
    private var mBtnLockPosition: Button? = null
    private var mChkOfflineMap: CheckBox? = null
    private var mBtnMyLoc: ImageView? = null
    private var mCrosshair: ImageView? = null
    private var mCrosshairMarker: ImageView? = null
    private var mAddressSearch: EditText? = null
    private var mAddressSearchLayout: LinearLayout? = null
    private var mBtnAddressSearch: Button? = null
    private var bIsMapBeingMoved: Boolean = false

    private var bNewPosition = false

    override fun getLayoutId(): Int = R.layout.mm_fragment_chooselocation

    private lateinit var mMyLocPermRequest: ActivityResultLauncher<String>

    private var mBtnLockPosAnimTime: Timer? = null

    private val mCrosshairStartScaleAnimation: ScaleAnimation = ScaleAnimation(
        1f, 0.75f,
        1f, 0.75f,
        Animation.RELATIVE_TO_SELF, 0.5f,
        Animation.RELATIVE_TO_SELF, 0.5f
    ).apply {
        duration = 250
        fillAfter = true
    }
    private val mCrosshairEndScaleAnimation: ScaleAnimation = ScaleAnimation(
        0.75f, 1f,
        0.75f, 1f,
        Animation.RELATIVE_TO_SELF, 0.5f,
        Animation.RELATIVE_TO_SELF, 0.5f
    ).apply {
        duration = 250
        fillAfter = true
    }

    private val mCrosshairImgStartMoveAnim: TranslateAnimation = TranslateAnimation(
        0f, 0f, 0f, -70f
    ).apply {
        duration = 250
        fillAfter = true
    }

    private val mCrosshairImgEndMoveAnim: TranslateAnimation = TranslateAnimation(
        0f, 0f, -70f, 0f
    ).apply {
        duration = 250
        fillAfter = true
    }

    override fun onViewInflated(v: View?) {
        // For location permission
        mMyLocPermRequest = ActivityUtil.requestPermission(this) {
            if (it) launchLocationForegroundService()
        }

        // References to the view's widgets
        mBtnLockPosition = v?.findViewById(R.id.lock_pos)
        mBtnMapDisplay = v?.findViewById(R.id.switch_map_display)
        mBtnManualLatLonEntry = v?.findViewById(R.id.manual_latlon_entry)
        mChkOfflineMap = v?.findViewById(R.id.toggle_offlinemap)
        mBtnMyLoc = v?.findViewById(R.id.my_location)
        mCrosshair = v?.findViewById(R.id.crosshair)
        mCrosshairMarker = v?.findViewById(R.id.crosshair_marker)
        mLocText = v?.findViewById(R.id.location_text)
        mTextPosNotNeeded = v?.findViewById(R.id.warn_position_not_needed)
        mAddressSearch = v?.findViewById(R.id.address_search)
        mAddressSearchLayout = v?.findViewById(R.id.location_search_layout)
        mBtnAddressSearch = v?.findViewById(R.id.btn_address_search)

        FontUtil.applyCustomFont(mBtnLockPosition)
        FontUtil.applyCustomFont(mBtnAddressSearch)

        if (!MMConstants.EnableAddressSearch) {
            mAddressSearchLayout?.visibility = View.GONE
        } else {
            mAddressSearchLayout?.visibility = View.VISIBLE
            mAddressSearch?.setOnEditorActionListener { textview, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // Search address
                    getLocationFromAddressQuery(textview.text.toString())
                    true
                }
                false
            }
            mBtnAddressSearch?.setOnClickListener { _ ->
                // Search address
                getLocationFromAddressQuery(mAddressSearch?.text.toString())
            }
        }

        // Disable position change if forced
        mBtnLockPosition?.isEnabled = builder?.isLocationLocked() == false

        val map = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        map?.getMapAsync { gmap ->
            context?.let { ctx ->
                // Google map
                mMapHelper = GoogleMapHelper(ctx, gmap)

                // Pan listener
                // if location is hardfixed, don't add panning listener
                if (builder?.isLocationLocked() == false) {
                    mMapHelper.setMapPanListener(ctx, { _ ->
                        bIsMapBeingMoved = true
                        // When map is moved, clear the map of other markers
                        mMapHelper.clearMarkers()
                        mCrosshair?.startAnimation(mCrosshairStartScaleAnimation)
                        mCrosshairMarker?.startAnimation(mCrosshairImgStartMoveAnim)
                    }, { lon, lat ->
                        bIsMapBeingMoved = false
                        mCrosshair?.startAnimation(mCrosshairEndScaleAnimation)
                        mCrosshairMarker?.startAnimation(mCrosshairImgEndMoveAnim)
                        // When map stops being moved, we set the new position here
                        // Don't need to put marker
                        markLocation(lat, lon, true, false, false)
                    })
                }

                // Location permission
                activity?.let  { _ ->
                    val ownLocAllowed = ActivityUtil.isPermissionGranted(
                            ctx, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    if (ownLocAllowed) {
                        displayOwnLocation()
                    }
                }

                // Change default map view depending on the saved preference. Offline maps will override any default maps (including street and sat)
                val pref = PreferenceManager.getDefaultSharedPreferences(ctx)
                val satAsDef = pref.getBoolean(getString(R.string.mm_prefkey_satasdefaulttile), false)
                if (satAsDef) {
                    changeMapDisplay(IMapHelper.Display.SATELLITE)
                }

                // Display the location marker on the map
                builder?.let { b ->
                    if (b.isLocationValid()) {
                        val loc = b.getLocation()
                        markLocation(loc.second, loc.first, false, false, false)
                        mMapHelper.moveTo(loc.second, loc.first, 15)
                    } else {
                        // we move to the center of the map to help user view their city and select the location manually
                        val defLoc = MMConstants.DefaultLatLon
                        mMapHelper.moveTo(defLoc.second, defLoc.first)

                        // Animate the button to steal their attention
                        val bounceAnim = AnimationUtils.loadAnimation(ctx, R.anim.grow)
                        mBtnLockPosAnimTime = Timer()
                        mBtnLockPosAnimTime?.scheduleAtFixedRate(object: TimerTask() {
                            override fun run() {
                                mBtnLockPosition?.startAnimation(bounceAnim)
                            }
                        }, 0, 2000)
                    }
                }

                mMapHelper.loadGeoJSONLayers()
            }
        }

        // mBtnManualLatLonEntry?.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
        mBtnMapDisplay?.setOnClickListener(this)
        mBtnLockPosition?.setOnClickListener(this)
        mBtnMyLoc?.setOnClickListener(this)
        mBtnManualLatLonEntry?.setOnClickListener(this)
    }

    override fun onViewBroughtUp() {
        /**
         * Important to set this to false as we don't want user to keep repeating the creation procedure when they return
         * to this fragment, unless they select a new position
         *
         * Additionally, a fixed location from QR-Code request should not set this flag
         * because we assume that the location is correct
         */
        bNewPosition = false
        builder?.let { b ->
            if (MMConstants.CategoryBeforePosition && b.isCategoryValid() && b.category.posReq == Category.POS_NEVER) {
                // Show warning that position is not needed on the map
                mTextPosNotNeeded?.visibility = View.VISIBLE
                mBtnLockPosition?.visibility = View.GONE
                mBtnMapDisplay?.visibility = View.GONE
                mBtnMyLoc?.visibility = View.GONE
            } else {
                mTextPosNotNeeded?.visibility = View.GONE
                mBtnLockPosition?.visibility = View.VISIBLE
                mBtnMapDisplay?.visibility = View.VISIBLE
                mBtnMyLoc?.visibility = View.VISIBLE
                if (b.isLocationValid()) {
                    val loc = b.getLocation()
                    mMapHelper.moveTo(loc.second, loc.first)
                }
            }

            // Get marker image if category is already known
            if (b.isCategoryValid()) {
                context?.let { c ->
                    val marker = ResourceProxy.getMarker(c, "white", builder?.category?.markerId?: "0")
                    mCrosshairMarker?.setImageDrawable(marker)
                }
            }

            mBtnLockPosition?.isEnabled = builder?.isLocationLocked() == false

            // When the view is brought up, especially if the steps start with category,
            // We need to re-check the user's location against domain availability
            /*
            if (MMConstants.CategoryBeforePosition && b.isCategoryValid() && b.isLocationValid()) {
                context?.let { c ->
                    val loadingDialog = LoadingDialog(c, getString(R.string.dialog_loading_checkcategory))
                    loadingDialog.show()
                    val loc = b.getLocation()
                    checkLocationForDomainCoverage(c, b, loc.second, loc.first, loadingDialog)
                }
            }
            */
        }
    }

    override fun onClick(v: View?) {
        when(v) {
            // Switch display map between street and satellite
            mBtnMapDisplay -> changeMapDisplay(
                    if (mCurrentDisplay == IMapHelper.Display.STREET) IMapHelper.Display.SATELLITE
                    else IMapHelper.Display.STREET
            )

            // Mark the center of the map as defined location for the message
            /**
             * if [MMConstants.CategoryBeforePosition] is true,
             *  we don't need to refresh the category and attributes again
             */
            mBtnLockPosition -> {
                val center = mMapHelper.getCenter()
                markLocation(center.second, center.first, !MMConstants.CategoryBeforePosition, true, false)
            }

            // Move to the user's current location
            // Also handles permission
            mBtnMyLoc -> {
                context?.let { c ->
                    if (ActivityUtil.isPermissionGranted(c, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                        launchLocationForegroundService()
                    } else {
                        mMyLocPermRequest.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            }

            mBtnManualLatLonEntry -> {
                context?.let { c ->
                    LatLonInputDialog(c) { lat, lon ->
                        markLocation(lat, lon, true, false, false)
                    }.show()
                }
            }
        }
    }

    /**
     * Launch location foreground service
     */
    private fun launchLocationForegroundService() {
        context?.let { c ->
            val i = Intent(context, ForegroundLocationService::class.java)
            ContextCompat.startForegroundService(c, i)

            mMapHelper.getMyLocation { longitude, latitude ->
                if (latitude != 0.0 && longitude != 0.0) {
                    mMapHelper.moveTo(latitude, longitude, zoom = 16)
                }
            }
        }
    }

    private fun getLocationFromAddressQuery(q: String) {
        if (q.length <= 2) {
            // No query for input less than 3 characters
            return
        }
        context?.let { c ->
            mAddressSearch?.isEnabled = false
            var api: MMv1AddressSearch? = null
            val loadingDialog = LoadingDialog(c, getString(R.string.search_address_loading)) {
                it.dismiss()
                mAddressSearch?.isEnabled = true
                api?.cancel("Cancelled by user")
            }
            loadingDialog.show()

            val domid = MMConstants.DefaultDomainId
            api = MMv1AddressSearch(c, domid, q).apply {
                listener = object : MMBMS.BMSListener<AddressSearchResult?, BaseResponse> {
                    override fun onData(data: AddressSearchResult?) {
                        if (data == null || (data.latitude > -0.1 && data.latitude < 1.0) || (data.longitude > -0.1 && data.longitude < 1.0)) {
                            Toast.makeText(c, R.string.search_address_fail, Toast.LENGTH_LONG).show()
                        } else {
                            // successful location search
                            markLocation(data.latitude, data.longitude, true, false, false)
                            mMapHelper.moveTo(data.latitude, data.longitude)
                            Toast.makeText(c, getString(R.string.acc_announce_location_address_found, data.text), Toast.LENGTH_LONG).show()
                        }
                        mAddressSearch?.isEnabled = true
                        loadingDialog.dismiss()
                    }

                    override fun onFail(err: BaseResponse) {
                        mAddressSearch?.setText("")
                        mAddressSearch?.isEnabled = true
                        Toast.makeText(c, R.string.search_address_fail, Toast.LENGTH_LONG).show()
                        loadingDialog.dismiss()
                    }
                }
            }
            api.execute()
        }
    }

    /**
     * When the checkbox for offline map usage is selected
     */
    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        context?.run {
            // TODO offline map check if any
        }
    }

    /**
     * Mark the give lat-lon as the location for the message. This also includes putting a marker and set the value in the [MessageBuilder]
     *
     * @param lat latitude
     * @param lon longitude
     * @param buttonClick Pass true if this method is called manually by user interaction
     * @param isNewPos sets whether the marked location is a new position compared from the previous location, if available
     * @param putMarker if true, put marker
     */
    private fun markLocation(lat: Double, lon: Double,
                             isNewPos: Boolean = false,
                             buttonClick: Boolean = false,
                             putMarker: Boolean = true) {
        val forceLocation = (builder?.getAdditionalData("force_loc")?: "false").toBoolean()
        if (forceLocation) {
            return
        }

        builder?.let { b ->
            b.setLocation(lat, lon)
            mMapHelper.clearMarkers()
            if (putMarker) {
                ResourcesCompat.getDrawable(resources, R.drawable.mm_marker_new, null)?.let { d ->
                    mMapHelper.addMarker("location", d, lat, lon, null, null)
                }
            }
            bNewPosition = isNewPos
            b.isLocationChanged = true
            mBtnLockPosAnimTime?.cancel()

            context?.let { ctx ->
                AccessibilityUtil.announce(ctx, ctx.getString(R.string.acc_announce_locationselected))

                if ((MMConstants.CategoryBeforePosition || builder?.isCategoryLocked() == true)
                    && b.isCategoryValid()) {

                    val loadingDialog = LoadingDialog(ctx, getString(R.string.dialog_loading_checkcategory))
                    loadingDialog.show()
                    // If category is selected before position, we need to check if the position is allowed
                    // by matching the selected category's domainid with domains available in the position
                    checkLocationForDomainCoverage(ctx, b, lat, lon, loadingDialog)
                } else {
                    if (buttonClick) {
                        (activity as MessageProcessActivity).nextStep()
                    }
                }
            }
        }
    }

    private fun checkLocationForDomainCoverage(ctx: Context,
                                               b: MessageBuilder,
                                               lat: Double,
                                               lon: Double,
                                               loadingDialog: LoadingDialog? = null) {
        MMv1Domain(ctx, lat, lon, "short").apply {
            // We don't need the whole domain object. Just enough to check for the correct domain id
            checkOriginalSystemOnly = !MMConstants.ForceUseOverriddenAppId
            listener = (object: MMBMS.BMSListener<List<Domain>, BaseResponse> {
                override fun onData(data: List<Domain>) {
                    val domainFound = data.find { d -> d.id == b.category.domainId }
                    if (domainFound != null) {
                        mBtnLockPosAnimTime?.cancel()
                        // Is still inside area? allow it
                        // Move to the next step
                        b.setLocation(lat, lon)
                        (activity as MessageProcessActivity).nextStep()
                    } else {
                        // outside area? reset marker and location and show error
                        mMapHelper.clearMarkers()
                        b.setLocation(0.0, 0.0)
                        AlertDialog.Builder(ctx)
                                .setMessage(R.string.warn_domain_not_allowed)
                                .setPositiveButton(R.string.dialog_outsidearea_yes) { di, _ ->
                                    di.dismiss()
                                }
                                .show()
                    }
                    loadingDialog?.dismiss()
                }
                override fun onFail(err: BaseResponse) {
                    // Failed to obtain domain
                    if (MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.OfflineMode] == true) {
                        // If offline mode is active, we assign the position either way and move to the next step
                        // As if the request went through
                        b.setLocation(lat, lon)
                        (activity as MessageProcessActivity).nextStep()
                    }
                    loadingDialog?.dismiss()
                }
            })
        }.execute()
    }

    /**
     * Method to change map display between Street and Satellite
     *
     * @param disp instance of enum [IMapHelper.Display]
     */
    private fun changeMapDisplay(disp: IMapHelper.Display) {
        if (!this::mMapHelper.isInitialized) return
        mCurrentDisplay = disp
        mMapHelper.changeDisplayTo(mCurrentDisplay)

        when (mCurrentDisplay) {
            // If satellite, change to street
            IMapHelper.Display.STREET -> {
                mBtnMapDisplay?.setImageResource(R.drawable.ic_satellite_circular)
                mBtnMapDisplay?.contentDescription = getString(R.string.acc_cd_locationstep_changemap_satellite)
            }
            // Change to satellite
            IMapHelper.Display.SATELLITE -> {
                mBtnMapDisplay?.setImageResource(R.drawable.ic_street_circular)
                mBtnMapDisplay?.contentDescription = getString(R.string.acc_cd_locationstep_changemap_street)
            }
        }
    }

    // On this fragment, user is not able to swipe to move to the next fragment, so it does not interfere with the positioning
    override fun canBeSwipedInsideViewPager(): Boolean = false

    // Step is completed when the [MessageBuilder] has noted the chosen position
    override fun isStepComplete(): Boolean = builder?.isLocationValid() == true

    override fun onResume() {
        super.onResume()
        if (this::mMapHelper.isInitialized) mMapHelper.resume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(mLocationUpdateReceiver,
                    IntentFilter(ForegroundLocationService.BCAST_FILTER),
                AppCompatActivity.RECEIVER_EXPORTED
            )
        } else {
            requireActivity().registerReceiver(mLocationUpdateReceiver,
                IntentFilter(ForegroundLocationService.BCAST_FILTER),
            )
        }
    }

    override fun onPause() {
        if (this::mMapHelper.isInitialized) mMapHelper.pause()
        requireActivity().unregisterReceiver(mLocationUpdateReceiver)
        super.onPause()
    }

    /**
     * Method to display user's location. It also checks whether the settings allow it
     */
    private fun displayOwnLocation() {
        context?.let { c ->
            val allowed = PreferenceManager.getDefaultSharedPreferences(c)
                    .getBoolean(c.getString(R.string.mm_prefkey_enableownloc), true)
            if (allowed) {
                mMapHelper.getMyLocation { lon, lat ->
                    mMapHelper.addMyLocationMarker(R.drawable.ic_marker_mylocation, lat, lon)
                }
            }
        }
    }

    override fun isLoading(): Boolean = false

    override fun getTitle(): String = mContext?.getString(R.string.step_choose_location)?: ""

    override fun shouldPromptBeforeChange(): Boolean = false

    override fun executeBeforeChange() {
        // Save location to database
        builder?.let { b ->
            val loc = b.getLocation()
            context?.let { ctx ->
                val db = MMDB.instance(ctx)
                db.updateMessage(b.message.id,
                        db.constants.COL_LAT to loc.second,
                        db.constants.COL_LON to loc.first)
                /**
                 * If a new position is selected, the user should again choose a new category and fill in the information, since
                 * new position can result in different area of responsibility
                 *
                 * This is ignored if it's idea mode since category is always shown first before location
                 *
                 * This is also ignored if category is selected first before position (we assume the category is valid)
                 *
                 * With QR-Code feature, we have to check whether the category is forced or not
                 * if it's forced, don't remove the category. We assume it is correct since it is checked beforehand
                 */
                val categoryForced = builder?.isCategoryLocked() == true
                if (bNewPosition
                    && b.message.internalType != MessageProcessActivity.TYPE_IDEA
                    && !MMConstants.CategoryBeforePosition
                    && !categoryForced) {

                    bNewPosition = false
                    b.category = Category()
                    b.message.systemId = ""

                    db.updateMessage(b.message.id,
                            db.constants.COL_CAT_ID to "",
                            db.constants.COL_SYSTEM_ID to "")

                    // Clear the rubrik selection
                    (activity as MessageProcessActivity).clearRubricSelection()
                }
            }
        }

    }

    override fun promptBeforeChange(f: (Boolean) -> Unit) { }

    inner class LocationUpdateBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra(ForegroundLocationService.RESULT_LAT, 0.0)?: 0.0
            val lon = intent?.getDoubleExtra(ForegroundLocationService.RESULT_LON, 0.0)?: 0.0
            if (::mMapHelper.isInitialized) {
                mMapHelper.addMyLocationMarker(R.drawable.ic_marker_mylocation, lat, lon)
            }
        }
    }
}