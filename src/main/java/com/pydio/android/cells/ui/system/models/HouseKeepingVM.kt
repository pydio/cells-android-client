package com.pydio.android.cells.ui.system.models

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.CoroutineService
import com.pydio.android.cells.services.ErrorService
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Expose methods used to perform house keeping on the App */
class HouseKeepingVM(
    private val stateID: StateID,
    private val coroutineService: CoroutineService,
    private val authService: AuthService,
    private val jobService: JobService,
    private val accountService: AccountService,
    private val nodeService: NodeService,
    private val errorService: ErrorService,
) : ViewModel() {

    private val logTag = "HouseKeepingVM"

    private val _alsoEmptyOffline = MutableStateFlow(false)
    val alsoEmptyOffline = _alsoEmptyOffline.asStateFlow()
    private val _alsoLogout = MutableStateFlow(false)
    val alsoLogout = _alsoLogout.asStateFlow()
    private val _includeAllAccounts = MutableStateFlow(false)
    val includeAllAccounts: StateFlow<Boolean> = _includeAllAccounts
    private val _eraseAll = MutableStateFlow(false)
    val eraseAll = _eraseAll.asStateFlow()

    fun isEraseAll(): Boolean {
        return _eraseAll.value
    }

    fun isAllAccount(): Boolean {
        return _includeAllAccounts.value
    }

    fun toggleIncludeAll(checked: Boolean) {
        _includeAllAccounts.value = checked
    }

    fun toggleLogout(checked: Boolean) {
        _alsoLogout.value = checked
    }

    fun toggleEmptyOffline(checked: Boolean) {
        _alsoEmptyOffline.value = checked
    }

    fun toggleEraseAll(checked: Boolean) {
        _eraseAll.value = checked
    }

    fun launchCacheClearing() {
        coroutineService.cellsIoScope.launch {
            var hasFailed = false
            val eraseAll = eraseAll.value

            val emptyOffline = alsoEmptyOffline.value || eraseAll
            val logout = alsoLogout.value || eraseAll
            val allAccounts = includeAllAccounts.value || eraseAll

            val allIDs: List<StateID> = if (allAccounts) {
                val sessions = accountService.listSessionViews(true)
                sessions.map {
                    it.getStateID()
                }
            } else {
                listOf(stateID)
            }

            for (currStateID in allIDs) {
                try {
                    nodeService.clearAccountCache(currStateID, emptyOffline, logout)
                } catch (se: SDKException) {
                    val msg = "Could not clear cache for $currStateID, cause: ${se.message}"
                    Log.e(logTag, msg)
                    errorService.appendError(msg)
                    hasFailed = true
                }
            }
            if (eraseAll) {
                for (currStateID in allIDs) {
                    try {
                        accountService.forgetAccount(currStateID)
                    } catch (se: SDKException) {
                        val msg = "Could not delete account $currStateID, cause: ${se.message}"
                        Log.e(logTag, msg)
                        errorService.appendError(msg)
                        hasFailed = true
                    }
                }
                jobService.clearAllJobs()
                jobService.clearAllLogs()
                authService.clearOAuthStates()
            }
            try {
                nodeService.emptyGlideCache()
            } catch (se: SDKException) {
                val msg = "Could not empty Glide cache, cause: ${se.message}"
                Log.e(logTag, msg)
                errorService.appendError(msg)
                hasFailed = true
            }
//             Log.e(logTag, "#### Cache clearing launched, still active: ${this.isActive}")
            if (!hasFailed) {
                val msg = "Cache has been emptied"
                Log.e(logTag, msg)
                errorService.appendError(msg)
            }
        }
    }
}
