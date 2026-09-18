package com.example.gains.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gains.data.UserProfile
import com.example.gains.theme.BodySemiBold
import com.example.gains.theme.HeaderBold
import com.example.gains.theme.LabelCaps
import com.example.gains.theme.SystemRed
import java.io.File
import java.io.FileOutputStream

@Composable
fun ConfirmDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = "DELETE WORKOUT?",
    message: String = "Are you sure you want to permanently delete this workout session? All logged sets will be removed."
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
                    color = SystemRed,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = message,
                    style = BodySemiBold.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", style = LabelCaps, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("DELETE", style = LabelCaps, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateLabelDialog(
    onDismiss: () -> Unit,
    onCreateClick: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val colors = listOf("#FF5E3A", "#10B981", "#0EA5E9", "#8B5CF6", "#F59E0B", "#F43F5E")
    var selectedColor by remember { mutableStateOf(colors.first()) }
    
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
                    .padding(20.dp)
            ) {
                Text(
                    text = "CREATE WORKOUT LABEL",
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Label name (e.g. Leg Day)", color = MaterialTheme.colorScheme.secondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "CHOOSE COLOR",
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { hex ->
                         val isSelected = selectedColor == hex
                         val color = Color(android.graphics.Color.parseColor(hex))
                         Box(
                             modifier = Modifier
                                 .size(36.dp)
                                 .clip(CircleShape)
                                 .background(color)
                                 .border(
                                     width = if (isSelected) 3.dp else 0.dp,
                                     color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                     shape = CircleShape
                                 )
                                 .clickable { selectedColor = hex }
                         )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", style = LabelCaps, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onCreateClick(name, selectedColor)
                            }
                        },
                        enabled = name.isNotBlank(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    profile: UserProfile?,
    onDismiss: () -> Unit,
    onSaveClick: (name: String, photoUri: String?, height: Double?, age: Int?, weight: Double?, birthDateTimestamp: Long?) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var height by remember { mutableStateOf(profile?.height?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var weight by remember { mutableStateOf(profile?.currentWeight?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var birthDateTimestamp by remember { mutableStateOf(profile?.birthDateTimestamp) }
    var photoUri by remember { mutableStateOf(profile?.photoUri) }
    var showDatePicker by remember { mutableStateOf(false) }

    val calculatedAge = remember(birthDateTimestamp, profile?.age) {
        val dob = birthDateTimestamp
        if (dob != null && dob > 0L) {
            val dobCal = java.util.Calendar.getInstance().apply { timeInMillis = dob }
            val nowCal = java.util.Calendar.getInstance()
            var years = nowCal.get(java.util.Calendar.YEAR) - dobCal.get(java.util.Calendar.YEAR)
            if (nowCal.get(java.util.Calendar.DAY_OF_YEAR) < dobCal.get(java.util.Calendar.DAY_OF_YEAR)) {
                years--
            }
            years.coerceAtLeast(0)
        } else {
            profile?.age
        }
    }

    val birthDateFormatted = remember(birthDateTimestamp) {
        val dob = birthDateTimestamp
        if (dob != null && dob > 0L) {
            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(dob))
        } else {
            "Select Birth Date"
        }
    }

    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = copyUriToInternalStorage(context, it)
            if (savedPath != null) {
                photoUri = savedPath
            }
        }
    }

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
                    text = "EDIT PROFILE",
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(photoUri) {
                        val uriStr = photoUri
                        if (uriStr != null) {
                            try {
                                BitmapFactory.decodeFile(uriStr)
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Change profile picture",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "PHOTO",
                                style = LabelCaps.copy(fontSize = 8.sp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = MaterialTheme.colorScheme.secondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Height (cm)", color = MaterialTheme.colorScheme.secondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg)", color = MaterialTheme.colorScheme.secondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date of Birth Selection & Calculated Age Display
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DATE OF BIRTH",
                        style = LabelCaps.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = birthDateFormatted,
                                style = LabelCaps.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                color = if (birthDateTimestamp != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary
                            )
                            if (calculatedAge != null) {
                                Text(
                                    text = "$calculatedAge YRS OLD",
                                    style = LabelCaps.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", style = LabelCaps, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSaveClick(
                                    name,
                                    photoUri,
                                    height.toDoubleOrNull(),
                                    calculatedAge,
                                    weight.toDoubleOrNull(),
                                    birthDateTimestamp
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("SAVE", style = LabelCaps, color = Color.White)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = birthDateTimestamp ?: (System.currentTimeMillis() - 820_800_000_000L) // Default ~26 yrs ago
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    birthDateTimestamp = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("CANCEL")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        context.filesDir.listFiles { _, name -> name.startsWith("profile_picture_") }?.forEach {
            it.delete()
        }
        val newFile = File(context.filesDir, "profile_picture_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(newFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        newFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
