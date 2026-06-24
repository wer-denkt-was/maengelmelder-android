package de.maengelmelder.mainmodule.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.common.Barcode
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.customviews.dialogs.LoadingDialog
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.utils.DeviceUtil
import de.maengelmelder.mainmodule.utils.DomainUtil
import de.maengelmelder.mainmodule.utils.ResourceProxy
import java.net.URL

class QRCodeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode)
        supportActionBar?.hide()

        /**
         * NOTE: Don't use noHistory=true for this activity since QR code scanning opens another activity
         * Otherwise, this activity with noHistory=true would be killed and the QR Code result would not be returned
         */
        // Start scanning
        DeviceUtil.scanQR(this).addOnSuccessListener { barcode ->
            // Successfully captured barcode
            val qrUrl = barcode.url?.url?: ""
            val url = try { URL(qrUrl) } catch (e: Exception) { null }
            if (url == null) {
                // Empty of malformed URL
                showErrorDialog(getString(R.string.err_qrscan_invalid_url))
            } else {
                if (MMConstants.AllowedDomainHostsForBrowsableUrl.contains(url.host)) {
                    // parse URL, validate message param, and create message from it if they are valid
                    validateMessageParameters(url.query)
                } else {
                    // Host not allowed. Don't create message
                    showErrorDialog(getString(R.string.err_qrscan_invalid_url))
                }
            }
        }.addOnFailureListener { e ->
            showErrorDialog(getString(R.string.err_qrscan_exception, e.message))
            e.printStackTrace()
        }.addOnCanceledListener {
            // Cancelled by user?
            // Just exit the activity
            finish()
        }
    }

    /**
     * Show error dialog
     *
     * @param text text to show
     * @param messageCreationOption if true, also shows button to start message creation
     *
     */
    private fun showErrorDialog(text: String, messageCreationOption: Boolean = false) {
        var builder = AlertDialog.Builder(this)
            .setMessage(text)
            .setNegativeButton(R.string.dialog_cancel) { d, which ->
                d.dismiss()
                // Exit back to overview map
                finish()
            }
        if (messageCreationOption) {
            builder = builder.setPositiveButton(R.string.dialog_continue_create_message) { d, which ->
                d.dismiss()
                // proceed to message creation but without any additional params
                startMessageCreation(true, null, null, null, null)
            }
        }

        builder.show()
    }

    /**
     * Try to validate message parameters from a URL query String
     *
     * @param query URL Query string
     * */
    private fun validateMessageParameters(query: String) {
        // Parse query param
        val loading = LoadingDialog(this, getString(R.string.qr_scan_check_params))
        loading.show()
        ResourceProxy.validateBrowsableUrlQuery(this, query) { success, param, errorMsg ->
            if (success && param != null) {
                // Category is available and valid. create messageprocess intent with the parameters
                startMessageCreation(false, param.lat, param.lon, param.category, query)
            } else {
                showErrorDialog(errorMsg)
            }
            loading.dismiss()
        }
    }

    private fun startMessageCreation(empty: Boolean, lat: Double?, lon: Double?, category: Category?, qParam: String?) {
        val intent = Intent(this, MessageProcessActivity::class.java)
        if (!empty) {
            intent.putExtra(MessageProcessActivity.BUNDLE_INITIAL_LON, lon)
            intent.putExtra(MessageProcessActivity.BUNDLE_INITIAL_LAT, lat)
            intent.putExtra(MessageProcessActivity.BUNDLE_INITIAL_CATEGORY, category)
            intent.putExtra(MessageProcessActivity.BUNDLE_INITIAL_QPARAM, qParam) // Attribute values will be extracted from here
        }
        finish() // finish current activity first to remove it from backstack
        startActivity(intent)
    }
}