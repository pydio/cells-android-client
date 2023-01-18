package com.pydio.android.cells.ui.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.transport.StateID

private val unknownStateId = StateID("https://example.com")
private val logTag = BrowseRemote::class.simpleName

class BrowseLocal(
    private val nodeService: NodeService
) : ViewModel() {

/*
    private val viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private var currWatcher: Job? = null
*/

    private var _stateID = MutableLiveData<StateID>()
    val stateID: LiveData<StateID>
        get() = _stateID
    init {
        _stateID.value = unknownStateId
    }

    private lateinit var _children: LiveData<List<RTreeNode>>
    val childNodes: LiveData<List<RTreeNode>>
        get() = _children

    fun afterCreate(stateID: StateID) {
        Log.d(logTag, "After Create, state ID: $stateID")
        _children = nodeService.listChildFolders(stateID)
    }

/*
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
*/

}
