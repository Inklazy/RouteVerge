package com.inklazy.routeverge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.inklazy.routeverge.data.SavedPoint
import com.inklazy.routeverge.data.SavedRoute
import com.inklazy.routeverge.ui.theme.RouteVergeShapes
import com.inklazy.routeverge.ui.theme.RouteVergeSpacing
import com.inklazy.routeverge.ui.theme.RouteVergeMotion
import com.inklazy.routeverge.ui.theme.rememberRouteVergeReducedMotion

/** Stable, type-safe presentation models for the shared saved-record row. */
sealed interface SavedRecord {
    val id: String
    val title: String
    val subtitle: String?
}

data class PointRecord(val point: SavedPoint, override val subtitle: String) : SavedRecord {
    override val id: String get() = point.id
    override val title: String get() = point.name
}

data class RouteRecord(val route: SavedRoute, override val subtitle: String) : SavedRecord {
    override val id: String get() = route.id
    override val title: String get() = route.name
}

/**
 * One fixed-height row for both saved points and routes. The menu is hosted
 * here so its click target is independent from the full-row selection target.
 */
@Composable
fun SavedRecordRow(
    record: SavedRecord,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    menuContent: @Composable (dismiss: () -> Unit) -> Unit
) {
    var menuExpanded by remember(record.id) { mutableStateOf(false) }
    val interactionSource = remember(record.id) { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberRouteVergeReducedMotion()
    val rowColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.background,
        animationSpec = RouteVergeMotion.spec(reducedMotion),
        label = "saved_record_selection"
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = RouteVergePressFeedback.color(rowColor, MaterialTheme.colorScheme.onSurface, pressed),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RouteVergeShapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                // The row's indication must share the same shape as the
                // visual surface instead of painting a rectangular layer.
                .clip(RouteVergeShapes.medium)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.RadioButton,
                    onClick = onSelect
                )
                .semantics { this.selected = selected }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RouteVergeSpacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.Text(
                        text = record.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (record.subtitle != null) {
                        androidx.compose.material3.Text(
                            text = record.subtitle.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    } else {
                        // Reserve the same second line even when a point has no metadata.
                        Spacer(Modifier.height(20.dp))
                    }
                }
                Spacer(Modifier.width(RouteVergeSpacing.sm))
                Box {
                    RouteVergeIconButton(
                        onClick = { menuExpanded = true },
                        variant = RouteVergeIconButtonVariant.Plain,
                        contentDescription = "更多操作"
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        menuContent { menuExpanded = false }
                    }
                }
            }
        }
    }
}
