package com.pydio.android.cells

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.ui.box.SelectTargetApp
import com.pydio.android.cells.ui.box.TargetSelectionHost
import com.pydio.android.cells.ui.model.AccountListViewModel
import com.pydio.android.cells.ui.model.BrowseLocal
import com.pydio.android.cells.ui.model.BrowseRemote
import com.pydio.android.cells.ui.model.SelectTargetViewModel
import com.pydio.cells.transport.StateID
import org.koin.androidx.viewmodel.ext.android.viewModel

sealed class SelectTargetDestination(val route: String) {

    object ChooseAccount : SelectTargetDestination("choose-account")

    object OpenFolder : SelectTargetDestination("open/{stateId}") {
        fun createRoute(stateId: StateID) = "open/${stateId.id}"
        fun getPathKey() = "stateId"
    }

    object CreateFolder : SelectTargetDestination("create-folder")

    // Login
    // Logout ?
    // Cancel
    // OK
    // Up
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

            SelectTargetApp {

                val navController = rememberNavController()

                val cancel: () -> Unit = { finishAndRemoveTask() }
                val post: (intent: Intent) -> Unit = {
                    setResult(Activity.RESULT_OK, it)
                    finishAndRemoveTask()
                }

                val browseRemoteVM by viewModel<BrowseRemote>()
                val browseLocalVM by viewModel<BrowseLocal>()

                val selectTargetViewModel by viewModel<SelectTargetViewModel>()
                val accountListVM by viewModel<AccountListViewModel>()

                // Current StateID
                // FlowOf LiveData for this state id
                // Background worker that regularly triggers remote fetch of data
                //    - with backoff
                //    - with cancel (?)
                //    - with reset backoff <=> force flag

                TargetSelectionHost(
                    navController,
                    browseLocalVM,
                    browseRemoteVM,
                    selectTargetViewModel,
                    accountListVM,
                    post,
                    cancel
                )
            }
        }
    }
}


