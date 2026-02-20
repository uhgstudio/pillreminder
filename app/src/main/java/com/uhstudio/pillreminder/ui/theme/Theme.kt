package com.uhstudio.pillreminder.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Stitch Design forces Light Mode (Warm Theme)
private val StitchLightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = WarmPrimary,
    onPrimary = WarmOnPrimary,
    primaryContainer = WarmPrimaryContainer,
    onPrimaryContainer = WarmOnPrimaryContainer,
    secondary = WarmSecondary,
    onSecondary = WarmOnSecondary,
    secondaryContainer = WarmSecondaryContainer,
    background = WarmBackground,
    onBackground = WarmOnBackground,
    surface = WarmSurface,
    onSurface = WarmOnSurface,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = WarmOnSurfaceVariant,
    error = WarmError,
    onError = WarmOnError,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant
)

@Composable
fun PillReminderTheme(
    darkTheme: Boolean = false,  // Default to Light
    dynamicColor: Boolean = false, // Disable dynamic color
    content: @Composable () -> Unit
) {
    // Always use Light Scheme for this design
    val colorScheme = StitchLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            // In Light Mode, status bar icons should be dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StitchTypography,
        content = content
    )
}