package com.example.gains.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.gains.MetricHistory
import com.example.gains.data.MetricWithLatestEntry
import com.example.gains.data.SettingsManager
import com.example.gains.theme.*
import com.example.gains.ui.components.BentoMetricCard
import com.example.gains.ui.components.GainsCard
import com.example.gains.ui.components.GoalsHubCard
import com.example.gains.ui.components.ProfileSummaryCard
import com.example.gains.ui.components.EditProfileDialog
import com.example.gains.ui.components.LogMetricDialog
import com.example.gains.ui.components.NutritionGoalDialog
import com.example.gains.ui.components.SetGoalDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ChartTimeFilter(val label: String, val days: Int?) {
    ONE_MONTH("1M", 30),
    THREE_MONTHS("3M", 90),
    ONE_YEAR("1Y", 365),
    ALL("ALL", null)
}

@Composable
fun YouTabContent(
    viewModel: MainScreenViewModel,
    settingsManager: SettingsManager,
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.allMetrics.collectAsStateWithLifecycle(initialValue = emptyList())
    val hiddenMetricNames by settingsManager.hiddenMetricNames.collectAsStateWithLifecycle(initialValue = emptySet())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessions = remember(uiState) { (uiState as? MainScreenUiState.Success)?.sessions ?: emptyList() }

    val profile = (uiState as? MainScreenUiState.Success)?.userProfile
    val currentName = profile?.name ?: "Wouter"
    val currentPhotoUri = profile?.photoUri
    val currentHeight = profile?.height
    val currentAge = profile?.age
    val currentWeight = profile?.currentWeight

    val targets by viewModel.nutritionTargets.collectAsState()
    val weeklyCalibration by viewModel.weeklyCalibrationState.collectAsState()
    var showNutritionGoalDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    var showGoalDialog by remember { mutableStateOf<MetricWithLatestEntry?>(null) }
    var metricToLog by remember { mutableStateOf<MetricWithLatestEntry?>(null) }

    // Filter visible metrics based on user preferences
    val visibleMetrics = remember(metrics, hiddenMetricNames) {
        metrics.filter { !hiddenMetricNames.contains(it.name) }
    }

    // Calculate workouts logged this week
    val workoutsThisWeek = remember(sessions) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfWeek = cal.timeInMillis
        sessions.count { it.timestamp >= startOfWeek }
    }

    val weightMetric = remember(metrics) { metrics.find { it.name.lowercase().contains("weight") } }

    // Time of day greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Night session"
        }
    }

    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Section 1: Page Title Header with Settings Icon
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "You",
                        style = HeaderBold.copy(fontSize = 24.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Health & Body Analytics",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(
                    onClick = { onItemClick(com.example.gains.AppSettings) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Section 2: Profile Summary Card
        item {
            ProfileSummaryCard(
                name = currentName,
                photoUri = currentPhotoUri,
                age = currentAge,
                heightCm = currentHeight,
                currentWeight = currentWeight ?: weightMetric?.latestValue?.toDouble(),
                weightUnit = weightMetric?.unit ?: "kg",
                biologicalSex = profile?.biologicalSex,
                onEditProfileClick = { showEditProfileDialog = true }
            )
        }

        // Section 4: Bento Grid Dashboard Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(InfraredAccent)
                    )
                    Text(
                        text = "METRICS",
                        style = LabelCaps.copy(fontSize = 11.sp, letterSpacing = 1.5.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Section 5: Bento Grid Metric Cards
        if (visibleMetrics.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val pairs = visibleMetrics.chunked(2)
                    pairs.forEach { rowMetrics ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isOrphanRow = rowMetrics.size == 1
                            rowMetrics.forEach { metric ->
                                BentoMetricCard(
                                    metric = metric,
                                    onClick = { onItemClick(MetricHistory(metric.id)) },
                                    modifier = if (isOrphanRow) Modifier.fillMaxWidth() else Modifier.weight(1f)
                                )
                            }
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
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "All metric cards are currently hidden.",
                                style = BodySemiBold.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "You can toggle metric cards back on in Settings.",
                                style = LabelCaps.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // Section 6: My Goals Hub at the Bottom
        item {
            GoalsHubCard(
                gymWorkoutsThisWeek = workoutsThisWeek,
                gymWorkoutsTarget = 4,
                weightCurrent = weightMetric?.latestValue,
                weightTarget = weightMetric?.targetValue,
                weightUnit = weightMetric?.unit ?: "kg",
                nutritionTargets = targets,
                onSetGoalClick = {
                    if (weightMetric != null) {
                        showGoalDialog = weightMetric
                    }
                },
                onNutritionGoalClick = {
                    showNutritionGoalDialog = true
                }
            )
        }

        // Smart Weekly Calibration Card
        weeklyCalibration?.let { calibration ->
            item {
                GainsCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = InfraredAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "WEEKLY NUTRITION CALIBRATION",
                                style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = InfraredAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = calibration.message,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.dismissCalibration() }
                            ) {
                                Text(
                                    text = "DISMISS",
                                    style = LabelCaps.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.applyCalibrationAdjustment(calibration.recommendedCalorieAdjustment) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = InfraredAccent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                val sign = if (calibration.recommendedCalorieAdjustment > 0) "+" else ""
                                Text(
                                    text = "APPLY $sign${calibration.recommendedCalorieAdjustment} KCAL",
                                    style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (metricToLog != null) {
        LogMetricDialog(
            metricName = metricToLog!!.name,
            metricUnit = metricToLog!!.unit,
            onDismiss = { metricToLog = null },
            onSave = { value, timestamp ->
                viewModel.logMetric(metricToLog!!.id, value, timestamp)
                metricToLog = null
            }
        )
    }

    if (showGoalDialog != null) {
        SetGoalDialog(
            metricName = showGoalDialog!!.name,
            metricUnit = showGoalDialog!!.unit,
            currentGoal = showGoalDialog!!.targetValue,
            onDismiss = { showGoalDialog = null },
            onSave = { targetValue ->
                viewModel.updateMetricGoal(showGoalDialog!!.id, targetValue, null)
                showGoalDialog = null
            }
        )
    }

    if (showNutritionGoalDialog) {
        NutritionGoalDialog(
            userProfile = profile,
            onSavePlan = { activity, mode, alloc, split, preset, goal, rate, date, cCal, cProt, cCarb, cFat, cFib ->
                viewModel.updateNutritionPlan(
                    activity, mode, alloc, split, preset, goal, rate, date, cCal, cProt, cCarb, cFat, cFib
                )
            },
            onDismiss = { showNutritionGoalDialog = false }
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
private fun QuickStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subtext: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    GainsCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = label,
                    style = LabelCaps.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = value,
                    style = HeaderBold.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MetricLineChart(
    entries: List<com.example.gains.data.MetricEntry>,
    targetValue: Float?,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val targetColor = InfraredAccent
    val labelColor = MaterialTheme.colorScheme.secondary
    val textMeasurer = rememberTextMeasurer()
    val sdf = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (entries.isEmpty() && targetValue == null) return@Canvas

        val allValues = entries.map { it.value }.toMutableList()
        if (targetValue != null) allValues.add(targetValue)

        val maxVal = allValues.maxOrNull() ?: 100f
        val minVal = allValues.minOrNull() ?: 0f

        val valRange = if (maxVal == minVal) 1f else maxVal - minVal
        val chartMax = maxVal + (valRange * 0.12f)
        val chartMin = (minVal - (valRange * 0.12f)).coerceAtLeast(0f)
        val chartRange = if (chartMax == chartMin) 1f else chartMax - chartMin

        val paddingTop = 16.dp.toPx()
        val paddingBottom = 28.dp.toPx()
        val paddingRight = 12.dp.toPx()
        val paddingLeft = 36.dp.toPx()

        val width = size.width - paddingLeft - paddingRight
        val height = size.height - paddingTop - paddingBottom

        val xStep = if (entries.size > 1) width / (entries.size - 1).toFloat() else width

        // Horizontal Grid Lines & Y-Axis Scale Text
        for (i in 0..4) {
            val ratio = i / 4f
            val y = paddingTop + (height * (1f - ratio))
            val gridVal = chartMin + (chartRange * ratio)

            val textStr = if (gridVal % 1f == 0f) gridVal.toInt().toString() else String.format(Locale.getDefault(), "%.1f", gridVal)

            val textResult = textMeasurer.measure(
                text = textStr,
                style = LabelCaps.copy(fontSize = 9.sp, color = labelColor)
            )
            val textW = textResult.size.width.toFloat()
            val textH = textResult.size.height.toFloat()

            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    x = paddingLeft - textW - 8.dp.toPx(),
                    y = y - textH / 2f
                )
            )

            // Grid Line
            drawLine(
                color = outlineColor.copy(alpha = 0.25f),
                start = Offset(paddingLeft, y),
                end = Offset(size.width - paddingRight, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        // Dashed Goal Target Line
        if (targetValue != null) {
            val normalizedTargetY = 1f - ((targetValue - chartMin) / chartRange)
            val targetY = paddingTop + (normalizedTargetY * height)
            drawLine(
                color = targetColor,
                start = Offset(paddingLeft, targetY),
                end = Offset(size.width - paddingRight, targetY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
            )

            // Target Tag Box
            val goalStr = "GOAL ${targetValue}"
            val goalTextResult = textMeasurer.measure(
                text = goalStr,
                style = LabelCaps.copy(fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
            )
            val tagW = goalTextResult.size.width.toFloat() + 10.dp.toPx()
            val tagH = goalTextResult.size.height.toFloat() + 4.dp.toPx()
            val tagX = size.width - paddingRight - tagW

            drawRoundRect(
                color = targetColor,
                topLeft = Offset(tagX, targetY - tagH / 2f),
                size = androidx.compose.ui.geometry.Size(tagW, tagH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawText(
                textLayoutResult = goalTextResult,
                topLeft = Offset(tagX + 5.dp.toPx(), targetY - goalTextResult.size.height.toFloat() / 2f)
            )
        }

        if (entries.isEmpty()) return@Canvas

        val strokePath = Path()
        val fillPath = Path()
        val points = mutableListOf<Offset>()

        entries.forEachIndexed { index, entry ->
            val normalizedY = 1f - ((entry.value - chartMin) / chartRange)
            val x = paddingLeft + (index * xStep)
            val y = paddingTop + (normalizedY * height)
            points.add(Offset(x, y))
        }

        if (points.isNotEmpty()) {
            strokePath.moveTo(points.first().x, points.first().y)
            fillPath.moveTo(points.first().x, paddingTop + height)
            fillPath.lineTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                val control1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                val control2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)

                strokePath.cubicTo(control1.x, control1.y, control2.x, control2.y, p2.x, p2.y)
                fillPath.cubicTo(control1.x, control1.y, control2.x, control2.y, p2.x, p2.y)
            }

            fillPath.lineTo(points.last().x, paddingTop + height)
            fillPath.close()

            // Area Gradient Fill under chart curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent),
                    startY = paddingTop,
                    endY = paddingTop + height
                )
            )

            // Smooth Curve Stroke
            drawPath(
                path = strokePath,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Draw Interactive Data Points & X-Axis Labels
        points.forEachIndexed { index, point ->
            val entry = entries[index]

            // Outer Translucent Ring Glow
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f),
                radius = 6.dp.toPx(),
                center = point
            )
            // Core Dot
            drawCircle(
                color = primaryColor,
                radius = 4.dp.toPx(),
                center = point
            )
            // Inner Highlight
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = point
            )

            // X-Axis Timestamp Labels
            val shouldDrawXLabel = index == 0 || index == entries.size - 1 || (entries.size > 4 && index == entries.size / 2)
            if (shouldDrawXLabel) {
                val dateStr = sdf.format(Date(entry.timestamp))
                val textResult = textMeasurer.measure(
                    text = dateStr,
                    style = LabelCaps.copy(fontSize = 9.sp, color = labelColor)
                )
                val textW = textResult.size.width.toFloat()

                var textX = point.x - (textW / 2f)
                if (index == 0) textX = paddingLeft
                if (index == entries.size - 1) textX = size.width - paddingRight - textW

                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(
                        x = textX,
                        y = size.height - paddingBottom + 8.dp.toPx()
                    )
                )
            }
        }
    }
}
