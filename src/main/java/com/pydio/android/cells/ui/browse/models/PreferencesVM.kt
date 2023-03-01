package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.reactive.LiveSharedPreferences
import com.pydio.android.cells.services.CellsPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val logTag = OfflineVM::class.simpleName

/** Expose methods to manage preferences */
class PreferencesVM(
    private val prefs: CellsPreferences,
) : ViewModel() {

    private var liveSharedPreferences: LiveSharedPreferences = LiveSharedPreferences(prefs.get())

    private val _sortBy = MutableStateFlow("")
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    init {
        viewModelScope.launch {
            liveSharedPreferences.getString(
                AppKeys.CURR_RECYCLER_ORDER,
                AppNames.DEFAULT_SORT_BY
            ).asFlow().collect {
                if (it != _sortBy.value) {
                    _sortBy.value = it
                }
            }
        }
    }

    fun setSortBy(newSortBy: String) {
        val doUpdate = sortBy.value != newSortBy
        if (doUpdate) {
            prefs.setString(AppKeys.CURR_RECYCLER_ORDER, newSortBy)
        }
    }

//    private val _layout = MutableStateFlow(ListLayout.LIST)
//    val layout: StateFlow<ListLayout> = _layout.asStateFlow()

//
//    private val liveSortBy: MutableLiveData<String> = liveSharedPreferences.getString(
//        AppKeys.CURR_RECYCLER_ORDER,
//        AppNames.DEFAULT_SORT_BY
//    )
//
//    private val liveDisplayType: MutableLiveData<String> = liveSharedPreferences.getString(
//        AppKeys.CURR_RECYCLER_LAYOUT,
//        AppNames.RECYCLER_LAYOUT_LIST
//    )
//
//    private var _oldLayout = prefs.getString(
//        AppKeys.CURR_RECYCLER_LAYOUT,
//        AppNames.RECYCLER_LAYOUT_LIST
//    )

//    fun setListLayout(listLayout: ListLayout) {
//        prefs.setString(AppKeys.CURR_RECYCLER_LAYOUT, listLayout.name)
//    }

}
