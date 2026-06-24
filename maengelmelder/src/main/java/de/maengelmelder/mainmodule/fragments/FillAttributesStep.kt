package de.maengelmelder.mainmodule.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Build
import androidx.preference.PreferenceManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.customviews.attributes.*
import de.maengelmelder.mainmodule.customviews.dialogs.LoadingDialog
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Attribute
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Category
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Attribute
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.utils.AccessibilityUtil
import de.maengelmelder.mainmodule.utils.UserData
import org.w3c.dom.Attr

/**
 *
 * This fragment contains all the attributes needed to be filled in after choosing a category
 */
class FillAttributesStep : BaseMessageStepFragment() {

    /**
     * @property PREFIX_ATTR_PREF prefix for preference's key for default value on attribute. It should be followed by the attribute Id
     * @property mHelp textview displaying help
     * @property mAttributesView [LinearLayout] displaying the required view for each attribute.
     * @property mAttributeList List containing the [BaseAttributeView]. This object is used to store reference to both view and [Attribute]
     * @property mTxtTitle Input for title
     * @property mTxtDesc Input for description
     */
    private val PREFIX_ATTR_PREF = "attrib-"

    private var mHelp: TextView? = null
    private var mAttributesView: LinearLayout? = null
    private var mAttributeList: ArrayList<BaseAttributeView<Any>>? = null

    private var mTxtTitleLayout: TextInputLayout? = null
    private var mTxtTitle: TextInputEditText? = null
    private var mTxtDesc: TextInputEditText? = null
    private var mTxtLimitWarning: TextView? = null
    private var mTxtLimit: TextView? = null
    private var mHelpText: TextView? = null

    private var mDescSection: LinearLayout? = null
    private var mTxtDescInputLayout: TextInputLayout? = null
    private var mDescTextLimit = -1
    private var mDescTextWarning = ""

    private var mPref: SharedPreferences? = null
    private var bFirstTimeLoaded = true

    override fun getLayoutId(): Int = R.layout.mm_fragment_form

    override fun onViewInflated(v: View?) {
        // Set references to the view's widget
        mHelp = v?.findViewById(R.id.help)
        mTxtTitle = v?.findViewById(R.id.title)
        mTxtDesc = v?.findViewById(R.id.description)
        mAttributesView = v?.findViewById (R.id.attribviews)

        mTxtTitleLayout = v?.findViewById(R.id.titleLayout)
        mDescSection = v?.findViewById(R.id.form_description_section)
        mTxtDescInputLayout = v?.findViewById(R.id.descLayout)
        mTxtLimit = v?.findViewById(R.id.txt_text_limit)
        mTxtLimitWarning = v?.findViewById(R.id.txt_warn_text_limit)
        mHelpText= v?.findViewById(R.id.help)

        mTxtTitle?.setText(builder?.title?: "")

        if (MMConstants.HideDescripton) {
            mDescSection?.visibility = View.GONE
        } else {
            mDescSection?.visibility = View.VISIBLE
            mTxtDesc?.setText(builder?.description?: "")
        }

        // Preference
        context?.let {
            mPref = PreferenceManager.getDefaultSharedPreferences(it)
        }
    }

    /**
     * When the fragment is brought up to the user, the attributes are re-populated in case a new category is selected
     */
    override fun onViewBroughtUp() {
        // Re-populate all attributes and the views
        mAttributesView?.removeAllViews()
        mAttributeList?.clear()

        builder?.let { b ->
            if (!b.category.isValid()) {
                mTxtTitleLayout?.visibility = View.GONE
                mDescSection?.visibility = View.GONE
                context?.let { c ->
                    AccessibilityUtil.announce(c, getString(R.string.warn_choose_category))
                }
            } else {
                if (b.category.hasTitle) {
                    mTxtTitleLayout?.visibility = View.VISIBLE
                } else {
                    mTxtTitleLayout?.visibility = View.GONE
                }

                if (MMConstants.HideDescripton) {
                    mDescSection?.visibility = View.GONE
                } else {
                    mDescSection?.visibility = View.VISIBLE
                }

                if (b.description.trim().isEmpty() && !bFirstTimeLoaded) {
                    mTxtLimitWarning?.text = getString(R.string.warn_desc_empty)
                } else {
                    mTxtLimitWarning?.text = ""
                }
            }

            context?.let { c ->
                mHelpText?.let { AccessibilityUtil.announce(c, it.text.toString()) }
            }

            populateAttributes(b.category)
        }
    }

