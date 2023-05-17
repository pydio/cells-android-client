package com.pydio.android.cells.ui.browse.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.pydio.android.cells.ui.browse.models.FolderVM
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.share.screens.SelectFolderScaffold
import com.pydio.cells.transport.StateID

@Composable
fun SelectFolderPage(
    action: String,
    stateID: StateID,
    loadingStatus: LoadingState,
    forceRefresh: (stateId: StateID) -> Unit,
    open: (StateID) -> Unit,
    canPost: (StateID) -> Boolean,
    doAction: (String, StateID) -> Unit,
    folderVM: FolderVM,
) {

    val childNodes by folderVM.children.collectAsState(listOf())

    SelectFolderScaffold(
        loadingStatus = loadingStatus,
        action = action,
        stateID = stateID,
        children = childNodes,
        forceRefresh = { forceRefresh(stateID) },
        open = open,
        canPost = canPost,
        doAction = doAction,
    )
}
