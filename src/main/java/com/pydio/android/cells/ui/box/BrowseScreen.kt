package com.pydio.android.cells.ui.box

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.pydio.android.cells.ui.models.BrowseLocalFoldersVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM

private const val logTag = "BrowseScreen.kt"

@Composable
fun BrowseScreen(
    navController: NavHostController,
    browseLocalVM: BrowseLocalFoldersVM,
    browseRemoteVM: BrowseRemoteVM,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current

    val currLoadingState by browseRemoteVM.isLoading.observeAsState()

    Text(text = "Implement me")
}
