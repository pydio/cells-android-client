package com.pydio.android.cells.ui.browse.models

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.utils.externallyView
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Only provide access to the repository while browsing, does not hold any state.
 */
open class AbstractBrowseVM(
    private val prefs: PreferencesService,
    private val nodeService: NodeService,
) : ViewModel() {

    // private val logTag = "AbstractBrowseVM"

    protected val _loadingState = MutableLiveData(LoadingState.STARTING)
    protected val _errorMessage = MutableLiveData<String?>()
    val loadingState: LiveData<LoadingState> = _loadingState
    val errorMessage: LiveData<String?> = _errorMessage

    protected fun launchProcessing() {
        _loadingState.value = LoadingState.PROCESSING
        _errorMessage.value = null
    }

    /* Pass a non-empty err parameter when the process has terminated with an error*/
    protected fun done(err: String? = null) {
        _loadingState.value = LoadingState.IDLE
        _errorMessage.value = err
    }

    protected fun error(msg: String) {
        _loadingState.value = LoadingState.IDLE
        _errorMessage.value = msg
    }


    protected val listPrefs = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list
    }

    protected val sortOrder = listPrefs.map { it.order }.asLiveData(viewModelScope.coroutineContext)
    val layout = listPrefs.map { it.layout }

    fun setListLayout(listLayout: ListLayout) {
        viewModelScope.launch {
            prefs.setListLayout(listLayout)
        }
    }

    suspend fun viewFile(context: Context, stateID: StateID) {
        getNode(stateID)?.let { node ->
            viewFile(context, node)
        }
    }

    private suspend fun viewFile(context: Context, node: RTreeNode) {
        nodeService.getLocalFile(node, true)?.let { file ->
            externallyView(context, file, node)
            return
        } ?: run {
            throw SDKException(ErrorCodes.no_local_file)
        }
    }

    suspend fun getNode(stateID: StateID): RTreeNode? {
        return nodeService.getNode(stateID) ?: run {
            // also try to remove from the remote
            nodeService.tryToCacheNode(stateID)
        }
    }
}
