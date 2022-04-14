package com.pydio.android.cells.ui.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.ServerURL
import com.pydio.cells.legacy.P8Credentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pydio.android.cells.services.AccountService

/**
 * Manages retrieval of P8 credentials, optionally with a captcha.
 */
class P8CredViewModel(private val accountService: AccountService) : ViewModel() {

    private val logTag = P8CredViewModel::class.simpleName

    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var serverURL: ServerURL? = null

    // Set upon successful authentication against the remote server
    private val _accountID = MutableLiveData<String?>()
    val accountID: LiveData<String?>
        get() = _accountID

    // Manage UI
    private val _isProcessing = MutableLiveData<Boolean>()
    val isProcessing: LiveData<Boolean>
        get() = _isProcessing

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

//    private val _launchCredValidation = MutableLiveData<Boolean>()
//    val launchCredValidation: LiveData<Boolean>
//        get() = _launchCredValidation
//
//    fun launchValidation() {
//        _launchCredValidation.value = true
//    }
//
//    fun validationLaunched() {
//        _accountID.value = null
//        _launchCredValidation.value = false
//    }

    fun setUrl(serverUrl: ServerURL) {
        this.serverURL = serverUrl
    }

    fun logToP8(login: String, password: String, captcha: String?) {
        // TODO validate passed parameters
        switchLoading(true)
        vmScope.launch {
            _errorMessage.value = doP8Auth(login, password, captcha)
            switchLoading(false)
        }
    }

    private fun switchLoading(newState: Boolean) {
        _isProcessing.value = newState
    }

    fun cancel() {
        val wasOn = isProcessing.value ?: false
        if (wasOn) {
            _isProcessing.value = false
        }
        // TODO also cancel running jobs
    }

    private suspend fun doP8Auth(login: String, password: String, captcha: String?): String? {

        val currURL = serverURL ?: return "No server URL defined, cannot start P8 auth process"

        val credentials = P8Credentials(login, password, captcha)
        var errorMsg: String? = null

        val accountIDStr = withContext(Dispatchers.IO) {
            Log.i(
                logTag,
                "Launching P8 authentication process for ${credentials.username}@${currURL.url}"
            )
            var id: String? = null
            try {
                id = accountService.registerAccount(currURL, credentials)
                accountService.refreshWorkspaceList(id)
            } catch (e: SDKException) {
                // TODO handle captcha here
                errorMsg = e.message ?: "Invalid credentials, please try again"
            }
            id
        }

        if (accountIDStr != null) {
            _accountID.value = accountIDStr
        }
        return errorMsg
    }

    override fun onCleared() {
        Log.d(logTag, "destroyed")
        super.onCleared()
        viewModelJob.cancel()
    }

    init {
        Log.d(logTag, "created")
    }
}
