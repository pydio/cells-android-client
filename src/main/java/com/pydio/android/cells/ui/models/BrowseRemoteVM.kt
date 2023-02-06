package com.pydio.android.cells.ui.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.utils.BackOffTicker
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private val unknownStateId = StateID("https://example.com")
private val logTag = BrowseRemoteVM::class.simpleName

class BrowseRemoteVM(
    private val accountService: AccountService,
    private val nodeService: NodeService
) : ViewModel() {

    private val viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val backOffTicker = BackOffTicker()
    private var currWatcher: Job? = null

    private val _stateID = MutableStateFlow(StateID(/* serverUrl = */ Transport.UNDEFINED_URL))
    val stateID: StateFlow<StateID> = _stateID.asStateFlow()

    private var _isActive = false
    private val _isLoading = MutableLiveData<Boolean>()
    private val _errorMessage = MutableLiveData<String?>()

    init {
        _isLoading.value = true
    }

//    val stateID: LiveData<StateID>
//        get() = _stateID

    val isLoading: LiveData<Boolean>
        get() = _isLoading

    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun watch(newStateID: StateID) {
        _isLoading.value = true
        pause()
        currWatcher?.cancel()
        _stateID.value = newStateID
        resume()
    }

    fun pause() {
        Log.e(logTag, "... About to pause remote VM, state: ${stateID.value}")
        _isActive = false
    }

    fun resume() {
        if (!_isActive) {
            _isActive = true
            currWatcher = watchFolder()
        }
        backOffTicker.resetIndex()
    }

    // Technical local objects
    private fun watchFolder() = vmScope.launch {
        while (_isActive) {
            doPull()
            val nd = backOffTicker.getNextDelay()
            delay(TimeUnit.SECONDS.toMillis(nd))
            val msg = "... Watching folders at ${stateID.value}"
            if (_isActive) {
                Log.d(logTag, "$msg, next delay: ${nd}s")
            } else {
                Log.d(logTag, "$msg has been stopped, leaving the loop")
            }
        }
    }

    private suspend fun doPull() {
        // TODO clean and add "Cancel feature"
        var result: Pair<Int, String?> = Pair(0, "")
        stateID.value.let {
            if (unknownStateId != it) {
                result = if (Str.empty(it.file)) {
                    accountService.refreshWorkspaceList(it.accountId)
                } else {
                    nodeService.pull(it)
                }
            }
        }

        withContext(Dispatchers.Main) {
            if (Str.notEmpty(result.second)) {
                _errorMessage.value = result.second
                pause()
            }
            if (result.first > 0) { // At least one change => reset backoff ticker
                backOffTicker.resetIndex()
            }
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        viewModelJob.cancel()
        super.onCleared()
    }
}
