package com.pydio.android.cells.ui.system.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.PreferenceSectionTitle
import com.pydio.android.cells.ui.core.composables.SwitchSetting
import com.pydio.android.cells.ui.core.composables.dialogs.AskForConfirmation
import com.pydio.android.cells.ui.core.nav.DefaultTopAppBar
import com.pydio.android.cells.ui.system.models.HouseKeepingVM
import com.pydio.android.cells.ui.theme.CellsIcons
import kotlinx.coroutines.launch

// private const val LOG_TAG = "HouseKeeping.kt"
private const val HOUSEKEEPING = "house-keeping-screen"
private const val CONFIRM_CLEAN = "confirm-clean"

@Composable
fun HouseKeeping(
    isExpandedScreen: Boolean,
    houseKeepingVM: HouseKeepingVM,
    openDrawer: () -> Unit,
    dismiss: (Boolean) -> Unit,
) {

    val modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = dimensionResource(id = R.dimen.margin_small))

    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    NavHost(navController, HOUSEKEEPING) {
        composable(HOUSEKEEPING) {  // Fills the area provided to the NavHost
            Scaffold(
                topBar = {
                    DefaultTopAppBar(
                        title = stringResource(R.string.action_house_keeping),
                        isExpandedScreen = isExpandedScreen,
                        openDrawer = openDrawer,
                    )
                },
            ) { innerPadding ->
                val scrollingState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = dimensionResource(R.dimen.margin_medium))
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.Start)
                        .verticalScroll(scrollingState)
                ) {
                    ParameterSection(houseKeepingVM, modifier)
//                    PreferenceDivider(modifier)
                    Button(
                        onClick = { navController.navigate(CONFIRM_CLEAN) },
                        modifier
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .padding(dimensionResource(R.dimen.margin_medium))
                    ) {
                        Icon(
                            imageVector = CellsIcons.Delete,
                            contentDescription = stringResource(R.string.button_empty_cache)
                        )
                        Text(text = stringResource(R.string.button_empty_cache))
                    }
                }
            }
        }

        dialog(CONFIRM_CLEAN) {

            val desc = if (houseKeepingVM.isEraseAll()) {
                stringResource(R.string.confirm_reset_all_message)
            } else if (houseKeepingVM.isAllAccount()) {
                stringResource(R.string.confirm_for_all_accounts_message)
            } else {
                stringResource(R.string.confirm_cache_deletion_message)
            }

            AskForConfirmation(
                icon = null,
                title = stringResource(R.string.confirm_permanent_deletion_title),
                desc = desc,
                confirm = {
                    scope.launch {
                        houseKeepingVM.launchCacheClearing()
                        navController.popBackStack(HOUSEKEEPING, false)
                        dismiss(houseKeepingVM.isEraseAll())
                    }
                },
                dismiss = { navController.popBackStack(HOUSEKEEPING, false) },
            )
        }
    }
}

@Composable
private fun ParameterSection(
    houseKeepingVM: HouseKeepingVM,
    modifier: Modifier
) {
    PreferenceSectionTitle(
        stringResource(R.string.pref_clean_cache_params_title),
        modifier,
        stringResource(R.string.pref_clean_cache_params_desc)
    )

    val alsoForget = houseKeepingVM.alsoLogout.collectAsState()
    val alsoOffline = houseKeepingVM.alsoEmptyOffline.collectAsState()

    val includeAll = houseKeepingVM.includeAllAccounts.collectAsState()
    val eraseAll = houseKeepingVM.eraseAll.collectAsState()

    SwitchSetting(
        label = stringResource(R.string.pref_clean_cache_include_offline_title),
        description = stringResource(R.string.pref_clean_cache_include_offline_desc),
        isSelected = alsoOffline.value || eraseAll.value,
        isEnabled = !eraseAll.value,
        onItemClick = { houseKeepingVM.toggleEmptyOffline(it) },
        modifier = modifier,
    )

    SwitchSetting(
        label = stringResource(R.string.pref_clean_cache_forget_accounts_title),
        description = stringResource(R.string.pref_clean_cache_forget_accounts_desc),
        isSelected = alsoForget.value || eraseAll.value,
        isEnabled = !eraseAll.value,
        onItemClick = { houseKeepingVM.toggleLogout(it) },
        modifier = modifier,
    )

    SwitchSetting(
        label = stringResource(R.string.pref_clean_cache_all_accounts_title),
        description = stringResource(R.string.pref_clean_cache_all_accounts_desc),
        isSelected = includeAll.value || eraseAll.value,
        isEnabled = !eraseAll.value,
        onItemClick = { houseKeepingVM.toggleIncludeAll(it) },
        modifier = modifier,
    )

    SwitchSetting(
        label = stringResource(R.string.pref_clean_cache_clean_all_title),
        description = stringResource(R.string.pref_clean_cache_clean_all_desc),
        isSelected = eraseAll.value,
        onItemClick = { houseKeepingVM.toggleEraseAll(it) },
        modifier = modifier,
    )
}
