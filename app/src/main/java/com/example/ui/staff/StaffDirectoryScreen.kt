package com.example.ui.staff

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
import com.example.data.local.entity.StaffUserEntity
import com.example.data.repository.StaffLiveAvailability
import com.example.model.StaffStatus
import com.example.model.UserRole
import com.example.ui.components.CampusHeader
import com.example.ui.components.RoleBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun StaffDirectoryScreen(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    modifier: Modifier = Modifier
) {
    val orgId = currentUser.organizationId
    val allStaff by app.staffRepository.getAllStaff(orgId).collectAsState(initial = emptyList())
    val periods by app.timetableRepository.getPeriodConfigs(orgId).collectAsState(initial = emptyList())
    val dayOfWeek = remember { app.timetableRepository.getCurrentDayOfWeek() }
    val todayStr = remember { LocalDate.now().toString() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, TEACHER, ADMIN, STAFF, ON_LEAVE
    var editingStaff by remember { mutableStateOf<StaffUserEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val userRole = UserRole.fromString(currentUser.role)
    val canManageStaff = userRole in listOf(UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.PRINCIPAL)

    val currentPeriod = remember(periods) {
        app.timetableRepository.calculateCurrentPeriod(periods, LocalTime.now())
    }

    var staffAvailabilityMap by remember { mutableStateOf<Map<String, StaffLiveAvailability>>(emptyMap()) }
    LaunchedEffect(allStaff, currentPeriod) {
        val map = mutableMapOf<String, StaffLiveAvailability>()
        for (staff in allStaff) {
            val availability = app.staffRepository.evaluateStaffLiveStatus(
                staff = staff,
                dayOfWeek = dayOfWeek,
                currentPeriodNumber = currentPeriod?.periodNumber,
                dateStr = todayStr
            )
            map[staff.id] = availability
        }
        staffAvailabilityMap = map
    }

    val filteredStaff = remember(allStaff, searchQuery, selectedFilter) {
        allStaff.filter { s ->
            val matchesQuery = s.name.contains(searchQuery, ignoreCase = true) ||
                    s.department.contains(searchQuery, ignoreCase = true) ||
                    s.phoneNumber.contains(searchQuery) ||
                    s.id.contains(searchQuery, ignoreCase = true) ||
                    s.subjects.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "TEACHER" -> s.role == "TEACHER"
                "ADMIN" -> s.role in listOf("ADMIN", "PRINCIPAL", "SUPER_ADMIN")
                "STAFF" -> s.role == "STAFF"
                "ON_LEAVE" -> s.isOnLeave
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CampusHeader(
            title = "Staff & Currently Here",
            subtitle = "School-wide operational directory and real-time staff locations.",
            actions = {
                if (canManageStaff) {
                    FilledTonalButton(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_staff_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Staff", fontSize = 12.sp)
                    }
                }
            }
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, department, ID, subject...", color = AppTextPlaceholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate600) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = AppTextFieldDefaults.outlinedColors(
                containerColor = Color.White,
                textColor = AppTextPrimary,
                placeholderColor = AppTextPlaceholder,
                unfocusedBorderColor = AppInputBorder,
                focusedBorderColor = BlueAccent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("staff_search_input")
        )

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL" to "All (${allStaff.size})", "TEACHER" to "Teachers", "ADMIN" to "Admins", "STAFF" to "Staff", "ON_LEAVE" to "On Leave").forEach { (filterKey, label) ->
                val isSel = selectedFilter == filterKey
                FilterChip(
                    selected = isSel,
                    onClick = { selectedFilter = filterKey },
                    label = {
                        Text(
                            label,
                            fontSize = 12.sp,
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
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Staff List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredStaff) { staff ->
                val availability = staffAvailabilityMap[staff.id]

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(14.dp))
                        .then(
                            if (canManageStaff) Modifier.clickable { editingStaff = staff }
                            else Modifier
                        )
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
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(BlueSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = staff.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                                        fontWeight = FontWeight.Bold,
                                        color = Navy800,
                                        fontSize = 14.sp
                                    )
                                }
                                Column {
                                    Text(
                                        text = staff.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                    Text(
                                        text = "${staff.id} • ${staff.department}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500
                                    )
                                }
                            }
                            RoleBadge(role = staff.role)
                        }

                        // Real-time "Currently Here" Location Pill
                        if (availability != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate50)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Currently Here: ${availability.operationalLocation}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Navy900
                                    )
                                }
                                StatusBadge(status = availability.status)
                            }
                        }

                        // Subjects & Contact
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Subjects: ${staff.subjects.take(28)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                            Text(
                                text = staff.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Staff Dialog
    if (editingStaff != null) {
        StaffEditDialog(
            app = app,
            staff = editingStaff!!,
            currentUser = currentUser,
            onDismiss = { editingStaff = null }
        )
    }

    // Create Staff Dialog
    if (showCreateDialog) {
        StaffCreateDialog(
            app = app,
            currentUser = currentUser,
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
fun StaffEditDialog(
    app: SchoolApplication,
    staff: StaffUserEntity,
    currentUser: StaffUserEntity,
    onDismiss: () -> Unit
) {
    var role by remember { mutableStateOf(staff.role) }
    var department by remember { mutableStateOf(staff.department) }
    var subjects by remember { mutableStateOf(staff.subjects) }
    var assignedClasses by remember { mutableStateOf(staff.assignedClasses) }
    var isOnLeave by remember { mutableStateOf(staff.isOnLeave) }
    var isActive by remember { mutableStateOf(staff.isActive) }
    val coroutineScope = rememberCoroutineScope()

    val roles = listOf("TEACHER", "ADMIN", "PRINCIPAL", "STAFF")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Manage Staff: ${staff.name}", fontWeight = FontWeight.Bold, color = Navy900)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Employee ID: ${staff.id} • Phone: ${staff.phoneNumber}", fontSize = 12.sp, color = Slate500)

                // Role Selector
                Text(text = "Assigned Role", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    roles.forEach { r ->
                        val isSel = role == r
                        FilterChip(
                            selected = isSel,
                            onClick = { role = r },
                            label = {
                                Text(
                                    r,
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

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department", color = AppTextSecondary) },
                    singleLine = true,
                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subjects,
                    onValueChange = { subjects = it },
                    label = { Text("Subjects (comma separated)", color = AppTextSecondary) },
                    singleLine = true,
                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = assignedClasses,
                    onValueChange = { assignedClasses = it },
                    label = { Text("Assigned Classes", color = AppTextSecondary) },
                    singleLine = true,
                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                // Toggles for Leave and Active
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mark Absent / On Leave Today", fontSize = 13.sp)
                    Switch(checked = isOnLeave, onCheckedChange = { isOnLeave = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Active (Soft deactivation)", fontSize = 13.sp)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        val updated = staff.copy(
                            role = role,
                            department = department.trim(),
                            subjects = subjects.trim(),
                            assignedClasses = assignedClasses.trim(),
                            isOnLeave = isOnLeave,
                            isActive = isActive
                        )
                        app.staffRepository.updateStaffDetails(updated, currentUser)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy800)
            ) {
                Text("Save Updates")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun StaffCreateDialog(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+91") }
    var role by remember { mutableStateOf("TEACHER") }
    var department by remember { mutableStateOf("Science") }
    var subjects by remember { mutableStateOf("Physics") }
    var empId by remember { mutableStateOf("TCH${(100..999).random()}") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enroll New Staff Member", fontWeight = FontWeight.Bold, color = Navy900) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name", color = AppTextSecondary) },
                    singleLine = true,
                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (+91...)", color = AppTextSecondary) },
                    singleLine = true,
                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = empId,
                    onValueChange = { empId = it },
                    label = { Text("Staff Employee ID", color = AppTextSecondary) },
                    singleLine = true,
                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department", color = AppTextSecondary) },
                    singleLine = true,
                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subjects,
                    onValueChange = { subjects = it },
                    label = { Text("Subjects", color = AppTextSecondary) },
                    singleLine = true,
                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(text = errorText!!, color = CrimsonRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || phone.length < 13) {
                        errorText = "Please enter valid name and 10-digit phone number with +91."
                        return@Button
                    }
                    coroutineScope.launch {
                        val newStaff = StaffUserEntity(
                            id = empId.trim(),
                            name = name.trim(),
                            phoneNumber = phone.trim(),
                            role = role,
                            department = department.trim(),
                            subjects = subjects.trim(),
                            assignedClasses = "Unassigned",
                            permissions = "VERIFY_CLASS,VIEW_TIMETABLE",
                            isActive = true
                        )
                        val res = app.staffRepository.createStaff(newStaff, currentUser)
                        if (res.isSuccess) {
                            onDismiss()
                        } else {
                            errorText = res.exceptionOrNull()?.message
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy800)
            ) {
                Text("Enroll Staff")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
