package com.rdt.kmclient

import android.os.Bundle
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class FragSetting : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d("tag", "----1111----")
        addPreferencesFromResource(R.xml.pref)
        val pref = findPreference<EditTextPreference>("ip_addr")
        pref?.onPreferenceChangeListener = this
        onPreferenceChange(
            pref,
            PreferenceManager.getDefaultSharedPreferences(pref?.context).getString(pref?.key, "")
        )
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        Log.d("tag", "----2222----")
        val value = newValue.toString()
        if (preference is EditTextPreference) {
            preference.setSummary(value)
        }
        return true
    }

}

/* EOF */