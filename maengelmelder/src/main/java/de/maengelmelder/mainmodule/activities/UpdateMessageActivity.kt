package de.maengelmelder.mainmodule.activities

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.customviews.attributes.BaseAttributeView
import de.maengelmelder.mainmodule.customviews.attributes.CheckboxAttributeView
import de.maengelmelder.mainmodule.customviews.attributes.SpinnerAttributeView
import de.maengelmelder.mainmodule.customviews.attributes.TextAreaAttributeView
import de.maengelmelder.mainmodule.customviews.attributes.TextAttributeView
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.databinding.MmActivityUpdateMessageBinding
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Bms
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Category
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1UpdateMessage
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.network.responses.MessageUpdateResponse
import de.maengelmelder.mainmodule.objects.Attribute
import de.maengelmelder.mainmodule.objects.BmsDomain
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Log
import de.maengelmelder.mainmodule.objects.MessageDetail
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.utils.AccessibilityUtil
import de.maengelmelder.mainmodule.utils.ActivityUtil
import de.maengelmelder.mainmodule.utils.DeviceUtil
import de.maengelmelder.mainmodule.utils.ResourceProxy
import de.maengelmelder.mainmodule.utils.UserData
import de.maengelmelder.mainmodule.utils.images.ImageManipulator
import de.maengelmelder.mainmodule.utils.images.PhotoSelector
import de.maengelmelder.mainmodule.utils.notification.NotifUtility
import de.werdenktwas.modules.android.notificationutil.NotifWizard
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Form to submit comments to message
 */
