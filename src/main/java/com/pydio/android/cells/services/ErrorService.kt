package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.ui.models.ErrorMessage
import com.pydio.android.cells.ui.models.fromException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.shareIn

/**
 * Holds a shared flow of errors to notify the end user.
 */
class ErrorService(
    coroutineService: CoroutineService,
) {
    private val logTag = "ErrorService"

    private val serviceScope = coroutineService.cellsIoScope

    // Expose a flow of error messages for the end-user.
    private val _allMessages = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: Flow<ErrorMessage?> = _allMessages

    // TODO make this variable
    private val errorDebounceDelay = 800L

    // We debounce the error messages to avoid saturating the snackbar
    @OptIn(FlowPreview::class)
    private var _userMessages: Flow<ErrorMessage?> = _allMessages.debounce(errorDebounceDelay)

    //    val userMessages: StateFlow<ErrorMessage?> = _userMessages.stateIn(
//        scope = serviceScope,
//        started = SharingStarted.WhileSubscribed(5000),
//        initialValue = null
//    )

    // We rather use a shared flow to be able to see messages only once
    // otherwise, each view model will show latest error message when starting to listen
    val userMessages: SharedFlow<ErrorMessage?> = _userMessages.buffer(0).shareIn(
        scope = serviceScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 0
    )

    fun appendError(errorMsg: ErrorMessage? = null) {
        _allMessages.value = errorMsg
    }

    fun appendError(e: Exception) {
        _allMessages.value = fromException(e)
    }

    fun appendError(msg: String) {
        _allMessages.value = ErrorMessage(msg, -1, listOf())
    }

    fun clearStack() {
        _allMessages.value = null
    }

    init {
        Log.i(logTag, "### ErrorService initialised")
    }
}
