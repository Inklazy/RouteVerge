package com.example.campusrunner.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campusrunner.nfc.NfcLauncherController
import com.example.campusrunner.ui.theme.RouteVergeSpacing

@Composable
fun UpdateCheckingDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("正在检查更新") },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RouteVergeSpacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                Text("正在确认 GitHub 最新版本，请稍候。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {}
    )
}

@Composable
fun UpdateCheckFailedDialog(
    onRetry: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("检查更新失败") },
        text = {
            Text(
                text = "暂时无法检查更新，请稍后重试。\n\n你可以继续使用当前版本，或重新检查。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text("重新检查")
            }
        },
        dismissButton = {
            TextButton(onClick = onContinue) {
                Text("继续使用")
            }
        }
    )
}

@Composable
fun ForceUpdateDialog(
    latestVersion: String,
    message: String,
    onOpenRelease: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("发现新版本") },
        text = {
            Text(
                text = buildString {
                    append("当前版本不是最新版，请更新到 v")
                    append(latestVersion)
                    append(" 后继续使用。")
                    if (message.isNotBlank()) {
                        append("\n\n")
                        append(message)
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(onClick = onOpenRelease) {
                Text("前往 GitHub")
            }
        },
        dismissButton = {
            TextButton(onClick = onRetry) {
                Text("我已更新，重新检查")
            }
        }
    )
}

@Composable
fun StartupAgreementDialog(
    onAccept: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("软件使用协议与免责声明") },
        text = {
            val scrollState = rememberScrollState()
            Text(
                text = NfcLauncherController.SOFTWARE_AGREEMENT,
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
            )
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("确认并进入")
            }
        },
        dismissButton = {
            TextButton(onClick = onExit) {
                Text("退出")
            }
        }
    )
}
