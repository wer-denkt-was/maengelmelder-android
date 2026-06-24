package de.maengelmelder.mainmodule.fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.Html
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup.LayoutParams
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.children
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.activities.MessageDetailActivity
import de.maengelmelder.mainmodule.activities.MessageProcessActivity
import de.maengelmelder.mainmodule.adapters.CategoryListAdapter
import de.maengelmelder.mainmodule.adapters.DomainsSpinnerAdapter
import de.maengelmelder.mainmodule.adapters.DuplicateListAdapter
import de.maengelmelder.mainmodule.customviews.dialogs.CategoryInfoDialog
import de.maengelmelder.mainmodule.customviews.dialogs.ExternalCategoryPromptDialog
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.MMAPI
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.collectives.coroutines.APIGetDomains
import de.maengelmelder.mainmodule.network.coroutines.MMOkHttpClient
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Bms
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1CategorySearch
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Duplicates
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1System
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.BmsDomain
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.Message
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.service.tasks.CategoriesAndAttributesThread
import de.maengelmelder.mainmodule.utils.AccessibilityUtil
import de.maengelmelder.mainmodule.utils.Connectivity
import de.maengelmelder.mainmodule.utils.DeviceUtil
import de.maengelmelder.mainmodule.utils.DomainUtil
import de.maengelmelder.mainmodule.utils.QuickPrompt
import de.maengelmelder.mainmodule.utils.ResourceProxy
import de.maengelmelder.mainmodule.utils.UserData
import org.w3c.dom.Text
import java.lang.Exception

/**
 * Fragment for choosing category.
 *
 */
