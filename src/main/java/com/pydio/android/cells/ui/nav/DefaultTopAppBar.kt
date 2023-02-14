package com.pydio.android.cells.ui.nav

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.CellsVectorIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultTopAppBar(
    title: String,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    showSearch: Boolean = false,
    topAppBarState: TopAppBarState = rememberTopAppBarState(),
) {
    TopAppBar(
        title = {
            Text(title)
        },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(
                    imageVector = CellsVectorIcons.Menu,
                    contentDescription = stringResource(R.string.open_drawer),
                    // tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            if (showSearch) {
                IconButton(onClick = { /* TODO: Open search */ }) {
                    Icon(
                        imageVector = CellsVectorIcons.Search,
                        contentDescription = stringResource(R.string.search_label)
                    )
                }
            }
        },
        modifier = modifier
    )
}
