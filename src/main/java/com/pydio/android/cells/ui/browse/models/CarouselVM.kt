package com.pydio.android.cells.ui.browse.models

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.ui.core.AbstractCellsVM
import com.pydio.android.cells.utils.isPreViewable
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Hold the state for the carousel component */
class CarouselVM(
    initialStateID: StateID,
    private val accountService: AccountService,
) : AbstractCellsVM() {

    private val logTag = "CarouselVM"

    private var _isRemoteLegacy = false
    val isRemoteLegacy: Boolean
        get() = _isRemoteLegacy

    // Observe current folder children
    @OptIn(ExperimentalCoroutinesApi::class)
    val allOrdered: Flow<List<RTreeNode>> = defaultOrderPair.flatMapLatest { currPair ->
        try {
            nodeService.listLiveChildren(
                initialStateID.parent(),
                "",
                currPair.first,
                currPair.second
            )
        } catch (e: Exception) {
            // This should never happen but it has been seen in prod
            // Adding a failsafe to avoid crash
            Log.e(logTag, "Could not list children of $initialStateID: ${e.message}")
            flow { listOf<RTreeNode>() }
        }
    }

    val preViewableItems: Flow<List<RTreeNode>> = allOrdered.map { childList ->
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
