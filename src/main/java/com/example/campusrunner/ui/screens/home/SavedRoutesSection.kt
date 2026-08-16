package com.example.campusrunner.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.campusrunner.data.SavedRoute
import com.example.campusrunner.geo.RouteMath
import com.example.campusrunner.ui.components.RouteVergeIconButton
import com.example.campusrunner.ui.components.RouteVergeIconButtonVariant
import com.example.campusrunner.ui.formatDistance
import com.example.campusrunner.ui.theme.RouteVergeSpacing

/** Saved routes as a light linear list (name / metadata / play / overflow). */
@Composable
fun SavedRoutesSection(
    routes: List<SavedRoute>,
    speedText: String,
    onStartRoute: (SavedRoute, String) -> Unit,
    onEditRoute: (SavedRoute) -> Unit,
    onDeleteRoute: (SavedRoute) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
        HomeSectionTitle("保存路线", count = routes.size)
        if (routes.isEmpty()) {
            EmptyRoutesHint()
        } else {
            routes.forEach { route ->
                RouteListItem(
                    route = route,
                    onPlay = { onStartRoute(route, speedText) },
                    onEdit = { onEditRoute(route) },
                    onDelete = { onDeleteRoute(route) }
                )
            }
        }
    }
}

@Composable
private fun RouteListItem(
    route: SavedRoute,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val distance = RouteMath.totalDistanceMeters(route.points, closeLoop = route.closeLoop)
    val loopText = if (route.closeLoop) "闭环 ${route.loopCount} 圈" else "往返"

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
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
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("取消")
                }
            }
        )
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
