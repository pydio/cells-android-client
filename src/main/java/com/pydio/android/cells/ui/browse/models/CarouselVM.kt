package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.utils.isPreViewable
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Expose methods to simplify navigation while browsing*/
class CarouselVM(
    initialStateID: StateID,
    private val accountService: AccountService,
    private val nodeService: NodeService,
) : ViewModel() {

    // private val logTag = "CarouselVM"

    private val viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val parentStateID = initialStateID.parent()

    // FIXME this must be the first seen index
    val currentID: StateFlow<StateID> = MutableStateFlow(initialStateID)

    private val allChildren: LiveData<List<RTreeNode>> = nodeService.listViewable(parentStateID, "")

    val preViewableItems: LiveData<List<RTreeNode>>
        get() = Transformations.switchMap(allChildren) { childList ->
            val filteredChildren = MutableLiveData<List<RTreeNode>>()
            val filteredList = childList.filter { item ->
                val preViewable = isPreViewable(item)
                preViewable
            }
            filteredChildren.value = filteredList
            filteredChildren
        }

    private var _isRemoteLegacy = false
    val isRemoteLegacy: Boolean
        get() = _isRemoteLegacy

    init {
        vmScope.launch {
            withContext(Dispatchers.IO) {
                _isRemoteLegacy = accountService.isLegacy(initialStateID)
            }
        }
    }
}
