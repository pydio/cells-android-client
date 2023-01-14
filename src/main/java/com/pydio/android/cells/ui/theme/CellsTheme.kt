package com.pydio.android.cells.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * A Theme for Cells based on Material3.
 */
@Composable
fun CellsTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

/**
 * A sample theme overlay to be used with dark dialogs.
 * Based on the Rally Navigation Codelab (com.example.compose.rally.ui.theme)
 */
@Composable
fun CellsDarkDialogThemeOverlay(content: @Composable () -> Unit) {

    val dialogColors = darkColorScheme(
        primary = Color.White,
        surface = Color.White.copy(alpha = 0.12f).compositeOver(Color.Black),
        onSurface = Color.White,
    )

    val currentTypography = MaterialTheme.typography
    val dialogTypography = currentTypography.copy(
        bodyMedium = currentTypography.bodyMedium.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 1.sp
        ),
        labelLarge = currentTypography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.em
        )
    )
    MaterialTheme(colorScheme = dialogColors, typography = dialogTypography, content = content)
}
