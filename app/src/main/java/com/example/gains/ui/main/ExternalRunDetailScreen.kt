package com.example.gains.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gains.data.ExternalActivity
import com.example.gains.theme.*
import com.example.gains.ui.components.GainsCard
import com.example.gains.ui.main.tabs.RouteMapThumbnail
import com.example.gains.ui.main.tabs.getShortSourceBadge
import com.example.gains.ui.main.tabs.parseRouteJson
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalRunDetailScreen(
    activityId: Long,
    viewModel: MainScreenViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val externalActivities by viewModel.allExternalActivities.collectAsStateWithLifecycle(initialValue = emptyList())
    val activity = remember(externalActivities, activityId) {
        externalActivities.find { it.id == activityId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RUN DETAILS",
                        style = LabelCaps.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (activity != null) {
                        val isNikeRunClub = activity.sourceApp?.lowercase()?.let { it.contains("nike") || it.contains("plusgps") } == true
                        val badgeText = getShortSourceBadge(activity.sourceApp)
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isNikeRunClub) Color(0xFF1E2405) else MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = if (isNikeRunClub) Color(0xFFC1F807) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        if (activity == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val date = remember(activity.startTime) { Date(activity.startTime) }
            val dayOfWeek = remember(activity.startTime) { SimpleDateFormat("EEEE", Locale.getDefault()).format(date) }
            val dateStr = remember(activity.startTime) { SimpleDateFormat("MMM dd, yyyy  •  hh:mm a", Locale.getDefault()).format(date) }

            val isNikeRunClub = remember(activity.sourceApp) {
                val app = activity.sourceApp?.lowercase() ?: ""
                app.contains("nike") || app.contains("plusgps")
            }

            val durationFormatted = remember(activity.durationSeconds) {
                val m = activity.durationSeconds / 60
                val s = activity.durationSeconds % 60
                if (s > 0) "${m}m ${s}s" else "${m}m"
            }

            val distanceKmNum = remember(activity.distanceMeters) {
                activity.distanceMeters?.let { String.format(Locale.getDefault(), "%.2f", it / 1000.0) } ?: "--"
            }

            val paceStr = remember(activity.distanceMeters, activity.durationSeconds) {
                if (activity.distanceMeters != null && activity.distanceMeters > 0 && activity.durationSeconds > 0) {
                    val km = activity.distanceMeters / 1000.0
                    val totalSecPerKm = (activity.durationSeconds / km).toInt()
                    val pMin = totalSecPerKm / 60
                    val pSec = totalSecPerKm % 60
                    String.format(Locale.getDefault(), "%d'%02d\" /km", pMin, pSec)
                } else "--"
            }

            val speedStr = remember(activity.distanceMeters, activity.durationSeconds) {
                if (activity.distanceMeters != null && activity.distanceMeters > 0 && activity.durationSeconds > 0) {
                    val km = activity.distanceMeters / 1000.0
                    val hours = activity.durationSeconds / 3600.0
                    val kmh = km / hours
                    String.format(Locale.getDefault(), "%.1f km/h", kmh)
                } else "--"
            }

            val caloriesStr = remember(activity.caloriesKcal) {
                activity.caloriesKcal?.let { "${it.toInt()} kcal" } ?: "--"
            }

            val routePoints = remember(activity.routeJson) { parseRouteJson(activity.routeJson) }
            val brandAccentColor = if (isNikeRunClub) Color(0xFFC1F807) else MaterialTheme.colorScheme.primary

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Banner
                Column {
                    Text(
                        text = activity.title,
                        style = HeaderBold.copy(fontSize = 24.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$dayOfWeek • $dateStr",
                        style = BodySemiBold.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Hero Distance Section (Nike Run Club Typography Style)
                GainsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "DISTANCE",
                            style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = distanceKmNum,
                                style = HeaderBold.copy(fontSize = 52.sp, fontWeight = FontWeight.Black),
                                color = if (isNikeRunClub) Color(0xFFC1F807) else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KM",
                                style = HeaderBold.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }

                // High-Res GPS Route Map Container
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GPS ROUTE TRACK",
                            style = LabelCaps,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (routePoints.size >= 2) {
                            Text(
                                text = "${routePoints.size} GPS Points",
                                style = LabelCaps.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (routePoints.size >= 2) {
                        RouteMapThumbnail(
                            points = routePoints,
                            accentColor = brandAccentColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    } else {
                        GainsCard(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No GPS route coordinates recorded for this run.",
                                    style = BodySemiBold.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                // 2x2 Performance Grid Cards (Nike Style)
                Text(
                    text = "RUN PERFORMANCE METRICS",
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GainsCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("AVG PACE", style = LabelCaps.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(paceStr, style = HeaderBold.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    GainsCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("DURATION", style = LabelCaps.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(durationFormatted, style = HeaderBold.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GainsCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("AVG SPEED", style = LabelCaps.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(speedStr, style = HeaderBold.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    GainsCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("CALORIES BURNED", style = LabelCaps.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(caloriesStr, style = HeaderBold.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Data Origin & Integration Footer Card
                GainsCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Source Application",
                                style = BodySemiBold.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activity.sourceApp ?: "Health Connect",
                                style = LabelCaps.copy(fontSize = 10.sp),
                                color = if (isNikeRunClub) Color(0xFFC1F807) else MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Synced via Health Connect",
                            style = LabelCaps.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
