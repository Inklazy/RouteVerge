package com.example.campusrunner.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.campusrunner.data.SavedRoute
import com.example.campusrunner.geo.RouteMath
import com.example.campusrunner.ui.components.RouteRecord
import com.example.campusrunner.ui.components.RouteVergeButton
import com.example.campusrunner.ui.components.RouteVergeButtonVariant
import com.example.campusrunner.ui.components.SavedRecordRow
import com.example.campusrunner.ui.formatDistance
import com.example.campusrunner.ui.theme.RouteVergeSpacing

/** Saved routes use the same fixed-height row as saved points. */
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
    Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.xs)) {
        HomeSectionTitle("保存路线", count = routes.size)
        if (routes.isEmpty()) {
            EmptyRoutesHint()
        } else {
            routes.forEach { route ->
                key(route.id) {
                    RouteListItem(
                        route = route,
                        selected = route.id == selectedRouteId,
                        onStart = { onStartRoute(route, speedText) },
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
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    var confirmDelete by remember(route.id) { mutableStateOf(false) }
    val distance = RouteMath.totalDistanceMeters(route.points, closeLoop = route.closeLoop)
    val loopText = if (route.closeLoop) "闭环 ${route.loopCount} 圈" else "往返"
    SavedRecordRow(
        record = RouteRecord(route, "${formatDistance(distance)} · ${route.points.size} 个点 · $loopText"),
        selected = selected,
        onSelect = onSelect,
        menuContent = { dismiss ->
            DropdownMenuItem(text = { Text("开始路线") }, onClick = { dismiss(); onSelect(); onStart() })
            DropdownMenuItem(text = { Text("编辑") }, onClick = { dismiss(); onEdit() })
            DropdownMenuItem(text = { Text("删除") }, onClick = { dismiss(); confirmDelete = true })
        }
    )
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除路线") },
            text = { Text("确认删除“${route.name}”？") },
            confirmButton = {
                RouteVergeButton(
                    onClick = { confirmDelete = false; onDelete() },
                    variant = RouteVergeButtonVariant.DestructiveText
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = RouteVergeSpacing.lg)
    )
}
