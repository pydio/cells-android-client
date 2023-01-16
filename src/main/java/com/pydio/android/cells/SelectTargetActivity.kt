package com.pydio.android.cells

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.ui.box.browse.FolderList
import com.pydio.android.cells.ui.box.browse.SessionList
import com.pydio.android.cells.ui.model.AccountListViewModel
import com.pydio.android.cells.ui.model.SelectTargetViewModel
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.ui.transfer.PickFolderViewModel
import com.pydio.cells.transport.StateID
import org.koin.androidx.viewmodel.ext.android.viewModel

sealed class SelectTargetDestination(val route: String) {

    object ChooseAccount : SelectTargetDestination("choose-account")

    object OpenFolder : SelectTargetDestination("open/{stateId}") {
        fun createRoute(stateId: StateID) = "open/${stateId.id}"
        fun getPathKey() = "stateId"
    }

    object CreateFolder : SelectTargetDestination("create-folder")
}

/**
 * Let the end-user choose a target in one of the defined remote servers.
 * This is both used for receiving intents from third-party applications and
 * for choosing a target location for copy and moved.
 */
class SelectTargetActivity : ComponentActivity() {

    private val logTag = SelectTargetActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate: launching target selection process")
        super.onCreate(savedInstanceState)
        setContent {
            CellsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val selectTargetViewModel by viewModel<SelectTargetViewModel>()
                    val accountListVM by viewModel<AccountListViewModel>()
                    val pickFolderVM by viewModel<PickFolderViewModel>()

                    TargetSelectionHost(
                        navController,
                        selectTargetViewModel,
                        accountListVM,
                        pickFolderVM
                    )
                }
            }
        }
    }
}

@Composable
fun TargetSelectionHost(
    navController: NavHostController,
    selectTargetVM: SelectTargetViewModel,
    accountListVM: AccountListViewModel,
    pickFolderVM: PickFolderViewModel,
) {
    val ctx = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = SelectTargetDestination.ChooseAccount.route
    ) {

        val modifier = Modifier
        val open: (stateId: StateID) -> Unit = { stateId ->
            navController.navigate(SelectTargetDestination.OpenFolder.createRoute(stateId))
        }

        val openParent: (stateId: StateID) -> Unit = { stateId ->
            if (stateId.isWorkspaceRoot) {
                navController.navigate(SelectTargetDestination.ChooseAccount.route)
            } else {
                val parent = stateId.parent()
                navController.navigate(SelectTargetDestination.OpenFolder.createRoute(parent))
            }
        }

        val login: (stateId: StateID) -> Unit = { stateId -> Log.e("TEST", "Login to $stateId") }

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
                    StateID.fromId(stateId),
                    selectTargetVM,
                    pickFolderVM,
                    open,
                    openParent,
                    modifier,
                )
            }
        }
    }
}


