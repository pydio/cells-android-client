package com.pydio.android.cells.ui.auth

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.Server
import com.pydio.cells.api.ServerURL
import com.pydio.cells.transport.ServerURLImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.SessionFactory
import java.io.IOException
import java.net.MalformedURLException
import javax.net.ssl.SSLException

/**
 * Manages the declaration of a new server, by:
 * - checking existence of the target URL
 * - validating TLS status
 * - retrieving server type (Cells or P8)
 */
class ServerUrlViewModel(
    private val authService: AuthService,
    private val sessionFactory: SessionFactory
) : ViewModel() {

    private val logTag = ServerUrlViewModel::class.simpleName
    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // First step, we have nothing then an address
    private val _serverAddress = MutableLiveData<String>()
    val serverAddress: LiveData<String>
        get() = _serverAddress

    // Handle TLS if necessary
    private val _skipVerify = MutableLiveData<Boolean>()
    private val _invalidTLS = MutableLiveData<Boolean>()
    val invalidTLS: LiveData<Boolean>
        get() = _invalidTLS

    // A valid server URL with TLS managed
    private val _serverUrl = MutableLiveData<ServerURL>()
    val serverUrl: LiveData<ServerURL>
        get() = _serverUrl

    // Server is a Pydio instance and has already been registered
    private val _server = MutableLiveData<Server?>()
    val server: LiveData<Server?>
        get() = _server

    // Temporary intent to launch external OAuth Process
    private val _nextIntent = MutableLiveData<Intent?>()
    val nextIntent: LiveData<Intent?>
        get() = _nextIntent

    // Manage UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun pingAddress(serverAddress: String) {
        _serverAddress.value = serverAddress
        vmScope.launch {

            switchLoading(true)

            // First Ping the server to insure Address is valid
            // and distant server has a valid TLS configuration
            val serverURL = doPing(serverAddress)
            serverURL?.let {
                _serverUrl.value = it

                // ServerURL is OK, tries to register server
                val server = doRegister(serverURL)
                server?.let {
                    _server.value = it
                }
            }
        }
    }

    fun authLaunched() {
        // rather reset this when the launch has been canceled and the user modifies the URL
        // _server.value = null
        switchLoading(false)
    }

    fun intentStarted() {
        _nextIntent.value = null
    }

    private suspend fun doPing(serverAddress: String): ServerURL? {
        return withContext(Dispatchers.IO) {
            Log.i(logTag, "Perform real ping to $serverAddress")
            var newURL: ServerURL? = null
            try {
                newURL = ServerURLImpl.fromAddress(serverAddress, _skipVerify.value ?: false)
                newURL.ping()
            } catch (e: MalformedURLException) {
                updateErrorMsg(e.message ?: "Invalid address, please update")
            } catch (e: SSLException) {
                updateErrorMsg("Invalid certificate for $serverAddress")
                withContext(Dispatchers.Main) {
                    _skipVerify.value = false
                    _invalidTLS.value = true
                }
                return@withContext null
            } catch (e: IOException) {
                updateErrorMsg(e.message ?: "IOException:")
                e.printStackTrace()
            } catch (e: Exception) {
                updateErrorMsg(e.message ?: "Invalid address, please update")
                e.printStackTrace()
            }
            newURL
        }
    }

    fun confirmTlsValidationSkip(doSkip: Boolean) {
        if (doSkip) {
            _skipVerify.value = true
            serverAddress.value?.let { pingAddress(it) }
        } else {
            // cancel server and give the user the possibility to enter another address
        }
    }

    private suspend fun doRegister(su: ServerURL): Server? = withContext(Dispatchers.IO) {
        Log.d(logTag, "About to register the server ${su.id}")
        var newServer: Server? = null
        try {
            newServer = sessionFactory.registerServer(su)
        } catch (e: SDKException) {
            updateErrorMsg(
                e.message
                    ?: "This does not seem to be a Pydio server address, please double check"
            )
        }
        newServer
    }

    fun launchOAuthProcess(serverURL: ServerURL) {
        vmScope.launch {
            _nextIntent.value = authService.createOAuthIntent(
                sessionFactory,
                serverURL,
                AuthService.NEXT_ACTION_BROWSE
            )
            switchLoading(false)
        }
    }

    private fun switchLoading(newState: Boolean) {
        _isLoading.value = newState
    }

    private suspend fun updateErrorMsg(msg: String) {
        return withContext(Dispatchers.Main) {
            _errorMessage.value = msg
            switchLoading(false)
        }
    }

    override fun onCleared() {
        Log.d(logTag, "onCleared")
        super.onCleared()
        viewModelJob.cancel()
    }

    init {
        Log.d(logTag, "Created")
    }
}
