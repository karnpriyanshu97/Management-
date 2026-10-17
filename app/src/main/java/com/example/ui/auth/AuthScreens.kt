package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.data.repository.InstitutionRegistrationRepository
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.CountryCodeItem
import com.example.ui.components.CountryCodeSelector
import com.example.ui.components.SupportedCountries
import com.example.ui.components.RoleBadge
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

enum class AuthFlowMode {
    LOGIN,
    REGISTER_FORM,
    REGISTER_VERIFY_REP,
    REGISTER_STATUS,
    JOIN_SCHOOL,
    PLATFORM_ADMIN
}

@Composable
fun PhoneLoginScreen(
    authRepository: AuthRepository,
    registrationRepository: InstitutionRegistrationRepository,
    onOtpSent: (normalizedPhone: String, maskedPhone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var flowMode by remember { mutableStateOf(AuthFlowMode.LOGIN) }
    var rawPhone by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(SupportedCountries.findByDialCode("+91")) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showDirectory by remember { mutableStateOf(false) }

    // State for registration sub-flow
    var activeRegId by remember { mutableStateOf<String?>(null) }
    var repPhoneForVerify by remember { mutableStateOf("") }
    var maskedRepPhoneForVerify by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    val primaryInstitution by authRepository.observePrimaryInstitution().collectAsState(initial = null)
    val publicFacultyList by authRepository.observePublicFacultyDirectory().collectAsState(initial = emptyList())

    val schoolTitle = primaryInstitution?.name?.ifBlank { null } ?: "1campus Unified Portal"
    val portalSubtitle = if (primaryInstitution != null && primaryInstitution?.code?.isNotBlank() == true) {
        "${primaryInstitution?.code} • Multi-Institution Operations"
    } else {
        "Multi-Institution Operations"
    }

    when (flowMode) {
        AuthFlowMode.REGISTER_FORM -> {
            SchoolRegistrationScreen(
                registrationRepo = registrationRepository,
                onRegistrationSubmitted = { regId, repPhone, masked ->
                    activeRegId = regId
                    repPhoneForVerify = repPhone
                    maskedRepPhoneForVerify = masked
                    flowMode = AuthFlowMode.REGISTER_VERIFY_REP
                },
                onBackToLogin = { flowMode = AuthFlowMode.LOGIN },
                modifier = modifier
            )
            return
        }
        AuthFlowMode.REGISTER_VERIFY_REP -> {
            RepresentativeVerificationScreen(
                registrationId = activeRegId ?: "",
                phoneNumber = repPhoneForVerify,
                maskedPhoneNumber = maskedRepPhoneForVerify,
                registrationRepo = registrationRepository,
                onVerified = {
                    flowMode = AuthFlowMode.REGISTER_STATUS
                },
                onChangeDetails = {
                    coroutineScope.launch {
                        registrationRepository.invalidatePendingSession(repPhoneForVerify)
                    }
                    flowMode = AuthFlowMode.REGISTER_FORM
                },
                modifier = modifier
            )
            return
        }
        AuthFlowMode.REGISTER_STATUS -> {
            RegistrationStatusScreen(
                registrationId = activeRegId ?: "",
                registrationRepo = registrationRepository,
                onBackToLogin = { flowMode = AuthFlowMode.LOGIN },
                onLoginAsAdmin = { phone ->
                    rawPhone = phone.replace("+91", "").trim()
                    flowMode = AuthFlowMode.LOGIN
                },
                modifier = modifier
            )
            return
        }
        AuthFlowMode.JOIN_SCHOOL -> {
            JoinSchoolScreen(
                registrationRepo = registrationRepository,
                onJoinRequestSubmitted = { _ -> },
                onLoginWithPhone = { _ -> flowMode = AuthFlowMode.LOGIN },
                onBackToLogin = { flowMode = AuthFlowMode.LOGIN },
                modifier = modifier
            )
            return
        }
        AuthFlowMode.PLATFORM_ADMIN -> {
            PlatformAdminConsoleScreen(
                registrationRepo = registrationRepository,
                onBackToLogin = { flowMode = AuthFlowMode.LOGIN },
                modifier = modifier
            )
            return
        }
        AuthFlowMode.LOGIN -> {
            // Render the main login screen with 3 prominent entry options
        }
    }

    fun handleContinue() {
        errorMessage = null
        val fullNumber = "${selectedCountry.dialCode}$rawPhone"
        val normalized = authRepository.normalizePhoneNumber(fullNumber)
        val activity = context.findActivity()
        isLoading = true

        coroutineScope.launch {
            val result = authRepository.sendOtp(normalized, activity = activity)
            isLoading = false
            if (result.isSuccess) {
                val masked = result.getOrNull() ?: authRepository.maskPhoneNumber(normalized)
                onOtpSent(normalized, masked)
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to dispatch SMS verification code"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .border(1.dp, Slate200, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1campus App Crest Logo
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                        .border(1.5.dp, Slate800, RoundedCornerShape(20.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_1campus_logo),
                        contentDescription = "1campus Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "1campus",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Navy900,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = portalSubtitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = BlueAccent,
                        textAlign = TextAlign.Center
                    )
                }

                // 3 CLEARLY DISTINGUISHABLE ENTRY OPTIONS
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Choose Entry Portal",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 2.dp)
                    )

                    // Option A: Faculty & Staff Login (Active)
                    Surface(
                        color = BlueSoft,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, BlueAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Navy800),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("A. Faculty & Staff Login", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 13.sp)
                                Text("Sign in with your registered mobile number", color = Slate600, fontSize = 11.sp)
                            }
                            Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = BlueAccent, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Option B: Register Your School / Coaching Institute
                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { flowMode = AuthFlowMode.REGISTER_FORM }
                            .testTag("entry_option_register_school")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Slate200),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null, tint = Navy900, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("B. Register Your School / Coaching", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 13.sp)
                                Text("New institution onboarding & accreditation", color = Slate500, fontSize = 11.sp)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Option C: Join an Existing School
                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { flowMode = AuthFlowMode.JOIN_SCHOOL }
                            .testTag("entry_option_join_school")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Slate200),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Navy900, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("C. Join an Existing School", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 13.sp)
                                Text("Enter School Code & submit membership request", color = Slate500, fontSize = 11.sp)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                HorizontalDivider(color = Slate200, thickness = 1.dp)

                // Phone Input Field (For Faculty & Staff Login)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Faculty / Staff Registered Mobile",
                        style = MaterialTheme.typography.labelMedium,
                        color = Slate700,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Country Code Selector
                        CountryCodeSelector(
                            selectedCountry = selectedCountry,
                            onCountrySelected = { selectedCountry = it },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        // Number Input
                        OutlinedTextField(
                            value = rawPhone,
                            onValueChange = { input ->
                                if (input.length <= selectedCountry.maxLength && input.all { it.isDigit() }) {
                                    rawPhone = input
                                }
                            },
                            placeholder = { Text("${selectedCountry.minLength}–${selectedCountry.maxLength} digits", color = AppTextPlaceholder) },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Slate600)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { if (rawPhone.length >= selectedCountry.minLength) handleContinue() }),
                            shape = RoundedCornerShape(12.dp),
                            colors = AppTextFieldDefaults.outlinedColors(
                                containerColor = Color.White,
                                textColor = AppTextPrimary,
                                placeholderColor = AppTextPlaceholder,
                                unfocusedBorderColor = AppInputBorder,
                                focusedBorderColor = BlueAccent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("phone_input")
                        )
                    }
                }

                // Error Message Display
                if (errorMessage != null) {
                    Surface(
                        color = CrimsonLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonDark)
                            Text(
                                text = errorMessage!!,
                                color = CrimsonDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Continue Button
                Button(
                    onClick = { handleContinue() },
                    enabled = rawPhone.length >= selectedCountry.minLength && !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy800,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_continue_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Send Verification OTP",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Public Faculty Directory Toggle
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(
                        onClick = { showDirectory = !showDirectory },
                        modifier = Modifier.testTag("toggle_directory_btn")
                    ) {
                        Icon(
                            imageVector = if (showDirectory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showDirectory) "Hide Staff Roster" else "View Registered Faculty Directory",
                            color = BlueAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    AnimatedVisibility(visible = showDirectory) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate100, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Registered Faculty & Staff (Active):",
                                fontSize = 11.sp,
                                color = Slate500,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (publicFacultyList.isEmpty()) {
                                Text(
                                    text = "No active staff members registered in directory.",
                                    fontSize = 12.sp,
                                    color = Slate500,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                publicFacultyList.forEach { staff ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = staff.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                            Text(text = staff.designation, fontSize = 11.sp, color = Slate500)
                                        }
                                        RoleBadge(role = staff.role)
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer for Platform Admin Access
                TextButton(
                    onClick = { flowMode = AuthFlowMode.PLATFORM_ADMIN },
                    modifier = Modifier.testTag("platform_admin_console_btn")
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Platform Administrator Verification Console",
                        color = Slate500,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    maskedPhoneNumber: String,
    authRepository: AuthRepository,
    onVerificationSuccess: () -> Unit,
    onChangeNumber: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var otpInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableIntStateOf(60) }
    var isResending by remember { mutableStateOf(false) }
    var infoNotice by remember { mutableStateOf("A 6-digit verification code has been dispatched via Firebase SMS. Carrier delivery may take up to 60 seconds.") }
    val coroutineScope = rememberCoroutineScope()

    // Invalidate pending session on back press
    BackHandler {
        coroutineScope.launch {
            authRepository.invalidatePendingSession(phoneNumber)
        }
        onChangeNumber()
    }

    // Countdown Timer for Resend
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    fun submitVerification() {
        if (otpInput.length != 6 || isVerifying) return
        errorMessage = null
        isVerifying = true

        coroutineScope.launch {
            val result = authRepository.verifyOtp(phoneNumber, otpInput)
            isVerifying = false
            if (result.isSuccess) {
                onVerificationSuccess()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Verification failed."
            }
        }
    }

    fun handleResend() {
        if (resendCooldown > 0 || isResending) return
        errorMessage = null
        isResending = true
        val activity = context.findActivity()

        coroutineScope.launch {
            val result = authRepository.sendOtp(phoneNumber, activity = activity, forceResend = true)
            isResending = false
            if (result.isSuccess) {
                resendCooldown = 60
                infoNotice = "New Firebase SMS code dispatched to $maskedPhoneNumber"
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to resend code"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .border(1.dp, Slate200, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Back button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                authRepository.invalidatePendingSession(phoneNumber)
                            }
                            onChangeNumber()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Change phone number", tint = Navy900)
                    }
                    Text(
                        text = "SMS Verification",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Navy900,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(40.dp))
                }

                Text(
                    text = "Enter the 6-digit verification code sent via SMS to:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = maskedPhoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Navy900,
                    textAlign = TextAlign.Center
                )

                // Info banner regarding real SMS delivery
                Surface(
                    color = BlueSoft,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(20.dp))
                        Text(
                            text = infoNotice,
                            color = Navy800,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 6-digit OTP Input
                OutlinedTextField(
                    value = otpInput,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            otpInput = input
                            if (input.length == 6) {
                                submitVerification()
                            }
                        }
                    },
                    placeholder = { Text("••••••", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 24.sp, letterSpacing = 8.sp, color = AppTextPlaceholder) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 10.sp,
                        color = Navy900
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitVerification() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = AppTextFieldDefaults.outlinedColors(
                        containerColor = Color.White,
                        textColor = Navy900,
                        placeholderColor = AppTextPlaceholder,
                        unfocusedBorderColor = AppInputBorder,
                        focusedBorderColor = BlueAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .testTag("otp_input")
                )

                // Error Message
                if (errorMessage != null) {
                    Surface(
                        color = CrimsonLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonDark)
                            Text(
                                text = errorMessage!!,
                                color = CrimsonDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Verify Button
                Button(
                    onClick = { submitVerification() },
                    enabled = otpInput.length == 6 && !isVerifying,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy800,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("verify_otp_button")
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Verify & Access Operations",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Resend & Change Number Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onChangeNumber) {
                        Text(text = "Change Number", color = Slate500, fontSize = 13.sp)
                    }

                    if (resendCooldown > 0) {
                        Text(
                            text = "Resend in ${resendCooldown}s",
                            color = Slate400,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        TextButton(
                            onClick = { handleResend() },
                            enabled = !isResending
                        ) {
                            Text(
                                text = if (isResending) "Sending..." else "Resend SMS",
                                color = BlueAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
