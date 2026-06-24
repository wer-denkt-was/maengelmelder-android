package de.maengelmelder.mainmodule.customviews.attributes

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.InputFilter
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.objects.Attribute

/**
 * Text attribute field
 */
open class TextAttributeView(c: Context, attr: Attribute, layoutId: Int = R.layout.mm_attribute_text) :
        BaseAttributeView<String>(c, layoutId, attr) {

    protected var mError: TextView? = null
    protected var mTitle: TextView? = null
    protected var mContent: EditText? = null
    protected var mReadOnlyValue: TextView? = null

    init {
        mTitle = view?.findViewById(R.id.title)
        mTitle?.movementMethod = LinkMovementMethod()
        mContent = view?.findViewById(R.id.text)
        mError = view?.findViewById(R.id.error)
        mReadOnlyValue = view?.findViewById(R.id.readonly)
        mReadOnlyValue?.visibility = View.GONE
        mContent?.hint = attr.helpText

        val maxLength = attribute?.maxLength ?: 0
        if ((attribute?.type == "text" || attribute?.type == "textarea") && maxLength > 0) {
            mContent?.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        }
    }

    fun setInputType(inpType: Int): TextAttributeView {
        mContent?.inputType = inpType
        return this
    }

    override fun hasValue(): Boolean {
        val value = mContent?.text?.toString()
        return if (mContent?.inputType == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
            Patterns.EMAIL_ADDRESS.matcher(value?: "").matches()
        } else {
            value != null && value.isNotEmpty()
        }
    }

    override fun getType(): String = Attribute.TYPE_TEXT

    override fun getValue(): String? = mContent?.text.toString().trim()

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

    @Suppress("DEPRECATION")
    override fun setHint(hint: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mContent?.hint = Html.fromHtml(hint, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            mContent?.hint = Html.fromHtml(hint).toString()
        }
    }

    override fun toggleReadOnly(toggle: Boolean) {
        mContent?.isEnabled = !toggle
        mContent?.visibility = if (toggle) View.GONE else View.VISIBLE
        mReadOnlyValue?.visibility = if (toggle) View.VISIBLE else View.GONE

        if (mContent?.inputType == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS){
            val email = (mReadOnlyValue?.text?: "").toString().replace(" ", "")
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError(attribute?.errorText)
            } else {
                showError(null)
            }
        } else {
            val value = getValue()
            if (attribute?.required == true && (value == null || value.isEmpty())) {
                showError(attribute?.errorText)
            } else {
                showError(null)
            }
        }
    }

    private fun showError(errText: String? = null) {
        if (errText == null) {
            mError?.visibility = View.GONE
            mError?.text = ""
            mError?.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0)
        } else {
            mError?.visibility = View.VISIBLE
            mError?.text = errText
            mError?.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_drwtext_error, 0, 0, 0
            )
        }
    }

    override fun setValue(value: Any) {
        mContent?.setText(value as String)
        mReadOnlyValue?.text = value as String
    }

    override fun isAttributeFilledOut(): Boolean {
        if (!isRequired()) return true
        val content = mContent?.text?.trim()
        return !content.isNullOrEmpty() && content.isNotBlank()
    }

    override fun toggleError(toggle: Boolean, errText: String) {
        mError?.visibility = if (toggle) View.VISIBLE else View.GONE
        if (toggle) {
            mError?.text = errText
        }
    }

}