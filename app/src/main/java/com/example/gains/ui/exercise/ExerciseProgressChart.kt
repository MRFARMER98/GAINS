package com.example.gains.ui.exercise

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gains.theme.BodySemiBold
import com.example.gains.theme.InfraredAccent
import com.example.gains.theme.LabelCaps
import com.example.gains.ui.components.GainsCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExerciseProgressChart(
    points: List<ExerciseProgressPoint>,
    modifier: Modifier = Modifier
) {
    GainsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "PROGRESSION TREND (EST. 1RM)",
                style = LabelCaps,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (points.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log at least 2 sessions to unlock\nprogressive overload trend charts.",
                        style = BodySemiBold.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val minWeight = remember(points) { (points.minOf { it.est1RM } * 0.9).coerceAtLeast(0.0) }
                val maxWeight = remember(points) { (points.maxOf { it.est1RM } * 1.1).coerceAtLeast(minWeight + 1.0) }
                val sdf = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val strokeColor = InfraredAccent
                    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    val labelColor = MaterialTheme.colorScheme.secondary

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val paddingLeft = 10.dp.toPx()
                        val paddingRight = 10.dp.toPx()
                        val paddingTop = 10.dp.toPx()
                        val paddingBottom = 20.dp.toPx()

                        val chartWidth = size.width - paddingLeft - paddingRight
                        val chartHeight = size.height - paddingTop - paddingBottom

                        if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

                        // Horizontal gridlines (3 lines)
                        val gridLines = 3
                        for (i in 0 until gridLines) {
                            val y = paddingTop + (chartHeight / (gridLines - 1)) * i
                            drawLine(
                                color = gridColor,
                                start = Offset(paddingLeft, y),
                                end = Offset(size.width - paddingRight, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        // Map points to coordinates
                        val offsets = points.mapIndexed { index, point ->
                            val x = paddingLeft + (index.toFloat() / (points.size - 1)) * chartWidth
                            val weightNormalized = ((point.est1RM - minWeight) / (maxWeight - minWeight)).toFloat()
                            val y = paddingTop + chartHeight * (1f - weightNormalized)
                            Offset(x, y)
                        }

                        // Build smooth fill and stroke path
                        val strokePath = Path()
                        val fillPath = Path()

                        offsets.forEachIndexed { i, offset ->
                            if (i == 0) {
                                strokePath.moveTo(offset.x, offset.y)
                                fillPath.moveTo(offset.x, size.height - paddingBottom)
                                fillPath.lineTo(offset.x, offset.y)
                            } else {
                                val prev = offsets[i - 1]
                                val control1 = Offset((prev.x + offset.x) / 2f, prev.y)
                                val control2 = Offset((prev.x + offset.x) / 2f, offset.y)
                                strokePath.cubicTo(control1.x, control1.y, control2.x, control2.y, offset.x, offset.y)
                                fillPath.cubicTo(control1.x, control1.y, control2.x, control2.y, offset.x, offset.y)
                            }
                        }

                        fillPath.lineTo(offsets.last().x, size.height - paddingBottom)
                        fillPath.close()

                        // Draw area gradient fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(strokeColor.copy(alpha = 0.25f), Color.Transparent),
                                startY = paddingTop,
                                endY = size.height - paddingBottom
                            )
                        )

                        // Draw main line path
                        drawPath(
                            path = strokePath,
                            color = strokeColor,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw data points
                        offsets.forEach { pt ->
                            drawCircle(
                                color = strokeColor,
                                radius = 5.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }
        }
    }
}
