package com.example.ui.replacements

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
import com.example.data.local.entity.PeriodConfigEntity
import com.example.data.local.entity.ReplacementDutyEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.data.service.AiRecommendationResult
import com.example.data.service.ReplacementCandidate
import com.example.model.UserRole
import com.example.ui.components.CampusHeader
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ReplacementManagementScreen(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active Duties, 1: AI Allocator
    val orgId = currentUser.organizationId
    val todayStr = remember { LocalDate.now().toString() }
    val replacements by app.replacementRepository.getReplacementsForDate(orgId, todayStr).collectAsState(initial = emptyList())
    val allStaff by app.staffRepository.getAllStaff(orgId).collectAsState(initial = emptyList())
    val periods by app.timetableRepository.getPeriodConfigs(orgId).collectAsState(initial = emptyList())
    val teachingPeriods = remember(periods) { periods.filter { !it.isBreak && !it.isLunch } }
    val staffMap = remember(allStaff) { allStaff.associateBy { it.id } }

    val userRole = UserRole.fromString(currentUser.role)
    val canAllocate = userRole in listOf(UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.PRINCIPAL)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CampusHeader(
            title = "Teacher Replacement Engine",
            subtitle = "AI-powered conflict-free substitution allocation."
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
                text = { Text("Today's Duties (${replacements.size})", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            if (canAllocate) {
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("AI Allocation Wizard", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        if (selectedTab == 0) {
            // List of today's substitutions
            if (replacements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(48.dp))
                        Text(
                            text = "No Substitutions Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = "All scheduled faculty members are active or classes have no unassigned gaps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(replacements) { duty ->
                        val assignedTeacher = staffMap[duty.assignedTeacherId]
                        val absentTeacher = staffMap[duty.absentTeacherId]

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
                                    Surface(
                                        color = BlueSoft,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Period ${duty.periodNumber} • Class ${duty.grade}-${duty.section}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Navy800,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        color = if (duty.status == "APPROVED") EmeraldLight else AmberLight,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = duty.status,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (duty.status == "APPROVED") EmeraldDark else AmberDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Assigned Substitute:", fontSize = 11.sp, color = Slate500)
                                        Text(
                                            text = assignedTeacher?.name ?: "Unknown",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "Department: ${assignedTeacher?.department ?: "General"}",
                                            fontSize = 12.sp,
                                            color = Slate500
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "Covering for:", fontSize = 11.sp, color = Slate500)
                                        Text(
                                            text = absentTeacher?.name ?: "Unknown",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CrimsonDark
                                        )
                                        Text(text = duty.subject, fontSize = 12.sp, color = Slate500)
                                    }
                                }

                                Divider(color = Slate100, thickness = 1.dp)

                                // AI Justification
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Slate50)
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PurpleDuty, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = duty.aiRecommendationReason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate700
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // AI Allocation Wizard
            AiAllocationWizard(
                app = app,
                currentUser = currentUser,
                staffList = allStaff.filter { it.role == "TEACHER" },
                teachingPeriods = teachingPeriods,
                onReplacementCompleted = {
                    selectedTab = 0
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAllocationWizard(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    staffList: List<StaffUserEntity>,
    teachingPeriods: List<PeriodConfigEntity>,
    onReplacementCompleted: () -> Unit
) {
    var selectedAbsentTeacher by remember { mutableStateOf<StaffUserEntity?>(null) }
    var selectedPeriodNumber by remember { mutableIntStateOf(1) }
    var selectedGrade by remember { mutableStateOf("10") }
    var selectedSection by remember { mutableStateOf("A") }
    var subjectInput by remember { mutableStateOf("English") }

    var isAnalyzing by remember { mutableStateOf(false) }
    var aiResult by remember { mutableStateOf<AiRecommendationResult?>(null) }
    var selectedCandidateToApprove by remember { mutableStateOf<ReplacementCandidate?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var isApproving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val grades = listOf("UKG", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
    val sections = listOf("A", "B", "C", "D")

    fun triggerAiRecommendation() {
        if (selectedAbsentTeacher == null) {
            actionError = "Please select the absent faculty member."
            return
        }
        actionError = null
        isAnalyzing = true
        aiResult = null
        selectedCandidateToApprove = null

        coroutineScope.launch {
            val result = app.replacementRepository.getAiRecommendation(
                orgId = currentUser.organizationId,
                absentTeacherId = selectedAbsentTeacher!!.id,
                grade = selectedGrade,
                section = selectedSection,
                subject = subjectInput,
                periodNumber = selectedPeriodNumber,
                dayOfWeek = app.timetableRepository.getCurrentDayOfWeek()
            )
            isAnalyzing = false
            if (result.isSuccess) {
                val res = result.getOrNull()
                aiResult = res
                selectedCandidateToApprove = res?.topCandidate
            } else {
                actionError = result.exceptionOrNull()?.message ?: "Recommendation engine failed"
            }
        }
    }

    fun confirmAssignment() {
        if (selectedCandidateToApprove == null || selectedAbsentTeacher == null) return
        actionError = null
        isApproving = true

        coroutineScope.launch {
            val res = app.replacementRepository.createAndApproveReplacement(
                orgId = currentUser.organizationId,
                absentTeacherId = selectedAbsentTeacher!!.id,
                assignedTeacherId = selectedCandidateToApprove!!.teacher.id,
                grade = selectedGrade,
                section = selectedSection,
                subject = subjectInput,
                periodNumber = selectedPeriodNumber,
                dayOfWeek = app.timetableRepository.getCurrentDayOfWeek(),
                reason = selectedCandidateToApprove!!.matchReason,
                actor = currentUser
            )
            isApproving = false
            if (res.isSuccess) {
                onReplacementCompleted()
            } else {
                actionError = res.exceptionOrNull()?.message ?: "Assignment could not be completed"
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. Class & Absentee Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )

                    // Select Absent Teacher
                    Text(text = "Absent Teacher", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                    var teacherDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = teacherDropdownExpanded,
                        onExpandedChange = { teacherDropdownExpanded = !teacherDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedAbsentTeacher?.name ?: "Select absent faculty...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherDropdownExpanded) },
                            colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = teacherDropdownExpanded,
                            onDismissRequest = { teacherDropdownExpanded = false }
                        ) {
                            staffList.forEach { teacher ->
                                DropdownMenuItem(
                                    text = { Text("${teacher.name} (${teacher.department})", color = AppTextPrimary, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedAbsentTeacher = teacher
                                        teacherDropdownExpanded = false
                                        // Auto-populate subject
                                        subjectInput = teacher.subjects.split(",").first().trim()
                                    }
                                )
                            }
                        }
                    }

                    // Period, Grade & Section Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Period Selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Period", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                            var periodDropdown by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = periodDropdown,
                                onExpandedChange = { periodDropdown = !periodDropdown }
                            ) {
                                OutlinedTextField(
                                    value = "Period $selectedPeriodNumber",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodDropdown) },
                                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = periodDropdown,
                                    onDismissRequest = { periodDropdown = false }
                                ) {
                                    teachingPeriods.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text(p.label, color = AppTextPrimary, fontWeight = FontWeight.SemiBold) },
                                            onClick = {
                                                selectedPeriodNumber = p.periodNumber
                                                periodDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Grade
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Grade", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                            var gradeDropdown by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = gradeDropdown,
                                onExpandedChange = { gradeDropdown = !gradeDropdown }
                            ) {
                                OutlinedTextField(
                                    value = "Class $selectedGrade",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeDropdown) },
                                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = gradeDropdown,
                                    onDismissRequest = { gradeDropdown = false }
                                ) {
                                    grades.forEach { g ->
                                        DropdownMenuItem(
                                            text = { Text("Class $g", color = AppTextPrimary, fontWeight = FontWeight.SemiBold) },
                                            onClick = {
                                                selectedGrade = g
                                                gradeDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Section
                        Column(modifier = Modifier.weight(0.7f)) {
                            Text(text = "Section", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                            var sectionDropdown by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = sectionDropdown,
                                onExpandedChange = { sectionDropdown = !sectionDropdown }
                            ) {
                                OutlinedTextField(
                                    value = selectedSection,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionDropdown) },
                                    colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = sectionDropdown,
                                    onDismissRequest = { sectionDropdown = false }
                                ) {
                                    sections.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text("Sec $s", color = AppTextPrimary, fontWeight = FontWeight.SemiBold) },
                                            onClick = {
                                                selectedSection = s
                                                sectionDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Subject
                    OutlinedTextField(
                        value = subjectInput,
                        onValueChange = { subjectInput = it },
                        label = { Text("Subject to Cover", color = AppTextSecondary) },
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { triggerAiRecommendation() },
                        enabled = selectedAbsentTeacher != null && !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("run_ai_allocation_btn")
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing Timetables & Workloads...")
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run AI Replacement Evaluation")
                            }
                        }
                    }
                }
            }
        }

        if (actionError != null) {
            item {
                Surface(
                    color = CrimsonLight,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = actionError!!,
                        color = CrimsonDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Recommendations Display
        if (aiResult != null) {
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "2. AI Recommendation & Ranking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )

                        // Natural Language Explanation Banner
                        Surface(
                            color = BlueSoft,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(24.dp))
                                Column {
                                    Text(text = "AI Recommendation Summary", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = aiResult!!.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate800,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        val candidates = listOfNotNull(aiResult!!.topCandidate) + aiResult!!.alternativeCandidates

                        candidates.forEachIndexed { idx, candidate ->
                            val isSelected = selectedCandidateToApprove?.teacher?.id == candidate.teacher.id
                            val isTop = idx == 0

                            Surface(
                                color = if (isSelected) BlueSoft else Slate50,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) BlueAccent else Slate200
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCandidateToApprove = candidate }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = candidate.teacher.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Navy900
                                            )
                                            if (isTop) {
                                                Surface(
                                                    color = EmeraldLight,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "TOP MATCH",
                                                        color = EmeraldDark,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "${candidate.teacher.department} • ${candidate.teacher.subjects}",
                                            fontSize = 12.sp,
                                            color = Slate500
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = candidate.matchReason,
                                            fontSize = 11.sp,
                                            color = Slate700
                                        )
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedCandidateToApprove = candidate },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Navy800,
                                            unselectedColor = Slate500
                                        )
                                    )
                                }
                            }
                        }

                        // Confirmation CTA
                        Button(
                            onClick = { confirmAssignment() },
                            enabled = selectedCandidateToApprove != null && !isApproving,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("approve_replacement_btn")
                        ) {
                            if (isApproving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Approve & Confirm Substitution",
                                        fontWeight = FontWeight.Bold
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
