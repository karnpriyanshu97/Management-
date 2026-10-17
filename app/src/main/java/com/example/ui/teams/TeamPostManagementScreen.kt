package com.example.ui.teams

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entity.TeamPostEntity
import com.example.model.UserRole
import com.example.ui.components.CampusHeader
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TeamPostManagementScreen(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    modifier: Modifier = Modifier
) {
    val orgId = currentUser.organizationId
    val teams by app.teamRepository.getAllTeams(orgId).collectAsState(initial = emptyList())
    val allStaff by app.staffRepository.getAllStaff(orgId).collectAsState(initial = emptyList())
    val staffMap = remember(allStaff) { allStaff.associateBy { it.id } }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTeam by remember { mutableStateOf<TeamPostEntity?>(null) }

    val userRole = UserRole.fromString(currentUser.role)
    val canManageTeams = userRole in listOf(UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.PRINCIPAL)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CampusHeader(
            title = "Operational Teams & Posts",
            subtitle = "Organized staff committees and administrative duties.",
            actions = {
                if (canManageTeams) {
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("create_team_post_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Post / Team", fontSize = 12.sp)
                    }
                }
            }
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(teams) { team ->
                val leader = staffMap[team.leaderId]
                val memberNames = remember(team.memberIds, staffMap) {
                    team.memberIds.split(",")
                        .mapNotNull { staffMap[it.trim()]?.name }
                        .joinToString(", ")
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                        .then(
                            if (canManageTeams) Modifier.clickable { editingTeam = team }
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
                            Text(
                                text = team.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )

                            Surface(
                                color = if (team.isActive) EmeraldLight else Slate200,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (team.isActive) "ACTIVE POST" else "INACTIVE",
                                    color = if (team.isActive) EmeraldDark else Slate700,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = team.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate50)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Appointed Team Leader:", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.SemiBold)
                                Text(text = leader?.name ?: "Unassigned", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy800)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Operating Hours:", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.SemiBold)
                                Text(text = team.workingHours, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = "Assigned Members (${team.memberIds.split(",").size}):", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.SemiBold)
                            Text(text = memberNames, fontSize = 12.sp, color = Slate800)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = "Responsibilities & Mandate:", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.SemiBold)
                            Text(text = team.responsibilities, fontSize = 12.sp, color = Slate700)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        TeamCreateDialog(
            app = app,
            currentUser = currentUser,
            staffList = allStaff,
            onDismiss = { showCreateDialog = false }
        )
    }

    if (editingTeam != null) {
        TeamEditDialog(
            app = app,
            currentUser = currentUser,
            team = editingTeam!!,
            staffList = allStaff,
            onDismiss = { editingTeam = null }
        )
    }
}

@Composable
fun TeamCreateDialog(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    staffList: List<StaffUserEntity>,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var leaderId by remember { mutableStateOf(staffList.firstOrNull()?.id ?: "") }
    var workingHours by remember { mutableStateOf("13:45 - 14:30") }
    var responsibilities by remember { mutableStateOf("1. Supervise student movement\n2. Maintain safety standards") }
    val selectedMembers = remember { mutableStateListOf<String>() }
    var errorText by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Operational Team / Post", fontWeight = FontWeight.Bold, color = Navy900) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Post Title (e.g. Dispersal Team)", color = AppTextSecondary) },
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("team_title_input")
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Mission", color = AppTextSecondary) },
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = workingHours,
                        onValueChange = { workingHours = it },
                        label = { Text("Active Duty Timings", color = AppTextSecondary) },
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text(text = "Select Team Members:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
                }
                items(staffList) { staff ->
                    val isChecked = selectedMembers.contains(staff.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedMembers.remove(staff.id)
                                else selectedMembers.add(staff.id)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { check ->
                                if (check == true) selectedMembers.add(staff.id)
                                else selectedMembers.remove(staff.id)
                            },
                            colors = AppCheckboxDefaults.colors()
                        )
                        Text(text = "${staff.name} (${staff.role})", fontSize = 12.sp, color = AppTextPrimary)
                    }
                }
                item {
                    OutlinedTextField(
                        value = responsibilities,
                        onValueChange = { responsibilities = it },
                        label = { Text("Responsibilities Checklist", color = AppTextSecondary) },
                        maxLines = 4,
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (errorText != null) {
                    item { Text(text = errorText!!, color = CrimsonRed, fontSize = 12.sp) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || selectedMembers.isEmpty()) {
                        errorText = "Title and at least one member are required."
                        return@Button
                    }
                    coroutineScope.launch {
                        app.teamRepository.createTeam(
                            orgId = currentUser.organizationId,
                            title = title,
                            description = description,
                            leaderId = leaderId.ifEmpty { selectedMembers.first() },
                            memberIds = selectedMembers.toList(),
                            workingHours = workingHours,
                            responsibilities = responsibilities,
                            actor = currentUser
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy800)
            ) {
                Text("Establish Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TeamEditDialog(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    team: TeamPostEntity,
    staffList: List<StaffUserEntity>,
    onDismiss: () -> Unit
) {
    var isActive by remember { mutableStateOf(team.isActive) }
    var workingHours by remember { mutableStateOf(team.workingHours) }
    var responsibilities by remember { mutableStateOf(team.responsibilities) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage ${team.title}", fontWeight = FontWeight.Bold, color = Navy900) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = workingHours,
                    onValueChange = { workingHours = it },
                    label = { Text("Operating Timings", color = AppTextSecondary) },
                    singleLine = true,
                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = responsibilities,
                    onValueChange = { responsibilities = it },
                    label = { Text("Responsibilities", color = AppTextSecondary) },
                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Team Active", fontSize = 13.sp, color = AppTextPrimary, fontWeight = FontWeight.Medium)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        val updated = team.copy(
                            workingHours = workingHours.trim(),
                            responsibilities = responsibilities.trim(),
                            isActive = isActive
                        )
                        app.teamRepository.updateTeam(updated, currentUser)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy800)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
