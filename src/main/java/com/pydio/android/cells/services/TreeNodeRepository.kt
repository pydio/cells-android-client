package com.pydio.android.cells.services

import android.content.Context
import android.util.Log
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.accounts.RSession
import com.pydio.android.cells.db.accounts.SessionDao
import com.pydio.android.cells.db.nodes.TreeNodeDB
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TreeNodeRepository(
    private val applicationContext: Context,
    private val sessionDao: SessionDao
) {

    private val logTag = "TreeNodeRepository"
    private var treeNodeRepoJob = Job()
    private val treeNodeRepoScope = CoroutineScope(Dispatchers.IO + treeNodeRepoJob)

    // Holds a map to find DB and files for a given account
    private val _sessions = mutableMapOf<String, RSession>()
    val sessions: Map<String, RSession>
        get() = _sessions

    init {
        treeNodeRepoScope.launch {
            refreshSessionCache()
        }
    }

    suspend fun refreshSessionCache() = withContext(Dispatchers.IO) {
        Log.i(logTag, "... Refreshing session cache")
        val sessions = sessionDao.getSessions()
        _sessions.clear()
        for (rSession in sessions) {
            _sessions[rSession.accountID] = rSession
            Log.d(logTag, "   - ${rSession.accountID} at ${rSession.dirName}")
        }
    }

    fun nodeDB(stateID: StateID): TreeNodeDB {
        // TODO cache this
        val accId = sessions[stateID.accountId]
            ?: throw IllegalStateException("No dir name found for $stateID")
        return TreeNodeDB.getDatabase(
            CellsApp.instance.applicationContext,
            stateID.accountId,
            accId.dbName,
        )
    }

    fun closeNodeDb(accountId: String) {

        val accId = sessions[accountId]
            ?: throw IllegalStateException("No dir name found for $accountId")

        TreeNodeDB.closeDatabase(
            applicationContext,
            accountId,
            accId.dbName,
        )
    }
}
