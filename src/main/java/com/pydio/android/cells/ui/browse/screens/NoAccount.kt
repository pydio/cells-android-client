package com.pydio.android.cells.ui.browse.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.box.common.DefaultTopBar
import com.pydio.cells.transport.StateID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoAccount(
    openDrawer: (StateID?) -> Unit,
    addAccount: () -> Unit,
) {

    Scaffold(
        topBar = {
            DefaultTopBar(stringResource(R.string.welcome_title), openDrawer = { openDrawer(null) })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { addAccount() }
            ) { Icon(Icons.Filled.Add, /* TODO */ contentDescription = "") }
        },
    ) { padding -> // Since Compose 1.2.0 it's required to use padding parameter, passed into Scaffold content composable. You should apply it to the topmost container/view in content:

        Column(
            modifier = Modifier.padding(padding),
        ) {
//            Text(
//                text = stringResource(id = R.string.welcome_title)
//            )
            Text(
                text = stringResource(id = R.string.welcome_subtitle)
            )
            Text(
                text = stringResource(id = R.string.welcome_text)
            )
            Text(
                text = stringResource(id = R.string.welcome_no_account_instructions)
            )
        }
    }


}