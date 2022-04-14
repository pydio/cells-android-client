package com.pydio.android.cells.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import com.pydio.cells.utils.Log
import com.pydio.android.cells.R

/** Display XML based settings */
class SettingsFragment : PreferenceFragmentCompat() {

    private val logTag = SettingsFragment::class.simpleName

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.e(logTag, "onCreateView")
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.e(logTag, "onCreatePreferences, rootKey: $rootKey")
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
