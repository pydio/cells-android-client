package com.pydio.android.cells.services

import android.content.Context
import android.util.Log
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.accounts.RSession
import com.pydio.android.cells.db.accounts.SessionDao
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.nodes.TreeNodeDB
import com.pydio.android.cells.utils.currentTimestamp
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
        Log.d(logTag, "... Refreshing session cache. Known accounts:")
        val sessions = sessionDao.getSessions()
        _sessions.clear()
        for (rSession in sessions) {
            _sessions[rSession.accountID] = rSession
            Log.d(logTag, "   - ${rSession.account()} at ./${rSession.dirName}")
        }
    }

    fun nodeDB(stateID: StateID): TreeNodeDB {
        // TODO cache this
        val rSession = sessions[stateID.accountId]
            ?: throw IllegalStateException("No dir name found for $stateID")
        return TreeNodeDB.getDatabase(
            CellsApp.instance.applicationContext,
            stateID.accountId,
            rSession.dbName,
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

    fun persistUpdated(rTreeNode: RTreeNode) {
        rTreeNode.localModificationTS = rTreeNode.remoteModificationTS
        val dao = nodeDB(rTreeNode.getStateID()).treeNodeDao()
        dao.getNode(rTreeNode.getStateID().id)
            ?.let { dao.update(rTreeNode) }
            ?: let { dao.insert(rTreeNode) }
    }

    fun persistLocallyModified(rTreeNode: RTreeNode, modificationType: String) {
        rTreeNode.localModificationTS = currentTimestamp()
        rTreeNode.localModificationStatus = modificationType
        nodeDB(rTreeNode.getStateID()).treeNodeDao().update(rTreeNode)
    }

    suspend fun abortLocalChanges(stateID: StateID) = withContext(Dispatchers.IO) {
        val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id) ?: return@withContext
        node.localModificationTS = node.remoteModificationTS
        node.localModificationStatus = null
        nodeDB(stateID).treeNodeDao().update(node)
    }
}
