package com.example.campusrunner.ui.screens.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.campusrunner.ui.components.RouteVergeButton
import com.example.campusrunner.ui.components.RouteVergeButtonVariant
import com.example.campusrunner.ui.components.RouteVergeCard
import com.example.campusrunner.ui.components.RouteVergeCardVariant
import com.example.campusrunner.ui.components.RouteVergeIconButton
import com.example.campusrunner.ui.components.RouteVergeIconButtonVariant
import com.example.campusrunner.ui.theme.RouteVergeSpacing

/**
 * Mode-driven compact bottom action surface for the route editor.
 * Only the actions relevant to the current task are exposed:
 *  - empty idle: start drawing
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

    RouteVergeCard(
        variant = RouteVergeCardVariant.Elevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(RouteVergeSpacing.md),
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
                    Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
                        RouteVergeButton(
                            onClick = onCancelTemplate,
                            variant = RouteVergeButtonVariant.Outlined,
                            modifier = Modifier.weight(1f),
                            text = { Text("取消模板") }
                        )
                        RouteVergeButton(
                            onClick = onApplyTemplate,
                            modifier = Modifier.weight(1f),
                            text = { Text("应用模板") }
                        )
                    }
                }

                drawMode -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "绘制中 · " + pointCount + " 点 · " + distanceText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
                        RouteVergeButton(
                            onClick = onUndo,
                            enabled = pointCount > 0,
                            variant = RouteVergeButtonVariant.Outlined,
                            modifier = Modifier.weight(1f),
                            text = { Text("撤销") }
                        )
                        RouteVergeButton(
                            onClick = onDoneDrawing,
                            modifier = Modifier.weight(1f),
                            text = { Text("完成绘制") }
                        )
                    }
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
                    RouteVergeButton(
                        onClick = onStartDrawing,
                        modifier = Modifier.fillMaxWidth(),
                        text = { Text("开始绘制") }
                    )
                }

                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            pointCount.toString() + " 点 · " + distanceText + " · " + loopSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
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
                    Row(horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
                        RouteVergeButton(
                            onClick = onContinueDrawing,
                            variant = RouteVergeButtonVariant.Tonal,
                            modifier = Modifier.weight(1f),
                            text = { Text("继续绘制") }
                        )
                        RouteVergeButton(
                            onClick = onUndo,
                            enabled = pointCount > 0,
                            variant = RouteVergeButtonVariant.Outlined,
                            modifier = Modifier.weight(1f),
                            text = { Text("撤销") }
                        )
                        RouteVergeButton(
                            onClick = onSave,
                            enabled = canSave,
                            modifier = Modifier.weight(1f),
                            text = { Text("保存路线") }
                        )
                    }
                }
            }
        }
    }
}
