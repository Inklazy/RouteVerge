package com.example.campusrunner.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.campusrunner.ui.components.RouteVergeStatus
import com.example.campusrunner.ui.components.StatusDot
import com.example.campusrunner.ui.theme.RouteVergeShapes
import com.example.campusrunner.ui.theme.RouteVergeSpacing
import com.example.campusrunner.ui.theme.RouteVergeIconSizes
import com.example.campusrunner.ui.theme.RouteVergeTheme

/**
 * Advanced tools — one compact NFC row. Activated: tapping performs the
 * Alipay jump; not activated: tapping starts the activation flow. Link
 * management stays out of Home (advanced feature, returns in Settings).
 */
@Composable
fun NfcToolsRow(
    isActivated: Boolean,
    onOpenAlipay: () -> Unit,
    onVerify: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RouteVergeShapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = RouteVergeSpacing.lg, vertical = RouteVergeSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.xs)
        ) {
            HomeSectionTitle("工具")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RouteVergeShapes.medium)
                    .clickable { if (isActivated) onOpenAlipay() else onVerify() }
                    .padding(vertical = RouteVergeSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Nfc,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(RouteVergeIconSizes.medium)
                )
                Spacer(Modifier.width(RouteVergeSpacing.md))
                Column(Modifier.weight(1f)) {
                    Text("支付宝 NFC 跳转", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (isActivated) "已验证" else "设备未验证",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActivated) MaterialTheme.colorScheme.onSurfaceVariant else RouteVergeTheme.statusColors.warning
                    )
                }
                StatusDot(status = if (isActivated) RouteVergeStatus.Ready else RouteVergeStatus.Warning)
                Spacer(Modifier.width(RouteVergeSpacing.sm))
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
