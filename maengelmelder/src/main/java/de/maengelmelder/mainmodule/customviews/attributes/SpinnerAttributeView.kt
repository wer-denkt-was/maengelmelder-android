package de.maengelmelder.mainmodule.customviews.attributes

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnMultiChoiceClickListener
import android.os.Build
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.objects.Attribute

/**
 * Dropdown attribute form field
 */
class SpinnerAttributeView : BaseAttributeView<Array<String>>, AdapterView.OnItemSelectedListener {

    private var mTitle: TextView? = null
    private var mError: TextView? = null
    private var mSpinner: Spinner? = null
    private var mReadOnly: TextView? = null
    private var mMultiSelLayout: LinearLayout? = null
    private var mMultiSelValues: TextView? = null
    private var mMultiSelBtn: Button? = null
    private var mValues: ArrayList<Pair<String, String>> = ArrayList() // List of all options
    private var mArrayAdapter: ArrayAdapter<String>? = null

    // Holds the list of actual values (not displayed ones) selected by multiselect
    private var mSelectedValuesMultisel: ArrayList<String> = arrayListOf()

    constructor(c: Context, attr: Attribute)
            : super(c, R.layout.mm_attribute_spinner, attr) {
        // View references
        mTitle = view?.findViewById(R.id.title)
        mTitle?.movementMethod = LinkMovementMethod()
        mSpinner = view?.findViewById(R.id.spinner)
        mError = view?.findViewById(R.id.error)
        mReadOnly = view?.findViewById(R.id.readonly)
        mReadOnly?.visibility = View.GONE
        // For multiselect
        mMultiSelLayout = view?.findViewById(R.id.multiselect_layout)
        mMultiSelValues = view?.findViewById(R.id.multiselect_values)
        mMultiSelBtn = view?.findViewById(R.id.multiselect_btn)

        if (attr.multiselect) {
            // For multiselect, we use a dialog builder populated with the multiple choices
            mValues = attr.choices
            val choiceValues = attr.choicesValueToStringArray()
            val choiceNames = getDisplayedValues()
            mMultiSelLayout?.visibility = View.VISIBLE
            mSpinner?.visibility = View.GONE
            mMultiSelBtn?.setOnClickListener { v ->
                val boolArray = BooleanArray(choiceNames.size) { idx -> mSelectedValuesMultisel.contains(choiceValues[idx])}
                val dialogBuilder = AlertDialog.Builder(c)
                        .setTitle(attr.helpText)
                        .setMultiChoiceItems(choiceNames, boolArray) { _, which, isChecked ->
                            val realValue = choiceValues[which];
                            if (isChecked) {
                                if (!mSelectedValuesMultisel.contains(realValue))
                                    mSelectedValuesMultisel.add(realValue)
                            } else {
                                mSelectedValuesMultisel.remove(realValue)
                            }
                            mMultiSelValues?.text = getDisplayValue(mSelectedValuesMultisel.toTypedArray())
                        }
                        .setPositiveButton(R.string.dialog_ok2) { dialog, _ ->
                            mMultiSelValues?.text = getDisplayValue(mSelectedValuesMultisel.toTypedArray())
                            dialog.dismiss()
                        }
                dialogBuilder.show()
            }
        } else {
            mSpinner?.visibility = View.VISIBLE
            mMultiSelLayout?.visibility = View.GONE
            // Adds the the first value (-1)
            mValues = attr.choices
            mValues.add(0, Pair("-1", c.getString(R.string.spinner_default_choice)))

            // Setup the adapter
            mArrayAdapter = ArrayAdapter(c, android.R.layout.simple_spinner_item, getDisplayedValues())
            mArrayAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mSpinner?.adapter = mArrayAdapter
            mSpinner?.onItemSelectedListener = this
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val chosen = parent?.selectedItem as String?
        mReadOnly?.text = chosen
    }

    override fun hasValue(): Boolean = getValue().isNotEmpty()

    /**
     * Get an array of values that are displayed in the spinner
     */
    private fun getDisplayedValues(): Array<String> {
        val arr: ArrayList<String> = ArrayList()
        mValues.forEach { pair -> arr.add(pair.second) }
        return arr.toTypedArray()
    }

    /**
     * Get the actual referenced value from the given displayed value
     */
    private fun getRealValue(displayed: Array<String>): Array<String> {
        val list = arrayListOf<String>()
        displayed.forEach { d ->
            mValues.find { p -> p.second == d }?.first?.let { value -> list.add(value) }
        }
        return list.toTypedArray()
    }

    /**
     * Get the display value (the one shown in spinner) from its actual referenced value
     * In case of "-1" (<Bitte wählen>), it will just show "-"
     */
    private fun getDisplayValue(realValue: Array<String>): String {
        return if (attribute?.multiselect == true) {
            val list = arrayListOf<String>()
            realValue.forEach { rv ->
                mValues.find { p -> p.first == rv }?.second?.let { value -> list.add(value) }
            }
            list.joinToString(", ")
        } else {
            if (realValue[0] == "-1") "-" else mValues.find { p -> p.first == realValue[0] }?.second?: "-"
        }
    }

    /**
     * Get the index of the array from the given reference value
     */
    private fun getIndexFromValue(value: String): Int {
        return mValues.indexOfFirst { v -> value == v.first }
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

    override fun toggleReadOnly(toggle: Boolean) {
        mSpinner?.isEnabled = !toggle
        mSpinner?.visibility = if (toggle) View.GONE else View.VISIBLE
        mMultiSelLayout?.visibility = if (toggle) View.GONE else View.VISIBLE
        mReadOnly?.visibility = if (toggle) View.VISIBLE else View.GONE
    }

    override fun setHint(hint: String) { }

    override fun getType(): String = Attribute.TYPE_VALUELIST

    override fun getValue(): Array<String> {
        if (attribute?.multiselect == true) {
            return mSelectedValuesMultisel.toTypedArray()
        } else {
            val chosen = mSpinner?.selectedItem as String
            return getRealValue(arrayOf(chosen))
        }
    }

    override fun setValue(value: Any) {
        val givenValues = try { value as Array<String> } catch (e: Exception) { arrayOf() }
        if (attribute?.multiselect == true) {
            // Assign the value
            mSelectedValuesMultisel = arrayListOf(*givenValues)
            mMultiSelValues?.text = getDisplayValue(givenValues)
        } else {
            val idx = getIndexFromValue(givenValues[0])
            mSpinner?.setSelection(if (idx == -1) 0 else idx)
        }
        mReadOnly?.text = getDisplayValue(givenValues)
    }

    override fun isAttributeFilledOut(): Boolean {
        if (!isRequired()) return true
        val chosenValue = getValue()
        return chosenValue.isNotEmpty()
    }

    override fun toggleError(toggle: Boolean, errText: String) {
        mError?.visibility = if (toggle) View.VISIBLE else View.GONE
        if (toggle) {
            mError?.text = errText
        }
    }
}