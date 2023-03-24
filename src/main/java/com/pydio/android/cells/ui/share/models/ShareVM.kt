package com.pydio.android.cells.ui.share.models

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch

/**
 * Provides upload to remote server feature while sharing file
 * with the Pydio App from third party Android Apps
 * */
class ShareVM(
    stateID: StateID,
    nodeService: NodeService,
    private val jobService: JobService,
    private val transferService: TransferService
) : ViewModel() {

    private val logTag = "ShareVM"

    // TODO rather inject this
    private val cr = CellsApp.instance.contentResolver

    val childNodes: LiveData<List<RTreeNode>> = if (Str.empty(stateID.workspace)) {
        nodeService.listWorkspaces(stateID)
    } else {
        nodeService.listViewable(stateID, "")
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
                Log.e(logTag, "#### processing $uri ")
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
