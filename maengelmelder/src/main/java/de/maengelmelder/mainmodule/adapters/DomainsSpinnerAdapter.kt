package de.maengelmelder.mainmodule.adapters

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import de.maengelmelder.mainmodule.objects.Domain

class DomainsSpinnerAdapter(c: Context, domains: List<Domain>)
    : ArrayAdapter<Domain>(c, android.R.layout.simple_spinner_item, domains) {

    private val mContext = c
    private val mDomains = domains
    private val mDispMetrics = c.resources.displayMetrics
    private val mPadding = (mDispMetrics.density * 10).toInt()

    override fun getCount(): Int = mDomains.size

    override fun getItem(position: Int): Domain? = mDomains[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = (convertView as TextView?)?: TextView(mContext)
        val dom = getItem(position)
        view.text = dom?.name?: ""
        view.setPadding(mPadding, mPadding, mPadding, mPadding)
        return view
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = (convertView as TextView?)?: TextView(mContext)
        val dom = getItem(position)
        val span = SpannableString(dom?.name?: "").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
        }
        view.text = span
        view.setPadding(mPadding, mPadding, mPadding, mPadding)
        return view
    }

}