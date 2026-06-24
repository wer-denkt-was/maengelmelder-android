package de.maengelmelder.mainmodule.adapters

import android.content.Context
import android.text.Spanned
import android.util.Log
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.utils.ResourceProxy
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class CategoryListAdapter(
        c: Context,
        private val categories: Array<Category>,
) : BaseExpandableListAdapter() {

    private var mContext = c
    private var mHeaders: List<String> = ArrayList()
    private var mMapped: TreeMap<String, ArrayList<Category>> = TreeMap()
    private var mChosen: Category? = null
    private var mSelectedRubric: String = ""

    private var bIsSearchResult: Boolean = false
    private var bHideGroupHeader: Boolean = true

    private val mBgColorNormal = ContextCompat.getColor(mContext, R.color.mmcolor_category_listitem_normal_bg)
    private val mBgColorSelected = ContextCompat.getColor(mContext, R.color.mmcolor_category_listitem_chosen_bg)
    private val mTextColorNormal = ContextCompat.getColor(mContext, R.color.mmcolor_category_listitem_normal_text)
    private val mTextColorSelected = ContextCompat.getColor(mContext, R.color.mmcolor_category_listitem_chosen_text)

    private val mBGColorHeader = ContextCompat.getColor(mContext, R.color.lightgray)

    init {
        mapCategories(categories)
        hasOnlySingleCategoryPerGroup()
    }

    var chosen: Category? get() = mChosen
        set(value) {
            mChosen = value
            notifyDataSetChanged()
        }

    var selectedRubric: String
        get() = mSelectedRubric
        set(value) {
            // refresh adapter (remap categories)
            mSelectedRubric = value
            mapCategories(categories)
            notifyDataSetChanged()
        }

    var isSearchResult: Boolean
        get() = bIsSearchResult
        set(value) {
            bIsSearchResult = value
            notifyDataSetChanged()
        }

    // Num of categories being displayed on screen (after being filtered by rubric, etc)
    val displayedCategoryCount: Int get() {
        if (mMapped.isEmpty()) return 0
        var count = 0
        mMapped.keys.forEach { key ->
            count += mMapped[key]?.size?: 0
        }
        return count
    }

    fun hasOnlySingleCategoryPerGroup(): Boolean {
        // Check if every mapped categories only have 1 category. If so, we can just hide the header
        mMapped.keys.forEach { groupTitle ->
            if ((mMapped[groupTitle]?.size ?: 0) > 1) {
                bHideGroupHeader = false
                return@forEach
            }
        }
        return bHideGroupHeader
    }

    private fun mapCategories(arr: Array<Category>) {
        mMapped = TreeMap()
        // Filter the categories by rubric first
        val filtered = if (mSelectedRubric.isNotEmpty()) {
            arr.filter { c -> c.rubric == mSelectedRubric }.toTypedArray()
        } else {
            arr
        }

        filtered.forEach { cat ->
            var grpName = if (cat.group.isNotEmpty()) cat.group else ""
            if (grpName.isEmpty() && cat.name.contains(">")) {
                grpName = cat.name.split(">")[0]
            }
            if (grpName.isEmpty()) {
                grpName = cat.name
            }

            if (mMapped.containsKey(grpName)) {
                mMapped[grpName]?.add(cat)
            } else {
                mMapped[grpName] = arrayListOf(cat)
            }
        }
        mHeaders = mMapped.keys.toList()
    }

    override fun getGroup(p0: Int): Any = mHeaders[p0]

    override fun isChildSelectable(grp: Int, child: Int): Boolean = true

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(grpIdx: Int, expanded: Boolean, v: View?, vg: ViewGroup?): View {
        lateinit var view: View
        if (bHideGroupHeader) {
            view = FrameLayout(mContext) // Empty layout
        } else {
            view = getGeneratedView(v, vg)
            val holder = view.tag as ViewHolder
            holder.parent.visibility = View.VISIBLE
            holder.parent.setBackgroundColor(mBGColorHeader)
            holder.image.visibility = View.GONE
            holder.subtitle.visibility = View.GONE
            holder.title.visibility = View.VISIBLE
            holder.title.text = getGroupTitle(grpIdx)
            holder.title.textSize = 18f
            holder.title.setPadding(70, 0, 0, 0)
            view.isFocusable = false
            view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        return view
    }

    override fun getChildrenCount(grp: Int): Int = mMapped[mHeaders[grp]]?.size?: 0

    override fun getChild(grp: Int, child: Int): Any? = mMapped[mHeaders[grp]]?.get(child)

    override fun getGroupId(grp: Int): Long = grp.toLong()

    fun getChildren(grp: Int): Array<Category>? {
        return mMapped[mHeaders[grp]]?.toTypedArray()
    }

    private fun getGroupTitle(grp: Int): Spanned {
        val childCount = getChildrenCount(grp)
        var groupTitle = getGroup(grp) as String
        if (groupTitle.contains(">")) {
            groupTitle = groupTitle.split(">")[0]
        }
        return ResourceProxy.fromHTML("$groupTitle ( $childCount )")
    }

    private fun getOptimizedChildView(grpIdx: Int, childIdx: Int, lastChild: Boolean, v: View?, vg: ViewGroup?,
                                      pad: Boolean = true): View {

        val view: View = getGeneratedView(v, vg, pad)
        val holder = view.tag as ViewHolder
        val cat = getChild(grpIdx, childIdx) as Category

        holder.image.visibility = View.VISIBLE
        holder.title.visibility = View.VISIBLE
        holder.title.text = ResourceProxy.fromHTML(cat.getActualName())
        holder.title.textSize = 15f

        // If users can only upload on default domain, no need to show domain text
        if (!MMConstants.MessageUploadOnDefaultDomainOnly) {
            holder.subtitle.visibility = View.VISIBLE
            holder.subtitle.text = cat.domainText
        } else {
            holder.subtitle.visibility = View.GONE
        }

        if (bIsSearchResult) {
            holder.rubric.visibility = View.VISIBLE
            holder.rubric.text = cat.rubric
        } else {
            holder.rubric.visibility = View.GONE
        }

        holder.image.setImageDrawable(ResourceProxy.getMarker(mContext, "white", cat.markerId))
        holder.image.contentDescription = cat.displayedName

        view.contentDescription = "${cat.group}, ${cat.displayedName}"

        view.setBackgroundColor(if (mChosen != null && mChosen?.equalToCategory(cat) == true) mBgColorSelected else mBgColorNormal)
        holder.title.setTextColor(if (mChosen != null && mChosen?.equalToCategory(cat) == true) mTextColorSelected else mTextColorNormal)
        holder.subtitle.setTextColor(if (mChosen != null && mChosen?.equalToCategory(cat) == true) mTextColorSelected else mTextColorNormal)

        if (mChosen != null && mChosen?.equalToCategory(cat) == true && mChosen?.description?.isNotEmpty() == true) {
            holder.showInfo()
        } else {
            holder.hideInfo()
        }

        return view
    }

    override fun getChildView(grpIdx: Int, childIdx: Int, lastChild: Boolean, v: View?, vg: ViewGroup?): View =
        getOptimizedChildView(grpIdx, childIdx, lastChild, v, vg)


    override fun getChildId(grp: Int, child: Int): Long = child.toLong()

    override fun getGroupCount(): Int = mHeaders.size

    private fun getGeneratedView(v: View?, vg: ViewGroup?, pad: Boolean = false): View {
        val view = v?: LayoutInflater.from(mContext).inflate(R.layout.mm_listitem_category, vg, false)
        val holder = ViewHolder(view)
        holder.pad(pad)
        view.tag = holder
        return view
    }

    inner class ViewHolder(v: View) {
        val title = v.findViewById<TextView>(R.id.cat_title)
        val subtitle = v.findViewById<TextView>(R.id.cat_sub)
        val rubric = v.findViewById<TextView>(R.id.cat_rubric)
        val image = v.findViewById<ImageView>(R.id.cat_icon)
        val info = v.findViewById<ImageView>(R.id.info)
        val parent = v.findViewById<RelativeLayout>(R.id.parent)
        private val padding = v.findViewById<View>(R.id.padding)

        fun showInfo() {
            info.visibility = View.VISIBLE
        }

        fun hideInfo() {
            info.visibility = View.GONE
        }

        fun pad(toggle: Boolean) {
            padding.visibility = if (toggle) View.VISIBLE else View.GONE
        }
    }
}