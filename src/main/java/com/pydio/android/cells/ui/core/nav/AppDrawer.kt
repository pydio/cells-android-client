package com.pydio.android.cells.ui.core.nav

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.services.ConnectionService
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.android.cells.ui.browse.BrowseNavigationActions
import com.pydio.android.cells.ui.core.composables.MenuTitleText
import com.pydio.android.cells.ui.core.composables.getWsThumbVector
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetDivider
import com.pydio.android.cells.ui.system.SystemDestinations
import com.pydio.android.cells.ui.system.SystemNavigationActions
import com.pydio.android.cells.ui.system.models.PrefReadOnlyVM
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel

// private const val logTag = "AppDrawer"

/** AppDrawer provides the main drawer menu for small screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    currRoute: String?,
    currSelectedID: StateID?,
    closeDrawer: () -> Unit,
    prefReadOnlyVM: PrefReadOnlyVM = koinViewModel(),
    connectionService: ConnectionService,
    cellsNavActions: CellsNavigationActions,
    systemNavActions: SystemNavigationActions,
    browseNavActions: BrowseNavigationActions
) {

    val showDebugTools = prefReadOnlyVM.showDebugTools.collectAsState(initial = false)
    val accountID = connectionService.currAccountID.collectAsState(StateID.NONE)
    val wss = connectionService.wss.collectAsState(listOf())
    val cells = connectionService.cells.collectAsState(listOf())

    val defaultPadding = PaddingValues(horizontal = dimensionResource(R.dimen.horizontal_padding))
    val defaultModifier = Modifier.padding(defaultPadding)
    val defaultTitleModifier = defaultModifier.padding(
        PaddingValues(
            top = 12.dp,
            bottom = 8.dp,
        )
    )

    ModalDrawerSheet(
        windowInsets = WindowInsets.systemBars
            //.only(if (excludeTop) WindowInsetsSides.Bottom else WindowInsetsSides.Vertical)
            .add(WindowInsets(bottom = 12.dp))
    ) {

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            AccountHeader(
                username = accountID.value?.username ?: stringResource(R.string.ask_url_title),
                address = accountID.value?.serverUrl ?: "",
                openAccounts = { cellsNavActions.navigateToAccounts(); closeDrawer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(defaultPadding)
                    .padding(
                        bottom = 12.dp,
                        top = 20.dp,
                    )
            )

            // Offline, Bookmark, Transfers and Workspace roots accesses:
            // This section is only relevant when we have a defined account
            accountID.value?.let { currAccountID ->

                MyNavigationDrawerItem(
                    label = stringResource(R.string.action_open_offline_roots),
                    icon = CellsIcons.KeepOffline,
                    selected = BrowseDestinations.OfflineRoots.isCurrent(currRoute),
                    onClick = { browseNavActions.toOfflineRoots(currAccountID); closeDrawer() },
                )
                MyNavigationDrawerItem(
                    label = stringResource(R.string.action_open_bookmarks),
                    icon = CellsIcons.Bookmark,
                    selected = BrowseDestinations.Bookmarks.isCurrent(currRoute),
                    onClick = { browseNavActions.toBookmarks(currAccountID);closeDrawer() },
                )
                MyNavigationDrawerItem(
                    label = stringResource(R.string.action_open_transfers),
                    icon = CellsIcons.Transfers,
                    selected = BrowseDestinations.Transfers.isCurrent(currRoute),
                    onClick = { browseNavActions.toTransfers(currAccountID); closeDrawer() },
                )

                BottomSheetDivider()

                MenuTitleText(stringResource(R.string.my_workspaces), defaultTitleModifier)
                wss.value.listIterator().forEach {
                    val selected = BrowseDestinations.Open.isCurrent(currRoute)
                            && it.getStateID() == currSelectedID
                    MyNavigationDrawerItem(
                        label = it.label ?: it.slug,
                        icon = getWsThumbVector(it.sortName ?: ""),
                        selected = selected,
                        onClick = { browseNavActions.toBrowse(it.getStateID());closeDrawer() },
                    )
                }
                cells.value.listIterator().forEach {
                    val selected = BrowseDestinations.Open.isCurrent(currRoute)
                            && it.getStateID() == currSelectedID
                    MyNavigationDrawerItem(
                        label = it.label ?: it.slug,
                        icon = getWsThumbVector(it.sortName ?: ""),
                        selected = selected,
                        onClick = { browseNavActions.toBrowse(it.getStateID()); closeDrawer() },
                    )
                }
            } ?: run { // Temporary fallback when no account is defined
                // until all routes are hardened for all corner cases
                MyNavigationDrawerItem(
                    label = stringResource(id = R.string.choose_account),
                    icon = Icons.Filled.Group,
                    selected = CellsDestinations.Accounts.route == currRoute,
                    onClick = { cellsNavActions.navigateToAccounts();closeDrawer() },
                )
            }

            BottomSheetDivider()

            MenuTitleText(stringResource(R.string.my_account), defaultTitleModifier)
            MyNavigationDrawerItem(
                label = stringResource(R.string.action_settings),
                icon = CellsIcons.Settings,
                selected = SystemDestinations.Settings.route == currRoute,
                onClick = { systemNavActions.navigateToSettings(); closeDrawer() },
            )
            accountID.value?.let { accID -> // We also temporarily disable this when no account is defined
                // TODO Remove the check once the "clear cache" / housekeeping strategy has been refined
                MyNavigationDrawerItem(
                    label = stringResource(R.string.action_clear_cache),
                    icon = CellsIcons.EmptyRecycle,
                    selected = SystemDestinations.ClearCache.isCurrent(currRoute),
                    onClick = { systemNavActions.navigateToClearCache(accID); closeDrawer() },
                )
            }
            if (showDebugTools.value) {
                MyNavigationDrawerItem(
                    label = stringResource(R.string.action_open_jobs),
                    icon = CellsIcons.Jobs,
                    selected = SystemDestinations.Jobs.route == currRoute,
                    onClick = { systemNavActions.navigateToJobs(); closeDrawer() },
                )
                MyNavigationDrawerItem(
                    label = stringResource(R.string.action_open_logs),
                    icon = CellsIcons.Logs,
                    selected = SystemDestinations.Logs.route == currRoute,
                    onClick = { systemNavActions.navigateToLogs(); closeDrawer() },
                )
            }
            MyNavigationDrawerItem(
                label = stringResource(id = R.string.action_open_about),
                icon = CellsIcons.About,
                selected = SystemDestinations.About.route == currRoute,
                onClick = { systemNavActions.navigateToAbout(); closeDrawer() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyNavigationDrawerItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, label) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.height(dimensionResource(R.dimen.menu_item_height)),
        shape = ShapeDefaults.Small,
    )
}
