package com.pydio.android.cells.reactive

import android.content.SharedPreferences
import android.util.Log
import io.reactivex.rxjava3.subjects.PublishSubject

/**
 * Create a live data object from the shared preferences that can be observed by the UI components.
 * It uses RxJava under the hood.
 */
class LiveSharedPreferences(private val sharedPrefs: SharedPreferences) {

    private val logTag = LiveSharedPreferences::class.simpleName
    private val updateSubject = PublishSubject.create<String>()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        Log.d(logTag, "- Shared pref change event: $key")
        updateSubject.onNext(key)
    }

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
}
