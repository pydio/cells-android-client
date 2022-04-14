package com.pydio.android.cells.ui.menus

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.TransferService

/**
 * Holds a Transfer record for the dedicated context menu.
 */
class TransferMenuViewModel(
    transferUID: Long,
    val transferService: TransferService
) : ViewModel() {

//    private val tag = TransferMenuViewModel::class.simpleName
//    private var viewModelJob = Job()
//    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    val rTransfer = transferService.getLiveRecord(transferUID)

//    class TransferMenuViewModelFactory(
//        private val transferUID: Long,
//        private val transferService: TransferService,
//        private val application: Application
//    ) : ViewModelProvider.Factory {
//        @Suppress("unchecked_cast")
//        override fun <T : ViewModel> create(modelClass: Class<T>): T {
//            if (modelClass.isAssignableFrom(TransferMenuViewModel::class.java)) {
//                return TransferMenuViewModel(transferUID, transferService, application) as T
//            }
//            throw IllegalArgumentException("Unknown ViewModel class")
//        }
//    }
}