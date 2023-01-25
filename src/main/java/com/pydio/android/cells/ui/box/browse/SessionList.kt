package com.pydio.android.cells.ui.box.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.pydio.android.cells.ui.box.account.AccountList
import com.pydio.android.cells.ui.model.AccountListVM
import com.pydio.cells.transport.StateID

@Composable
fun SessionList(
    accountListVM: AccountListVM,
    openAccount: (stateID: StateID) -> Unit,
    login: (stateID: StateID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accounts by accountListVM.sessions.observeAsState()
    AccountList(accounts, openAccount, login, modifier)
}