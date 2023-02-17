package com.pydio.android.cells.ui.core.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithInternetBanner(
    content: @Composable () -> Unit
) {

    val showBanner = rememberSaveable {
        mutableStateOf(true)
    }

    // TODO add Snackbar host
    // TODO add bottom sheet

    Scaffold(
//        topBar = {
//            DefaultTopBar(
//                title = label,
//                openDrawer = openDrawer,
//                openSearch = openSearch,
//            )
//        },
//        floatingActionButton = {
//            if (Str.notEmpty(stateID.workspace)) {
//                FloatingActionButton(
//                    onClick = { /*TODO*/ }
//                ) { Icon(Icons.Filled.Add, /* TODO */ contentDescription = "") }
//            }
//        },
    ) { padding -> // Since Compose 1.2.0 it's required to use padding parameter, passed into Scaffold content composable. You should apply it to the topmost container/view in content:
        Column(modifier = Modifier.padding(padding)) {
            if (showBanner.value) {
                Text(text = "no internet")
            }
            content()
        }
    }
}
