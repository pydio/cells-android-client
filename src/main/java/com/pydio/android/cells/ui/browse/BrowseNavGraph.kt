package com.pydio.android.cells.ui.browse

import android.util.Log
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

private const val logTag = "BrowseNavGraph"

fun NavGraphBuilder.browseNavGraph(
    navController: NavHostController,
    browseRemoteVM: BrowseRemoteVM,
    back: () -> Unit,
    openDrawer: () -> Unit,
) {

    composable(BrowseDestinations.Open.route) { navBackStackEntry ->
        val stateID = lazyStateID(navBackStackEntry)
        val folderVM: FolderVM = koinViewModel(parameters = { parametersOf(stateID) })
        val helper = BrowseHelper(navController, folderVM)

        Log.i(logTag, "## BrowseDestinations.open - $stateID")
        LaunchedEffect(key1 = stateID) {
            Log.e(logTag, "  ... Also launching effect to watch $stateID")
            if (stateID == StateID.NONE) {
                browseRemoteVM.pause()
            } else {
                browseRemoteVM.watch(stateID, false)
            }
        }

        when {
            stateID == StateID.NONE ->
                NoAccount(
                    openDrawer = openDrawer,
                    addAccount = {},
                )
            Str.notEmpty(stateID.workspace) -> {
                Folder(
                    stateID,
                    openDrawer = openDrawer,
                    openSearch = {
                        navController.navigate(
                            CellsDestinations.Search.createRoute(
                                "Folder",
                                stateID
                            )
                        )
                    },
                    browseRemoteVM = browseRemoteVM,
                    folderVM = folderVM,
                    browseHelper = helper,
                )
            }
            else -> {
                var i = 0
                navController.backQueue.forEach {
                    val currID = lazyStateID(it)
                    Log.e(logTag, "#${i++} - ${it.destination.route} - $currID ")
                }

                val accountHomeVM: AccountHomeVM =
                    koinViewModel(parameters = { parametersOf(stateID) })
                AccountHome(
                    stateID,
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
        Log.i(logTag, "... In BrowseDestinations.Offline at $stateID")
        if (stateID == StateID.NONE) {
            Log.e(logTag, "Cannot open OfflineRoots with no ID")
            back()
        } else {
            val offlineVM: OfflineVM = koinViewModel(parameters = { parametersOf(stateID) })
            val helper = BrowseHelper(navController, offlineVM)
            OfflineRoots(
                offlineVM = offlineVM,
                openDrawer = openDrawer,
                openSearch = {}, // FIXME
                browseHelper = helper,
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
            val bookmarksVM: BookmarksVM = koinViewModel(parameters = { parametersOf(stateID) })
            val helper = BrowseHelper(navController, bookmarksVM)
            Bookmarks(
                stateID,
                openDrawer = openDrawer,
                browseHelper = helper,
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
