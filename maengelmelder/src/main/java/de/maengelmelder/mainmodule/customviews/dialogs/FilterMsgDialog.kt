package de.maengelmelder.mainmodule.customviews.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Build
import android.view.KeyEvent
import com.google.android.material.textfield.TextInputEditText
import androidx.core.widget.CompoundButtonCompat
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.objects.MessageFilterParam
import de.maengelmelder.mainmodule.utils.ResourceProxy

/**
 * Dialog used to filter messages in a list
 */
class FilterMsgDialog(c: Context) : Dialog(c), View.OnClickListener, DialogInterface.OnCancelListener, TextView.OnEditorActionListener {

    /**
     * @property mCtx context
     * @property mLayoutCheckboxes layout containing generated Checkboxes
     * @property mEdtFilterDesc Text input for filtering by title / desc
     * @property mEdtFilterCat Text Input for filtering by category
     * @property mListener Output listener when filter is chosen or when dialog is cancelled
     * @property mBtnReset Reset button
     * @property mBtnFilter Filter button
     * @property mBtnCancel cancel button
     * @property mStatusSet set of statuses and their color
     */
    private val mCtx = c
    private var mLayoutCheckboxes: LinearLayout
    private var mEdtFilterDesc: TextInputEditText
    private var mEdtFilterCat: TextInputEditText
    private var mListener: Listener? = null

    private var mBtnReset: Button
    private var mBtnFilter: Button
    private var mBtnCancel: Button
    private var mChkFavOnly: CheckBox

    private var mStatusSet: Set<Pair<String, Int>>? = null

    init {
        setTitle(R.string.filter)
        setCancelable(true)
        setOnCancelListener(this)

        val v = LayoutInflater.from(context).inflate(R.layout.mm_dialog_msg_filter, null)
        setContentView(v)

        mEdtFilterDesc = v.findViewById(R.id.filter_title_text)
        mEdtFilterCat = v.findViewById(R.id.filter_category)
        mBtnFilter = v.findViewById(R.id.btn_filter)
        mBtnReset = v.findViewById(R.id.btn_clear)
        mBtnCancel = v.findViewById(R.id.btn_cancel)
        mLayoutCheckboxes = v.findViewById(R.id.checkboxes_status)
        mChkFavOnly = v.findViewById(R.id.chk_fav_only)

        mBtnReset.setOnClickListener(this)
        mBtnFilter.setOnClickListener(this)
        mBtnCancel.setOnClickListener(this)

        mEdtFilterDesc.setOnEditorActionListener(this)
        mEdtFilterCat.setOnEditorActionListener(this)
    }

    override fun onCancel(dialog: DialogInterface?) {
        mListener?.onCancel()
    }

    /**
     * sets the output listener of type [Listener]
     */
    fun setListener(l: Listener) {
        mListener = l
    }

    /**
     * Sets the statuses to be generated as checkboxes. Calling this method and supplying the parameter will remove the previous checkboxes.
     * By default, all checkboxes are set to be true (ticked)
     *
     * @param set set of statuses and their colors
     */
    fun setStatuses(set: Set<Pair<String, Int>>) {
        mStatusSet = set

        mLayoutCheckboxes.removeAllViews()

        mStatusSet?.forEach { pair ->
            val cb = CheckBox(mCtx)
            cb.text = pair.first
            cb.isChecked = true
            cb.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            cb.buttonTintList = ColorStateList.valueOf(pair.second)
            cb.minHeight = ResourceProxy.dpToPixel(mCtx.resources, 48f).toInt() // Accessibility
            cb.tag = pair.first

            mLayoutCheckboxes.addView(cb)
        }
    }

    fun setExistingStatus(statuses: Array<String>) {
        mLayoutCheckboxes.children.forEach {
            if (it is CheckBox) {
                if (statuses.contains(it.tag)) {
                    it.isChecked = true
                } else {
                    it.isChecked = false
                }
            }
        }
    }

    private fun isDefaultFilter(): Boolean {
        val statusStates = mLayoutCheckboxes.children.filter { v -> v is CheckBox }.map { v -> (v as CheckBox).isChecked }
        return mEdtFilterDesc.text.toString().isEmpty()             // no input on desc
                && mEdtFilterCat.text.toString().isEmpty()          // No input on category
                && !mChkFavOnly.isChecked                           // checkbox "fav only" not set
                && !statusStates.any { x -> !x }                    // All statuses are checked
    }

    private fun gatherAndExecuteFilter() {
        val filterDesc = mEdtFilterDesc.text.toString()
        val filterCat = mEdtFilterCat.text.toString()
        val enabledStatus = ArrayList<String>()
        if (mLayoutCheckboxes.childCount > 0) {
            for (i in 0..<mLayoutCheckboxes.childCount) {
                val cb = mLayoutCheckboxes.getChildAt(i)
                if (cb is CheckBox && cb.isChecked) {
                    enabledStatus.add(cb.text.toString())
                }
            }
        }

        mListener?.onFilter(filterDesc, filterCat, mChkFavOnly.isChecked, enabledStatus.toTypedArray(), isDefaultFilter())
    }

    override fun onEditorAction(p0: TextView?, p1: Int, p2: KeyEvent?): Boolean {
        if (p1 == EditorInfo.IME_ACTION_SEARCH) {
            gatherAndExecuteFilter()
            dismiss()
            return true
        }
        return false
    }

    override fun onClick(v: View?) {
        when(v) {
            mBtnReset -> {
                mEdtFilterCat.setText("")
                mEdtFilterDesc.setText("")

                if (mLayoutCheckboxes.childCount > 0) {
                    for (i in 0..<mLayoutCheckboxes.childCount) {
                        val cb = mLayoutCheckboxes.getChildAt(i)
                        if (cb is CheckBox) {
                            cb.isChecked = true
                        }
                    }
                }
            }

            mBtnFilter -> {
                gatherAndExecuteFilter()
                dismiss()
            }

            mBtnCancel -> {
                mListener?.onCancel()
                dismiss()
            }
        }
    }

    /**
     * Output listener
     */
    interface Listener {
        /**
         * Called when dialog is cancelled
         */
        fun onCancel()
        /**
         * Called when filters have been chosen (clicking "Filtern" button)
         */
        fun onFilter(desc: String, cat: String, favOnly: Boolean, statuses: Array<String>, isDefaultFilter: Boolean)
    }

}