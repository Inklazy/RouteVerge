package com.inklazy.routeverge.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.inklazy.routeverge.ui.components.RouteVergeStatus
import com.inklazy.routeverge.ui.components.StatusDot
import com.inklazy.routeverge.ui.theme.RouteVergeShapes
import com.inklazy.routeverge.ui.theme.RouteVergeSpacing
import com.inklazy.routeverge.ui.theme.RouteVergeIconSizes
import com.inklazy.routeverge.ui.theme.RouteVergeTheme

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
    Column(
        // This is deliberately the canvas token, not a card. It remains opaque
        // so scrolled records can never show through the fixed bottom bar.
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = RouteVergeSpacing.lg, vertical = RouteVergeSpacing.sm),
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
