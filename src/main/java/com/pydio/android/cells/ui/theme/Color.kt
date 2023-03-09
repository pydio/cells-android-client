package com.pydio.android.cells.ui.theme

import androidx.compose.ui.graphics.Color

// v3.0 Styles

object CellsColor {

    val ok = Color(0xFF219600)
    val warning = Color(0xFFFF9800)
    val danger = Color(0xFFBA1B1B)
    // TODO
    val debug = Color(0xFFB6C9D7)
    val info = Color(0xFFCAC1E9)

    val flagBookmark = Color(0xFFFF9800)
    val flagOffline = Color(0xFF607D8B)
    val flagShare = Color(0xFF009688)

    // Document icon colors
    val material_yellow = Color(0xFFFFE500)
    val material_orange = Color(0xFFFF9800)
    val material_deep_orange = Color(0xFFFF5722)
    val material_red = Color(0xFFF44336)
    val material_green = Color(0xFF4CAF50)
    val material_blue = Color(0xFF2196F3)
    val material_indigo = Color(0xFF3F51B5)
    val material_neutral = Color(0xFF5C5F61)
}

// Secondary
val LightBlue = Color(0xFF79D0FF)
val LightBlueGrey = Color(0xFFB6C9D7)
val LightPurpleGrey = Color(0xFFCAC1E9)
val PinkRed = Color(0xFFFFB4A9)

// Default color palette
// for devices that do not support or authorize dynamic colors
object LightCellsScheme {
    val primary = Color(0xFF00668B)
    val onPrimary = Color(0xFFFFFFFF)
    val primaryContainer = Color(0xFFC3E7FF)
    val onPrimaryContainer = Color(0xFF001E2D)
    val inversePrimary = Color(0xFF79D0FF)
    val secondary = Color(0xFF4E616D)
    val onSecondary = Color(0xFFFFFFFF)
    val secondaryContainer = Color(0xFFD1E5F4)
    val onSecondaryContainer = Color(0xFF0A1E28)
    val tertiary = Color(0xFF615A7C)
    val onTertiary = Color(0xFFFFFFFF)
    val tertiaryContainer = Color(0xFFE7DDFF)
    val onTertiaryContainer = Color(0xFF1D1735)
    val background = Color(0xFFFBFCFF)
    val onBackground = Color(0xFF191C1E)
    val surface = Color(0xFFFBFCFF)
    val onSurface = Color(0xFF191C1E)
    val surfaceVariant = Color(0xFFDDE3EA)
    val onSurfaceVariant = Color(0xFF41484D)
    val surfaceTint = Color(0xFF00668B) // TODO
    val inverseSurface = Color(0xFF2E3133)
    val inverseOnSurface = Color(0xFFF0F1F4)
    val error = Color(0xFFBA1B1B)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFFDAD4)
    val onErrorContainer = Color(0xFF410001)
    val outline = Color(0xFF71787E)
    val outlineVariant = Color(0xFF00668B) // TODO
    val scrim = Color(0xFF00668B) // TODO
}

object DarkCellsScheme {
    val primary = Color(0xFF79D0FF)
    val onPrimary = Color(0xFF00344A)
    val primaryContainer = Color(0xFF004C6A)
    val onPrimaryContainer = Color(0xFFC3E7FF)
    val inversePrimary = Color(0xFF00668B)
    val secondary = Color(0xFFB6C9D7)
    val onSecondary = Color(0xFF20333E)
    val secondaryContainer = Color(0xFF374955)
    val onSecondaryContainer = Color(0xFFD1E5F4)
    val tertiary = Color(0xFFCAC1E9)
    val onTertiary = Color(0xFF322C4B)
    val tertiaryContainer = Color(0xFF494264)
    val onTertiaryContainer = Color(0xFFE7DDFF)
    val background = Color(0xFF191C1E)
    val onBackground = Color(0xFFE1E2E5)
    val surface = Color(0xFF191C1E)
    val onSurface = Color(0xFFE1E2E5)
    val surfaceVariant = Color(0xFF41484D)
    val onSurfaceVariant = Color(0xFFC0C7CD)
    val surfaceTint = Color(0xFF00FF00) // TODO
    val inverseSurface = Color(0xFFE1E2E5)
    val inverseOnSurface = Color(0xFF191C1E)
    val error = Color(0xFFFFB4A9)
    val onError = Color(0xFF680003)
    val errorContainer = Color(0xFF930006)
    val onErrorContainer = Color(0xFFFFDAD4)
    val outline = Color(0xFF8B9298)
    val outlineVariant = Color(0xFF00FF00) // TODO
    val scrim = Color(0xFF00FF00) // TODO
}
