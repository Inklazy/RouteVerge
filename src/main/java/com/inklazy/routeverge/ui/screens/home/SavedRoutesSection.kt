package com.inklazy.routeverge.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.inklazy.routeverge.data.SavedRoute
import com.inklazy.routeverge.geo.RouteMath
import com.inklazy.routeverge.ui.components.RouteRecord
import com.inklazy.routeverge.ui.components.RouteVergeButton
import com.inklazy.routeverge.ui.components.RouteVergeButtonVariant
import com.inklazy.routeverge.ui.components.SavedRecordRow
import com.inklazy.routeverge.ui.formatDistance
import com.inklazy.routeverge.ui.theme.RouteVergeSpacing

/** Saved routes use the same fixed-height row as saved points. */
@Composable
fun SavedRoutesList(
    modifier: Modifier,
    listState: LazyListState,
    routes: List<SavedRoute>,
    selectedRouteId: String?,
    speedText: String,
    onStartRoute: (SavedRoute, String) -> Unit,
    onEditRoute: (SavedRoute) -> Unit,
    onDeleteRoute: (SavedRoute) -> Unit,
    onSelectRoute: (SavedRoute) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.xs)
    ) {
        if (routes.isEmpty()) {
            item { EmptyRoutesHint() }
        } else {
            items(routes, key = { it.id }) { route ->
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
