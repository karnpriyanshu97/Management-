package com.example.ui.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.InstitutionEntity
import com.example.data.local.entity.InstitutionRegistrationEntity
import com.example.data.repository.InstitutionRegistrationRepository
import com.example.data.repository.RegistrationFormInput
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FormInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    helperText: String? = null,
    modifier: Modifier = Modifier,
    isRequired: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Persistent label clearly visible ABOVE input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Slate800,
                fontSize = 13.sp
            )
            if (isRequired) {
                Text(
                    text = "*",
                    color = CrimsonDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            } else {
                Text(
                    text = "(Optional)",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // Short, helpful description
        if (!helperText.isNullOrBlank()) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        // Outlined text field with example placeholder
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = AppTextPlaceholder,
                    fontSize = 14.sp
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            singleLine = singleLine,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = AppTextFieldDefaults.outlinedColors(
                containerColor = Color.White,
                textColor = AppTextPrimary,
                placeholderColor = AppTextPlaceholder,
                unfocusedBorderColor = AppInputBorder,
                focusedBorderColor = BlueAccent,
                errorTextColor = AppErrorText,
                errorBorderColor = AppInputBorderError
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        )

        // Inline validation error message
        if (isError && !errorMessage.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = CrimsonDark,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = errorMessage,
                    color = CrimsonDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SchoolRegistrationScreen(
    registrationRepo: InstitutionRegistrationRepository,
    onRegistrationSubmitted: (registrationId: String, repPhone: String, maskedPhone: String) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Step state: 1 (Basic Details), 2 (Campus Location), 3 (Official Contacts), 4 (Authorized Rep), 5 (Review Screen)
    var currentStep by remember { mutableIntStateOf(1) }

    // Section 1: Basic Details
    var instType by remember { mutableStateOf("SCHOOL") } // "SCHOOL" or "COACHING"
    var instName by remember { mutableStateOf("") }
    var boardAffiliation by remember { mutableStateOf("CBSE") }
    var boardOtherName by remember { mutableStateOf("") }
    var officialName by remember { mutableStateOf("") }
    var instEmail by remember { mutableStateOf("") }

    // Section 2: Campus Location & Address
    var address by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("India") }

    // Section 3: Official Contacts
    var contactPhone by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }

    // Section 4: Authorized Representative
    var repName by remember { mutableStateOf("") }
    var repDesignation by remember { mutableStateOf("Principal") }
    var repPhone by remember { mutableStateOf("") }
    var repConsentConfirmed by remember { mutableStateOf(false) }

    // UI state
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var draftStatusNotice by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var attemptedStepValidation by remember { mutableStateOf(false) }

    // Draft persistence helper using SharedPreferences
    val draftPrefs = remember { context.getSharedPreferences("edu_school_reg_draft", Context.MODE_PRIVATE) }

    // Load saved draft on initial composition
    LaunchedEffect(Unit) {
        val hasDraft = draftPrefs.getBoolean("has_saved_draft", false)
        if (hasDraft) {
            instType = draftPrefs.getString("inst_type", "SCHOOL") ?: "SCHOOL"
            instName = draftPrefs.getString("inst_name", "") ?: ""
            boardAffiliation = draftPrefs.getString("board_affiliation", if (instType == "COACHING") "Not Applicable" else "CBSE") ?: "CBSE"
            boardOtherName = draftPrefs.getString("board_other", "") ?: ""
            officialName = draftPrefs.getString("official_name", "") ?: ""
            instEmail = draftPrefs.getString("inst_email", "") ?: ""
            address = draftPrefs.getString("address", "") ?: ""
            state = draftPrefs.getString("state", "") ?: ""
            city = draftPrefs.getString("city", "") ?: ""
            postalCode = draftPrefs.getString("postal_code", "") ?: ""
            country = draftPrefs.getString("country", "India") ?: "India"
            contactPhone = draftPrefs.getString("contact_phone", "") ?: ""
            website = draftPrefs.getString("website", "") ?: ""
            logoUrl = draftPrefs.getString("logo_url", "") ?: ""
            repName = draftPrefs.getString("rep_name", "") ?: ""
            repDesignation = draftPrefs.getString("rep_designation", "Principal") ?: "Principal"
            repPhone = draftPrefs.getString("rep_phone", "") ?: ""
            val savedAt = draftPrefs.getLong("saved_timestamp", 0L)
            if (savedAt > 0) {
                draftStatusNotice = "Draft restored from previous session."
            }
        }
    }

    fun saveDraftToLocal(showFeedback: Boolean = true) {
        draftPrefs.edit().apply {
            putBoolean("has_saved_draft", true)
            putString("inst_type", instType)
            putString("inst_name", instName)
            putString("board_affiliation", boardAffiliation)
            putString("board_other", boardOtherName)
            putString("official_name", officialName)
            putString("inst_email", instEmail)
            putString("address", address)
            putString("state", state)
            putString("city", city)
            putString("postal_code", postalCode)
            putString("country", country)
            putString("contact_phone", contactPhone)
            putString("website", website)
            putString("logo_url", logoUrl)
            putString("rep_name", repName)
            putString("rep_designation", repDesignation)
            putString("rep_phone", repPhone)
            putLong("saved_timestamp", System.currentTimeMillis())
            apply()
        }
        if (showFeedback) {
            draftStatusNotice = "Draft saved successfully. You can return anytime."
            Toast.makeText(context, "Registration draft saved.", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearSavedDraft() {
        draftPrefs.edit().clear().apply()
        draftStatusNotice = null
    }

    // Step-specific validation helpers
    val isStep1Valid = remember(instName, instEmail, boardAffiliation, boardOtherName) {
        val validName = instName.trim().length >= 3
        val validEmail = instEmail.trim().matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
        val validBoard = if (boardAffiliation == "Other") boardOtherName.trim().length >= 2 else true
        validName && validEmail && validBoard
    }

    val isStep2Valid = remember(address, city, state, postalCode) {
        val validAddress = address.trim().length >= 5
        val validCity = city.trim().length >= 2
        val validState = state.trim().length >= 2
        val validPin = postalCode.trim().matches(Regex("^\\d{6}$"))
        validAddress && validCity && validState && validPin
    }

    val isStep3Valid = remember(instEmail, contactPhone) {
        val validEmail = instEmail.trim().matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
        val cleanPhone = contactPhone.filter { it.isDigit() }
        val validPhone = cleanPhone.length == 10 && cleanPhone.first() in '6'..'9'
        validEmail && validPhone
    }

    val isStep4Valid = remember(repName, repDesignation, repPhone, repConsentConfirmed) {
        val validName = repName.trim().length >= 2
        val validDesig = repDesignation.trim().length >= 2
        val cleanRepPhone = repPhone.filter { it.isDigit() }
        val validPhone = cleanRepPhone.length == 10 && cleanRepPhone.first() in '6'..'9'
        validName && validDesig && validPhone && repConsentConfirmed
    }

    fun handleContinue() {
        attemptedStepValidation = true
        when (currentStep) {
            1 -> {
                if (isStep1Valid) {
                    saveDraftToLocal(showFeedback = false)
                    attemptedStepValidation = false
                    currentStep = 2
                }
            }
            2 -> {
                if (isStep2Valid) {
                    saveDraftToLocal(showFeedback = false)
                    attemptedStepValidation = false
                    currentStep = 3
                }
            }
            3 -> {
                if (isStep3Valid) {
                    saveDraftToLocal(showFeedback = false)
                    attemptedStepValidation = false
                    currentStep = 4
                }
            }
            4 -> {
                if (isStep4Valid) {
                    saveDraftToLocal(showFeedback = false)
                    attemptedStepValidation = false
                    currentStep = 5
                }
            }
        }
    }

    fun handleSubmitForApproval() {
        errorMessage = null
        if (!isStep1Valid || !isStep2Valid || !isStep3Valid || !isStep4Valid) {
            errorMessage = "Please complete and verify all required fields across all sections before submitting."
            return
        }

        isSubmitting = true
        val resolvedBoard = if (boardAffiliation == "Other") boardOtherName.trim() else boardAffiliation

        coroutineScope.launch {
            val input = RegistrationFormInput(
                institutionName = instName.trim(),
                institutionType = instType,
                officialName = officialName.trim(),
                address = address.trim(),
                city = city.trim(),
                state = state.trim(),
                country = country.ifBlank { "India" }.trim(),
                postalCode = postalCode.trim(),
                contactPhone = contactPhone.trim(),
                contactEmail = instEmail.trim(),
                representativeName = repName.trim(),
                representativeDesignation = repDesignation.trim(),
                representativePhone = repPhone.trim(),
                website = website.trim(),
                logoUrl = logoUrl.trim(),
                boardAffiliation = resolvedBoard
            )

            val result = registrationRepo.submitRegistration(input)
            isSubmitting = false
            if (result.isSuccess) {
                clearSavedDraft()
                val entity = result.getOrNull()!!
                val masked = if (entity.representativePhone.length >= 13) {
                    val pfx = entity.representativePhone.substring(0, 5)
                    val sfx = entity.representativePhone.substring(entity.representativePhone.length - 2)
                    "$pfx •••• ••$sfx"
                } else entity.representativePhone
                onRegistrationSubmitted(entity.id, entity.representativePhone, masked)
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to submit registration request."
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .border(1.dp, Slate200, RoundedCornerShape(24.dp))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Top Navigation Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentStep > 1) {
                                    currentStep -= 1
                                    attemptedStepValidation = false
                                } else {
                                    onBackToLogin()
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (currentStep > 1) "Previous Step" else "Back to Login",
                                tint = Navy900
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Register School / Coaching",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Text(
                                text = "1campus Accreditation & Multi-Institution Setup",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                        // Quick Save Draft Action in Top Bar
                        TextButton(
                            onClick = { saveDraftToLocal(showFeedback = true) },
                            modifier = Modifier.testTag("save_draft_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Draft", color = BlueAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Progress Indicator
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val stepName = when (currentStep) {
                                1 -> "1. Basic Details"
                                2 -> "2. Campus Location"
                                3 -> "3. Official Contacts"
                                4 -> "4. Authorized Rep"
                                5 -> "5. Review & Submit"
                                else -> ""
                            }
                            Text(
                                text = "Step $currentStep of 5: $stepName",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Text(
                                text = "${(currentStep * 20)}% Complete",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { currentStep / 5f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = BlueAccent,
                            trackColor = Slate200
                        )

                        // Interactive step breadcrumb chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(
                                1 to "Basic",
                                2 to "Location",
                                3 to "Contacts",
                                4 to "Representative",
                                5 to "Review"
                            ).forEach { (stepIdx, label) ->
                                val isDone = stepIdx < currentStep
                                val isCurrent = stepIdx == currentStep
                                Surface(
                                    color = when {
                                        isCurrent -> BlueSoft
                                        isDone -> EmeraldSoft
                                        else -> Slate100
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clickable(enabled = stepIdx < currentStep) {
                                            currentStep = stepIdx
                                            attemptedStepValidation = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isDone) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(12.dp))
                                        }
                                        Text(
                                            text = "$stepIdx. $label",
                                            fontSize = 11.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isCurrent -> BlueAccent
                                                isDone -> EmeraldDark
                                                else -> Slate500
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Draft Notice Banner
                if (draftStatusNotice != null) {
                    item {
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
                                Icon(Icons.Default.Info, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(18.dp))
                                Text(
                                    text = draftStatusNotice!!,
                                    fontSize = 12.sp,
                                    color = Navy900,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        clearSavedDraft()
                                        // Reset fields
                                        instName = ""
                                        instEmail = ""
                                        officialName = ""
                                        address = ""
                                        city = ""
                                        state = ""
                                        postalCode = ""
                                        contactPhone = ""
                                        website = ""
                                        logoUrl = ""
                                        repName = ""
                                        repPhone = ""
                                        repConsentConfirmed = false
                                    }
                                ) {
                                    Text("Reset Form", color = CrimsonDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Error Message Banner
                if (errorMessage != null) {
                    item {
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
                                Text(
                                    text = errorMessage!!,
                                    color = CrimsonDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // =================================================================
                // STEP 1: SECTION 1 — BASIC INSTITUTION DETAILS
                // =================================================================
                if (currentStep == 1) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "SECTION 1 — BASIC INSTITUTION DETAILS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Navy900
                            )
                            Text(
                                text = "Provide official institution type, recognized title, affiliation, and administrative contact email.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }

                    // Institution Type
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Institution Type",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800,
                                    fontSize = 13.sp
                                )
                                Text(text = "*", color = CrimsonDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(
                                text = "Select whether your institution is a formal K-12 school or a coaching/tutoring academy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                fontSize = 11.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FilterChip(
                                    selected = instType == "SCHOOL",
                                    onClick = {
                                        instType = "SCHOOL"
                                        if (boardAffiliation == "Not Applicable") boardAffiliation = "CBSE"
                                    },
                                    label = {
                                        Text(
                                            "School / K-12 Academy",
                                            fontWeight = FontWeight.Bold,
                                            color = if (instType == "SCHOOL") AppChipSelectedText else AppChipUnselectedText
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.School,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (instType == "SCHOOL") AppChipSelectedText else Slate700
                                        )
                                    },
                                    colors = AppFilterChipDefaults.colors(),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = instType == "SCHOOL",
                                        borderColor = AppChipUnselectedBorder,
                                        selectedBorderColor = Navy800,
                                        borderWidth = 1.dp,
                                        selectedBorderWidth = 1.5.dp
                                    ),
                                    modifier = Modifier.weight(1f).testTag("select_inst_school")
                                )
                                FilterChip(
                                    selected = instType == "COACHING",
                                    onClick = {
                                        instType = "COACHING"
                                        boardAffiliation = "Not Applicable"
                                    },
                                    label = {
                                        Text(
                                            "Coaching Centre / Institute",
                                            fontWeight = FontWeight.Bold,
                                            color = if (instType == "COACHING") AppChipSelectedText else AppChipUnselectedText
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.AutoStories,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (instType == "COACHING") AppChipSelectedText else Slate700
                                        )
                                    },
                                    colors = AppFilterChipDefaults.colors(),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = instType == "COACHING",
                                        borderColor = AppChipUnselectedBorder,
                                        selectedBorderColor = Navy800,
                                        borderWidth = 1.dp,
                                        selectedBorderWidth = 1.5.dp
                                    ),
                                    modifier = Modifier.weight(1f).testTag("select_inst_coaching")
                                )
                            }
                        }
                    }

                    // Official Institution Name
                    item {
                        FormInputField(
                            label = "Official Institution Name",
                            helperText = "Enter your institution's registered name as recognized on government or trust records.",
                            value = instName,
                            onValueChange = { instName = it },
                            placeholder = "e.g. Enter your institution's registered name",
                            isRequired = true,
                            isError = attemptedStepValidation && instName.trim().length < 3,
                            errorMessage = "Please enter at least 3 characters for the official institution name.",
                            testTag = "reg_inst_name_input"
                        )
                    }

                    // Board / Affiliation
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Board / Affiliation",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800,
                                    fontSize = 13.sp
                                )
                                if (instType == "SCHOOL") {
                                    Text(text = "*", color = CrimsonDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Text(text = "(Optional for Coaching)", color = Slate400, fontSize = 11.sp)
                                }
                            }
                            Text(
                                text = "Select applicable board: CBSE, ICSE, State Board, Other, or Not Applicable. Coaching centres are not forced to select a school board.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                fontSize = 11.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("CBSE", "ICSE", "State Board", "Other", "Not Applicable").forEach { boardOption ->
                                    val isSel = boardAffiliation == boardOption
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { boardAffiliation = boardOption },
                                        label = {
                                            Text(
                                                boardOption,
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
                                        )
                                    )
                                }
                            }

                            if (boardAffiliation == "Other") {
                                FormInputField(
                                    label = "Specify Board / Affiliation Name",
                                    helperText = "Enter curriculum board name (e.g. Cambridge, IB, NIOS).",
                                    value = boardOtherName,
                                    onValueChange = { boardOtherName = it },
                                    placeholder = "e.g. Cambridge Assessment / IB World School",
                                    isRequired = true,
                                    isError = attemptedStepValidation && boardOtherName.trim().length < 2,
                                    errorMessage = "Please specify the board affiliation name."
                                )
                            }
                        }
                    }

                    // Official Legal Entity Name (Optional)
                    item {
                        FormInputField(
                            label = "Official Legal Entity Name (if different)",
                            helperText = "Registered society, trust, or foundation name if different from the institution's public name.",
                            value = officialName,
                            onValueChange = { officialName = it },
                            placeholder = "e.g. Apex Educational Foundation Trust",
                            isRequired = false
                        )
                    }

                    // Official Institution Email
                    item {
                        FormInputField(
                            label = "Official Institution Email",
                            helperText = "An official email address that the institution's management and staff can access.",
                            value = instEmail,
                            onValueChange = { instEmail = it },
                            placeholder = "e.g. admissions@apexacademy.edu.in",
                            isRequired = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                            isError = attemptedStepValidation && !instEmail.trim().matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")),
                            errorMessage = "Please enter a valid institution email address."
                        )
                    }

                    // Step 1 Actions
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBackToLogin,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Text("Cancel", color = Slate700)
                            }
                            Button(
                                onClick = { handleContinue() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(52.dp)
                                    .testTag("continue_step_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Continue to Location", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // =================================================================
                // STEP 2: SECTION 2 — CAMPUS LOCATION & ADDRESS
                // =================================================================
                if (currentStep == 2) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "SECTION 2 — CAMPUS LOCATION & ADDRESS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Navy900
                            )
                            Text(
                                text = "Provide the physical location and postal details of the main campus. Every field is explicitly labeled.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }

                    // Full Street Address
                    item {
                        FormInputField(
                            label = "Full Street Address",
                            helperText = "Physical campus address, plot number, institutional zone, or street.",
                            value = address,
                            onValueChange = { address = it },
                            placeholder = "e.g. Sector 12, Institutional Area, Plot 42",
                            isRequired = true,
                            isError = attemptedStepValidation && address.trim().length < 5,
                            errorMessage = "Please enter a complete campus street address (min 5 characters)."
                        )
                    }

                    // State and District / City
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FormInputField(
                                label = "State",
                                helperText = "State or Union Territory",
                                value = state,
                                onValueChange = { state = it },
                                placeholder = "e.g. Delhi",
                                isRequired = true,
                                isError = attemptedStepValidation && state.trim().length < 2,
                                errorMessage = "Enter valid state.",
                                modifier = Modifier.weight(1f)
                            )
                            FormInputField(
                                label = "District / City",
                                helperText = "City or district name",
                                value = city,
                                onValueChange = { city = it },
                                placeholder = "e.g. New Delhi",
                                isRequired = true,
                                isError = attemptedStepValidation && city.trim().length < 2,
                                errorMessage = "Enter valid city.",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // PIN Code and Country
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FormInputField(
                                label = "PIN Code",
                                helperText = "Exactly 6 digits for Indian postal codes",
                                value = postalCode,
                                onValueChange = { input ->
                                    if (input.length <= 6 && input.all { it.isDigit() }) postalCode = input
                                },
                                placeholder = "e.g. 110075",
                                isRequired = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = attemptedStepValidation && !postalCode.matches(Regex("^\\d{6}$")),
                                errorMessage = "Must be exactly 6 digits.",
                                modifier = Modifier.weight(1f)
                            )
                            FormInputField(
                                label = "Country",
                                helperText = "Default is India; other countries supported",
                                value = country,
                                onValueChange = { country = it },
                                placeholder = "India",
                                isRequired = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Step 2 Actions
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentStep = 1
                                    attemptedStepValidation = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Back", color = Slate700)
                                }
                            }
                            Button(
                                onClick = { handleContinue() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(52.dp)
                                    .testTag("continue_step_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Continue to Contacts", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // =================================================================
                // STEP 3: SECTION 3 — OFFICIAL INSTITUTION CONTACTS
                // =================================================================
                if (currentStep == 3) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "SECTION 3 — OFFICIAL INSTITUTION CONTACTS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Navy900
                            )
                            Text(
                                text = "Institutional public contacts for inquiries and accreditation notices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }

                    // Prominent Explanation Banner distinguishing Public Phone vs Rep Personal Phone
                    item {
                        Surface(
                            color = BlueSoft,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BlueAccent.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(22.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Official Institution Phone vs Representative Verification Number",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Navy900
                                    )
                                    Text(
                                        text = "The Official Institution Phone Number entered here is the public contact number displayed for institutional correspondence and parent inquiries. In the next step, you will enter the Authorized Representative's personal mobile number, which receives the secure SMS OTP for identity verification.",
                                        fontSize = 11.sp,
                                        color = Slate700,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Official Contact Email
                    item {
                        FormInputField(
                            label = "Official Contact Email",
                            helperText = "The official institutional email monitored by administrative leadership.",
                            value = instEmail,
                            onValueChange = { instEmail = it },
                            placeholder = "e.g. admissions@apexacademy.edu.in",
                            isRequired = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            isError = attemptedStepValidation && !instEmail.trim().matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")),
                            errorMessage = "Please enter a valid official contact email address."
                        )
                    }

                    // Official Institution Phone Number with clearly visible +91 country code
                    item {
                        val cleanPhone = contactPhone.filter { it.isDigit() }
                        val isPhoneValid = cleanPhone.length == 10 && cleanPhone.first() in '6'..'9'
                        FormInputField(
                            label = "Official Institution Phone Number",
                            helperText = "Office desk or main reception telephone with +91 country code.",
                            value = contactPhone,
                            onValueChange = { input ->
                                if (input.length <= 10 && input.all { it.isDigit() }) contactPhone = input
                            },
                            placeholder = "e.g. 9811122233 (10 digits)",
                            leadingIcon = {
                                Surface(
                                    color = Slate100,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = "+91",
                                        color = Navy900,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            },
                            isRequired = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = attemptedStepValidation && !isPhoneValid,
                            errorMessage = "Please enter a valid 10-digit Indian phone number starting with 6, 7, 8, or 9."
                        )
                    }

                    // Official Website (Optional)
                    item {
                        FormInputField(
                            label = "Official Website",
                            helperText = "Institution public web portal (optional).",
                            value = website,
                            onValueChange = { website = it },
                            placeholder = "e.g. https://apexacademy.edu.in",
                            isRequired = false
                        )
                    }

                    // Official Logo URL (Optional)
                    item {
                        FormInputField(
                            label = "Official Logo URL",
                            helperText = "Web link to high-resolution campus crest or emblem (optional).",
                            value = logoUrl,
                            onValueChange = { logoUrl = it },
                            placeholder = "e.g. https://apexacademy.edu.in/logo.png",
                            isRequired = false
                        )
                    }

                    // Step 3 Actions
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentStep = 2
                                    attemptedStepValidation = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Back", color = Slate700)
                                }
                            }
                            Button(
                                onClick = { handleContinue() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(52.dp)
                                    .testTag("continue_step_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Continue to Representative", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // =================================================================
                // STEP 4: SECTION 4 — AUTHORIZED REPRESENTATIVE
                // =================================================================
                if (currentStep == 4) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "SECTION 4 — AUTHORIZED REPRESENTATIVE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Navy900
                            )
                            Text(
                                text = "Designate the institutional official who will complete SMS OTP verification and lead administrative onboarding.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }

                    // Verification Policy Notice Banner
                    item {
                        Surface(
                            color = AmberSoft,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberDark.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = AmberDark, modifier = Modifier.size(22.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Representative Authorization Notice",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Navy900
                                    )
                                    Text(
                                        text = "This person must be authorized to register this institution and complete verification. An instant SMS OTP code will be sent to this mobile number. Phone verification verifies identity, but platform administrator approval is required before school-owner privileges are activated.",
                                        fontSize = 11.sp,
                                        color = Slate800,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Representative Full Name
                    item {
                        FormInputField(
                            label = "Authorized Representative Full Name",
                            helperText = "Legal name of the designated representative.",
                            value = repName,
                            onValueChange = { repName = it },
                            placeholder = "e.g. Dr. Ramesh Chander",
                            isRequired = true,
                            isError = attemptedStepValidation && repName.trim().length < 2,
                            errorMessage = "Please enter the authorized representative's full legal name.",
                            testTag = "reg_rep_name_input"
                        )
                    }

                    // Representative Designation
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FormInputField(
                                label = "Designation / Role",
                                helperText = "Official role held within the institution.",
                                value = repDesignation,
                                onValueChange = { repDesignation = it },
                                placeholder = "e.g. Principal / Director / Managing Trustee",
                                isRequired = true,
                                isError = attemptedStepValidation && repDesignation.trim().length < 2,
                                errorMessage = "Please enter the representative's official designation."
                            )
                            // Quick designation suggestion chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Principal", "Director", "Owner", "Managing Trustee", "Authorized Representative").forEach { desig ->
                                    val isSel = repDesignation == desig
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { repDesignation = desig },
                                        label = {
                                            Text(
                                                desig,
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
                    }

                    // Representative Mobile Number with visible +91
                    item {
                        val cleanRepPhone = repPhone.filter { it.isDigit() }
                        val isRepPhoneValid = cleanRepPhone.length == 10 && cleanRepPhone.first() in '6'..'9'
                        FormInputField(
                            label = "Representative Mobile Number (for SMS OTP)",
                            helperText = "The personal mobile number that will receive the real SMS OTP for identity verification.",
                            value = repPhone,
                            onValueChange = { input ->
                                if (input.length <= 10 && input.all { it.isDigit() }) repPhone = input
                            },
                            placeholder = "e.g. 9876543210 (10 digits)",
                            leadingIcon = {
                                Surface(
                                    color = Slate100,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = "+91",
                                        color = Navy900,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            },
                            isRequired = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = attemptedStepValidation && !isRepPhoneValid,
                            errorMessage = "Please enter a valid 10-digit mobile number starting with 6, 7, 8, or 9.",
                            testTag = "reg_rep_phone_input"
                        )
                    }

                    // Consent & Authorization Confirmation
                    item {
                        Surface(
                            color = Slate50,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (attemptedStepValidation && !repConsentConfirmed) CrimsonDark else Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { repConsentConfirmed = !repConsentConfirmed }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = repConsentConfirmed,
                                    onCheckedChange = { repConsentConfirmed = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Navy800,
                                        uncheckedColor = AppInputBorder,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Column {
                                    Text(
                                        text = "Institutional Authority & Truthfulness Declaration *",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Navy900
                                    )
                                    Text(
                                        text = "I certify that I am legally authorized by the governing board or management of this institution to submit this application for 1campus accreditation, and all details submitted are accurate and verifiable.",
                                        fontSize = 11.sp,
                                        color = Slate600,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                        if (attemptedStepValidation && !repConsentConfirmed) {
                            Text(
                                text = "You must confirm institutional authorization before continuing.",
                                color = CrimsonDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    // Step 4 Actions
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentStep = 3
                                    attemptedStepValidation = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Back", color = Slate700)
                                }
                            }
                            Button(
                                onClick = { handleContinue() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(52.dp)
                                    .testTag("continue_step_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Review Registration", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // =================================================================
                // STEP 5: REVIEW SCREEN & FINAL SUBMISSION
                // =================================================================
                if (currentStep == 5) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "REVIEW REGISTRATION DETAILS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Navy900
                            )
                            Text(
                                text = "Carefully review all entered institution details before final submission. Click Edit on any section to make adjustments.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }

                    // Summary Card 1: Basic Details
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate50),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Section 1: Basic Institution Details", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 13.sp)
                                    TextButton(onClick = { currentStep = 1 }) {
                                        Text("Edit", color = BlueAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                Text(text = "Type: ${if (instType == "SCHOOL") "K-12 School / Academy" else "Coaching Centre"}", fontSize = 12.sp, color = Slate700)
                                Text(text = "Institution Name: $instName", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                Text(text = "Board / Affiliation: ${if (boardAffiliation == "Other") boardOtherName else boardAffiliation}", fontSize = 12.sp, color = Slate700)
                                if (officialName.isNotBlank()) {
                                    Text(text = "Legal Entity: $officialName", fontSize = 12.sp, color = Slate700)
                                }
                                Text(text = "Official Email: $instEmail", fontSize = 12.sp, color = Slate700)
                            }
                        }
                    }

                    // Summary Card 2: Campus Location
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate50),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Section 2: Campus Location & Address", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 13.sp)
                                    TextButton(onClick = { currentStep = 2 }) {
                                        Text("Edit", color = BlueAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                Text(text = "Address: $address", fontSize = 12.sp, color = Slate700)
                                Text(text = "City / District: $city", fontSize = 12.sp, color = Slate700)
                                Text(text = "State: $state • PIN Code: $postalCode", fontSize = 12.sp, color = Slate700)
                                Text(text = "Country: $country", fontSize = 12.sp, color = Slate700)
                            }
                        }
                    }

                    // Summary Card 3: Contacts
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate50),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Section 3: Official Institution Contacts", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 13.sp)
                                    TextButton(onClick = { currentStep = 3 }) {
                                        Text("Edit", color = BlueAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                Text(text = "Public Phone: +91 $contactPhone", fontSize = 12.sp, color = Slate700)
                                Text(text = "Contact Email: $instEmail", fontSize = 12.sp, color = Slate700)
                                if (website.isNotBlank()) Text(text = "Website: $website", fontSize = 12.sp, color = Slate700)
                                if (logoUrl.isNotBlank()) Text(text = "Logo Asset: $logoUrl", fontSize = 12.sp, color = Slate700)
                            }
                        }
                    }

                    // Summary Card 4: Authorized Representative
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate50),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Section 4: Authorized Representative", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 13.sp)
                                    TextButton(onClick = { currentStep = 4 }) {
                                        Text("Edit", color = BlueAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                Text(text = "Full Name: $repName", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                Text(text = "Designation: $repDesignation", fontSize = 12.sp, color = Slate700)
                                Text(text = "SMS OTP Verification Phone: +91 $repPhone", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlueAccent)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(14.dp))
                                    Text(text = "Authority & Truthfulness Declaration Confirmed", fontSize = 11.sp, color = EmeraldDark, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Final Submission Warning / Explanation
                    item {
                        Surface(
                            color = BlueSoft,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(22.dp))
                                Text(
                                    text = "Upon clicking 'Submit for Approval', a real 6-digit SMS OTP will be dispatched to +91 $repPhone. After phone verification, platform administrators will review institutional accreditation before issuing the unique persistent School Code.",
                                    fontSize = 11.sp,
                                    color = Navy900,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Step 5 Actions: Save Draft and Submit for Approval
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { handleSubmitForApproval() },
                                enabled = !isSubmitting,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Navy800,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("submit_registration_button")
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Submit for Approval & Verify Phone", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        currentStep = 4
                                        attemptedStepValidation = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Back to Edit", color = Slate700)
                                }
                                OutlinedButton(
                                    onClick = { saveDraftToLocal(showFeedback = true) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(16.dp))
                                        Text("Save Draft", color = BlueAccent, fontWeight = FontWeight.SemiBold)
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

@Composable
fun RepresentativeVerificationScreen(
    registrationId: String,
    phoneNumber: String,
    maskedPhoneNumber: String,
    registrationRepo: InstitutionRegistrationRepository,
    onVerified: () -> Unit,
    onChangeDetails: () -> Unit,
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

    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    fun submitVerification() {
        if (otpInput.length != 6) return
        errorMessage = null
        isVerifying = true

        coroutineScope.launch {
            val result = registrationRepo.verifyRepresentativePhone(registrationId, otpInput)
            isVerifying = false
            if (result.isSuccess) {
                onVerified()
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
            val result = registrationRepo.resendRepresentativeOtp(registrationId, activity = activity)
            isResending = false
            if (result.isSuccess) {
                resendCooldown = 60
                infoNotice = "New Firebase SMS code dispatched to $maskedPhoneNumber"
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to resend OTP"
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onChangeDetails) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Navy900)
                    }
                    Text(
                        text = "Verify Representative",
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
                            if (input.length == 6) submitVerification()
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
                        .testTag("rep_otp_input")
                )

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

                Button(
                    onClick = { submitVerification() },
                    enabled = otpInput.length == 6 && !isVerifying,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("rep_verify_otp_button")
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    } else {
                        Text("Confirm Phone Verification", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onChangeDetails) {
                        Text(text = "Change Number", color = Slate500, fontSize = 13.sp)
                    }

                    if (resendCooldown > 0) {
                        Text(text = "Resend in ${resendCooldown}s", color = Slate400, fontSize = 13.sp)
                    } else {
                        TextButton(onClick = { handleResend() }, enabled = !isResending) {
                            Text(text = if (isResending) "Sending..." else "Resend SMS", color = BlueAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RegistrationStatusScreen(
    registrationId: String,
    registrationRepo: InstitutionRegistrationRepository,
    onBackToLogin: () -> Unit,
    onLoginAsAdmin: (phone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val registration by registrationRepo.observeRegistrationById(registrationId).collectAsState(initial = null)
    val context = LocalContext.current

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
                .widthIn(max = 520.dp)
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
                // Crest Logo
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.5.dp, Slate800, RoundedCornerShape(16.dp))
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_1campus_logo),
                        contentDescription = "1campus Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = "Application Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )

                if (registration == null) {
                    CircularProgressIndicator(color = Navy800)
                } else {
                    val reg = registration!!

                    // Reference Badge
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Reference: ${reg.id}",
                            fontWeight = FontWeight.Bold,
                            color = Slate700,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // Status specific banner & details
                    when (reg.status) {
                        "PENDING_APPROVAL" -> {
                            Surface(
                                color = AmberSoft,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = AmberDark)
                                        Text(
                                            text = "Awaiting Platform Verification",
                                            fontWeight = FontWeight.Bold,
                                            color = AmberDark,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Text(
                                        text = "Representative phone verified. In accordance with zero-trust institutional standards, school ownership is not granted solely upon form submission. Platform administrators are reviewing institutional accreditation.",
                                        fontSize = 12.sp,
                                        color = Navy900,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                        "APPROVED" -> {
                            Surface(
                                color = EmeraldSoft,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDark)
                                        Text(
                                            text = "Institution Approved & Active",
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldDark,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Text(
                                        text = "Unique persistent School Code generated and registered. You may now log in with the representative mobile number to access School Admin onboarding.",
                                        fontSize = 12.sp,
                                        color = Navy900
                                    )
                                }
                            }

                            // Persistent School Code Box
                            if (reg.assignedSchoolCode != null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Slate100),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("OFFICIAL SCHOOL CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                            Text(
                                                text = reg.assignedSchoolCode!!,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Navy900
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("School Code", reg.assignedSchoolCode))
                                                Toast.makeText(context, "School Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy School Code", tint = BlueAccent)
                                        }
                                    }
                                }
                            }
                        }
                        "REJECTED" -> {
                            Surface(
                                color = CrimsonLight,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Cancel, contentDescription = null, tint = CrimsonDark)
                                        Text(
                                            text = "Registration Rejected",
                                            fontWeight = FontWeight.Bold,
                                            color = CrimsonDark,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Text(
                                        text = reg.rejectionReason?.ifBlank { null } ?: "Institutional accreditation criteria were not satisfied.",
                                        fontSize = 13.sp,
                                        color = CrimsonDark
                                    )
                                }
                            }
                        }
                    }

                    // Institution summary
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate50, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "Institution: ${reg.institutionName}", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 14.sp)
                        Text(text = "Location: ${reg.city}, ${reg.state}", color = Slate600, fontSize = 12.sp)
                        Text(text = "Representative: ${reg.representativeName} (${reg.representativeDesignation})", color = Slate600, fontSize = 12.sp)
                        Text(text = "Official Email: ${reg.contactEmail}", color = Slate600, fontSize = 12.sp)
                    }

                    if (reg.status == "APPROVED") {
                        Button(
                            onClick = { onLoginAsAdmin(reg.representativePhone) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800, contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Log In as School Admin", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onBackToLogin,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Return to Portal Login", color = Slate700)
                    }
                }
            }
        }
    }
}

@Composable
fun JoinSchoolScreen(
    registrationRepo: InstitutionRegistrationRepository,
    onJoinRequestSubmitted: (schoolName: String) -> Unit,
    onLoginWithPhone: (schoolCode: String) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var schoolCodeInput by remember { mutableStateOf("") }
    var searchedSchool by remember { mutableStateOf<InstitutionEntity?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // Join form fields
    var fullName by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("TEACHER") } // TEACHER, STAFF, STUDENT
    var deptOrGrade by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var requestSubmittedNotice by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    fun handleLookup() {
        searchError = null
        searchedSchool = null
        submitError = null
        requestSubmittedNotice = null
        if (schoolCodeInput.trim().isBlank()) return

        isSearching = true
        coroutineScope.launch {
            val result = registrationRepo.lookupSchoolByCode(schoolCodeInput)
            isSearching = false
            if (result.isSuccess) {
                searchedSchool = result.getOrNull()
            } else {
                searchError = result.exceptionOrNull()?.message ?: "School Code not found."
            }
        }
    }

    fun handleSubmitJoin() {
        submitError = null
        val school = searchedSchool ?: return
        isSubmitting = true

        coroutineScope.launch {
            val result = registrationRepo.submitJoinRequest(
                schoolCode = school.code,
                fullName = fullName,
                phoneNumber = phoneInput,
                role = selectedRole,
                departmentOrGrade = deptOrGrade,
                idNumber = idNumber
            )
            isSubmitting = false
            if (result.isSuccess) {
                requestSubmittedNotice = "Your affiliation request has been submitted to the administration of ${school.name}. A School Code identifies the institution but does not grant automatic access. Once approved by the school administrator or class teacher, you will be authorized to log in with your phone number."
            } else {
                submitError = result.exceptionOrNull()?.message ?: "Failed to submit affiliation request."
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
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .border(1.dp, Slate200, RoundedCornerShape(24.dp))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackToLogin) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Navy900)
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Join an Existing School",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Text(
                                text = "Find your school and request membership",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }

                // Step 1: School Code Search
                item {
                    Text(
                        text = "1. Enter School Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    Text(
                        text = "Your school administrator provides this code (e.g. DPA-ND-10492 or similar format).",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = schoolCodeInput,
                            onValueChange = { schoolCodeInput = it.uppercase() },
                            placeholder = { Text("e.g. DPA-PAT-001", color = AppTextPlaceholder) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = AppTextFieldDefaults.outlinedColors(
                                containerColor = Color.White,
                                textColor = AppTextPrimary,
                                placeholderColor = AppTextPlaceholder,
                                unfocusedBorderColor = AppInputBorder,
                                focusedBorderColor = BlueAccent
                            ),
                            modifier = Modifier.weight(1f).testTag("join_school_code_input")
                        )
                        Button(
                            onClick = { handleLookup() },
                            enabled = schoolCodeInput.isNotBlank() && !isSearching,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                            modifier = Modifier.height(56.dp).testTag("validate_school_code_button")
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Find School")
                            }
                        }
                    }
                }

                if (searchError != null) {
                    item {
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
                                Text(text = searchError!!, color = CrimsonDark, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Found School Card
                if (searchedSchool != null) {
                    val school = searchedSchool!!
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate100),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = school.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
                                    Surface(
                                        color = EmeraldSoft,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("VERIFIED", color = EmeraldDark, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Text(text = "Code: ${school.code} • ${school.type}", fontSize = 12.sp, color = BlueAccent, fontWeight = FontWeight.SemiBold)
                                Text(text = school.address, fontSize = 12.sp, color = Slate600)
                                Text(text = "Leadership: ${school.principalOrDirector}", fontSize = 12.sp, color = Slate600)
                            }
                        }
                    }

                    // Notice regarding access control
                    item {
                        Surface(
                            color = AmberSoft,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = AmberDark, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "Security Policy: A School Code identifies the institution but never grants instant access. If you are already enrolled, log in directly. Otherwise, submit an affiliation request below for the school to authorize you.",
                                    fontSize = 11.sp,
                                    color = Navy900,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    if (requestSubmittedNotice != null) {
                        item {
                            Surface(
                                color = EmeraldSoft,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDark)
                                        Text("Request Submitted Successfully", fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 15.sp)
                                    }
                                    Text(
                                        text = requestSubmittedNotice!!,
                                        fontSize = 12.sp,
                                        color = Navy900,
                                        lineHeight = 16.sp
                                    )
                                    Button(
                                        onClick = onBackToLogin,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Back to Login Screen")
                                    }
                                }
                            }
                        }
                    } else {
                        // Join Request Form
                        item {
                            Text(
                                text = "2. Your Affiliation Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Your Full Name *", color = AppTextSecondary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) phoneInput = it },
                                label = { Text("Your Registered Mobile Number *", color = AppTextSecondary) },
                                leadingIcon = { Text(" +91 ", color = Slate700, fontWeight = FontWeight.Bold) },
                                placeholder = { Text("10-digit number", color = AppTextPlaceholder) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Text(
                                text = "Requested Role *",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("TEACHER" to "Teacher", "STAFF" to "Staff", "STUDENT" to "Student").forEach { (rCode, rLabel) ->
                                    val isSel = selectedRole == rCode
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedRole = rCode },
                                        label = {
                                            Text(
                                                rLabel,
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
                            OutlinedTextField(
                                value = deptOrGrade,
                                onValueChange = { deptOrGrade = it },
                                label = {
                                    Text(if (selectedRole == "STUDENT") "Class & Section (e.g. 10-A) *" else "Department / Subjects *", color = AppTextSecondary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = idNumber,
                                onValueChange = { idNumber = it },
                                label = {
                                    Text(if (selectedRole == "STUDENT") "Roll Number (Optional)" else "Staff Employee ID (Optional)", color = AppTextSecondary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (submitError != null) {
                            item {
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
                                        Text(text = submitError!!, color = CrimsonDark, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = { handleSubmitJoin() },
                                enabled = !isSubmitting && fullName.isNotBlank() && phoneInput.length == 10 && deptOrGrade.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("submit_join_request_button")
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Submit Affiliation Request", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformAdminConsoleScreen(
    registrationRepo: InstitutionRegistrationRepository,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val adminCount by registrationRepo.observePlatformAdminCount().collectAsState(initial = 0)
    val registrations by registrationRepo.observeAllRegistrations().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Bootstrap Form State
    var bootstrapName by remember { mutableStateOf("") }
    var bootstrapPhone by remember { mutableStateOf("") }
    var bootstrapEmail by remember { mutableStateOf("") }
    var bootstrapToken by remember { mutableStateOf("") }
    var bootstrapError by remember { mutableStateOf<String?>(null) }
    var isBootstrapping by remember { mutableStateOf(false) }

    // Admin Review State
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING_APPROVAL, APPROVED, REJECTED
    var rejectingRegId by remember { mutableStateOf<String?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("") }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    val filteredList = remember(registrations, selectedFilter) {
        if (selectedFilter == "ALL") registrations
        else registrations.filter { it.status == selectedFilter }
    }

    fun handleBootstrap() {
        bootstrapError = null
        isBootstrapping = true
        coroutineScope.launch {
            val result = registrationRepo.bootstrapPlatformAdmin(
                name = bootstrapName,
                phone = bootstrapPhone,
                email = bootstrapEmail,
                setupToken = bootstrapToken
            )
            isBootstrapping = false
            if (result.isSuccess) {
                actionMessage = "Root Platform Administrator provisioned successfully."
            } else {
                bootstrapError = result.exceptionOrNull()?.message ?: "Bootstrapping failed."
            }
        }
    }

    fun handleApprove(regId: String) {
        actionMessage = null
        coroutineScope.launch {
            val result = registrationRepo.approveRegistration(regId, "PLATFORM_SUPER_ADMIN_CONSOLE")
            if (result.isSuccess) {
                val appResult = result.getOrNull()!!
                actionMessage = "Approved! Assigned School Code: ${appResult.schoolCode} for ${appResult.institution.name}"
            } else {
                actionMessage = "Approval failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun handleReject(regId: String, reason: String) {
        actionMessage = null
        coroutineScope.launch {
            val result = registrationRepo.rejectRegistration(regId, "PLATFORM_SUPER_ADMIN_CONSOLE", reason)
            rejectingRegId = null
            rejectionReasonInput = ""
            if (result.isSuccess) {
                actionMessage = "Registration marked as REJECTED."
            } else {
                actionMessage = "Rejection failed: ${result.exceptionOrNull()?.message}"
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
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .border(1.dp, Slate200, RoundedCornerShape(24.dp))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackToLogin) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Navy900)
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Platform Administrator Console",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Text(
                                text = "Accreditation & Institutional Governance",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }

                if (actionMessage != null) {
                    item {
                        Surface(
                            color = BlueSoft,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = actionMessage!!,
                                color = Navy900,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // TRUTHFUL STATE: If Platform Administrator is NOT configured
                if (adminCount == 0) {
                    item {
                        Surface(
                            color = AmberSoft,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AmberDark)
                                    Text(
                                        text = "Setup Required: Platform Administrator Not Configured",
                                        fontWeight = FontWeight.Bold,
                                        color = AmberDark,
                                        fontSize = 15.sp
                                    )
                                }
                                Text(
                                    text = "No platform administrator account is currently provisioned. In production, platform administrators are authorized to verify institutional legitimacy, issue persistent school codes, and activate school admin accounts. Use the secure provisioning form below to configure the root administrator.",
                                    fontSize = 12.sp,
                                    color = Navy900,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Bootstrap Root Platform Administrator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = bootstrapName,
                            onValueChange = { bootstrapName = it },
                            label = { Text("Administrator Full Name *", color = AppTextSecondary) },
                            placeholder = { Text("e.g. Operations Director", color = AppTextPlaceholder) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = bootstrapPhone,
                            onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) bootstrapPhone = it },
                            label = { Text("Administrator Mobile Number *", color = AppTextSecondary) },
                            leadingIcon = { Text(" +91 ", color = Slate700, fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = bootstrapEmail,
                            onValueChange = { bootstrapEmail = it },
                            label = { Text("Administrator Email *", color = AppTextSecondary) },
                            placeholder = { Text("admin@1campus.internal", color = AppTextPlaceholder) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = bootstrapToken,
                            onValueChange = { bootstrapToken = it },
                            label = { Text("Root Setup Key *", color = AppTextSecondary) },
                            placeholder = { Text("Enter authorized setup key (e.g. CAMPUS-ROOT-2026)", color = AppTextPlaceholder) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (bootstrapError != null) {
                        item {
                            Surface(
                                color = CrimsonLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = bootstrapError!!,
                                    color = CrimsonDark,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { handleBootstrap() },
                            enabled = !isBootstrapping && bootstrapName.isNotBlank() && bootstrapPhone.length == 10 && bootstrapToken.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            if (isBootstrapping) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                            } else {
                                Text("Provision Root Administrator")
                            }
                        }
                    }
                } else {
                    // CONFIGURED: Review Registrations Console
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Institutional Registrations (${registrations.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "ALL" to "All (${registrations.size})",
                                "PENDING_APPROVAL" to "Pending Review (${registrations.count { it.status == "PENDING_APPROVAL" }})",
                                "APPROVED" to "Approved (${registrations.count { it.status == "APPROVED" }})",
                                "REJECTED" to "Rejected (${registrations.count { it.status == "REJECTED" }})"
                            ).forEach { (fKey, fLabel) ->
                                val isSel = selectedFilter == fKey
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedFilter = fKey },
                                    label = {
                                        Text(
                                            fLabel,
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

                    if (filteredList.isEmpty()) {
                        item {
                            Text(
                                text = "No registrations match the selected filter.",
                                color = Slate500,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(filteredList) { reg ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Slate100),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = reg.institutionName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Navy900)
                                            Text(text = "Ref: ${reg.id} • ${reg.institutionType}", fontSize = 12.sp, color = Slate500)
                                        }
                                        Surface(
                                            color = when (reg.status) {
                                                "APPROVED" -> EmeraldSoft
                                                "REJECTED" -> CrimsonLight
                                                "PENDING_APPROVAL" -> AmberSoft
                                                else -> Slate200
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = reg.status,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = when (reg.status) {
                                                    "APPROVED" -> EmeraldDark
                                                    "REJECTED" -> CrimsonDark
                                                    "PENDING_APPROVAL" -> AmberDark
                                                    else -> Slate700
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Slate200)

                                    Text(text = "Campus: ${reg.address}, ${reg.city}, ${reg.state} - ${reg.postalCode}", fontSize = 12.sp, color = Slate700)
                                    Text(text = "Official Email: ${reg.contactEmail} • Phone: ${reg.contactPhone}", fontSize = 12.sp, color = Slate700)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = "Representative: ${reg.representativeName} (${reg.representativeDesignation})", fontSize = 12.sp, color = Slate700)
                                        if (reg.isPhoneVerified) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Phone Verified", tint = EmeraldDark, modifier = Modifier.size(14.dp))
                                            Text("Verified Phone", color = EmeraldDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    if (reg.status == "APPROVED" && reg.assignedSchoolCode != null) {
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("ASSIGNED SCHOOL CODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                                    Text(reg.assignedSchoolCode!!, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Navy900)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("School Code", reg.assignedSchoolCode))
                                                        Toast.makeText(context, "School Code copied", Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = BlueAccent)
                                                }
                                            }
                                        }
                                    }

                                    if (reg.status == "REJECTED" && reg.rejectionReason != null) {
                                        Text(text = "Reason: ${reg.rejectionReason}", color = CrimsonDark, fontSize = 12.sp)
                                    }

                                    // Action buttons for PENDING_APPROVAL
                                    if (reg.status == "PENDING_APPROVAL") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { handleApprove(reg.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Approve & Issue Code")
                                            }
                                            OutlinedButton(
                                                onClick = { rejectingRegId = reg.id },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Reject", color = CrimsonDark)
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

    // Reject Dialog
    if (rejectingRegId != null) {
        AlertDialog(
            onDismissRequest = { rejectingRegId = null },
            title = { Text("Reject Institution Registration", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Please provide the justification or missing documentation reason:", color = AppTextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        placeholder = { Text("e.g. Institutional accreditation certificate could not be verified.", color = AppTextPlaceholder) },
                        shape = RoundedCornerShape(8.dp),
                        colors = AppTextFieldDefaults.outlinedColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { handleReject(rejectingRegId!!, rejectionReasonInput.ifBlank { "Accreditation criteria not satisfied." }) },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDark)
                ) {
                    Text("Confirm Rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingRegId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
