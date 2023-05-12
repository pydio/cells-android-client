package com.pydio.android.cells.ui.core

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.ui.models.ErrorMessage
import com.pydio.android.cells.ui.models.fromException
import com.pydio.android.cells.utils.externallyView
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val _loadingStateF = MutableStateFlow(LoadingState.STARTING)
    private val _errorMessageF = MutableStateFlow<ErrorMessage?>(null)

    val loadingState: Flow<LoadingState> = _loadingStateF
    val errorMessage: Flow<ErrorMessage?> = _errorMessageF

    protected val listPrefs = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list
    }
    val layout = listPrefs.map { it.layout }

    protected val defaultListOrderFlow = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        prefs.getOrderByPair(
            cellsPreferences,
            ListType.DEFAULT
        )
    }

    protected val sortOrder = listPrefs.map { it.order }.asLiveData(viewModelScope.coroutineContext)

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

    // Entry points for children models to update current UI state

    protected fun launchProcessing() {
        _loadingStateF.value = LoadingState.PROCESSING
        _errorMessageF.value = null
    }

    /* Pass a non-null errorMsg parameter when the process has terminated with an error*/
    protected fun done(errorMsg: ErrorMessage? = null) {
        _loadingStateF.value = LoadingState.IDLE
        _errorMessageF.value = errorMsg
    }

    protected fun done(e: Exception) {
        _loadingStateF.value = LoadingState.IDLE
        _errorMessageF.value = fromException(e)
    }

    protected fun error(msg: String) {
        _loadingStateF.value = LoadingState.IDLE
        _errorMessageF.value = ErrorMessage(msg, -1, listOf())
    }
}
