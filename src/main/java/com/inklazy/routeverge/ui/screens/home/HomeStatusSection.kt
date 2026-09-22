package com.inklazy.routeverge.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inklazy.routeverge.ui.components.RouteVergeStatus
import com.inklazy.routeverge.ui.components.StatusBadge
import com.inklazy.routeverge.ui.theme.RouteVergeShapes
import com.inklazy.routeverge.ui.theme.RouteVergeSpacing
import com.inklazy.routeverge.ui.theme.RouteVergeTheme

/**
 * Compact status area — always visible.
 * Shows only user-facing state (simulation state badge) plus actionable
 * blocking hints (permission / mock app / NFC gate). Technical metrics
 * (map provider, route count, ...) are intentionally not shown here.
 */
@Composable
fun HomeStatusSection(
    hasLocationPermission: Boolean,
    canMockLocation: Boolean,
    isServiceRunning: Boolean,
    isServicePaused: Boolean,
    isNfcActivated: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onVerifyNfc: () -> Unit
) {
    // Ready only when every prerequisite for starting a simulation is satisfied
    // (the same conditions the blocking hints below surface).
    val readyForMock = hasLocationPermission && canMockLocation && isNfcActivated
    val runLabel = when {
        isServicePaused -> "模拟暂停"
        isServiceRunning -> "模拟运行中"
        !readyForMock -> "未就绪"
        else -> "待命"
    }
    val runStatus = when {
        isServicePaused -> RouteVergeStatus.Paused
        isServiceRunning -> RouteVergeStatus.Running
        !readyForMock -> RouteVergeStatus.Warning
        else -> RouteVergeStatus.Ready
    }
    val description = when {
        isServicePaused -> "模拟已暂停，可继续或停止"
        isServiceRunning -> "正在模拟位置"
        !readyForMock -> "完成必要设置后即可开始模拟"
        else -> "已就绪，可以开始模拟"
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RouteVergeShapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(RouteVergeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "当前状态",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(description, style = MaterialTheme.typography.bodyMedium)
                }
                StatusBadge(label = runLabel, status = runStatus)
            }

            if (!hasLocationPermission) {
                BlockingHintRow(
                    message = "需要定位权限",
                    actionLabel = "授权",
                    onAction = onRequestPermissions
                )
            } else if (!canMockLocation) {
                BlockingHintRow(
                    message = "需要选择模拟位置应用",
                    actionLabel = "设置",
                    onAction = onOpenDeveloperOptions
                )
            }
            if (!isNfcActivated) {
                BlockingHintRow(
                    message = "设备尚未验证",
                    actionLabel = "验证",
                    onAction = onVerifyNfc
                )
            }
        }
    }
}

@Composable
private fun BlockingHintRow(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            Icons.Rounded.Warning,
            contentDescription = null,
            tint = RouteVergeTheme.statusColors.warning,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(RouteVergeSpacing.sm))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}
