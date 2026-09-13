package com.example.campusrunner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * RouteVerge Design System — Phase 2.
 *
 * One token set, two modes (light / dark), zero hard-coded values in business code.
 *
 * Usage rules:
 *  - Prefer [MaterialTheme.colorScheme] roles (primary / onSurface / surfaceContainer / ...).
 *  - Use [RouteVergeTheme.statusColors] for semantic status colors M3 does not provide
 *    (success / warning / danger).
 *  - Use [RouteVergeTheme.palette] only for tokens without an M3 role
 *    (textDisabled today).
 *  - Use [RouteVergeSpacing] / [RouteVergeShapes] / MaterialTheme typography for all
 *    spacing / radius / text styles.
 */

// ===== Brand constants (platform-level, e.g. native map rendering) =====
// Claude-inspired RouteVerge identity tokens. Map SDK rendering uses these
// only for semantic overlays; the underlying real map remains untouched.
object RouteVergeBrand {
    const val PrimaryHex = "#CC785C"
    const val SuccessHex = "#5DB872"
    const val DangerHex = "#C64545"
}

// ===== Spacing tokens =====
// DESIGN.md §8 — 8dp-oriented scale: 4 / 8 / 12 / 16 / 20 / 24 / 32.
object RouteVergeSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

/** Common sizes for ordinary UI icons. Map and SDK drawing sizes stay local. */
object RouteVergeIconSizes {
    val small = 16.dp
    val standard = 18.dp
    val medium = 20.dp
    val large = 24.dp
}

/** Visual constants for overlays and route strokes rendered on map canvases. */
object RouteVergeMapTokens {
    val crosshairSize = 34.dp
    val crosshairArmLength = 28.dp
    val crosshairStrokeWidth = 3.dp
    val crosshairShadowElevation = 2.dp
    val crosshairForeground = Color(0xFFFFFFFF)
    val crosshairShadow = Color(0x99000000)

    const val routeStrokeWidth = 8f
    const val previewRouteStrokeWidth = 6f
    const val secondaryRouteAlpha = 0xB3
}

// ===== Shape tokens =====
// DESIGN.md §9 — radius vocabulary: 12 / 16 / 22 / 28 / 999(pill).
object RouteVergeShapes {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val extraLarge = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(999.dp)
}

/**
 * Raw color tokens for one mode (DESIGN.md §4.2 / §5, plus derived M3 tonal variants).
 * The Material 3 [ColorScheme] is built FROM this palette so the two can never diverge.
 */
@Immutable
data class RouteVergePalette(
    // Brand / primary
    val primary: Color,
    val primaryPressed: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    // Secondary warm neutrals (NOT status colors)
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    // Tertiary warm neutral, reserved for future accents
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    // Surfaces
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,       // DESIGN.md "Surface Elevated"
    val surfaceContainer: Color,          // DESIGN.md "Surface Container"
    val surfaceContainerHigh: Color,      // floating controls / map overlays
    val surfaceContainerHighest: Color,   // disabled containers
    val surfaceVariant: Color,            // DESIGN.md "Surface Variant" (dark)
    val onSurfaceVariant: Color,          // DESIGN.md "Secondary Text"
    val surfaceBright: Color,
    val surfaceDim: Color,
    // Lines
    val outline: Color,                   // DESIGN.md "Border"
    val outlineVariant: Color,
    // Error (mirrors statusColors.danger)
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    // Inverse
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,
    // No M3 role
    val textDisabled: Color               // DESIGN.md "Disabled Text"
)

val RouteVergeLightPalette = RouteVergePalette(
    primary = Color(0xFFCC785C),
    primaryPressed = Color(0xFFA9583E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8E0D2),
    onPrimaryContainer = Color(0xFF141413),
    secondary = Color(0xFF6C6A64),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFE9DE),
    onSecondaryContainer = Color(0xFF3D3D3A),
    tertiary = Color(0xFF8E8B82),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8E0D2),
    onTertiaryContainer = Color(0xFF3D3D3A),
    background = Color(0xFFFAF9F5),
    onBackground = Color(0xFF141413),
    surface = Color(0xFFFAF9F5),
    onSurface = Color(0xFF141413),
    surfaceContainerLowest = Color(0xFFFAF9F5),
    surfaceContainerLow = Color(0xFFF5F0E8),
    surfaceContainer = Color(0xFFEFE9DE),
    surfaceContainerHigh = Color(0xFFE8E0D2),
    surfaceContainerHighest = Color(0xFFE6DFD8),
    surfaceVariant = Color(0xFFF5F0E8),
    onSurfaceVariant = Color(0xFF6C6A64),
    surfaceBright = Color(0xFFFAF9F5),
    surfaceDim = Color(0xFFE6DFD8),
    outline = Color(0xFFE6DFD8),
    outlineVariant = Color(0xFFEBE6DF),
    error = Color(0xFFC64545),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF5D8D2),
    onErrorContainer = Color(0xFF5A1B18),
    inverseSurface = Color(0xFF181715),
    inverseOnSurface = Color(0xFFFAF9F5),
    inversePrimary = Color(0xFFE6A28A),
    textDisabled = Color(0xFF8E8B82)
)

