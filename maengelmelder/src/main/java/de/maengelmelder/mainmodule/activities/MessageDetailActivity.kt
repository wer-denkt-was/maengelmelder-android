package de.maengelmelder.mainmodule.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import android.text.Html
import android.text.Spanned
import android.text.SpannedString
import android.text.method.LinkMovementMethod
import android.util.Log
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.SupportMapFragment
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.adapters.MessageImageViewPagerAdapter
import de.maengelmelder.mainmodule.customviews.dialogs.EmailSubDialog
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.databinding.MmActivityMessageDetailBinding
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.coroutines.MMOkHttpClient
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1MessageDetail
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1SubscribeMessage
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.MessageSubsResponse
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.MessageDetail
import de.maengelmelder.mainmodule.objects.MessageHistory
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.utils.*
import de.maengelmelder.mainmodule.utils.MapCacheInfo
import de.maengelmelder.mainmodule.utils.ResourceProxy
import de.maengelmelder.mainmodule.utils.UserData
import de.maengelmelder.mainmodule.utils.images.ImageManipulator
import de.maengelmelder.mainmodule.utils.interfaces.IMapHelper
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import java.text.SimpleDateFormat
import java.util.*


/**
 * Shows detailed view of the message, strictly only the one retrieved from the server
 */
class MessageDetailActivity : AppCompatActivity(), MMBMS.BMSListener<MessageDetail?, BaseResponse>, View.OnClickListener {

    /**
     * @property BUNDLE_MSG_ID bundle key for message Id. When this activity is called, the intent should have value with this key
     * @property mDetail detail of the message, obtained from [MMv1MessageDetail]
     * @property mMessage The message itself, queried using the given message Id from bundle key [BUNDLE_MSG_ID]
     * @property mEmailSubDialog Email subscription dialog.
     * @property mMap map helper for the marker display
     * @property bIsDisplayingImage whether the activity is displaying the image in fullscreen
     */

    companion object {
        val BUNDLE_MSG_ID = "messagedetail.messageid"
        val USE_USER_DOMAIN = "messagedetail.use_user_domain"
        val BUNDLE_MSG = "messagedetail.message"
        val REQ_CODE = 1771
    }

