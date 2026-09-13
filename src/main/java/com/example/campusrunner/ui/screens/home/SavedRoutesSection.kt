package com.example.campusrunner.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.campusrunner.data.SavedRoute
import com.example.campusrunner.geo.RouteMath
import com.example.campusrunner.ui.components.RouteVergeButton
import com.example.campusrunner.ui.components.RouteVergeButtonVariant
import com.example.campusrunner.ui.components.RouteVergeIconButton
import com.example.campusrunner.ui.components.RouteVergeIconButtonVariant
import com.example.campusrunner.ui.formatDistance
import com.example.campusrunner.ui.theme.RouteVergeSpacing
import com.example.campusrunner.ui.theme.RouteVergeShapes
import com.example.campusrunner.ui.theme.RouteVergeMotion
import com.example.campusrunner.ui.theme.rememberRouteVergeReducedMotion

/** Saved routes as a light linear list (name / metadata / play / overflow). */
@Composable
fun SavedRoutesSection(
    routes: List<SavedRoute>,
    selectedRouteId: String?,
    speedText: String,
    onStartRoute: (SavedRoute, String) -> Unit,
    onEditRoute: (SavedRoute) -> Unit,
    onDeleteRoute: (SavedRoute) -> Unit,
    onSelectRoute: (SavedRoute) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
        HomeSectionTitle("保存路线", count = routes.size)
        if (routes.isEmpty()) {
            EmptyRoutesHint()
        } else {
            routes.forEach { route ->
                key(route.id) {
                    RouteListItem(
                        route = route,
                        selected = route.id == selectedRouteId,
                        onPlay = { onStartRoute(route, speedText) },
                        onEdit = { onEditRoute(route) },
                        onDelete = { onDeleteRoute(route) },
                        onSelect = { onSelectRoute(route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteListItem(
    route: SavedRoute,
    selected: Boolean,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
    ,onSelect: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(true) }
    val reducedMotion = rememberRouteVergeReducedMotion()
    val rowColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = RouteVergeMotion.spec(reducedMotion),
        label = "route_selection_color"
    )
    val distance = RouteMath.totalDistanceMeters(route.points, closeLoop = route.closeLoop)
    val loopText = if (route.closeLoop) "闭环 ${route.loopCount} 圈" else "往返"

    LaunchedEffect(visible) {
        if (!visible) {
            kotlinx.coroutines.delay(RouteVergeMotion.listDuration.toLong())
            onDelete()
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(RouteVergeMotion.listDuration)),
        exit = fadeOut(tween(RouteVergeMotion.listDuration)) + shrinkVertically(tween(RouteVergeMotion.listDuration))
    ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .background(rowColor, RouteVergeShapes.medium)
            .clickable(onClick = onSelect)
            .padding(horizontal = RouteVergeSpacing.sm)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                route.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${formatDistance(distance)} · ${route.points.size} 个点 · $loopText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        RouteVergeIconButton(
            onClick = onPlay,
            variant = RouteVergeIconButtonVariant.Plain,
            contentDescription = "播放路线",
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
        }
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
                DropdownMenuItem(
                    text = { Text("编辑") },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = {
                        menuExpanded = false
                        confirmDelete = true
                    }
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除路线") },
            text = { Text("确认删除“${route.name}”？") },
            confirmButton = {
                RouteVergeButton(
                    onClick = {
                    confirmDelete = false
                    visible = false
                    },
                    variant = RouteVergeButtonVariant.DestructiveText
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("取消")
                }
            }
        )
    }
    }
}

@Composable
private fun EmptyRoutesHint() {
    Text(
        "还没有保存路线",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = RouteVergeSpacing.lg)
    )
}
