package com.pydio.android.cells.ui.account

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.accounts.LiveSessionDao

/**
 * Central ViewModel when dealing with a user's accounts.
 */
class AccountListViewModel(
    liveSessionDao: LiveSessionDao,
//    application: Application
) : ViewModel() {

    // TODO rather implement a wrapping method in the account service
    val sessions = liveSessionDao.getLiveSessions()

//    class AccountListViewModelFactory(
//        private val accountService: AccountService,
//        private val application: Application
//    ) : ViewModelProvider.Factory {
//        @Suppress("unchecked_cast")
//        override fun <T : ViewModel> create(modelClass: Class<T>): T {
//            if (modelClass.isAssignableFrom(AccountListViewModel::class.java)) {
//                return AccountListViewModel(accountService, application) as T
//            }
//            throw IllegalArgumentException("Unknown ViewModel class")
//        }
//    }
}
