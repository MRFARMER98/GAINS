package com.example.gains.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation3.runtime.NavKey
import com.example.gains.WorkoutLogger
import com.example.gains.data.PlannedSession
import com.example.gains.data.WorkoutLabel
import com.example.gains.data.WorkoutSessionWithLabel
import com.example.gains.theme.BodySemiBold
import com.example.gains.theme.HeaderBold
import com.example.gains.theme.InfraredAccent
import com.example.gains.theme.LabelCaps
import com.example.gains.theme.PrimarySoftBg
import com.example.gains.theme.SystemRed
import com.example.gains.ui.components.GainsCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─── Models ───────────────────────────────────────────────────────────────────

data class CalendarDay(
    val date: Date,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isPast: Boolean,
    val completedSessions: List<WorkoutSessionWithLabel>,
    val plannedSessions: List<PlannedSession>
)

// ─── Helper ───────────────────────────────────────────────────────────────────

private fun parseColorHex(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

// ─── Main Composable ──────────────────────────────────────────────────────────

enum class PlannerViewMode { MONTH, WEEK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerTabContent(
    sessions: List<WorkoutSessionWithLabel>,
    plannedSessions: List<PlannedSession>,
    labels: List<WorkoutLabel>,
    onSchedulePlan: (dateTimestamp: Long, name: String, workoutType: String, labelId: Int?) -> Unit,
    onDeletePlan: (id: Long) -> Unit,
    onStartPlan: (planned: PlannedSession) -> Unit,
    onItemClick: (NavKey) -> Unit
) {
    var viewModeStr by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(PlannerViewMode.WEEK.name) }
    val viewMode = PlannerViewMode.valueOf(viewModeStr)
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var weekStart by remember {
        mutableStateOf(run {
            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.time
        })
    }
    var selectedDate by remember { mutableStateOf(Date()) }
    var showPlanDialog by remember { mutableStateOf(false) }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val weekRangeFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val dayHeaderFormat = remember { SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()) }
    val dayKeyFormat    = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val weekDayFormat   = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

    val todayStr       = remember { dayKeyFormat.format(Date()) }
    val selectedDateStr = remember(selectedDate) { dayKeyFormat.format(selectedDate) }

    // Sessions & planned indexed by day key
    val sessionsByDay = remember(sessions) {
        sessions.groupBy { dayKeyFormat.format(Date(it.timestamp)) }
    }
    val plannedByDay = remember(plannedSessions) {
        plannedSessions.groupBy { dayKeyFormat.format(Date(it.dateTimestamp)) }
    }
    val labelsMap = remember(labels) { labels.associateBy { it.id } }

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
    }

    // ─── Monthly grid ──────────────────────────────────────────────────────────
    val daysInGrid = remember(calendarMonth, sessionsByDay, plannedByDay, todayStart) {
        val grid = mutableListOf<CalendarDay>()
        val cal = calendarMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val firstOffset = if (dow == Calendar.SUNDAY) 6 else dow - 2
        cal.add(Calendar.DAY_OF_MONTH, -firstOffset)
        val totalCells = if (firstOffset + calendarMonth.getActualMaximum(Calendar.DAY_OF_MONTH) > 35) 42 else 35
        repeat(totalCells) {
            val date = cal.time
            val key  = dayKeyFormat.format(date)
            grid.add(CalendarDay(
                date              = date,
                dayOfMonth        = cal.get(Calendar.DAY_OF_MONTH),
                isCurrentMonth    = cal.get(Calendar.MONTH) == calendarMonth.get(Calendar.MONTH),
                isToday           = key == todayStr,
                isPast            = date.before(todayStart),
                completedSessions = sessionsByDay[key] ?: emptyList(),
                plannedSessions   = plannedByDay[key] ?: emptyList()
            ))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        grid
    }
    val dayRows = remember(daysInGrid) { daysInGrid.chunked(7) }

    // ─── Weekly grid ───────────────────────────────────────────────────────────
    val weekDays = remember(weekStart, sessionsByDay, plannedByDay, todayStart) {
        val cal = Calendar.getInstance().apply { time = weekStart }
        (0 until 7).map {
            val date = cal.time
            val key  = dayKeyFormat.format(date)
            val day  = CalendarDay(
                date              = date,
                dayOfMonth        = cal.get(Calendar.DAY_OF_MONTH),
                isCurrentMonth    = true,
                isToday           = key == todayStr,
                isPast            = date.before(todayStart),
                completedSessions = sessionsByDay[key] ?: emptyList(),
                plannedSessions   = plannedByDay[key] ?: emptyList()
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
            day
        }
    }

    val weekEnd = remember(weekStart) {
        Calendar.getInstance().apply { time = weekStart; add(Calendar.DAY_OF_MONTH, 6) }.time
    }

    // Selected day details
    val selectedDayCompleted = remember(selectedDateStr, sessionsByDay) { sessionsByDay[selectedDateStr] ?: emptyList() }
    val selectedDayPlanned   = remember(selectedDateStr, plannedByDay)  { plannedByDay[selectedDateStr]  ?: emptyList() }

    // ─── UI ────────────────────────────────────────────────────────────────────

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Planner", style = HeaderBold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Schedule & track sessions",
                        fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // MONTH / WEEK toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
                ) {
                    listOf(PlannerViewMode.WEEK, PlannerViewMode.MONTH).forEach { mode ->
                        val selected = viewMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) PrimarySoftBg else Color.Transparent)
                                .clickable { viewModeStr = mode.name }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (mode == PlannerViewMode.MONTH) "MONTH" else "WEEK",
                                style = LabelCaps.copy(fontSize = 10.sp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // ── Period selector card ───────────────────────────────────────────────
        item {
            GainsCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (viewMode == PlannerViewMode.MONTH) {
                            val cal = calendarMonth.clone() as Calendar
                            cal.add(Calendar.MONTH, -1)
                            calendarMonth = cal
                        } else {
                            val cal = Calendar.getInstance().apply { time = weekStart; add(Calendar.WEEK_OF_YEAR, -1) }
                            weekStart = cal.time
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    val periodLabel = if (viewMode == PlannerViewMode.MONTH) {
                        monthYearFormat.format(calendarMonth.time).uppercase()
                    } else {
                        "${weekRangeFormat.format(weekStart)} – ${weekRangeFormat.format(weekEnd)}".uppercase()
                    }
                    Text(periodLabel, style = LabelCaps.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)

                    IconButton(onClick = {
                        if (viewMode == PlannerViewMode.MONTH) {
                            val cal = calendarMonth.clone() as Calendar
                            cal.add(Calendar.MONTH, 1)
                            calendarMonth = cal
                        } else {
                            val cal = Calendar.getInstance().apply { time = weekStart; add(Calendar.WEEK_OF_YEAR, 1) }
                            weekStart = cal.time
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // ── MONTH VIEW ────────────────────────────────────────────────────────
        if (viewMode == PlannerViewMode.MONTH) {

            // Day-of-week header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("MON","TUE","WED","THU","FRI","SAT","SUN").forEach { day ->
                        Text(
                            text = day,
                            style = LabelCaps.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Day rows
            items(dayRows) { rowDays ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowDays.forEach { cell ->
                        val isSelected = dayKeyFormat.format(cell.date) == selectedDateStr
                        val cellBg = when {
                            isSelected      -> PrimarySoftBg
                            cell.isPast     -> MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                            else            -> MaterialTheme.colorScheme.surface
                        }
                        val borderColor = when {
                            isSelected   -> MaterialTheme.colorScheme.primary
                            cell.isToday -> InfraredAccent
                            else         -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f).aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellBg)
                                .border(BorderStroke(if (isSelected || cell.isToday) 1.5.dp else 0.dp, borderColor), RoundedCornerShape(8.dp))
                                .clickable { selectedDate = cell.date }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = cell.dayOfMonth.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = if (cell.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        !cell.isCurrentMonth -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                        isSelected           -> MaterialTheme.colorScheme.primary
                                        cell.isToday         -> InfraredAccent
                                        cell.isPast          -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                        else                 -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    cell.completedSessions.take(2).forEach { s ->
                                        Box(Modifier.size(5.dp).clip(CircleShape).background(parseColorHex(s.labelColorHex, MaterialTheme.colorScheme.primary)))
                                    }
                                    cell.plannedSessions.take(2).forEach { p ->
                                        Box(Modifier.size(5.dp).clip(CircleShape).background(parseColorHex(labelsMap[p.labelId]?.colorHex, InfraredAccent)))
                                    }
                                    if (cell.isPast && cell.isCurrentMonth && cell.completedSessions.isEmpty() && cell.plannedSessions.isEmpty()) {
                                        Box(Modifier.size(3.5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── WEEK VIEW ─────────────────────────────────────────────────────────
        if (viewMode == PlannerViewMode.WEEK) {
            items(weekDays) { cell ->
                val isSelected = dayKeyFormat.format(cell.date) == selectedDateStr
                val borderColor = when {
                    isSelected   -> MaterialTheme.colorScheme.primary
                    cell.isToday -> InfraredAccent
                    else         -> MaterialTheme.colorScheme.outline.copy(alpha = if (cell.isPast) 0.4f else 1f)
                }
                val bgColor = when {
                    isSelected  -> PrimarySoftBg
                    cell.isPast -> MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                    else        -> MaterialTheme.colorScheme.surface
                }

                GainsCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(if (isSelected || cell.isToday) 1.5.dp else 1.dp, borderColor), RoundedCornerShape(12.dp))
                        .clickable { selectedDate = cell.date }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Day column
                        Column(
                            modifier = Modifier.width(42.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = weekDayFormat.format(cell.date).uppercase(),
                                style = LabelCaps.copy(fontSize = 9.sp),
                                color = when {
                                    cell.isToday -> InfraredAccent
                                    cell.isPast  -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                    else         -> MaterialTheme.colorScheme.secondary
                                }
                            )
                            Text(
                                text = cell.dayOfMonth.toString(),
                                fontSize = 20.sp,
                                fontWeight = if (cell.isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isSelected   -> MaterialTheme.colorScheme.primary
                                    cell.isToday -> InfraredAccent
                                    cell.isPast  -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                    else         -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        // Session pills or empty label
                        if (cell.completedSessions.isEmpty() && cell.plannedSessions.isEmpty()) {
                            Text(
                                text = if (cell.isPast) "Rest day" else "",
                                style = BodySemiBold.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                cell.completedSessions.forEach { s ->
                                    val c = parseColorHex(s.labelColorHex, MaterialTheme.colorScheme.primary)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(Modifier.size(6.dp).clip(CircleShape).background(c))
                                        Text(s.name, style = BodySemiBold.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                cell.plannedSessions.forEach { p ->
                                    val c = parseColorHex(labelsMap[p.labelId]?.colorHex, InfraredAccent)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(Modifier.size(6.dp).clip(CircleShape).background(c.copy(alpha = 0.5f)))
                                        Text(
                                            p.name,
                                            style = BodySemiBold.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Selected day agenda header (single + Plan button here) ────────────
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayHeaderFormat.format(selectedDate).uppercase(),
                    style = LabelCaps,
                    color = MaterialTheme.colorScheme.secondary
                )
                // ← The one and only "+ PLAN WORKOUT" button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showPlanDialog = true }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, "Plan", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PLAN WORKOUT", style = LabelCaps.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ── Completed workouts ─────────────────────────────────────────────────
        if (selectedDayCompleted.isNotEmpty()) {
            item {
                Text("COMPLETED WORKOUTS", style = LabelCaps.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.secondary)
            }
            items(selectedDayCompleted, key = { it.id }) { session ->
                val labelColor = parseColorHex(session.labelColorHex, MaterialTheme.colorScheme.primary)
                GainsCard(modifier = Modifier.clickable { onItemClick(WorkoutLogger(session.id)) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(labelColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null, tint = labelColor, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(session.name, style = BodySemiBold.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    session.labelName?.uppercase() ?: "COMPLETED WORKOUT",
                                    style = LabelCaps.copy(fontSize = 9.sp), color = labelColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Planned workouts ───────────────────────────────────────────────────
        if (selectedDayPlanned.isNotEmpty()) {
            item {
                Text("SCHEDULED PLANNED SESSIONS", style = LabelCaps.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.secondary)
            }
            items(selectedDayPlanned, key = { it.id }) { planned ->
                val labelObj   = labelsMap[planned.labelId]
                val tagColor   = parseColorHex(labelObj?.colorHex, InfraredAccent)
                GainsCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(planned.name, style = BodySemiBold.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                labelObj?.name?.uppercase() ?: "SCHEDULED ${planned.workoutType}",
                                style = LabelCaps.copy(fontSize = 9.sp), color = tagColor
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { onStartPlan(planned) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, "Start", modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("START", style = LabelCaps.copy(fontSize = 9.sp))
                            }
                            Spacer(Modifier.width(6.dp))
                            IconButton(onClick = { onDeletePlan(planned.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, "Delete", tint = SystemRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // ── Empty state ────────────────────────────────────────────────────────
        if (selectedDayCompleted.isEmpty() && selectedDayPlanned.isEmpty()) {
            item {
                GainsCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No workouts logged or planned for this day.",
                            style = BodySemiBold.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showPlanDialog) {
        PlanWorkoutDialog(
            selectedDate = selectedDate,
            labels = labels,
            onDismiss = { showPlanDialog = false },
            onSave = { name, workoutType, labelId ->
                showPlanDialog = false
                val cal = Calendar.getInstance().apply {
                    time = selectedDate
                    set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                onSchedulePlan(cal.timeInMillis, name, workoutType, labelId)
            }
        )
    }
}

// ─── Plan Workout Dialog ──────────────────────────────────────────────────────

@Composable
fun PlanWorkoutDialog(
    selectedDate: Date,
    labels: List<WorkoutLabel>,
    onDismiss: () -> Unit,
    onSave: (name: String, workoutType: String, labelId: Int?) -> Unit
) {
    var workoutName    by remember { mutableStateOf("") }
    var selectedType   by remember { mutableStateOf("GYM") }
    var selectedLabelId by remember { mutableStateOf<Int?>(null) }

    val dateFormat = remember { SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()) }

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
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PLAN WORKOUT", style = LabelCaps, color = MaterialTheme.colorScheme.secondary)
                Text(
                    dateFormat.format(selectedDate),
                    style = BodySemiBold.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = workoutName,
                    onValueChange = { workoutName = it },
                    label = { Text("Workout Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(Modifier.height(14.dp))

                // Workout Type chips
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("GYM", "RUN", "HYROX").forEach { type ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimarySoftBg else MaterialTheme.colorScheme.surface)
                                .border(
                                    BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(type, style = LabelCaps.copy(fontSize = 10.sp), color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                // Label chips (optional)
                if (labels.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "WORKOUT LABEL (OPTIONAL)",
                        style = LabelCaps.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val isNoneSelected = selectedLabelId == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isNoneSelected) PrimarySoftBg else MaterialTheme.colorScheme.surface)
                                .border(
                                    BorderStroke(if (isNoneSelected) 1.5.dp else 1.dp, if (isNoneSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedLabelId = null }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("NONE", style = LabelCaps.copy(fontSize = 9.sp), color = if (isNoneSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                        }
                        labels.forEach { label ->
                            val isSelected = selectedLabelId == label.id
                            val lc = try { Color(android.graphics.Color.parseColor(label.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(lc.copy(alpha = 0.15f))
                                    .border(BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) lc else MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
                                    .clickable { selectedLabelId = label.id }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(label.name.uppercase(), style = LabelCaps.copy(fontSize = 9.sp), color = lc)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", style = LabelCaps, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (workoutName.isNotBlank()) onSave(workoutName, selectedType, selectedLabelId) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("SAVE PLAN", style = LabelCaps, color = Color.White)
                    }
                }
            }
        }
    }
}
