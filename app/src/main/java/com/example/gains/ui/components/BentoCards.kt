package com.example.gains.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gains.data.MetricWithLatestEntry
import com.example.gains.theme.*
import java.util.Locale

// Dynamic Color Palette per Health Metric Category
fun getMetricThemeColor(metricName: String): Color {
    val lower = metricName.lowercase()
    return when {
        lower.contains("weight") -> Color(0xFF0EA5E9)      // Electric Cyan / Teal
        lower.contains("heart") || lower.contains("pulse") || lower.contains("bpm") -> InfraredAccent // High-energy Infrared
        lower.contains("step") || lower.contains("walk") || lower.contains("run") -> Color(0xFF10B981) // Emerald Green
        lower.contains("fat") || lower.contains("cal") || lower.contains("burn") -> Color(0xFFF59E0B) // Amber Orange
        lower.contains("sleep") || lower.contains("rest") -> Color(0xFF8B5CF6) // Royal Violet
        else -> InfraredAccent
    }
}

fun isTrendDesirable(metricName: String, isUp: Boolean): Boolean {
    val lower = metricName.lowercase()
    val isLowerBetter = lower.contains("weight") ||
            lower.contains("fat") ||
            lower.contains("heart") ||
            lower.contains("pulse") ||
            lower.contains("bpm") ||
            lower.contains("pressure")
    return if (isLowerBetter) !isUp else isUp
}

