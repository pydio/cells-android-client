package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.ui.core.AbstractCellsVM
import com.pydio.android.cells.utils.isPreViewable
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Expose methods to simplify navigation while browsing*/
class CarouselVM(
    initialStateID: StateID,
    private val accountService: AccountService,
) : AbstractCellsVM() {

    // private val logTag = "CarouselVM"

//    private val viewModelJob = Job()
//    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

//    private val parentStateID = initialStateID.parent()
//
//    first shown index is handled in the composable
//    val currentID: StateFlow<StateID> = MutableStateFlow(initialStateID)

    private var _isRemoteLegacy = false
    val isRemoteLegacy: Boolean
        get() = _isRemoteLegacy

    private val allChildren: Flow<List<RTreeNode>> =
        nodeService.listLiveChildren(initialStateID.parent(), "")

    val preViewableItems: Flow<List<RTreeNode>> = allChildren.map { childList ->
        childList.filter { item ->
            val preViewable = isPreViewable(item)
            preViewable
        }
    }

    init {
        viewModelScope.launch {
            _isRemoteLegacy = accountService.isLegacy(initialStateID)
        }
    }
}
