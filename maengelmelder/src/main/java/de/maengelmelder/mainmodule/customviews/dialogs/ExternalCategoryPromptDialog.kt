package de.maengelmelder.mainmodule.customviews.dialogs

import android.app.Activity
import android.app.Dialog
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
        findViewById<TextView>(R.id.cat_ext_prompt)?.let { txtPrompt ->
            txtPrompt.text = c.getString(R.string.dialog_ext_category_content, cat.displayedName, cat.externalURL)
        }

        findViewById<Button>(R.id.btn_go_ext)?.let { btnExt ->
            btnExt.contentDescription = c.getString(R.string.acc_cd_external_category_go, cat.externalURL)
            btnExt.setOnClickListener {
                Intent(Intent.ACTION_VIEW, Uri.parse(cat.externalURL)).also { i ->
                    c.startActivity(i)
                }
                activity.finishAffinity()
            }
        }

        findViewById<Button>(R.id.btn_cancel)?.let { btnCancel ->
            btnCancel.setOnClickListener { cancel() }
        }
    }

}