    private var mDetail: MessageDetail? = null
    private var mMessage: Message? = null
    private var mSystemInfo: SystemInfo? = null
    private lateinit var mEmailSubDialog: EmailSubDialog
    private lateinit var mMap: GoogleMapHelper
    private var bUseUserDomain = false
    private var bIsDisplayingImage: Boolean = false
    private val mTz = TimeZone.getTimeZone("Europe/Berlin")
    private val mDtFormatter = SimpleDateFormat("dd.MMM.yyyy", Locale.getDefault()).apply {
        timeZone = mTz
    }
    private val mTimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
        timeZone = mTz
    }

    private lateinit var mImagesAdapter: MessageImageViewPagerAdapter
    private lateinit var mLaunchUpdateActivity: ActivityResultLauncher<Intent>
    private lateinit var mBinding: MmActivityMessageDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Content view and toolbar
        mBinding = MmActivityMessageDetailBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Result launcher
        mLaunchUpdateActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val margin = ResourceProxy.dpToPixel(resources, 48f)
                // Prompt
                QuickPrompt.inform(this,
                        mBinding.parentLayout,
                        getString(R.string.info_success_update_message_short),
                        margin
                )
            }
        }

        val userCred = UserData.getUserCred(this)

        val db = MMDB.instance(this)
        // Get the message id from bundle
        val msgId = intent.getStringExtra(BUNDLE_MSG_ID)
        if (msgId == null) {
            // get message itself from intent
            mMessage = ResourceProxy.getSerializeableExtra(intent, BUNDLE_MSG, Message::class.java)
            if (mMessage == null) {
                onBackPressedDispatcher.onBackPressed()
                return
            }
        } else {
            // Get the message from the database
            mMessage = db.getMessage(msgId)
            if (mMessage == null) {
                // Try if it is a server Id
                mMessage = db.getMessage(msgId, true)
                if (mMessage == null) {
                    onBackPressedDispatcher.onBackPressed()
                    return
                }
            }
        }

        bUseUserDomain = intent.getBooleanExtra(USE_USER_DOMAIN, false)

        val msgInternalType = mMessage?.internalType?: MessageProcessActivity.TYPE_DEFECT_REPORT
        if (msgInternalType == MessageProcessActivity.TYPE_IDEA) {
            supportActionBar?.title = getString(R.string.activity_idea)
            mBinding.mapPanel.visibility = View.GONE
        } else {
            // Setting up map
            val map = supportFragmentManager.findFragmentById(R.id.mapview) as SupportMapFragment?
            map?.getMapAsync {gmap ->
                mMap = GoogleMapHelper(this, gmap).apply {
                    togglePanning(false)
                }

                mMessage?.let { m ->
                    mMap.moveTo(m.lat, m.lon, 16)
                }

                val pref = PreferenceManager.getDefaultSharedPreferences(this)
                if (pref.getBoolean(getString(R.string.mm_prefkey_satasdefaulttile), false)) {
                    mMap.changeDisplayTo(IMapHelper.Display.SATELLITE)
                }

                mMap.loadGeoJSONLayers()
            }
        }

        // Listener
        with(mBinding) {
            removePreview.setOnClickListener(this@MessageDetailActivity)
            updateMessageButton.setOnClickListener(this@MessageDetailActivity)
            updateSubscribe.setOnClickListener(this@MessageDetailActivity)
            // Animation
            image.startAnimation(AnimationUtils.loadAnimation(this@MessageDetailActivity, R.anim.anim_enter_from_right))
        }

        val ctx = this@MessageDetailActivity
        mMessage?.run {

            // Get the system info
            mSystemInfo = if (bUseUserDomain && userCred != null && userCred.isUserValid()) {
                userCred.systemInfo
            } else {
                db.getSystem(systemId)
            }
            mBinding.details.removeAllViews()

            // Add message's description
            createCard(getString(R.string.p_detail_type), SpannedString(this.category.name))

            // Retrieve details
            // Don't attach user credential because we want them to be able to see other messages outside the their domain
            mBinding.loading.visibility = View.VISIBLE
            MMv1MessageDetail(ctx, this).apply {
                attachUserCred = false
                externalSystemInfo = mSystemInfo
                listener = this@MessageDetailActivity
            }.execute()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    /**
     * From [MMv1MessageDetail]
     */
    override fun onData(data: MessageDetail?) {
        runOnUiThread {
            mBinding.loading.visibility = View.GONE
            data?.let {d ->
                mDetail = d

                supportActionBar?.subtitle = "#${d.id} - ${d.domainName}"

                // Status
                createCard(getString(R.string.p_detail_status), SpannedString(d.state))
                // date
                d.createdAt?.let { date ->
                    createCard(getString(R.string.p_detail_date), SpannedString(mDtFormatter.format(date)))
                }
                // Desc
                if (d.description.isNotEmpty()) {
                    createCard(getString(R.string.p_detail_desc), SpannedString(d.description), 0)
                }

                // Address
                createCard(getString(R.string.p_detail_addr), SpannedString(d.address))
                mBinding.mapPanel.contentDescription = d.address

                // Fill out history
                createCard(getString(R.string.p_detail_history), d.details)

                // Show thumbnail image from server
                if (d.images.isNotEmpty()) {
                    mBinding.imageLayout.visibility = View.VISIBLE
                    val first = d.images[0]
                    if (first.thumbnailUri.isNotEmpty()) {
                        try {
                            ImageManipulator.setImage(this, mBinding.image, first.thumbnailUri, R.drawable.blank_image)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    with(mBinding) {
                        if (d.images.size > 1) {
                            // There are more images
                            txtNumPhoto.text = getString(
                                R.string.more_image_indicator,
                                (d.images.size - 1).toString()
                            )
                            txtNumPhoto.visibility = View.VISIBLE
                            txtNumPhoto.contentDescription = getString(
                                R.string.acc_cd_msgdetail_multiple_foto,
                                d.images.size.toString()
                            )
                        } else {
                            txtNumPhoto.visibility = View.GONE
                        }
                        image.setOnClickListener(this@MessageDetailActivity)
                    }
                } else {
                    mBinding.imageLayout.visibility = View.GONE
                }

                // Move to the marker
                if (this::mMap.isInitialized) {
                    mMap.moveTo(d.lat, d.lon, 16)
                    mMessage?.let { msg ->
                        if (MMConstants.UseMarkerUri) {
                            mMap.addMarkerFromImageUrl(msg.id, msg.markerUrl, d.lat, d.lon, null, null)
                        } else {
                            val drawable = ResourceProxy.getMarker(
                                this@MessageDetailActivity,
                                d.colorString, msg.category.markerId
                            )
                            mMap.addMarker(msg.id, drawable, d.lat, d.lon, null, null)
                        }
                    }
                }

                // Toggle comment button when needed
                with (mBinding) {
                    updateMessageButton.visibility = if (d.allowComment) View.VISIBLE else View.GONE
                    updateSubscribe.visibility = if (d.allowComment) View.VISIBLE else View.GONE
                }

                // Update the relevant info in the database if it's a user-defined message
                mMessage?.let { m ->
                    val db = MMDB.instance(this)
                    try {
                        db.updateMessage(m.id,
                                db.constants.COL_STATE to d.state,
                                db.constants.COL_STATE_EN to d.state_en,
                                db.constants.COL_COLOR to d.colorString)
                    } catch (e: Exception) {
                        // Failed to update due to whatever reason. Just ignore
                    }
                }
            }
        }

    }

    private fun createCard(title: String, content: Spanned, index: Int = -1) {
        val card = layoutInflater.inflate(R.layout.mm_layout_messagedetail_item, null)
        val titleTxt = card.findViewById<TextView>(R.id.title)
        val contentTxt = card.findViewById<TextView>(R.id.content)
        titleTxt.text = title
        contentTxt.text = content
        if (index < 0) mBinding.details.addView(card)
        else mBinding.details.addView(card, index)
    }

    private fun createCard(title: String, history: List<MessageHistory>?, index: Int = -1) {
        if (history == null || history.isEmpty()) {
            createCard(title, SpannedString("-"), index)
        } else {
            val card = layoutInflater.inflate(R.layout.mm_layout_messagedetail_item, null)
            card.findViewById<TextView>(R.id.content)?.apply { visibility = View.GONE }
            card.findViewById<TextView>(R.id.title)?.apply { text = title }
            val listedContent = card.findViewById<LinearLayout>(R.id.listed_content)

            history.forEach { h ->
                val historyLayout = layoutInflater.inflate(R.layout.mm_listitem_messagehistory, null)
                historyLayout.findViewById<TextView>(R.id.time)?.apply {
                    if (h.created != null) {
                        this.visibility = View.VISIBLE
                        val dateString = mDtFormatter.format(h.created)
                        val timeString = mTimeFormatter.format(h.created)
                        text = getString(R.string.p_detail_history_datetime, dateString, timeString)
                    } else {
                        this.visibility = View.GONE
                    }
                }
                historyLayout.findViewById<TextView>(R.id.manual_text)?.apply {
                    movementMethod = LinkMovementMethod.getInstance()
                    if (h.manualText.isNotEmpty()) {
                        val manualText = ResourceProxy.linkifyText(h.manualText)
                        visibility = View.VISIBLE
                        text = ResourceProxy.fromHTML(manualText)
                    } else {
                        visibility = View.GONE
                        text = ""
                    }
                }
                historyLayout.findViewById<TextView>(R.id.avatar_name)?.apply { text = h.ownerName }
                historyLayout.findViewById<TextView>(R.id.text)?.apply {
                    val historyText = ResourceProxy.linkifyText(h.text)
                    movementMethod = LinkMovementMethod.getInstance()
                    text = ResourceProxy.fromHTML(historyText)
                }
                historyLayout.contentDescription = getString(R.string.acc_cd_msgdetail_historyitem,
                        h.ownerName, mDtFormatter.format(h.created), h.text
                )
                listedContent.addView(historyLayout)
            }

            if (index < 0) mBinding.details.addView(card)
            else mBinding.details.addView(card, index)
        }
    }

    override fun onFail(err: BaseResponse) {
        runOnUiThread {
            mBinding.loading.visibility = View.GONE
            when (err.code) {
                403 -> {
                    QuickPrompt.inform(applicationContext, mBinding.parentLayout, getString(R.string.err_msgdetail_noperm))
                }
                404 -> {
                    QuickPrompt.inform(applicationContext, mBinding.parentLayout, getString(R.string.err_msgdetail_notfound))
                }
                else -> {
                    QuickPrompt.inform(applicationContext, mBinding.parentLayout, getString(R.string.err_msgdetail_fetch_aborted))
                }
            }
        }
    }

    override fun onClick(p0: View?) {
        when (p0) {
            // Show the image in fullscreen
            mBinding.image -> toggleBigImage()

            // Close the fullscreen mode
            mBinding.removePreview -> toggleBigImage(false)

            // Go to [UpdateMessageActivity]
            mBinding.updateMessageButton -> {
                val i = Intent(this, UpdateMessageActivity::class.java)
                i.putExtra(UpdateMessageActivity.BUNDLE_MSG_DETAIL, mDetail)
                i.putExtra(UpdateMessageActivity.BUNDLE_CAT_ID, mMessage?.category?.typeId?.toString()?: "")
                i.putExtra(UpdateMessageActivity.BUNDLE_SYSTEM, mSystemInfo)
                i.putExtra(UpdateMessageActivity.BUNDLE_MODE,
                        mMessage?.internalType?: MessageProcessActivity.TYPE_DEFECT_REPORT)
                mLaunchUpdateActivity.launch(i)
            }

            // Show subscription option
            mBinding.updateSubscribe -> {
                if (!::mEmailSubDialog.isInitialized) {
                    val uc = UserData.getUserCred(this)
                    mEmailSubDialog = EmailSubDialog(this, mOnEmailSub)
                    mEmailSubDialog.setCancelable(true)
                    mEmailSubDialog.setEmail(uc?.email?: "")
                    mEmailSubDialog.setDescription(getString(R.string.email_sub_explain_2))
                    mEmailSubDialog.forceEmailInput()
                }
                mEmailSubDialog.show()
            }
        }
    }

    private val mOnEmailSub: (Boolean, String?) -> Unit = { _, email ->
        // subscribe
        mMessage?.let { msg ->
            if (email != null) {
                mBinding.loading.visibility = View.VISIBLE
                val id = if (msg.serverId.isNotEmpty()) msg.serverId else msg.id
                mBinding.updateSubscribe.isEnabled = false

                MMv1SubscribeMessage(this, id, email, msg.category.domainId.toInt()).apply {
                    attachUserCred = false
                    externalSystemInfo = mSystemInfo
                    listener = (object: MMBMS.BMSListener<MessageSubsResponse, BaseResponse> {
                        override fun onData(data: MessageSubsResponse) {

                            with (mBinding) {
                                QuickPrompt.inform(
                                    applicationContext,
                                    parentLayout,
                                    getString(R.string.info_message_subscribed)
                                )
                                loading.visibility = View.GONE
                                updateSubscribe.isEnabled = true
                            }
                        }
                        override fun onFail(err: BaseResponse) {
                            val appCtx = applicationContext
                            with(mBinding) {
                                when (err.code) {
                                    409 -> QuickPrompt.inform(
                                        appCtx,
                                        parentLayout,
                                        getString(R.string.info_message_already_subs)
                                    )

                                    403 -> QuickPrompt.inform(
                                        appCtx,
                                        parentLayout,
                                        getString(R.string.info_message_cannot_subs)
                                    )

                                    404 -> QuickPrompt.inform(
                                        appCtx,
                                        parentLayout,
                                        getString(R.string.err_message_not_exists)
                                    )

                                    MMOkHttpClient.RESPSTATUS_CONNECTION_FAILED, MMOkHttpClient.RESPSTATUS_TIMEOUT -> {
                                        QuickPrompt.inform(
                                            appCtx,
                                            parentLayout,
                                            getString(R.string.err_no_conn)
                                        )
                                    }

                                    MMOkHttpClient.RESPSTATUS_IOEXC, MMOkHttpClient.RESPSTATUS_ABORTED -> {
                                        QuickPrompt.inform(
                                            appCtx,
                                            parentLayout,
                                            getString(R.string.err_server_error)
                                        )
                                    }
                                }
                                loading.visibility = View.GONE
                                updateSubscribe.isEnabled = true
                            }
                        }
                    })
                }.execute()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> onBackPressed()
        }

        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Close the fullscreen image first, before going back to the previous activity
        if (bIsDisplayingImage) {
            toggleBigImage(false)
        } else {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    /**
     * Toggles the fullscreen mode of the image.
     *
     * @param toggle true -> show image in fullscreen, false -> close the fullscreen mode
     *
     * @see com.jsibbold.zoomage.ZoomageView
     */
    private fun toggleBigImage(toggle: Boolean = true) {
        if (bIsDisplayingImage && toggle) return
        bIsDisplayingImage = toggle

        mBinding.bigimagelayout.visibility = if (toggle) View.VISIBLE else View.INVISIBLE

        if (toggle) {
            if (!this::mImagesAdapter.isInitialized) {
                mImagesAdapter = MessageImageViewPagerAdapter(this, mDetail?.images?: listOf())
                mBinding.viewpagerBigimage.adapter = mImagesAdapter
            }
        }
    }
}