class UpdateMessageActivity : AppCompatActivity(), View.OnClickListener,
        MMBMS.BMSListener<MessageUpdateResponse, MessageUpdateResponse>,
        DialogInterface.OnClickListener {

    companion object {
        val BUNDLE_MSG_DETAIL = "UpdateMessageActivity.msgDetail"
        val BUNDLE_SYSTEM = "UpdateMessageActivity.system"
        val BUNDLE_CAT_ID = "UpdateMessageActivity.category_id"
        val BUNDLE_MODE = "UpdateMessageActivity.mode"
    }

    private var mDB: MMDB? = null
    private var mCatId: String = ""
    private var mDetail: MessageDetail? = null
    private var mSysInfo: SystemInfo? = null
    private var mImgPath: String? = null
    private var bIsUploading: Boolean = false
    private var mDialogChoosePhoto: AlertDialog.Builder? = null

    private val mAttributeViews: ArrayList<BaseAttributeView<Any>> = arrayListOf()

    private var bShouldSaveLog = true
    private var mBmsDomain: BmsDomain? = null

    private var mNotifWiz: NotifWizard? = null
    private var mNotifBuilder: NotificationCompat.Builder? = null
    private var mNotifId: Int = 0

    // Request permissions
    private lateinit var mCameraPermRequest: ActivityResultLauncher<String>

    private lateinit var mBinding: MmActivityUpdateMessageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = MmActivityUpdateMessageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        setSupportActionBar(findViewById(R.id.toolbar))

        mDB = MMDB.instance(this)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        bShouldSaveLog = pref.getBoolean(getString(R.string.mm_prefkey_should_log), true)

        // perm request handlers
        mCameraPermRequest = ActivityUtil.requestPermission(this) {
            if (it) onClick(null, DialogInterface.BUTTON_POSITIVE) // Launch camera
        }

        mCatId = intent.getStringExtra(BUNDLE_CAT_ID)?: ""
        mDetail = ResourceProxy.getSerializeableExtra(intent, BUNDLE_MSG_DETAIL, MessageDetail::class.java)
        if (mDetail == null) {
            onBackPressedDispatcher.onBackPressed()
            return
        }
        mSysInfo = try {
            ResourceProxy.getSerializeableExtra(intent, BUNDLE_SYSTEM, SystemInfo::class.java)
        } catch (_: Exception) { null }

        val mode = intent.getStringExtra(BUNDLE_MODE)?: MessageProcessActivity.TYPE_DEFECT_REPORT
        if (mode == MessageProcessActivity.TYPE_IDEA) {
            supportActionBar?.title = getString(R.string.activity_idea_comment)
            supportActionBar?.subtitle = getString(R.string.idea_num, mDetail?.id?: -1)
        } else {
            supportActionBar?.subtitle = getString(R.string.message_num, mDetail?.id?: -1)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set old image
        if ((mDetail?.images?.size ?: 0) > 0) {
            ImageManipulator.setImage(this, mBinding.oldImage, mDetail?.images?.get(0)?.thumbnailUri?: "")
        }

        // Set listener
        with (mBinding) {
            mBinding.newImage.setOnClickListener(this@UpdateMessageActivity)
            mBinding.updateMessageButton.setOnClickListener(this@UpdateMessageActivity)
        }

        mDetail?.let { detail ->
            val domId = detail.domainId.toInt()
            // Grab settings
            MMv1Bms(this, domId).apply {
                externalSystemInfo = mSysInfo
                listener = (object: MMBMS.BMSListener<BmsDomain?, BaseResponse> {
                    override fun onData(data: BmsDomain?) {
                        mBmsDomain = data
                        data?.let { applyBmsDomainSettings(data) }
                    }
                    override fun onFail(err: BaseResponse) { }
                })
            }.execute()

            // Grab attributes
            if (mCatId.isNotEmpty()) {
                MMv1Category(this, domId, mCatId).apply {
                    externalSystemInfo = mSysInfo
                    listener = (object: MMBMS.BMSListener<Category?, BaseResponse> {
                        override fun onData(data: Category?) {
                            data?.getAttributeIdsForUpdate()?.let { attrIds ->
                                generateAttributeForms(data, attrIds)
                            }
                        }
                        override fun onFail(err: BaseResponse) {
                            // Fail for whatever reason. Just fail silently
                        }
                    })
                }.execute()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        return true
    }

    fun generateAttributeForms(category: Category, attrIds: List<Int>) {
        // generate the form.
        mAttributeViews.clear()
        mBinding.attribform.removeAllViews()
        category.iterateAttributes { attr ->
            val allow = attrIds.contains(attr.localId.toInt())
            if (allow) {
                val view: BaseAttributeView<Any>? = when (attr.type) {
                    Attribute.TYPE_TEXT -> TextAttributeView(this, attr).setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                    Attribute.TYPE_TEXTAREA -> TextAreaAttributeView(this, attr)
                    Attribute.TYPE_EMAIL -> TextAttributeView(this, attr).setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                    Attribute.TYPE_CHECKBOX -> CheckboxAttributeView(this, attr)
                    Attribute.TYPE_VALUELIST -> SpinnerAttributeView(this, attr)
                    else -> null
                }
                view?.run {
                    setTitle(attr.name ?: "")
                    setHint(attr.helpText ?: "")
                    mBinding.attribform.addView(getInflatedView())
                    mAttributeViews.add(this)
                }
            }
        }
    }

    fun applyBmsDomainSettings(bms: BmsDomain) {
        val textLimit = bms.getSetting("bmsTextLimit", 0)
        val textWarning = bms.getSetting("bmsLimitWarning", "")

        with (mBinding) {
            if (textLimit > 0) {
                txtTextLimit.text = "0 / $textLimit"
                updateText.filters = arrayOf(InputFilter.LengthFilter(textLimit))
                updateText.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        txtTextLimit.text = "${updateText.length()} / $textLimit"
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        if (updateText.length() > textLimit) {
                            // Limit reached
                            txtWarnTextLimit.text = textWarning
                        } else {
                            if (txtWarnTextLimit.text.isNotEmpty()) txtWarnTextLimit.text = ""
                        }
                    }
                })
                errorComment.visibility = View.VISIBLE
            } else {
                updateText.filters = arrayOf()
                errorComment.visibility = View.GONE
            }
        }
    }

    override fun onClick(v: View?) {
        if (bIsUploading) return
        when (v) {

            // Show dialog for user to choose between camera or gallery
            mBinding.newImage -> getChoosePhotoDialog().show()

            // Send updates to the message
            mBinding.updateMessageButton -> {
                DeviceUtil.hideSoftKeyboard(this@UpdateMessageActivity, currentFocus)

                // check attributes first
                var attrValid = true
                if (mAttributeViews.isNotEmpty()) {
                    mAttributeViews.forEach { av ->
                        if (!av.isAttributeFilledOut()) {
                            attrValid = false
                            av.toggleError(true, av.getAttrib()?.errorText ?: "")
                        } else {
                            av.toggleError(false)
                        }
                    }
                }

                val detail = mDetail
                if (attrValid && validate() && !bIsUploading && detail != null) {
                    bIsUploading = true
                    with (mBinding) {
                        updateMessageButton.isEnabled = false
                        loadingLayout.visibility = View.VISIBLE
                    }
                    val attrs = extractAttributeValues()

                    // Prepare notif
                    if (mNotifWiz == null) {
                        mNotifWiz =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                    NotifUtility.getWizard(this,
                                            getString(R.string.notif_channel_id),
                                            getString(R.string.app_name),
                                            "", NotificationManager.IMPORTANCE_LOW)
                                else NotifUtility.getWizardOld(
                                        this,
                                        getString(R.string.notif_channel_id))
                    }
                    mNotifBuilder = mNotifWiz?.getBuilder()
                            ?.setSmallIcon(R.drawable.upload_notif)
                            ?.setContentTitle(getString(R.string.comment_upload_start_title))
                    mNotifId = NotifUtility.getIncrementalNotifId(this)
                    mNotifBuilder?.let {
                        mNotifWiz?.notify(mNotifId, it.build())
                    }

                    var shouldAttachUserCred = false
                    val userCred = UserData.getUserCred(this)
                    if (userCred != null && userCred.isUserValid() && userCred.domain?.id == detail.domainId) {
                        // User is logged in and message belongs to the domain where the user is logged in
                        // you can attach user auth in message update
                        // Otherwise, assume it's anonymous
                        shouldAttachUserCred = true
                    }

                    // Call the API
                    MMv1UpdateMessage(this, detail.domainId.toInt(), detail.id?: "",
                            mBinding.updateText.text.toString(),
                            mImgPath,
                            mBinding.problemSolvedCheckbox.isChecked,
                            attrs
                            ).apply {
                        attachUserCred = shouldAttachUserCred
                        externalSystemInfo = mSysInfo
                        listener = (this@UpdateMessageActivity)
                    }.execute()
                }
            }
        }
    }

    private fun extractAttributeValues(): Map<String, Any?> {
        if (mAttributeViews.isEmpty()) return mapOf()
        val map = hashMapOf<String, Any?>()
        mAttributeViews.forEach { v ->
            v.getAttrib()?.let { attr ->
                map.put(attr.localId, v.getValue())
            }
        }
        return map
    }

    private fun getChoosePhotoDialog(): AlertDialog.Builder {
        return mDialogChoosePhoto?: AlertDialog.Builder(this)
                .setTitle(R.string.alertdialog_title_choose_photo)
                .setPositiveButton(R.string.camera, this)
                .setNegativeButton(R.string.gallery, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (RESULT_CANCELED == resultCode) {
            mImgPath = null
            return
        }

        var savedImage: Bitmap?
        if (requestCode == PhotoSelector.PHOTO_RESULT_CODE) {
            val inps = FileInputStream(mImgPath)
            savedImage = BitmapFactory.decodeStream(inps)
            inps.close()

            if (savedImage != null) {
                mBinding.newImage.setImageBitmap(ImageManipulator.getScaledBitmap(savedImage, 480))

                try {
                    ResourceProxy.saveImageToMediaStore(
                        this,
                        "updatephoto_${mDetail?.id}_${System.currentTimeMillis()}.jpg",
                        savedImage)
                } catch (e: IOException) {
                    // Cannot create uri, cannot save the image
                    e.printStackTrace()
                } catch (e: FileNotFoundException) {
                    // Image does not exist
                    e.printStackTrace()
                } catch (e: IllegalArgumentException) {
                    // Image cannot be decoded
                    e.printStackTrace()
                }
            }

        } else if (requestCode == PhotoSelector.GALLERY_RESULT_CODE) {
            if (data != null && data.data != null) {
                ImageManipulator.getScaledBitmap(this, data.data!!, 480) {
                    savedImage = it
                    if (savedImage != null) {
                        runOnUiThread { mBinding.newImage.setImageBitmap(it) }
                    }
                }
                mImgPath = data.data.toString()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onClick(di: DialogInterface?, btn: Int) {
        when (btn) {
            DialogInterface.BUTTON_POSITIVE -> {
                // Camera
                if (ActivityUtil.isPermissionGranted(this, Manifest.permission.CAMERA)) {
                    launchCamera()
                } else {
                    mCameraPermRequest.launch(Manifest.permission.CAMERA)
                }
            }
            DialogInterface.BUTTON_NEGATIVE -> {
                // Gallery
                launchGallery()
            }
        }
        di?.dismiss()
    }

    private fun launchCamera(outputFileName: String? = null, ext: String = "jpg") {
        val path = outputFileName?: "updatephoto_${mDetail?.id}"
        val file = ResourceProxy.getPhotoFilePath(this, path, ext)
        mImgPath = file.path
        PhotoSelector.startCamera(this, this, file)
    }

    private fun launchGallery() {
        PhotoSelector.startGallery2(this, getString(R.string.select_photo))
    }

    override fun onData(data: MessageUpdateResponse) {
        bIsUploading = false
        with (mBinding) {
            loadingLayout.visibility = View.INVISIBLE
            updateMessageButton.isEnabled = true
        }
        if (bShouldSaveLog) {
            mDB?.addLog(Log.TYPE_MSG_UPDATED, hashMapOf(
                    Log.KEY_REF_ID to (mDetail?.id ?: "-1"),
                    Log.KEY_TITLE to (data.respMessage)
            ))
        }

        // Announce for screenreader
        AccessibilityUtil.announce(this, getString(R.string.acc_announce_message_updated))

        // Notification
        mNotifBuilder?.setSmallIcon(R.drawable.upload_notif_success)
                ?.setColorized(true)
                ?.setColor(Color.GREEN)
                ?.setContentTitle(getString(R.string.comment_upload_completed_title))
                ?.setContentText(getString(R.string.info_success_update_message))
        mNotifBuilder?.let {
            mNotifWiz?.notify(mNotifId, it.build())
        }

        setResult(RESULT_OK)
        finish()
    }

    override fun onFail(err: MessageUpdateResponse) {
        bIsUploading = false
        with (mBinding) {
            loadingLayout.visibility = View.INVISIBLE
            updateMessageButton.isEnabled = true
        }
        var text = ""

        when (err.code) {
            1, -1 -> {
                text = getString(R.string.err_server_error)
            }
            -2 -> {
                text = getString(R.string.err_auth)
            }
            2 -> {
                text = getString(R.string.err_message_not_exists)
            }
            5 -> {
                text = getString(R.string.err_message_comment_disabled)
            }
            6, 7 -> {
                text = getString(R.string.message_upload_fail_reason_attribute)
            }
            8 -> {
                text = getString(R.string.message_upload_fail_reason_image)
            }

        }

        if (text.isNotEmpty()) {
            Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
            AccessibilityUtil.announce(this, getString(R.string.acc_announce_message_failed_to_update, text))
        }

        if (bShouldSaveLog) {
            mDB?.addLog(Log.TYPE_MSG_UPDATE_FAILED, hashMapOf(
                    Log.KEY_REF_ID to (mDetail?.id ?: "-1"),
                    Log.KEY_TITLE to err.msgId,
                    Log.KEY_REASON to err.respMessage
            ))
        }
    }

    private fun validate(): Boolean {
        if (mBinding.updateText.text.isEmpty()) {
            mBinding.updateText.error = getString(R.string.err_message_empty)
            return false
        }
        return true
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }
}