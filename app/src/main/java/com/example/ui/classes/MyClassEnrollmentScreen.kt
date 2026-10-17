package com.example.ui.classes

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
import com.example.data.local.entity.InstitutionMembershipEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.data.local.entity.StudentEntity
import com.example.model.UserRole
import com.example.ui.components.CampusHeader
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MyClassEnrollmentScreen(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    currentMembership: InstitutionMembershipEntity,
    activeOrgId: String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isClassTeacher = currentMembership.isClassTeacher && !currentMembership.classTeacherGrade.isNullOrBlank()
    val ctGrade = currentMembership.classTeacherGrade ?: "10"
    val ctSection = currentMembership.classTeacherSection ?: "A"

    val userRole = UserRole.fromString(currentMembership.role)
    val isAdmin = userRole in listOf(UserRole.ADMIN, UserRole.PRINCIPAL, UserRole.SUPER_ADMIN)

    var showEnrollDialog by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var actionSuccess by remember { mutableStateOf<String?>(null) }

    // Roster of students in this class
    val classStudents by app.studentRepository.getStudentsForClass(activeOrgId, ctGrade, ctSection).collectAsState(initial = emptyList())
    val allOrgStudents by app.studentRepository.getAllStudentsForOrg(activeOrgId).collectAsState(initial = emptyList())

    val displayedStudents = if (isClassTeacher) classStudents else allOrgStudents

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CampusHeader(
            title = if (isClassTeacher) "My Class Roster (Class $ctGrade-$ctSection)" else "Student Enrollment & Records",
            subtitle = if (isClassTeacher) "Class Teacher enrollment authority and roster management." else "Institutional student directory and enrollment oversight.",
            actions = {
                if (isClassTeacher || isAdmin) {
                    Button(
                        onClick = { showEnrollDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_student_fab")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Student", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )

        // Class Teacher Authority Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate200, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isClassTeacher) EmeraldLight else BlueSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isClassTeacher) Icons.Default.VerifiedUser else Icons.Default.Groups,
                            contentDescription = null,
                            tint = if (isClassTeacher) EmeraldDark else BlueAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isClassTeacher) "Designated Class Teacher" else if (isAdmin) "Administrative Enrollment Control" else "Subject Faculty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = if (isClassTeacher) "Assigned: Class $ctGrade-$ctSection (Enrollment Permitted)"
                            else if (isAdmin) "School-wide student oversight enabled"
                            else "Subject teacher (Class Teacher rights not assigned)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }

                Surface(
                    color = if (isClassTeacher) EmeraldLight else Slate100,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${displayedStudents.size} Enrolled",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isClassTeacher) EmeraldDark else Slate700,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (actionError != null) {
            Surface(
                color = CrimsonLight,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonDark)
                    Text(text = actionError!!, color = CrimsonDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (actionSuccess != null) {
            Surface(
                color = EmeraldLight,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDark)
                    Text(text = actionSuccess!!, color = EmeraldDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Student Roster List
        if (displayedStudents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PersonOff, contentDescription = null, tint = Slate400, modifier = Modifier.size(48.dp))
                    Text("No Students Enrolled", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy900)
                    Text(
                        text = if (isClassTeacher) "Use 'Add Student' above to register students into Class $ctGrade-$ctSection." else "No student records found.",
                        color = Slate500,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedStudents) { student ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Slate200, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Surface(
                                        color = BlueSoft,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Roll ${student.rollNumber}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = BlueAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = student.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Navy900
                                        )
                                        Text(
                                            text = "Class ${student.classGrade}-${student.section} • ID: ${student.studentId}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate500
                                        )
                                    }
                                }

                                Surface(
                                    color = if (student.status == "ACTIVE") EmeraldLight else Slate200,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = student.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (student.status == "ACTIVE") EmeraldDark else Slate700,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Slate100, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Mobile: ${student.phoneNumber}",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                                Text(
                                    text = "Parent: ${student.parentName} (${student.parentPhone})",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Student Enrollment Dialog
    if (showEnrollDialog) {
        StudentEnrollmentDialog(
            app = app,
            currentUser = currentUser,
            currentMembership = currentMembership,
            activeOrgId = activeOrgId,
            defaultGrade = ctGrade,
            defaultSection = ctSection,
            isLockedToClassTeacher = isClassTeacher && !isAdmin,
            onDismiss = { showEnrollDialog = false },
            onSuccess = { enrolledStudent ->
                showEnrollDialog = false
                actionSuccess = "Successfully enrolled ${enrolledStudent.name} (Roll ${enrolledStudent.rollNumber}) in Class ${enrolledStudent.classGrade}-${enrolledStudent.section}."
                actionError = null
            },
            onError = { err ->
                actionError = err
                actionSuccess = null
            }
        )
    }
}

@Composable
fun StudentEnrollmentDialog(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    currentMembership: InstitutionMembershipEntity,
    activeOrgId: String,
    defaultGrade: String,
    defaultSection: String,
    isLockedToClassTeacher: Boolean,
    onDismiss: () -> Unit,
    onSuccess: (StudentEntity) -> Unit,
    onError: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("STU-${defaultGrade}${defaultSection}-${(100..999).random()}") }
    var rollNumber by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf(defaultGrade) }
    var section by remember { mutableStateOf(defaultSection) }
    var studentPhone by remember { mutableStateOf("+91") }
    var parentName by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("+91") }
    var emergencyContact by remember { mutableStateOf("") }

    var localError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Enroll Student into Class $grade-$section", fontWeight = FontWeight.Bold, color = Navy900)
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Student Full Name *", color = AppTextSecondary) },
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("student_name_input")
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = rollNumber,
                            onValueChange = { rollNumber = it },
                            label = { Text("Roll Number *", color = AppTextSecondary) },
                            singleLine = true,
                            colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("student_roll_input")
                        )
                        OutlinedTextField(
                            value = studentId,
                            onValueChange = { studentId = it },
                            label = { Text("Student ID *", color = AppTextSecondary) },
                            singleLine = true,
                            colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = grade,
                            onValueChange = { if (!isLockedToClassTeacher) grade = it },
                            label = { Text("Class / Grade", color = AppTextSecondary) },
                            readOnly = isLockedToClassTeacher,
                            singleLine = true,
                            colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = section,
                            onValueChange = { if (!isLockedToClassTeacher) section = it },
                            label = { Text("Section", color = AppTextSecondary) },
                            readOnly = isLockedToClassTeacher,
                            singleLine = true,
                            colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (isLockedToClassTeacher) {
                        Text(
                            text = "Class & section locked to your authorized Class Teacher assignment.",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = studentPhone,
                        onValueChange = { studentPhone = it },
                        label = { Text("Student Mobile (+91...)", color = AppTextSecondary) },
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("student_phone_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = parentName,
                        onValueChange = { parentName = it },
                        label = { Text("Parent / Guardian Full Name", color = AppTextSecondary) },
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = parentPhone,
                        onValueChange = { parentPhone = it },
                        label = { Text("Parent Phone Number", color = AppTextSecondary) },
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = emergencyContact,
                        onValueChange = { emergencyContact = it },
                        label = { Text("Emergency Contact (Optional)", color = AppTextSecondary) },
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (localError != null) {
                    item {
                        Text(text = localError!!, color = CrimsonRed, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || rollNumber.isBlank() || studentId.isBlank()) {
                        localError = "Name, Roll Number, and Student ID are required."
                        return@Button
                    }
                    localError = null
                    isSubmitting = true
                    coroutineScope.launch {
                        val result = app.studentRepository.enrollStudent(
                            actorUser = currentUser,
                            activeOrgId = activeOrgId,
                            name = name,
                            studentId = studentId,
                            rollNumber = rollNumber,
                            grade = grade,
                            section = section,
                            studentPhone = studentPhone,
                            parentName = parentName,
                            parentPhone = parentPhone,
                            emergencyContact = emergencyContact
                        )
                        isSubmitting = false
                        if (result.isSuccess) {
                            onSuccess(result.getOrNull()!!)
                        } else {
                            val err = result.exceptionOrNull()?.message ?: "Enrollment failed."
                            localError = err
                            onError(err)
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                modifier = Modifier.testTag("confirm_enrollment_btn")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Confirm Enrollment")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
