package com.pydio.android.cells.reactive

import android.content.SharedPreferences
import android.util.Log
import com.pydio.android.cells.ui.core.ListLayout
import io.reactivex.rxjava3.subjects.PublishSubject

/**
 * Create a live data object from the shared preferences that can be observed by the UI components.
 * It uses RxJava under the hood.
 */
class LiveSharedPreferences(private val sharedPrefs: SharedPreferences) {

    private val logTag = "LiveSharedPreferences"
    private val updateSubject = PublishSubject.create<String>()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        Log.d(logTag, "- Shared pref change event: $key")
        if (key != null) {
            updateSubject.onNext(key)
        }
    }

// We cannot simply retrieve a pref from its key if we do not know its type
// TODO do it cleverly
//    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
//        Log.d(logTag, "- Shared pref change event: $key - ${prefs.getString(key, "")}")
//        updateSubject.onNext(key)
//    }

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun getBoolean(key: String, defaultValue: Boolean?) =
        LivePreference(updateSubject, sharedPrefs, key, defaultValue)

    fun getString(key: String, defaultValue: String?) =
        LivePreference(updateSubject, sharedPrefs, key, defaultValue)

    fun getInt(key: String, defaultValue: Int?) =
        LivePreference(updateSubject, sharedPrefs, key, defaultValue)

    fun getLong(key: String, defaultValue: Long?) =
        LivePreference(updateSubject, sharedPrefs, key, defaultValue)

    fun getFloat(key: String, defaultValue: Float?) =
        LivePreference(updateSubject, sharedPrefs, key, defaultValue)

    fun getLayout(key: String, defaultValue: ListLayout) =
        MappedLivePref(
            updateSubject,
            sharedPrefs,
            key,
            defaultValue.name,
            transform = { strValue ->
                val newValue = try {
                    ListLayout.valueOf(strValue)
                } catch (e: IllegalArgumentException) {
                    ListLayout.LIST
                }
                newValue
            }
        )
}
