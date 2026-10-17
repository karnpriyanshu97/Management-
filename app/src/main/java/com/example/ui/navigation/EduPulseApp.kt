package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.local.entity.StaffUserEntity
import com.example.model.UserRole
import com.example.ui.auth.OtpVerificationScreen
import com.example.ui.auth.PhoneLoginScreen
import com.example.ui.classes.ClassesDirectoryScreen
import com.example.ui.classes.MyClassEnrollmentScreen
import com.example.ui.components.RoleBadge
import com.example.ui.dashboard.MainDashboardScreen
import com.example.ui.meetings.MeetingManagementScreen
import com.example.ui.notifications.NotificationCenterScreen
import com.example.ui.replacements.ReplacementManagementScreen
import com.example.ui.reports.ReportsAndAuditScreen
import com.example.ui.settings.SchoolSettingsScreen
import com.example.ui.staff.StaffDirectoryScreen
import com.example.ui.student.StudentDashboardScreen
import com.example.ui.theme.*
import com.example.ui.timetable.TimetableScreen
import com.example.ui.verification.ClassroomVerificationScreen
import kotlinx.coroutines.launch

enum class Screen(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    MY_CLASS("My Class", Icons.Default.People),
    TIMETABLE("Timetable", Icons.Default.Schedule),
    CLASSES("Classes", Icons.Default.School),
    REPLACEMENTS("Replacements", Icons.Default.SwapHoriz),
    VERIFICATION("Verify Room", Icons.Default.Verified),
    STAFF("Staff & Loc", Icons.Default.Badge),
    MEETINGS("Meetings", Icons.Default.Groups),
    TEAMS("Teams & Posts", Icons.AutoMirrored.Filled.Assignment),
    NOTIFICATIONS("Alerts", Icons.Default.Notifications),
    REPORTS("Reports", Icons.Default.Insights),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusApp(
    app: SchoolApplication,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by app.authRepository.currentUserFlow.collectAsState(initial = null)
    val currentStudent by app.authRepository.currentStudentFlow.collectAsState(initial = null)
    val activeInstitution by app.authRepository.activeInstitutionFlow.collectAsState(initial = null)
    val activeMembership by app.authRepository.activeMembershipFlow.collectAsState(initial = null)
    val allMemberships by app.authRepository.userMembershipsFlow.collectAsState(initial = emptyList())
    val allInstitutions by app.authRepository.observeAllInstitutions().collectAsState(initial = emptyList())
    val instMap = remember(allInstitutions) { allInstitutions.associateBy { it.id } }

    val activeOrgId = activeInstitution?.id ?: currentUser?.organizationId ?: ""
    val notifications by app.notificationRepository.getNotificationsForStaff(activeOrgId, currentUser?.id ?: "").collectAsState(initial = emptyList())
    val unreadNotifCount = remember(notifications) { notifications.count { !it.isRead } }
    val scope = rememberCoroutineScope()

    // Auth flow state
    var authPhoneNumber by remember { mutableStateOf<String?>(null) }
    var authMaskedPhone by remember { mutableStateOf<String?>(null) }

    // Screen navigation stack
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    // Sub-screen back handling
    if (currentScreen != Screen.DASHBOARD) {
        BackHandler {
            currentScreen = Screen.DASHBOARD
        }
    }

    // 1. Not Authenticated -> Show Phone / OTP Login
    if (currentUser == null && currentStudent == null) {
        if (authPhoneNumber == null) {
            PhoneLoginScreen(
                authRepository = app.authRepository,
                registrationRepository = app.registrationRepository,
                onOtpSent = { phone, masked ->
                    authPhoneNumber = phone
                    authMaskedPhone = masked
                },
                modifier = modifier
            )
        } else {
            OtpVerificationScreen(
                phoneNumber = authPhoneNumber!!,
                maskedPhoneNumber = authMaskedPhone ?: authPhoneNumber!!,
                authRepository = app.authRepository,
                onVerificationSuccess = {
                    authPhoneNumber = null
                    authMaskedPhone = null
                    currentScreen = Screen.DASHBOARD
                },
                onChangeNumber = {
                    val prev = authPhoneNumber
                    authPhoneNumber = null
                    authMaskedPhone = null
                    if (prev != null) {
                        scope.launch { app.authRepository.invalidatePendingSession(prev) }
                    }
                },
                modifier = modifier
            )
        }
        return
    }

    // 2. Student Role Environment (Isolated Student Portal)
    if (currentStudent != null && activeInstitution != null) {
        StudentDashboardScreen(
            app = app,
            student = currentStudent!!,
            institution = activeInstitution!!,
            allMemberships = allMemberships,
            onSignOut = {
                coroutineScope.launch {
                    app.authRepository.signOut()
                    authPhoneNumber = null
                    authMaskedPhone = null
                }
            },
            modifier = modifier
        )
        return
    }

    // 3. Staff / Teacher / Principal Environment
    val user = currentUser ?: return
    val inst = activeInstitution ?: InstitutionEntity(
        id = user.organizationId,
        name = "School Management System",
        code = user.organizationId,
        type = "SCHOOL",
        address = "Campus Operations",
        principalOrDirector = "Administration",
        academicSession = "2026-2027"
    )

    val userRole = UserRole.fromString(user.role)
    val isClassTeacher = activeMembership?.isClassTeacher == true

    // Allowed navigation items for role
    val allowedScreens = remember(userRole, isClassTeacher) {
        when (userRole) {
            UserRole.TEACHER -> {
                val list = mutableListOf(
                    Screen.DASHBOARD,
                    Screen.TIMETABLE
                )
                if (isClassTeacher) {
                    list.add(Screen.MY_CLASS)
                }
                list.addAll(
                    listOf(
                        Screen.CLASSES,
                        Screen.VERIFICATION,
                        Screen.REPLACEMENTS,
                        Screen.MEETINGS,
                        Screen.STAFF,
                        Screen.NOTIFICATIONS,
                        Screen.SETTINGS
                    )
                )
                list
            }
            UserRole.STAFF -> listOf(
                Screen.DASHBOARD,
                Screen.TIMETABLE,
                Screen.CLASSES,
                Screen.STAFF,
                Screen.NOTIFICATIONS,
                Screen.SETTINGS
            )
            else -> listOf(
                Screen.DASHBOARD,
                Screen.MY_CLASS,
                Screen.TIMETABLE,
                Screen.CLASSES,
                Screen.REPLACEMENTS,
                Screen.VERIFICATION,
                Screen.STAFF,
                Screen.MEETINGS,
                Screen.TEAMS,
                Screen.NOTIFICATIONS,
                Screen.REPORTS,
                Screen.SETTINGS
            )
        }
    }

    // Organization switcher dropdown state
    var showOrgDropdown by remember { mutableStateOf(false) }

    // Responsive Phone vs Tablet layout
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Tablet / Desktop Layout: Permanent Navigation Rail / Sidebar
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = Navy900,
                    contentColor = Color.White,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp)
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
                                    contentDescription = "1campus",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1campus",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    allowedScreens.forEach { screen ->
                        NavigationRailItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (screen == Screen.NOTIFICATIONS && unreadNotifCount > 0) {
                                            Badge { Text("$unreadNotifCount") }
                                        }
                                    }
                                ) {
                                    Icon(screen.icon, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title, fontSize = 10.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = Navy700,
                                unselectedIconColor = Slate400,
                                unselectedTextColor = Slate400
                            )
                        )
                    }
                }

                // Main Content
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = currentScreen.title,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                    Text(
                                        text = "${inst.name} (${inst.code})",
                                        fontSize = 11.sp,
                                        color = BlueAccent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            },
                            actions = {
                                // Multi-institution Switcher button if user has multiple memberships
                                if (allMemberships.size > 1) {
                                    Box {
                                        OutlinedButton(
                                            onClick = { showOrgDropdown = true },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("switch_org_btn")
                                        ) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(inst.code, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        DropdownMenu(
                                            expanded = showOrgDropdown,
                                            onDismissRequest = { showOrgDropdown = false }
                                        ) {
                                            allMemberships.forEach { m ->
                                                val orgDisplay = instMap[m.organizationId]?.let { "${it.name} (${it.code})" } ?: "Institution ${m.organizationId}"
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(
                                                                text = orgDisplay,
                                                                fontWeight = FontWeight.Bold,
                                                                color = AppTextPrimary
                                                            )
                                                            Text("Role: ${m.role}", fontSize = 11.sp, color = AppTextMuted)
                                                        }
                                                    },
                                                    onClick = {
                                                        showOrgDropdown = false
                                                        coroutineScope.launch {
                                                            app.authRepository.switchActiveInstitution(m.organizationId)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                RoleBadge(role = user.role)
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(onClick = { currentScreen = Screen.NOTIFICATIONS }) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadNotifCount > 0) {
                                                Badge { Text("$unreadNotifCount") }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Navy900)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Navy800)
                                        .clickable { currentScreen = Screen.SETTINGS },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.name.firstOrNull()?.toString() ?: "U",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        ScreenContent(
                            screen = currentScreen,
                            app = app,
                            currentUser = user,
                            institution = inst,
                            activeMembership = activeMembership,
                            onNavigateTo = { currentScreen = it },
                            onSignOut = {
                                coroutineScope.launch {
                                    app.authRepository.signOut()
                                    authPhoneNumber = null
                                    authMaskedPhone = null
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // Mobile Phone Layout: TopAppBar + Bottom Navigation Bar
            val mobileBottomTabs = remember(userRole, isClassTeacher) {
                if (isClassTeacher) {
                    listOf(
                        Screen.DASHBOARD,
                        Screen.MY_CLASS,
                        Screen.TIMETABLE,
                        Screen.CLASSES,
                        Screen.SETTINGS
                    )
                } else {
                    listOf(
                        Screen.DASHBOARD,
                        Screen.TIMETABLE,
                        Screen.CLASSES,
                        Screen.REPLACEMENTS,
                        Screen.SETTINGS
                    )
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                        .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_1campus_logo),
                                        contentDescription = "1campus",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Column {
                                    Text(
                                        text = inst.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Navy900,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${user.name} • ${inst.code}",
                                        fontSize = 11.sp,
                                        color = Slate500,
                                        maxLines = 1
                                    )
                                }
                            }
                        },
                        actions = {
                            if (allMemberships.size > 1) {
                                Box {
                                    IconButton(onClick = { showOrgDropdown = true }) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Institution", tint = BlueAccent)
                                    }
                                    DropdownMenu(
                                        expanded = showOrgDropdown,
                                        onDismissRequest = { showOrgDropdown = false }
                                    ) {
                                        allMemberships.forEach { m ->
                                            val orgDisplay = instMap[m.organizationId]?.let { "${it.name} (${it.code})" } ?: "Institution ${m.organizationId}"
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = orgDisplay,
                                                            fontWeight = FontWeight.Bold,
                                                            color = AppTextPrimary
                                                        )
                                                        Text("Role: ${m.role}", fontSize = 11.sp, color = AppTextMuted)
                                                    }
                                                },
                                                onClick = {
                                                    showOrgDropdown = false
                                                    coroutineScope.launch {
                                                        app.authRepository.switchActiveInstitution(m.organizationId)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            RoleBadge(role = user.role)
                            IconButton(onClick = { currentScreen = Screen.NOTIFICATIONS }) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotifCount > 0) {
                                            Badge { Text("$unreadNotifCount") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Navy900)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        mobileBottomTabs.forEach { screen ->
                            NavigationBarItem(
                                selected = currentScreen == screen,
                                onClick = { currentScreen = screen },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (screen == Screen.NOTIFICATIONS && unreadNotifCount > 0) {
                                                Badge { Text("$unreadNotifCount") }
                                            }
                                        }
                                    ) {
                                        Icon(screen.icon, contentDescription = screen.title)
                                    }
                                },
                                label = { Text(screen.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Navy900,
                                    selectedTextColor = Navy900,
                                    indicatorColor = BlueSoft,
                                    unselectedIconColor = Slate400,
                                    unselectedTextColor = Slate400
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    ScreenContent(
                        screen = currentScreen,
                        app = app,
                        currentUser = user,
                        institution = inst,
                        activeMembership = activeMembership,
                        onNavigateTo = { currentScreen = it },
                        onSignOut = {
                            coroutineScope.launch {
                                app.authRepository.signOut()
                                authPhoneNumber = null
                                authMaskedPhone = null
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenContent(
    screen: Screen,
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    institution: InstitutionEntity,
    activeMembership: InstitutionMembershipEntity?,
    onNavigateTo: (Screen) -> Unit,
    onSignOut: () -> Unit
) {
    when (screen) {
        Screen.DASHBOARD -> MainDashboardScreen(
            app = app,
            currentUser = currentUser,
            institution = institution,
            activeOrgId = currentUser.organizationId,
            onNavigateToVerification = { onNavigateTo(Screen.VERIFICATION) },
            onNavigateToReplacements = { onNavigateTo(Screen.REPLACEMENTS) },
            onNavigateToMeetings = { onNavigateTo(Screen.MEETINGS) },
            onNavigateToTimetable = { onNavigateTo(Screen.TIMETABLE) }
        )
        Screen.MY_CLASS -> {
            if (activeMembership != null) {
                MyClassEnrollmentScreen(
                    app = app,
                    currentUser = currentUser,
                    currentMembership = activeMembership,
                    activeOrgId = currentUser.organizationId
                )
            } else {
                ClassesDirectoryScreen(app = app, currentUser = currentUser)
            }
        }
        Screen.TIMETABLE -> TimetableScreen(app = app, currentUser = currentUser, activeOrgId = currentUser.organizationId)
        Screen.CLASSES -> ClassesDirectoryScreen(app = app, currentUser = currentUser)
        Screen.REPLACEMENTS -> ReplacementManagementScreen(app = app, currentUser = currentUser)
        Screen.VERIFICATION -> ClassroomVerificationScreen(app = app, currentUser = currentUser)
        Screen.STAFF -> StaffDirectoryScreen(app = app, currentUser = currentUser)
        Screen.MEETINGS -> MeetingManagementScreen(app = app, currentUser = currentUser)
        Screen.TEAMS -> com.example.ui.teams.TeamPostManagementScreen(app = app, currentUser = currentUser)
        Screen.NOTIFICATIONS -> NotificationCenterScreen(app = app, currentUser = currentUser)
        Screen.REPORTS -> ReportsAndAuditScreen(app = app, currentUser = currentUser)
        Screen.SETTINGS -> SchoolSettingsScreen(app = app, currentUser = currentUser, onSignOut = onSignOut)
    }
}
