package com.pydio.android.cells.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.android.cells.ui.core.composables.DefaultTopBar
import com.pydio.android.cells.ui.models.AccountListVM
import com.pydio.android.cells.ui.nav.CellsDestinations
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel


private const val logTag = "AccountsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    navigateTo: (String, StateID) -> Unit,
    openDrawer: () -> Unit,
    accountListVM: AccountListVM = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val scope = rememberCoroutineScope()
    val accounts by accountListVM.sessions.observeAsState()

    // TODO handle errors
    AccountsScreen(
        accounts = accounts.orEmpty(),
        openAccount = {
            scope.launch {
                accountListVM.openSession(it)?.let {
                    Log.e(logTag, "About to open session for: $it")
                    navigateTo(BrowseDestinations.Open.route, it.getStateID())
                }
            }
        },
        openDrawer = openDrawer,
        registerNew = { navigateTo(CellsDestinations.Login.route, Transport.UNDEFINED_STATE_ID) },
        login = { navigateTo(CellsDestinations.Login.route, it) },
        logout = { accountListVM.logoutAccount(it) },
        forget = { accountListVM.forgetAccount(it) },
        contentPadding = contentPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountsScreen(
    accounts: List<RSessionView>,
    openAccount: (stateID: StateID) -> Unit,
    openDrawer: () -> Unit,
    registerNew: () -> Unit,
    login: (stateID: StateID) -> Unit,
    logout: (stateID: StateID) -> Unit,
    forget: (stateID: StateID) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val confirmForget: (stateID: StateID) -> Unit = {
        // TODO implement dialog validation
        forget(it)
    }

    Scaffold(
        topBar = { DefaultTopBar(title = "Choose an account", openDrawer = openDrawer) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { registerNew() }
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.create_account)
                )
            }
        },
        modifier = Modifier.padding(contentPadding),
        content = { innerPadding ->
            AccountList(
                accounts,
                openAccount,
                login,
                logout,
                confirmForget,
                modifier,
                innerPadding,
                Arrangement.spacedBy(dimensionResource(R.dimen.list_vertical_padding)),
            )
        }
    )
}
