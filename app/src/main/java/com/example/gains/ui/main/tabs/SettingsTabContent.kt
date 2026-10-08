package com.example.gains.ui.main.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gains.data.SettingsManager
import com.example.gains.data.health.HealthConnectManager
import com.example.gains.theme.BodySemiBold
import com.example.gains.theme.HeaderBold
import com.example.gains.theme.LabelCaps
import com.example.gains.ui.components.CreateLabelDialog
import com.example.gains.ui.components.EditProfileDialog
import com.example.gains.ui.components.GainsCard
import com.example.gains.ui.main.MainScreenUiState
import com.example.gains.ui.main.MainScreenViewModel
import com.example.gains.ui.main.SyncState

@Composable
fun SettingsTabContent(
    viewModel: MainScreenViewModel,
    settingsManager: SettingsManager,
    modifier: Modifier = Modifier
) {
    val themeMode by settingsManager.themeMode.collectAsStateWithLifecycle()
    var showCreateLabelDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    val labels by viewModel.allLabels.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val profile = (uiState as? MainScreenUiState.Success)?.userProfile

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = HeaderBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Configure app preferences",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Health Connect Section
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val hcAvailable = remember { HealthConnectManager.isAvailable(context) }
        var hcGranted by remember { mutableStateOf(false) }
        val hcSyncState by viewModel.hcSyncState.collectAsStateWithLifecycle()

        val hcPermissionLauncher = rememberLauncherForActivityResult(
            androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
        ) { _ ->
            scope.launch {
                hcGranted = HealthConnectManager.hasPermissions(context)
                viewModel.syncHealthConnect(context)
            }
        }

        LaunchedEffect(Unit) {
            if (hcAvailable) {
                hcGranted = HealthConnectManager.hasPermissions(context)
                viewModel.syncHealthConnect(context)
            }
        }

        // Workout Preferences
        val autoLoadPrevious by settingsManager.autoLoadPreviousPerformance.collectAsStateWithLifecycle(initialValue = true)

        Text(
            text = "WORKOUT LOGGER PREFERENCES",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GainsCard(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Load Previous Performance",
                            style = BodySemiBold.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Auto-fill previous sets, weight & reps when adding exercises or starting routines",
                            style = LabelCaps.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = autoLoadPrevious,
                        onCheckedChange = { checked ->
                            settingsManager.setAutoLoadPreviousPerformance(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }

        // Dashboard Customization Section
        val hiddenMetricNames by settingsManager.hiddenMetricNames.collectAsStateWithLifecycle(initialValue = emptySet())
        val metrics by viewModel.allMetrics.collectAsStateWithLifecycle(initialValue = emptyList())

        Text(
            text = "DASHBOARD CUSTOMIZATION",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GainsCard(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Visible Metric Cards",
                    style = BodySemiBold.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose which health metrics appear on your 'You' tab dashboard",
                    style = LabelCaps.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (metrics.isEmpty()) {
                    Text("No metrics configured.", style = BodySemiBold.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.secondary)
                } else {
                    metrics.forEachIndexed { idx, metric ->
                        val isVisible = !hiddenMetricNames.contains(metric.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = metric.name,
                                style = BodySemiBold.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = isVisible,
                                onCheckedChange = { checked ->
                                    settingsManager.toggleMetricVisibility(metric.name, checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                        if (idx < metrics.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        Text(
            text = "HEALTH CONNECT",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GainsCard(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sync Health & Workouts",
                            style = BodySemiBold.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val statusText = when {
                            !hcAvailable -> "Not Available on Device"
                            hcGranted -> "Connected & Synced"
                            else -> "Tap to Connect Health Connect"
                        }
                        Text(
                            text = statusText,
                            style = LabelCaps.copy(fontSize = 11.sp),
                            color = if (hcGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (hcAvailable) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!hcGranted) {
                                Button(
                                    onClick = {
                                        hcPermissionLauncher.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "CONNECT",
                                        style = LabelCaps.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (!hcGranted) {
                                        hcPermissionLauncher.launch(HealthConnectManager.REQUIRED_PERMISSIONS)
                                    }
                                    viewModel.syncHealthConnect(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (hcSyncState is SyncState.Syncing) "SYNCING..." else "SYNC NOW",
                                    style = LabelCaps.copy(fontSize = 10.sp),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Health Connect Data Sources Customization
        val disabledSourceApps by settingsManager.disabledSourceApps.collectAsStateWithLifecycle(initialValue = emptySet())
        val externalActivities by viewModel.allExternalActivities.collectAsStateWithLifecycle(initialValue = emptyList())
        val detectedSources = remember(externalActivities) {
            externalActivities.mapNotNull { it.sourceApp?.ifBlank { null } }.toSet().sorted()
        }

        if (detectedSources.isNotEmpty()) {
            Text(
                text = "HEALTH CONNECT DATA SOURCES",
                style = LabelCaps,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            GainsCard(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enabled Activity Sources",
                        style = BodySemiBold.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Toggle which apps display workouts on your history timeline",
                        style = LabelCaps.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    detectedSources.forEachIndexed { idx, sourceName ->
                        val isEnabled = !disabledSourceApps.contains(sourceName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sourceName,
                                style = BodySemiBold.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    settingsManager.toggleSourceAppVisibility(sourceName, checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                        if (idx < detectedSources.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        // Profile Section
        Text(
            text = "PROFILE",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        GainsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEditProfileDialog = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val currentName = profile?.name ?: "Wouter"
                        Text(
                            text = currentName.firstOrNull()?.toString() ?: "U",
                            style = BodySemiBold.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Name",
                            style = BodySemiBold.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = profile?.name ?: "Wouter",
                            style = BodySemiBold.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                IconButton(onClick = { showEditProfileDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Display Preferences
        Text(
            text = "DISPLAY PREFERENCES",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        GainsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "App Theme",
                    style = BodySemiBold.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose light, dark, or follow system default",
                    style = BodySemiBold.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SYSTEM", "LIGHT", "DARK").forEach { mode ->
                        val isSelected = themeMode == mode
                        Button(
                            onClick = { settingsManager.setThemeMode(mode) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = mode,
                                style = LabelCaps.copy(fontSize = 9.sp),
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Custom Labels Manager
        Text(
            text = "WORKOUT LABELS",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        GainsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Workout Labels",
                        style = BodySemiBold.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { showCreateLabelDialog = true }) {
                        Text("+ CREATE LABEL", style = LabelCaps, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                if (labels.isEmpty()) {
                    Text(
                        text = "No custom labels created yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        labels.forEach { label ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(label.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = label.name,
                                        style = BodySemiBold.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteLabel(label) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Label",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Account & Sync Placeholders
        Text(
            text = "ACCOUNT & SYNC (COMING SOON)",
            style = LabelCaps,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        GainsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsPlaceholderRow(
                    title = "Measurement Units",
                    value = "Metric (kg, km)"
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsPlaceholderRow(
                    title = "Google Sheets Sync",
                    value = "Published Sheet Link"
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsPlaceholderRow(
                    title = "Local Database Export",
                    value = "Backup data (.json)"
                )
            }
        }
    }

    if (showCreateLabelDialog) {
        CreateLabelDialog(
            onDismiss = { showCreateLabelDialog = false },
            onCreateClick = { labelName, colorHex ->
                viewModel.createLabel(labelName, colorHex)
                showCreateLabelDialog = false
            }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditProfileDialog = false },
            onSaveClick = { newName, photo, h, a, w, dob, sex ->
                viewModel.saveProfile(newName, photo, h, a, w, dob, sex)
                showEditProfileDialog = false
            }
        )
    }
}

@Composable
fun SettingsPlaceholderRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = BodySemiBold.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = LabelCaps.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