    /**
     * This method populates the attributes in a form from a given Category. It handles:
     * - Creating the proper widget (spinner, edittext, checkboxes, etc.) based on the attribute type
     * - Filling in saved values such as email
     * - Filling in the previously filled fields.
     *
     * @param cat Category
     */
    private fun populateAttributes(cat: Category) {
        builder?.let { b ->
            context?.let { ctx ->
                if (!cat.isValid()) {
                    mHelp?.text = getString(R.string.warn_choose_category)
                    mHelp?.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_drwtext_warn, 0, 0, 0)
                    return
                }

                // Get the extras which contains values of the previously filled-in attributes.
                // The attribute values were saved in a JSON string inside the database with column [de.maengelmelder.mainmodule.database.MMDBConstants.COL_EXTRASJSON]
                val db = MMDB.instance(ctx)
                val extras = db.getExtrasJSON(b.messageId)
                b.attributeValuesFromJson(extras)

                // Hide title if not needed
                if (b.category.hasTitle) {
                    mTxtTitleLayout?.visibility = View.VISIBLE
                } else {
                    mTxtTitleLayout?.visibility = View.GONE
                }

                // Set the help text
                mAttributeList = ArrayList()
                mHelp?.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0)
                mHelp?.text = getString(R.string.edit_form_desc)
                mHelp?.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)

                // get domain settings
                db.getDomain(cat.domainId)?.let { dom ->
                    mDescTextLimit = try { dom.settings[Domain.DescTextLimit].toString().toInt() } catch (e: Exception) { -1 }
                    mDescTextWarning = dom.settings[Domain.DescTextLimitWarning]?.toString()?: ""
                }
                if (mDescTextLimit != -1) {
                    mTxtDescInputLayout?.run {
                        isCounterEnabled = true
                        counterMaxLength = mDescTextLimit
                    }
                    mTxtDesc?.run {
                        filters = arrayOf(InputFilter.LengthFilter(mDescTextLimit))
                        addTextChangedListener(mDescTextWatcher)
                    }
                }

