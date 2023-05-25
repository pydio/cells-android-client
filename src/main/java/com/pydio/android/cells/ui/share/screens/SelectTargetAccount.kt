package com.pydio.android.cells.ui.share.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.pydio.android.cells.ui.account.AccountListVM
import com.pydio.android.cells.ui.share.composables.TargetAccountList
import com.pydio.cells.transport.StateID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectTargetAccount(
    accountListVM: AccountListVM,
    openAccount: (stateID: StateID) -> Unit,
    cancel: () -> Unit,
    login: (stateID: StateID) -> Unit,
    modifier: Modifier = Modifier,
) {

    val accounts = accountListVM.sessions.collectAsState(listOf())

    val interceptOpen: (stateID: StateID) -> Unit = {
        accountListVM.pause()
        openAccount(it)
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
                    IconButton(onClick = { cancel() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        content = { innerPadding ->
            TargetAccountList(
                accounts.value,
                interceptOpen,
                login,
                modifier,
                innerPadding,
            )
        }
    )
}
