package com.example.gains.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gains.data.WorkoutTemplateWithDetails
import com.example.gains.theme.BodySemiBold
import com.example.gains.theme.InfraredAccent
import com.example.gains.theme.LabelCaps

@Composable
fun SelectWorkoutTypeDialog(
    title: String = "START WORKOUT",
    templates: List<WorkoutTemplateWithDetails> = emptyList(),
    onDismiss: () -> Unit,
    onTypeSelect: (String) -> Unit,
    onTemplateSelect: (Long) -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (templates.isNotEmpty()) {
                    Text(
                        text = "SAVED ROUTINES",
                        style = LabelCaps.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        textAlign = TextAlign.Start
                    )
                    templates.forEach { tmpl ->
                        WorkoutTypeOptionRow(
                            title = tmpl.name,
                            description = "${tmpl.exerciseCount} Exercises • ${tmpl.totalSets} Sets",
                            icon = Icons.Default.FitnessCenter,
                            onClick = { onTemplateSelect(tmpl.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "OR CHOOSE WORKOUT TYPE",
                        style = LabelCaps.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        textAlign = TextAlign.Start
                    )
                }

                // Gym Option
                WorkoutTypeOptionRow(
                    title = "Gym Workout",
                    description = "Strength & bodybuilding tracking",
                    icon = Icons.Default.FitnessCenter,
                    onClick = { onTypeSelect("GYM") }
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Run Option
                WorkoutTypeOptionRow(
                    title = "Running Session",
                    description = "Cardio, pace, & distance",
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    onClick = { onTypeSelect("RUN") }
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Hyrox Option
                WorkoutTypeOptionRow(
                    title = "Hyrox Challenge",
                    description = "Functional fitness racing",
                    icon = Icons.Default.FlashOn,
                    onClick = { onTypeSelect("HYROX") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", style = LabelCaps, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun WorkoutTypeOptionRow(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(InfraredAccent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = InfraredAccent,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = BodySemiBold.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = LabelCaps.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
