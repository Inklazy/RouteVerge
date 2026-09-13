package com.example.campusrunner.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
// DESIGN.md §4.1 — the RouteVerge identity blue stays fixed in both modes.
object RouteVergeBrand {
    const val PrimaryHex = "#3482FF"
    const val SuccessHex = "#36D167"
    const val DangerHex = "#FF4D4F"
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
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    // Secondary (calm blue-gray, NOT a status color)
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    // Tertiary (muted slate, reserved for future accents)
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
    primary = Color(0xFF0066CC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5F0FF),
    onPrimaryContainer = Color(0xFF004B99),
    secondary = Color(0xFF5A6B87),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE4F0),
    onSecondaryContainer = Color(0xFF2C3B52),
    tertiary = Color(0xFF64748B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDAE2F0),
    onTertiaryContainer = Color(0xFF2E3A4A),
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF1D1D1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1D1F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDFEFF),
    surfaceContainer = Color(0xFFF5F5F7),
    surfaceContainerHigh = Color(0xFFFAFAFC),
    surfaceContainerHighest = Color(0xFFEAEAEC),
    surfaceVariant = Color(0xFFF5F5F7),
    onSurfaceVariant = Color(0xFF6E6E73),
    surfaceBright = Color(0xFFFDFEFF),
    surfaceDim = Color(0xFFE2E6EE),
    outline = Color(0xFFDDE3EE),
    outlineVariant = Color(0xFFE4E9F2),
    error = Color(0xFFFF4D4F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2E3644),
    inverseOnSurface = Color(0xFFF2F5FA),
    inversePrimary = Color(0xFF9DC3FF),
    textDisabled = Color(0xFF98A2B3)
)

val RouteVergeDarkPalette = RouteVergePalette(
    primary = Color(0xFF2997FF),
    onPrimary = Color(0xFF001B33),
    primaryContainer = Color(0xFF123B61),
    onPrimaryContainer = Color(0xFFD5E8FF),
    secondary = Color(0xFFADBFD8),
    onSecondary = Color(0xFF223041),
    secondaryContainer = Color(0xFF33445C),
    onSecondaryContainer = Color(0xFFD4E4F9),
    tertiary = Color(0xFFA9B8CE),
    onTertiary = Color(0xFF273344),
    tertiaryContainer = Color(0xFF3A4A60),
    onTertiaryContainer = Color(0xFFDAE3F1),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF5F5F7),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF242426),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF38383A),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2),
    surfaceBright = Color(0xFF38383A),
    surfaceDim = Color(0xFF000000),
    outline = Color(0xFF48484A),
    outlineVariant = Color(0xFF38383A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE4E8F0),
    inverseOnSurface = Color(0xFF1D222B),
    inversePrimary = Color(0xFF0066CC),
    textDisabled = Color(0xFF6C7480)
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
    success = Color(0xFF36D167),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD9F5E3),
    onSuccessContainer = Color(0xFF0B6B33),
    warning = Color(0xFFFFA726),
    onWarning = Color(0xFF4A2C00),
    warningContainer = Color(0xFFFFE9CC),
    onWarningContainer = Color(0xFF6E4300),
    danger = Color(0xFFFF4D4F),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFFFDAD6),
    onDangerContainer = Color(0xFF410002)
)

val RouteVergeDarkStatusColors = RouteVergeStatusColors(
    success = Color(0xFF77DD96),
    onSuccess = Color(0xFF00391C),
    successContainer = Color(0xFF1E4A30),
    onSuccessContainer = Color(0xFFA9F2BE),
    warning = Color(0xFFFFB74D),
    onWarning = Color(0xFF3F2A00),
    warningContainer = Color(0xFF4A3200),
    onWarningContainer = Color(0xFFFFDDB2),
    danger = Color(0xFFFFB4AB),
    onDanger = Color(0xFF690005),
    dangerContainer = Color(0xFF93000A),
    onDangerContainer = Color(0xFFFFDAD6)
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
 * @param dynamicColor disabled by default — RouteVerge keeps its fixed brand identity
 *   (decision from the Phase 2 kick-off). The parameter stays for future expansion
 *   (e.g. an optional "dynamic color" setting).
 */
@Composable
fun RouteVergeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val palette = if (darkTheme) RouteVergeDarkPalette else RouteVergeLightPalette
    val statusColors = if (darkTheme) RouteVergeDarkStatusColors else RouteVergeLightStatusColors

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)

        darkTheme -> routeVergeDarkColorScheme(palette, statusColors)
        else -> routeVergeLightColorScheme(palette, statusColors)
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
