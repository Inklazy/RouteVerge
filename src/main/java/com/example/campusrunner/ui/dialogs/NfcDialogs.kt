package com.example.campusrunner.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.campusrunner.R
import com.example.campusrunner.nfc.NfcLauncherController
import com.example.campusrunner.ui.components.RouteVergeButton
import com.example.campusrunner.ui.components.RouteVergeButtonVariant
import com.example.campusrunner.ui.components.RouteVergeTextField
import com.example.campusrunner.ui.theme.RouteVergeSpacing

/**
 * Device activation dialog (non-cancelable, like the legacy dialog).
 * The user must scroll the agreement to the bottom before the checkbox is
 * enabled; the action buttons stay disabled until the agreement is checked.
 */
@Composable
fun NfcActivationDialog(
    deviceId: String,
    isSubmitting: Boolean,
    onActivate: (String) -> Unit,
    onTelegram: () -> Unit
) {
    val context = LocalContext.current
    var termsMeasured by remember { mutableStateOf(false) }
    var agreementRead by remember { mutableStateOf(false) }
    var agreementChecked by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState, termsMeasured) {
        if (!termsMeasured) return@LaunchedEffect
        withFrameNanos { }
        if (scrollState.maxValue == 0) {
            // Content fits without scrolling — agreement is considered read.
            agreementRead = true
            return@LaunchedEffect
        }
        snapshotFlow { scrollState.value }
            .collect { value ->
                if (value >= scrollState.maxValue) {
                    agreementRead = true
                }
            }
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.nfc_dialog_activation_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)) {
                Text(
                    stringResource(R.string.nfc_dialog_activation_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .verticalScroll(scrollState)
                        .onGloballyPositioned { termsMeasured = true },
                    verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)
                ) {
                    Text(
                        NfcLauncherController.SOFTWARE_AGREEMENT,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = agreementChecked,
                        onCheckedChange = { agreementChecked = it },
                        enabled = agreementRead
                    )
                    Text(
                        if (agreementRead) stringResource(R.string.nfc_dialog_agreement_accepted)
                        else stringResource(R.string.nfc_dialog_agreement_pending),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                SelectionContainer {
                    Text(
                        stringResource(R.string.nfc_dialog_device_id, deviceId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RouteVergeTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = stringResource(R.string.nfc_dialog_code_hint),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrect = false
                    )
                )
            }
        },
        confirmButton = {
            RouteVergeButton(
                onClick = {
                    if (code.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.nfc_dialog_code_empty), Toast.LENGTH_SHORT).show()
                    } else {
                        onActivate(code)
                    }
                },
                enabled = agreementChecked && !isSubmitting,
                loading = isSubmitting
            ) {
                Text(if (isSubmitting) "正在激活..." else stringResource(R.string.nfc_dialog_activate))
            }
        },
        dismissButton = {
            RouteVergeButton(
                onClick = onTelegram,
                variant = RouteVergeButtonVariant.Outlined
            ) {
                Text(stringResource(R.string.nfc_dialog_telegram))
            }
        }
    )
}

/** Dialog shown when a new Alipay NFC link is discovered on a tag. */
@Composable
fun NfcLinkDialog(
    url: String,
    onSave: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nfc_link_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
                Text(url, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onCopy) {
                    Text(stringResource(R.string.nfc_link_dialog_copy))
                }
            }
        },
        confirmButton = {
            RouteVergeButton(onClick = onSave) {
                Text(stringResource(R.string.nfc_link_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.nfc_link_dialog_cancel))
            }
        }
    )
}

/** Dialog showing the currently configured NFC link with a copy action. */
@Composable
fun CurrentNfcLinkDialog(
    url: String,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.current_nfc_link_dialog_title)) },
        text = { Text(url, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            RouteVergeButton(onClick = onCopy) {
                Text(stringResource(R.string.nfc_link_dialog_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.current_nfc_link_dialog_close))
            }
        }
    )
}
