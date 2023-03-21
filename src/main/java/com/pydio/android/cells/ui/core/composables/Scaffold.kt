package com.pydio.android.cells.ui.core.composables

import android.content.res.Configuration
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.nav.DefaultTopAppBar
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.utils.Str

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithDefaultScaffold(
    title: String,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val topAppBarState = rememberTopAppBarState()
    Scaffold(
        topBar = {
            DefaultTopAppBar(
                title = title,
                openDrawer = openDrawer,
                topAppBarState = topAppBarState
            )
        },
        modifier = modifier
    ) { innerPadding ->
        content(innerPadding)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultTopBar(
    title: String,
    back: (() -> Unit)? = null,
    openDrawer: (() -> Unit)? = null,
    openSearch: (() -> Unit)? = null,
) {

    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        navigationIcon = {
            if (back != null) {
                IconButton(onClick = { back() }) {
                    Icon(
                        imageVector = CellsIcons.ArrowBack,
                        contentDescription = stringResource(id = R.string.button_back)
                    )
                }
            } else if (openDrawer != null) {
                IconButton(
                    onClick = { openDrawer() },
                    enabled = true
                ) {
                    Icon(
                        CellsIcons.Menu,
                        contentDescription = stringResource(id = R.string.open_drawer)
                    )
                }
            }
        },
        actions = {
            if (openSearch != null) {
                IconButton(onClick = { openSearch() }) {
                    Icon(
                        CellsIcons.Search,
                        contentDescription = stringResource(id = R.string.action_search)
                    )
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithMoreMenu(
    title: String,
    back: (() -> Unit)? = null,
    openDrawer: (() -> Unit)? = null,
    openSearch: (() -> Unit)? = null,
    isActionMenuShown: Boolean,
    showMenu: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {

    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        navigationIcon = {
            if (back != null) {
                IconButton(onClick = { back() }) {
                    Icon(
                        imageVector = CellsIcons.ArrowBack,
                        contentDescription = stringResource(id = R.string.button_back)
                    )
                }
            } else if (openDrawer != null) {
                IconButton(
                    onClick = { openDrawer() },
                    enabled = true
                ) {
                    Icon(
                        CellsIcons.Menu,
                        contentDescription = stringResource(id = R.string.open_drawer)
                    )
                }
            }
        },
        actions = {
            if (openSearch != null) {
                IconButton(onClick = { openSearch() }) {
                    Icon(
                        CellsIcons.Search,
                        contentDescription = stringResource(id = R.string.action_search)
                    )
                }
            }
            IconButton(onClick = { showMenu(!isActionMenuShown) }) {
                Icon(
                    CellsIcons.MoreVert,
                    contentDescription = stringResource(R.string.open_more_menu)
                )
            }
            DropdownMenu(
                expanded = isActionMenuShown,
                onDismissRequest = { showMenu(false) },
                content = content
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithSearch(
    queryStr: String,
    errorMessage: String?,
    updateQuery: (String) -> Unit,
//    queryStr: String,
//    updateQuery: ((String) -> Unit),
    cancel: (() -> Unit),
    isActionMenuShown: Boolean,
    showMenu: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    contentPadding: PaddingValues = PaddingValues(all = 16.dp),
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = queryStr,
                label = { Icon(CellsIcons.Search, "Search") },
                supportingText = {
                    if (Str.notEmpty(errorMessage)) {
                        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                enabled = true,
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = keyboardActions,
                onValueChange = { newValue -> updateQuery(newValue) },
                modifier = Modifier.padding(contentPadding),
            )

        },
        navigationIcon = {
            IconButton(onClick = { cancel() }) {
                Icon(
                    imageVector = CellsIcons.Cancel,
                    contentDescription = stringResource(id = R.string.button_cancel)
                )
            }

        },
        actions = {
            IconButton(onClick = { showMenu(!isActionMenuShown) }) {
                Icon(
                    CellsIcons.MoreVert,
                    contentDescription = stringResource(R.string.open_more_menu)
                )
            }
            DropdownMenu(
                expanded = isActionMenuShown,
                onDismissRequest = { showMenu(false) },
                content = content
            )
        }
    )
}

/** Helper method to provide a better design depending on the user's device,
 * inspired from: [TODO]
 */
@Composable
fun extraTopPadding(isExpandedScreen: Boolean): Dp {
    return if (!isExpandedScreen) {
        0.dp
    } else {
        dimensionResource(R.dimen.expanded_screen_extra_top_padding)
    }
}

@Preview(name = "Default top bar - Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Default top bar - Dark Mode"
)
@Composable
private fun DefaultTopBarPreview() {
    CellsTheme {
        DefaultTopBar(
            "Pydio Cells server",
            { },
            null,
            { },
        )
    }
}
