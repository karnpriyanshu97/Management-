package com.example.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SchoolApplication
import com.example.data.local.entity.PeriodConfigEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.model.UserRole
import com.example.ui.components.CampusHeader
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TimetableScreen(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    activeOrgId: String,
    modifier: Modifier = Modifier
) {
    val periods by app.timetableRepository.getPeriodConfigs(activeOrgId).collectAsState(initial = emptyList())
    var selectedDay by remember { mutableIntStateOf(app.timetableRepository.getCurrentDayOfWeek()) }
    val dayTimetable by app.timetableRepository.getTimetableForDay(activeOrgId, selectedDay).collectAsState(initial = emptyList())
    val allStaff by app.staffRepository.getAllStaff(activeOrgId).collectAsState(initial = emptyList())
    val staffMap = remember(allStaff) { allStaff.associateBy { it.id } }

    var editingPeriod by remember { mutableStateOf<PeriodConfigEntity?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Routine Timings, 1: Full Day Schedule

    val userRole = UserRole.fromString(currentUser.role)
    val canEditTimings = userRole in listOf(UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.PRINCIPAL)

    val days = listOf("Monday" to 1, "Tuesday" to 2, "Wednesday" to 3, "Thursday" to 4, "Friday" to 5)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CampusHeader(
            title = "School Routine & Periods",
            subtitle = "Configured bell schedule and instructional periods for current institution."
        )

        // Tab Selector: Routine Timings vs Class Schedule
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Navy800,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Period Configuration", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Daily Schedule", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (selectedTab == 0) {
            // Period Configuration List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    if (canEditTimings) {
                        Surface(
                            color = BlueSoft,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = BlueAccent)
                                Text(
                                    text = "Tap on any period to adjust timings. Timing updates immediately propagate to busy/free tracking, substitutions and classroom verifications.",
                                    fontSize = 12.sp,
                                    color = Navy800
                                )
                            }
                        }
                    }
                }

                items(periods) { period ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Slate200, RoundedCornerShape(14.dp))
                            .then(
                                if (canEditTimings) Modifier.clickable { editingPeriod = period }
                                else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                period.isBreak -> AmberLight
                                                period.isLunch -> AmberLight
                                                else -> BlueSoft
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            period.isBreak -> Icons.Default.FreeBreakfast
                                            period.isLunch -> Icons.Default.Restaurant
                                            else -> Icons.Default.Schedule
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            period.isBreak || period.isLunch -> AmberDark
                                            else -> BlueAccent
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = period.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = when {
                                            period.isBreak -> "Recess (Non-Instructional)"
                                            period.isLunch -> "Lunch Break (Non-Instructional)"
                                            else -> "Instructional Period ${period.periodNumber}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Slate100,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${period.startTime} - ${period.endTime}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Navy900,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                if (canEditTimings) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit timing",
                                        tint = Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Daily Schedule view
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Day selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    days.forEach { (name, dayNum) ->
                        val isSel = selectedDay == dayNum
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedDay = dayNum },
                            label = {
                                Text(
                                    name.take(3),
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSel) AppChipSelectedText else AppChipUnselectedText
                                )
                            },
                            colors = AppFilterChipDefaults.colors(),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSel,
                                borderColor = AppChipUnselectedBorder,
                                selectedBorderColor = Navy800,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(dayTimetable) { entry ->
                        val teacherName = staffMap[entry.teacherId]?.name ?: "Unassigned Faculty"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Class ${entry.grade}-${entry.section} • ${entry.subject}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                    Text(
                                        text = "$teacherName • ${entry.roomNo}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                                Surface(
                                    color = BlueSoft,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Period ${entry.periodNumber}",
                                        color = BlueAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Period Timing Edit Dialog
    if (editingPeriod != null) {
        PeriodEditDialog(
            period = editingPeriod!!,
            onDismiss = { editingPeriod = null },
            onSave = { updatedLabel, start, end, isBreak, isLunch ->
                val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                coroutineScope.launch {
                    app.timetableRepository.updatePeriodTiming(
                        orgId = activeOrgId,
                        periodNumber = editingPeriod!!.periodNumber,
                        label = updatedLabel,
                        startTime = start,
                        endTime = end,
                        isBreak = isBreak,
                        isLunch = isLunch,
                        actor = currentUser
                    )
                    editingPeriod = null
                }
            }
        )
    }
}

@Composable
fun PeriodEditDialog(
    period: PeriodConfigEntity,
    onDismiss: () -> Unit,
    onSave: (label: String, start: String, end: String, isBreak: Boolean, isLunch: Boolean) -> Unit
) {
    var label by remember { mutableStateOf(period.label) }
    var startTime by remember { mutableStateOf(period.startTime) }
    var endTime by remember { mutableStateOf(period.endTime) }
    var isBreak by remember { mutableStateOf(period.isBreak) }
    var isLunch by remember { mutableStateOf(period.isLunch) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit ${period.label} Timing", fontWeight = FontWeight.Bold, color = Navy900)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Period Name / Label", color = AppTextSecondary) },
                    singleLine = true,
                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:mm)", color = AppTextSecondary) },
                        singleLine = true,
                        placeholder = { Text("08:10", color = AppTextPlaceholder) },
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (HH:mm)", color = AppTextSecondary) },
                        singleLine = true,
                        placeholder = { Text("08:50", color = AppTextPlaceholder) },
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isBreak,
                            onCheckedChange = { isBreak = it },
                            colors = AppCheckboxDefaults.colors()
                        )
                        Text("Recess Break", fontSize = 13.sp, color = AppTextPrimary, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isLunch,
                            onCheckedChange = { isLunch = it },
                            colors = AppCheckboxDefaults.colors()
                        )
                        Text("Lunch Break", fontSize = 13.sp, color = AppTextPrimary, fontWeight = FontWeight.Medium)
                    }
                }

                if (errorText != null) {
                    Text(text = errorText!!, color = CrimsonRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isBlank() || startTime.isBlank() || endTime.isBlank()) {
                        errorText = "All timing fields are required."
                        return@Button
                    }
                    onSave(label.trim(), startTime.trim(), endTime.trim(), isBreak, isLunch)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy800)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate500)
            }
        }
    )
}
