package com.example.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SchoolApplication
import com.example.data.local.entity.StaffUserEntity
import com.example.ui.components.CampusHeader
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsAndAuditScreen(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    modifier: Modifier = Modifier
) {
    val orgId = currentUser.organizationId
    val auditLogs by app.auditRepository.getAuditLogs(orgId).collectAsState(initial = emptyList())
    val allReplacements by app.replacementRepository.getAllReplacements(orgId).collectAsState(initial = emptyList())
    val allStaff by app.staffRepository.getAllStaff(orgId).collectAsState(initial = emptyList())
    val staffMap = remember(allStaff) { allStaff.associateBy { it.id } }
    val allVerifications by app.verificationRepository.getAllVerifications(orgId).collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Operational Analytics, 1: Security Audit Trail

    // Metrics
    val totalSubstitutions = allReplacements.size
    val totalVerifications = allVerifications.size
    val verifiedCount = allVerifications.count { it.status == "VERIFIED" }
    val complianceRate = if (totalVerifications > 0) (verifiedCount * 100) / totalVerifications else 100

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CampusHeader(
            title = "Reports & Audit Trail",
            subtitle = "School operational telemetry, substitution loads, and audit logs."
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Navy800,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Operations Analytics", fontWeight = FontWeight.SemiBold, color = if (selectedTab == 0) Navy900 else AppTextSecondary) },
                icon = { Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selectedTab == 0) Navy900 else AppTextSecondary) },
                selectedContentColor = Navy900,
                unselectedContentColor = AppTextSecondary
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("System Audit Trail (${auditLogs.size})", fontWeight = FontWeight.SemiBold, color = if (selectedTab == 1) Navy900 else AppTextSecondary) },
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selectedTab == 1) Navy900 else AppTextSecondary) },
                selectedContentColor = Navy900,
                unselectedContentColor = AppTextSecondary
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Total Substitutions",
                            value = "$totalSubstitutions",
                            subtitle = "Cumulative duties assigned",
                            icon = Icons.Default.SwapHoriz,
                            iconTint = PurpleDuty,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Room Compliance",
                            value = "$complianceRate%",
                            subtitle = "$verifiedCount of $totalVerifications confirmed",
                            icon = Icons.Default.VerifiedUser,
                            iconTint = EmeraldGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

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
                                text = "Substitution Load by Faculty Member",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )

                            val substitutionCounts = remember(allReplacements) {
                                allReplacements.groupBy { it.assignedTeacherId }
                                    .mapValues { it.value.size }
                                    .toList()
                                    .sortedByDescending { it.second }
                            }

                            if (substitutionCounts.isEmpty()) {
                                Text(
                                    text = "No substitutions recorded yet.",
                                    color = Slate500,
                                    fontSize = 13.sp
                                )
                            } else {
                                substitutionCounts.forEach { (teacherId, count) ->
                                    val t = staffMap[teacherId]
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
                                                text = t?.name ?: teacherId,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Navy900
                                            )
                                            Text(
                                                text = t?.department ?: "Faculty",
                                                fontSize = 11.sp,
                                                color = Slate500
                                            )
                                        }
                                        Surface(
                                            color = BlueSoft,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "$count duty / duties",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = BlueAccent,
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
        } else {
            // Immutable Audit Trail
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(auditLogs) { log ->
                    val dateStr = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Slate100,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = log.action,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Navy900,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(text = dateStr, fontSize = 11.sp, color = Slate400)
                            }

                            Text(
                                text = "Operator: ${log.userName} (${log.userId}) • Resource: ${log.resource}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
                            )

                            if (!log.oldValue.isNullOrBlank()) {
                                Text(
                                    text = "Previous: ${log.oldValue}",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                            if (!log.newValue.isNullOrBlank()) {
                                Text(
                                    text = "New Value: ${log.newValue}",
                                    fontSize = 11.sp,
                                    color = Navy800,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
