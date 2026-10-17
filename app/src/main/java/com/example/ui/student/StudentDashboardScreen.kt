package com.example.ui.student

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.SchoolApplication
import com.example.data.local.entity.InstitutionEntity
import com.example.data.local.entity.InstitutionMembershipEntity
import com.example.data.local.entity.StudentEntity
import com.example.ui.components.CampusHeader
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun StudentDashboardScreen(
    app: SchoolApplication,
    student: StudentEntity,
    institution: InstitutionEntity,
    allMemberships: List<InstitutionMembershipEntity>,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val dayOfWeek = remember { app.timetableRepository.getCurrentDayOfWeek() }
    val periods by app.timetableRepository.getPeriodConfigs(institution.id).collectAsState(initial = emptyList())
    val classTimetable by app.timetableRepository.getTimetableForClass(
        orgId = institution.id,
        grade = student.classGrade,
        section = student.section,
        dayOfWeek = dayOfWeek
    ).collectAsState(initial = emptyList())

    val allStaff by app.staffRepository.getAllStaff(institution.id).collectAsState(initial = emptyList())
    val staffMap = remember(allStaff) { allStaff.associateBy { it.id } }

    val notifications by app.notificationRepository.getNotificationsForStaff(institution.id, "ALL").collectAsState(initial = emptyList())
    val todayVerifications by app.verificationRepository.getAllVerifications(institution.id).collectAsState(initial = emptyList())
    val allInstitutions by app.authRepository.observeAllInstitutions().collectAsState(initial = emptyList())
    val instMap = remember(allInstitutions) { allInstitutions.associateBy { it.id } }

    val currentPeriod = remember(periods) {
        app.timetableRepository.calculateCurrentPeriod(periods, LocalTime.now())
    }

    // Classroom verification status for student's class
    val todayStr = remember { LocalDate.now().toString() }
    val classVerification = remember(todayVerifications, currentPeriod) {
        if (currentPeriod != null) {
            todayVerifications.firstOrNull {
                it.date == todayStr && it.grade == student.classGrade && it.section == student.section && it.periodNumber == currentPeriod.periodNumber
            }
        } else null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Institution Environment Header
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
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
                                    text = "${institution.code} • ${institution.type}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BlueAccentLight
                                )
                            }
                        }

                        Surface(
                            color = Navy900,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "STUDENT PORTAL",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Navy700, thickness = 1.dp)

                    // Multiple Memberships / Institution Switcher (If user belongs to multiple institutions)
                    if (allMemberships.size > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Switch Enrolled Institution (${allMemberships.size} Verified Memberships):",
                                fontSize = 11.sp,
                                color = Slate400,
                                fontWeight = FontWeight.SemiBold
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allMemberships) { membership ->
                                    val isCurrentOrg = membership.organizationId == institution.id
                                    FilterChip(
                                        selected = isCurrentOrg,
                                        onClick = {
                                            if (!isCurrentOrg) {
                                                coroutineScope.launch {
                                                    app.authRepository.switchActiveInstitution(membership.organizationId)
                                                }
                                            }
                                        },
                                        label = {
                                            val instName = instMap[membership.organizationId]?.name ?: if (membership.organizationId == institution.id) institution.name else "Institution ${membership.organizationId}"
                                            Text(
                                                text = instName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BlueAccent,
                                            selectedLabelColor = Color.White,
                                            containerColor = Navy900,
                                            labelColor = Slate300
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("switch_inst_${membership.organizationId}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Student Identity Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = student.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Text(
                                text = "Class ${student.classGrade}-${student.section} • Roll No: ${student.rollNumber}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = BlueAccent
                            )
                        }

                        Surface(
                            color = EmeraldLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ACTIVE ENROLLMENT",
                                color = EmeraldDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Slate100, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Student ID:", fontSize = 12.sp, color = Slate500)
                        Text(text = student.studentId, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Parent / Guardian:", fontSize = 12.sp, color = Slate500)
                        Text(text = "${student.parentName} (${student.parentPhone})", fontSize = 12.sp, color = Slate800)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Live Classroom Presence:", fontSize = 12.sp, color = Slate500)
                        Surface(
                            color = if (classVerification?.status == "VERIFIED") EmeraldLight else BlueSoft,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (classVerification?.status == "VERIFIED") "Verified (Room ${classVerification.typedRoomNo})" else "In Session",
                                color = if (classVerification?.status == "VERIFIED") EmeraldDark else BlueAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Today's Class Timetable
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Class Schedule (${student.classGrade}-${student.section})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE")),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlueAccent
                        )
                    }

                    if (classTimetable.isEmpty()) {
                        Text(
                            text = "No instructional periods scheduled for today.",
                            color = Slate500,
                            fontSize = 13.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            classTimetable.sortedBy { it.periodNumber }.forEach { slot ->
                                val teacher = staffMap[slot.teacherId]
                                val isCurrent = currentPeriod?.periodNumber == slot.periodNumber

                                Surface(
                                    color = if (isCurrent) BlueSoft else Slate50,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isCurrent) 1.5.dp else 1.dp,
                                        color = if (isCurrent) BlueAccent else Slate200
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Period ${slot.periodNumber}: ${slot.subject}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Navy900
                                                )
                                                if (isCurrent) {
                                                    Surface(color = EmeraldLight, shape = RoundedCornerShape(4.dp)) {
                                                        Text("NOW", color = EmeraldDark, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "Faculty: ${teacher?.name ?: "Assigned Faculty"}",
                                                fontSize = 12.sp,
                                                color = Slate600
                                            )
                                        }

                                        Surface(color = Slate100, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = slot.roomNo,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Slate800,
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
        }

        // 4. Institutional Announcements
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Campus Announcements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )

                    if (notifications.isEmpty()) {
                        Text(text = "No active notices.", color = Slate500, fontSize = 13.sp)
                    } else {
                        notifications.take(3).forEach { n ->
                            Surface(
                                color = Slate50,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(text = n.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                    Text(text = n.message, fontSize = 12.sp, color = Slate700)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sign Out
        item {
            Button(
                onClick = {
                    coroutineScope.launch {
                        app.authRepository.signOut()
                        onSignOut()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("student_sign_out_btn")
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out Securely", fontWeight = FontWeight.Bold)
            }
        }
    }
}
