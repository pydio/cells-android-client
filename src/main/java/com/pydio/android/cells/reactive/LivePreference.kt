package com.pydio.android.cells.reactive

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

/** A live preference that can be observed, using RxJava */
class LivePreference<T> constructor(
    private val updates: Observable<String>,
    private val preferences: SharedPreferences,
    private val key: String,
    private val defaultValue: T?
) : MutableLiveData<T>() {

    // private val logTag = LivePreference::class.simpleName

    private var disposable: Disposable? = null
    private var lastValue: T? = null

    init {
        lastValue = (preferences.all[key] as T) ?: defaultValue
        value = lastValue
    }

    override fun onActive() {
        super.onActive()

        if (lastValue != preferences.all[key] && preferences.all[key] != null) {
            lastValue = preferences.all[key] as T
            postValue(lastValue)
        }

        disposable = updates
            .filter { t -> t == key }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { postValue((preferences.all[it] as T) ?: defaultValue) }
    }

    override fun onInactive() {
        super.onInactive()
        disposable?.dispose()
    }
}
