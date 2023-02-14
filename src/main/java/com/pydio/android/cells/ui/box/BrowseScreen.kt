package com.pydio.android.cells.ui.box

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.ui.browse.BrowseDestination
import com.pydio.android.cells.ui.models.AccountListVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import org.koin.androidx.compose.koinViewModel

private const val logTag = "BrowseScreen.kt"

@Composable
fun BrowseScreen(
    initialStateID: StateID,
    postActivity: (stateID: StateID, action: String?) -> Unit,
    browseRemoteVM: BrowseRemoteVM = koinViewModel(),
    accountListVM: AccountListVM = koinViewModel(),
) {
    val ctx = LocalContext.current

    val navController = rememberNavController()

    val currLoadingState by browseRemoteVM.isLoading.observeAsState()

    val startDestination = when {
//        initialStateID == Transport.UNDEFINED_STATE_ID
//        -> BrowseDestination.ChooseAccount.route
        Str.empty(initialStateID.workspace)
//        -> BrowseDestination.AccountHome.createRoute(initialStateID.account())
        -> BrowseDestination.AccountHome.route
        else
        -> BrowseDestination.OpenFolder.createRoute(initialStateID)
    }

    val openDrawer: (StateID) -> Unit = {

    }

    val open: (StateID) -> Unit = { stateID ->
        navController.navigate(
            if (Str.notEmpty(stateID.workspace)) {
                BrowseDestination.OpenFolder.createRoute(stateID)
            } else {
                BrowseDestination.AccountHome.route
            }
        )
    }

    val openParent: (StateID) -> Unit = { stateID ->
        val parent = stateID.parent()
        open(parent)
    }

    // val openAccounts: () -> Unit = { navController.navigate(BrowseDestination.ChooseAccount.route) }
    val openAccounts: () -> Unit = { throw RuntimeException("Implement me") }

    NavHost(
        navController = navController, startDestination = startDestination
    ) {

//composable(BrowseDestination.ChooseAccount.route) {
//            Log.d(logTag, ".... Open choose account page")
//            // TODO double check this might not be called on the second pass
//            LaunchedEffect(true) {
//                Log.e(logTag, ".... Choose account, launching effect")
//                accountListVM.watch()
//                browseRemoteVM.pause()
//            }
//
//            SelectAccount(
//                accountListVM = accountListVM,
//                openAccount = open,
//                back = {},
//                registerNew = {},
//                login = {},
//                logout = {},
//                forget = {},
//            )
//        }

//        composable(BrowseDestination.AccountHome.route) { navBackStackEntry ->
//            val stateId =
//                navBackStackEntry.arguments?.getString(BrowseDestination.AccountHome.getPathKey())
//                    ?: initialStateID.id
//            Log.e(logTag, ".... Open account home with ID: ${StateID.fromId(stateId)}")
//
//            AccountHome(
//                StateID.fromId(stateId),
//                openDrawer = openDrawer,
//                openAccounts = openAccounts,
//                openSearch = {},
//                openWorkspace = open,
//                browseRemoteVM = browseRemoteVM,
//            )
//        }
//
//        composable(BrowseDestination.OpenFolder.route) { navBackStackEntry ->
//            val stateId =
//                navBackStackEntry.arguments?.getString(BrowseDestination.AccountHome.getPathKey())
//                    ?: initialStateID.id
//            Log.e(logTag, ".... Open folder at ${StateID.fromId(stateId)}")
//
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
}

@Composable
fun BrowseApp(content: @Composable () -> Unit) {
    CellsTheme {
        Surface(
            modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}
