package com.pydio.android.cells.ui.fragments

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.pydio.android.cells.AppKeys
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
        val numberPreference: EditTextPreference? = findPreference(AppKeys.METERED_ASK_B4_DL_FILES_SIZE)
        numberPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }
}
