package com.pydio.android.cells.ui.core.composables

import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.ui.theme.CellsVectorIcons

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
        navigationIcon = {
            if (back != null) {
                IconButton(onClick = { back() }) {
                    Icon(
                        imageVector = CellsVectorIcons.ArrowBack,
                        contentDescription = stringResource(id = R.string.button_back)
                    )
                }
            } else if (openDrawer != null) {
                IconButton(
                    onClick = { openDrawer() },
                    enabled = true
                ) {
                    Icon(
                        CellsVectorIcons.Menu,
                        contentDescription = stringResource(id = R.string.open_drawer)
                    )
                }
            }
        },
        actions = {
            if (openSearch != null) {
                IconButton(onClick = { openSearch() }) {
                    Icon(
                        CellsVectorIcons.Search,
                        contentDescription = stringResource(id = R.string.action_search)
                    )
                }
            }
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
