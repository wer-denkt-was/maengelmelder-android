package de.maengelmelder.mainmodule.customviews.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.objects.Category

/**
 * Shows dialog that [Category] is an external category
 */
class ExternalCategoryPromptDialog(private val c: Context,
                                   private val activity: Activity,
                                   private val cat: Category) : Dialog(c) {

    init {
        setContentView(R.layout.mm_dialog_ext_cat_prompt)

        findViewById<TextView>(R.id.cat_title)?.let { txtTitle ->
            txtTitle.text = cat.displayedName
        }
        findViewById<TextView>(R.id.cat_desc)?.let { txtDesc ->
            if (cat.description.isNotEmpty()) {
                txtDesc.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                txtDesc.text = cat.description
                txtDesc.visibility = View.VISIBLE
            } else {
                txtDesc.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                txtDesc.visibility = View.GONE
            }
        }
        val isMail = cat.isExternalMail
        val target = if (isMail) mailAddress(cat.externalURL) else cat.externalURL ?: ""

        findViewById<TextView>(R.id.cat_ext_prompt)?.let { txtPrompt ->
            txtPrompt.text = if (isMail)
                c.getString(R.string.dialog_ext_category_mail_content, cat.displayedName, target)
            else
                c.getString(R.string.dialog_ext_category_content, cat.displayedName, target)
        }

        findViewById<Button>(R.id.btn_go_ext)?.let { btnExt ->
            btnExt.contentDescription = if (isMail)
                c.getString(R.string.acc_cd_external_category_mail_go, target)
            else
                c.getString(R.string.acc_cd_external_category_go, target)
            btnExt.setOnClickListener {
                if (openExternalUri(isMail)) {
                    activity.finishAffinity()
                } else {
                    dismiss()
                    showError(isMail, target)
                }
            }
        }

        findViewById<Button>(R.id.btn_cancel)?.let { btnCancel ->
            btnCancel.setOnClickListener { cancel() }
        }
    }

    /**
     * Opens the external uri of the category. Supports http(s) and mailto links (incl. parameters like subject/body).
     * @return false if no app could handle the uri
     */
    private fun openExternalUri(isMail: Boolean): Boolean {
        val raw = cat.externalURL?.trim() ?: return false
        // Enriched links (e.g. mailto:a@b.de?subject=Bäche melden) may contain characters which have to be encoded first.
        // Reserved characters and already encoded sequences (%) are kept.
        val uri = Uri.parse(Uri.encode(raw, ":/?#[]@!$&'()*+,;=%~"))
        val intent = Intent(if (isMail) Intent.ACTION_SENDTO else Intent.ACTION_VIEW, uri)
        return try {
            c.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private fun showError(isMail: Boolean, target: String) {
        AlertDialog.Builder(c)
                .setMessage(c.getString(
                        if (isMail) R.string.dialog_ext_category_mail_error else R.string.dialog_ext_category_url_error,
                        target))
                .setPositiveButton(R.string.dialog_ok2, null)
                .show()
    }

    companion object {
        /**
         * Extracts the (decoded) mail address(es) of a mailto link, e.g. "mailto:a@b.de?subject=x" -> "a@b.de"
         */
        fun mailAddress(mailto: String?): String {
            val trimmed = mailto?.trim() ?: return ""
            val withoutScheme = if (trimmed.lowercase().startsWith("mailto:")) trimmed.substring(7) else trimmed
            return Uri.decode(withoutScheme.substringBefore('?'))
        }
    }
}
