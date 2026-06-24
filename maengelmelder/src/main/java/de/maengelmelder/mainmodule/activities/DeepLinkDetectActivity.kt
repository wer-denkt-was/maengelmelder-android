package de.maengelmelder.mainmodule.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.MMInitiator
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.databinding.MmActivityDeepLinkBinding
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.collectives.coroutines.APIGetDomains
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1System
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.SystemInfo
import de.maengelmelder.mainmodule.utils.DomainUtil
import de.maengelmelder.mainmodule.utils.ResourceProxy


/**
 * Valid URLs:
 *  - Essen, valid category, but location is outside bound
 *      https://maengelmelder.essen.de/bms/create?lat=50.993597&lon=8.335318&selected_typeid=1437
 *
 *  - Essen, invalid category, correct location
 *      https://maengelmelder.essen.de/bms/create?lat=51.454234&lon=7.022019&selected_typeid=9999
 *
 * -  Essen, valid category, valid location, force category, force attribute input
 *      https://maengelmelder.essen.de/bms/create?lat=51.455156&lon=7.012931&force_typeid=1&selected_typeid=1437&force_attribute517=88889999&force_attribute518=facebook&selected_attribute514=LastNameQR
 *
 */
class DeepLinkDetectActivity : AppCompatActivity() {

    companion object {
        private val BUNDLE_QUERY_PARAMS = "deeplink.query_params"

        fun deepLinkExtras(intent: Intent, qParams: String) {
            intent.putExtra(BUNDLE_QUERY_PARAMS, qParams)
        }

    }

    private lateinit var mBinding: MmActivityDeepLinkBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = MmActivityDeepLinkBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // App config
        MMConstants.load(this)
        title = MMInitiator.getConfig(this, MMInitiator.Config.APP_TITLE)

        val qParams = intent.getStringExtra(BUNDLE_QUERY_PARAMS)?: ""
        ResourceProxy.validateBrowsableUrlQuery(this, qParams) { success, param, error ->
            if (success && param != null) {
                // Category is valid. Launch message creation activity with the correct params
                startMessageCreation(param.lat, param.lon, param.category, qParams)
            } else {
                // Invalid
                showWarningDialog(error)
            }
        }
    }

    fun startMessageCreation(lat: Double, lon: Double, type: Category? = null, qParam: String? = null) {
        val intent = Intent(this, MessageProcessActivity::class.java)
        intent.putExtra(MessageProcessActivity.BUNDLE_SOURCE, MessageProcessActivity.SOURCE_DEEPLINK)
        intent.putExtra(MessageProcessActivity.BUNDLE_INITIAL_LAT, lat)
        intent.putExtra(MessageProcessActivity.BUNDLE_INITIAL_LON, lon)
        type?.let { t -> intent.putExtra(MessageProcessActivity.BUNDLE_INITIAL_CATEGORY, t) }
        intent.putExtra(MessageProcessActivity.BUNDLE_INITIAL_QPARAM, qParam)
        startActivity(intent)
    }

    fun showWarningDialog(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setNegativeButton(R.string.dialog_cancel) { d, which ->
                d.dismiss()
                finishAffinity()
            }.show()
    }
}