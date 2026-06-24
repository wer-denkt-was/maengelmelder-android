package de.maengelmelder.mainmodule.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.preference.PreferenceManager
import android.text.Html
import android.text.InputType
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.maps.SupportMapFragment
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.activities.MessageProcessActivity
import de.maengelmelder.mainmodule.activities.OverviewActivity
import de.maengelmelder.mainmodule.customviews.attributes.*
import de.maengelmelder.mainmodule.customviews.dialogs.EmailSubDialog
import de.maengelmelder.mainmodule.customviews.dialogs.LoadingDialog
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Domain
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1System
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Attribute
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.service.MessageUploadService
import de.maengelmelder.mainmodule.utils.*
import de.maengelmelder.mainmodule.utils.ResourceProxy
import de.maengelmelder.mainmodule.utils.images.ImageManipulator
import de.maengelmelder.mainmodule.utils.images.ImageOrientatorCoroutine
import de.maengelmelder.mainmodule.utils.interfaces.IMapHelper

/**
 *
 * The fragment shows the overview of the created message including the chosen position, chosen category, and filled attributes
 */
class ReviewStep : BaseMessageStepFragment(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    /**
     * @property mPhoto imageview for holding the photo. Only the first photo is displayed
     * @property mMapHelper map helper for displaying selected lcoation
     * @property mCategoryText contains chosen category
     * @property mCategoryIcon contains chosen category marker icon
     * @property mErrText contains error text, if any
     * @property mTitle contains message's title
     * @property mDesc contains message's description
     * @property mChkOfflineMap checkbox for offline map usage. Activation depends on build config parameter
     * @property mBtnChangeLoc button to change location, will bring you back to [ChooseLocationStep]
     * @property mCategoryChange button to change category, will bring you back to [ChooseCategoryStep]
     * @property mAttributeEdit button to edit the content of the message, will bring you to [FillAttributesStep]
     * @property mBtnSave save the message content
     * @property mBtnNext continue to upload
     *
     */
    companion object {
        private val REQ_PERM_READ_STORAGE = 450
    }

    private var mPhoto: ImageView? = null
    private var mNumPhotoText: TextView? = null
    private var mMapHelper: GoogleMapHelper? = null
    private var mCategoryText: TextView? = null
    private var mCategoryIcon: ImageView? = null
    private var mErrText: TextView? = null
    private var mTitleSection: LinearLayout? = null
    private var mTitle: TextView? = null
    private var mDescSection: LinearLayout? = null
    private var mDesc: TextView? = null
    private var mAuthority: TextView? = null
    private var mTxtErrNoPhoto: TextView? = null
    private var mChkOfflineMap: CheckBox? = null
    private var mBtnChangeLoc: ImageButton? = null
    private var mAttribReview: LinearLayout? = null
    private var mMinimapLayout: RelativeLayout? = null

    private var mCategoryChange: ImageButton? = null
    private var mDescEdit: ImageButton? = null
    private var mAttributeEdit: ImageButton? = null

    private var mBtnSave: Button? = null
    private var mBtnNext: Button? = null

    private var bIsSendingMessage: Boolean = false

    /**
     * @property mMsgIncompleteDialog this dialog is used to warn user that the message is not complete and they will not be able
     *                                  to send this message before finishing
     * @property mSubMessage this dialog is used to prompt user whether they want to subscribe their message. This dialog only shows up
     *                          when the user did not fill any email address before
     */
    private var mSubMessage: EmailSubDialog? = null
    private var mMsgIncompleteDialog: AlertDialog? = null

    private lateinit var mExtStoragePermRequest: ActivityResultLauncher<String>

    override fun getLayoutId(): Int = R.layout.mm_fragment_review

    override fun onViewInflated(v: View?) {
        // View's references
        mPhoto = v?.findViewById(R.id.photo)
        mNumPhotoText = v?.findViewById(R.id.txt_num_photo)
        mErrText = v?.findViewById(R.id.err_map_or_photo)
        mTitleSection = v?.findViewById(R.id.title_box)
        mTitle = v?.findViewById(R.id.title)
        mDesc = v?.findViewById(R.id.description)
        mDescSection = v?.findViewById(R.id.description_box)
        mCategoryText = v?.findViewById(R.id.category)
        mCategoryIcon = v?.findViewById(R.id.catIcon)
        mCategoryChange = v?.findViewById(R.id.changeCat)
        mAttributeEdit = v?.findViewById(R.id.btnChangeAttr)
        mDescEdit = v?.findViewById(R.id.btnChangeDesc)
        mBtnNext = v?.findViewById(R.id.send_message_button)
        mBtnSave = v?.findViewById(R.id.save_message_button)
        mChkOfflineMap = v?.findViewById(R.id.toggle_offlinemap)
        mBtnChangeLoc = v?.findViewById(R.id.changeLoc)
        mAttribReview = v?.findViewById(R.id.attr_review)
        mMinimapLayout = v?.findViewById(R.id.minimap_layout)
        mAuthority = v?.findViewById(R.id.domain)
        mTxtErrNoPhoto = v?.findViewById(R.id.txt_err_photo_req)

        // Hide or show description based on app settings
        if (MMConstants.HideDescripton) {
            mDescSection?.visibility = View.GONE
            mDescEdit?.visibility = View.GONE
        } else {
            mDescSection?.visibility = View.VISIBLE
            mDescEdit?.visibility = View.VISIBLE
        }
        mDesc?.movementMethod = ScrollingMovementMethod()

        // permission request handler
        mExtStoragePermRequest = ActivityUtil.requestPermission(this) {
            if (it) displayImage()
        }

        // Setup the map
        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)?.let { map ->
            map.getMapAsync { gmap ->
                context?.let { ctx ->
                    // Disable panning since we don't want the user to interact with the map
                    mMapHelper = GoogleMapHelper(ctx, gmap).apply {
                        togglePanning(false)
                    }

                    // Change to satellite if needed
                    val pref = PreferenceManager.getDefaultSharedPreferences(ctx)
                    if (pref.getBoolean(getString(R.string.mm_prefkey_satasdefaulttile), false)) {
                        mMapHelper?.changeDisplayTo(IMapHelper.Display.SATELLITE)
                    }

                    // Move the center of the map to the coordinates set from  "ChoosePosition"
                    setMap()
                }
            }
        }

        // listeners
        mBtnChangeLoc?.setOnClickListener(this)
        mBtnNext?.setOnClickListener(this)
        mBtnSave?.setOnClickListener(this)
        mCategoryChange?.setOnClickListener(this)
        mAttributeEdit?.setOnClickListener(this)
        mDescEdit?.setOnClickListener(this)
        mPhoto?.setOnClickListener(this)
    }

    override fun onViewBroughtUp() {
        // Show updated title and desc
        builder?.let { b ->
            // Title
            if (b.category.hasTitle) {
                mTitleSection?.visibility = View.VISIBLE
                mTitle?.text = b.title
            } else {
                mTitleSection?.visibility = View.GONE
            }

            // Description
            if (MMConstants.HideDescripton) {
                mDescSection?.visibility = View.GONE
                mDescEdit?.visibility = View.GONE
            } else {
                mDescSection?.visibility = View.VISIBLE
                mDescEdit?.visibility = View.VISIBLE
                if (b.description.isEmpty()) {
                    mDesc?.error = ""
                    mDesc?.text = ""
                } else {
                    mDesc?.error = null
                    mDesc?.text = b.description
                }
            }

            // Lock buttons if needed
            mBtnChangeLoc?.visibility = if(builder?.isLocationLocked() == false) View.VISIBLE else View.GONE
            mCategoryChange?.visibility = if(builder?.isCategoryLocked() == false) View.VISIBLE else View.GONE

            // Move the map to the selected position
            setMap()

            // Show the picked category
            showCategory()

            // Show the first picked photo
            showPhoto()

            // Show the filled attributes
            showAttributes()
        }
    }

    private fun setMap() {
        // set map (visibility and moving to position)
        builder?.let { b ->
            if (b.isCategoryValid() && b.category.posReq == Category.POS_NEVER) {

                // Hide map
                mMinimapLayout?.visibility = View.GONE

            } else {
                mMinimapLayout?.visibility = View.VISIBLE
                if (b.isLocationValid()) {
                    val loc = b.getLocation()

                    // Show marker
                    context?.let { ctx ->
                        if (b.isCategoryValid()) {
                            mMapHelper?.addMarker(b.messageId,
                                ResourceProxy.getMarker(ctx, "white", b.category.markerId),
                                loc.second, loc.first, null, null)
                        }
                    }
                    mMapHelper?.moveTo(lat = loc.second, lon = loc.first, zoom = 16)
                }
            }
        }
    }

    private fun showCategory() {
        builder?.let { b ->
            // Show category
            if (b.isCategoryValid()) {
                mCategoryText?.let { txtCat ->
                    txtCat.text = b.category.name
                    txtCat.contentDescription = getString(R.string.acc_cd_summarystep_chosen_category, b.category.name)
                    txtCat.error = null
                    AccessibilityUtil.focus(txtCat)
                }

                // Show the marker for the category
                context?.let { ctx ->
                    mCategoryIcon?.setImageDrawable(ResourceProxy.getMarker(ctx, "white", b.category.markerId))
                    val db = MMDB.instance(ctx)
                    val attrs = db.getAttributesByCategoryId(b.category.generateId())
                    b.category.setAttribute(attrs)
                }
            } else {
                mCategoryIcon?.setImageResource(0)
                mCategoryText?.text = ""
                mCategoryText?.contentDescription = ""
                mCategoryText?.error = getString(R.string.missing_category)
            }
        }
    }

    private fun showPhoto() {
        builder?.let { b ->
            if (b.hasImage() && b.getNumOfImages() > 1) {
                mNumPhotoText?.visibility = View.VISIBLE
                mNumPhotoText?.text = getString(R.string.more_image_indicator, (b.getNumOfImages()-1).toString())
                mNumPhotoText?.contentDescription = getString(R.string.acc_cd_summarystep_multiple_foto, b.getNumOfImages().toString())
            } else {
                mNumPhotoText?.visibility = View.GONE
                if (!b.hasImage() && b.category.photoReq == Category.PHOTO_REQ) {
                    mTxtErrNoPhoto?.visibility = View.VISIBLE
                } else {
                    mTxtErrNoPhoto?.visibility = View.GONE
                }
            }

            if (!MMConstants.BypassImageReq) {
                // Check for image
                when (b.category.photoReq) {

                    Category.PHOTO_NEVER, Category.PHOTO_OPTIONAL -> {
                        mErrText?.visibility = View.GONE
                    }

                    Category.PHOTO_REQ -> {
                        mErrText?.text = getString(R.string.err_no_photo)
                        mErrText?.visibility = if (b.getNumOfImages() == 0) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun showAttributes() {
        context?.let { ctx ->
            // Authority
            mAuthority?.text = builder?.category?.domainText?: ""

            // Attributes
            mAttribReview?.removeAllViews()

            var contentDescriptor = ""

            // Needed to check for duplicated attribute ID
            val checkedAttributeIds = arrayListOf<String>()
            val attrIdsMessage = builder?.category?.attrIdsMessage?: arrayListOf()
            builder?.category?.iterateAttributes { attr ->
                val localid = attr.localId.toInt()
                if (attrIdsMessage.isEmpty() || attrIdsMessage.contains(localid)) {
                    if (!checkedAttributeIds.contains(attr.localId)) {
                        val view: BaseAttributeView<Any>? = when (attr.type) {
                            Attribute.TYPE_TEXT -> TextAttributeView(ctx, attr)
                            Attribute.TYPE_TEXTAREA -> TextAreaAttributeView(ctx, attr)
                            Attribute.TYPE_EMAIL -> TextAttributeView(ctx, attr).setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                            Attribute.TYPE_CHECKBOX -> CheckboxAttributeView(ctx, attr)
                            Attribute.TYPE_VALUELIST -> SpinnerAttributeView(ctx, attr)
                            else -> null
                        }?.also { view ->
                            view.setTitle(attr.name ?: "")
                            view.setHint(attr.helpText ?: "")
                        }

                        attr.localId.let { id ->
                            val prevValue = builder?.getAttributeValue(id)
                            if (prevValue != null) {
                                view?.setValue(prevValue)
                            }
                        }

                        // Build contentDescriptor for accessibility
                        if (view != null) {
                            contentDescriptor += attr.name
                            when (attr.type) {
                                Attribute.TYPE_CHECKBOX -> {
                                    val attrValue = view.getValue() as Boolean?
                                    val attrValueText = if (attrValue == true)
                                        getString(R.string.overwrite_yes)
                                    else getString(R.string.overwrite_no)
                                    contentDescriptor += ": $attrValueText"
                                }
                                else -> {
                                    contentDescriptor += ": " + view.getValue()?.toString()
                                }

                            }
                            contentDescriptor += "."

                            view.toggleReadOnly(true)
                            view.getInflatedView()?.let { v ->
                                v.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                                v.isFocusable = false
                                mAttribReview?.addView(v)
                            }
                        }
                        checkedAttributeIds.add(attr.localId)
                    }
                }
            }
            mAttribReview?.contentDescription = getString(R.string.acc_cd_summarystep_filledoutattributes, contentDescriptor)
        }
    }

    override fun onClick(v: View?) {
        when(v) {
            // upload the message. Also checks for completion
            mBtnNext -> {
                mBtnNext?.isEnabled = false
                context?.let { c ->
                    builder?.let { b ->
                        val loadingDialog = LoadingDialog(c, getString(R.string.dialog_loading_checkcategory))
                        loadingDialog.show()
                        DomainUtil.isMessageInCorrectDomain(c, b.message, b.isCategoryLocked()) {
                            // Log.d("ReviewStep", "isMessageinCorrectDomain: $it")
                            loadingDialog.dismiss()
                            if (it) {
                                showUploadConfirmDialog()
                            } else {
                                // Message cannot be uploaded to this domain
                                AlertDialog.Builder(c)
                                    .setMessage(R.string.warn_domain_not_allowed)
                                    .setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.dismiss() }
                                    .create()
                                    .show()
                            }
                            mBtnNext?.isEnabled = true
                        }
                    }
                }
            }

            // Save the message and end the current activity
            mBtnSave -> {
                activity?.let { a ->
                    Toast.makeText(a.applicationContext, R.string.save_message, Toast.LENGTH_LONG).show()
                    a.finish()
                }
            }

            mCategoryChange -> {
                // Go back to category step
                activity?.let { act ->
                    try {
                        (act as MessageProcessActivity).moveToByName(getString(R.string.step_choose_category))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            mDescEdit, mAttributeEdit -> {
                // Go back to form step
                activity?.let { act ->
                    try {
                        (act as MessageProcessActivity).moveToByName(getString(R.string.step_edit_attributes))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            mPhoto -> {
                // Go back to photo step
                activity?.let { act ->
                    try {
                        (act as MessageProcessActivity).moveToByName(getString(R.string.step_choose_photo))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            mCategoryChange -> {
                // Go back to category step
                activity?.let { act ->
                    try {
                        (act as MessageProcessActivity).moveToByName(getString(R.string.step_choose_category))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            mAttributeEdit -> {
                // Go back to form step
                activity?.let { act ->
                    try {
                        (act as MessageProcessActivity).moveToByName(getString(R.string.step_edit_attributes))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            mBtnChangeLoc -> {
                // Go back to location step
                activity?.let { act ->
                    try {
                        (act as MessageProcessActivity).moveToByName(getString(R.string.step_choose_location))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        context?.run {
            // TODO checkbox for offline map if any
        }
    }

    override fun isLoading(): Boolean = false

    /**
     * Shows a dialog which tells the user that the message is incomplete.
     */
    private fun showIncompleteMsgDialog(completeness: Array<Pair<MessageBuilder.STEP, Boolean>>?) {
        context?.let { ctx ->
            mMsgIncompleteDialog = AlertDialog.Builder(ctx)
                    .setMessage(R.string.warn_incomplete_msg)
                    .setPositiveButton(R.string.dialog_ok) {
                        dialog, _ -> dialog.dismiss()
                    }
                    .create()
            mMsgIncompleteDialog?.show()
        }
    }

    /**
     * Shows confirm dialog for user before uploading message
     */
    private fun showUploadConfirmDialog() {
        builder?.let { b ->
            context?.let { ctx ->
                val loadingDialog = LoadingDialog(ctx, getString(R.string.upload_message_progress))
                loadingDialog.show()

                val domain = MMDB.instance(ctx).getDomain(b.category.domainId)

                // Check domain difference if user is logged in
                /*
                var warnDiffDomain = ""
                UserData.getUserCred(ctx)?.let {
                    if (it.isUserValid() && it.domain != null) {
                        if (it.domain?.id != domain?.id) {
                            // TODO different domain id. Show warning text
                        }
                    }
                }
                */

                // If offline mode is turned on, we need to check if the category we selected is valid for the given domain
                // Since we have multiple external systems, we have to query the external system first before the domain
                // System -> Domain in external system (or internal system if no ext) -> Check domainid
                val messageLoc = b.getLocation()
                var canUpload = false
                var uploadMessage = ""

                // System API call
                MMv1System(ctx, messageLoc.second, messageLoc.first, true).apply {
                    listener = (object: MMBMS.BMSListener<List<SystemInfo>, BaseResponse> {
                        override fun onData(data: List<SystemInfo>) {
                            val extOnly = if (data.size == 1) data else data.filter { d -> d.isExternal }
                            // Domain API call using external system (or internal otherwise)
                            MMv1Domain(ctx, messageLoc.second, messageLoc.first).apply {
                                externalSystemInfo = extOnly[0]
                                listener = (object: MMBMS.BMSListener<List<Domain>, BaseResponse> {
                                    override fun onData(data: List<Domain>) {
                                        loadingDialog.dismiss()
                                        if (data.isEmpty()) {
                                            // No domain listed here (usually not possible since primary domain is 32 by default, but we need to account for possibility)
                                            uploadMessage = getString(R.string.message_upload_category_not_valid)
                                        } else {
                                            // Check the domain
                                            val domainFound = data.find { d -> d.id == b.message.category.domainId }
                                            if (domainFound == null) {
                                                // If the domain of the message category is not the same as the domain delivered from lat lon,
                                                // we know that the user has possibly selected a category that's not valid for this domain
                                                uploadMessage = getString(R.string.message_upload_category_not_valid)
                                            } else {
                                                // Domain and category match. Proceed to upload
                                                val domName = domain?.name?: "Mängelmelder.de"
                                                val confirmText = getString(R.string.dialog_confirmupload_text, domName)
                                                val bigsize = if (b.hasImage()) {
                                                    getString(R.string.dialog_confirmupload_bigsize, String.format("%.2f", b.getTotalPhotoSizeMB(ctx) / 2))
                                                } else ""
                                                uploadMessage = "$confirmText<br/><br/>$bigsize"
                                                canUpload = true
                                            }

                                            mBtnNext?.isEnabled = true
                                            doShowUploadDialog(canUpload, uploadMessage)
                                        }
                                    }
                                    override fun onFail(err: BaseResponse) {
                                        // Failed to obtain domain
                                        loadingDialog.dismiss()
                                        uploadMessage = getString(R.string.message_upload_fail_connect)
                                        doShowUploadDialog(canUpload, uploadMessage)
                                        mBtnNext?.isEnabled = true
                                    }
                                })
                            }.execute()
                        }
                        override fun onFail(err: BaseResponse) {
                            // Failed to obtain System info
                            loadingDialog.dismiss()
                            uploadMessage = getString(R.string.message_upload_fail_connect)
                            doShowUploadDialog(canUpload, uploadMessage)
                            mBtnNext?.isEnabled = true
                        }
                    })
                }.execute()
            }
        }
    }

    private fun doShowUploadDialog(canUpload: Boolean, text: String) {
        // TODO use [ResourceProxy.fromHTML] instead if possible
        val dialogMsg = if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(text)
        }

        context?.let { c ->
            var dialog = AlertDialog.Builder(c)
                .setMessage(dialogMsg)
                .setNegativeButton(R.string.dialog_cancel) { dialog, _ ->
                    dialog.dismiss()
                }
            if (canUpload) {
                dialog = dialog.setPositiveButton(R.string.dialog_upload) { d, _ ->
                    d.dismiss()
                    sendMessage(null)
                }
            }
            dialog.create().show()
        }
    }

    /**
     * Starts calling [MessageUploadService] to upload the message.
     */
    private fun sendMessage(subEmail: String? = null) {
        if (bIsSendingMessage) return
        bIsSendingMessage = true
        // Validate completeness of message
        val completeness = validateMsg()
        val isComplete = completeness?.fold(true) { init, value -> init && value.second }?: false
        val mb = builder
        if (mb == null) {
            bIsSendingMessage = false
            return
        }

        if (isComplete) {
            context?.let { ctx ->
                val db = MMDB.instance(ctx)
                val saved = db.getMessage(mb.messageId)

                Toast.makeText(ctx, R.string.upload_message_started, Toast.LENGTH_LONG).show()
                val i = Intent(ctx, MessageUploadService::class.java)
                // Required for Android 8
                // Ref: https://stackoverflow.com/questions/61289833/android-10-not-able-to-use-openfiledescriptor-inside-intentservice
                i.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                i.putExtra(MessageUploadService.KEY_SERVICE_TYPE, MessageUploadService.TYPE_MESSAGE)
                i.putExtra(MessageUploadService.KEY_MESSAGE, builder)
                i.putExtra(MessageUploadService.KEY_TIMESTAMP_CREATED, saved?.createdAt)
                i.putExtra(MessageUploadService.KEY_EMAIL_SUBSCRIPTION, subEmail)
                ctx.startService(i)
                // if it came from deeplink, we need to start Overview map activity instead of finishing the activity
                if ((activity as MessageProcessActivity).fromDeeplink()) {
                    startActivity(Intent(ctx, OverviewActivity::class.java))
                } else {
                    activity?.finish()
                }
                bIsSendingMessage = false
            }
        } else {
            showIncompleteMsgDialog(completeness)
            bIsSendingMessage = false
        }

        mBtnNext?.isEnabled = true
    }

    /**
     * Returns true if the message is complete
     */
    private fun validateMsg(): Array<Pair<MessageBuilder.STEP, Boolean>>? {
        return builder?.getStatus()
    }

    override fun onResume() {
        super.onResume()
        displayImage()
    }

    private fun displayImage() {
        context?.let { c ->
            builder?.let { b ->
                if (b.hasImage()) {
                    try {
                        val first = b.getImagePath(0)
                        // For displaying, just get it in small thumbnail (512px width)
                        ImageManipulator.getBitmapFromUriOrPath(c, first, 512) {
                            if (it != null) {
                                ImageOrientatorCoroutine(it, first).setListener(object : ImageOrientatorCoroutine.ExecutionListener {
                                    override fun afterOrientation(bmp: Bitmap?, iv: Any?) {
                                        bmp?.let { b -> mPhoto?.setImageBitmap(b) }
                                    }
                                    override fun beforeExecuted() {}
                                }).execute()
                            }
                        }
                    } catch (e: Exception) {
                        // Security or smth
                    }
                }
            }
        }
    }

    override fun isStepComplete(): Boolean = builder?.hasImage() == true && // Has photo
            builder?.isLocationValid() == true && // Location valid
            builder?.category?.isValid() == true && // category chosen
            (MMConstants.HideDescripton || builder?.description?.trim() != "") && // desc (after trimmed) not empty
            builder?.areAttributeValuesFilled() == true // Required attributes filled out


    override fun getTitle(): String = mContext?.getString(R.string.step_review)?: ""

    override fun shouldPromptBeforeChange(): Boolean = false

    override fun executeBeforeChange() { }

    override fun promptBeforeChange(f: (Boolean) -> Unit) { }
}