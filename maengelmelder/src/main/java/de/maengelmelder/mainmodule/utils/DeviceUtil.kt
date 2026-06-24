package de.maengelmelder.mainmodule.utils

import android.app.Activity
import android.content.Context
import android.location.LocationManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Manage device's properties
 */
object DeviceUtil {

    /**
     * Check whether the user's location can be derived from the device
     *
     * @param c context
     * @return true / false
     */
    fun isGPSEnabled(c: Context): Boolean {
        val lm = c.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Hide soft keyboard from the view
     */
    fun hideSoftKeyboard(c: Context, focusedView: View?) {
        val inpMgr = c.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        focusedView?.let { v ->
            inpMgr.hideSoftInputFromInputMethod(v.windowToken, 0)
        }
    }

    /**
     * Start QR scanning activity (will open camera for it)
     *
     * NOTE: Since this uses Google's MLKit, it will download barcode_ui module during App installation
     * If it fails to load, it is possible that the module failed to download
     * The quick fix is to clear Google Play store cache from App settings.
     * This usually doesn't happen in actual Android devices (only in emulators mostly)
     *
     * @param c Context object
     * @return Task of type [Barcode]
     */
    fun scanQR(c: Context): Task<Barcode> {
        val opt = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = GmsBarcodeScanning.getClient(c, opt)
        return scanner.startScan()
    }

}