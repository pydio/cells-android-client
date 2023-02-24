package com.pydio.android.cells


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import org.koin.android.ext.android.inject

/**
 * Let the end-user choose a target in one of the defined remote servers.
 * This is both used for receiving intents from third-party applications and
 * for choosing a target location for copy and moved.
 */
class SelectTargetActivity : ComponentActivity() {

    private val logTag = SelectTargetActivity::class.simpleName

    private val nodeService: NodeService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate: launching target selection process")
        super.onCreate(savedInstanceState)

        val (iAction, iState, iUris) = handleIntent(intent)

        setContent {
            val ctx = LocalContext.current
            if (iAction == AppNames.ACTION_UPLOAD && (iUris.isEmpty())) {
                // Avoid crash until we know how to limit share intents to file only
                Toast.makeText(ctx, "You can only share files with Pydio", Toast.LENGTH_SHORT)
                    .show()
                finishAndRemoveTask()
                return@setContent
            }

//            SelectTargetApp {
//                // val ctx = LocalContext.current
//                val navController = rememberNavController()
//                val showCreateFolderDialog = rememberSaveable { mutableStateOf(false) }
//                val createFolderParent =
//                    rememberSaveable { mutableStateOf(Transport.UNDEFINED_STATE) }
//
//                val initialAction = rememberSaveable { iAction }
//                val initialStateId = rememberSaveable { iState.id }
//                val uris = rememberSaveable { iUris }
//                val coroutineScope = rememberCoroutineScope()
//
//                val browseRemoteVM by viewModel<BrowseRemoteVM>()
//                val browseLocalVM by viewModel<BrowseLocalFoldersVM>()
//                val accountListVM by viewModel<AccountListVM>()
//                val uploadsVM by viewModel<UploadsVM>()
//
//                if (!initialStateId.equals(Transport.UNDEFINED_STATE)) {
//                    val initialStateID = StateID.fromId(initialStateId)
//                    browseLocalVM.setState(initialStateID)
//                    browseRemoteVM.watch(initialStateID, false)
//                }
//
//                val launchTaskFor: (StateID, String?) -> Unit = { stateID, action ->
//                    when (val currAction: String = action ?: initialAction) {
//                        AppNames.ACTION_COPY, AppNames.ACTION_MOVE -> {
//                            val intent = createNextIntent(ctx, currAction, stateID)
//                            setResult(Activity.RESULT_OK, intent)
//                            finishAndRemoveTask()
//                        }
//                        AppNames.ACTION_LOGIN -> {
//                            // TODO this does not work when remote is Cells and we have to perform the OAuth flow.
//                            //    we haven't found yet a way to call back this task once the process has succeed.
//                            coroutineScope.launch {
//                                val session = withContext(Dispatchers.IO) {
//                                    accountListVM.getSession(stateID)
//                                } ?: return@launch
//
//                                // TODO clean this when implementing custom certificate acceptance.
//                                val serverURL =
//                                    ServerURLImpl.fromAddress(session.url, session.tlsMode == 1)
//                                val toAuthIntent = Intent(ctx, LoginActivity::class.java)
//                                toAuthIntent.putExtra(AppKeys.EXTRA_SERVER_URL, serverURL.toJson())
//                                toAuthIntent.putExtra(
//                                    AppKeys.EXTRA_SERVER_IS_LEGACY,
//                                    session.isLegacy
//                                )
//                                toAuthIntent.putExtra(
//                                    AppKeys.EXTRA_AFTER_AUTH_ACTION,
//                                    AuthService.NEXT_ACTION_SHARE
//                                )
//                                // We don't want that the login intermediary activity pollute the history of the end user
//                                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
//                                startActivity(toAuthIntent)
//                            }
//                        }
//                        AppNames.ACTION_UPLOAD -> {
//                            uploadsVM.launchShareToPydioAt(stateID, uris)
////                            finishAndRemoveTask()
//                        }
//                        AppNames.ACTION_CANCEL -> {
//                            finishAndRemoveTask()
//                        }
//                        AppNames.ACTION_CREATE_FOLDER -> {
//                            createFolderParent.value = stateID.id
//                            showCreateFolderDialog.value = true
////                            createFolder(ctx, stateID, nodeService)
//                        }
//                    }
//                }
//
//                SelectTargetHost(
//                    navController,
//                    initialAction,
//                    initialStateId,
//                    browseLocalVM,
//                    browseRemoteVM,
//                    accountListVM,
//                    uploadsVM,
//                    launchTaskFor,
//                )
//
//                if (showCreateFolderDialog.value) {
//                    val doCreate: (StateID, String) -> Unit = { parentID, name ->
//                        coroutineScope.launch {
//                            val errMsg = nodeService.createFolder(parentID, name)
//                            withContext(Dispatchers.Main) {
//                                if (Str.notEmpty(errMsg)) {
//                                    showMessage(ctx, errMsg!!)
//                                } else {
//                                    browseRemoteVM.watch(parentID, true) // This force resets the backoff ticker
//                                    showMessage(ctx, "Folder created at ${parentID.file}.")
//                                }
//                            }
//                        }
//                    }
//                    AskForFolderName(
//                        parStateID = StateID.fromId(createFolderParent.value),
//                        createFolderAt = { parentId, name ->
//                            doCreate(parentId, name)
//                            showCreateFolderDialog.value = false
//                        },
//                        dismiss = { showCreateFolderDialog.value = false },
//                    )
//                }
//            }
        }
    }

    private fun handleIntent(inIntent: Intent): Triple<String, StateID, List<Uri>> {

        var actionContext = AppNames.ACTION_COPY
        var stateID = StateID.fromId(Transport.UNDEFINED_STATE)
        val uris: MutableList<Uri> = mutableListOf()

        when (inIntent.action) {
            AppNames.ACTION_CHOOSE_TARGET -> {
                actionContext =
                    intent.getStringExtra(AppKeys.EXTRA_ACTION_CONTEXT) ?: AppNames.ACTION_COPY
                stateID = StateID.fromId(intent.getStringExtra(AppKeys.EXTRA_STATE))
            }
            Intent.ACTION_SEND -> {
                val clipData = intent.clipData
                Log.e(logTag, "Clip Data: $clipData")
                clipData?.let {
                    actionContext = AppNames.ACTION_UPLOAD
                    clipData.getItemAt(0).uri?.let {
                        uris.add(it)
                    }
                }
                // TODO: rather provide smarter starting point(s) when launching the "browse and select process"
                // CellsApp.instance.getCurrentState()
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val tmpClipData = intent.clipData
                tmpClipData?.let { clipData ->
                    actionContext = AppNames.ACTION_UPLOAD
                    for (i in 0 until clipData.itemCount) {
                        clipData.getItemAt(i).uri?.let {
                            uris.add(it)
                        }
                    }
                }
            }
        }
        return Triple(actionContext, stateID, uris)
    }

    private fun createNextIntent(context: Context, action: String, stateID: StateID): Intent {
        var nextIntent = Intent()
        when (action) {
            AppNames.ACTION_COPY -> {
                nextIntent = Intent(context, MainActivity::class.java)
                nextIntent.action = AppNames.ACTION_CHOOSE_TARGET
                nextIntent.putExtra(AppKeys.EXTRA_STATE, stateID.id)
            }
            AppNames.ACTION_MOVE -> {
                nextIntent = Intent(context, MainActivity::class.java)
                nextIntent.action = AppNames.ACTION_CHOOSE_TARGET
                nextIntent.putExtra(AppKeys.EXTRA_STATE, stateID.id)
            }
            else -> Log.e(logTag, "Unexpected action: $action")
        }
        return nextIntent
    }

    override fun onDestroy() {
        Log.d(logTag, "onDestroy")
        super.onDestroy()
    }

    override fun onPause() {
        Log.d(logTag, "onPause")
        super.onPause()
    }

    override fun onResume() {
        Log.d(logTag, "onResume")
        super.onResume()
    }
}
