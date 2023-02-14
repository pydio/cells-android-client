package com.pydio.android.cells.ui.box.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.ui.box.account.AccountList
import com.pydio.android.cells.ui.models.AccountListVM
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAccount(
    currAccountID: StateID,
    switchAccount: (StateID) -> Unit,
    login: (StateID) -> Unit,
    back: () -> Unit,
    accountListVM: AccountListVM = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val accounts by accountListVM.sessions.observeAsState()

    SelectAccount(
        currAccountID = currAccountID,
        accounts = accounts.orEmpty(),
        openAccount = switchAccount,
        back = back,
        registerNew = { login(Transport.UNDEFINED_STATE_ID) },
        login = login,
        logout = { accountListVM.logoutAccount(it) },
        forget = { accountListVM.forgetAccount(it) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAccount(
    currAccountID: StateID,
    accounts: List<RSessionView>,
    openAccount: (stateID: StateID) -> Unit,
    back: () -> Unit,
    registerNew: () -> Unit,
    login: (stateID: StateID) -> Unit,
    logout: (stateID: StateID) -> Unit,
    forget: (stateID: StateID) -> Unit,
    modifier: Modifier = Modifier,
) {

    val confirmForget: (stateID: StateID) -> Unit = {
        // TODO implement dialog validation
        forget(it)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Choose an account",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { back() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.button_back)
                        )
                    }
                },
                actions = {
//                    IconButton(onClick = { /* doSomething() */ }) {
//                        Icon(
//                            imageVector = Icons.Filled.MoreVert,
//                            contentDescription = "More options"
//                        )
//                    }
                }
            )
        },
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
        content = { innerPadding ->
            AccountList(
                currAccountID,
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
