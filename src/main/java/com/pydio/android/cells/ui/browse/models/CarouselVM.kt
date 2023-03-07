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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val logTag = CarouselVM::class.simpleName

/** Expose methods to simplify navigation while browsing*/
class CarouselVM(
    private val accountService: AccountService,
    private val nodeService: NodeService,
) : ViewModel() {

    private val logTag = CarouselVM::class.simpleName
    private val viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private lateinit var _allChildren: LiveData<List<RTreeNode>>
    val allChildren: LiveData<List<RTreeNode>>
        get() = _allChildren

    val preViewableItems: LiveData<List<RTreeNode>>
        get() = Transformations.switchMap(_allChildren) { childList ->
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

    fun afterCreate(startElement: StateID) {
        vmScope.launch {
            withContext(Dispatchers.IO) {
                _isRemoteLegacy = accountService.isLegacy(startElement)
            }
        }
        _allChildren = nodeService.ls(startElement.parent())
    }
}
