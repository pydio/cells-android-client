package com.pydio.android.cells.ui.box

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.ui.models.AccountListVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.nav.MainNavDrawer
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainHost(
    initialStateID: StateID,
    launchIntent: (Intent?, Boolean, Boolean) -> Unit,
    widthSizeClass: WindowWidthSizeClass,
    browseRemoteVM: BrowseRemoteVM = koinViewModel(),
    accountListVM: AccountListVM = koinViewModel(),
) {

    val navController = rememberNavController()

    MainNavDrawer(
        initialStateID,
        launchIntent,
        navController = navController,
        widthSizeClass
    )
}

@Composable
fun UseCellsTheme(content: @Composable () -> Unit) {
    CellsTheme {
        Surface(
            modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}

