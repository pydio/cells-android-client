package com.pydio.android.cells.ui.core.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.ConnectionVM
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.CellsTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun WithInternetBanner(
    contentPadding: PaddingValues,
    connectionVM: ConnectionVM = koinViewModel(),
    content: @Composable () -> Unit
) {
    val network = connectionVM.liveNetwork.observeAsState()
    // TODO add Snackbar host
    // TODO add bottom sheet

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        if (!connectionVM.isConnected(network.value)) {
            ConnectionStatus(
                desc = stringResource(id = R.string.no_internet),
                icon = CellsIcons.NoInternet
            )
        } else if (connectionVM.isLimited(network.value)) {
            ConnectionStatus(
                desc = stringResource(id = R.string.metered_connection),
                icon = CellsIcons.Metered
            )
        }
        content()
    }
}

@Composable
private fun ConnectionStatus(desc: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.margin_small),
                vertical = dimensionResource(R.dimen.margin_xxsmall)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            modifier = Modifier.size(dimensionResource(id = R.dimen.list_thumb_decorator_size))
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.list_item_inner_h_padding)))
        Text(text = desc)
    }
}

@Preview
@Composable
private fun ConnectionStatusPreview() {
    CellsTheme {
        ConnectionStatus(
            desc = stringResource(id = R.string.metered_connection),
            icon = CellsIcons.Metered
        )
    }
}
