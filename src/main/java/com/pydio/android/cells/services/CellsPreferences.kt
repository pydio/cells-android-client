package com.pydio.android.cells.services

import android.content.Context
import androidx.preference.PreferenceManager

class CellsPreferences(context: Context) {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun getPreference(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun setPreference(key: String, value: String) {
        with(sharedPreferences.edit()) {
            putString(key, value)
            apply()
        }
    }
}
