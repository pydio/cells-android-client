package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.NetworkStatus
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.timestampToString
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Hold the session that is currently in foreground for browsing the cache and the remote server.
 */
@OptIn(FlowPreview::class)
class CredentialWatcher(
    private val coroutineService: CoroutineService,
    private val appCredentialService: AppCredentialService,
    networkService: NetworkService,
    accountService: AccountService,
) {
    private val id: String = UUID.randomUUID().toString()
    private val logTag = "CredentialWatcher_${id.substring(30)}"

    private val serviceScope = coroutineService.cellsIoScope

    // Local watching state
    private var wasActive = false

    // Map of currently watched servers, typically the foreground session
    // and optionally sessions that are currently used in background by the workers (e.G sync worker)
    private var credentialsJobs: MutableMap<StateID, Job> = mutableMapOf()

    // We record separately the required background *and* the foreground servers
    private var foregroundSessionID: StateID = StateID.NONE
    private val backgroundSessionIDs: MutableSet<StateID> = mutableSetOf()

    fun registerServer(accountID: StateID) {
        serviceScope.launch {
            if (accountID != foregroundSessionID) {
                // TODO are we sure we want to restart the credential job at this point
                credentialsJobs[accountID]?.cancelAndJoin()
                val newJob = relaunchCredJob(accountID)
                credentialsJobs[accountID] = newJob
                Log.e(logTag, "### Relaunched Cred. Job for $accountID, new ID: $newJob")
            }
            backgroundSessionIDs.add(accountID)
        }
    }

    suspend fun unregisterServer(accountID: StateID) {
        if (accountID != foregroundSessionID) {
            credentialsJobs[accountID]?.let {
                it.cancelAndJoin()
                credentialsJobs.remove(accountID)
                Log.e(logTag, "### Stopped Cred. Job for $accountID, ID was: $it")
            }
        }
        backgroundSessionIDs.remove(accountID)
    }

    private suspend fun pauseCredJob(stateID: StateID) {
        credentialsJobs[stateID]?.let {
            it.cancelAndJoin()
            credentialsJobs.remove(stateID)
        }
    }

    private suspend fun unpauseCredJob(accountID: StateID) {
        val job = credentialsJobs[accountID]
        if (job != null && job.isActive) {
            // nothing to do
        } else {
            job?.cancelAndJoin()
            val newJob = relaunchCredJob(accountID)
            credentialsJobs[accountID] = newJob
        }
    }

    private suspend fun handleForegroundSessionChange(rSessionView: RSessionView) {
        val newID = rSessionView.getStateID()
        val oldID = foregroundSessionID
        if (newID == oldID) {
            if (credentialsJobs[oldID]?.isActive == true) {
                // Nothing to do
            } else {
                // Session was already foreground but job has been canceled (e.G after demo logout)
                // We relaunch the job
                credentialsJobs[newID] = relaunchCredJob(newID)
            }
        } else {
            // Handle the OLD monitoring session
            if (backgroundSessionIDs.contains(oldID)) {
                // Registered for background -> leave it running
            } else {
                // Not registered, we cancel it
                credentialsJobs[oldID]?.let {
                    it.cancelAndJoin()
                    credentialsJobs.remove(oldID)
                }
            }
            // Register a new server
            if (backgroundSessionIDs.contains(newID)) {
                // Registered for background -> was already running
            } else {
                // We start a new monitoring session
                // TODO are we sure we want to restart the credential job at this point
                credentialsJobs[newID]?.cancelAndJoin()
                val newJob = relaunchCredJob(newID)
                credentialsJobs[newID] = newJob
            }
            foregroundSessionID = newID
        }
    }

    private suspend fun handleNetworkChange(networkStatus: NetworkStatus) {
        when (networkStatus) {
            NetworkStatus.UNAVAILABLE -> {
                Log.w(logTag, "... Lost connection to the internet, pausing all credential flows")
                backgroundSessionIDs.forEach { pauseCredJob(it) }
                if (!backgroundSessionIDs.contains(foregroundSessionID)) {
                    pauseCredJob(foregroundSessionID)
                }
                wasActive = false
            }

            else -> {
                if (!wasActive) {
                    Log.i(logTag, "... We are online again, relaunching credential flows")
                    unpauseCredJob(foregroundSessionID)
                    backgroundSessionIDs.forEach {
                        if (foregroundSessionID != it)
                            unpauseCredJob(it)
                    }
                }
                wasActive = true
            }
        }
    }

    private fun relaunchCredJob(accountID: StateID): Job = serviceScope.launch {
        while (this.isActive) {
            monitorCredentials(accountID, this.hashCode())
            // Log.e(logTag, "### About to sleep: ${this.isActive}")
            delay(20000L)
            // Log.e(logTag, "### After sleep, is active: ${this.isActive}")
        }
    }

    // TODO this must be improved
    private suspend fun monitorCredentials(currID: StateID, scopeHash: Int) =
        withContext(coroutineService.ioDispatcher) {

//            Log.e(logTag, "...............................................")
//            Log.e(logTag, "#$scopeHash: checking session $currID")

            // FIXME this check must be done before launching  the credential poll
//            if (currSession.isLegacy) {
//                // this is for Cells only
//                return@withContext
//            }

            val token = appCredentialService.get(currID.id) ?: run {
                Log.w(logTag, "#$scopeHash: Session $currID has no credentials, pausing check")
                pauseCredJob(currID)
                return@withContext
            }
            val validDur = token.expirationTime - currentTimestamp()
//            Log.d(logTag, "#$scopeHash: Monitoring Creds for $currID, expires in $validDur secs.")
            if (validDur > 120) {
                return@withContext
            }

            Log.w(logTag, "#$scopeHash: Monitoring Creds for $currID, token needs refresh:")
            val expTimeStr = timestampToString(token.expirationTime, "dd/MM HH:mm")
            Log.d(logTag, "   --> Expiration time is $expTimeStr")
            val timeout = currentTimestamp() + 30
            val oldTs = token.expirationTime
            var newTs = oldTs

            appCredentialService.requestRefreshToken(currID)
            try {
                while (oldTs == newTs && currentTimestamp() < timeout) {
                    delay(1000)
                    appCredentialService.getToken(currID)?.let {
                        newTs = it.expirationTime
                    }
                }
                return@withContext
            } catch (se: SDKException) {
                Log.e(logTag, "Unexpected error while monitoring refresh token process for $currID")
            }
        }

    init {
        serviceScope.launch {
            // We listen network status changes
            networkService.networkStatusFlow.debounce(1000L).collect { networkStatus ->
                // Log.d(logTag, "... New network status received: $networkStatus")
                handleNetworkChange(networkStatus)
            }
        }

        serviceScope.launch {
            // and foreground session
            accountService.activeSessionView.debounce(1000L).collect { rSessionView ->
                // Log.d(logTag, "... New foreground session: ${rSessionView?.getStateID()}")
                rSessionView?.let {
                    handleForegroundSessionChange(it)
                }
            }
        }
        Log.i(logTag, "... PollService initialised")
    }
}
