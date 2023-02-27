package com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.mixed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pydio.android.cells.ui.box.beta.BottomSheetContent
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.ui.theme.ok
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


/**
 * inspired from https://github.com/johncodeos-blog/BottomSheetComposeExample.
 *
 * Quite dirty, we mix material and material3 libraries.
 * scrim seems to be broken
 */

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetScaffoldDecorator(
    content: @Composable (openFor: (Long) -> Unit) -> Unit
) {
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    val openScaffold: (Long) -> Unit = { _ ->
        scope.launch {
            bottomSheetScaffoldState.bottomSheetState.expand()
        }
    }

    BottomSheetScaffold(
        sheetContent = {
            BottomSheetContent()
        },
        scaffoldState = bottomSheetScaffoldState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MaterialTheme.colorScheme.primary,
        // sheetPeekHeight = 0.dp,
        // scrimColor = Color.Red,  // Color for the fade background when you open/close the bottom sheet
    ) {
        Scaffold(
//            topBar = { TopBar() },
            containerColor = CellsColor.warning
        ) { padding ->  // We need to pass scaffold's inner padding to content. That's why we use Box.
            Box(modifier = Modifier.padding(padding)) {
                content(openScaffold)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetScaffoldScreen() {
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()
    BottomSheetScaffold(
        sheetContent = {
            BottomSheetContent()
        },
        scaffoldState = bottomSheetScaffoldState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetBackgroundColor = MaterialTheme.colorScheme.primary,
        // sheetPeekHeight = 0.dp,
        // scrimColor = Color.Red,  // Color for the fade background when you open/close the bottom sheet
    ) {
        Scaffold(
            topBar = { TopBar() },
            containerColor = ok
        ) { padding ->  // We need to pass scaffold's inner padding to content. That's why we use Box.
            Box(modifier = Modifier.padding(padding)) {
                DummyMainScreen(scope = scope, state = bottomSheetScaffoldState)
            }
        }
    }
}

@Composable
fun TopBar() {
    TopAppBar(
        title = { androidx.compose.material.Text(text = "Appname test", fontSize = 18.sp) },
        backgroundColor = androidx.compose.material.MaterialTheme.colors.primary,
        contentColor = androidx.compose.material.MaterialTheme.colors.onPrimary,
    )
}

@Preview(showBackground = true)
@Composable
fun TopBarPreview() {
    TopBar()
}


@Preview(showBackground = true)
@Composable
fun BottomSheetScaffoldScreenPreview() {
    BottomSheetScaffoldScreen()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DummyMainScreen(scope: CoroutineScope, state: BottomSheetScaffoldState) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            onClick = {
                scope.launch {
                    if (state.bottomSheetState.isCollapsed) {
                        state.bottomSheetState.expand()
                    } else {
                        state.bottomSheetState.collapse()
                    }
                }
            }) {
            if (state.bottomSheetState.isCollapsed) {
                Text(text = "Open Bottom Sheet Scaffold")
            } else {
                Text(text = "Close Bottom Sheet Scaffold")
            }
        }
    }
}

