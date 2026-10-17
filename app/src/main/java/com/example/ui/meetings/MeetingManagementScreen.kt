package com.example.ui.meetings

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
import com.example.data.local.entity.MeetingEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.model.UserRole
import com.example.ui.components.CampusHeader
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun MeetingManagementScreen(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    modifier: Modifier = Modifier
) {
    val orgId = currentUser.organizationId
    val allMeetings by app.meetingRepository.getAllMeetings(orgId).collectAsState(initial = emptyList())
    val allStaff by app.staffRepository.getAllStaff(orgId).collectAsState(initial = emptyList())
    val staffMap = remember(allStaff) { allStaff.associateBy { it.id } }

    var showGenerateMeetingDialog by remember { mutableStateOf(false) }

    val userRole = UserRole.fromString(currentUser.role)
    val canGenerateMeeting = userRole in listOf(UserRole.PRINCIPAL, UserRole.SUPER_ADMIN, UserRole.ADMIN)

    // Filter meetings: if teacher, show meetings they are participating in or organized; if Principal/Admin, show all
    val relevantMeetings = remember(allMeetings, currentUser) {
        if (canGenerateMeeting) {
            allMeetings
        } else {
            allMeetings.filter { m ->
                m.organizerId == currentUser.id ||
                        m.participantIds.split(",").map { it.trim() }.contains(currentUser.id)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CampusHeader(
            title = "Principal & Faculty Meetings",
            subtitle = "Convened administrative and academic briefings.",
            actions = {
                if (canGenerateMeeting) {
                    Button(
                        onClick = { showGenerateMeetingDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("generate_meeting_btn")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Meeting", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )

        if (relevantMeetings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = Slate400, modifier = Modifier.size(48.dp))
                    Text("No Scheduled Meetings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy900)
                    Text("There are no upcoming meetings for your profile.", color = Slate500, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(relevantMeetings) { meeting ->
                    val isCancelled = meeting.status == "CANCELLED"
                    val participantNames = remember(meeting.participantIds, staffMap) {
                        meeting.participantIds.split(",")
                            .mapNotNull { staffMap[it.trim()]?.name }
                            .joinToString(", ")
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Slate200, RoundedCornerShape(16.dp))
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
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (meeting.priority) {
                                                    "URGENT" -> CrimsonLight
                                                    "HIGH" -> AmberLight
                                                    else -> BlueSoft
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = when (meeting.priority) {
                                                "URGENT" -> CrimsonDark
                                                "HIGH" -> AmberDark
                                                else -> BlueAccent
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = meeting.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCancelled) Slate400 else Navy900
                                        )
                                        Text(
                                            text = "Organized by ${meeting.organizerName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate500
                                        )
                                    }
                                }

                                Surface(
                                    color = if (isCancelled) CrimsonLight else Slate100,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = meeting.status,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isCancelled) CrimsonDark else Slate700,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (meeting.description.isNotBlank()) {
                                Text(
                                    text = meeting.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate700
                                )
                            }

                            // Time & Location Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate50)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "${meeting.date} • ${meeting.startTime} - ${meeting.endTime}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Navy900
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Room, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = meeting.location,
                                        fontSize = 12.sp,
                                        color = Slate700,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Participants
                            Column {
                                Text(text = "Selected Attendees:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                                Text(
                                    text = if (participantNames.isNotBlank()) participantNames else meeting.participantIds,
                                    fontSize = 12.sp,
                                    color = Slate800
                                )
                            }

                            if (!meeting.instructions.isNullOrBlank()) {
                                Text(
                                    text = "Instructions: ${meeting.instructions}",
                                    fontSize = 11.sp,
                                    color = AmberDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Generate Meeting Dialog
    if (showGenerateMeetingDialog) {
        GenerateMeetingDialog(
            app = app,
            currentUser = currentUser,
            staffList = allStaff,
            onDismiss = { showGenerateMeetingDialog = false }
        )
    }
}

@Composable
fun GenerateMeetingDialog(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    staffList: List<StaffUserEntity>,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var startTime by remember { mutableStateOf("14:15") }
    var endTime by remember { mutableStateOf("14:45") }
    var location by remember { mutableStateOf("Principal's Conference Suite (Room 101)") }
    var priority by remember { mutableStateOf("HIGH") }
    var instructions by remember { mutableStateOf("") }

    val selectedStaffIds = remember { mutableStateListOf<String>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPublishing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Generate Principal Meeting", fontWeight = FontWeight.Bold, color = Navy900)
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Meeting Title (e.g. Dispersal Planning)", color = AppTextSecondary) },
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("meeting_title_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Agenda / Purpose", color = AppTextSecondary) },
                        maxLines = 3,
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start (HH:mm)", color = AppTextSecondary) },
                            singleLine = true,
                            colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End (HH:mm)", color = AppTextSecondary) },
                            singleLine = true,
                            colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location / Room", color = AppTextSecondary) },
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(text = "Priority Level", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("NORMAL", "HIGH", "URGENT").forEach { p ->
                            val isSel = priority == p
                            FilterChip(
                                selected = isSel,
                                onClick = { priority = p },
                                label = {
                                    Text(
                                        p,
                                        fontSize = 11.sp,
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
                                )
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Select Specific Teachers/Staff (${selectedStaffIds.size} selected):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary
                    )
                }

                items(staffList.filter { it.id != currentUser.id }) { staff ->
                    val isChecked = selectedStaffIds.contains(staff.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (isChecked) selectedStaffIds.remove(staff.id)
                                else selectedStaffIds.add(staff.id)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { check ->
                                if (check == true) selectedStaffIds.add(staff.id)
                                else selectedStaffIds.remove(staff.id)
                            },
                            colors = AppCheckboxDefaults.colors()
                        )
                        Column {
                            Text(text = staff.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate900)
                            Text(text = "${staff.role} • ${staff.department}", fontSize = 11.sp, color = Slate500)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Special Instructions (Optional)", color = AppTextSecondary) },
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage != null) {
                    item {
                        Text(text = errorMessage!!, color = CrimsonRed, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || selectedStaffIds.isEmpty()) {
                        errorMessage = "Please enter title and select at least one staff member."
                        return@Button
                    }
                    errorMessage = null
                    isPublishing = true
                    coroutineScope.launch {
                        val result = app.meetingRepository.createMeeting(
                            orgId = currentUser.organizationId,
                            title = title.trim(),
                            description = description.trim(),
                            date = date.trim(),
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            location = location.trim(),
                            priority = priority,
                            instructions = instructions.trim().ifEmpty { null },
                            participantIds = selectedStaffIds.toList(),
                            organizer = currentUser
                        )
                        isPublishing = false
                        if (result.isSuccess) {
                            onDismiss()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message
                        }
                    }
                },
                enabled = !isPublishing,
                colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                modifier = Modifier.testTag("publish_meeting_confirm_btn")
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Publish & Notify Attendees")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
