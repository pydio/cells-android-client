package com.pydio.android.cells.ui.nav

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID

private const val logTag = "AppDrawer"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    currAccountID: StateID,
    currentRoute: String?,
    navigateToHome: () -> Unit,
    navigateToBrowse: (StateID) -> Unit,
    navigateToAccounts: () -> Unit,
//    navigateToLogin: (StateID) -> Unit,
    navigateToAbout: () -> Unit,
    closeDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {

    Log.e(logTag, "--- Got a new account: $currAccountID")
    ModalDrawerSheet(modifier) {
        PydioLogo(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.action_home)) },
            icon = { Icon(Icons.Filled.Home, null) },
            selected = CellsDestinations.Home.route == currentRoute,
            onClick = { navigateToHome(); closeDrawer() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.action_browse)) },
            icon = { Icon(Icons.Filled.Explore, null) },
            selected = CellsDestinations.Browse.route == currentRoute,
            onClick = { navigateToHome(); closeDrawer() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.switch_account)) },
            icon = { Icon(Icons.Filled.Group, null) },
            selected = CellsDestinations.Accounts.route == currentRoute,
            onClick = { navigateToAccounts();closeDrawer() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(id = R.string.action_open_about)) },
            icon = { Icon(Icons.Filled.ListAlt, null) },
            selected = SystemDestinations.ABOUT_ROUTE == currentRoute,
            onClick = { navigateToAbout(); closeDrawer() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

@Composable
private fun PydioLogo(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Icon(
            painterResource(R.drawable.pydio_logo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
//        Spacer(Modifier.width(8.dp))
//        Icon(
//            painter = painterResource(R.drawable.ic_jetnews_wordmark),
//            contentDescription = stringResource(R.string.app_name),
//            tint = MaterialTheme.colorScheme.onSurfaceVariant
//        )
    }
}

@Preview("Drawer contents")
@Preview("Drawer contents (dark)", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewAppDrawer() {
    CellsTheme {
        AppDrawer(
            currAccountID = Transport.UNDEFINED_STATE_ID,
            currentRoute = CellsDestinations.Home.route,
            navigateToHome = {},
            navigateToBrowse = {},
            navigateToAccounts = {},
            navigateToAbout = {},
            closeDrawer = { }
        )
    }
}
