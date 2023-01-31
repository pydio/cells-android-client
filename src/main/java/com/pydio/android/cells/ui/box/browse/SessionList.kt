package com.pydio.android.cells.ui.box.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.box.account.AccountList
import com.pydio.android.cells.ui.models.AccountListVM
import com.pydio.cells.transport.StateID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionList(
    accountListVM: AccountListVM,
    openAccount: (stateID: StateID) -> Unit,
    cancel: () -> Unit,
    login: (stateID: StateID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accounts by accountListVM.sessions.observeAsState()

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
                    IconButton(onClick = { cancel() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
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
        content = { innerPadding ->
            AccountList(
                accounts,
                openAccount,
                login,
                innerPadding,
                Arrangement.spacedBy(dimensionResource(R.dimen.list_vertical_padding)),
                modifier,
            )
        }
    )
}