package com.pydio.android.cells.ui.share

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.ui.core.lazyID
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.share.screens.SelectFolderScreen
import com.pydio.android.cells.ui.share.screens.SelectTargetAccount
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID

private const val logTag = "shareNavGraph"

fun NavGraphBuilder.shareNavGraph(
    helper: ShareHelper,
    browseRemoteVM: BrowseRemoteVM,
    back: () -> Unit,
    // open: (StateID) -> Unit,
//    isExpandedScreen: Boolean,
) {


//@Composable
//fun SelectTargetHost(
//    navController: NavHostController,
//    action: String,
//    initialStateId: String,
//    browseLocalVM: BrowseLocalFoldersVM,
//    browseRemoteVM: BrowseRemoteVM,
//    accountListVM: AccountListVM,
//    uploadsVM: UploadsVM,
//    postActivity: (stateID: StateID, action: String?) -> Unit,
//) {
//
//    val currLoadingState by browseRemoteVM.loadingState.observeAsState()
//
//    /* Define callbacks */
//    val open: (StateID) -> Unit = { stateID ->
//        Log.d(logTag, "in open($stateID)")
//        val newRoute = SelectTargetDestination.OpenFolder.createRoute(stateID)
//        Log.i(logTag, "About to navigate to [$newRoute]")
//        navController.navigate(newRoute)
//    }
//
//    val openParent: (StateID) -> Unit = { stateID ->
//        Log.d(logTag, ".... In OpenParent: $stateID - ${stateID.workspace} ")
//        if (Str.empty(stateID.workspace)) {
//            navController.navigate(SelectTargetDestination.ChooseAccount.route)
//        } else {
//            val parent = stateID.parent()
//            navController.navigate(SelectTargetDestination.OpenFolder.createRoute(parent))
//        }
//    }
//
//    val interceptPost: (StateID, String?) -> Unit = { stateID, currAction ->
//        if (AppNames.ACTION_UPLOAD == currAction) {
//            navController.navigate(
//                SelectTargetDestination.UploadInProgress.createRoute(stateID),
//            ) {
//                // We insure that the navigation page is first on the back stack
//                // So that the end user cannot launch the upload twice using the back btn
//                popUpTo(SelectTargetDestination.ChooseAccount.route) { inclusive = true }
//            }
//        }
//        postActivity(stateID, currAction)
//    }
//
//    val canPost: (StateID) -> Boolean = { stateID ->
//        Str.notEmpty(stateID.workspace)
//        // true
////        if (action == AppNames.ACTION_UPLOAD) {
////            true
////        } else {
////            // Optimistic check to prevent trying to copy move inside itself
////            // TODO this does not work: we get the parent state as initial input
////            //   (to start from the correct location), we should rather get a list of states
////            //   that are about to copy / move to provide better behaviour in the select target activity
////            !((stateID.id.startsWith(initialStateId) && (stateID.id.length > initialStateId.length)))
////        }
//    }
//
//    val forceRefresh: (StateID) -> Unit = { browseRemoteVM.watch(it, true) }
//
//    val startDestination = if (initialStateId != Transport.UNDEFINED_STATE) {
//        SelectTargetDestination.OpenFolder.route
//    } else {
//        SelectTargetDestination.ChooseAccount.route
//    }
//
//    // Configure navigation
//    NavHost(
//        navController = navController, startDestination = startDestination
//    ) {

    composable(ShareDestination.ChooseAccount.route) {
        Log.d(logTag, ".... Open choose account page")

//            // TODO double check this might not be called on the second pass
//            LaunchedEffect(true) {
//                Log.e(logTag, ".... Choose account, launching effect")
//                accountListVM.watch()
//                browseRemoteVM.pause()
//            }
//
        SelectTargetAccount(
            openAccount = helper::open,
            cancel = { helper.interceptPost(AppNames.ACTION_CANCEL, Transport.UNDEFINED_STATE_ID) },
            login = { helper.interceptPost(AppNames.ACTION_LOGIN, it) },
        )
    }

    composable(ShareDestination.OpenFolder.route) { navBackStackEntry ->
        val stateID = lazyID(navBackStackEntry)
        Log.e(logTag, ".... ShareDestination.OpenFolder with ID: $stateID")
        if (stateID == Transport.UNDEFINED_STATE_ID) {
            Log.e(logTag, "Cannot open OfflineRoots with no ID")
            back()
        } else {
            SelectFolderScreen(
                stateID = stateID,
                browseRemoteVM = browseRemoteVM,
                open = helper::open,
                doAction = helper::interceptPost,
            )
        }
    }

    composable(ShareDestination.UploadInProgress.route) { navBackStackEntry ->
        Log.d(logTag, "About to navigate to upload screen")
        Text("Implement me")
// FIXME

        // throws an IllegalArgExc:
        // java.lang.IllegalArgumentException: DerivedState(value=<Not calculated>)@254214545 cannot be saved using the current SaveableStateRegistry. The default implementation only supports types which can be stored inside the Bundle. Please consider implementing a custom Saver for this class and pass it to rememberSaveable().
//            val stateId = rememberSaveable() {
//                derivedStateOf {
//                    navBackStackEntry.arguments
//                        ?.getString(SelectTargetDestination.UploadInProgress.getPathKey())
//                        ?: Transport.UNDEFINED_STATE_ID
//                }
//            }
//
//            val stateId = navBackStackEntry.arguments
//                ?.getString(SelectTargetDestination.UploadInProgress.getPathKey())
//                ?: Transport.UNDEFINED_STATE
//
//            LaunchedEffect(key1 = stateId) {
//                Log.d(logTag, "... In upload root, launching effects for $stateId")
//                accountListVM.pause()
//                browseRemoteVM.pause()
//            }
//
//            UploadProgressList(uploadsVM) {
//                postActivity(StateID.fromId(stateId), AppNames.ACTION_CANCEL)
//            }
    }
//    }
}

//@Composable
//fun SelectTargetApp(content: @Composable () -> Unit) {
//    CellsTheme {
//        Surface(
//            modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
//        ) {
//            content()
//        }
//    }
//}