val RouteVergeDarkPalette = RouteVergePalette(
    primary = Color(0xFFCC785C),
    primaryPressed = Color(0xFFA9583E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF493229),
    onPrimaryContainer = Color(0xFFF5D8CC),
    secondary = Color(0xFFB7B1A8),
    onSecondary = Color(0xFF272521),
    secondaryContainer = Color(0xFF3B3832),
    onSecondaryContainer = Color(0xFFE8E0D2),
    tertiary = Color(0xFF8E8B82),
    onTertiary = Color(0xFF272521),
    tertiaryContainer = Color(0xFF3B3832),
    onTertiaryContainer = Color(0xFFE8E0D2),
    background = Color(0xFF181715),
    onBackground = Color(0xFFFAF9F5),
    surface = Color(0xFF1F1E1B),
    onSurface = Color(0xFFFAF9F5),
    surfaceContainerLowest = Color(0xFF181715),
    surfaceContainerLow = Color(0xFF1F1E1B),
    surfaceContainer = Color(0xFF252320),
    surfaceContainerHigh = Color(0xFF2D2A26),
    surfaceContainerHighest = Color(0xFF3A3630),
    surfaceVariant = Color(0xFF2D2A26),
    onSurfaceVariant = Color(0xFFA09D96),
    surfaceBright = Color(0xFF3A3630),
    surfaceDim = Color(0xFF181715),
    outline = Color(0xFF514B43),
    outlineVariant = Color(0xFF3A3630),
    error = Color(0xFFE38A84),
    onError = Color(0xFF3D0908),
    errorContainer = Color(0xFF64221E),
    onErrorContainer = Color(0xFFF5D8D2),
    inverseSurface = Color(0xFFFAF9F5),
    inverseOnSurface = Color(0xFF181715),
    inversePrimary = Color(0xFF9F5540),
    textDisabled = Color(0xFF8E8B82)
)

/**
 * Semantic status colors (DESIGN.md §6): green = success, orange = warning, red = danger.
 * M3 has no roles for success/warning, so these ride a dedicated CompositionLocal.
 */
@Immutable
data class RouteVergeStatusColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color
)

val RouteVergeLightStatusColors = RouteVergeStatusColors(
    success = Color(0xFF5DB872),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFDDEEDB),
    onSuccessContainer = Color(0xFF1B5E2A),
    warning = Color(0xFFD4A017),
    onWarning = Color(0xFF3D2D00),
    warningContainer = Color(0xFFF4E5B8),
    onWarningContainer = Color(0xFF5A4300),
    danger = Color(0xFFC64545),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFF5D8D2),
    onDangerContainer = Color(0xFF5A1B18)
)

val RouteVergeDarkStatusColors = RouteVergeStatusColors(
    success = Color(0xFF5DB872),
    onSuccess = Color(0xFF00391C),
    successContainer = Color(0xFF1E4A30),
    onSuccessContainer = Color(0xFFA9F2BE),
    warning = Color(0xFFD4A017),
    onWarning = Color(0xFF3F2A00),
    warningContainer = Color(0xFF4A3C16),
    onWarningContainer = Color(0xFFF4E5B8),
    danger = Color(0xFFE38A84),
    onDanger = Color(0xFF690005),
    dangerContainer = Color(0xFF64221E),
    onDangerContainer = Color(0xFFF5D8D2)
)

// ===== Theme access =====
private val LocalRouteVergePalette = staticCompositionLocalOf { RouteVergeLightPalette }
private val LocalRouteVergeStatusColors = staticCompositionLocalOf { RouteVergeLightStatusColors }

/** Theme-aware accessor for tokens that have no Material 3 role. */
object RouteVergeTheme {
    val palette: RouteVergePalette
        @Composable @ReadOnlyComposable get() = LocalRouteVergePalette.current

    val statusColors: RouteVergeStatusColors
        @Composable @ReadOnlyComposable get() = LocalRouteVergeStatusColors.current
}

// ===== Typography =====
// DESIGN.md §7 semantic hierarchy mapped onto Material 3 roles:
//   Screen Title -> titleLarge · Section Title -> titleMedium
//   Primary Body -> bodyLarge  · Secondary Body -> bodyMedium
//   Label -> labelLarge/labelMedium · Caption -> labelSmall
private val RouteVergeTypography: Typography = run {
    val base = Typography()
    base.copy(
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium)
    )
}

