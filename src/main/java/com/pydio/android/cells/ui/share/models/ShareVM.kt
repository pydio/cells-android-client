package com.pydio.android.cells.ui.share.models

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch

/**
 * Provides upload to remote server feature while sharing file
 * with the Pydio App from third party Android Apps
 * */
class ShareVM(
    private val nodeService: NodeService,
    private val jobService: JobService,
    private val transferService: TransferService
) : ViewModel() {

    private val logTag = ShareVM::class.simpleName

    // TODO rather inject this
    private val cr = CellsApp.instance.contentResolver

    private val _stateID: MutableLiveData<StateID> = MutableLiveData(Transport.UNDEFINED_STATE_ID)
    val childNodes: LiveData<List<RTreeNode>>
        get() = Transformations.switchMap(
            _stateID
        ) { currID ->
            if (currID == Transport.UNDEFINED_STATE_ID) {
                MutableLiveData()
            } else if (Str.empty(currID.workspace)) {
                nodeService.listWorkspaces(currID)
            } else {
                nodeService.ls(currID)
            }
        }

    fun afterCreate(accountID: StateID) {
        _stateID.value = accountID
    }

    fun launchPost(stateID: StateID, uris: List<Uri>, postLaunched: (Long) -> Unit) {
//        viewModelScope.launch {
//            // TODO enhance this
//            val jobID = jobService.create(
//                owner =AppNames.JOB_OWNER_USER,
//                template = AppNames.JOB_TEMPLATE_SHARE,
//                label = "Upload ${uris.size} files at $stateID",
//                maxSteps = uris.size.toLong()
//            )
//            for (uri in uris) {
//                // TODO implement error management
//                val error = transferService.enqueueUpload(stateID, uri)
//            }
//            afterDone()
//        }
//    }
//
//    fun launchShareToPydioAt(stateID: StateID, uris: List<Uri>) {

//         setAccountID(stateID.account())
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

//                    val tid = transferService.register(cr, uri, stateID)
//                    ids[tid.first] = Pair(tid.second, uri)
//                    setIds(ids.keys)
                } catch (e: Exception) {
                    // TODO handle this
                }
            }
            // Launch the 2 steps process
            ids.forEach {
                val (currName, currUri) = it.value
                transferService.launchCopy(cr, currUri, stateID, it.key, currName)?.let {
                    launch {
                        try {
                            transferService.uploadOne(it)
                            Log.w(logTag, "... $it ==> upload DONE")
                        } catch (e: Exception) {
                            Log.e(logTag, "... $it ==> upload FAILED: ${e.message}")
                        }
                    }
                    Log.w(logTag, "... $it ==> upload LAUNCHED")
                } ?: run {
                    // TODO better error management
                    Log.e(logTag, "could not upload $currName at $stateID")
                }
            }
            postLaunched(jobID)
        }
    }
}
