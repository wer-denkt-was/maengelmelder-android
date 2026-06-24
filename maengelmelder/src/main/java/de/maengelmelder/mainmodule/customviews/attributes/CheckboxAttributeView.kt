package de.maengelmelder.mainmodule.customviews.attributes

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.objects.Attribute

/**
 * Checkbox form field
 */
class CheckboxAttributeView : BaseAttributeView<Boolean> {

    private var mTitle: TextView? = null
    private var mError: TextView? = null
    private var mChkbox: CheckBox? = null

    constructor(c: Context, attr: Attribute) : super(c, R.layout.mm_attribute_checkbox, attr) {
        mTitle = view?.findViewById<TextView>(R.id.title)
        mTitle?.movementMethod = LinkMovementMethod()
        mChkbox = view?.findViewById<CheckBox>(R.id.chkbox)
        mChkbox?.movementMethod = LinkMovementMethod()
        mError = view?.findViewById<TextView>(R.id.error)
    }

    @Suppress("DEPRECATION")
    override fun setTitle(title: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mTitle?.text = Html.fromHtml(title, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            mTitle?.text = Html.fromHtml(title).toString()
        }
        if (attribute?.required == true) {
            mTitle?.text = mTitle?.text?.toString()?.plus("*");
        }
    }

    override fun setHint(hint: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mChkbox?.text = Html.fromHtml(hint, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            mChkbox?.text = Html.fromHtml(hint).toString()
        }
    }

    override fun toggleReadOnly(toggle: Boolean) {
        mChkbox?.isEnabled = !toggle
    }

    override fun hasValue(): Boolean = true

    override fun getType(): String = Attribute.TYPE_CHECKBOX

    override fun getValue(): Boolean? = mChkbox?.isChecked

    override fun setValue(value: Any) { mChkbox?.isChecked = java.lang.Boolean.parseBoolean(value.toString()) }

    override fun isAttributeFilledOut(): Boolean {
        if (!isRequired()) return true
        return getValue()?: false
    }

    override fun toggleError(toggle: Boolean, errText: String) {
        mError?.visibility = if (toggle) View.VISIBLE else View.GONE
        if (toggle) {
            mError?.text = errText
        }
    }
}