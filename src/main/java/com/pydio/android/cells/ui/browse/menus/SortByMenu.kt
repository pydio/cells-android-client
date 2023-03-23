package com.pydio.android.cells.ui.browse.menus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.ListType
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.browse.models.SortByMenuVM
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetDivider
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetListItem
import com.pydio.android.cells.ui.core.composables.menus.GenericBottomSheetHeader
import com.pydio.android.cells.ui.theme.CellsIcons
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val logTag = "SortByMenu"

@Composable
fun SortByMenu(
    type: ListType,
    done: () -> Unit,
) {
    val sortByMenuVM: SortByMenuVM = koinViewModel(parameters = { parametersOf(type) })

    val (keys, labels) = when (type) {
        ListType.TRANSFER ->
            stringArrayResource(R.array.transfer_order_by_values) to stringArrayResource(R.array.transfer_order_by_labels)
        ListType.JOB ->
            stringArrayResource(R.array.job_order_by_values) to stringArrayResource(R.array.job_order_by_labels)
        ListType.DEFAULT ->
            stringArrayResource(R.array.order_by_values) to stringArrayResource(R.array.order_by_labels)
    }

    val selectedOrder =
        sortByMenuVM.encodedOrder.collectAsState(initial = AppNames.DEFAULT_SORT_ENCODED)

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(id = R.dimen.bottom_sheet_v_spacing))
            .verticalScroll(scrollState)

    ) {
        GenericBottomSheetHeader(
            icon = CellsIcons.SortBy,
            title = stringResource(id = R.string.sort_by),
        )

        BottomSheetDivider()

        for (i in keys.indices) {
            BottomSheetListItem(
                icon = null,
                title = labels[i],
                onItemClick = {
                    sortByMenuVM.setSortBy(keys[i])
                    done()
                },
                selected = keys[i] == selectedOrder.value,
            )
        }
    }
}
