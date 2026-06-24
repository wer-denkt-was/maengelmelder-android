package de.maengelmelder.mainmodule.activities

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.databinding.MmActivityTestinfoBinding
import de.maengelmelder.mainmodule.utils.Connectivity
import de.maengelmelder.mainmodule.utils.UserData

/**
 * Only for test environment information.
 */
class TestInfoActivity : AppCompatActivity() {

    private lateinit var mBinding: MmActivityTestinfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = MmActivityTestinfoBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        with (mBinding) {
            testinfoPackagename.text = "$packageName\n"

            val pInfo = packageManager.getPackageInfo(packageName, 0)
            testinfoAppversion.text = "${pInfo.versionName} - ${pInfo.versionCode}\n"

            testinfoDevice.text =
                "Model[${Build.MODEL}]\n${Build.DEVICE} - Brand[${Build.BRAND}]\n${Build.VERSION.CODENAME} - SDK[${Build.VERSION.SDK_INT}]\n"

            testinfoEndpoint.text =
                if (BuildConfig.debug) "${MMConstants.ServerUrl_Test}\n" else "${MMConstants.ServerUrl}\n"

            testinfoUser.text = UserData.getUsername(this@TestInfoActivity)
            testinfoToken.text = UserData.getUserCred(this@TestInfoActivity)?.token ?: ""
        }
        refreshNetworkState()
    }

    private fun refreshNetworkState() {
        val ni = Connectivity.getNetworkType(this)
        if (ni != null) {
            mBinding.testinfoConnection.text = getString(R.string.text_conn,
                    ni.typeName,
                    "(${ni.subtypeName})",
                    ni.detailedState.name) + "\n"
        } else {
            mBinding.testinfoConnection.text = getString(R.string.text_no_conn) + "\n"
        }
    }
}