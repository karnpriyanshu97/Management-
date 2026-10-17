package com.example.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.SchoolApplication
import com.example.data.local.entity.InstitutionEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.data.repository.StaffLiveAvailability
import com.example.model.StaffStatus
import com.example.model.UserRole
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun MainDashboardScreen(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    institution: InstitutionEntity,
    activeOrgId: String,
    onNavigateToVerification: () -> Unit,
    onNavigateToReplacements: () -> Unit,
    onNavigateToMeetings: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val periodConfigs by app.timetableRepository.getPeriodConfigs(activeOrgId).collectAsState(initial = emptyList())
    val allStaff by app.staffRepository.getAllStaff(activeOrgId).collectAsState(initial = emptyList())
    val todayStr = remember { LocalDate.now().toString() }
    val replacementsToday by app.replacementRepository.getReplacementsForDate(activeOrgId, todayStr).collectAsState(initial = emptyList())
    val meetings by app.meetingRepository.getAllMeetings(activeOrgId).collectAsState(initial = emptyList())

    val dayOfWeek = remember { app.timetableRepository.getCurrentDayOfWeek() }
    val teacherTimetable by app.timetableRepository.getTimetableForTeacher(activeOrgId, currentUser.id, dayOfWeek).collectAsState(initial = emptyList())

    // Clock ticker that refreshes every 10 seconds for real-time period calculations
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            currentTime = LocalTime.now()
        }
    }

    val currentPeriod = remember(periodConfigs, currentTime) {
        app.timetableRepository.calculateCurrentPeriod(periodConfigs, currentTime)
    }

    // Evaluate live staff statuses
    var staffLiveStatusList by remember { mutableStateOf<List<StaffLiveAvailability>>(emptyList()) }
    LaunchedEffect(allStaff, currentPeriod, currentTime) {
        val list = mutableListOf<StaffLiveAvailability>()
        for (staff in allStaff) {
            val availability = app.staffRepository.evaluateStaffLiveStatus(
                staff = staff,
                dayOfWeek = dayOfWeek,
                currentPeriodNumber = currentPeriod?.periodNumber,
                dateStr = todayStr,
                currentTime = currentTime
            )
            list.add(availability)
        }
        staffLiveStatusList = list
    }

    // Stats calculations
    val totalStaffCount = allStaff.size
    val absentCount = allStaff.count { it.isOnLeave }
    val presentCount = totalStaffCount - absentCount
    val teachingCount = staffLiveStatusList.count { it.status == StaffStatus.TEACHING }
    val freeCount = staffLiveStatusList.count { it.status == StaffStatus.FREE && it.staff.role == "TEACHER" }
    val activeDutyCount = replacementsToday.count { it.status == "APPROVED" }

    // User's current period assignment if teacher
    val currentTeacherClass = remember(teacherTimetable, currentPeriod) {
        if (currentPeriod != null) {
            teacherTimetable.firstOrNull { it.periodNumber == currentPeriod.periodNumber }
        } else null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Operational Live Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Navy800),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Navy700, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_1campus_logo),
                                    contentDescription = "1campus Logo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column {
                                Text(
                                    text = institution.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${institution.code} • ${LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                        }
                        RoleBadge(role = currentUser.role)
                    }

                    HorizontalDivider(color = Navy700, thickness = 1.dp)

                    // Current Period Live Badge
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
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (currentPeriod != null && !currentPeriod.isBreak && !currentPeriod.isLunch) EmeraldGreen else AmberWarning)
                            )
                            Column {
                                val periodTitle = when {
                                    currentPeriod == null -> "Transition / Non-Instructional Hours"
                                    currentPeriod.isBreak -> "Morning Recess"
                                    currentPeriod.isLunch -> "Lunch Break"
                                    else -> currentPeriod.label
                                }
                                Text(
                                    text = periodTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = currentPeriod?.let { "${it.startTime} - ${it.endTime}" } ?: "School Operations Active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BlueAccentLight
                                )
                            }
                        }

                        // Live Clock
                        Surface(
                            color = Navy900,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. High-Level Operations Stat Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Present Faculty",
                        value = "$presentCount / $totalStaffCount",
                        subtitle = if (absentCount == 0) "100% Attendance" else "$absentCount marked absent",
                        icon = Icons.Default.People,
                        iconTint = BlueAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Currently Teaching",
                        value = "$teachingCount",
                        subtitle = "Active in classrooms",
                        icon = Icons.Default.School,
                        iconTint = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Free Standby Teachers",
                        value = "$freeCount",
                        subtitle = "Ready for substitution",
                        icon = Icons.Default.PersonSearch,
                        iconTint = PurpleDuty,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToReplacements
                    )
                    StatCard(
                        title = "Substitutions Today",
                        value = "$activeDutyCount",
                        subtitle = "AI allocated & confirmed",
                        icon = Icons.Default.SwapHoriz,
                        iconTint = AmberWarning,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToReplacements
                    )
                }
            }
        }

        // 3. Role-Specific Action & Assignment Card
        val userRole = UserRole.fromString(currentUser.role)
        if (userRole == UserRole.TEACHER) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "My Current Period Assignment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )

                        if (currentTeacherClass != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Class ${currentTeacherClass.grade}-${currentTeacherClass.section} • ${currentTeacherClass.subject}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "Scheduled Room: ${currentTeacherClass.roomNo}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                                Button(
                                    onClick = onNavigateToVerification,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("verify_room_btn")
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verify Room", fontSize = 13.sp)
                                }
                            }
                        } else {
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
                                    Icon(Icons.Default.FreeBreakfast, contentDescription = null, tint = BlueAccent)
                                    Text(
                                        text = "You have no assigned teaching class for this period in ${institution.name}. Status: Free / Standby.",
                                        fontSize = 13.sp,
                                        color = Navy800
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Principal & Admin Fast Operations Palette
        if (userRole in listOf(UserRole.PRINCIPAL, UserRole.ADMIN, UserRole.SUPER_ADMIN)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Executive Operations Controls (${institution.name})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateToMeetings,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("generate_meeting_action")
                            ) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Meeting", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = onNavigateToReplacements,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("assign_replacement_action")
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Assign Cover", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = onNavigateToTimetable,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("adjust_timings_action")
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Routine", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. Live Staff "Currently Here" Directory Preview
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Staff Location & Status (${institution.name})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = "${staffLiveStatusList.size} Active Personnel",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        staffLiveStatusList.take(6).forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Slate50)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.staff.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "${item.operationalLocation} • ${item.currentActivityDetails}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                StatusBadge(status = item.status)
                            }
                        }
                    }
                }
            }
        }
    }
}
