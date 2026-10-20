package com.goaltv.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GoalTvDarkColorScheme = darkColorScheme(
    primary = OrangePrimary,
    onPrimary = TextPrimary,
    primaryContainer = SurfaceDarkVariant,
    onPrimaryContainer = OrangeLight,
    secondary = OrangeSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = SurfaceDarkCard,
    onSecondaryContainer = TextSecondary,
    tertiary = GoldAccent,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDarkCard,
    onSurfaceVariant = TextSecondary,
    error = StatusError,
    onError = TextPrimary
)

@Composable
fun GoaltvTheme(
    darkTheme: Boolean = true, // Force dark GOALTV theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = GoalTvDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.let { ctx ->
                var current = ctx
                while (current is ContextWrapper) {
                    if (current is Activity) return@let current
                    current = current.baseContext
                }
                null
            }
            activity?.window?.let { window ->
                window.statusBarColor = DarkBackground.toArgb()
                window.navigationBarColor = DarkBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}