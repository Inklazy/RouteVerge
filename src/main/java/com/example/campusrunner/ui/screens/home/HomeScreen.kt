package com.example.campusrunner.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.campusrunner.data.SavedRoute
import com.example.campusrunner.data.SpeedPreset
import com.example.campusrunner.ui.RuntimeSession
import com.example.campusrunner.ui.components.RouteVergeIconButton
import com.example.campusrunner.ui.components.RouteVergeIconButtonVariant
import com.example.campusrunner.ui.formatNumber
import com.example.campusrunner.ui.theme.RouteVergeSpacing

/**
 * Home destination — a single vertical flow:
 * status -> simulation (mode + config + primary action) -> saved routes -> tools.
 * No card stacks, no dashboard. Technical state lives in the status detail
 * (Phase 9 Settings), not on the first screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    hasLocationPermission: Boolean,
    canMockLocation: Boolean,
    routes: List<SavedRoute>,
    isServiceRunning: Boolean,
    isServicePaused: Boolean,
    runtimeSession: RuntimeSession?,
    isNfcActivated: Boolean,
    speedText: String,
    pointLatInput: String,
    pointLngInput: String,
    onPointLatChange: (String) -> Unit,
    onPointLngChange: (String) -> Unit,
    onSpeedTextChange: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onVerifyNfc: () -> Unit,
    onOpenAlipayNfc: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPointPicker: () -> Unit,
    onOpenRouteEditor: () -> Unit,
    onEditRoute: (SavedRoute) -> Unit,
    onStartPoint: () -> Unit,
    onStartRoute: (SavedRoute, String) -> Unit,
    onPause: () -> Unit,
    onResumeMock: () -> Unit,
    onStop: () -> Unit,
    onDeleteRoute: (SavedRoute) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("RouteVerge") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    RouteVergeIconButton(
                        onClick = onOpenSettings,
                        variant = RouteVergeIconButtonVariant.Plain,
                        contentDescription = "设置"
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = RouteVergeSpacing.lg),
            contentPadding = PaddingValues(bottom = RouteVergeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(RouteVergeSpacing.lg)
        ) {
            item {
                HomeStatusSection(
                    hasLocationPermission = hasLocationPermission,
                    canMockLocation = canMockLocation,
                    isServiceRunning = isServiceRunning,
                    isServicePaused = isServicePaused,
                    isNfcActivated = isNfcActivated,
                    onRequestPermissions = onRequestPermissions,
                    onOpenDeveloperOptions = onOpenDeveloperOptions,
                    onVerifyNfc = onVerifyNfc
                )
            }
            item {
                SimulationSection(
                    routes = routes,
                    runtimeSession = runtimeSession,
                    speedText = speedText,
                    pointLatInput = pointLatInput,
                    pointLngInput = pointLngInput,
                    isServiceRunning = isServiceRunning,
                    isServicePaused = isServicePaused,
                    onPointLatChange = onPointLatChange,
                    onPointLngChange = onPointLngChange,
                    onSpeedTextChange = onSpeedTextChange,
                    onOpenPointPicker = onOpenPointPicker,
                    onOpenRouteEditor = onOpenRouteEditor,
                    onStartPoint = onStartPoint,
                    onStartRoute = onStartRoute,
                    onPause = onPause,
                    onResumeMock = onResumeMock,
                    onStop = onStop
                )
            }
            item {
                SavedRoutesSection(
                    routes = routes,
                    speedText = speedText,
                    onStartRoute = onStartRoute,
                    onEditRoute = onEditRoute,
                    onDeleteRoute = onDeleteRoute
                )
            }
            item {
                NfcToolsRow(
                    isActivated = isNfcActivated,
                    onOpenAlipay = onOpenAlipayNfc,
                    onVerify = onVerifyNfc
                )
            }
        }
    }
}

/** Lightweight section title used across the home flow (Linear-style). */
@Composable
internal fun HomeSectionTitle(text: String, count: Int? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (count != null) {
            Spacer(Modifier.width(RouteVergeSpacing.sm))
            Text(
                "$count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
