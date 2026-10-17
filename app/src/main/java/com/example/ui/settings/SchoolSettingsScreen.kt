package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SchoolApplication
import com.example.data.local.entity.StaffUserEntity
import com.example.model.UserRole
import com.example.ui.components.CampusHeader
import com.example.ui.components.RoleBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SchoolSettingsScreen(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var schoolName by remember { mutableStateOf("School Management System") }
    var schoolCode by remember { mutableStateOf("") }
    var academicSession by remember { mutableStateOf("2026-2027") }
    var principalName by remember { mutableStateOf("") }
    var isSavedNotice by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userRole = UserRole.fromString(currentUser.role)
    val canEditSchoolSettings = userRole in listOf(UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.PRINCIPAL)

    val orgId = currentUser.organizationId
    val pendingJoinRequests by app.registrationRepository.observePendingJoinRequestsForOrg(orgId).collectAsState(initial = emptyList())

    LaunchedEffect(orgId) {
        val sName = app.schoolSettingsRepository.getSetting(orgId, "school_name", "School Management System")
        val sCode = app.schoolSettingsRepository.getSetting(orgId, "school_code", "")
        val aSession = app.schoolSettingsRepository.getSetting(orgId, "academic_session", "2026-2027")
        val pName = app.schoolSettingsRepository.getSetting(orgId, "principal_name", "")
        schoolName = sName
        schoolCode = sCode
        academicSession = aSession
        principalName = pName
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
                title = "Settings & Profile",
                subtitle = "Institutional settings, academic parameters, and user credentials."
            )
        }

        // Current User Profile Card
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
                    Text(
                        text = "Authenticated Personnel Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Navy800),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "ID: ${currentUser.id} • ${currentUser.department}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                            Text(
                                text = "Mobile: ${currentUser.phoneNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BlueAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        RoleBadge(role = currentUser.role)
                    }

                    Divider(color = Slate100, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Subjects / Scope:", fontSize = 12.sp, color = Slate500)
                        Text(text = currentUser.subjects, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Assigned Classes:", fontSize = 12.sp, color = Slate500)
                        Text(text = currentUser.assignedClasses, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                    }

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
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out Securely")
                    }
                }
            }
        }

        // Institutional Configuration Card
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
                    Text(
                        text = "Institutional Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )

                    // Persistent Unique School Code Display with Copy Button
                    Surface(
                        color = BlueSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("UNIQUE INSTITUTION SCHOOL CODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BlueAccent)
                                Text(
                                    text = schoolCode.ifBlank { "Generating..." },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Navy900
                                )
                                Text("Share this code with staff and students to identify this institution.", fontSize = 11.sp, color = Slate600)
                            }
                            if (schoolCode.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("School Code", schoolCode))
                                        Toast.makeText(context, "School Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("copy_school_code_btn")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy School Code", tint = BlueAccent)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("School Name", color = AppTextSecondary) },
                        enabled = canEditSchoolSettings,
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(
                            containerColor = Color.White,
                            disabledContainerColor = Slate100,
                            disabledTextColor = AppTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = schoolCode,
                        onValueChange = { schoolCode = it },
                        label = { Text("Affiliation / School Code", color = AppTextSecondary) },
                        enabled = false, // Persistent server-assigned code
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(
                            containerColor = Slate100,
                            disabledContainerColor = Slate100,
                            disabledTextColor = AppTextPrimary,
                            disabledBorderColor = AppInputBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = academicSession,
                        onValueChange = { academicSession = it },
                        label = { Text("Academic Session", color = AppTextSecondary) },
                        enabled = canEditSchoolSettings,
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(
                            containerColor = Color.White,
                            disabledContainerColor = Slate100,
                            disabledTextColor = AppTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = principalName,
                        onValueChange = { principalName = it },
                        label = { Text("Principal Name", color = AppTextSecondary) },
                        enabled = canEditSchoolSettings,
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(
                            containerColor = Color.White,
                            disabledContainerColor = Slate100,
                            disabledTextColor = AppTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isSavedNotice) {
                        Surface(color = EmeraldLight, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Settings successfully updated in school repository.",
                                color = EmeraldDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (canEditSchoolSettings) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    app.schoolSettingsRepository.setSetting(orgId, "school_name", schoolName.trim())
                                    app.schoolSettingsRepository.setSetting(orgId, "school_code", schoolCode.trim())
                                    app.schoolSettingsRepository.setSetting(orgId, "academic_session", academicSession.trim())
                                    app.schoolSettingsRepository.setSetting(orgId, "principal_name", principalName.trim())
                                    isSavedNotice = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Institutional Settings")
                        }
                    }
                }
            }
        }

        // Pending Affiliation & Join Requests (For Administrators)
        if (canEditSchoolSettings && pendingJoinRequests.isNotEmpty()) {
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
                                text = "Pending Affiliation Requests (${pendingJoinRequests.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Surface(
                                color = AmberSoft,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ACTION REQUIRED",
                                    color = AmberDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        pendingJoinRequests.forEach { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Slate100),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = req.fullName, fontWeight = FontWeight.Bold, color = Navy900, fontSize = 14.sp)
                                        RoleBadge(role = req.requestedRole)
                                    }
                                    Text(text = "Mobile: ${req.phoneNumber} • ${req.departmentOrGrade}", color = Slate700, fontSize = 12.sp)
                                    if (req.idNumber.isNotBlank()) {
                                        Text(text = "ID / Roll No: ${req.idNumber}", color = Slate500, fontSize = 11.sp)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    app.registrationRepository.approveJoinRequest(req.id, currentUser.id)
                                                    Toast.makeText(context, "Approved ${req.fullName}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Approve Membership", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    app.registrationRepository.rejectJoinRequest(req.id, currentUser.id, "Rejected by administration.")
                                                    Toast.makeText(context, "Rejected request", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reject", color = CrimsonDark, fontSize = 12.sp)
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
}