// ===== Shapes (MaterialTheme roles) =====
// Material 3 roles map to the RouteVerge non-pill vocabulary only.
// RouteVergeShapes.pill is intentionally NOT mapped to any M3 role —
// it is used explicitly only by pill-shaped components (StatusBadge, chips).
private val RouteVergeShapesValue = Shapes(
    extraSmall = RouteVergeShapes.small,
    small = RouteVergeShapes.medium,
    medium = RouteVergeShapes.large,
    large = RouteVergeShapes.extraLarge,
    extraLarge = RouteVergeShapes.extraLarge
)

// ===== ColorScheme builders (single source of truth: the palettes) =====
private fun routeVergeLightColorScheme(
    p: RouteVergePalette,
    s: RouteVergeStatusColors
) = lightColorScheme(
    primary = p.primary,
    onPrimary = p.onPrimary,
    primaryContainer = p.primaryContainer,
    onPrimaryContainer = p.onPrimaryContainer,
    secondary = p.secondary,
    onSecondary = p.onSecondary,
    secondaryContainer = p.secondaryContainer,
    onSecondaryContainer = p.onSecondaryContainer,
    tertiary = p.tertiary,
    onTertiary = p.onTertiary,
    tertiaryContainer = p.tertiaryContainer,
    onTertiaryContainer = p.onTertiaryContainer,
    background = p.background,
    onBackground = p.onBackground,
    surface = p.surface,
    onSurface = p.onSurface,
    surfaceVariant = p.surfaceVariant,
    onSurfaceVariant = p.onSurfaceVariant,
    surfaceContainerLowest = p.surfaceContainerLowest,
    surfaceContainerLow = p.surfaceContainerLow,
    surfaceContainer = p.surfaceContainer,
    surfaceContainerHigh = p.surfaceContainerHigh,
    surfaceContainerHighest = p.surfaceContainerHighest,
    surfaceBright = p.surfaceBright,
    surfaceDim = p.surfaceDim,
    outline = p.outline,
    outlineVariant = p.outlineVariant,
    error = s.danger,
    onError = s.onDanger,
    errorContainer = s.dangerContainer,
    onErrorContainer = s.onDangerContainer,
    inverseSurface = p.inverseSurface,
    inverseOnSurface = p.inverseOnSurface,
    inversePrimary = p.inversePrimary
)

private fun routeVergeDarkColorScheme(
    p: RouteVergePalette,
    s: RouteVergeStatusColors
) = darkColorScheme(
    primary = p.primary,
    onPrimary = p.onPrimary,
    primaryContainer = p.primaryContainer,
    onPrimaryContainer = p.onPrimaryContainer,
    secondary = p.secondary,
    onSecondary = p.onSecondary,
    secondaryContainer = p.secondaryContainer,
    onSecondaryContainer = p.onSecondaryContainer,
    tertiary = p.tertiary,
    onTertiary = p.onTertiary,
    tertiaryContainer = p.tertiaryContainer,
    onTertiaryContainer = p.onTertiaryContainer,
    background = p.background,
    onBackground = p.onBackground,
    surface = p.surface,
    onSurface = p.onSurface,
    surfaceVariant = p.surfaceVariant,
    onSurfaceVariant = p.onSurfaceVariant,
    surfaceContainerLowest = p.surfaceContainerLowest,
    surfaceContainerLow = p.surfaceContainerLow,
    surfaceContainer = p.surfaceContainer,
    surfaceContainerHigh = p.surfaceContainerHigh,
    surfaceContainerHighest = p.surfaceContainerHighest,
    surfaceBright = p.surfaceBright,
    surfaceDim = p.surfaceDim,
    outline = p.outline,
    outlineVariant = p.outlineVariant,
    error = s.danger,
    onError = s.onDanger,
    errorContainer = s.dangerContainer,
    onErrorContainer = s.onDangerContainer,
    inverseSurface = p.inverseSurface,
    inverseOnSurface = p.inverseOnSurface,
    inversePrimary = p.inversePrimary
)

/**
 * RouteVerge theme entry point.
 *
 * @param darkTheme follows the system setting by default.
 * @param dynamicColor retained for API compatibility; RouteVerge deliberately keeps
 *   its fixed warm palette so system dynamic colors never replace the brand tokens.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun RouteVergeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) RouteVergeDarkPalette else RouteVergeLightPalette
    val statusColors = if (darkTheme) RouteVergeDarkStatusColors else RouteVergeLightStatusColors

    val colorScheme = if (darkTheme) {
        routeVergeDarkColorScheme(palette, statusColors)
    } else {
        routeVergeLightColorScheme(palette, statusColors)
    }

    CompositionLocalProvider(
        LocalRouteVergePalette provides palette,
        LocalRouteVergeStatusColors provides statusColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RouteVergeTypography,
            shapes = RouteVergeShapesValue,
            content = content
        )
    }
}
