package com.pydio.android.cells.ui.browse

import android.util.Log
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.browse.screens.AccountHome
import com.pydio.android.cells.ui.browse.screens.Carousel
import com.pydio.android.cells.ui.browse.screens.Folder
import com.pydio.android.cells.ui.browse.screens.NoAccount
import com.pydio.android.cells.ui.core.lazyID
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

private const val logTag = "BrowseNavGraph"

sealed class BrowseDestinations(val route: String) {

    fun getPathKey() = "state-id"

    object Open : BrowseDestinations("open/{state-id}") {
        fun createRoute(stateID: StateID) = "open/${stateID.id}"
    }

    object OpenCarousel : BrowseDestinations("carousel/{state-id}") {
        fun createRoute(stateID: StateID) = "carousel/${stateID.id}"
    }

    object Bookmarks : BrowseDestinations("bookmarks/{state-id}") {
        fun createRoute(stateID: StateID) = "bookmarks/${stateID.id}"
    }

    object OfflineRoots : BrowseDestinations("offline-roots/{state-id}") {
        fun createRoute(stateID: StateID) = "offline-roots/${stateID.id}"
    }
}

fun NavGraphBuilder.browseNavGraph(
    browseRemoteVM: BrowseRemoteVM,
    back: () -> Unit,
    openDrawer: () -> Unit,
    open: (StateID) -> Unit,
//    isExpandedScreen: Boolean,

) {

    composable(BrowseDestinations.Open.route) { navBackStackEntry ->
        val stateID = lazyID(navBackStackEntry)
        Log.i(logTag, ".... Open node at $stateID")
        if (stateID == Transport.UNDEFINED_STATE_ID) {
            NoAccount(
                openDrawer = openDrawer,
                addAccount = {},
            )
        } else if (Str.notEmpty(stateID.workspace)) {
            Folder(
                stateID,
                openDrawer = openDrawer,
//                openParent = {}, // TODO
                open = open,
                openSearch = {}, // TODO
                browseRemoteVM = browseRemoteVM,
            )
        } else {
            AccountHome(
                stateID,
                openDrawer = openDrawer,
                openAccounts = {}, // TODO openAccounts,
                openSearch = {},
                openWorkspace = open,
                browseRemoteVM = browseRemoteVM,
            )
        }


//        if (stateID == Transport.UNDEFINED_STATE_ID) {
//            // This should never happen, we fall back on account home
//            Log.e(logTag, "... trying to browse with no state ID for $stateID")
//            LaunchedEffect(key1 = accountID) {
//                navController.popBackStack(BrowseDestination.AccountHome.route, false)
//            }
//        } else {
//            Folder(
//                StateID.fromId(stateId),
//                openDrawer = openDrawer,
//                openParent = openParent,
//                open = open,
//                openSearch = {},
//                browseRemoteVM = browseRemoteVM,
//            )
//        }
    }

    composable(BrowseDestinations.OpenCarousel.route) { navBackStackEntry ->
        val stateID = lazyID(navBackStackEntry)
        Carousel(
            stateID,
            back = back,
            // back = { navController.popBackStack() },
        )

//        if (stateId == null) { // Fall back as (unnecessary) failsafe
//            LaunchedEffect(key1 = accountID) {
//                // This should never happen
//                navController.popBackStack(BrowseDestination.AccountHome.route, false)
//            }
//        } else {
//            Carousel(
//                StateID.fromId(stateId),
//                back = { navController.popBackStack() },
//            )
//        }
    }
}


//
///** Main host for the navigation while browsing a given account */
//@Composable
//fun BrowseHost(
//    accountID: StateID,
//    openAccounts: () -> Unit,
//    back: () -> Unit,
//    openDrawer: () -> Unit,
//    browseRemoteVM: BrowseRemoteVM = koinViewModel(),
//   // browseHostVM: BrowseHostVM = koinViewModel(),
//) {
//
//    val navController = rememberNavController()
//    val scope = rememberCoroutineScope()
//
//    val open: (StateID) -> Unit = { stateID ->
//        scope.launch {
//            var route = BrowseDestination.AccountHome.route
//            if (Str.notEmpty(stateID.workspace)) {
//                val item = browseRemoteVM.getTreeNode(stateID) ?: run {
//                    // We cannot navigate to an unknown node item
//                    Log.e(logTag, "No TreeNode found for $stateID in local repo, aborting")
//                    return@launch
//                }
//                if (item.isFolder()) {
//                    route = BrowseDestination.OpenFolder.createRoute(stateID)
//                } else if (item.isPreViewable()) {
//                    route = BrowseDestination.OpenCarousel.createRoute(stateID)
//                } else {
//                    // FIXME launch external view
//                    Log.e(logTag, "Implement me - not a viewable file for $stateID, aborting")
//                    return@launch
//                }
//            }
//            navController.navigate(route) {
//                launchSingleTop = true
//            }
//        }
//    }
//
//    val openParent: (StateID) -> Unit = { stateID ->
//        val parent = stateID.parent()
//        open(parent)
//    }
//
//    NavHost(
//        navController = navController,
//        startDestination = BrowseDestination.AccountHome.route
//    ) {

//    composable(BrowseDestinations.AccountHome.route) { navBackStackEntry ->
//        val stateId = lazyID(navBackStackEntry)
//        Log.i(logTag, ".... Open account home for $stateId")
//        AccountHome(
//            stateId,
//            openDrawer = openDrawer,
//            openAccounts = {}, // TODO openAccounts,
//            openSearch = {},
//            openWorkspace = open,
//            browseRemoteVM = browseRemoteVM,
//        )
//    }
