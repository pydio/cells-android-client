package com.pydio.android.cells.ui.models

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the current location while choosing a target for file uploads or moves.
 */
class SelectTargetVM(private val transferService: TransferService) : ViewModel() {

    private val logTag = "SelectTargetVM"
    private val viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // Context
    private var _actionContext: String? = null
    val actionContext: String
        get() = _actionContext ?: AppNames.ACTION_UPLOAD

    private var uris = mutableListOf<Uri>()

    // Runtime
    private var _currLocation = MutableLiveData<StateID?>()
    private val currLocation: LiveData<StateID?>
        get() = _currLocation

    // Flags to trigger termination of the ChooseTarget activity:
    // Either we have an intent or we set the postDone flag
    private val _postIntent = MutableLiveData<Intent?>()
    val postIntent: LiveData<Intent?>
        get() = _postIntent

    private var _postDone = MutableLiveData<Boolean>()
    val postDone: LiveData<Boolean>
        get() = _postDone

    fun setCurrentState(stateID: StateID?) {
        _currLocation.value = stateID
    }

    fun isTargetValid(): Boolean {
        return currLocation.value?.path?.let { it.length > 1 } ?: false
    }

    fun launchPost(context: Context) {
        currLocation.value?.let { stateID ->
            vmScope.launch {
                when (_actionContext) {
                    // We do not use that anymore
                    // TODO Clean
//                    AppNames.ACTION_COPY -> {
//                        val intent = Intent(context, MainActivity::class.java)
//                        intent.action = AppNames.ACTION_CHOOSE_TARGET
//                        intent.putExtra(AppKeys.EXTRA_STATE, stateID.id)
//                        withContext(Dispatchers.Main) {
//                            _postIntent.value = intent
//                        }
//                    }
//                    AppNames.ACTION_MOVE -> {
//                        val intent = Intent(context, MainActivity::class.java)
//                        intent.action = AppNames.ACTION_CHOOSE_TARGET
//                        intent.putExtra(AppKeys.EXTRA_STATE, stateID.id)
//                        withContext(Dispatchers.Main) {
//                            _postIntent.value = intent
//                        }
//                    }
                    AppNames.ACTION_UPLOAD -> {
                        for (uri in uris) {
                            // TODO implement error management
                            val error = transferService.enqueueUpload(stateID, uri)
                        }
                        withContext(Dispatchers.Main) {
                            _postDone.value = true
                        }
                    }
                    else -> Log.e(logTag, "Unexpected action context: $_actionContext")
                }
            }
        }
    }

    fun setActionContext(actionContext: String) {
        this._actionContext = actionContext
    }

    fun initUploadAction(targets: List<Uri>) {
        uris.clear()
        uris.addAll(targets)
    }
}
