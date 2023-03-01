package com.pydio.android.cells.ui.browse.menus

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.browse.models.PreferencesVM
import com.pydio.android.cells.ui.core.composables.BottomSheetDivider
import com.pydio.android.cells.ui.core.composables.BottomSheetListItem
import com.pydio.android.cells.ui.core.composables.GenericBottomSheetHeader
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.utils.Log
import org.koin.androidx.compose.koinViewModel

private const val logTag = "SortByMenu"

@Composable
fun SortByMenu(
    done: () -> Unit,
    tint: Color,
    bgColor: Color,
    preferencesVM: PreferencesVM = koinViewModel()
) {
    val keys = stringArrayResource(R.array.order_by_values)
    val labels = stringArrayResource(R.array.order_by_labels)

    val selectedOrder = preferencesVM.sortBy.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(vertical = dimensionResource(id = R.dimen.bottom_sheet_v_spacing)),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            GenericBottomSheetHeader(
                icon = CellsIcons.SortBy,
                title = stringResource(id = R.string.sort_by),
            )
        }
        item { BottomSheetDivider() }
        for (i in keys.indices) {
            val selected = keys[i] == selectedOrder.value
            item {
                BottomSheetListItem(
                    icon = null,
                    title = labels[i],
                    onItemClick = {
                        Log.d(logTag, "New order: ${keys[i]}")
                        preferencesVM.setSortBy(keys[i])
                        done()
                    },
                    tint = if (selected) MaterialTheme.colorScheme.inverseOnSurface else tint,
                    bgColor = if (selected) MaterialTheme.colorScheme.inverseSurface else bgColor,
                )
            }
        }
    }
}
