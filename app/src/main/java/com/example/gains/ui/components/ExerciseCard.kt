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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gains.data.LoggedSetWithExercise
import com.example.gains.theme.BodySemiBold
import com.example.gains.theme.LabelCaps
import com.example.gains.theme.PrimarySoftBg

@Composable
fun ExerciseCard(
    exerciseName: String,
    muscleGroup: String,
    sets: List<LoggedSetWithExercise>,
    onAddSetClick: () -> Unit,
    onWeightChange: (Int, Double) -> Unit,
    onRepsChange: (Int, Int) -> Unit,
    onToggleComplete: (Int) -> Unit,
    onDeleteSet: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false
) {
    GainsCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = exerciseName,
                        style = BodySemiBold.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PrimarySoftBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = muscleGroup.uppercase(),
                            style = LabelCaps.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (!isReadOnly) {
                    TextButton(onClick = onAddSetClick) {
                        Text("+ ADD SET", style = LabelCaps, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Set Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SET", modifier = Modifier.weight(1f), style = LabelCaps, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                Text("PREVIOUS", modifier = Modifier.weight(2f), style = LabelCaps, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                Text("KG", modifier = Modifier.weight(2f), style = LabelCaps, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                Text("REPS", modifier = Modifier.weight(2f), style = LabelCaps, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                Text("DONE", modifier = Modifier.weight(1.5f), style = LabelCaps, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                // Spacer matching the delete button area width in SetRow to fix alignment
                Spacer(modifier = Modifier.width(28.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Set rows
            sets.forEachIndexed { index, set ->
                SetRow(
                    index = index + 1,
                    weight = set.weight,
                    reps = set.reps,
                    isCompleted = set.isCompleted,
                    onWeightChange = { w -> onWeightChange(set.id, w) },
                    onRepsChange = { r -> onRepsChange(set.id, r) },
                    onToggleComplete = { onToggleComplete(set.id) },
                    onDelete = { onDeleteSet(set.id) },
                    isReadOnly = isReadOnly
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
