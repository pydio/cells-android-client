package com.pydio.android.cells.ui.share

import android.util.Log
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.core.lazyUID
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.share.models.MonitorUploadsVM
import com.pydio.android.cells.ui.share.models.ShareVM
import com.pydio.android.cells.ui.share.screens.SelectFolderScreen
import com.pydio.android.cells.ui.share.screens.SelectTargetAccount
import com.pydio.android.cells.ui.share.screens.UploadProgressList
import com.pydio.cells.api.Transport
import org.koin.androidx.compose.koinViewModel

private const val logTag = "shareNavGraph"

fun NavGraphBuilder.shareNavGraph(
    helper: ShareHelper,
    browseRemoteVM: BrowseRemoteVM,
    back: () -> Unit,
    // open: (StateID) -> Unit,
//    isExpandedScreen: Boolean,
) {

    composable(ShareDestination.ChooseAccount.route) {
        Log.i(logTag, "... Open ShareDestination.ChooseAccount")
        SelectTargetAccount(
            openAccount = helper::open,
            cancel = { helper.launchTaskFor(AppNames.ACTION_CANCEL, Transport.UNDEFINED_STATE_ID) },
            login = { helper.launchTaskFor(AppNames.ACTION_LOGIN, it) },
        )
    }

    composable(ShareDestination.OpenFolder.route) { navBackStackEntry ->
        val stateID = lazyStateID(navBackStackEntry, ShareDestination.OpenFolder.getStateIdKey())
        Log.i(logTag, ".... ShareDestination.OpenFolder for $stateID")

        val shareVM: ShareVM = koinViewModel()

        if (stateID == Transport.UNDEFINED_STATE_ID) {
            Log.e(logTag, "Cannot open target selection folder page with no ID")
            back()
        } else {
            SelectFolderScreen(
                stateID = stateID,
                browseRemoteVM = browseRemoteVM,
                shareVM = shareVM,
                open = helper::open,
                canPost = helper::canPost,
                startUpload = helper::startUpload,
                doAction = { action, currID -> helper.launchTaskFor(action, currID) },
            )
        }
    }

    composable(ShareDestination.UploadInProgress.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry, ShareDestination.UploadInProgress.getStateIdKey())
        val jobID = lazyUID(nbsEntry)
        Log.i(logTag, ".... ShareDestination.UploadInProgress for #$jobID @ $stateID")
        val monitorUploadsVM: MonitorUploadsVM = koinViewModel()
        monitorUploadsVM.afterCreate(stateID, jobID)
        UploadProgressList(monitorUploadsVM) {
            helper.launchTaskFor(AppNames.ACTION_CANCEL, stateID)
        }
    }
}
