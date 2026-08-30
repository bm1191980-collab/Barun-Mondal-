package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Modern Material 3 Dark Color Scheme (Electric Blue & Royal Purple)
private val ModernDarkColorScheme = darkColorScheme(
    primary = BrandBluePrimary,
    onPrimary = BrandBlueOnPrimary,
    primaryContainer = BrandBlueContainerDark,
    onPrimaryContainer = BrandBlueOnContainerDark,
    inversePrimary = BrandBluePrimary,

    secondary = BrandPurpleSecondary,
    onSecondary = BrandPurpleOnSecondary,
    secondaryContainer = BrandPurpleContainerDark,
    onSecondaryContainer = BrandPurpleOnContainerDark,

    tertiary = BrandCyanTertiary,
    onTertiary = BrandCyanOnTertiary,
    tertiaryContainer = BrandCyanContainerDark,
    onTertiaryContainer = BrandCyanOnContainerDark,

    background = DeepMidnightBg,
    onBackground = TextPrimaryDark,
    surface = DeepMidnightSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DeepMidnightSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = BrandBluePrimary,

    surfaceContainerLowest = Color(0xFF070A10),
    surfaceContainerLow = DeepMidnightBg,
    surfaceContainer = DeepMidnightSurface,
    surfaceContainerHigh = DeepMidnightSurfaceVariant,
    surfaceContainerHighest = DeepMidnightSurfaceElevated,

    outline = DeepMidnightBorder,
    outlineVariant = DeepMidnightBorderSubtle,
    error = SatisfyRed,
    onError = Color.White
)

// Modern Material 3 Light Color Scheme (Ice Blue & Royal Purple)
private val ModernLightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = BrandBlueContainerLight,
    onPrimaryContainer = BrandBlueOnContainerLight,
    inversePrimary = BrandBluePrimary,

    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = BrandPurpleContainerLight,
    onSecondaryContainer = BrandPurpleOnContainerLight,

    tertiary = Color(0xFF0891B2),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF164E63),

    background = LightIceBg,
    onBackground = TextPrimaryLight,
    surface = LightIceSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightIceSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceTint = Color(0xFF2563EB),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = LightIceSurface,
    surfaceContainerHigh = LightIceSurfaceVariant,
    surfaceContainerHighest = Color(0xFFE2E8F0),

    outline = LightIceBorder,
    outlineVariant = LightIceBorderSubtle,
    error = SatisfyRed,
    onError = Color.White
)

// Modern Material 3 Shapes
val ModernShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun SatisfyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ModernDarkColorScheme else ModernLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ModernShapes,
        content = content
    )
}
