package com.pydio.android.cells.ui.account

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.android.cells.ui.core.composables.DefaultTopBar
import com.pydio.android.cells.ui.login.LoginDestinations
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
import kotlinx.coroutines.launch

private const val logTag = "AccountsScreen"

@Composable
fun AccountsScreen(
    isExpandedScreen: Boolean,
    accountListVM: AccountListVM,
    navigateTo: (String) -> Unit,
    openDrawer: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val scope = rememberCoroutineScope()
    val accounts = accountListVM.sessions.collectAsState(listOf())

    AccountsScreen(
        isExpandedScreen = isExpandedScreen,
        accounts = accounts.value,
        openAccount = {
            scope.launch {
                accountListVM.openSession(it)?.let {
                    Log.e(logTag, "About to open session for: $it")
                    navigateTo(BrowseDestinations.Open.createRoute(it.getStateID()))
                }
            }
        },
        openDrawer = openDrawer,
        registerNew = {
            navigateTo(LoginDestinations.AskUrl.createRoute())
        },
        login = { stateID, skipVerify, isLegacy ->
            val route = if (isLegacy) {
                LoginDestinations.P8Credentials.createRoute(stateID, skipVerify)
            } else {
                LoginDestinations.LaunchAuthProcessing.createRoute(stateID, skipVerify)
            }
            navigateTo(route)
        },
        logout = { accountListVM.logoutAccount(it) },
        forget = { accountListVM.forgetAccount(it) },
        contentPadding = contentPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountsScreen(
    isExpandedScreen: Boolean,
    accounts: List<RSessionView>,
    openAccount: (stateID: StateID) -> Unit,
    openDrawer: () -> Unit,
    registerNew: () -> Unit,
    login: (stateID: StateID, skipVerify: Boolean, isLegacy: Boolean) -> Unit,
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
        topBar = {
            DefaultTopBar(
                title = stringResource(R.string.choose_account),
                isExpandedScreen = isExpandedScreen,
                openDrawer = if (isExpandedScreen) null else openDrawer,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { registerNew() }
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.create_account)
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
            )
        }
    )
}
