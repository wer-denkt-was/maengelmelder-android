package de.maengelmelder.mainmodule.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.Serializable

/**
 * Permissions, activity contracts, etc. (Since SDK 30)
 */
object ActivityUtil {

    /**
     * This is meant to replace deprecated startActivityForResult method
     * IMPORTANT: Only call this method in onCreate, as activity contract needs to be initialized before activity lifecycle is running
     */
    fun startActivityForResult(
            ca: ComponentActivity,
            onResult: (ActivityResult) -> Unit) : ActivityResultLauncher<Intent> {
        return ca.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onResult(it)
        }
    }
    /**
     * This is meant to replace deprecated startActivityForResult method (fragment)
     * IMPORTANT: Only call this method in onCreate, as activity contract needs to be initialized before activity lifecycle is running
     */
    fun startActivityForResult(
            fa: Fragment,
            onResult: (ActivityResult) -> Unit) : ActivityResultLauncher<Intent> {
        return fa.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onResult(it)
        }
    }

    /**
     * ActivityCompat.requestPermission is deprecated, so use the updated method for single permission
     */
    fun requestPermission(ca: ComponentActivity, onResult: (Boolean) -> Unit) : ActivityResultLauncher<String> {
        return ca.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            onResult(it)
        }
    }

    /**
     * ActivityCompat.requestPermission is deprecated, so use the updated method for single permission in fragment
     */
    fun requestPermission(fa: Fragment, onResult: (Boolean) -> Unit) : ActivityResultLauncher<String> {
        return fa.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            onResult(it)
        }
    }

    /**
     * ActivityCompat.requestPermission is deprecated, so use the updated method for multiple permissions
     */
    fun requestPermissions(ca: ComponentActivity, onResult: (Map<String, Boolean>) -> Unit) : ActivityResultLauncher<Array<String>> {
        return ca.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            onResult(it)
        }
    }

    /**
     * Checks whether permission is granted
     */
    fun isPermissionGranted(c: Context, perm: String): Boolean {
        return ContextCompat.checkSelfPermission(c, perm) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Run code on UI Thread
     */
    fun runOnUiThread(c: Context, f: Context.() -> Unit) {
        if (Looper.getMainLooper() === Looper.myLooper()) {
            f(c)
        } else {
            val h = Handler(Looper.getMainLooper())
            h.post {
                f(c)
            }
        }
    }

    /**
     * Get serializable intent data
     */
    fun<T: Serializable?> getIntentSerializeableExtra(intent: Intent, key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(key) as T?
        }
    }

}