package com.example.campusrunner.ui.screens.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.campusrunner.ui.components.RouteVergeButton
import com.example.campusrunner.ui.components.RouteVergeButtonVariant
import com.example.campusrunner.ui.components.RouteVergeIconButton
import com.example.campusrunner.ui.components.RouteVergeIconButtonVariant
import com.example.campusrunner.ui.theme.RouteVergeSpacing
import com.example.campusrunner.ui.theme.RouteVergeShapes

/**
 * Mode-driven compact bottom action surface for the route editor.
 * Only the actions relevant to the current task are exposed:
 *  - empty idle: choose a route creation method (free drawing or track template)
 *  - drawing: undo / finish drawing
 *  - route exists: continue drawing / undo / save + overflow (loop, template, clear)
 *  - template: cancel / apply template
 */
@Composable
fun RouteEditorBottomSurface(
    pointCount: Int,
    distanceText: String,
    drawMode: Boolean,
    templateEnabled: Boolean,
    closeLoop: Boolean,
    loopCountText: String,
    canSave: Boolean,
    onStartDrawing: () -> Unit,
    onDoneDrawing: () -> Unit,
    onContinueDrawing: () -> Unit,
    onUndo: () -> Unit,
    onSave: () -> Unit,
    onOpenLoop: () -> Unit,
    onOpenTemplate: () -> Unit,
    onClear: () -> Unit,
    onCancelTemplate: () -> Unit,
    onApplyTemplate: () -> Unit
) {
    var overflowExpanded by remember { mutableStateOf(false) }
    val loopSummary = if (closeLoop) "闭环 " + (loopCountText.toIntOrNull()?.coerceAtLeast(1) ?: 1) + " 圈" else "往返"

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = com.example.campusrunner.ui.theme.RouteVergeShapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        BoxWithConstraints {
            val stackActions = maxWidth < 270.dp || LocalDensity.current.fontScale >= 1.5f
            Column(
                modifier = Modifier.padding(RouteVergeSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)
            ) {
            when {
                templateEnabled -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "跑道模板预览",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AdaptiveActionPair(
                        stacked = stackActions,
                        first = { actionModifier -> RouteVergeButton(
                            onClick = onCancelTemplate,
                            variant = RouteVergeButtonVariant.Outlined,
                            modifier = actionModifier,
                            shape = RouteVergeShapes.large,
                            text = { Text("取消模板") }
                        ) },
                        second = { actionModifier -> RouteVergeButton(
                            onClick = onApplyTemplate,
                            modifier = actionModifier,
                            shape = RouteVergeShapes.large,
                            text = { Text("应用模板") }
                        ) }
                    )
                }

                drawMode -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "绘制中 · " + pointCount + " 点 · " + distanceText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    AdaptiveActionPair(
                        stacked = stackActions,
                        first = { actionModifier -> RouteVergeButton(
                            onClick = onUndo,
                            enabled = pointCount > 0,
                            variant = RouteVergeButtonVariant.Outlined,
                            modifier = actionModifier,
                            shape = RouteVergeShapes.large,
                            text = { Text("撤销") }
                        ) },
                        second = { actionModifier -> RouteVergeButton(
                            onClick = onDoneDrawing,
                            modifier = actionModifier,
                            shape = RouteVergeShapes.large,
                            text = { Text("完成绘制") }
                        ) }
                    )
                }

                pointCount == 0 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "0 点 · " + distanceText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AdaptiveActionPair(
                        stacked = stackActions,
                        first = { actionModifier -> RouteVergeButton(
                            onClick = onStartDrawing,
                            variant = RouteVergeButtonVariant.Tonal,
                            modifier = actionModifier,
                            shape = RouteVergeShapes.large,
                            text = { Text("绘制线条") }
                        ) },
                        second = { actionModifier -> RouteVergeButton(
                            onClick = onOpenTemplate,
                            modifier = actionModifier,
                            shape = RouteVergeShapes.large,
                            text = { Text("添加跑道") }
                        ) }
                    )
                }

                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            pointCount.toString() + " 点 · " + distanceText + " · " + loopSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        RouteVergeIconButton(
                            onClick = onUndo,
                            enabled = pointCount > 0,
                            variant = if (pointCount > 0) RouteVergeIconButtonVariant.Floating else RouteVergeIconButtonVariant.Plain,
                            contentDescription = "撤销"
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null)
                        }
                        Box {
                            RouteVergeIconButton(
                                onClick = { overflowExpanded = true },
                                variant = RouteVergeIconButtonVariant.Plain,
                                contentDescription = "更多路线选项"
                            ) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = overflowExpanded,
                                onDismissRequest = { overflowExpanded = false }
                            ) {
                                // Loop settings only make sense for a real route (>= 2 points).
                                if (pointCount >= 2) {
                                    DropdownMenuItem(
                                        text = { Text("闭环设置") },
                                        onClick = {
                                            overflowExpanded = false
                                            onOpenLoop()
                                        }
                                    )
                                }
                                // Template stays available for any non-empty draft.
                                DropdownMenuItem(
                                    text = { Text("跑道模板") },
                                    onClick = {
                                        overflowExpanded = false
                                        onOpenTemplate()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("清空路线", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        overflowExpanded = false
                                        onClear()
                                    }
                                )
                            }
                        }
                    }
                    AdaptiveActionPair(
                        stacked = stackActions,
                        first = { actionModifier -> RouteVergeButton(
                            onClick = onContinueDrawing,
                            variant = RouteVergeButtonVariant.Tonal,
                            modifier = actionModifier,
                            shape = RouteVergeShapes.large,
                            text = { Text("继续绘制") }
                        ) },
                        second = { actionModifier -> RouteVergeButton(
                            onClick = onSave,
                            enabled = canSave,
                            modifier = actionModifier,
                            shape = RouteVergeShapes.large,
                            text = { Text("保存路线") }
                        ) }
                    )
                }
            }
        }
    }
}

}

@Composable
private fun AdaptiveActionPair(
    stacked: Boolean,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    }
}
