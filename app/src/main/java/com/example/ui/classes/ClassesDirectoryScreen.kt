package com.example.ui.classes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.local.entity.ClassroomVerificationEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.data.local.entity.TimetableEntity
import com.example.ui.components.CampusHeader
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun ClassesDirectoryScreen(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    modifier: Modifier = Modifier
) {
    val grades = listOf("UKG", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
    var selectedGrade by remember { mutableStateOf("10") }
    var selectedSectionForDetails by remember { mutableStateOf<String?>(null) }

    val orgId = currentUser.organizationId
    val dayOfWeek = remember { app.timetableRepository.getCurrentDayOfWeek() }
    val dayTimetable by app.timetableRepository.getTimetableForDay(orgId, dayOfWeek).collectAsState(initial = emptyList())
    val periods by app.timetableRepository.getPeriodConfigs(orgId).collectAsState(initial = emptyList())
    val allStaff by app.staffRepository.getAllStaff(orgId).collectAsState(initial = emptyList())
    val staffMap = remember(allStaff) { allStaff.associateBy { it.id } }

    val todayStr = remember { LocalDate.now().toString() }
    val verifications by app.verificationRepository.getAllVerifications(orgId).collectAsState(initial = emptyList())
    val todayReplacements by app.replacementRepository.getReplacementsForDate(orgId, todayStr).collectAsState(initial = emptyList())

    val currentPeriod = remember(periods) {
        app.timetableRepository.calculateCurrentPeriod(periods, LocalTime.now())
    }

    // Filter timetable for selected grade
    val gradeTimetable = remember(dayTimetable, selectedGrade) {
        dayTimetable.filter { it.grade == selectedGrade }
    }
    val sectionsInGrade = remember(gradeTimetable) {
        gradeTimetable.map { it.section }.distinct().sorted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CampusHeader(
            title = "Classrooms & Wings",
            subtitle = "Live operational state of every grade and section."
        )

        // Grade Selector Pill Row
        Text(
            text = "Select Academic Grade",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Slate700
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(grades) { grade ->
                val isSelected = grade == selectedGrade
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedGrade = grade
                        selectedSectionForDetails = null
                    },
                    label = {
                        Text(
                            "Class $grade",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) AppChipSelectedText else AppChipUnselectedText
                        )
                    },
                    colors = AppFilterChipDefaults.colors(),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = AppChipUnselectedBorder,
                        selectedBorderColor = Navy800,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.5.dp
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Divider(color = Slate200, thickness = 1.dp)

        // Sections Grid / Card layout
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grade $selectedGrade Sections",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    Text(
                        text = currentPeriod?.let { "Active: ${it.label}" } ?: "Transition Hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = BlueAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (sectionsInGrade.isEmpty()) {
                item {
                    Text(
                        text = "No sections configured for Grade $selectedGrade.",
                        color = Slate500,
                        fontSize = 13.sp
                    )
                }
            }

            items(sectionsInGrade) { section ->
                val currPeriodNum = currentPeriod?.periodNumber ?: 1
                val activeSlot = gradeTimetable.firstOrNull { it.section == section && it.periodNumber == currPeriodNum }
                val teacher = activeSlot?.let { staffMap[it.teacherId] }

                // Check substitution
                val replacement = todayReplacements.firstOrNull {
                    it.grade == selectedGrade && it.section == section && it.periodNumber == currPeriodNum && it.status == "APPROVED"
                }
                val replacementTeacher = replacement?.let { staffMap[it.assignedTeacherId] }

                // Check verification
                val verification = verifications.firstOrNull {
                    it.date == todayStr && it.grade == selectedGrade && it.section == section && it.periodNumber == currPeriodNum
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                        .clickable {
                            selectedSectionForDetails = if (selectedSectionForDetails == section) null else section
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BlueSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$selectedGrade$section",
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900,
                                        fontSize = 13.sp
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Section $selectedGrade-$section",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                    Text(
                                        text = activeSlot?.let { "Subject: ${it.subject}" } ?: "Free / Break",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                            }

                            // Verification Badge
                            Surface(
                                color = when {
                                    verification?.status == "VERIFIED" -> EmeraldLight
                                    verification?.status == "MISMATCH_FLAGGED" -> AmberLight
                                    else -> Slate100
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            verification?.status == "VERIFIED" -> Icons.Default.CheckCircle
                                            verification?.status == "MISMATCH_FLAGGED" -> Icons.Default.Warning
                                            else -> Icons.Default.HourglassEmpty
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            verification?.status == "VERIFIED" -> EmeraldDark
                                            verification?.status == "MISMATCH_FLAGGED" -> AmberDark
                                            else -> Slate500
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = when {
                                            verification?.status == "VERIFIED" -> "VERIFIED"
                                            verification?.status == "MISMATCH_FLAGGED" -> "FLAGGED"
                                            else -> "PENDING"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = when {
                                            verification?.status == "VERIFIED" -> EmeraldDark
                                            verification?.status == "MISMATCH_FLAGGED" -> AmberDark
                                            else -> Slate700
                                        }
                                    )
                                }
                            }
                        }

                        // Details Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate50)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (replacement != null) "Faculty (Substitute):" else "Current Faculty:",
                                    fontSize = 10.sp,
                                    color = if (replacement != null) PurpleDuty else Slate500,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = replacementTeacher?.name ?: teacher?.name ?: "Unassigned",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Slate900
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Room Location:",
                                    fontSize = 10.sp,
                                    color = Slate500,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = verification?.typedRoomNo ?: activeSlot?.roomNo ?: "Not set",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Navy800
                                )
                            }
                        }

                        // Inline full day schedule panel if expanded
                        if (selectedSectionForDetails == section) {
                            Divider(color = Slate200, thickness = 1.dp)
                            Text(
                                text = "Today's Full Timetable for $selectedGrade-$section",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            val sectionSlots = gradeTimetable.filter { it.section == section }.sortedBy { it.periodNumber }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                sectionSlots.forEach { slot ->
                                    val t = staffMap[slot.teacherId]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "P${slot.periodNumber}: ${slot.subject}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Slate800)
                                        Text(text = "${t?.name?.take(16) ?: ""} (${slot.roomNo})", fontSize = 12.sp, color = Slate500)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
