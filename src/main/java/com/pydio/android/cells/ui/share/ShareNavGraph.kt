package com.pydio.android.cells.ui.share

import android.util.Log
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.ui.account.AccountListVM
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.core.lazyUID
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.share.models.MonitorUploadsVM
import com.pydio.android.cells.ui.share.models.ShareVM
import com.pydio.android.cells.ui.share.screens.SelectFolderScreen
import com.pydio.android.cells.ui.share.screens.SelectTargetAccount
import com.pydio.android.cells.ui.share.screens.UploadProgressList
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val logTag = "shareNavGraph"

fun NavGraphBuilder.shareNavGraph(
    browseRemoteVM: BrowseRemoteVM,
    helper: ShareHelper,
    back: () -> Unit,
) {
    composable(ShareDestination.ChooseAccount.route) {
        Log.i(logTag, "... Open ShareDestination.ChooseAccount")
        val accountListVM: AccountListVM = koinViewModel()
        SelectTargetAccount(
            accountListVM = accountListVM,
            openAccount = helper::open,
            cancel = {
                helper.launchTaskFor(
                    AppNames.ACTION_CANCEL,
                    StateID.NONE
                )
            },
            login = { helper.launchTaskFor(AppNames.ACTION_LOGIN, it) },
        )

        DisposableEffect(key1 = true) {
            accountListVM.watch()
            onDispose { accountListVM.pause() }
        }
    }

    composable(ShareDestination.OpenFolder.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        if (stateID == StateID.NONE) {
            Log.e(logTag, "Cannot open target selection folder page with no ID")
            back()
        } else {
            val shareVM: ShareVM = koinViewModel { parametersOf(stateID) }
            SelectFolderScreen(
                targetAction = AppNames.ACTION_UPLOAD,
                stateID = stateID,
                browseRemoteVM = browseRemoteVM,
                shareVM = shareVM,
                open = helper::open,
                canPost = helper::canPost,
                doAction = { action, currID ->
                    if (AppNames.ACTION_UPLOAD == action) {
                        helper.startUpload(shareVM, currID)
                    } else {
                        helper.launchTaskFor(action, currID)
                    }
                },
            )
            DisposableEffect(key1 = stateID) {
                if (stateID == StateID.NONE) {
                    browseRemoteVM.pause(StateID.NONE)
                } else {
                    browseRemoteVM.watch(stateID, false)
                }
                onDispose {
                    browseRemoteVM.pause(stateID)
                }
            }
        }
    }

    composable(ShareDestination.UploadInProgress.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        val jobID = lazyUID(nbsEntry)
        Log.i(logTag, ".... ShareDestination.UploadInProgress for #$jobID @ $stateID")
        val monitorUploadsVM: MonitorUploadsVM =
            koinViewModel(parameters = { parametersOf(browseRemoteVM.isLegacy, stateID, jobID) })
        UploadProgressList(
            monitorUploadsVM,
            { helper.runInBackground(stateID) },
            { helper.done(stateID) },
            { helper.cancel(stateID) },
            { helper.openParentLocation(stateID) },
        )
    }
}
