package com.pydio.android.cells.ui.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.utils.BackOffTicker
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Central ViewModel when dealing with a user's accounts.
 */
class AccountListVM(
    private val accountService: AccountService,
) : ViewModel() {

    private val logTag = "AccountListVM"
//    private var viewModelJob = Job()
//    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var _sessions = accountService.getLiveSessions()
    val sessions: LiveData<List<RSessionView>>
        get() = _sessions

    private val backOffTicker = BackOffTicker()
    private var _isActive = false
    private var currWatcher: Job? = null

    // Manage UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    init {
        setLoading(true)
    }

    suspend fun getSession(stateID: StateID): RSessionView? {
        return accountService.getSession(stateID)
    }

    fun watch() {
        setLoading(true)
        pause()
        resume()
    }

    fun resume() {
        if (!_isActive) {
            _isActive = true
            currWatcher = watchAccounts()
        }
        backOffTicker.resetIndex()
    }

    fun pause() {
        currWatcher?.cancel()
        _isActive = false
        setLoading(false)
    }

    fun forgetAccount(stateID: StateID) {
        viewModelScope.launch {
            accountService.forgetAccount(stateID)
        }
    }

    suspend fun openSession(stateID: StateID): RSessionView? {
        return accountService.openSession(stateID)
    }

    fun logoutAccount(stateID: StateID) {
        viewModelScope.launch {
            accountService.logoutAccount(stateID)
        }
    }

    // Local helpers
    private fun watchAccounts() = viewModelScope.launch {
        while (_isActive) {
            doCheckAccounts()
            val nd = backOffTicker.getNextDelay()
            Log.d(logTag, "... Watching accounts, next delay: ${nd}s")
            delay(TimeUnit.SECONDS.toMillis(nd))
        }
        Log.i(logTag, "pausing the account watch process")
    }

    private suspend fun doCheckAccounts() {
        val result = accountService.checkRegisteredAccounts()
        withContext(Dispatchers.Main) {
            if (result.second != null) {
                if (backOffTicker.getCurrentIndex() > 0) {
                    // Not optimal, we should rather check the current session status
                    // before launching the poll
                    // We do not display the error message if first
                    _errorMessage.value = result.second
                }
                pause()
            } else if (result.first > 0) {
                Log.e(logTag, "Found ${result.first} change(s)")
                backOffTicker.resetIndex()
            }
            setLoading(false)
        }
    }

    private fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    override fun onCleared() {
        // viewModelJob.cancel()
        super.onCleared()
        Log.i(logTag, "AccountListVM cleared")
    }
}
