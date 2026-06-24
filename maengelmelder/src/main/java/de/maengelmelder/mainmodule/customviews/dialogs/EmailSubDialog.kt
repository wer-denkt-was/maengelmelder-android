package de.maengelmelder.mainmodule.customviews.dialogs

import android.app.Dialog
import android.content.Context
import androidx.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import de.maengelmelder.mainmodule.R
import java.util.regex.Pattern

/**
 *
 * Dialog used for email subscription of a created message.
 */
class EmailSubDialog(c: Context, onResolve: (Boolean, String?) -> Unit) : Dialog(c) {

    /**
     * Generated dialog view
     */
    private var mView: View

    /**
     * Whether the user wants to subscribe to updates regarding his created message
     */
    private var mCheckSub: CheckBox

    /**
     * Description text
     */
    private var mTxtDesc: TextView

    /**
     * Email field
     */
    private var mEmailField: EditText

    /**
     * Button to submit the message along with subscription email
     */
    private var mSendMessage: Button

    /**
     * Pattern to identify whether the entered text is an email address
     */
    private val mEmailPattern = Pattern.compile(
            "[äüößa-zÄÜÖA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
            "\\@" +
            "[äüößa-zÄÜÖA-Z0-9][äüößa-zÄÜÖA-Z0-9\\-]{0,64}" +
            "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+")

    init {
        // Set dialog title and option
        setTitle(R.string.email_sub)
        setCancelable(false)

        // inflate layout
        mView = LayoutInflater.from(context).inflate(R.layout.mm_dialog_emailsub, null)
        setContentView(mView)

        // get widgets
        mTxtDesc = mView.findViewById(R.id.desc)
        mEmailField = mView.findViewById(R.id.email)
        mSendMessage = mView.findViewById(R.id.send_message)
        mCheckSub = mView.findViewById(R.id.subscribe_check)

        // Set default email value
        val sp = PreferenceManager.getDefaultSharedPreferences(c)
        val email = sp.getString("attrib-email", null)
        if (email != null) {
            mEmailField.setText(email)
        }

        // Set listener
        mCheckSub.setOnCheckedChangeListener { _, b ->
            mEmailField.visibility = if (b) View.VISIBLE else View.GONE
        }
        mSendMessage.setOnClickListener { _ ->
            if (mCheckSub.isChecked) {
                if (mEmailPattern.matcher(mEmailField.text.toString()).matches()) {
                    onResolve(true, mEmailField.text.toString())
                    cancel()
                } else {
                    mEmailField.error = context.getString(R.string.error_email_invalid)
                }
            } else {
                onResolve(false, null)
                cancel()
            }
        }
    }

    /**
     * Overwrite the description part of the dialog
     */
    fun setDescription(desc: String) {
        mTxtDesc.text = desc
    }

    /**
     * Set the content of the email field
     */
    fun setEmail(email: String) {
        mEmailField.setText(email)
    }

    /**
     * Force the dialog to require user to enter the email (basically forcing to subscribe)
     */
    fun forceEmailInput() {
        mCheckSub.isChecked = true
        mCheckSub.visibility = View.GONE
        mEmailField.visibility = View.VISIBLE
    }

}