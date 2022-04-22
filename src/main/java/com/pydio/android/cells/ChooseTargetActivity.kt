package com.pydio.android.cells

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.pydio.android.cells.databinding.ActivityChooseTargetBinding
import com.pydio.android.cells.ui.transfer.ChooseTargetViewModel

/**
 * Let the end-user choose a target in one of the defined remote servers.
 * This is both used for receiving intents from third-party applications and
 * for choosing a target location for copy and moved.
 */
class ChooseTargetActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private val logTag = ChooseTargetActivity::class.simpleName

    private val chooseTargetVM by viewModel<ChooseTargetViewModel>()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var binding: ActivityChooseTargetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate: launching target choice process")
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_choose_target)
        setSupportActionBar(binding.toolbar)

        navController = findNavController(R.id.upload_fragment_host)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        chooseTargetVM.postDone.observe(this) {
            if (it) {
                finishAndRemoveTask()
            }
        }

        chooseTargetVM.postIntent.observe(this) {
            it?.let {
                Log.d(
                    logTag,
                    "Terminating, chosen target state: ${chooseTargetVM.currentLocation.value}"
                )
                setResult(Activity.RESULT_OK, it)
                finishAndRemoveTask()
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onResume() {
        Log.d(logTag, "onResume, intent: $intent")
        super.onResume()
        handleIntent(intent)
    }

    override fun onPause() {
        Log.d(logTag, "onPause, intent: $intent")
        super.onPause()
    }

    private fun handleIntent(inIntent: Intent) {

        when (inIntent.action) {
            AppNames.ACTION_CHOOSE_TARGET -> {
                val actionContext =
                    intent.getStringExtra(AppNames.EXTRA_ACTION_CONTEXT) ?: AppNames.ACTION_COPY
                chooseTargetVM.setActionContext(actionContext)
                val stateID = StateID.fromId(intent.getStringExtra(AppNames.EXTRA_STATE))
                chooseTargetVM.setCurrentState(stateID)
            }
            Intent.ACTION_SEND -> {
                val clipData = intent.clipData
                clipData?.let {
                    chooseTargetVM.setActionContext(AppNames.ACTION_UPLOAD)
                    chooseTargetVM.initUploadAction(listOf(clipData.getItemAt(0).uri))
                }
                // TODO retrieve starting state from: ?
                // CellsApp.instance.getCurrentState()
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val clipData = intent.clipData
                clipData?.let {
                    // Here also ?
                    chooseTargetVM.setActionContext(AppNames.ACTION_UPLOAD)
                    val uris = mutableListOf<Uri>()
                    for (i in 0 until it.itemCount) {
                        uris.add(clipData.getItemAt(i).uri)
                    }
                    chooseTargetVM.initUploadAction(uris)
                }
            }
        }

        // Directly go inside a target location if defined
        chooseTargetVM.currentLocation.value?.let {
            val action = UploadNavigationDirections.actionPickFolder(it.id)
            navController.navigate(action)
            return
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.upload_options, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val valid = chooseTargetVM.isTargetValid()
        Log.d(logTag, "On PrepareOptionMenu, target is valid: $valid")
        super.onPrepareOptionsMenu(menu)
        menu?.let { currMenu ->
            currMenu.findItem(R.id.launch_upload)?.let {
                it.isVisible = valid
            }
        }
        return true
    }

    override fun onStop() {
        Log.d(logTag, "onStop: target state: ${chooseTargetVM.currentLocation.value}")
        super.onStop()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.launch_upload -> {
                chooseTargetVM.launchPost(this)
                true
            }
            R.id.cancel_upload -> {
                this.finishAndRemoveTask()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}
