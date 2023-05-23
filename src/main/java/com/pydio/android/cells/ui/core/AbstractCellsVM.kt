package com.pydio.android.cells.ui.core

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.ListType
import com.pydio.android.cells.SessionStatus
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.ConnectionService
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Provides generic flows to ease cells app page implementation
 */
open class AbstractCellsVM() : ViewModel(), KoinComponent {

    private val logTag = "AbstractBrowseVM"

    // Avoid boiling plate to have the connection service here.
    private val connectionService: ConnectionService by inject()
    protected val prefs: PreferencesService by inject()
    protected val nodeService: NodeService by inject()

    // Expose a flow of error messages for the end-user.
    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: Flow<ErrorMessage?> = _errorMessage

    // Loading data from server state
    private val _loadingState = MutableStateFlow(LoadingState.STARTING)
    val loadingState: StateFlow<LoadingState> =
        _loadingState.combine(connectionService.sessionStatusFlow) { currLoadingState, sessionStatus ->
            Log.e(logTag, "Computing loading sate with:")
            Log.e(logTag, "State: $currLoadingState, status: $sessionStatus")
            if (SessionStatus.NO_INTERNET == sessionStatus || SessionStatus.SERVER_UNREACHABLE == sessionStatus) {
                LoadingState.SERVER_UNREACHABLE
            } else {
                currLoadingState
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LoadingState.SERVER_UNREACHABLE
        )

    // Preferences
    protected val listPrefs = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list
    }
    protected val defaultOrder = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list.order
    }

    protected val defaultOrderPair = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        prefs.getOrderByPair(
            cellsPreferences,
            ListType.DEFAULT
        )
    }
    val layout = listPrefs.map { it.layout }

    fun setListLayout(listLayout: ListLayout) {
        viewModelScope.launch {
            prefs.setListLayout(listLayout)
        }
    }

    // Generic access to the underlying objects

    suspend fun getNode(stateID: StateID): RTreeNode? {
        return nodeService.getNode(stateID) ?: run {
            // also try to remove from the remote
            nodeService.tryToCacheNode(stateID)
        }
    }

    suspend fun viewFile(context: Context, stateID: StateID) {
        getNode(stateID)?.let { node ->
            viewFile(context, node)
        }
    }

    private suspend fun viewFile(context: Context, node: RTreeNode) {
        val checkUpToDate = LoadingState.SERVER_UNREACHABLE != loadingState.value
        Log.e(logTag, "Launch view file, check: $checkUpToDate, loading: ${loadingState.value}")
        nodeService.getLocalFile(node, checkUpToDate)?.let { file ->
            externallyView(context, file, node)
        } ?: run {
            throw SDKException(ErrorCodes.no_local_file)
        }
    }

    // Entry points for children models to update current UI state

    protected fun launchProcessing() {
        _loadingState.value = LoadingState.PROCESSING
        _errorMessage.value = null
    }

    /* Pass a non-null errorMsg parameter when the process has terminated with an error*/
    protected fun done(errorMsg: ErrorMessage? = null) {
        _loadingState.value = LoadingState.IDLE
        _errorMessage.value = errorMsg
    }

    protected fun done(e: Exception) {
        _loadingState.value = LoadingState.IDLE
        _errorMessage.value = fromException(e)
    }

    protected fun error(msg: String) {
        _loadingState.value = LoadingState.IDLE
        _errorMessage.value = ErrorMessage(msg, -1, listOf())
    }
}