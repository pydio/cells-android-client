package com.pydio.android.cells.reactive


interface LivePreference<T> {}

//
//    /** A live preference that can be observed, using RxJava */
//class LivePreference<T> constructor(
//    private val updates: Observable<String>,
//    private val preferences: SharedPreferences,
//    private val key: String,
//    private val defaultValue: T?,
//) : MutableLiveData<T>() {
//
//    // private val logTag = LivePreference::class.simpleName
//
//    private var disposable: Disposable? = null
//    private var lastValue: T? = null
//
//    init {
//        lastValue = (preferences.all[key] as T) ?: defaultValue
//        value = lastValue
//    }
//
//    override fun onActive() {
//        super.onActive()
//
//        if (lastValue != preferences.all[key] && preferences.all[key] != null) {
//            lastValue = preferences.all[key] as T
//            postValue(lastValue)
//        }
//
//        disposable = updates
//            .filter { t -> t == key }
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe { postValue((preferences.all[it] as T) ?: defaultValue) }
//    }
//
//    override fun onInactive() {
//        super.onInactive()
//        disposable?.dispose()
//    }
//}
//
///** A live preference that can be observed, using RxJava */
//class MappedLivePref<T, V> constructor(
//    private val updates: Observable<String>,
//    private val preferences: SharedPreferences,
//    private val key: String,
//    private val defaultValue: T,
//    private val transform: (T) -> V,
//) : MutableLiveData<V>() {
//
//    // private val logTag = LivePreference::class.simpleName
//
//    private var disposable: Disposable? = null
//    private var lastValue: T
//
//    init {
//        lastValue = (preferences.all[key] as T) ?: defaultValue
//        value = transform(lastValue)
//    }
//
//    override fun onActive() {
//        super.onActive()
//
//        if (lastValue != preferences.all[key] && preferences.all[key] != null) {
//            lastValue = preferences.all[key] as T
//            postValue(transform(lastValue))
//        }
//
//        disposable = updates
//            .filter { t -> t == key }
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe { postValue(transform((preferences.all[it] as T) ?: defaultValue)) }
//    }
//
//    override fun onInactive() {
//        super.onInactive()
//        disposable?.dispose()
//    }
//}
