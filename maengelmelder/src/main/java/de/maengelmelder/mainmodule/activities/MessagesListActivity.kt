package de.maengelmelder.mainmodule.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.PopupMenu
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.adapters.MessagesListAdapter
import de.maengelmelder.mainmodule.customviews.dialogs.FilterMsgDialog
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.databinding.MmActivityListMessagesBinding
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.service.tasks.MessageLoadingCoroutine
import de.maengelmelder.mainmodule.utils.comparators.SortBy
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import java.lang.StringBuilder

/**
 * Activity where the user can:
 * - See all messages in a list
 * - Filter messages
 * - Check the detail of the message or view them on a map
 */
class MessagesListActivity : AppCompatActivity(), MessageLoadingCoroutine.Listener, View.OnClickListener,
        FilterMsgDialog.Listener, AdapterView.OnItemClickListener, PopupMenu.OnMenuItemClickListener {

    companion object {
        const val REQ_CODE = 555

        const val BUNDLE_MESSAGE_IDS = "wdw.mmv2.bundle.message_ids"
    }

    /**
     * @property mAdapter adapter for the list
     * @property mLoader asynctask for loading the message
     * @property mFilterDialog dialog for filter menu
     * @property mPopup popup menu when selecting a message
     * @property mChosen currently selected message
     */
    private var mAdapter: MessagesListAdapter? = null
    private lateinit var mLoader: MessageLoadingCoroutine
    private lateinit var mFilterDialog: FilterMsgDialog
    private lateinit var mPopup: PopupMenu
    private lateinit var mDB: MMDB
    private var mChosen: MessageBuilder? = null

    private var mCurrentDescFilter = ""
    private var mCurrentCatFilter = ""
    private var mFavOnlyFilter: Boolean = false
    private var mFilterStatuses: Array<String> = arrayOf()

    private lateinit var mBinding: MmActivityListMessagesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = MmActivityListMessagesBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mDB = MMDB.instance(this)

        val msgIds = intent?.getStringArrayListExtra(BUNDLE_MESSAGE_IDS)
        mLoader = MessageLoadingCoroutine(this, mAdapter, msgIds)
        mLoader.setListener(this)
        mBinding.filterinfo.setOnClickListener(this)

        refreshList()

        mBinding.list.onItemClickListener = this
    }

    /**
     * sets the adapter to the list
     */
    override fun onAdapterReady(adapter: MessagesListAdapter?) {
        mAdapter = adapter
        mBinding.list.adapter = mAdapter
        refreshMessageCountState()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    /**
     * Refresh message count state
     */
    private fun refreshMessageCountState() {
        val cnt = mAdapter?.getFilteredSize()?: 0
        with (mBinding) {
            if (cnt == 0) {
                status.text = getString(R.string.info_no_messages)
                msgCount.visibility = View.GONE
            } else {
                status.visibility = View.GONE
                msgCount.visibility = View.VISIBLE
                msgCount.text = resources.getQuantityString(R.plurals.msgs_found, cnt, cnt)
            }
        }
    }

    /**
     * show the popup menu
     */
    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val msg = parent?.getItemAtPosition(position) as MessageBuilder?
        msg?.let { m ->
            mChosen = m
            view?.let { v ->
                showOption(m, v)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_context_msg_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_by_name -> {
                mAdapter?.sort()
            }
            R.id.menu_sort_by_category -> {
                mAdapter?.sort(SortBy.Param.CATEGORY)
            }
            R.id.menu_sort_by_status -> {
                mAdapter?.sort(SortBy.Param.STATUS)
            }
            android.R.id.home -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.filterinfo -> showFilter()
        }
    }

    /**
     *  shows the popup menu of the selected message
     *
     *  @param mb the message
     *  @param anchor view as the anchor for popup.
     */
    private fun showOption(mb: MessageBuilder, anchor: View) {
        mChosen = mb
        if (!::mPopup.isInitialized) {
            mPopup = PopupMenu(this, anchor)
            mPopup.inflate(R.menu.menu_msg_list)
            mPopup.setOnMenuItemClickListener(this)
        }
        mPopup.show()
    }

    /**
     * Shows the filter dialog
     */
    private fun showFilter() {
        mAdapter?.let { adapter ->
            if (!::mFilterDialog.isInitialized) {
                mFilterDialog = FilterMsgDialog(this)
                mFilterDialog.setListener(this)
                mFilterDialog.setStatuses(adapter.getStatuses())
            }
            mFilterDialog.show()
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.menu_findonmap -> {
                // back to overview map and navigate to the message's location
                mChosen?.let {
                    val bundle = Intent()
                    bundle.putExtra(OverviewActivity.BUNDLE_MSG_ID, it.messageId)
                    setResult(Activity.RESULT_OK, bundle)
                    finish()
                }
                return true
            }
            R.id.menu_detail -> {
                // Directly go to detail
                mChosen?.let {
                    val i = Intent(this, MessageDetailActivity::class.java)
                    i.putExtra(MessageDetailActivity.BUNDLE_MSG_ID, it.messageId)
                    startActivity(i)
                }
                return true
            }

            R.id.menu_fav -> {
                // Mark the chosen message as favorite
                mChosen?.let {
                    mAdapter?.toggleFavorite(it.messageId)
                }
            }
        }

        return false
    }

    override fun onFilter(desc: String, cat: String, favOnly: Boolean, statuses: Array<String>, isDefaultFilter: Boolean) {
        // get the filters
        mCurrentDescFilter = desc
        mCurrentCatFilter = cat
        mFavOnlyFilter = favOnly
        mFilterStatuses = statuses

        // Filter against the adapter
        mAdapter?.filter(desc, cat, favOnly, statuses, -1)
        setFilterText(desc, cat, favOnly, statuses)

        // Refresh the message count based on the filtered messages
        refreshMessageCountState()
    }

    /**
     * Sets the filter text for the chosen filters
     *
     * @param desc text for filtering description
     * @param cat text for filtering category
     * @param statuses array of allowed statuses
     */
    private fun setFilterText(desc: String, cat: String, favOnly: Boolean, statuses: Array<String>) {
        val sb = StringBuilder()
        if (!desc.isEmpty()) {
            sb.append("- ")
            sb.append(getString(R.string.filter_info_desc, desc))
            sb.append("\n")
        }
        if (!cat.isEmpty()) {
            sb.append("- ")
            sb.append(getString(R.string.filter_info_cat, cat))
            sb.append("\n")
        }
        if (statuses.isNotEmpty()) {
            sb.append("- ")
            sb.append(getString(R.string.filter_info_status, statuses.fold("") { start, item -> "$item, $start" }))
            sb.append("\n")
        }
        if (favOnly) {
            sb.append("- ")
            sb.append(getString(R.string.filter_fav_only))
        }

        mBinding.filterinfo.text = sb.toString()
    }

    override fun onCancel() { }

    /**
     *  refreshOfflineMap the list again and load it with new data form database
     */
    private fun refreshList() {
        with (mBinding) {
            status.text = getString(R.string.status_loading_messages)
            status.visibility = View.VISIBLE
        }
        mLoader.execute()
    }

}