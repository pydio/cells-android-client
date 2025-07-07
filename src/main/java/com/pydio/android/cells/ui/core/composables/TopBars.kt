package com.pydio.android.cells.ui.core.composables

import android.content.res.Configuration
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.cells.transport.StateID

// private const val LOG_TAG = "TopBars.kt"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultTopBar(
    title: String,
    isExpandedScreen: Boolean = false,
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        navigationIcon = {
            if (isExpandedScreen) {
                // No icon there for expended screen.
            } else if (back != null) {
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
fun MultiSelectTopBar(
    selected: Set<StateID>,
    cancel: () -> Unit,
    isMoreMenuShown: Boolean,
    showMenu: (Boolean) -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "${selected.size} selected",
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        navigationIcon = {
            IconButton(onClick = { cancel() }) {
                Icon(
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    imageVector = CellsIcons.Cancel,
                    contentDescription = stringResource(id = R.string.button_cancel)
                )
            }

        },
        actions = {
            IconButton(onClick = { showMenu(!isMoreMenuShown) }) {
                Icon(
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    imageVector = CellsIcons.MoreVert,
                    contentDescription = stringResource(R.string.open_more_menu)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithMoreMenu(
    title: String,
    isExpandedScreen: Boolean = false,
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
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        navigationIcon = {
            if (isExpandedScreen) {
                // show nothing
            } else if (back != null) {
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
fun TopBarWithActions(
    title: String,
    back: (() -> Unit)? = null,
    openDrawer: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
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
        actions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TopBarWithSearch(
    queryStr: String,
    // errorMessage: ErrorMessage?,
    updateQuery: (String) -> Unit,
    cancel: (() -> Unit),
    isActionMenuShown: Boolean,
    showMenu: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    imeAction: ImeAction = ImeAction.Done
) {

    val focusRequester = remember { FocusRequester() }

    // val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val keyboardController = LocalSoftwareKeyboardController.current

    val onDone: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val keyboardActions = KeyboardActions(
        onNext = { onDone() },
        onDone = { onDone() }
    )

    val modifier = Modifier
        .onPreviewKeyEvent {
            if (it.key == Key.Tab && it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                focusManager.moveFocus(FocusDirection.Down)
                true
            } else if (it.key == Key.Enter) {
                onDone()
                true
            } else {
                false
            }
        }
        .focusRequester(focusRequester)


    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        title = {
            TextField(
                value = queryStr,
                // textStyle = MaterialTheme.typography.bodyMedium,
                // label = { Icon(CellsIcons.Search, "Search") },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_label),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f)
                    )
                },
//                supportingText = {
//                    errorMessage?.let {
//                        Text(
//                            text = toErrorMessage(context, errorMessage),
//                            color = MaterialTheme.colorScheme.error
//                        )
//
//                    }
//                },
                enabled = true,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = keyboardActions,
                onValueChange = { newValue -> updateQuery(newValue) },
                modifier = modifier.fillMaxWidth()
                    .padding(  bottom = dimensionResource(R.dimen.margin_xsmall)),
            )
        },
        navigationIcon = {
            IconButton(onClick = { cancel() }) {
                Icon(
                    imageVector = CellsIcons.CancelSearch,
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
        },
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)

        //     .height(96.dp)
        //    .wrapContentHeight(CenterVertically)
        // .wrapContentHeight(Top)
        //     .padding(top = dimensionResource(R.dimen.margin_xsmall)),
    )

    LaunchedEffect(key1 = Unit) {
        // Log.e(LOG_TAG, "... Showing search app bar")
        focusRequester.requestFocus()
    }
}

/** Helper method to provide a better design depending on the user's device,
 * inspired from: [TODO]
 */
//@Composable
//fun extraTopPadding(isExpandedScreen: Boolean): Dp {
//    return if (!isExpandedScreen) {
//        0.dp
//    } else {
//        dimensionResource(R.dimen.expanded_screen_extra_top_padding)
//    }
//}

@Preview(name = "Default top bar - Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Default top bar - Dark Mode"
)
@Composable
private fun DefaultTopBarPreview() {
    UseCellsTheme {
        DefaultTopBar(
            "Pydio Cells server",
            false,
            { },
            null,
            { },
        )
    }
}

@Preview(name = "Expanded top bar - Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Expanded top bar - Dark Mode"
)
@Composable
private fun ExpandedTopBarPreview() {
    UseCellsTheme {
        DefaultTopBar(
            "Pydio Cells server",
            true,
            { },
            null,
            { },
        )
    }
}
