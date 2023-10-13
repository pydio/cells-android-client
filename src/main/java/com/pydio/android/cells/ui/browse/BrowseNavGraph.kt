package com.pydio.android.cells.ui.browse

import android.util.Log
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.browse.models.AccountHomeVM
import com.pydio.android.cells.ui.browse.models.BookmarksVM
import com.pydio.android.cells.ui.browse.models.CarouselVM
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

fun NavGraphBuilder.browseNavGraph(
    isExpandedScreen: Boolean,
    navController: NavHostController,
    browseRemoteVM: BrowseRemoteVM,
    back: () -> Unit,
    openDrawer: () -> Unit,
) {

    val logTag = "BrowseNavGraph"

    composable(BrowseDestinations.Open.route) { navBackStackEntry ->
        val stateID = lazyStateID(navBackStackEntry)
        LaunchedEffect(key1 = stateID) {
            Log.i(logTag, "## First Composition for: browse/open/${stateID}")
        }

        if (stateID == StateID.NONE) {
            NoAccount(openDrawer = openDrawer, addAccount = {})
        } else if (Str.notEmpty(stateID.slug)) {
            val folderVM: FolderVM = koinViewModel(parameters = { parametersOf(stateID) })
            val helper = BrowseHelper(navController, folderVM)

            Folder(
                isExpandedScreen = isExpandedScreen,
                folderID = stateID,
                openDrawer = openDrawer,
                openSearch = {
                    navController.navigate(
                        CellsDestinations.Search.createRoute("Folder", stateID)
                    )
                },
                browseRemoteVM = browseRemoteVM,
                folderVM = folderVM,
                browseHelper = helper,
            )
        } else {

            val accountHomeVM: AccountHomeVM = koinViewModel(parameters = { parametersOf(stateID) })
            val helper = BrowseHelper(navController, accountHomeVM)

            AccountHome(
                isExpandedScreen = isExpandedScreen,
                accountID = stateID,
                openDrawer = openDrawer,
                openSearch = {
                    navController.navigate(
                        CellsDestinations.Search.createRoute(
                            "AccountHome",
                            stateID
                        )
                    )
                },
                browseRemoteVM = browseRemoteVM,
                accountHomeVM = accountHomeVM,
                browseHelper = helper,
            )
        }

        DisposableEffect(key1 = stateID) {
            if (stateID == StateID.NONE) {
                browseRemoteVM.pause(StateID.NONE)
            } else {
                browseRemoteVM.watch(stateID, false)
            }
            onDispose {
                Log.d(logTag, "onDispose for browse/open/$stateID, pause browseRemoteVM")
                browseRemoteVM.pause(stateID)
            }
        }
    }

    composable(BrowseDestinations.OpenCarousel.route) { navBackStackEntry ->
        val stateID = lazyStateID(navBackStackEntry)
        val carouselVM: CarouselVM = koinViewModel(parameters = { parametersOf(stateID) })
        Carousel(
            stateID,
            // back = back,
            carouselVM,
        )
    }

    composable(BrowseDestinations.OfflineRoots.route) { navBackStackEntry ->
        val stateID = lazyStateID(navBackStackEntry)
        LaunchedEffect(key1 = stateID) {
            Log.i(logTag, "## First Composition for: browse/offline/$stateID")
        }
        if (stateID == StateID.NONE) {
            Log.e(logTag, "Cannot open OfflineRoots with no ID")
            back()
        } else {
            val offlineVM: OfflineVM = koinViewModel(parameters = { parametersOf(stateID) })
            val helper = BrowseHelper(navController, offlineVM)
            OfflineRoots(
                offlineVM = offlineVM,
                openDrawer = openDrawer,
                browseHelper = helper,
            )
        }
    }

    composable(BrowseDestinations.Bookmarks.route) { navBackStackEntry ->
        val stateID = lazyStateID(navBackStackEntry)
        LaunchedEffect(key1 = stateID) {
            Log.i(logTag, "## First Composition for: browse/bookmarks/$stateID")
        }
        if (stateID == StateID.NONE) {
            Log.e(logTag, "Cannot open bookmarks with no ID")
            back()
        } else {
            val bookmarksVM: BookmarksVM = koinViewModel(parameters = { parametersOf(stateID) })
            val helper = BrowseHelper(navController, bookmarksVM)
            Bookmarks(
                stateID,
                openDrawer = openDrawer,
                browseHelper = helper,
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
            val transfersVM: TransfersVM =
                koinViewModel(parameters = { parametersOf(browseRemoteVM.isLegacy, stateID) })
            val helper = BrowseHelper(navController, transfersVM)
            Transfers(
                accountID = stateID.account(),
                transfersVM = transfersVM,
                openDrawer = openDrawer,
                browseHelper = helper,
            )
        }
    }
}
