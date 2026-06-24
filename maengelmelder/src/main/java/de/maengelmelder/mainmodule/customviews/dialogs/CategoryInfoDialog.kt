package de.maengelmelder.mainmodule.customviews.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.text.Html
import android.text.method.LinkMovementMethod
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.utils.ResourceProxy

/**
 * Dialog used to display information about the given [Category]
 */
class CategoryInfoDialog(ctx: Context, cat: Category) : Dialog(ctx), View.OnClickListener {

    /**
     * @property mCat the  [Category]
     * @property mNoDescText text used if the category has no description
     */
    private var mCat = cat
    private val mNoDescText = ctx.getString(R.string.catinfo_no_desc)

    private var mTxtCatTitle: TextView
    private var mTxtCatSubtitle: TextView
    private var mImgCat: ImageView
    private var mTxtDesc: TextView
    private var mBtnClose: Button

    init {
        // Set dialog title and option
        setTitle(R.string.catinfo_title)

        // inflate layout
        val view = LayoutInflater.from(context).inflate(R.layout.mm_dialog_categoryinfo, null)
        setContentView(view)

        val padding = view.findViewById<View>(R.id.padding)
        padding.visibility = View.GONE

        // get widgets
        mTxtCatTitle = view.findViewById(R.id.cat_title)
        mTxtCatSubtitle = view.findViewById(R.id.cat_sub)
        mImgCat = view.findViewById(R.id.cat_icon)
        mTxtDesc = view.findViewById(R.id.cat_desc)
        mBtnClose = view.findViewById(R.id.btn_close)
        mTxtDesc.movementMethod = LinkMovementMethod.getInstance()

        mBtnClose.setOnClickListener(this)

        // Accessibility
        view.contentDescription = ctx.getString(R.string.acc_cd_category_info, mCat.displayedName, mCat.description)

        populate()
    }

    override fun onClick(v: View?) {
        when (v) {
            mBtnClose -> dismiss()
        }
    }

    private fun populate() {
        mTxtCatTitle.text = mCat.name
        mTxtCatSubtitle.text = mCat.domainText
        mImgCat.setImageDrawable(ResourceProxy.getMarker(context, "white", mCat.markerId))
        mImgCat.contentDescription = mCat.name
        val cat = if (mCat.description.isEmpty()) mNoDescText else mCat.description
        if (Build.VERSION.SDK_INT >= 24) {
            mTxtDesc.text = Html.fromHtml(cat, Html.FROM_HTML_MODE_LEGACY)
        } else {
            mTxtDesc.text = Html.fromHtml(cat)
        }
    }

    var category: Category
        get() = mCat
        set(value) {
            mCat = value
            populate()
        }
}