                // get attributes
                val attrs = db.getAttributesByCategoryId(cat.generateId())
                // val attrs = arrayOf<Attribute>() // For testing API, uncomment this
                if (attrs.isEmpty()) {
                    // If the attributes from the DB is empty for some reason, we call category API
                    val loadingDialog = LoadingDialog(ctx, getString(R.string.dialog_loading))
                    loadingDialog.show()
                    val categoryAPI = MMv1Category(ctx, b.category.domainId.toInt(), b.category.typeId.toString())
                    categoryAPI.listener = object: MMBMS.BMSListener<Category?, BaseResponse> {
                        override fun onData(data: Category?) {
                            loadingDialog.dismiss()
                            if (data != null) {
                                doPopulateAttributesInForm(ctx, data.getAttributes())
                            }
                        }
                        override fun onFail(err: BaseResponse) {
                            loadingDialog.dismiss()
                        }
                    }
                    categoryAPI.execute()
                } else {
                    doPopulateAttributesInForm(ctx, attrs)
                }

            }

            bFirstTimeLoaded = false
        }
    }

    private fun doPopulateAttributesInForm(ctx: Context, attrs: Array<Attribute>) {
        builder?.category?.setAttribute(attrs)

        // Iterate the attributes to identify which [View] type is needed
        // Only show attributes allowed in message
        val attrLocalIds = arrayListOf<Int>()
        val category = builder?.category
        val userCred = UserData.getUserCred(ctx)
        category?.iterateAttributes { attr ->
            val localId = attr.localId.toInt()
            if ((category.attrIdsMessage.isEmpty() || category.attrIdsMessage.contains(localId)) && !attrLocalIds.contains(localId)) {
                val view: BaseAttributeView<Any>? = when (attr.type) {
                    Attribute.TYPE_TEXT ->
                        TextAttributeView(ctx, attr).setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                    Attribute.TYPE_TEXTAREA ->
                        TextAreaAttributeView(ctx, attr)
                    Attribute.TYPE_EMAIL ->
                        TextAttributeView(ctx, attr).setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                    Attribute.TYPE_CHECKBOX ->
                        CheckboxAttributeView(ctx, attr)
                    Attribute.TYPE_VALUELIST ->
                        SpinnerAttributeView(ctx, attr)
                    else -> null
                }
                view?.setTitle(attr.name ?: "")
                view?.setHint(attr.helpText ?: "")

                var alreadyhasDefault = false
                // Fill in the previous value if any
                attr.localId.let { id ->
                    var prevValue = builder?.getAttributeValue(id)
                    if (prevValue is String) {
                        prevValue = prevValue.trim()
                    }
                    // Either one or zero
                    // Usually comes from QR Code and saved to attribute values
                    if (prevValue is Int && attr.type == Attribute.TYPE_CHECKBOX) {
                        prevValue = prevValue > 0
                    }
                    if (prevValue != null) {
                        view?.setValue(prevValue)
                        alreadyhasDefault = true
                    }
                }

                // If the attribute should be cached, it should have saved the value from previous messages with the same field
                // E.g. If the user has previously filled an email field in a previous message, it will be loaded here
                // select fields should store its value in cache in a form of semicolon-separated string
                if (attr.shouldCache && mPref != null && !alreadyhasDefault) {
                    val saved: Any? = if (attr.type == Attribute.TYPE_VALUELIST) {
                        val storedVal = mPref?.getString("$PREFIX_ATTR_PREF${attr.code}", null)
                        storedVal?.split(";")?.toTypedArray()
                    } else {
                        mPref?.getString("$PREFIX_ATTR_PREF${attr.code}", null)
                    }
                    if (saved != null) {
                        view?.setValue(saved)
                        alreadyhasDefault = true
                    }
                }

                if (!alreadyhasDefault) {
                    // Check logged in user first and grab their info to fill in first
                    if (userCred?.isUserValid() == true) {
                        when (attr.code) {
                            "first_name" -> userCred.firstname
                            "last_name" -> userCred.lastname
                            "email" -> userCred.email
                            else -> null
                        }?.also { defValue -> view?.setValue(defValue) }
                        alreadyhasDefault = true
                    }
                }

                // Fixed default value from MMConstants, if it is still empty
                if (!alreadyhasDefault) {
                    view?.let { v -> assignConstantDefaultValueForAttribute(attr, v) }
                }

                // Set if the field should be disabled or not
                // Case with QR Code
                val readonly = builder?.isAttributeForced(attr.localId.toLong()) == true
                if (attr.type == Attribute.TYPE_CHECKBOX && attr.required && !(view?.getValue() as Boolean)) {
                    // if it's a checkbox, required and the default value is false, we cannot force it to be readonly
                    // as user has to check it in order to submit the message
                    view.toggleReadOnly(false)
                } else {
                    view?.toggleReadOnly(readonly)
                }

                // Toggle errors when it is not filled in, but required
                if (!bFirstTimeLoaded) {
                    view?.let { v ->
                        if (!v.hasValue() && attr.required) {
                            v.toggleError(true, attr.errorText?: "")
                        } else {
                            v.toggleError(false, "")
                        }
                    }
                }

                // Show the view to the user
                view?.run {
                    mAttributesView?.addView(getInflatedView())
                    mAttributeList?.add(this)
                }

                attrLocalIds.add(localId)
            }
        }
    }
    /**
     * Assign a default value from the [MMConstants.DefaultValuesMap]. This method should be called
     * after making sure that no values are found from cache/builder
     *
     * @param attr [Attribute]
     * @param v instance of [BaseAttributeView]
     */
    private fun assignConstantDefaultValueForAttribute(attr: Attribute, v: BaseAttributeView<Any>) {
        when (attr.code) {
            "first_name" -> MMConstants.DefaultValuesMap[MMConstants.DefaultValues.FormFirstName]
            "last_name" -> MMConstants.DefaultValuesMap[MMConstants.DefaultValues.FormLastName]
            "email" -> MMConstants.DefaultValuesMap[MMConstants.DefaultValues.FormEmail]
            else -> null
        }?.also { defValue -> v.setValue(defValue) }
    }

    /**
     * Check whether the form is completely filled out by the user. It checks for description and
     * all required attributes
     *
     * @return true if it is complete, false otherwise
     */
    private fun isFormComplete(): Boolean {
        if (builder?.isCategoryValid() == false) {
            // If category is not picked yet, we should not prompt the user since no attributes will be shown anyway
            return true
        }

        // Check for description
        val desc = (mTxtDesc?.text?.toString()?: "").trim()
        if (!MMConstants.HideDescripton && desc.isNotEmpty()) {
            return false
        }
        // Check for required attributes
        mAttributeList?.forEach { v ->
            if (!v.isAttributeFilledOut()) return false
        }

        return true
    }

    override fun isLoading(): Boolean = false

    // This fragment is complete when [isFormComplete] returns true and user has chosen a category
    override fun isStepComplete(): Boolean = builder?.isCategoryValid() == true && isFormComplete()

    override fun getTitle(): String = mContext?.getString(R.string.step_edit_attributes)?: ""

    // Prompt the user if the form is not yet complete before changing to another fragment or closing the message
    override fun shouldPromptBeforeChange(): Boolean = !isFormComplete()

    @RequiresApi(Build.VERSION_CODES.N)
    override fun executeBeforeChange() {
        // Cache the attribute values that should be cached (required)
        val edit = mPref?.edit()
        mAttributeList?.forEach { attrView ->
            attrView.getAttrib()?.run {
                var value = attrView.getValue()
                if (code == "email") {
                    // Remove any empty spaces from email field
                    value = value.toString().replace(" ", "")
                }
                if (attrView.getType() == Attribute.TYPE_VALUELIST) {
                    val selectFieldValues = try { value as Array<String> } catch (e: Exception) { arrayOf() }
                    if (attrView.getAttrib()?.multiselect == false) {
                        if (selectFieldValues[0] == "-1") selectFieldValues[0] = ""
                    }
                }
                if (shouldCache) {
                    if (value is Array<*>) {
                        edit?.putString("$PREFIX_ATTR_PREF$code", value.joinToString(";"))
                    } else {
                        edit?.putString("$PREFIX_ATTR_PREF$code", value.toString())
                    }
                }
                attrView.toggleError(required && !attrView.isAttributeFilledOut())
                builder?.addAttributeValue(key = localId, value = value)
            }
        }
        edit?.apply()

        builder?.let { b ->
            // Set the filled in title and desc
            b.title = (mTxtTitle?.text?.toString()?: getString(R.string.default_msg_title)).trim()
            b.description = (mTxtDesc?.text?.toString()?: getString(R.string.default_msg_description)).trim()

            val msgId = b.messageId

            // Save the filled out form as JSON string, along with title and desc in different columns
            context?.let { ctx ->
                val db = MMDB.instance(ctx)
                val json = b.getAttributesAsJson()
                db.updateMessage(msgId,
                        db.constants.COL_EXTRASJSON to json.toString(),
                        db.constants.COL_TITLE to b.title,
                        db.constants.COL_DESC to b.description)
            }
        }
    }

    // Prompt the user that there are several required fields that have not been filled out.
    override fun promptBeforeChange(f: (Boolean) -> Unit) {
        val attrNotFilledOut = mAttributeList?.find { x -> !x.isAttributeFilledOut() }
        if (attrNotFilledOut !== null || builder?.isDescriptionValid() == false) {
            activity?.let { a ->
                val b = AlertDialog.Builder(a)
                    .setTitle(R.string.warn_form_incomplete_title)
                    .setMessage(R.string.warn_form_incomplete_content)
                    .setPositiveButton(R.string.ok) { dialog: DialogInterface, which: Int ->
                        f(true)
                    }
                b.show()
            }
        }
    }

    /**
     * Instance of [TextWatcher] that listens when the number of characters in the description box is over the limit.
     * It will disable itself from further entering more characters and show a warning limit
     */
    private val mDescTextWatcher = object: TextWatcher {
        override fun afterTextChanged(s: Editable?) { }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            if (mDescTextLimit == -1) return
            mTxtDesc?.run {
                if (length() >= mDescTextLimit) {
                    // Limit reached
                    if (mTxtLimitWarning?.visibility != View.VISIBLE) mTxtLimitWarning?.visibility = View.VISIBLE
                    mTxtLimitWarning?.text = mDescTextWarning
                } else {
                    if (mTxtLimitWarning?.visibility != View.GONE) mTxtLimitWarning?.visibility = View.GONE
                    mTxtLimitWarning?.text = ""
                }
            }

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
    }
}