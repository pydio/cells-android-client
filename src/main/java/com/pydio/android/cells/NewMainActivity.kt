package com.pydio.android.cells


import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.ui.box.BrowseApp
import com.pydio.android.cells.ui.box.BrowseScreen
import com.pydio.android.cells.ui.models.AccountHomeVM
import com.pydio.android.cells.ui.models.AccountListVM
import com.pydio.android.cells.ui.models.BrowseLocalFoldersVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Let the end-user choose a target in one of the defined remote servers.
 * This is both used for receiving intents from third-party applications and
 * for choosing a target location for copy and moved.
 */
class NewMainActivity : ComponentActivity() {

    private val logTag = NewMainActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate: launching target selection process")
        super.onCreate(savedInstanceState)

        val encodedState = savedInstanceState?.getString(AppKeys.EXTRA_STATE)
            ?: intent.getStringExtra(AppKeys.EXTRA_STATE)

        val initialStateID = StateID.fromId(encodedState ?: Transport.UNDEFINED_STATE)
        Log.d(logTag, "onCreate for: $initialStateID")

        setContent {

            BrowseApp {

                val launchTaskFor: (StateID, String?) -> Unit = { stateID, action ->
                    when (val currAction: String = action ?: "none") {
                        AppNames.ACTION_COPY, AppNames.ACTION_MOVE -> {
                            finishAndRemoveTask()
                        }
                    }
                }

                BrowseScreen(
                    initialStateID,
                    launchTaskFor,
                )
            }
        }
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
