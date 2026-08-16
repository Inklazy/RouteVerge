package com.example.campusrunner.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campusrunner.BuildConfig
import com.example.campusrunner.R
import com.example.campusrunner.ui.components.RouteVergeIconButton
import com.example.campusrunner.ui.components.RouteVergeIconButtonVariant
import com.example.campusrunner.ui.components.RouteVergeStatus
import com.example.campusrunner.ui.components.StatusDot
import com.example.campusrunner.ui.theme.RouteVergeShapes
import com.example.campusrunner.ui.theme.RouteVergeSpacing

/**
 * Minimal settings surface (Phase 9B): NFC (device verification + current
 * link) and App (about / version / license / project home). Rows reuse the
 * existing NFC flows and dialogs — no second activation/link UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isNfcActivated: Boolean,
    nfcLinkConfigured: Boolean,
    onBack: () -> Unit,
    onVerifyNfc: () -> Unit,
    onShowCurrentLink: () -> Unit,
    onOpenProjectHome: () -> Unit
) {
    val context = LocalContext.current
    val licenseText = remember {
        runCatching {
            context.resources.openRawResource(R.raw.license).bufferedReader().use { it.readText() }
        }.getOrNull()
    }
    var showAbout by remember { mutableStateOf(false) }
    var showLicense by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    RouteVergeIconButton(
                        onClick = onBack,
                        variant = RouteVergeIconButtonVariant.Plain,
                        contentDescription = stringResource(R.string.settings_back)
                    ) {
                        androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = RouteVergeSpacing.lg),
            contentPadding = PaddingValues(bottom = RouteVergeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.md)
        ) {
            item {
                SectionHeader(stringResource(R.string.settings_section_nfc))
            }
            item {
                SettingGroup {
                    SettingRow(
                        title = stringResource(R.string.settings_device_verification),
                        subtitle = stringResource(
                            if (isNfcActivated) R.string.settings_verified else R.string.settings_not_verified
                        ),
                        status = if (isNfcActivated) RouteVergeStatus.Ready else RouteVergeStatus.Warning,
                        // Verified rows are plain info (no affordance); only
                        // unverified devices open the activation dialog.
                        onClick = if (isNfcActivated) null else onVerifyNfc
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        title = stringResource(R.string.settings_current_nfc_link),
                        subtitle = stringResource(
                            if (nfcLinkConfigured) R.string.settings_nfc_link_configured else R.string.settings_nfc_link_not_configured
                        ),
                        onClick = if (nfcLinkConfigured) onShowCurrentLink else null
                    )
                }
            }
            item {
                SectionHeader(stringResource(R.string.settings_section_app))
            }
            item {
                SettingGroup {
                    SettingRow(
                        title = stringResource(R.string.settings_about),
                        onClick = { showAbout = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        title = stringResource(R.string.settings_version),
                        trailing = {
                            Text(
                                BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        title = stringResource(R.string.settings_license),
                        onClick = { showLicense = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        title = stringResource(R.string.settings_project_home),
                        onClick = onOpenProjectHome
                    )
                }
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.settings_about)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.sm)) {
                    Text(stringResource(R.string.settings_about_description))
                    Text(
                        stringResource(R.string.settings_version) + " " + BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("关闭") }
            }
        )
    }

    if (showLicense) {
        AlertDialog(
            onDismissRequest = { showLicense = false },
            title = { Text(stringResource(R.string.settings_license)) },
            text = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RouteVergeShapes.medium
                ) {
                    Text(
                        licenseText ?: "GPL-3.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(RouteVergeSpacing.md)
                            .verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicense = false }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = RouteVergeSpacing.xs, vertical = RouteVergeSpacing.xs)
    )
}

/** M3 grouped-list container: subtle surface separation without per-row cards. */
@Composable
private fun SettingGroup(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RouteVergeShapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    status: RouteVergeStatus? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val baseModifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 48.dp)
        .padding(horizontal = RouteVergeSpacing.lg, vertical = RouteVergeSpacing.md)
    val clickableModifier = if (onClick != null) {
        baseModifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        baseModifier
    }
    Row(clickableModifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (status != null) {
            StatusDot(status = status)
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            androidx.compose.material3.Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
