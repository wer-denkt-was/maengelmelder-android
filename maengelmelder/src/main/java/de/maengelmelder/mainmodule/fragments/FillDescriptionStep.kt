package de.maengelmelder.mainmodule.fragments

import android.view.View
import android.widget.TextView
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.database.MMDB

/**
 * This fragment contains both fields for title and description.
 * This fragment is deprecated. You should use [FillAttributesStep] instead
 */
@Deprecated("FillAttributesStep has included the content of this fragment")
class FillDescriptionStep : BaseMessageStepFragment() {

    private var mTitle: TextView? = null
    private var mDesc: TextView? = null

    override fun getLayoutId(): Int = R.layout.mm_fragment_fill_desc

    override fun onViewInflated(v: View?) {
        mTitle = v?.findViewById(R.id.title)
        mDesc = v?.findViewById(R.id.description)

        mTitle?.text = builder?.title
        mDesc?.text = builder?.description
    }

    override fun isLoading(): Boolean = false

    override fun isStepComplete(): Boolean = builder?.description != ""

    override fun getTitle(): String = getString(R.string.step_check_duplicates)

    override fun shouldPromptBeforeChange(): Boolean = false

    override fun executeBeforeChange() {
        builder?.let { b ->

            // save content and show errors
            b.title = mTitle?.text.toString()
            b.description = mDesc?.text.toString()
            val msgId = b.messageId

            // Save the filled out form as JSON string
            context?.let { ctx ->
                val db = MMDB.instance(ctx)
                db.updateMessage(msgId,
                        db.constants.COL_DESC to (b.description),
                        db.constants.COL_TITLE to (b.title))
            }
        }

    }

    override fun promptBeforeChange(f: (Boolean) -> Unit) { }
}