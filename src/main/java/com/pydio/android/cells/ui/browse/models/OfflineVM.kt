package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID

private val logTag = OfflineVM::class.simpleName

/** Expose methods used by Offline pages */
class OfflineVM(
    //  private val accountService: AccountService,
    private val nodeService: NodeService
) : ViewModel() {

    //    val sessionView: LiveData<RSessionView?> = accountService.liveActiveSessionView
//    val currAccountId: LiveData<StateID?>
//        get() = Transformations.map(sessionView) { currSessionView ->
//            currSessionView?.accountID?.let { StateID.fromId(it) }
//        }
    private val _accountID: MutableLiveData<StateID> = MutableLiveData(Transport.UNDEFINED_STATE_ID)
    val offlineRoots: LiveData<List<RLiveOfflineRoot>>
        get() = Transformations.switchMap(
            _accountID
        ) { currID ->
            if (currID == Transport.UNDEFINED_STATE_ID) {
                MutableLiveData()
            } else {
                nodeService.listOfflineRoots(currID)
            }
        }


    fun afterCreate(accountID: StateID){
        _accountID.value = accountID
    }
}
