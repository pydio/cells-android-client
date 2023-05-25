package com.pydio.android.cells.ui.account

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.ui.core.AbstractCellsVM
import com.pydio.android.cells.ui.models.fromMessage
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
) : AbstractCellsVM() {

    private val logTag = "AccountListVM"

    val sessions = accountService.getLiveSessions()

    private val backOffTicker = BackOffTicker()
    private var _isActive = false
    private var currWatcher: Job? = null

    suspend fun getSession(stateID: StateID): RSessionView? {
        return accountService.getSession(stateID)
    }

    fun watch() {
        setLoading(true)
        pause()
        resume()
    }

    private fun resume() {
        backOffTicker.resetIndex()
        if (!_isActive) {
            _isActive = true
            currWatcher = watchAccounts()
        }
    }

    fun pause() {
        currWatcher?.cancel()
        _isActive = false
        setLoading(false)
        Log.i(logTag, "... Remote **ACCOUNT** watching paused.")
    }

    fun forgetAccount(stateID: StateID) {
        viewModelScope.launch {
            accountService.forgetAccount(stateID.account())
        }
    }

    suspend fun openSession(stateID: StateID): RSessionView? {
        return accountService.openSession(stateID)
    }

    fun logoutAccount(stateID: StateID) {
        viewModelScope.launch {
            accountService.logoutAccount(stateID.account())
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
        // First we check if some token are about to expire
        // accountService.checkRegisteredAccounts()
        // Then we refresh the list
        val result = accountService.checkRegisteredAccounts()
        withContext(Dispatchers.Main) {
            result.second?.let {
                if (backOffTicker.getCurrentIndex() > 0) {
                    // Not optimal, we should rather check the current session status
                    // before launching the poll
                    // We do not display the error message if first
                    done(fromMessage(it))
                } else {
                    done()
                }
                pause()
            } ?: run {
                if (result.first > 0) {
                    Log.e(logTag, "Found ${result.first} change(s)")
                    backOffTicker.resetIndex()
                }
                done()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            launchProcessing()
        }
    }

    override fun onCleared() {
        // viewModelJob.cancel()
        super.onCleared()
        Log.i(logTag, "Cleared")
    }

    init {
        setLoading(true)
    }
}
