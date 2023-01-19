package com.pydio.android.cells

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.box.SelectTargetApp
import com.pydio.android.cells.ui.box.TargetSelectionHost
import com.pydio.android.cells.ui.model.AccountListViewModel
import com.pydio.android.cells.ui.model.BrowseLocalFolders
import com.pydio.android.cells.ui.model.BrowseRemote
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

sealed class SelectTargetDestination(val route: String) {

    object ChooseAccount : SelectTargetDestination("choose-account")

    object OpenFolder : SelectTargetDestination("open/{stateId}") {
        fun createRoute(stateId: StateID) = "open/${stateId.id}"
        fun getPathKey() = "stateId"
    }

    // TODO
    object CreateFolder : SelectTargetDestination("create-folder")

    // TODO
    // Login
    // Logout ?
    // Up

    // TODO add a route that display the newly launched uploads with a "run in background option"

    // TODO also add a "swap to refresh" mechanism

    // TODO limit up action when we are not in "share" context

    // TODO Implement copy move between workspace of a same remote server

    // TODO add safety checks to prevent forbidden copy-move

}

/**
 * Let the end-user choose a target in one of the defined remote servers.
 * This is both used for receiving intents from third-party applications and
 * for choosing a target location for copy and moved.
 */
class SelectTargetActivity : ComponentActivity() {

    private val logTag = SelectTargetActivity::class.simpleName

    private val transferService: TransferService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate: launching target selection process")
        super.onCreate(savedInstanceState)

        val (iAction, iState, iUris) = handleIntent(intent)

        setContent {

            SelectTargetApp {

                val ctx = LocalContext.current
                val navController = rememberNavController()

                val action = rememberSaveable { iAction }
                val initialStateId = rememberSaveable { iState.id }
                val uris = rememberSaveable { iUris }

                val browseRemoteVM by viewModel<BrowseRemote>()
                val browseLocalVM by viewModel<BrowseLocalFolders>()

                if (!initialStateId.equals(Transport.UNDEFINED_STATE_ID)) {
                    val initialStateID = StateID.fromId(initialStateId)
                    browseLocalVM.setState(initialStateID)
                    browseRemoteVM.watch(initialStateID)
                }

                val cancel: () -> Unit = { finishAndRemoveTask() }

                val launchTaskFor: (stateID: StateID) -> Unit = { stateID ->
                    if (action == AppNames.ACTION_COPY || action == AppNames.ACTION_MOVE) {
                        val intent = getIntent(ctx, action, stateID)
                        setResult(Activity.RESULT_OK, intent)
                    } else if (action == AppNames.ACTION_UPLOAD) {
                        for (uri in uris) {
                            transferService.enqueueUpload(stateID, uri)
                        }
                        Toast.makeText(ctx, "#### Launch upload ---- New", Toast.LENGTH_LONG).show()
                    }
                    finishAndRemoveTask()
                }

                val accountListVM by viewModel<AccountListViewModel>()

                TargetSelectionHost(
                    navController,
                    action,
                    initialStateId,
                    browseLocalVM,
                    browseRemoteVM,
                    accountListVM,
                    launchTaskFor,
                    cancel
                )
            }
        }
    }

//     vmScope.launch {

    private fun handleIntent(inIntent: Intent): Triple<String, StateID, List<Uri>> {

        var actionContext = AppNames.ACTION_COPY
        var stateID = StateID.fromId(Transport.UNDEFINED_STATE_ID)
        val uris: MutableList<Uri> = mutableListOf()

        when (inIntent.action) {
            AppNames.ACTION_CHOOSE_TARGET -> {
                actionContext =
                    intent.getStringExtra(AppKeys.EXTRA_ACTION_CONTEXT) ?: AppNames.ACTION_COPY
                stateID = StateID.fromId(intent.getStringExtra(AppKeys.EXTRA_STATE))
            }
            Intent.ACTION_SEND -> {
                val clipData = intent.clipData
                clipData?.let {
                    actionContext = AppNames.ACTION_UPLOAD
                    uris.add(clipData.getItemAt(0).uri)
                }
                // TODO retrieve starting state from: ?
                // CellsApp.instance.getCurrentState()
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val clipData = intent.clipData
                clipData?.let {
                    actionContext = AppNames.ACTION_UPLOAD
                    for (i in 0 until it.itemCount) {
                        uris.add(clipData.getItemAt(i).uri)
                    }
                }
            }
        }

//        // Directly go inside a target location if defined
//        chooseTargetVM.currentLocation.value?.let {
//            val action = UploadNavigationDirections.actionPickFolder(it.id)
//            navController.navigate(action)
//            return
//        }

        return Triple(actionContext, stateID, uris)
    }

    // TODO refactor to rather use a sealed class
    fun getIntent(context: Context, actionContext: String, stateID: StateID): Intent {
        var intent = Intent()

        when (actionContext) {
            AppNames.ACTION_COPY -> {
                intent = Intent(context, MainActivity::class.java)
                intent.action = AppNames.ACTION_CHOOSE_TARGET
                intent.putExtra(AppKeys.EXTRA_STATE, stateID.id)
            }
            AppNames.ACTION_MOVE -> {
                intent = Intent(context, MainActivity::class.java)
                intent.action = AppNames.ACTION_CHOOSE_TARGET
                intent.putExtra(AppKeys.EXTRA_STATE, stateID.id)
            }
            AppNames.ACTION_UPLOAD -> {

                // TODO re-implement this
//                        for (uri in uris) {
//                            // TODO implement error management
//                            val error = transferService.enqueueUpload(stateID, uri)
//                        }
//                        withContext(Dispatchers.Main) {
//                            _postDone.value = true
//                        }
            }
            else -> Log.e(logTag, "Unexpected action context: $actionContext")
        }

        return intent
    }

    // }
}


