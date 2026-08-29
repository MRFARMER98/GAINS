package com.example.gains.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gains.data.WorkoutSessionWithLabel
import com.example.gains.theme.BodySemiBold
import com.example.gains.theme.LabelCaps
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryCard(
    session: WorkoutSessionWithLabel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val date = Date(session.timestamp)
    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
    val dateFormat = SimpleDateFormat("MMM d, yyyy - hh:mm a", Locale.getDefault()).format(date)

    val icon = when (session.workoutType) {
        "RUN" -> Icons.AutoMirrored.Filled.DirectionsRun
        "HYROX" -> Icons.Default.FlashOn
        else -> Icons.Default.FitnessCenter
    }

    GainsCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Workout Type Icon Container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dayFormat,
                            style = BodySemiBold.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Small label tag badge
                        if (session.labelName != null && session.labelColorHex != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            val color = try {
                                Color(android.graphics.Color.parseColor(session.labelColorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = session.labelName.uppercase(),
                                    style = LabelCaps.copy(fontSize = 8.sp),
                                    color = color
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateFormat,
                        style = BodySemiBold.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
