package com.pydio.android.cells.ui.core.nav

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.CellsIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultTopAppBar(
    title: String,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    isExpandedScreen: Boolean = false,
    showSearch: Boolean = false,
) {
    TopAppBar(
        title = {
            Text(title)
        },
        navigationIcon = {
            if (!isExpandedScreen){
                IconButton(onClick = openDrawer) {
                    Icon(
                        imageVector = CellsIcons.Menu,
                        contentDescription = stringResource(R.string.open_drawer),
                    )
                }
            }
        },
        actions = {
            if (showSearch) {
                IconButton(onClick = { /* TODO: Open search */ }) {
                    Icon(
                        imageVector = CellsIcons.Search,
                        contentDescription = stringResource(R.string.search_label)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    )
}
