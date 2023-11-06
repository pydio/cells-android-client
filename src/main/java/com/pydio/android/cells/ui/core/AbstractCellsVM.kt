package com.pydio.android.cells.ui.core

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.ListType
import com.pydio.android.cells.SessionStatus
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.ConnectionService
import com.pydio.android.cells.services.ErrorService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.ui.models.ErrorMessage
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
 * Provides generic flows to ease Cells App pages implementation.
 */
open class AbstractCellsVM : ViewModel(), KoinComponent {

    private val logTag = "AbstractCellsVM"

    // Avoid boiling plate to have the connection services here
    private val errorService: ErrorService by inject()
    private val connectionService: ConnectionService by inject()
    protected val prefs: PreferencesService by inject()
    protected val nodeService: NodeService by inject()
    protected val applicationContext: Context by inject()

    // Expose a flow of error messages for the end-user
    val errorMessage: Flow<ErrorMessage?> = errorService.userMessages

    // Loading data from server state
    private val _loadingState = MutableStateFlow(LoadingState.STARTING)
    val loadingState: StateFlow<LoadingState> =
        _loadingState.combine(connectionService.sessionStatusFlow) { currLoadingState, sessionStatus ->
            val newState =
                if (SessionStatus.NO_INTERNET == sessionStatus || SessionStatus.SERVER_UNREACHABLE == sessionStatus) {
                    LoadingState.SERVER_UNREACHABLE
                } else {
                    currLoadingState
                }
            Log.e(
                logTag,
                "## New VM loading state: $newState (State: $currLoadingState, status: $sessionStatus)"
            )
            newState
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LoadingState.STARTING
        )

    // Preferences
    private val listPrefs = prefs.cellsPreferencesFlow.map { cellsPreferences ->
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
    fun isServerReachable(): Boolean {
        return connectionService.loadingState.value != LoadingState.SERVER_UNREACHABLE
    }

    suspend fun getNode(stateID: StateID): RTreeNode? {
        return nodeService.getNode(stateID) ?: run {
            try {
                // also try to remove from the remote
                nodeService.tryToCacheNode(stateID)
            } catch (se: SDKException) {
                done(se)
                return null
            }
        }
    }

    @Throws(SDKException::class)
    suspend fun viewFile(context: Context, stateID: StateID, skipUpToDateCheck: Boolean = false) {
        getNode(stateID)?.let { node ->
            viewFile(context, node, skipUpToDateCheck)
        }
    }

    @Throws(SDKException::class)
    private suspend fun viewFile(
        context: Context,
        node: RTreeNode,
        skipUpToDateCheck: Boolean = false
    ) {
        val currSkip =
            skipUpToDateCheck || LoadingState.SERVER_UNREACHABLE == connectionService.loadingState.value
        Log.e(
            logTag, "Launch view file, skip check: $currSkip," +
                    " loading: ${connectionService.loadingState.value}"
        )
        val (lf, isUpToDate) = nodeService.getLocalFile(node, currSkip)

        if (lf == null) {
            throw SDKException(ErrorCodes.no_local_file)
        } else if (!isUpToDate) {
            throw SDKException(ErrorCodes.outdated_local_file)
        } else {
            // FIXME
            externallyView(applicationContext, lf, node)
            // externallyView(context, lf, node)
        }
    }

    // Entry points for children models to update current UI state

    protected fun launchProcessing() {
        _loadingState.value = LoadingState.PROCESSING
        errorService.appendError(errorMsg = null)
    }

    /* Pass a non-null errorMsg parameter when the process has terminated with an error*/
    protected fun done(errorMsg: ErrorMessage? = null) {
        _loadingState.value = LoadingState.IDLE
        errorService.appendError(errorMsg)
    }

    protected fun done(e: Exception) {
        _loadingState.value = LoadingState.IDLE
        Log.e(logTag, "Error for user received: ${e.message}")
        errorService.appendError(e)
    }

    protected fun error(msg: String) {
        _loadingState.value = LoadingState.IDLE
        errorService.appendError(msg)
    }

    fun showError(errorMsg: ErrorMessage) {
        errorService.appendError(errorMsg)
    }
}
