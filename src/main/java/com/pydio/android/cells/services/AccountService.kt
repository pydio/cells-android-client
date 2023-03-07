package com.pydio.android.cells.services

import androidx.lifecycle.LiveData
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.cells.api.Client
import com.pydio.cells.api.Credentials
import com.pydio.cells.api.Server
import com.pydio.cells.api.ServerURL
import com.pydio.cells.transport.StateID

interface AccountService {

    /* Clients and session management*/

    fun getClient(stateID: StateID): Client

    suspend fun isClientConnected(stateID: StateID): Boolean

    suspend fun signUp(serverURL: ServerURL, credentials: Credentials): StateID

    suspend fun registerAccount(username: String, server: Server, authStatus: String): StateID

    suspend fun openSession(accountID: StateID): RSessionView?

    suspend fun forgetAccount(accountID: StateID): String?

    suspend fun logoutAccount(accountID: StateID): String?

    suspend fun refreshWorkspaceList(accountID: StateID): Pair<Int, String?>

    /* Direct query to the DB with suspend functions */

    suspend fun getWorkspace(stateID: StateID): RWorkspace?

    suspend fun getActiveSession(): RSessionView?

    suspend fun getSession(stateID: StateID): RSessionView?

    suspend fun listSessionViews(includeLegacy: Boolean): List<RSessionView>

    suspend fun isLegacy(stateId: StateID): Boolean

    suspend fun isRemoteCells(stateId: StateID): Boolean

    suspend fun checkRegisteredAccounts(): Pair<Int, String?>

    /* Live Data */

    val liveActiveSessionView: LiveData<RSessionView?>

    fun getLiveSession(accountID: StateID): LiveData<RSessionView?>

    val liveSessionViews: LiveData<List<RSessionView>>

    fun getLiveSessions(): LiveData<List<RSessionView>>

    fun getLiveWorkspace(stateID: StateID): LiveData<RWorkspace>

    fun getLiveWorkspaces(accountId: String): LiveData<List<RWorkspace>>

    fun getLiveWsByType(type: String, accountID: String): LiveData<List<RWorkspace>>

    /* Helpers */

    suspend fun notifyError(stateID: StateID, code: Int)

    /* Legacy - to be removed */

    @Deprecated("Rather use method with the StateID")
    fun getLiveSession(accountId: String): LiveData<RSessionView?>

    @Deprecated("Rather use method with the StateID")
    suspend fun forgetAccount(accountId: String): String?

    @Deprecated("Rather use method with the StateID")
    suspend fun logoutAccount(accountId: String): String?

    @Deprecated("Rather use method with the StateID")
    suspend fun openSession(accountId: String)

}
