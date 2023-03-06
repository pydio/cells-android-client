package com.pydio.android.cells.ui.browse.composables

private const val logTag = "TransferWithActions.kt"

sealed class TransferAction(val id: String) {
    object Delete : TransferAction("delete")
    object SortBy : TransferAction("sort_by")
    object FilterBy : TransferAction("filter_by")
}