@Composable
fun ProfileSummaryCard(
    name: String,
    photoUri: String?,
    age: Int?,
    heightCm: Double?,
    currentWeight: Double?,
    weightUnit: String = "kg",
    onEditProfileClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(photoUri) {
        if (photoUri != null) {
            try {
                BitmapFactory.decodeFile(photoUri)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // BMI Calculation (converts lbs to kg if unit is lbs)
    val bmiValue = remember(heightCm, currentWeight, weightUnit) {
        if (heightCm != null && currentWeight != null && heightCm > 0) {
            val weightInKg = if (weightUnit.lowercase().contains("lb")) currentWeight * 0.453592 else currentWeight
            val heightM = heightCm / 100.0
            weightInKg / (heightM * heightM)
        } else null
    }

    val secondaryColor = MaterialTheme.colorScheme.secondary
    val primaryColor = MaterialTheme.colorScheme.primary

    val (bmiCategory, bmiColor) = remember(bmiValue, secondaryColor, primaryColor) {
        when {
            bmiValue == null -> "NO BMI DATA" to secondaryColor
            bmiValue < 18.5 -> "UNDERWEIGHT" to Color(0xFF38BDF8) // Cyan
            bmiValue < 25.0 -> "NORMAL WEIGHT" to Color(0xFF10B981) // Emerald
            bmiValue < 30.0 -> "OVERWEIGHT" to Color(0xFFF59E0B) // Amber
            else -> "OBESE" to SystemRed // Crimson
        }
    }

    GainsCard(
        onClick = onEditProfileClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Avatar, Identity & Quick Edit CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Elevated Avatar with Accent Ring Glow
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(InfraredAccent.copy(alpha = 0.25f), MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                )
                            )
                            .border(2.dp, Brush.linearGradient(listOf(InfraredAccent, MaterialTheme.colorScheme.outline)), CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = name.take(1).uppercase(Locale.getDefault()),
                                style = HeaderBold.copy(fontSize = 22.sp),
                                color = InfraredAccent
                            )
                        }
                    }

                    Column {
                        Text(
                            text = name,
                            style = HeaderBold.copy(fontSize = 19.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = "ATHLETE PROFILE",
                                style = LabelCaps.copy(fontSize = 9.sp, letterSpacing = 1.2.sp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Edit Profile Icon Button
                if (onEditProfileClick != null) {
                    IconButton(
                        onClick = onEditProfileClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)

            // Stat Chips Grid Row (Age, Height, Weight)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileStatChip(
                    label = "AGE",
                    value = age?.let { "$it yrs" } ?: "--",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ProfileStatChip(
                    label = "HEIGHT",
                    value = heightCm?.let { "${it.toInt()} cm" } ?: "--",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ProfileStatChip(
                    label = "WEIGHT",
                    value = currentWeight?.let { if (it % 1.0 == 0.0) "${it.toInt()} $weightUnit" else String.format(Locale.getDefault(), "%.1f %s", it, weightUnit) } ?: "--",
                    modifier = Modifier.weight(1f)
                )
            }

            // Visual Interactive BMI Gauge Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "BODY MASS INDEX",
                            style = LabelCaps.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(bmiColor.copy(alpha = 0.15f))
                            .border(1.dp, bmiColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (bmiValue != null) String.format(Locale.getDefault(), "BMI %.1f • %s", bmiValue, bmiCategory) else "NO BMI DATA",
                            style = LabelCaps.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = bmiColor
                        )
                    }
                }

                // Interactive Graphic BMI Spectrum Track Bar
                VisualBmiScaleBar(
                    bmiValue = bmiValue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = LabelCaps.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = BodySemiBold.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun VisualBmiScaleBar(
    bmiValue: Double?,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barH = 6.dp.toPx()
        val topY = (h - barH) / 2f
        val r = CornerRadius(3.dp.toPx(), 3.dp.toPx())

        // BMI category segments: 15-18.5 (Cyan), 18.5-25 (Emerald), 25-30 (Amber), 30-38 (Red)
        val minBmi = 15f
        val maxBmi = 38f
        val totalRange = maxBmi - minBmi

        val p1 = (18.5f - minBmi) / totalRange
        val p2 = (25.0f - minBmi) / totalRange
        val p3 = (30.0f - minBmi) / totalRange

        // Segment 1: Underweight
        drawRoundRect(
            color = Color(0xFF38BDF8),
            topLeft = Offset(0f, topY),
            size = Size(w * p1, barH),
            cornerRadius = r
        )
        // Segment 2: Normal
        drawRoundRect(
            color = Color(0xFF10B981),
            topLeft = Offset(w * p1 + 2.dp.toPx(), topY),
            size = Size(w * (p2 - p1) - 4.dp.toPx(), barH),
            cornerRadius = r
        )
        // Segment 3: Overweight
        drawRoundRect(
            color = Color(0xFFF59E0B),
            topLeft = Offset(w * p2 + 2.dp.toPx(), topY),
            size = Size(w * (p3 - p2) - 4.dp.toPx(), barH),
            cornerRadius = r
        )
        // Segment 4: Obese
        drawRoundRect(
            color = SystemRed,
            topLeft = Offset(w * p3 + 2.dp.toPx(), topY),
            size = Size(w * (1f - p3) - 2.dp.toPx(), barH),
            cornerRadius = r
        )

        // Indicator Dot
        if (bmiValue != null) {
            val clamped = bmiValue.toFloat().coerceIn(minBmi, maxBmi)
            val posX = w * ((clamped - minBmi) / totalRange)

            // Outer white ring + primary color core
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = Offset(posX, h / 2f)
            )
            drawCircle(
                color = primaryColor,
                radius = 4.dp.toPx(),
                center = Offset(posX, h / 2f)
            )
        }
    }
}

@Composable
fun BentoMetricCard(
    metric: MetricWithLatestEntry,
    recentEntries: List<Float> = emptyList(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val isHc = metric.source == "HEALTH_CONNECT"
    val accentThemeColor = getMetricThemeColor(metric.name)

    // Calculate percent trend if recentEntries has at least 2 points
    val trendPct = remember(recentEntries) {
        if (recentEntries.size >= 2) {
            val first = recentEntries.first()
            val last = recentEntries.last()
            if (first > 0f) ((last - first) / first) * 100f else null
        } else null
    }

    GainsCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Metric Accent Tag & Source Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentThemeColor)
                    )
                    Text(
                        text = metric.name.uppercase(),
                        style = LabelCaps.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Health Connect vs Manual Sync Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isHc) PrimarySoftBg else MaterialTheme.colorScheme.background)
                        .border(
                            width = 1.dp,
                            color = if (isHc) InfraredAccent.copy(alpha = 0.6f) else outlineColor,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (isHc) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Health Connect",
                                tint = InfraredAccent,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        Text(
                            text = if (isHc) "HC" else "MANUAL",
                            style = LabelCaps.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = if (isHc) InfraredAccent else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Value & Sparkline / Arc Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    val latestVal = metric.latestValue
                    if (latestVal != null) {
                        Text(
                            text = if (latestVal % 1f == 0f) latestVal.toInt().toString() else String.format(Locale.getDefault(), "%.1f", latestVal),
                            style = HeaderBold.copy(fontSize = 24.sp, fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = metric.unit.uppercase(),
                                style = LabelCaps.copy(fontSize = 9.sp),
                                color = accentThemeColor
                            )
                            if (trendPct != null) {
                                val isUp = trendPct >= 0f
                                val isGood = isTrendDesirable(metric.name, isUp)
                                val trendColor = if (isGood) Color(0xFF10B981) else SystemRed
                                val arrow = if (isUp) "▲" else "▼"
                                Text(
                                    text = String.format(Locale.getDefault(), "%s%.1f%% %s", if (isUp) "+" else "", trendPct, arrow),
                                    style = LabelCaps.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = trendColor
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "--",
                            style = HeaderBold.copy(fontSize = 24.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "TAP TO LOG",
                            style = LabelCaps.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Smooth Graphic Sparkline or Progress Arc
                if (recentEntries.size > 1) {
                    MiniSparkline(
                        values = recentEntries,
                        color = accentThemeColor,
                        modifier = Modifier
                            .width(68.dp)
                            .height(34.dp)
                    )
                } else if (metric.targetValue != null && metric.latestValue != null) {
                    MiniProgressArc(
                        current = metric.latestValue,
                        target = metric.targetValue,
                        color = accentThemeColor,
                        modifier = Modifier.size(38.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Details",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniSparkline(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val range = if (max == min) 1f else max - min

        val width = size.width
        val height = size.height
        val stepX = width / (values.size - 1).toFloat()

        val strokePath = Path()
        val fillPath = Path()

        val points = values.mapIndexed { i, v ->
            val normY = 1f - ((v - min) / range)
            Offset(i * stepX, normY * height)
        }

        strokePath.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, height)
        fillPath.lineTo(points.first().x, points.first().y)

        // Draw smooth cubic curves between points
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
            val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)

            strokePath.cubicTo(
                controlPoint1.x, controlPoint1.y,
                controlPoint2.x, controlPoint2.y,
                p2.x, p2.y
            )
            fillPath.cubicTo(
                controlPoint1.x, controlPoint1.y,
                controlPoint2.x, controlPoint2.y,
                p2.x, p2.y
            )
        }

        fillPath.lineTo(points.last().x, height)
        fillPath.close()

        // Gradient Fill underneath curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Stroke Line
        drawPath(
            path = strokePath,
            color = color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun MiniProgressArc(
    current: Float,
    target: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = (current / target).coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        val strokeWidth = 3.5.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val arcSize = Size(diameter, diameter)

        // Track Arc
        drawArc(
            color = color.copy(alpha = 0.15f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Dynamic Progress Arc with Cap
        drawArc(
            color = color,
            startAngle = 135f,
            sweepAngle = 270f * progress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun GoalsHubCard(
    gymWorkoutsThisWeek: Int,
    gymWorkoutsTarget: Int = 4,
    weightCurrent: Float?,
    weightTarget: Float?,
    weightUnit: String = "kg",
    onSetGoalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GainsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Title & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = InfraredAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "MY GOALS HUB",
                        style = HeaderBold.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = onSetGoalClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Set Goal",
                        tint = InfraredAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MANAGE GOALS",
                        style = LabelCaps.copy(fontSize = 9.sp),
                        color = InfraredAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Goal 1: Weekly Gym Workouts
            GoalProgressRow(
                title = "Weekly Gym Workouts",
                currentStr = "$gymWorkoutsThisWeek / $gymWorkoutsTarget sessions",
                progress = (gymWorkoutsThisWeek.toFloat() / gymWorkoutsTarget).coerceIn(0f, 1f),
                color = InfraredAccent,
                isCompleted = gymWorkoutsThisWeek >= gymWorkoutsTarget
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Goal 2: Target Weight Goal
            if (weightTarget != null) {
                val current = weightCurrent ?: 0f
                val pct = if (current > 0f) (current / weightTarget).coerceIn(0.5f, 1.2f) else 0.8f
                val progressVal = if (pct > 1f) 2f - pct else pct
                GoalProgressRow(
                    title = "Target Weight Goal",
                    currentStr = "${current} / ${weightTarget} $weightUnit",
                    progress = progressVal,
                    color = Color(0xFF0EA5E9),
                    isCompleted = weightCurrent != null && Math.abs(weightCurrent - weightTarget) < 0.5f
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable { onSetGoalClick() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "+ Set a target weight goal",
                            style = BodySemiBold.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GoalProgressRow(
    title: String,
    currentStr: String,
    progress: Float,
    color: Color,
    isCompleted: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = BodySemiBold.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = currentStr,
                    style = LabelCaps.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = color
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Multi-gradient progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
    }
}
