package com.pydio.android.cells.ui.browse.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.DefaultTopBar

@Composable
fun NoAccount(
    openDrawer: () -> Unit,
    addAccount: () -> Unit,
) {

    Scaffold(
        topBar = {
            DefaultTopBar(stringResource(R.string.welcome_title), openDrawer = openDrawer)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { addAccount() }
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.welcome_add_account_button)
                )
            }
        },
    ) { padding ->

        Column(modifier = Modifier.padding(padding)) {
            Text(text = stringResource(R.string.welcome_subtitle))
            Text(text = stringResource(R.string.welcome_text))
            Text(text = stringResource(R.string.welcome_no_account_instructions))
        }
    }
}
