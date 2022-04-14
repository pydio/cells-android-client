package com.pydio.android.cells.ui.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.SessionFactory

/**
 * Manages retrieval of Cells credentials via the OAuth2 credentials flow.
 */
class OAuthViewModel(
    private val authService: AuthService,
    private val sessionFactory: SessionFactory,
    private val accountService: AccountService,
) : ViewModel() {

    private val logTag = OAuthViewModel::class.simpleName
    private val viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // Set upon successful authentication against the remote server
    private val _accountID = MutableLiveData<Pair<String, String?>?>()
    val accountID: LiveData<Pair<String, String?>?>
        get() = _accountID

    // Manage UI
    private val _isProcessing = MutableLiveData<Boolean>().apply {
        this.value = true
    }
    val isProcessing: LiveData<Boolean>
        get() = _isProcessing

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?>
        get() = _message

    private fun switchLoading(newState: Boolean) {
        _isProcessing.value = newState
    }

    fun handleResponse(state: String, code: String) {
        vmScope.launch {
            _message.value = "Retrieving authentication token..."
            val newAccount = withContext(Dispatchers.IO) {
                authService.handleOAuthResponse(accountService, sessionFactory, state, code)
            } ?: run {
                _message.value = "could not retrieve token from code"
                return@launch
            }

            _message.value = "Configuring account..."
            withContext(Dispatchers.IO) {
                accountService.refreshWorkspaceList(newAccount.first)
            }

            _accountID.value = newAccount
            switchLoading(false)
        }
    }

    override fun onCleared() {
        Log.i(logTag, "onCleared")
        super.onCleared()
        viewModelJob.cancel()
    }

    init {
        Log.i(logTag, "created")
    }
}
