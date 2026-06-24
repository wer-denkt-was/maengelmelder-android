package de.maengelmelder.mainmodule.activities

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.databinding.MmActivityProfileBinding
import de.maengelmelder.mainmodule.network.MMBMS
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Logout
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import de.maengelmelder.mainmodule.utils.UserData
import de.maengelmelder.mainmodule.utils.images.ImageManipulator
import io.github.inflationx.viewpump.ViewPumpContextWrapper

class ProfileActivity : AppCompatActivity() {

    companion object {
        const val REQ_CODE = 283
    }

    private lateinit var mBinding: MmActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = MmActivityProfileBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get saved user credentials
        val uc = UserData.getUserCred(this)
        if (uc == null || !uc.isUserValid()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        // Display user's avatar
        if (uc.avatarUri.isNotEmpty()) {
            ImageManipulator.setImage(this, mBinding.profileImg, uc.avatarUri, R.drawable.upload_notif, R.drawable.add_photo)
        }

        // Display name and email
        with (mBinding) {
            profileName.apply {
                text = uc.publicName
                contentDescription = uc.publicName
            }
            profileEmail.apply {
                text = uc.email
                contentDescription = uc.email
            }

            val contentDescriptor = getString(R.string.name) + ": " + uc.publicName +
                    ", " + getString(R.string.email) + ": " + uc.email
            profileLayout.contentDescription = contentDescriptor

            // Logout button
            btnLogout.setOnClickListener {
                it.isEnabled = false
                MMv1Logout(this@ProfileActivity).apply {
                    listener = (object : MMBMS.BMSListener<BaseResponse, BaseResponse> {
                        override fun onData(data: BaseResponse) {
                            completeLogoutAndFinish()
                        }

                        override fun onFail(err: BaseResponse) {
                            completeLogoutAndFinish()
                        }
                    })
                }.execute()
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    private fun completeLogoutAndFinish() {
        mBinding.btnLogout.isEnabled = true
        UserData.removeUserCred(this)
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }

        return true
    }

}