package com.pydio.android.cells.ui.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.utils.BackOffTicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Central ViewModel when dealing with a user's accounts.
 */
class AccountListViewModel(
    private val accountService: AccountService,
) : ViewModel() {

    private val logTag = AccountListViewModel::class.simpleName
    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

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

    private fun watchAccounts() = vmScope.launch {
        while (_isActive) {
            doCheckAccounts()
            val nd = backOffTicker.getNextDelay()
            Log.d(logTag, "... Next delay: $nd")
            delay(TimeUnit.SECONDS.toMillis(nd))
        }
        Log.i(logTag, "paused")
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
                backOffTicker.resetIndex()
            }
            setLoading(false)
        }
    }

    fun resume(resetBackOffTicker: Boolean) {
        Log.i(logTag, "resumed")
        if (!_isActive) {
            _isActive = true
            currWatcher = watchAccounts()
        }
        if (resetBackOffTicker) {
            backOffTicker.resetIndex()
        }
    }

    fun pause() {
        _isActive = false
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}