package com.pydio.android.cells.ui.browse

import android.util.Log
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.browse.models.AccountHomeVM
import com.pydio.android.cells.ui.browse.models.BookmarksVM
import com.pydio.android.cells.ui.browse.models.FolderVM
import com.pydio.android.cells.ui.browse.models.OfflineVM
import com.pydio.android.cells.ui.browse.models.TransfersVM
import com.pydio.android.cells.ui.browse.screens.AccountHome
import com.pydio.android.cells.ui.browse.screens.Bookmarks
import com.pydio.android.cells.ui.browse.screens.Carousel
import com.pydio.android.cells.ui.browse.screens.Folder
import com.pydio.android.cells.ui.browse.screens.NoAccount
import com.pydio.android.cells.ui.browse.screens.OfflineRoots
import com.pydio.android.cells.ui.browse.screens.Transfers
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.core.nav.CellsDestinations
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val logTag = "BrowseNavGraph"

fun NavGraphBuilder.browseNavGraph(
    navController: NavHostController,
    browseRemoteVM: BrowseRemoteVM,
    back: () -> Unit,
    openDrawer: () -> Unit,
    open: (StateID) -> Unit,
//    isExpandedScreen: Boolean,
) {

    composable(BrowseDestinations.Open.route) { navBackStackEntry ->
        val stateID = lazyStateID(navBackStackEntry)
        Log.e(logTag, ".... ## In BrowseDestinations.open at $stateID")
        if (stateID == StateID.NONE) {
            NoAccount(
                openDrawer = openDrawer,
                addAccount = {},
            )
        } else if (Str.notEmpty(stateID.workspace)) {

            val folderVM: FolderVM = koinViewModel(parameters = { parametersOf(stateID) })
            val helper = BrowseHelper(stateID, navController, browseRemoteVM, folderVM)

            Folder(
                stateID,
                openDrawer = openDrawer,
                openSearch = {
                    navController.navigate(CellsDestinations.Search.createRoute("Folder", stateID))
                },
                browseRemoteVM = browseRemoteVM,
                folderVM = folderVM,
                browseHelper = helper,
            )
        } else {
            var i = 0
            navController.backQueue.forEach {
                val currID = lazyStateID(it)
                Log.e(logTag, "#${i++} - ${it.destination.route} - $currID ")
            }

            val accountHomeVM: AccountHomeVM = koinViewModel(parameters = { parametersOf(stateID) })
            AccountHome(
                stateID,
                openDrawer = openDrawer,
                openAccounts = { open(StateID.NONE) },
                openSearch = {},
                openWorkspace = open,
                browseRemoteVM = browseRemoteVM,
                accountHomeVM = accountHomeVM,
            )
        }
    }

    composable(BrowseDestinations.OpenCarousel.route) { navBackStackEntry ->
        val stateID = lazyStateID(navBackStackEntry)
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

    composable(BrowseDestinations.OfflineRoots.route) { navBackStackEntry ->
        val stateID = lazyStateID(navBackStackEntry)
        Log.i(logTag, "... In BrowseDestinations.Offline at $stateID")
        if (stateID == StateID.NONE) {
            Log.e(logTag, "Cannot open OfflineRoots with no ID")
            back()
        } else {
            val offlineVM: OfflineVM = koinViewModel()
            offlineVM.afterCreate(stateID.account())
            OfflineRoots(
                offlineVM = offlineVM,
                openDrawer = openDrawer,
                openSearch = {},
                open = open,
            )
        }
    }

    composable(BrowseDestinations.Bookmarks.route) { navBackStackEntry ->
        val stateID = lazyStateID(navBackStackEntry)
        Log.e(logTag, ".... ## In BrowseDestinations.Bookmarks at $stateID")
        if (stateID == StateID.NONE) {
            Log.e(logTag, "Cannot open bookmarks with no ID")
            back()
        } else {
            val bookmarksVM: BookmarksVM = koinViewModel()
            bookmarksVM.afterCreate(stateID.account())
            Bookmarks(
                stateID,
                openDrawer = openDrawer,
                // openSearch = {},
                open = open,
                browseRemoteVM = browseRemoteVM,
                bookmarksVM = bookmarksVM,
            )
        }
    }

    composable(BrowseDestinations.Transfers.route) { navBackStackEntry ->
        val stateID = lazyStateID(navBackStackEntry)
        if (stateID == StateID.NONE) {
            Log.e(logTag, "Cannot open Transfers with no ID")
            back()
        } else {
            val transfersVM: TransfersVM = koinViewModel(parameters = { parametersOf(stateID) })
            Transfers(
                accountID = stateID.account(),
                transfersVM = transfersVM,
                openDrawer = openDrawer,
                open = open,
            )
        }
    }
}
