package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    primaryContainer = ImmersivePrimaryContainer,
    onPrimaryContainer = ImmersiveOnPrimaryContainer,
    secondary = ImmersiveSecondary,
    onSecondary = ImmersiveOnSecondary,
    secondaryContainer = ImmersiveSecondaryContainer,
    onSecondaryContainer = ImmersiveOnSecondaryContainer,
    tertiary = ImmersiveTertiary,
    onTertiary = ImmersiveOnTertiary,
    background = ImmersiveDarkBg,
    onBackground = ImmersiveTextPrimaryDark,
    surface = ImmersiveDarkSurface,
    onSurface = ImmersiveTextPrimaryDark,
    surfaceVariant = ImmersiveDarkSurfaceVariant,
    onSurfaceVariant = ImmersiveTextSecondaryDark,
    outline = ImmersiveDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = ImmersivePrimaryContainer,
    onPrimary = Color.White,
    primaryContainer = ImmersivePrimary,
    onPrimaryContainer = ImmersiveOnPrimaryContainer,
    secondary = ImmersiveSecondaryContainer,
    onSecondary = Color.White,
    secondaryContainer = ImmersiveLightSurfaceVariant,
    onSecondaryContainer = ImmersiveTextPrimaryLight,
    tertiary = ImmersiveTertiary,
    background = ImmersiveLightBg,
    onBackground = ImmersiveTextPrimaryLight,
    surface = ImmersiveLightSurface,
    onSurface = ImmersiveTextPrimaryLight,
    surfaceVariant = ImmersiveLightSurfaceVariant,
    onSurfaceVariant = ImmersiveTextSecondaryLight,
    outline = ImmersiveLightBorder
)

@Composable
fun SatisfyTheme(
    darkTheme: Boolean = true, // Default to sleek YouTube-style dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
