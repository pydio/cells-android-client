package com.pydio.android.cells.ui.core.nav

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.ConnectionVM
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.android.cells.ui.browse.BrowseNavigationActions
import com.pydio.android.cells.ui.browse.screens.HomeHeader
import com.pydio.android.cells.ui.core.composables.BottomSheetDivider
import com.pydio.android.cells.ui.core.composables.MenuTitleText
import com.pydio.android.cells.ui.core.composables.getWsThumbVector
import com.pydio.android.cells.ui.system.SystemDestinations
import com.pydio.android.cells.ui.system.SystemNavigationActions
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.transport.StateID

private const val logTag = "AppDrawer"

/** AppDrawer provides the main drawer menu for small screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    currRoute: String?,
    currSelectedID: StateID?,
    connectionVM: ConnectionVM,
    cellsNavActions: CellsNavigationActions,
    systemNavActions: SystemNavigationActions,
    browseNavActions: BrowseNavigationActions,
    closeDrawer: () -> Unit,
) {

    val defaultPadding = NavigationDrawerItemDefaults.ItemPadding
    val defaultModifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    val accountID = connectionVM.currAccountID.observeAsState()
    val wss = connectionVM.wss.observeAsState()
    val cells = connectionVM.cells.observeAsState()

    ModalDrawerSheet(
        windowInsets = WindowInsets.systemBars
            //.only(if (excludeTop) WindowInsetsSides.Bottom else WindowInsetsSides.Vertical)
            .add(WindowInsets(bottom = 12.dp))
    ) {

        LazyColumn {

//            item {
//                PydioLogo(
//                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
//                )
//
            item {
                HomeHeader(
                    username = accountID.value?.username ?: "",
                    address = accountID.value?.serverUrl ?: "None defined",
                    openAccounts = { cellsNavActions.navigateToAccounts(); closeDrawer() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(defaultPadding)
                        .padding(
//                            vertical = dimensionResource(R.dimen.menu_padding)
                            bottom = 12.dp,
                            top = 20.dp
                        )
                )
            }
            // Offline, Bookmark, Transfers and Workspace roots accesses:
            // This section is only relevant when we have a defined account
            accountID.value?.let { currAccountID ->
                Log.d(logTag, "... Composing account part of the drawer for $currRoute")
                item {
                    MyNavigationDrawerItem(
                        label = stringResource(R.string.action_open_offline_roots),
                        icon = CellsIcons.KeepOfflineOld,
                        selected = BrowseDestinations.OfflineRoots.isCurrent(currRoute),
                        onClick = { browseNavActions.toOfflineRoots(currAccountID); closeDrawer() },
                        modifier = defaultModifier
                    )
                    MyNavigationDrawerItem(
                        label = stringResource(R.string.action_open_bookmarks),
                        iconID = R.drawable.aa_new_star_40px,
                        selected = BrowseDestinations.Bookmarks.isCurrent(currRoute),
                        onClick = { browseNavActions.toBookmarks(currAccountID);closeDrawer() },
                        modifier = defaultModifier
                    )
                    MyNavigationDrawerItem(
                        label = stringResource(R.string.action_open_transfers),
                        icon = CellsIcons.Transfers,
                        selected = BrowseDestinations.Transfers.isCurrent(currRoute),
                        onClick = { browseNavActions.toTransfers(currAccountID); closeDrawer() },
                        modifier = defaultModifier
                    )

                    BottomSheetDivider(defaultModifier)
                    MenuTitleText(stringResource(R.string.my_workspaces), defaultModifier)

                    wss.value?.listIterator()?.forEach {
                        val selected = BrowseDestinations.Open.isCurrent(currRoute)
                                && it.getStateID() == currSelectedID
                        MyNavigationDrawerItem(
                            label = it.label ?: it.slug,
                            icon = getWsThumbVector(it.sortName ?: ""),
                            selected = selected,
                            onClick = { browseNavActions.toBrowse(it.getStateID());closeDrawer() },
                            modifier = defaultModifier
                        )
                    }

                    cells.value?.listIterator()?.forEach {
                        val selected = BrowseDestinations.Open.isCurrent(currRoute)
                                && it.getStateID() == currSelectedID
                        MyNavigationDrawerItem(
                            label = it.label ?: it.slug,
                            icon = getWsThumbVector(it.sortName ?: ""),
                            selected = selected,
                            onClick = { browseNavActions.toBrowse(it.getStateID()); closeDrawer() },
                            modifier = defaultModifier
                        )
                    }
                }
            } ?: run { // Temporary fallback when no account is defined
                // until all routes are hardened for all corner cases
                item {
                    MyNavigationDrawerItem(
                        label = stringResource(id = R.string.choose_account),
                        icon = Icons.Filled.Group,
                        selected = CellsDestinations.Accounts.route == currRoute,
                        onClick = { cellsNavActions.navigateToAccounts();closeDrawer() },
                        modifier = defaultModifier
                    )
                }
            }

            item {
                BottomSheetDivider(defaultModifier)
                MenuTitleText(stringResource(R.string.my_account), defaultModifier)

                MyNavigationDrawerItem(
                    label = stringResource(R.string.action_settings),
                    icon = CellsIcons.Settings,
                    selected = SystemDestinations.Settings.route == currRoute,
                    onClick = { systemNavActions.navigateToSettings(); closeDrawer() },
                    modifier = defaultModifier
                )
                accountID.value?.let { accID -> // We also temporarily disable this when no account is defined
                    // TODO Remove the check once the "clear cache" / housekeeping strategy has been refined
                    MyNavigationDrawerItem(
                        label = stringResource(R.string.action_clear_cache),
                        icon = CellsIcons.EmptyRecycle,
                        selected = SystemDestinations.ClearCache.isCurrent(currRoute),
                        onClick = { systemNavActions.navigateToClearCache(accID); closeDrawer() },
                        modifier = defaultModifier
                    )
                }
                MyNavigationDrawerItem(
                    label = stringResource(R.string.action_open_jobs),
                    icon = CellsIcons.Jobs,
                    selected = SystemDestinations.Jobs.route == currRoute,
                    onClick = { systemNavActions.navigateToJobs(); closeDrawer() },
                    modifier = defaultModifier
                )
                MyNavigationDrawerItem(
                    label = stringResource(R.string.action_open_logs),
                    icon = CellsIcons.Logs,
                    selected = SystemDestinations.Logs.route == currRoute,
                    onClick = { systemNavActions.navigateToLogs(); closeDrawer() },
                    modifier = defaultModifier
                )
                MyNavigationDrawerItem(
                    label = stringResource(id = R.string.action_open_about),
                    icon = CellsIcons.About,
                    selected = SystemDestinations.About.route == currRoute,
                    onClick = { systemNavActions.navigateToAbout(); closeDrawer() },
                    modifier = defaultModifier,
                )
            }
        }
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyNavigationDrawerItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconID: Int,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(painterResource(iconID), label) },
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(dimensionResource(id = R.dimen.menu_item_height)),
        shape = ShapeDefaults.Small,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyNavigationDrawerItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, label) },
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(dimensionResource(id = R.dimen.menu_item_height)),
        shape = ShapeDefaults.Small,
    )
    //    badge: (@Composable () -> Unit)? = null,
    //    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    //    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
}

@Preview("Drawer contents")
@Preview("Drawer contents (dark)", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewAppDrawer() {
    CellsTheme {
//        AppDrawer(
//            currAccountID = Transport.UNDEFINED_STATE_ID,
//            currentRoute = CellsDestinations.Home.route,
//            navigateToHome = {},
//            navigateToBrowse = {},
//            navigateToAccounts = {},
//            navigateToAbout = {},
//            closeDrawer = { }
//        )
    }
}
