package com.example.gains.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gains.theme.AccentGreen
import com.example.gains.theme.AccentGreenBg

@Composable
fun SetRow(
    index: Int,
    weight: Double,
    reps: Int,
    isCompleted: Boolean,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val rowBgColor = if (isCompleted) AccentGreenBg else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBgColor)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set index
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
        }

        // Previous (Placeholder)
        Text(
            text = "—",
            modifier = Modifier.weight(2f),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )

        // Weight Input (Custom Box wrapper with BasicTextField for premium centering & padding control)
        var weightInput by remember(weight) { mutableStateOf(if (weight == 0.0) "" else weight.toString()) }
        Box(
            modifier = Modifier
                .weight(2f)
                .padding(horizontal = 4.dp)
                .height(38.dp) // Sleek compact height matching standard fitness input fields
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = weightInput,
                onValueChange = { newValue ->
                    weightInput = newValue
                    newValue.toDoubleOrNull()?.let { onWeightChange(it) }
                },
                enabled = !isReadOnly,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = if (isReadOnly) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
        }

        // Reps Input (Custom Box wrapper with BasicTextField)
        var repsInput by remember(reps) { mutableStateOf(if (reps == 0) "" else reps.toString()) }
        Box(
            modifier = Modifier
                .weight(2f)
                .padding(horizontal = 4.dp)
                .height(38.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = repsInput,
                onValueChange = { newValue ->
                    repsInput = newValue
                    newValue.toIntOrNull()?.let { onRepsChange(it) }
                },
                enabled = !isReadOnly,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = if (isReadOnly) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
        }

        // Complete Checkbox / Circle
        Box(
            modifier = Modifier
                .weight(1.5f),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onToggleComplete,
                enabled = !isReadOnly,
                modifier = Modifier.size(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) AccentGreen else Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = if (isCompleted) AccentGreen else MaterialTheme.colorScheme.secondary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Delete Button
        if (!isReadOnly) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(28.dp)
                    .padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Set",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            // Spacer to keep layout balanced when delete is hidden
            Spacer(modifier = Modifier.width(28.dp))
        }
    }
}
