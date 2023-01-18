package com.pydio.android.cells.ui.box

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pydio.android.cells.SelectTargetDestination
import com.pydio.android.cells.ui.box.browse.FolderList
import com.pydio.android.cells.ui.box.browse.SessionList
import com.pydio.android.cells.ui.model.AccountListViewModel
import com.pydio.android.cells.ui.model.BrowseLocal
import com.pydio.android.cells.ui.model.BrowseRemote
import com.pydio.android.cells.ui.model.SelectTargetViewModel
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

@Composable
fun TargetSelectionHost(
    navController: NavHostController,
    browseLocalVM: BrowseLocal,
    browseRemoteVM: BrowseRemote,
    selectTargetVM: SelectTargetViewModel,
    accountListVM: AccountListViewModel,
//    pickFolderVM: PickFolderViewModel,
    postActivity: (i: Intent) -> Unit,
    cancelActivity: () -> Unit,
) {
    val ctx = LocalContext.current
    val modifier = Modifier

    val currLoadingState by browseRemoteVM.isLoading.observeAsState()

    /* Define callbacks */
    val open: (stateId: StateID) -> Unit = { stateId ->
        Log.d("DEBUG", "in open($stateId)")

        browseRemoteVM.watch(stateId)
        browseLocalVM.afterCreate(stateId)
        selectTargetVM.setCurrentState(stateId)
        navController.navigate(SelectTargetDestination.OpenFolder.createRoute(stateId))
    }

    val openParent: (stateId: StateID) -> Unit = { stateId ->
        Log.d("DEBUG", ".... In OpenParent: $stateId - ${stateId.workspace} ")
        if (Str.empty(stateId.workspace)) {
            Log.d("DEBUG", "WS Root")
            navController.navigate(SelectTargetDestination.ChooseAccount.route)
        } else {
            Log.d("DEBUG", "##### Not a root")
            val parent = stateId.parent()
            Log.d("DEBUG", "##### Navigating to $parent")
            navController.navigate(SelectTargetDestination.OpenFolder.createRoute(parent))
        }
    }

    val login: (stateId: StateID) -> Unit = { stateId -> Log.e("TEST", "Login to $stateId") }

    /* Configure navigation */
    NavHost(
        navController = navController,
        startDestination = SelectTargetDestination.ChooseAccount.route
    ) {

        composable(SelectTargetDestination.ChooseAccount.route) {
            SessionList(accountListVM, open, login, modifier)
        }
        composable(SelectTargetDestination.OpenFolder.route) { navBackStackEntry ->
            val stateId =
                navBackStackEntry.arguments?.getString(SelectTargetDestination.OpenFolder.getPathKey())
            if (stateId == null) {
                Toast.makeText(ctx, "no element id", Toast.LENGTH_LONG).show()
            } else {
                FolderList(
                    stateId,
                    currLoadingState ?: true,
                    browseLocalVM,
                    open,
                    openParent,
                    postActivity,
                    cancelActivity,
                    modifier,
                )
            }
        }
    }
}

@Composable
fun SelectTargetApp(content: @Composable () -> Unit) {
    CellsTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}
