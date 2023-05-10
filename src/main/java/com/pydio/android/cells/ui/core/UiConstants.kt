package com.pydio.android.cells.ui.core

enum class LoadingState {
    NEW, STARTING, PROCESSING, IDLE, SERVER_UNREACHABLE
}

// see: https://kotlinlang.org/docs/enum-classes.html#working-with-enum-constants
enum class ListLayout {
    LIST, SMALL_GRID, GRID, LARGE_GRID
}
