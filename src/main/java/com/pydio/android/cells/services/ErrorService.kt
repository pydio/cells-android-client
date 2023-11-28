package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.ui.models.ErrorMessage
import com.pydio.android.cells.ui.models.fromException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Holds a shared flow of errors to notify the end user.
 */
class ErrorService(
    coroutineService: CoroutineService,
) {
    private val logTag = "ErrorService"
    private val uiScope = coroutineService.cellsUiScope

    // Expose a flow of error messages for the end-user.
    private val _allMessages = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: Flow<ErrorMessage?> = _allMessages

    // TODO make this variable
    private val errorDebounceDelay = 800L

    // We debounce the error messages to avoid saturating the snack-bar
    @OptIn(FlowPreview::class)
    private var _userMessages: Flow<ErrorMessage?> = _allMessages.debounce(errorDebounceDelay)

    // We rather use a shared flow to be able to see messages only once
    // otherwise, each view model will show latest error message when starting to listen
    // TODO always emit null in the errorMessages after a 5secs time out so that same error is displayed anew e.G if the user takes the same steps
//    val userMessages: SharedFlow<ErrorMessage?> = _userMessages.onEach {
//        Log.e(logTag, "... Received error message: ${it.toString()}")
//    }.shareIn(
//        scope = uiScope,
//        started = SharingStarted.WhileSubscribed(30000),
//        replay = 1
//    )

    val userMessages: StateFlow<ErrorMessage?> = _allMessages.stateIn(
        scope = uiScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun appendError(errorMsg: ErrorMessage? = null) {
        _allMessages.value = errorMsg
    }

    fun appendError(e: Exception) {
        _allMessages.value = fromException(e)
    }

    fun appendError(msg: String) {
        Log.e(logTag, ".... Append error $msg")
        _allMessages.value = ErrorMessage(msg, -1, listOf())
    }

    suspend fun asyncAppendError(msg: String) = withContext(uiScope.coroutineContext) {
        Log.e(logTag, ".... Append error $msg")
        _allMessages.value = ErrorMessage(msg, -1, listOf())
    }

    fun clearStack() {
        _allMessages.value = null
    }

    init {
        Log.i(logTag, "### ErrorService initialised")
    }
}
