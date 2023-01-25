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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.tasks.createFolder
import com.pydio.android.cells.ui.box.SelectTargetApp
import com.pydio.android.cells.ui.box.SelectTargetHost
import com.pydio.android.cells.ui.models.AccountListVM
import com.pydio.android.cells.ui.models.AuthVM
import com.pydio.android.cells.ui.models.BrowseLocalFoldersVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Let the end-user choose a target in one of the defined remote servers.
 * This is both used for receiving intents from third-party applications and
 * for choosing a target location for copy and moved.
 */
class SelectTargetActivity : ComponentActivity() {

    private val logTag = SelectTargetActivity::class.simpleName

    private val transferService: TransferService by inject()
    private val nodeService: NodeService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate: launching target selection process")
        super.onCreate(savedInstanceState)

        val (iAction, iState, iUris) = handleIntent(intent)
        setContent {
            SelectTargetApp {

                val ctx = LocalContext.current
                val navController = rememberNavController()

                val initialAction = rememberSaveable { iAction }
                val initialStateId = rememberSaveable { iState.id }
                val uris = rememberSaveable { iUris }
                val coroutineScope = rememberCoroutineScope()

                val browseRemoteVM by viewModel<BrowseRemoteVM>()
                val browseLocalVM by viewModel<BrowseLocalFoldersVM>()
                val accountListVM by viewModel<AccountListVM>()
                val authVM by viewModel<AuthVM>()

                if (!initialStateId.equals(Transport.UNDEFINED_STATE_ID)) {
                    val initialStateID = StateID.fromId(initialStateId)
                    browseLocalVM.setState(initialStateID)
                    browseRemoteVM.watch(initialStateID)
                }

                val launchTaskFor: (StateID, String?) -> Unit = { stateID, action ->
                    when (val currAction: String = action ?: initialAction) {
                        AppNames.ACTION_COPY, AppNames.ACTION_MOVE -> {
                            val intent = createNextIntent(ctx, currAction, stateID)
                            setResult(Activity.RESULT_OK, intent)
                            finishAndRemoveTask()
                        }
                        AppNames.ACTION_LOGIN -> {
                            coroutineScope.launch {
                                val session = withContext(Dispatchers.IO) {
                                    accountListVM.getSession(stateID)
                                } ?: return@launch
                                authVM.startAuthProcess(
                                    ctx,
                                    coroutineScope,
                                    session.isLegacy,
                                    session.url,
                                    session.tlsMode,
                                    "select" // TODO review this
                                )
                            }
                            Toast.makeText(
                                ctx,
                                "#### Login started for $stateID",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        AppNames.ACTION_UPLOAD -> {
                            for (uri in uris) {
                                transferService.enqueueUpload(stateID, uri)
                            }
                            finishAndRemoveTask()
                        }
                        AppNames.ACTION_CANCEL -> {
                            finishAndRemoveTask()
                        }
                        AppNames.ACTION_CREATE_FOLDER -> {
                            createFolder(ctx, stateID, nodeService)
                        }
                    }
                }

                SelectTargetHost(
                    navController,
                    initialAction,
                    initialStateId,
                    browseLocalVM,
                    browseRemoteVM,
                    accountListVM,
                    launchTaskFor,
                )
            }
        }
    }

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
                // TODO: rather provide smarter starting point(s) when launching the "browse and select process"
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
        return Triple(actionContext, stateID, uris)
    }

    // TODO refactor action names to rather use a sealed class
    private fun createNextIntent(context: Context, action: String, stateID: StateID): Intent {
        var intent = Intent()
        when (action) {
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
            else -> Log.e(logTag, "Unexpected action: $action")
        }
        return intent
    }
}
