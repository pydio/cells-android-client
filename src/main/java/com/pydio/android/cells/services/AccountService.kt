package com.pydio.android.cells.services

import androidx.lifecycle.LiveData
import com.pydio.cells.api.Client
import com.pydio.cells.api.Credentials
import com.pydio.cells.api.ServerURL
import com.pydio.cells.transport.StateID
import com.pydio.android.cells.db.accounts.RLiveSession
import com.pydio.android.cells.db.accounts.RWorkspace

interface AccountService {

    suspend fun registerAccount(serverURL: ServerURL, credentials: Credentials): String

    suspend fun openSession(accountID: String)

    fun getClient(stateId: StateID): Client

    suspend fun isClientConnected(stateID: String): Boolean

    suspend fun notifyError(stateID: StateID, code: Int): Unit?

    fun getLiveSession(accountID: String): LiveData<RLiveSession?>

    fun getLiveWorkspaces(accountID: String): LiveData<List<RWorkspace>>

    val activeSessionLive: LiveData<RLiveSession?>

    val liveSessions: LiveData<List<RLiveSession>>

    fun listLiveSessions(includeLegacy: Boolean): List<RLiveSession>

    suspend fun forgetAccount(accountId: String): String?

    suspend fun logoutAccount(accountID: String): String?

    suspend fun refreshWorkspaceList(accountIDStr: String): Pair<Int, String?>

}
