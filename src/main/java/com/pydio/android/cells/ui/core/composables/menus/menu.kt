package com.pydio.android.cells.ui.core.composables.menus

import androidx.compose.ui.graphics.vector.ImageVector

interface IMenuItem {
    fun onClick()
}

class SimpleMenuItem(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit,
    val selected: Boolean = false,
) : IMenuItem {
    override fun onClick() {
        onClick()
    }
}