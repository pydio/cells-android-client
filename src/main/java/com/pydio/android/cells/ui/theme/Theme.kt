package com.pydio.android.cells.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat

private val DarkColorScheme = darkColorScheme(

    primary = DarkCellsScheme.primary,
    onPrimary = DarkCellsScheme.onPrimary,
    primaryContainer = DarkCellsScheme.primaryContainer,
    onPrimaryContainer = DarkCellsScheme.onPrimaryContainer,
    inversePrimary = DarkCellsScheme.inversePrimary,
    secondary = DarkCellsScheme.secondary,
    onSecondary = DarkCellsScheme.onSecondary,
    secondaryContainer = DarkCellsScheme.secondaryContainer,
    onSecondaryContainer = DarkCellsScheme.onSecondaryContainer,
    tertiary = DarkCellsScheme.tertiary,
    onTertiary = DarkCellsScheme.onTertiary,
    tertiaryContainer = DarkCellsScheme.tertiaryContainer,
    onTertiaryContainer = DarkCellsScheme.onTertiaryContainer,
    background = DarkCellsScheme.background,
    onBackground = DarkCellsScheme.onBackground,
    surface = DarkCellsScheme.surface,
    onSurface = DarkCellsScheme.onSurface,
    surfaceVariant = DarkCellsScheme.surfaceVariant,
    onSurfaceVariant = DarkCellsScheme.onSurfaceVariant,
    surfaceTint = DarkCellsScheme.surfaceTint,
    inverseSurface = DarkCellsScheme.inverseSurface,
    inverseOnSurface = DarkCellsScheme.inverseOnSurface,
    error = DarkCellsScheme.error,
    onError = DarkCellsScheme.onError,
    errorContainer = DarkCellsScheme.errorContainer,
    onErrorContainer = DarkCellsScheme.onErrorContainer,
    outline = DarkCellsScheme.outline,
    outlineVariant = DarkCellsScheme.outlineVariant,
    scrim = DarkCellsScheme.scrim,
)

private val LightColorScheme = lightColorScheme(

    primary = LightCellsScheme.primary,
    onPrimary = LightCellsScheme.onPrimary,
    primaryContainer = LightCellsScheme.primaryContainer,
    onPrimaryContainer = LightCellsScheme.onPrimaryContainer,
    inversePrimary = LightCellsScheme.inversePrimary,
    secondary = LightCellsScheme.secondary,
    onSecondary = LightCellsScheme.onSecondary,
    secondaryContainer = LightCellsScheme.secondaryContainer,
    onSecondaryContainer = LightCellsScheme.onSecondaryContainer,
    tertiary = LightCellsScheme.tertiary,
    onTertiary = LightCellsScheme.onTertiary,
    tertiaryContainer = LightCellsScheme.tertiaryContainer,
    onTertiaryContainer = LightCellsScheme.onTertiaryContainer,
    background = LightCellsScheme.background,
    onBackground = LightCellsScheme.onBackground,
    surface = LightCellsScheme.surface,
    onSurface = LightCellsScheme.onSurface,
    surfaceVariant = LightCellsScheme.surfaceVariant,
    onSurfaceVariant = LightCellsScheme.onSurfaceVariant,
    surfaceTint = LightCellsScheme.surfaceTint,
    inverseSurface = LightCellsScheme.inverseSurface,
    inverseOnSurface = LightCellsScheme.inverseOnSurface,
    error = LightCellsScheme.error,
    onError = LightCellsScheme.onError,
    errorContainer = LightCellsScheme.errorContainer,
    onErrorContainer = LightCellsScheme.onErrorContainer,
    outline = LightCellsScheme.outline,
    outlineVariant = LightCellsScheme.outlineVariant,
    scrim = LightCellsScheme.scrim,
)

/**
 * Root theme for the Cells application based on Material3.
 */
@Composable
fun CellsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }


    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = colorScheme.surfaceVariant.toArgb()
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
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
