package de.maengelmelder.mainmodule.activities

import android.app.AlertDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.adapters.MyMessagesListAdapter
import de.maengelmelder.mainmodule.customviews.dialogs.LoadingDialog
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.databinding.MmActivityMyMessagesBinding
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.collectives.coroutines.APIMassMessageDetailUpdater
import de.maengelmelder.mainmodule.network.collectives.coroutines.APIUploadMessage
import de.maengelmelder.mainmodule.network.collectives.coroutines.IOCoroutine
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Domain
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1DomainMessages
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1System
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.service.MessageUploadService
import de.maengelmelder.mainmodule.service.receivers.BroadcastFilterList
import de.maengelmelder.mainmodule.utils.ActivityUtil
import de.maengelmelder.mainmodule.utils.DomainUtil
import de.maengelmelder.mainmodule.utils.UserData
import de.maengelmelder.mainmodule.utils.interfaces.OnMessageMenuItemClicked
import de.maengelmelder.mainmodule.utils.interfaces.OnMessageSelected
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.coroutines.CoroutineScope

/**
 * This activity contains a list with all messages that are created by the user
 */
class MyMessagesActivity : AppCompatActivity(),
        OnMessageSelected, AdapterView.OnItemClickListener, CompoundButton.OnCheckedChangeListener{

    companion object {
        const val REQ_CODE = 6712
        const val BUNDLE_MODE = "mymessages.mode"
    }

    /**
     * @property mMessages arraylist of the [MessageBuilder] containing all locally created messages
     * @property mListAdapter list adapter fot the [MessageBuilder]s
     * @property mStatusText a map containing the status text for incomplete part of the message (location, photo, etc.)
     * @property mWarnUpload dialog to confirm that the message will be uploaded
     * @property mMessageUploadComplete broadcast receiver for message uploading service.
     */
    private var mMessages = arrayListOf<MessageBuilder>()
    private var mListAdapter: MyMessagesListAdapter? = null
    private var mStatusText: HashMap<MessageBuilder.STEP, String>? = null

    private var mWarnUpload: Dialog? = null

    private var mMessageUploadComplete = MessageUploadBroadcastReceiver()
    private var mMassMessageDetailUpdater: APIMassMessageDetailUpdater? = null

    private var mListMode = MessageProcessActivity.TYPE_DEFECT_REPORT

    private lateinit var mAfterActivityRefreshMessages: ActivityResultLauncher<Intent>

    private lateinit var mBinding: MmActivityMyMessagesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = MmActivityMyMessagesBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mListMode = intent.getStringExtra(BUNDLE_MODE)?: MessageProcessActivity.TYPE_DEFECT_REPORT

        // Set up the status text for incomplete message
        mStatusText = hashMapOf(
                MessageBuilder.STEP.PHOTO to getString(R.string.missing_photo),
                MessageBuilder.STEP.CATEGORY to getString(R.string.missing_category),
                MessageBuilder.STEP.LOCATION to getString(R.string.missing_location),
                MessageBuilder.STEP.ATTRIBUTE to getString(R.string.missing_attribute)
        )

        // Listener
        with(mBinding) {
            list.onItemClickListener = this@MyMessagesActivity

            rbUploadedMessages.setOnCheckedChangeListener(this@MyMessagesActivity)
            rbNotUploadedMessages.setOnCheckedChangeListener(this@MyMessagesActivity)
        }
        // Activity result launcher
        mAfterActivityRefreshMessages = ActivityUtil.startActivityForResult(this) {
            RetrieveMyMessages().execute()
        }

        // Swipe down to refresh
        mBinding.listSwipeRefresh.setOnRefreshListener {
            RetrieveMyMessages(true).execute()
        }

        // Load the messages with a thread. Since it also selects categories and attributes from the database,
        // it may take quite a long time to have them in UI thread
        // It also fetches the latest update on the message from server
        RetrieveMyMessages(true).execute()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_mymessages, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()

            R.id.menu_update_statuses -> {
                with(mBinding) {
                    loadingMessageUpdate.visibility = View.VISIBLE
                    textMessageUpdate.visibility = View.VISIBLE
                }
                RetrieveMyMessages(true).execute()
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        // accepts broadcast from message upload service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mMessageUploadComplete, IntentFilter(BroadcastFilterList.MESSAGE_UPLOAD),
                RECEIVER_EXPORTED)
        } else {
            registerReceiver(mMessageUploadComplete, IntentFilter(BroadcastFilterList.MESSAGE_UPLOAD))
        }
    }

    override fun onPause() {
        unregisterReceiver(mMessageUploadComplete)
        super.onPause()
    }

    /**
     * Start loading the message to [MessageProcessActivity] for further edits
     */
    override fun onStartEdit(m: Message, v: View?) {
        val intent = Intent(this, MessageProcessActivity::class.java)
        intent.putExtra(MessageProcessActivity.BUNDLE_MESSAGE_ID, m.id)
        intent.putExtra(MessageProcessActivity.JUMP_TO_STEP, getString(R.string.step_choose_category))
        mAfterActivityRefreshMessages.launch(intent)
    }

    /**
     * Calls [removeMessage]. This method handles the animation part
     *
     * @param m Message
     * @param v the list item's view. Obtained from the view parameter in [android.widget.AdapterView.OnItemClickListener.onItemClick]. This
     * view is needed for the animation
     */
    override fun onRemove(m: Message, v: View?) {
        // If the view is null, just remove the message without animating
        if (v == null) {
            mListAdapter?.let { removeMessage(mBinding.list, it, m) }
            return
        }

        // Setup remove animation from left to right
        val removeAnim = AnimationUtils.loadAnimation(this, R.anim.anim_listitem_removed)
        removeAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation?) {
                mListAdapter?.let { removeMessage(mBinding.list, it, m) }
            }
            override fun onAnimationRepeat(animation: Animation?) { }
            override fun onAnimationStart(animation: Animation?) { }
        })
        v.startAnimation(removeAnim)
    }

    /**
     * When the filter is changed (the radio button for filtering uploaded and not-uploaded messages)
     */
    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        RetrieveMyMessages(false).execute()
    }

    /**
     * Removes a [Message] from the list and database
     */
    private fun removeMessage(list: ListView, adapter: MyMessagesListAdapter, m: Message) {
        // Delete from DB
        val db = MMDB.instance(this@MyMessagesActivity)
        db.deleteMessage(m.id)

        // Filter from current set
        val filtered = mMessages.filter { mb -> mb.messageId != m.id }
        mMessages = ArrayList(filtered)

        // Remove from list
        list.post { adapter.remove(m.id) }

        // LOG: logged when the message is removed
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val shouldSaveLog = pref.getBoolean(getString(R.string.mm_prefkey_should_log), true)
        if (shouldSaveLog) {
            db.addLog(de.maengelmelder.mainmodule.objects.Log.TYPE_MSG_REMOVED, hashMapOf(
                    de.maengelmelder.mainmodule.objects.Log.KEY_MSG_ID to m.id,
                    de.maengelmelder.mainmodule.objects.Log.KEY_TITLE to (m.title)
            ))
        }
    }

    /**
     * Show confirmation dialog to the user that the selected message will be uploaded
     */
    override fun onUpload(m: Message, v: View?) {
        mBinding.noSavedMessages.visibility = View.GONE
        val loadingDialog = LoadingDialog(this, getString(R.string.dialog_loading_checkcategory))
        loadingDialog.show()
        DomainUtil.isMessageInCorrectDomain(this, m, m.isCategoryLocked()) {
            loadingDialog.dismiss()
            if (it) {
                constructWarnDialog(m)
            } else {
                // Message cannot be uploaded to this domain
                AlertDialog.Builder(this)
                    .setMessage(R.string.warn_domain_not_allowed)
                    .setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
            }
        }
    }

    /**
     * Calls the [MessageUploadService] to upload the images. This option only comes out if the message is uploaded, but the images
     * fail to upload ([Message.uploadStatus] = [de.maengelmelder.mainmodule.database.MMDBConstants.STATUS_IMAGE_UPLOAD_FAIL]). Also happens
     * only to message with more than 1 image
     */
    override fun onUploadImages(m: Message, v: View?) {
        with (mBinding) {
            rbUploadedMessages.isEnabled = false
            rbNotUploadedMessages.isEnabled = false
        }
        val uploadServ = Intent(this, MessageUploadService::class.java)
        uploadServ.putExtra(MessageUploadService.KEY_SERVICE_TYPE, MessageUploadService.TYPE_MASS_UPDATE_IMAGES)
        uploadServ.putExtra(MessageUploadService.KEY_MESSAGE, MessageBuilder(m))
        startService(uploadServ)
    }

    /**
     * Opens up [MessageDetailActivity] with the given message Id. Note that this option can only come out if the selected message
     * is already successfully uploaded to the server
     *
     * It will use the system where user is logged in if user is logged in
     */
    override fun onDetail(m: Message, v: View?) {
        val userCred = UserData.getUserCred(this)
        val i = Intent(this, MessageDetailActivity::class.java)
        i.putExtra(MessageDetailActivity.BUNDLE_MSG, m)
        i.putExtra(MessageDetailActivity.USE_USER_DOMAIN, userCred != null && userCred.isUserValid())
        startActivity(i)
        // For some reason mAfterActivityRefreshMessages.launch(intent) doesn't work
        // It returns back to calling activity without any error
    }

    /**
     * Shows the popup message containing the menu options related to the selected message's status
     */
    override fun onItemClick(adapter: AdapterView<*>?, v: View?, row: Int, id: Long) {
        if (v == null) return
        val msg = adapter?.getItemAtPosition(row) as MessageBuilder
        mListAdapter?.getPopup(msg.message, v, OnMessageMenuItemClicked(v, msg.message, this))?.show()
    }

    /**
     * Shows warning dialog for a particular message. It also shows the missing parts of the message that need to be filled for uploading
     */
    private fun constructWarnDialog(m: Message) {
        // Get the saved attributes
        val mb = MessageBuilder(m)
        val db = MMDB.instance(this)
        mb.attributeValuesFromJson(db.getExtrasJSON(m.id))

        // Get the statuses
        val statuses = mb.getStatus()
        // Photos are optional, so skipped from check
        val completed = statuses.fold(true) { init, value -> init && value.second }

        var warnText = ""
        var warnSubText = ""
        if (!completed) {
            // Message is not complete
            warnText = getString(R.string.warn_message_incomplete)
            statuses.forEach { st ->
                warnSubText += "\n- ${mStatusText?.get(st.first)}"
            }
            doConstructWarnDialog(m, canUpload = false, warnText, warnSubText)
        } else {

            val loadingDialog = LoadingDialog(this, getString(R.string.upload_message_progress))
            loadingDialog.show()
            // Check whether message's category is valid for the given domain based on the lat-lon
            val messageLoc = mb.getLocation()
            var canUpload = false

            // Check system first before querying the domains (due to external systems)
            MMv1System(this, messageLoc.second, messageLoc.first, true).apply {
                listener = (object: MMBMS.BMSListener<List<SystemInfo>, BaseResponse> {
                    override fun onData(data: List<SystemInfo>) {
                        val extOnly = if (data.size == 1) data else data.filter { d -> d.isExternal }
                        MMv1Domain(this@MyMessagesActivity, messageLoc.second, messageLoc.first).apply {
                            externalSystemInfo = extOnly[0]
                            listener = (object: MMBMS.BMSListener<List<Domain>, BaseResponse> {
                                override fun onData(data: List<Domain>) {
                                    loadingDialog.dismiss()
                                    if (data.isEmpty()) {
                                        // No domain listed here (usually not possible since primary domain is 32 by default, but we need to account for possibility)
                                        warnText = getString(R.string.message_upload_category_not_valid)
                                    } else {
                                        // Check the domain
                                        val domainFound = data.find { d -> d.id == m.category.domainId }
                                        if (domainFound == null) {
                                            // If the domain of the message category is not the same as the domain delivered from lat lon,
                                            // we know that the user has possibly selected a category that's not valid for this domain
                                            warnText = getString(R.string.message_upload_category_not_valid)
                                        } else {
                                            // Domain and category match. Proceed with upload
                                            val domain = db.getDomain(m.category.domainId)
                                            val domainName = domain?.name?: "Mängelmelder.de"
                                            warnText = getString(R.string.dialog_confirmupload_text, domainName)
                                            canUpload = true
                                        }

                                        doConstructWarnDialog(m, canUpload, warnText)
                                    }
                                }
                                override fun onFail(err: BaseResponse) {
                                    loadingDialog.dismiss()
                                    // Failed to obtain domain
                                    warnText = getString(R.string.message_upload_fail_connect)
                                    doConstructWarnDialog(m, canUpload = false, warnText)
                                }
                            })
                        }.execute()
                    }

                    override fun onFail(err: BaseResponse) {
                        loadingDialog.dismiss()
                        // Failed to obtain system info
                        warnText = getString(R.string.message_upload_fail_connect)
                        doConstructWarnDialog(m, canUpload = false, warnText)
                    }
                })
            }.execute()

        }
    }

    private fun doConstructWarnDialog(m: Message, canUpload: Boolean, text: String, subText: String = "") {
        // Create the dialog
        // mWarnUpload = Dialog(this, android.R.style.Theme_Dialog)
        // mWarnUpload?.setContentView(R.layout.mm_dialog_warn_upload)

        val dialogMsg = if (Build.VERSION.SDK_INT >= 24) {
            Html.fromHtml(text+subText, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(text+subText)
        }

        var dialog = AlertDialog.Builder(this)
            .setTitle(R.string.warn_message_upload_title)
            .setMessage(dialogMsg)
            .setCancelable(true)
            .setNegativeButton(R.string.dialog_cancel) { dialog, _ ->
                dialog.dismiss()
            }

        if (canUpload) {
            // Confirm the user to start uploading the message
            dialog = dialog.setPositiveButton(R.string.dialog_upload) { d, _ ->
                d.dismiss()
                uploadMessage(m)
            }
        } else {
            // Either the message is not complete or category is not valid
            dialog = dialog.setPositiveButton(R.string.dialog_edit) { d, _ ->
                d.dismiss()
                onStartEdit(m, null)
            }
        }

        // Shows the dialog
        dialog.show()
    }

    /**
     * Upload the message by calling [MessageUploadService]. This option is only shown when the message is ready to be uploaded and
     * all parts of the message have been completed
     */
    private fun uploadMessage(m: Message) {
        // Disable checkboxes for filtering
        with (mBinding) {
            rbNotUploadedMessages.isEnabled = false
            rbUploadedMessages.isEnabled = false
            noSavedMessages.visibility = View.GONE
        }
        // Create the upload service
        val uploadServ = Intent(this, MessageUploadService::class.java)
        // Required for Android 8
        // Ref: https://stackoverflow.com/questions/61289833/android-10-not-able-to-use-openfiledescriptor-inside-intentservice
        uploadServ.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        // Uploading message
        uploadServ.putExtra(MessageUploadService.KEY_SERVICE_TYPE, MessageUploadService.TYPE_MESSAGE)
        // The message itself
        uploadServ.putExtra(MessageUploadService.KEY_MESSAGE, MessageBuilder(m))
        // Timestamp of when the message is being sent to the server
        uploadServ.putExtra(MessageUploadService.KEY_TIMESTAMP_CREATED, m.createdAt)
        // Start the service
        startService(uploadServ)
    }

    /**
     * Load the messages retrieved from the local database to the list, while also applying the filter
     */
    private fun showMessages(msgs: ArrayList<MessageBuilder>) {
        // Setup the adapter
        if (mListAdapter == null) {
            mListAdapter = MyMessagesListAdapter(this, msgs)
            mListAdapter?.setContextMenuListener(this)
            mBinding.list.adapter = mListAdapter
        } else {
            mListAdapter?.setMessages(msgs)
        }

        // Inform user when there's no message to be displayed using the given filter
        with (mBinding) {
            if (msgs.isEmpty()) {
                noSavedMessages.visibility = View.VISIBLE
                noSavedMessages.text = getString(R.string.info_no_saved_messages)
                list.visibility = View.GONE
            } else {
                noSavedMessages.visibility = View.GONE
                list.visibility = View.VISIBLE
            }
        }
    }

    inner class RetrieveMyMessages(fetchUpdateFromServer: Boolean = false) :
            IOCoroutine<ArrayList<MessageBuilder>>(this@MyMessagesActivity, listOf()) {

        private val mCtx = this@MyMessagesActivity
        private val mDB = MMDB.instance(mCtx)
        private val mTxtLoadingMsgs = mCtx.getString(R.string.status_loading_local_messages)
        private val mTxtNoMsgs = mCtx.getString(R.string.info_no_saved_messages)
        private val bFetchUpdateFromServer = fetchUpdateFromServer
        private val userData = UserData.getUserCred(mCtx)

        private fun getLocallySavedMessages(): ArrayList<MessageBuilder> {
            val mbs = arrayListOf<MessageBuilder>()
            val msgs = mDB.getMessages(
                mDB.constants.COL_ORIGIN to mDB.constants.ORIGIN_SELF,
                mDB.constants.COL_INTERNAL_TYPE to mListMode)
            msgs.forEach { m -> mbs.add(MessageBuilder(m)) }
            return mbs
        }

        override fun beforeDispatcherIO() {
            with (mBinding) {
                Log.d("SwipeToRefresh", "Preparing to refresh messages")
                listSwipeRefresh.isRefreshing = true
                list.isEnabled = false
                noSavedMessages.visibility = View.VISIBLE
                noSavedMessages.text = mTxtLoadingMsgs
                rbUploadedMessages.isEnabled = false
                rbNotUploadedMessages.isEnabled = false
            }
        }

        override fun insideDispatcherIO(scope: CoroutineScope): Pair<ArrayList<MessageBuilder>?, BaseResponse?> {
            val fetchUploadedMessages = mBinding.rbUploadedMessages.isChecked
            var locallySavedMessages = arrayListOf<MessageBuilder>()
            if (fetchUploadedMessages) {
                // If user data is present, pull messages from server instead of locally saved ones
                if (userData != null && userData.isUserValid()) {
                    // Retrieve messages from user
                    val userDomId = userData.domain?.id?.toInt()?: MMConstants.DefaultDomainId
                    val queryParams = hashMapOf(
                        "ownerid" to userData.id,
                        "sort" to "-id"
                    )
                    val messageApi = MMv1DomainMessages(mCtx, userDomId, queryParams, useUserCred = true, saveToDb = true)
                    messageApi.externalSystemInfo = userData.systemInfo
                    val response = messageApi.doExecuteAPI()
                    if (response != null) {
                        if (response.isSuccess()) {
                            val messagesList = messageApi.parseResponse(response)
                            messagesList.forEach { x ->
                                x.uploadStatus = mDB.constants.STATUS_FINISHED
                            }
                            return Pair(ArrayList(messagesList.map { m -> MessageBuilder(m) }), null)
                        } else {
                            // Error response from server
                            return Pair(null, response)
                        }
                    } else {
                        // No response
                        return Pair(null, null)
                    }
                } else {
                    locallySavedMessages = getLocallySavedMessages()
                }
            } else {
                locallySavedMessages = getLocallySavedMessages()
            }

            val filtered = locallySavedMessages.filter { x ->
                if (fetchUploadedMessages) {
                    x.message.uploadStatus == mDB.constants.STATUS_FINISHED
                } else {
                    x.message.uploadStatus != mDB.constants.STATUS_FINISHED
                }
            }

            return Pair(ArrayList(filtered), null)
        }

        override fun onSuccess(data: ArrayList<MessageBuilder>) {
            mMessages = data
            with (mBinding) {
                rbUploadedMessages.isEnabled = true
                rbNotUploadedMessages.isEnabled = true
                Log.d("SwipeToRefresh", "list finished refreshing with ${data.size} messages")
                listSwipeRefresh.isRefreshing = false
                list.isEnabled = true

                if (data.size > 0) {
                    list.visibility = View.VISIBLE
                    noSavedMessages.visibility = View.INVISIBLE
                    showMessages(data)
                } else {
                    noSavedMessages.visibility = View.VISIBLE
                    noSavedMessages.text = mTxtNoMsgs
                    list.visibility = View.INVISIBLE
                }
            }

            // After retrieving the messages from local DB, attempt to fetch the newest state from server again
            // Only fetch when user data is not valid (since this means that the messages come locally from DB)
            // If it's already fetched from server, we don't need to refetch it
            if (bFetchUpdateFromServer && (userData == null || !userData.isUserValid())) {
                with (mBinding) {
                    loadingMessageUpdate.visibility = View.VISIBLE
                    textMessageUpdate.visibility = View.VISIBLE
                }
                mMassMessageDetailUpdater?.cancel("New one called")
                val messages = arrayListOf<Message>()
                data.forEach { mb -> messages.add(mb.message) }
                mMassMessageDetailUpdater = APIMassMessageDetailUpdater(mCtx, messages) {
                    with (mBinding) {
                        loadingMessageUpdate.visibility = View.GONE
                        textMessageUpdate.visibility = View.GONE
                    }
                    RetrieveMyMessages(false).execute()
                }
                mMassMessageDetailUpdater?.execute()
            }
        }

        override fun onError(err: BaseResponse) {
            // TODO error from retrieving user messages from server
            with (mBinding) {
                rbUploadedMessages.isEnabled = true
                rbNotUploadedMessages.isEnabled = true
                listSwipeRefresh.isRefreshing = false
                list.isEnabled = true
            }
        }
    }

    inner class MessageUploadBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra(APIUploadMessage.STATUS)
            when (status) {
                APIUploadMessage.STATUS_SUCCESS, APIUploadMessage.STATUS_FAILED -> {
                    RetrieveMyMessages().execute()
                }
                else -> {
                    mListAdapter?.notifyDataSetChanged()
                }
            }

            with (mBinding) {
                rbUploadedMessages.isEnabled = true
                rbNotUploadedMessages.isEnabled = true
            }

            val msg = intent?.getStringExtra(APIUploadMessage.MESSAGE) ?: ""
            if (status == APIUploadMessage.STATUS_FAILED) {
                val alert = AlertDialog.Builder(context)
                    .setMessage(msg)
                    .setNegativeButton(R.string.dialog_cancel) { dialog, which ->
                        dialog.dismiss()
                    }
                try {
                    alert.show()
                } catch (e: Exception) {
                    // Trying to show the dialog window, but user has navigated away from the activity
                    e.printStackTrace()
                }
            } else {
                if (Build.VERSION.SDK_INT >= 24) {
                    val sb = Snackbar.make(mBinding.myMessagesLayout, msg, Snackbar.LENGTH_INDEFINITE)
                    sb.setAction(R.string.ok) { sb.dismiss() }
                    sb.show()
                } else {
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
                }
            }

        }
    }
}