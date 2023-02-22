package com.pydio.android.cells.ui.core.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.ConnectionVM
import com.pydio.android.cells.ui.nav.CellsDestinations
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.transport.StateID

private enum class Status {
    OK, WARNING, DANGER
}

@Composable
fun WithInternetBanner(
    contentPadding: PaddingValues,
    connectionVM: ConnectionVM, // = koinViewModel(),
    navigateTo: (String, StateID) -> Unit,
    content: @Composable () -> Unit
) {
//    val network = connectionVM.liveNetwork.observeAsState()
//    val sessionStatus = connectionVM.sessionStatus.observeAsState()
    val sessionStatus = connectionVM.sessionStatusFlow
        .collectAsState(initial = ConnectionVM.SessionStatus.OK)

    // TODO add Snackbar host
    // TODO add bottom sheet

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        if (ConnectionVM.SessionStatus.OK != sessionStatus.value) {
            when (sessionStatus.value) {
                ConnectionVM.SessionStatus.NO_INTERNET
                -> ConnectionStatus(
                    icon = CellsIcons.NoInternet,
                    desc = stringResource(R.string.no_internet)

                )
                ConnectionVM.SessionStatus.METERED,
                ConnectionVM.SessionStatus.ROAMING
                -> ConnectionStatus(
                    icon = CellsIcons.Metered,
                    desc = stringResource(R.string.metered_connection),
                    type = Status.WARNING
                )
                ConnectionVM.SessionStatus.CAN_RELOG
                -> CredExpiredStatus(
                    icon = CellsIcons.NoValidCredentials,
                    desc = stringResource(R.string.auth_err_expired),
                    type = Status.WARNING,
                    onClick = {
                        connectionVM.sessionView.value?.let {
                            navigateTo(CellsDestinations.Login.route, it.getStateID())
                        }
                    }
                )
                //ConnectionVM.SessionStatus.NOT_LOGGED_IN,
                else -> ConnectionStatus(
                    icon = CellsIcons.NoValidCredentials,
                    desc = stringResource(id = R.string.auth_err_no_token),
                    type = Status.DANGER
                )
            }
        }
        content()
    }
}

@Composable
private fun ConnectionStatus(icon: ImageVector, desc: String, type: Status = Status.OK) {

    val tint = when (type) {
        Status.WARNING -> CellsColor.warning
        Status.DANGER -> CellsColor.danger
        else -> MaterialTheme.colorScheme.onSurface
    }

    val bg = when (type) {
        Status.WARNING -> CellsColor.warning.copy(alpha = .1f)
        Status.DANGER -> CellsColor.danger.copy(alpha = .1f)
        else -> MaterialTheme.colorScheme.surface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(
                horizontal = dimensionResource(R.dimen.margin_small),
                vertical = dimensionResource(R.dimen.margin_xxsmall)
            )
    ) {
        Icon(
            tint = tint,
            imageVector = icon,
            contentDescription = desc,
            modifier = Modifier.size(dimensionResource(id = R.dimen.list_thumb_decorator_size))
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.list_item_inner_h_padding)))
        Text(
            text = desc,
            color = tint,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CredExpiredStatus(
    icon: ImageVector,
    desc: String,
    type: Status = Status.WARNING,
    onClick: () -> Unit
) {

    val tint = when (type) {
        Status.WARNING -> CellsColor.warning
        Status.DANGER -> CellsColor.danger
        else -> MaterialTheme.colorScheme.onSurface
    }

    val bg = when (type) {
        Status.WARNING -> CellsColor.warning.copy(alpha = .1f)
        Status.DANGER -> CellsColor.danger.copy(alpha = .1f)
        else -> MaterialTheme.colorScheme.surface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(
                horizontal = dimensionResource(R.dimen.margin_small),
                vertical = dimensionResource(R.dimen.margin_xxsmall)
            )
    ) {
        Icon(
            tint = tint,
            imageVector = icon,
            contentDescription = desc,
            modifier = Modifier.size(dimensionResource(id = R.dimen.list_button_size))
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.list_item_inner_h_padding)))
        Text(
            text = desc,
            color = tint,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClick) {
            Text(
                text = stringResource(R.string.launch_auth).uppercase(),
                color = tint,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}


@Preview
@Composable
private fun ConnectionStatusPreview() {
    CellsTheme {
        ConnectionStatus(
            icon = CellsIcons.Metered,
            desc = stringResource(id = R.string.metered_connection),
            Status.WARNING
        )
    }
}
