package com.example.gains.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gains.data.MetricEntry
import com.example.gains.data.MetricWithLatestEntry
import com.example.gains.theme.*
import com.example.gains.ui.components.GainsCard
import com.example.gains.ui.components.LogMetricDialog
import com.example.gains.ui.components.SetGoalDialog
import com.example.gains.ui.components.getMetricThemeColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun MetricHistoryScreen(
    metricId: Long,
    viewModel: MainScreenViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.getMetricEntriesFlow(metricId).collectAsStateWithLifecycle(initialValue = emptyList())
    val metrics by viewModel.allMetrics.collectAsStateWithLifecycle(initialValue = emptyList())
    val metric = metrics.find { it.id == metricId }

    var entryToEdit by remember { mutableStateOf<MetricEntry?>(null) }
    var showLogDialog by remember { mutableStateOf(false) }
    var activeTimeFilter by remember { mutableStateOf(ChartTimeFilter.ONE_MONTH) }

    val filterPrimary = MaterialTheme.colorScheme.primary
    val filterSurface = MaterialTheme.colorScheme.surface
    val filterOutline = MaterialTheme.colorScheme.outline
    val filterSecondary = MaterialTheme.colorScheme.secondary
    val metricThemeColor = remember(metric) { metric?.let { getMetricThemeColor(it.name) } ?: InfraredAccent }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top Navigation & App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
                    Text(
                        text = metric?.name?.uppercase(Locale.getDefault()) ?: "METRIC DEEP-DIVE",
                        style = HeaderBold.copy(fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "ANALYTICS & HISTORY",
                        style = LabelCaps.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (metric != null) {
                val isHc = metric.source == "HEALTH_CONNECT"
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isHc) PrimarySoftBg else MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = if (isHc) InfraredAccent.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            val newSource = if (isHc) "MANUAL" else "HEALTH_CONNECT"
                            viewModel.updateMetricSource(metric.id, newSource)
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isHc) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = InfraredAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = if (isHc) "HEALTH CONNECT" else "MANUAL",
                            style = LabelCaps.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = if (isHc) InfraredAccent else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        if (metric == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Metric not found.", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Analytics Chart Card
                item {
                    GainsCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Value Display & Quick Log CTA Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    val currentVal = metric.latestValue
                                    if (currentVal != null) {
                                        val formattedVal = when {
                                            metric.name.lowercase().contains("step") ->
                                                NumberFormat.getIntegerInstance().format(currentVal.toInt())
                                            metric.name.lowercase().contains("sleep") -> {
                                                val totalMins = (currentVal * 60).roundToInt()
                                                val h = totalMins / 60
                                                val m = totalMins % 60
                                                if (m == 0) "${h}h" else "${h}h ${m}m"
                                            }
                                            currentVal % 1f == 0f -> currentVal.toInt().toString()
                                            else -> String.format(Locale.getDefault(), "%.1f", currentVal)
                                        }
                                        val unitSubtext = when {
                                            metric.name.lowercase().contains("sleep") ->
                                                "${String.format(Locale.getDefault(), "%.1f", currentVal)} HRS • LATEST VALUE"
                                            else ->
                                                "${metric.unit.uppercase()} • LATEST VALUE"
                                        }
                                        Text(
                                            text = formattedVal,
                                            style = MetricLarge.copy(fontSize = 32.sp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = unitSubtext,
                                            style = LabelCaps.copy(fontSize = 9.sp),
                                            color = metricThemeColor
                                        )
                                    } else {
                                        Text(
                                            text = "No data logged",
                                            style = HeaderBold.copy(fontSize = 20.sp),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "LOG YOUR FIRST ENTRY BELOW",
                                            style = LabelCaps.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Button(
                                    onClick = { showLogDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = InfraredAccent),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("LOG ENTRY", style = LabelCaps.copy(fontSize = 10.sp), color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Time Filter Bar & Goal Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ChartTimeFilter.values().forEach { filter ->
                                        val isActive = activeTimeFilter == filter
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isActive) InfraredAccent else filterSurface)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isActive) InfraredAccent else filterOutline,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable { activeTimeFilter = filter }
                                                .padding(horizontal = 12.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = filter.label,
                                                style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = if (isActive) Color.White else filterSecondary
                                            )
                                        }
                                    }
                                }

                                if (metric.targetValue != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(InfraredAccent.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "GOAL: ${metric.targetValue} ${metric.unit}",
                                            style = LabelCaps.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = InfraredAccent
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Filter Entries & Render Curved Metric Line Chart
                            val filterDays = activeTimeFilter.days
                            val filteredEntries = if (filterDays != null) {
                                val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(filterDays.toLong())
                                entries.filter { it.timestamp >= cutoffTime }
                            } else {
                                entries
                            }

                            if (filteredEntries.size > 1 || (filteredEntries.size == 1 && metric.targetValue != null)) {
                                MetricLineChart(
                                    entries = filteredEntries.sortedBy { it.timestamp },
                                    targetValue = metric.targetValue,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                            } else if (filteredEntries.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.background),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text("No entries in selected time range.", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.background),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Log more data to reveal your progress curve.", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // History Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "LOGGED HISTORY (${entries.size})",
                                style = LabelCaps.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // History Entries List
                if (entries.isNotEmpty()) {
                    items(entries.sortedByDescending { it.timestamp }) { entry ->
                        GainsCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { entryToEdit = entry }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = SimpleDateFormat("MMM dd, yyyy  •  hh:mm a", Locale.getDefault()).format(Date(entry.timestamp)),
                                        style = BodySemiBold.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (entry.externalId != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = InfraredAccent,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "HEALTH CONNECT SYNCED",
                                                style = LabelCaps.copy(fontSize = 8.sp),
                                                color = InfraredAccent
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val formattedListVal = when {
                                        metric.name.lowercase().contains("step") ->
                                            "${NumberFormat.getIntegerInstance().format(entry.value.toInt())} ${metric.unit}"
                                        metric.name.lowercase().contains("sleep") -> {
                                            val totalMins = (entry.value * 60).roundToInt()
                                            "${totalMins / 60}h ${totalMins % 60}m (${String.format(Locale.getDefault(), "%.1f", entry.value)} hrs)"
                                        }
                                        entry.value % 1f == 0f -> "${entry.value.toInt()} ${metric.unit}"
                                        else -> "${String.format(Locale.getDefault(), "%.1f", entry.value)} ${metric.unit}"
                                    }
                                    Text(
                                        text = formattedListVal,
                                        style = BodySemiBold.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                        color = metricThemeColor
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit entry",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        GainsCard(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No history recorded yet.",
                                        style = BodySemiBold.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Button(
                                        onClick = { showLogDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = InfraredAccent),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("+ LOG FIRST ENTRY", style = LabelCaps, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showLogDialog && metric != null) {
        LogMetricDialog(
            metricName = metric.name,
            metricUnit = metric.unit,
            onDismiss = { showLogDialog = false },
            onSave = { value, timestamp ->
                viewModel.logMetric(metric.id, value, timestamp)
                showLogDialog = false
            }
        )
    }

    if (entryToEdit != null && metric != null) {
        EditMetricEntryDialog(
            entry = entryToEdit!!,
            unit = metric.unit,
            onDismiss = { entryToEdit = null },
            onSave = { updatedEntry ->
                viewModel.updateMetricEntry(updatedEntry)
                entryToEdit = null
            },
            onDelete = {
                viewModel.deleteMetricEntry(entryToEdit!!)
                entryToEdit = null
            }
        )
    }
}

@Composable
fun EditMetricEntryDialog(
    entry: MetricEntry,
    unit: String,
    onDismiss: () -> Unit,
    onSave: (MetricEntry) -> Unit,
    onDelete: () -> Unit
) {
    var valueStr by remember { mutableStateOf(entry.value.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        GainsCard {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                Text(
                    text = "EDIT LOG ENTRY",
                    style = HeaderBold.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy  •  hh:mm a", Locale.getDefault()).format(Date(entry.timestamp)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it },
                    label = { Text("Value ($unit)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InfraredAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDelete) {
                        Text("DELETE", style = LabelCaps, color = MaterialTheme.colorScheme.error)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("CANCEL", style = LabelCaps, color = MaterialTheme.colorScheme.secondary)
                        }
                        Button(
                            onClick = {
                                valueStr.toFloatOrNull()?.let { onSave(entry.copy(value = it)) }
                            },
                            enabled = valueStr.toFloatOrNull() != null,
                            colors = ButtonDefaults.buttonColors(containerColor = InfraredAccent),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text("SAVE", style = LabelCaps, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
