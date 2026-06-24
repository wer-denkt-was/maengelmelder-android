package de.maengelmelder.mainmodule.activities

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import de.maengelmelder.mainmodule.R
import de.maengelmelder.mainmodule.fragments.SettingsFragment
import io.github.inflationx.viewpump.ViewPumpContextWrapper

/**
 * Settings activity that uses [SettingsFragment]
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        const val REQ_CODE = 240
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mm_activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.beginTransaction().replace(R.id.content, SettingsFragment()).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return true
    }
}