package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// MODERN BLUE & PURPLE MATERIAL 3 COLOR TOKENS
// Inspired by the Satisfy 3D Blue-Yellow-Violet Logo
// ==========================================

// --- DARK THEME SURFACES & BACKGROUNDS ---
val DeepMidnightBg = Color(0xFF090D16)          // Deepest obsidian navy canvas
val DeepMidnightSurface = Color(0xFF0F1523)     // Primary surface container
val DeepMidnightSurfaceVariant = Color(0xFF161F33) // Card & elevated container
val DeepMidnightSurfaceElevated = Color(0xFF1E2A45) // Floating / high modal surface
val DeepMidnightBorder = Color(0xFF2B3A5E)      // Crisp subtle boundary line
val DeepMidnightBorderSubtle = Color(0xFF1E2842) // Very faint divider

// Text / Content on Dark
val TextPrimaryDark = Color(0xFFF8FAFC)        // Crisp high-contrast white
val TextSecondaryDark = Color(0xFFCBD5E1)      // Cool slate secondary
val TextMutedDark = Color(0xFF818FA8)          // Muted metadata / timestamps

// --- LIGHT THEME SURFACES & BACKGROUNDS ---
val LightIceBg = Color(0xFFF4F7FC)             // Cool clean ice-white background
val LightIceSurface = Color(0xFFFFFFFF)        // Pure white card surface
val LightIceSurfaceVariant = Color(0xFFE8EEF8) // Soft periwinkle card surface
val LightIceBorder = Color(0xFFCBD5E1)         // Clean light outline
val LightIceBorderSubtle = Color(0xFFE2E8F0)   // Faint separator

// Text / Content on Light
val TextPrimaryLight = Color(0xFF0F172A)       // Deep slate primary text
val TextSecondaryLight = Color(0xFF475569)     // Slate secondary text
val TextMutedLight = Color(0xFF64748B)         // Subtle caption text

// --- PRIMARY (ELECTRIC BLUE / SAPPHIRE) ---
val BrandBluePrimary = Color(0xFF3B82F6)        // Vibrant Electric Royal Blue
val BrandBlueOnPrimary = Color(0xFFFFFFFF)
val BrandBlueContainerDark = Color(0xFF1E3A8A)  // Deep glowing blue container
val BrandBlueOnContainerDark = Color(0xFFBFDBFE)
val BrandBlueContainerLight = Color(0xFFDBEAFE)
val BrandBlueOnContainerLight = Color(0xFF1E40AF)

// --- SECONDARY (ROYAL PURPLE / NEON VIOLET) ---
val BrandPurpleSecondary = Color(0xFF8B5CF6)    // Modern Royal Violet / Amethyst
val BrandPurpleOnSecondary = Color(0xFFFFFFFF)
val BrandPurpleContainerDark = Color(0xFF4C1D95) // Deep violet container
val BrandPurpleOnContainerDark = Color(0xFFEDE9FE)
val BrandPurpleContainerLight = Color(0xFFEDE9FE)
val BrandPurpleOnContainerLight = Color(0xFF5B21B6)

// --- TERTIARY (ELECTRIC CYAN & GOLD SWIRL) ---
val BrandCyanTertiary = Color(0xFF06B6D4)       // Radiant Cyan
val BrandCyanOnTertiary = Color(0xFFFFFFFF)
val BrandCyanContainerDark = Color(0xFF164E63)
val BrandCyanOnContainerDark = Color(0xFFCFFAFE)

// --- BRAND ACCENT COLORS ---
val SatisfyBlue = Color(0xFF3B82F6)             // Electric Blue
val SatisfyElectricBlue = Color(0xFF3B82F6)     // Electric Blue
val SatisfyBlueLight = Color(0xFF60A5FA)        // Sky Blue
val SatisfyCyan = Color(0xFF00E5FF)             // Electric Cyan (Logo Swirl)
val SatisfyPurple = Color(0xFF8B5CF6)           // Royal Violet
val SatisfyPurpleDark = Color(0xFF6366F1)       // Indigo Accent
val SatisfyPurpleLight = Color(0xFFA78BFA)      // Soft Lavender
val SatisfyGold = Color(0xFFFFB300)             // Golden Amber (Logo Swirl & Pro)
val SatisfyGoldDark = Color(0xFFD97706)         // Rich Amber
val SatisfyGoldLight = Color(0xFFFFD54F)        // Radiant Gold
val SatisfyGreen = Color(0xFF10B981)            // Emerald Success
val SatisfyRed = Color(0xFFEF4444)              // Alert / Danger Red (Smooth Modern)
val SatisfyRedLight = Color(0xFFF87171)
val SatisfyRedDark = Color(0xFFB91C1C)
val SatisfyNeonRed = Color(0xFFFF2A55)

// Aliases mapped to modern theme tokens for backwards compatibility
val ImmersiveDarkBg = DeepMidnightBg
val ImmersiveDarkSurface = DeepMidnightSurface
val ImmersiveDarkSurfaceVariant = DeepMidnightSurfaceVariant
val ImmersiveDarkSurfaceContainerHigh = DeepMidnightSurfaceElevated
val ImmersiveDarkBorder = DeepMidnightBorder
val ImmersiveDarkBorderSubtle = DeepMidnightBorderSubtle
val ImmersiveTextPrimaryDark = TextPrimaryDark
val ImmersiveTextSecondaryDark = TextSecondaryDark
val ImmersiveTextMutedDark = TextMutedDark

val ImmersivePrimary = BrandBluePrimary
val ImmersiveOnPrimary = BrandBlueOnPrimary
val ImmersivePrimaryContainer = BrandBlueContainerDark
val ImmersiveOnPrimaryContainer = BrandBlueOnContainerDark
val ImmersiveSecondary = BrandPurpleSecondary
val ImmersiveOnSecondary = BrandPurpleOnSecondary
val ImmersiveSecondaryContainer = BrandPurpleContainerDark
val ImmersiveOnSecondaryContainer = BrandPurpleOnContainerDark
val ImmersiveTertiary = BrandCyanTertiary
val ImmersiveOnTertiary = BrandCyanOnTertiary

val SatisfyDarkBg = DeepMidnightBg
val SatisfyDarkSurface = DeepMidnightSurface
val SatisfyDarkSurfaceVariant = DeepMidnightSurfaceVariant
val SatisfyDarkSurfaceElevated = DeepMidnightSurfaceElevated

val ImmersiveLightBg = LightIceBg
val ImmersiveLightSurface = LightIceSurface
val ImmersiveLightSurfaceVariant = LightIceSurfaceVariant
val ImmersiveLightBorder = LightIceBorder
val ImmersiveTextPrimaryLight = TextPrimaryLight
val ImmersiveTextSecondaryLight = TextSecondaryLight
val ImmersiveTextMutedLight = TextMutedLight

// --- BRAND GRADIENTS ---
val BrandHeroGradient = Brush.linearGradient(
    listOf(Color(0xFF00E5FF), Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF6366F1))
)
val BrandBluePurpleGradient = Brush.linearGradient(
    listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
)
val BrandPurpleGlowGradient = Brush.linearGradient(
    listOf(Color(0xFF8B5CF6), Color(0xFF4F46E5))
)
val BrandGoldAmberGradient = Brush.linearGradient(
    listOf(Color(0xFFFFB300), Color(0xFFF59E0B), Color(0xFFD97706))
)
val BrandSurfaceGlowGradient = Brush.verticalGradient(
    listOf(Color(0xFF1E2A45), Color(0xFF0F1523))
)
