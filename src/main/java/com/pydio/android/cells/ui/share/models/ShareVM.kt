package com.pydio.android.cells.ui.share.models

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.models.TreeNodeItem
import com.pydio.android.cells.ui.models.toTreeNodeItems
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Provides upload to remote server feature while sharing file
 * with the Pydio App from third party Android Apps
 * */
class ShareVM(
    stateID: StateID,
    prefs: PreferencesService,
    private val jobService: JobService,
    private val nodeService: NodeService,
    private val transferService: TransferService
) : ViewModel() {

    private val logTag = "ShareVM"

    // TODO rather inject this
    private val cr = CellsApp.instance.contentResolver

    private val orderFlow = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        // TODO we do not support order yet but we keep this just in case for a while
        ListType.DEFAULT
//        prefs.getOrderByPair(
//            cellsPreferences,
//            ListType.DEFAULT
//        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val children: StateFlow<List<TreeNodeItem>> = orderFlow.flatMapLatest { currPair ->
        val rtNodes = if (stateID.slug.isNullOrEmpty()) {
            nodeService.listWorkspaces(stateID)
        } else {
            nodeService.listChildren(stateID, "")
        }
        rtNodes.map { nodes -> toTreeNodeItems(nodeService, nodes) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf()
    )

    suspend fun getTreeNode(stateID: StateID): RTreeNode? {
        return nodeService.getNode(stateID)
    }

    fun launchPost(stateID: StateID, uris: List<Uri>, postLaunched: (Long) -> Unit) {
        val ids: MutableMap<Long, Pair<String, Uri>> = HashMap()
        viewModelScope.launch {
            // Register the parent Job
            val jobID = jobService.create(
                owner = AppNames.JOB_OWNER_USER,
                template = AppNames.JOB_TEMPLATE_SHARE,
                label = "Upload ${uris.size} files at $stateID",
                maxSteps = uris.size.toLong()
            )

            // Register the uploads
            for (uri in uris) {
                Log.i(logTag, "... Processing $uri ")
                try {
                    val tid = transferService.register(cr, uri, stateID, jobID)
                    ids[tid.first] = Pair(tid.second, uri)
                } catch (e: Exception) {
                    // TODO handle this
                }
            }

            // Mark the job has started
            jobService.launched(jobID)
            ids.forEach { currID ->
                // Launch the 2 steps process: local copy and then upload
                val (currName, currUri) = currID.value
                transferService.launchCopy(cr, currUri, stateID, currID.key, currName)?.let {
                    launch {
                        try {
                            transferService.uploadOne(it)
                            Log.w(logTag, "... $it ==> upload DONE")
                        } catch (e: Exception) {
                            jobService.failed(
                                jobID, e.message
                                    ?: "Unexpected error during upload of $currID at $stateID"
                            )
                            Log.e(logTag, "... $it ==> upload FAILED: ${e.message}")
                        }
                    }
                    Log.w(logTag, "... $it ==> upload LAUNCHED")
                } ?: run {
                    // TODO better error management
                    Log.e(logTag, "could not upload $currName at $stateID")
                    jobService.failed(jobID, "Could not launch copy for $currName")
                }
            }
            postLaunched(jobID)
        }
    }
}
