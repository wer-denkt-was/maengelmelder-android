package de.maengelmelder.mainmodule.fragments

import android.os.Bundle
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import de.maengelmelder.mainmodule.BuildConfig
import de.maengelmelder.mainmodule.MMConstants
import de.maengelmelder.mainmodule.R

/**
 * The settings is loaded from R.xml.settings.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(p0: Bundle?, p1: String?) {
        setPreferencesFromResource(R.xml.settings, p1)

        if (MMConstants.FeatureSettingsMap[MMConstants.FeatureSetting.ActivityHistory] == false) {
            val key = getString(R.string.mm_prefcat_logs_key)
            val histPref = preferenceScreen.findPreference<PreferenceCategory>(key)
            histPref?.let { preferenceScreen.removePreference(it) }

        }

    }
}