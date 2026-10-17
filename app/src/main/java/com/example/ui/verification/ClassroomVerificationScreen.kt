package com.example.ui.verification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.local.entity.ClassroomVerificationEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.ui.components.CampusHeader
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date
import java.util.Locale

@Composable
fun ClassroomVerificationScreen(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    modifier: Modifier = Modifier
) {
    val orgId = currentUser.organizationId
    val dayOfWeek = remember { app.timetableRepository.getCurrentDayOfWeek() }
    val periods by app.timetableRepository.getPeriodConfigs(orgId).collectAsState(initial = emptyList())
    val myTimetable by app.timetableRepository.getTimetableForTeacher(orgId, currentUser.id, dayOfWeek).collectAsState(initial = emptyList())
    val allVerifications by app.verificationRepository.getAllVerifications(orgId).collectAsState(initial = emptyList())

    val currentPeriod = remember(periods) {
        app.timetableRepository.calculateCurrentPeriod(periods, LocalTime.now())
    }

    val activeClass = remember(myTimetable, currentPeriod) {
        if (currentPeriod != null) {
            myTimetable.firstOrNull { it.periodNumber == currentPeriod.periodNumber }
        } else myTimetable.firstOrNull()
    }

    var typedRoomNo by remember { mutableStateOf(activeClass?.roomNo ?: "") }
    var photoTaken by remember { mutableStateOf(false) }
    var submissionError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var successRecord by remember { mutableStateOf<ClassroomVerificationEntity?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(activeClass) {
        if (typedRoomNo.isEmpty() && activeClass != null) {
            typedRoomNo = activeClass.roomNo
        }
    }

    fun submitVerification() {
        if (activeClass == null) {
            submissionError = "No scheduled class selected to verify."
            return
        }
        if (typedRoomNo.isBlank()) {
            submissionError = "Typed room number is required."
            return
        }
        submissionError = null
        isSubmitting = true

        coroutineScope.launch {
            val result = app.verificationRepository.submitClassroomVerification(
                orgId = orgId,
                teacher = currentUser,
                grade = activeClass.grade,
                section = activeClass.section,
                periodNumber = activeClass.periodNumber,
                typedRoomNo = typedRoomNo.trim(),
                scheduledRoomNo = activeClass.roomNo,
                photoPath = if (photoTaken) "simulated_camera_room_${typedRoomNo}.jpg" else null
            )
            isSubmitting = false
            if (result.isSuccess) {
                successRecord = result.getOrNull()
            } else {
                submissionError = result.exceptionOrNull()?.message ?: "Verification failed."
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CampusHeader(
                title = "Classroom Verification",
                subtitle = "Confirm physical presence and room location for operational tracking."
            )
        }

        // Active Submission Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Current Class Verification Form",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )

                    if (activeClass != null) {
                        Surface(
                            color = BlueSoft,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Class ${activeClass.grade}-${activeClass.section} • ${activeClass.subject}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                    Text(
                                        text = "Period ${activeClass.periodNumber} • Scheduled in ${activeClass.roomNo}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(BlueAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = Color.White)
                                }
                            }
                        }

                        // Mandatory Typed Room Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Actual Room Number (Mandatory)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            OutlinedTextField(
                                value = typedRoomNo,
                                onValueChange = { typedRoomNo = it },
                                placeholder = { Text("e.g. Room 402 or Lab 3", color = AppTextPlaceholder) },
                                leadingIcon = {
                                    Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = BlueAccent)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("typed_room_input")
                            )
                        }

                        // Photo Capture & OCR Simulated Module
                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (photoTaken) "Room Signboard Photo Attached" else "Signboard Photo / OCR Verification",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (photoTaken) EmeraldDark else Slate800
                                    )
                                    Text(
                                        text = if (photoTaken) "OCR identified: '$typedRoomNo' (Matches door sign)" else "Capture door plaque to auto-verify room barcode",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        photoTaken = !photoTaken
                                        if (photoTaken && typedRoomNo.isBlank()) {
                                            typedRoomNo = activeClass.roomNo
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (photoTaken) Icons.Default.Check else Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (photoTaken) "Retake" else "Capture", fontSize = 12.sp)
                                }
                            }
                        }

                        if (submissionError != null) {
                            Text(text = submissionError!!, color = CrimsonRed, fontSize = 12.sp)
                        }

                        if (successRecord != null) {
                            Surface(
                                color = if (successRecord!!.status == "VERIFIED") EmeraldLight else AmberLight,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (successRecord!!.status == "VERIFIED") Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (successRecord!!.status == "VERIFIED") EmeraldDark else AmberDark
                                    )
                                    Column {
                                        Text(
                                            text = if (successRecord!!.status == "VERIFIED") "Classroom Successfully Verified!" else "Location Mismatch Flagged",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (successRecord!!.status == "VERIFIED") EmeraldDark else AmberDark
                                        )
                                        Text(
                                            text = "Confirmed room: ${successRecord!!.typedRoomNo}. Live school operational status updated.",
                                            fontSize = 11.sp,
                                            color = if (successRecord!!.status == "VERIFIED") EmeraldDark else AmberDark
                                        )
                                    }
                                }
                            }
                        }

                        // Submit CTA
                        Button(
                            onClick = { submitVerification() },
                            enabled = typedRoomNo.isNotBlank() && !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_verification_btn")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Confirm Classroom Presence", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No scheduled class found for you in this period. Verification is enabled during instructional periods.",
                            color = Slate500,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Verification History / School-wide logs
        item {
            Text(
                text = "Recent Campus Verifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Navy900
            )
        }

        items(allVerifications.take(10)) { item ->
            val dateFmt = SimpleDateFormat("h:mm a, d MMM", Locale.getDefault()).format(Date(item.timestamp))
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
                            text = "Class ${item.grade}-${item.section} (Period ${item.periodNumber})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = "Room: ${item.typedRoomNo} (Scheduled: ${item.scheduledRoomNo})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700
                        )
                        Text(text = dateFmt, fontSize = 11.sp, color = Slate400)
                    }

                    Surface(
                        color = if (item.status == "VERIFIED") EmeraldLight else CrimsonLight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (item.status == "VERIFIED") "CONFIRMED" else "MISMATCH",
                            color = if (item.status == "VERIFIED") EmeraldDark else CrimsonDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