class ChooseCategoryStep : BaseMessageStepFragment(),
        ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupClickListener {

    /**
     * @property mList The [ListView] used to populate the categories. It uses [ExpandableListView] since there may be
     * branching categories. Branching categories are identified with ">" in its name
     * @property mListAdapter List adapter for [mList]
     * @property mTxtWarnNoLoc textview to warn the user to pick the location first before categories are displayed
     * @property mLoading progress bar for loading
     * @property mDuplicateView a view on top of the list displaying the list of possible duplicate messages
     * @property mDupCatName textview displaying the chosen category title on duplicate list
     * @property mDupText Textview displaying the chosen category subtitle on duplicate list
     * @property mDupIcon Textview displaying the chosen category icon on duplicate list
     *
     * @property mBtnIgnore button to close duplicate list
     * @property mCanChoose whether the user is allowed to choose a category or not
     * @property mEnterAnim animation
     * @property mCategoryInfoDialog dialog containing the information about the picked category
     */
    private var mStepInfo: TextView? = null
    private var mList: ExpandableListView? = null
    private var mListAdapter: CategoryListAdapter? = null
    private var mTxtWarnNoLoc: TextView? = null
    private var mTxtWarnNoCategory: TextView? = null
    private var mLoading: ProgressBar? = null
    private var mSystemSpinner: Spinner? = null

    private var mDuplicateView: LinearLayout? = null
    private var mDupText: TextView? = null
    private var mDupIcon: ImageView? = null
    private var mDupCatName: TextView? = null
    private var mDupList: ListView? = null
    private var mBtnIgnore: Button? = null
    private var mDupInfo: TextView? = null

    private var mCategoryLayout: RelativeLayout? = null
    private var mRubrikLayout: LinearLayout? = null
    private var mRubrikButtons: LinearLayout? = null
    private var mRubrikInfoLayout: LinearLayout? = null
    private var mRubrikInfoText: TextView? = null
    private var mRubrikInfoButton: Button ?= null

    private var mSearchLayout: LinearLayout? = null
    private var mSearchField: EditText? = null
    private var mSearchButton: ImageButton? = null
    private var mSearchCancel: ImageView? = null

    private var mCanChoose: Boolean = true
    private var mDupAdapter: DuplicateListAdapter? = null

    private var mEnterAnim: Animation? = null

    private var mLoadingWindow: ProgressDialog? = null

    private var mCategoryInfoDialog: CategoryInfoDialog? = null
    private var mDupTask: MMv1Duplicates? = null

    private val mAvailableRubrics: ArrayList<String> = arrayListOf()
    private var mSelectedRubric: String = ""
    private var bIsLoadingCategories: Boolean = false

    private var mRetrievedSystemInfos: List<SystemInfo>? = null

    override fun getLayoutId(): Int = R.layout.mm_fragment_choose_category

    override fun onViewInflated(v: View?) {
        // Ignore if not attached
        if (!isAdded) return

        // View's references
        mList = v?.findViewById(R.id.list)
        mLoading = v?.findViewById(R.id.loading)
        mTxtWarnNoLoc = v?.findViewById(R.id.warn_no_location)
        mTxtWarnNoCategory = v?.findViewById(R.id.warn_no_category_found)
        mDuplicateView = v?.findViewById(R.id.duplicateview)
        mDupText = v?.findViewById(R.id.numOfDuplicates)
        mDupList = v?.findViewById(R.id.duplicateList)
        mDupIcon = v?.findViewById(R.id.dup_icon)
        mDupInfo = v?.findViewById(R.id.step_dupfound_explain)
        mDupCatName = v?.findViewById(R.id.dup_cat_name)
        mBtnIgnore = v?.findViewById(R.id.btn_ignore)
        mStepInfo = v?.findViewById(R.id.step_cat_explain)
        mSystemSpinner = v?.findViewById(R.id.systems_spinner)
        mSystemSpinner?.visibility = View.GONE

        mCategoryLayout = v?.findViewById(R.id.category_layout)
        mRubrikLayout = v?.findViewById(R.id.rubrik_layout)
        mRubrikButtons = v?.findViewById(R.id.rubrik_buttons)
        mRubrikLayout?.visibility = View.GONE
        mRubrikInfoLayout = v?.findViewById(R.id.rubric_info)
        mRubrikInfoButton = v?.findViewById(R.id.btn_back_to_rubric)
        mRubrikInfoText = v?.findViewById(R.id.rubric_info_text)
        mRubrikInfoLayout?.visibility = View.GONE

        mSearchLayout = v?.findViewById(R.id.layout_searchbar)
        mSearchField = v?.findViewById(R.id.category_search)
        mSearchButton = v?.findViewById(R.id.btn_search_category)
        mSearchCancel = v?.findViewById(R.id.category_cancel)

        mRubrikInfoButton?.setOnClickListener { _ ->
            if (mAvailableRubrics.isNotEmpty()) showRubric()
        }

        mStepInfo?.text?.let { s ->
            if (s.isNotEmpty()) {
                mStepInfo?.visibility = View.VISIBLE
            } else {
                mStepInfo?.visibility = View.GONE
            }
        }

        // Show category search
        if (MMConstants.EnableCategorySearch) {
            mSearchLayout?.visibility = View.VISIBLE
            mSearchField?.setOnEditorActionListener { textView, i, keyEvent ->
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    context?.let {
                        DeviceUtil.hideSoftKeyboard(it, mSearchField)
                    }
                    executeCategorySearch()
                    true
                } else {
                    false
                }
            }
            mSearchButton?.setOnClickListener { _ -> executeCategorySearch() }
            mSearchCancel?.setOnClickListener { _ -> cancelCategorySearch() }
        } else {
            mSearchLayout?.visibility = View.GONE
        }

        // Show group indicator when it's not auto-expanded
        if (MMConstants.AutoExpandCategoryList) {
            mList?.setGroupIndicator(null)
        }

        // Start getting categories from the server
        loadCategories()

        // Ignore the duplicate list (basically hiding it)
        mBtnIgnore?.setOnClickListener {
            mDuplicateView?.visibility = View.GONE
            mList?.visibility = View.VISIBLE

            // Ignoring duplicate. go straight to the next fragment
            try {
                val category = builder?.category
                if (category != null) {
                    (activity as MessageProcessActivity).nextStepAfterSelectingCategory(category)
                } else {
                    (activity as MessageProcessActivity).nextStep()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        mEnterAnim = AnimationUtils.loadAnimation(context, R.anim.enter)

        // Listener for duplicate list
        mDupList?.setOnItemClickListener { parent, view, position, id ->
            val duplicate = parent.getItemAtPosition(position) as Message?
            if (duplicate != null) {
                context?.let { c ->
                    Intent(c, MessageDetailActivity::class.java).also { i ->
                        i.putExtra(MessageDetailActivity.BUNDLE_MSG, duplicate)
                        startActivity(i)
                    }
                }
            }
        }
    }

    override fun onViewBroughtUp() {
        builder?.let { b ->
            // When location is changed, refresh the categories with the given location
            if (b.isLocationChanged) {
                b.isLocationChanged = false
            } else if (b.isCategoryValid()) {
                context?.let { c ->
                    AccessibilityUtil.announce(c, getString(R.string.acc_cd_categorystep_chosen_cat, b.category.displayedName))
                }
            }

            mList?.isEnabled = builder?.isCategoryLocked() == false

            // Always load categories in case position changes
            loadCategories(true)
        }
    }

    private fun loadCategories(accessibilityAnnounce: Boolean = false) {
        if (bIsLoadingCategories) return;
        val mb = builder

        mTxtWarnNoCategory?.visibility = View.GONE

        // Location is also already provided from starting new message
        if (mb != null && mb.isLocationValid()) {
            val ctx = context

            mList?.visibility = View.GONE
            mSystemSpinner?.visibility = View.GONE
            mLoading?.visibility = View.VISIBLE
            mTxtWarnNoLoc?.visibility = View.GONE

            if (ctx != null) {

                if (accessibilityAnnounce) {
                    AccessibilityUtil.announce(ctx, getString(R.string.acc_cd_categorystep_item_loading))
                }

                val latlon = mb.getLocation()
                // Hide rubrik selection first (For when user selects new position)
                mRubrikInfoLayout?.visibility = View.GONE

                bIsLoadingCategories = true

                // Call systems-API to get the list of categories first
                val sys = MMv1System(ctx, latlon.second, latlon.first, true).apply {
                    listener = (object : MMBMS.BMSListener<List<SystemInfo>, BaseResponse> {
                        override fun onData(data: List<SystemInfo>) {
                            // Only pick external domains, if any
                            val extOnly =
                                if (data.size > 1) data.filter { i -> i.isExternal } else data
                            mRetrievedSystemInfos = extOnly

                            // Get the list of domains
                            APIGetDomains(ctx, extOnly, latlon.second, latlon.first, false).apply {
                                success = { domains -> mDomainListListener.onData(domains) }
                            }.execute()
                        }

                        override fun onFail(err: BaseResponse) {
                            // Check isAdded first before showing UI update
                            // Fragment is sometimes not attached to activity anymore (app in background or closed)
                            if (isAdded) {
                                if (MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.OfflineMode] == true
                                    && (err.code == MMOkHttpClient.RESPSTATUS_CONNECTION_FAILED || err.code == MMOkHttpClient.RESPSTATUS_IOEXC)
                                ) {
                                    // If offline mode is active, retrieve the standard categories instead
                                    retrieveStandardCategories()
                                } else {
                                    setWarning(
                                        getString(R.string.err_server_500),
                                        accessibilityAnnounce
                                    )
                                }
                            }
                            mLoading?.visibility = View.GONE
                            bIsLoadingCategories = false
                        }
                    })
                }
                sys.execute()

            }
        } else {
            // If not, warn the user that they have to select the location first before this step
            setWarning(getString(R.string.warn_no_location), accessibilityAnnounce)
            mLoading?.visibility = View.GONE
        }
    }

    private fun retrieveStandardCategories() {
        context?.let { c ->
            val db = MMDB.instance(c)
            val standardCategories = db.getCategoriesFromDomain("", "32", true)
            listCategories(standardCategories.toTypedArray())
        }
    }

    /**
     * Clear search text and load all categories
     */
    private fun cancelCategorySearch() {
        mSearchField?.text?.clear()
        mSelectedRubric = ""
        loadCategories(true)
    }

    private fun executeCategorySearch() {
        // Don't do it while app is fetching categories
        if (bIsLoadingCategories) return

        // Only call search API for text longer than 2 characters
        // If less than 2 characters, load the whole categories
        val searchText = mSearchField?.text?: ""
        if (searchText.length < 2) {
            context?.let { c ->
                AccessibilityUtil.announce(c, c.getString(R.string.acc_announce_category_search_too_short))
            }
            return
        }

        bIsLoadingCategories = true
        mLoading?.visibility = View.VISIBLE
        mTxtWarnNoCategory?.visibility = View.GONE

        // Hide the rubric layout
        mRubrikLayout?.visibility = View.GONE

        // Show category list layout immediately, but hide the list
        mCategoryLayout?.visibility = View.VISIBLE
        mList?.visibility = View.GONE

        context?.let { c ->
            if (mRetrievedSystemInfos == null || mRetrievedSystemInfos?.isEmpty() == true) {
                mRetrievedSystemInfos = listOf(SystemInfo.getDefaultSystemInfo())
            }
            // Get the list of domains from system info -> Get all searched categories from each retrieved domain
            // This way, we cover domain-specific platform and Germany-wide platform as well
            val latlon = builder?.getLocation()
            APIGetDomains(c, mRetrievedSystemInfos!!, latlon!!.second, latlon.first).apply {
                success = { domains ->
                    domains.forEach {
                        val systemInfo = mRetrievedSystemInfos?.find { x -> x.generateId() == it.systemId }
                        MMv1CategorySearch(c, it.id?.toInt()?: 0, searchText.toString()).apply {
                            externalSystemInfo = systemInfo
                            listener = object: MMBMS.BMSListener<List<Category>, BaseResponse> {
                                override fun onData(data: List<Category>) {
                                    bIsLoadingCategories = false
                                    mLoading?.visibility = View.GONE
                                    listCategories(data.toTypedArray(), true)
                                }
                                override fun onFail(err: BaseResponse) {
                                    // Failed to retrieve categories
                                    // Show error and the full list
                                    mSearchLayout?.let { layout ->
                                        QuickPrompt.inform(c, layout, c.getString(R.string.err_category_search_failed))
                                    }
                                    mLoading?.visibility = View.GONE
                                    bIsLoadingCategories = false
                                }
                            }
                        }.execute()
                    }
                }
            }.execute()
        }
    }

    private fun setWarning(text: String?, accessibilityAnnounce: Boolean = false) {
        mTxtWarnNoLoc?.let { txt ->
            if (text == null) {
                txt.visibility = View.GONE
            } else {
                txt.text = text
                txt.visibility = View.VISIBLE
                context?.let { c ->
                    if (accessibilityAnnounce) AccessibilityUtil.announce(c, txt.text.toString())
                }
            }
        }
    }

    override fun isLoading(): Boolean = false

    /**
     * Setup the list and display it
     */
    private fun listCategories(categories: Array<Category>, fromKeywordSearch: Boolean = false) {
        val ctx = context
        if (ctx == null) return

        // Query rubric first from the categories list
        mAvailableRubrics.clear()
        categories.forEach { c ->
            if (c.hasRubric() && !mAvailableRubrics.contains(c.rubric)) {
                mAvailableRubrics.add(c.rubric)
            }
        }

        // If there is only 1 rubric, no need to filter list or show rubrik selection.
        if (mAvailableRubrics.size == 1) {
            mAvailableRubrics.clear()
        }

        val selectedCat = builder?.category
        // Setup the list adapter
        mListAdapter = CategoryListAdapter(ctx, categories).apply {
            // if search result, show alternative view
            isSearchResult = fromKeywordSearch
            // If the user has picked a category before, it will be highlighted on the list
            selectedCat?.let { b ->
                if (selectedCat.isValid()) {
                    val chosenCat = categories.filter {
                            cat -> cat.generateId() == selectedCat.generateId()
                    }
                    if (chosenCat.isNotEmpty()) {
                        chosen = selectedCat
                    }
                }
            }
        }

        // Assign the adapter to the list
        mList?.setAdapter(mListAdapter)

        // Assign listener if category is not forced
        if (builder?.isCategoryLocked() == false) {
            mList?.setOnChildClickListener(this@ChooseCategoryStep)
            mList?.setOnGroupClickListener(this@ChooseCategoryStep)
        }

        if (mListAdapter?.hasOnlySingleCategoryPerGroup() == true) {
            mList?.setGroupIndicator(null)
        }

        // Show the correct layout based on the selected category
        if (!fromKeywordSearch) {
            if (mAvailableRubrics.isNotEmpty()) {
                // Show rubric page if no category is chosen yet
                if (selectedCat?.isValid() == true && selectedCat.hasRubric()) {
                    mSelectedRubric = selectedCat.rubric
                    showCategories()
                } else {
                    showRubric()
                }
            } else {
                showCategories()
            }
        } else {
            // If it comes from keyword search, just show the categories directly
            showCategories()
        }
    }

    private val mDomainListListener = object: MMBMS.BMSListener<List<Domain>, BaseResponse> {
        override fun onData(data: List<Domain>) {
            /*
            Log.d("mDomainListListener", "domains: "+data.size);
            data.forEach { x ->
                Log.d("mDomainListListener", "domain ${x.id} from system: ${x.systemId}");
            }
            */
            bIsLoadingCategories = false
            if (!isAdded) return

            mLoading?.visibility = View.GONE
            mSystemSpinner?.visibility = View.VISIBLE

            var domainList = data
            // If MessageUploadOnDefaultDomainOnly is true, check the domains first
            if (MMConstants.MessageUploadOnDefaultDomainOnly) {
                domainList = data.filter { dom -> dom.id == MMConstants.DefaultDomainId.toString() }
                if (domainList.isEmpty()) {
                    mSearchLayout?.visibility = View.GONE
                    mSystemSpinner?.visibility = View.GONE

                    mTxtWarnNoLoc?.text = getString(R.string.warn_domain_not_allowed)
                    mTxtWarnNoLoc?.visibility = View.VISIBLE
                    mList?.visibility = View.GONE
                    return
                }
            }

            /*
            // If there are no categories, warn user
            if (data.isEmpty() || data.find { d -> d.hasCategories() } == null) {
                mSystemSpinner?.visibility = View.GONE
                mTxtWarnNoLoc?.text = getString(R.string.warn_no_category)
                mTxtWarnNoLoc?.visibility = View.VISIBLE
                mList?.visibility = View.GONE
            } else {
                mList?.visibility = View.VISIBLE
                mTxtWarnNoLoc?.visibility = View.GONE
                context?.let { ctx ->
                    // Save the categories and attributes of different domains to the database
                    CategoriesAndAttributesThread(ctx, data).start()

                    // Setup the domain spinner
                    mSystemSpinner?.adapter = DomainsSpinnerAdapter(ctx, data)
                    mSystemSpinner?.onItemSelectedListener = mDomainSelectedListener
                    if (data.size > 1) {
                        mSystemSpinner?.visibility = View.VISIBLE
                    } else {
                        mSystemSpinner?.onItemSelectedListener?.
                                onItemSelected(mSystemSpinner, null, 0, 0)
                        mSystemSpinner?.visibility = View.GONE
                    }
                }
            }
            */
            val msgInternalType = builder?.message?.internalType?: MessageProcessActivity.TYPE_DEFECT_REPORT
            val ideaOnlyCatIds = context?.resources?.getIntArray(R.array.mm_mode_idea_catids)
            // TODO unify all categories to 1 list. Need to be reverted back when iOS is able to implement this
            val allCategories = domainList.fold(arrayListOf<Category>()) { initial, domain ->
                initial.addAll(domain.categoriesAsArray())
                initial
            }.filter { c ->
                ideaOnlyCatIds?.contains(c.typeId.toInt()) == (msgInternalType == MessageProcessActivity.TYPE_IDEA)
            }.toTypedArray()

            if (MMConstants.EnableCategorySearch) {
                mSearchLayout?.visibility = View.VISIBLE
            }

            mSystemSpinner?.visibility = View.GONE
            if (allCategories.isEmpty()) {
                mTxtWarnNoLoc?.text = getString(R.string.warn_no_category)
                mTxtWarnNoLoc?.visibility = View.VISIBLE
                mList?.visibility = View.GONE
            } else {
                mList?.visibility = View.VISIBLE
                mTxtWarnNoLoc?.visibility = View.GONE
                context?.let { ctx ->
                    listCategories(allCategories)

                    // Save the categories and attributes of different domains to the database
                    CategoriesAndAttributesThread(ctx, domainList).start()

                    /**
                     * Only used for testing rubric grouping.
                     *      If you don't want to modify the server parameters,
                     *      you can uncomment this to assign 3 different rubrics to the categories
                     */
                    /*
                    allCategories.forEachIndexed { index, category ->
                        category.rubric = if (index % 3 == 0) "Rubrik 1" else if (index % 3 == 1) "Rubrik 2" else "Rubrik 3"
                    }
                    */
                }
            }
        }

        override fun onFail(err: BaseResponse) {
            bIsLoadingCategories = false
            when (err.code) {
                400 -> Toast.makeText(mContext, getString(R.string.def_err_400), Toast.LENGTH_LONG).show()
                500 -> Toast.makeText(mContext, getString(R.string.def_err_500), Toast.LENGTH_LONG).show()
                503 -> Toast.makeText(mContext, getString(R.string.def_err_503), Toast.LENGTH_LONG).show()
                else -> Toast.makeText(context, R.string.err_no_conn, Toast.LENGTH_LONG).show()
            }
            mLoading?.visibility = View.GONE
        }
    }

    private val mDomainSelectedListener = object: AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) { }
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val selDomain = parent?.getItemAtPosition(position) as Domain?
            val ctx = context
            if (selDomain != null && ctx != null) {
                // get categories
                val cats = selDomain.categoriesAsArray()

                // Set up the list adapter and the listener
                mTxtWarnNoLoc?.visibility = View.GONE
                mListAdapter = CategoryListAdapter(ctx, cats).apply {
                    // If the user has picked a category before, it will be highlighted on the list
                    builder?.let { b ->
                        if (b.category.isValid()) {
                            val chosenCat = cats.filter { cat -> cat.equalToCategory(b.category) }
                            if (chosenCat.isNotEmpty()) {
                                chosen = b.category
                            }
                        }
                    }
                }
                mList?.setAdapter(mListAdapter)
                if (builder?.isCategoryLocked() == false) {
                    mList?.setOnChildClickListener(this@ChooseCategoryStep)
                    mList?.setOnGroupClickListener(this@ChooseCategoryStep)
                }
            }
        }
    }

    // When a category is clicked.
    override fun onChildClick(parent: ExpandableListView?, view: View?, grp: Int, child: Int, id: Long): Boolean {
        if (!mCanChoose) return false
        val cat = mListAdapter?.getChild(grp, child) as Category?

        // Set the picked category to the message builder
        builder?.let { b ->
            cat?.run {
                val extUrl = externalURL
                if (extUrl != null && extUrl.isNotEmpty() && extUrl.startsWith("http")) {
                    // If external URL is not empty, this category is an external category and thus
                    // doesn't have anything to do with our system anymore
                    // We prompt the user and if the user agrees, leads them to the given URL in browser
                    context?.let { c -> showExternalCategoryPrompt(c, this) }
                } else {
                    checkCategoryPickable(cat)
                }
            }
        }

        return true
    }

    /**
     * Since the list is an [ExpandableListView], the group click functions both as :
     * - child click if the category has no child. The display part is handled by [CategoryListAdapter]
     * - group click if the category has sub-categories. This acts similarly to normal group in expandable list view
     */
    override fun onGroupClick(parent: ExpandableListView?, view: View?, grp: Int, id: Long): Boolean {
        if (!mCanChoose) return false
        // Expand / close the clicked group to display the subcategories
        parent?.let {
            if (it.isGroupExpanded(grp)) {
                it.collapseGroup(grp)
            } else {
                it.expandGroup(grp)
            }

            // Close other groups, except if the selected category is inside the group
            // Only when the app config does not allow auto-expand on the grouped categories
            if (!MMConstants.AutoExpandCategoryList) {
                val groupCount = mListAdapter?.groupCount ?: 0
                for (i in 0 until groupCount) {
                    if (i == grp) continue
                    val children = mListAdapter?.getChildren(i)
                    val foundSelected = mListAdapter?.chosen != null && children?.find { c ->
                        mListAdapter?.chosen?.equalToCategory(c) == true
                    } != null
                    if (foundSelected) continue
                    it.collapseGroup(i)
                }
            }
        }


        return true
    }

    private fun showExternalCategoryPrompt(c: Context, cat: Category) {
        activity?.let { a -> ExternalCategoryPromptDialog(c, a, cat).show() }
    }

    private fun showCategoryDialogInfo(cat: Category) {
        if (cat.description.isEmpty()) return
        context?.let { ctx ->
            if (mCategoryInfoDialog == null) {
                mCategoryInfoDialog = CategoryInfoDialog(ctx, cat)
            } else {
                if (mCategoryInfoDialog?.isShowing == true)
                    mCategoryInfoDialog?.dismiss()
                mCategoryInfoDialog?.category = cat
            }
            mCategoryInfoDialog?.show()
        }
    }

    private fun checkCategoryPickable(cat: Category) {
        // if user is logged in, we need to check if the selected category matches the domain where the user is logged in
        context?.let { c ->
            activity?.let { a ->
                val systemInfo = MMDB.instance(c).getSystem(cat.systemId)
                DomainUtil.isCategoryPickable(c, a, cat, systemInfo) { canPick, statusCode ->
                    if (canPick) {
                        doSelectCategory(cat)
                    }
                }
            }
        }
    }

    private fun doSelectCategory(cat: Category) {
        context?.let { c ->
            builder?.let { b ->
                if (cat != b.category) {
                    // Set the chosen category
                    mListAdapter?.chosen = cat
                    b.category = cat
                    mSelectedRubric = cat.rubric

                    // Find duplicates
                    val loc = b.getLocation()
                    findDuplicate(cat, lat = loc.second, lon = loc.first)
                } else {
                    // Check if category has been selected before. If not, display the description automatically
                    showCategoryDialogInfo(cat)
                }
                val db = MMDB.instance(c)
                val catId = cat.generateId()
                if (!db.isCategorySeen(catId)) {
                    db.updateCategory(null, catId, Pair(db.constants.COL_ISSEEN, 1))
                    cat.isSeen = true
                    showCategoryDialogInfo(cat)
                }
            }
        }
    }

    /**
     * Open rubric page and hide the category list
     */
    private fun showRubric() {
        if (mAvailableRubrics.isEmpty()) return

        mCategoryLayout?.visibility = View.GONE
        mRubrikLayout?.visibility = View.VISIBLE
        mRubrikButtons?.removeAllViews()

        // populate buttons for rubrik
        mAvailableRubrics.forEach { r ->
            val btn = Button(ContextThemeWrapper(context, R.style.MMTheme_GeneralButton))
            btn.text = r
            btn.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            btn.contentDescription = getString(R.string.acc_cd_categorystep_rubrikbtn, r)
            btn.setOnClickListener(OnRubricClicked(r))
            mRubrikButtons?.addView(btn)
        }
    }

    /**
     * Show category subpage and hide rubric one. Also filters the category by rubric
     */
    private fun showCategories() {
        mCategoryLayout?.visibility = View.VISIBLE
        mList?.visibility = View.VISIBLE
        mRubrikLayout?.visibility = View.GONE

        if (mSelectedRubric.isNotEmpty() && mSelectedRubric.trim() != "null") {
            mRubrikInfoLayout?.visibility = View.VISIBLE
            mRubrikInfoText?.text = getString(R.string.form_category_rubrikinfo_title, mSelectedRubric)
            mRubrikInfoText?.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }

        // filter the list
        mListAdapter?.selectedRubric = mSelectedRubric

        if (mListAdapter?.displayedCategoryCount == 0) {
            mTxtWarnNoCategory?.visibility = View.VISIBLE
        } else {
            mTxtWarnNoCategory?.visibility = View.GONE
            // Highlight selected if exists
            builder?.let { b ->
                if (b.isCategoryValid()) {
                    mListAdapter?.chosen = b.category
                }
            }

            // Expand or collapse group when given
            val grpCount = mListAdapter?.groupCount ?: 0
            if (grpCount > 0) {
                for (i in 0..<grpCount) {
                    val childCount = mListAdapter?.getChildrenCount(i)
                    var shouldExpand =
                        MMConstants.AutoExpandCategoryList || (childCount == MMConstants.AutoExpandWhenOnlyNSubcategory)

                    // If a subcategory is already chosen and resides inside the given group, auto expand it regardless of settings
                    if (mListAdapter?.chosen != null) {
                        val subcategories = mListAdapter?.getChildren(i)
                        val isChosenInGroup =
                            subcategories?.any { c -> c.generateId() == mListAdapter?.chosen?.generateId() }
                                ?: false
                        if (isChosenInGroup) {
                            shouldExpand = true
                        }
                    }

                    try {
                        if (shouldExpand) {
                            mList?.expandGroup(i)
                        } else {
                            mList?.collapseGroup(i)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * Listener for rubrik buttons
     */
    inner class OnRubricClicked(private val rubric: String) : OnClickListener {
        override fun onClick(p0: View?) {
            mSelectedRubric = rubric
            showCategories()
        }
    }

    /**
     * Method to find duplicates of messages with the given category. It calls [MMBMSGetDuplicates]
     *
     * @param cat [Category]
     * @param lat latitude
     * @param lon longitude
     */
    private fun findDuplicate(cat: Category, lat: Double, lon: Double) {
        // Disable the user from choosing new category before any duplicate is found
        mCanChoose = false
        context?.let { ctx ->
            // Show loading indicator
            mLoadingWindow = ProgressDialog(ctx).apply {
                setMessage(ctx.getString(R.string.loading_finding_dup))
                setCancelable(true)
                setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_cancel)) {
                    dialog, _ ->
                        dialog.dismiss()
                        mDupTask?.cancel("Duplicate API cancelled manually by user before it resolves")
                        mCanChoose = true
                }
                setOnCancelListener { dialog ->
                    mDupTask?.cancel("Duplicate API cancelled manually by user before it resolves")
                    mCanChoose = true
                }
            }
            mLoadingWindow?.show()

            // get relevant system
            val db = MMDB.instance(ctx)
            val sys = db.getSystem(cat.systemId)

            // Execute the [MMBMSGetDuplicates] task
            if (mDupTask != null) mDupTask?.cancel("Remove old duplicate Call due to category change")
            mDupTask = MMv1Duplicates(ctx, cat, lat, lon).apply {
                attachUserCred = false // Don't attach user cred
                externalSystemInfo = sys
                listener = (object : MMBMS.BMSListener<List<Message>, BaseResponse> {
                    override fun onData(data: List<Message>) {
                        mCanChoose = true
                        mLoadingWindow?.dismiss()
                        // If there is at least 1 duplicate found, shows the duplicate message list overlaying the category list
                        if (data.isNotEmpty()) {
                            context?.let { ctx ->
                                AccessibilityUtil.announce(
                                        ctx,
                                        ctx.getString(R.string.acc_announce_duplicatefound, data.size.toString()))
                            }
                            mDuplicateView?.visibility = View.VISIBLE
                            mList?.visibility = View.GONE

                            getString(R.string.warn_duplicate_found).let { s ->
                                if (s.isNotEmpty()) {
                                    mDupInfo?.visibility = View.VISIBLE
                                    mDupInfo?.text = s
                                } else {
                                    mDupInfo?.visibility = View.GONE
                                }
                            }

                            mDupIcon?.setImageDrawable(ResourceProxy.getMarker(ctx, "white", cat.markerId))
                            mDupCatName?.text = cat.displayedName
                            mDupText?.text = getString(R.string.info_num_duplicates, data.size)

                            mDupAdapter = DuplicateListAdapter(ctx, data.toTypedArray())
                            mDupList?.adapter = mDupAdapter

                            // Small animation
                            mDuplicateView?.startAnimation(mEnterAnim)
                        } else {
                            // No duplicate go straight to the next fragment
                            context?.let { ctx ->
                                AccessibilityUtil.announce(
                                        ctx,
                                        ctx.getString(R.string.acc_announce_duplicatenotfound))
                            }
                            try {
                                (activity as MessageProcessActivity).nextStepAfterSelectingCategory(cat)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    override fun onFail(err: BaseResponse) {
                        // If it fails, continue as usual
                        mCanChoose = true
                        mLoadingWindow?.dismiss()

                        try {
                            (activity as MessageProcessActivity).nextStepAfterSelectingCategory(cat)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })
                execute()
            }
        }
    }

    fun clearRubricSelection() {
        mSelectedRubric = ""
    }

    // Completion of the step is marked by whether the user has chosen a category or not
    override fun isStepComplete(): Boolean = builder?.category?.isValid() == true

    override fun getTitle(): String = mContext?.getString(R.string.step_choose_category)?: ""

    override fun shouldPromptBeforeChange(): Boolean = false

    override fun executeBeforeChange() {
        // Clear search field if any
        mSearchField?.text?.clear()

        // Save the chosen category to the database
        val chosen = mListAdapter?.chosen

        // Add the chosen category ID to category_id column of the message
        builder?.let { b ->
            chosen?.let {
                b.category = it
                b.message.systemId = it.systemId
                val msgId = b.messageId
                context?.let { ctx ->
                    val db = MMDB.instance(ctx)
                    db.updateMessage(msgId,
                            db.constants.COL_CAT_ID to chosen.generateId(),
                            db.constants.COL_SYSTEM_ID to chosen.systemId)
                }
            }
        }
    }

    override fun promptBeforeChange(f: (Boolean) -> Unit) { }
}