package com.inklazy.routeverge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.inklazy.routeverge.ui.theme.RouteVergeShapes

/**
 * RouteVerge card — flat, shadow-free surface grouping (DESIGN.md §11/§12).
 *
 * No padding is forced: the caller decides content layout.
 *
 * Variants:
 *  - [RouteVergeCardVariant.Default]  surfaceContainerLow + subtle border (cards on background)
 *  - [RouteVergeCardVariant.Elevated] surfaceContainerHigh + subtle border (floating / map context)
 *  - [RouteVergeCardVariant.Outlined] surface + outline border
 */
enum class RouteVergeCardVariant { Default, Elevated, Outlined }

@Composable
fun RouteVergeCard(
    modifier: Modifier = Modifier,
    variant: RouteVergeCardVariant = RouteVergeCardVariant.Default,
    containerColor: Color? = null,
    content: @Composable () -> Unit
) {
    val resolvedContainer = containerColor ?: when (variant) {
        RouteVergeCardVariant.Default -> MaterialTheme.colorScheme.surfaceContainerLow
        RouteVergeCardVariant.Elevated -> MaterialTheme.colorScheme.surfaceContainerHigh
        RouteVergeCardVariant.Outlined -> MaterialTheme.colorScheme.surface
    }
    val border = when (variant) {
        RouteVergeCardVariant.Outlined -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Card(
        modifier = modifier,
        shape = RouteVergeShapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = resolvedContainer),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}
