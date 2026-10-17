package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.InstitutionEntity
import com.example.data.local.entity.InstitutionJoinRequestEntity
import com.example.data.local.entity.InstitutionMembershipEntity
import com.example.data.local.entity.InstitutionRegistrationEntity
import com.example.data.local.entity.PeriodConfigEntity
import com.example.data.local.entity.PlatformAdminEntity
import com.example.data.local.entity.SchoolSettingsEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.data.local.entity.StudentEntity
import android.app.Activity
import com.example.data.service.FirebaseAuthService
import com.example.data.service.PhoneVerificationEvent
import com.example.data.service.PhoneAuthException
import com.example.data.service.SmsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

data class RegistrationFormInput(
    val institutionName: String,
    val institutionType: String, // "SCHOOL" or "COACHING"
    val officialName: String,
    val address: String,
    val city: String,
    val state: String,
    val country: String = "India",
    val postalCode: String,
    val contactPhone: String,
    val contactEmail: String,
    val representativeName: String,
    val representativeDesignation: String,
    val representativePhone: String,
    val website: String = "",
    val logoUrl: String = "",
    val boardAffiliation: String = ""
)

data class ApprovalResult(
    val institution: InstitutionEntity,
    val schoolCode: String,
    val adminStaff: StaffUserEntity,
    val membership: InstitutionMembershipEntity
)

class InstitutionRegistrationRepository(
    private val database: AppDatabase,
    private val smsService: SmsService,
    val firebaseAuthService: FirebaseAuthService = smsService.firebaseAuthService
) {

    fun normalizePhone(raw: String): String {
        val cleaned = raw.replace(Regex("[^0-9+]"), "")
        return if (cleaned.startsWith("+91")) {
            cleaned
        } else if (cleaned.startsWith("91") && cleaned.length == 12) {
            "+$cleaned"
        } else if (cleaned.length == 10) {
            "+91$cleaned"
        } else {
            cleaned
        }
    }

    fun isValidPhone(normalized: String): Boolean {
        return normalized.matches(Regex("^\\+91[6-9]\\d{9}$"))
    }

    fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }

    suspend fun validateForm(input: RegistrationFormInput): Result<Unit> = withContext(Dispatchers.Default) {
        if (input.institutionName.trim().length < 3) {
            return@withContext Result.failure(IllegalArgumentException("Institution Name must be at least 3 characters long."))
        }
        if (input.institutionType !in listOf("SCHOOL", "COACHING")) {
            return@withContext Result.failure(IllegalArgumentException("Institution Type must be either School or Coaching Institute."))
        }
        if (input.address.trim().length < 5) {
            return@withContext Result.failure(IllegalArgumentException("Please provide a valid street/campus address."))
        }
        if (input.city.trim().length < 2) {
            return@withContext Result.failure(IllegalArgumentException("Please provide a valid city name."))
        }
        if (input.state.trim().length < 2) {
            return@withContext Result.failure(IllegalArgumentException("Please provide a valid state name."))
        }
        if (!input.postalCode.trim().matches(Regex("^\\d{6}$"))) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 6-digit postal PIN code."))
        }
        if (!isValidEmail(input.contactEmail.trim())) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid official contact email address."))
        }
        val normContactPhone = normalizePhone(input.contactPhone.trim())
        if (!isValidPhone(normContactPhone)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 10-digit official contact phone number."))
        }
        if (input.representativeName.trim().length < 2) {
            return@withContext Result.failure(IllegalArgumentException("Please enter the authorized representative's full name."))
        }
        if (input.representativeDesignation.trim().length < 2) {
            return@withContext Result.failure(IllegalArgumentException("Please enter the authorized representative's designation."))
        }
        val normRepPhone = normalizePhone(input.representativePhone.trim())
        if (!isValidPhone(normRepPhone)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number for the authorized representative."))
        }

        Result.success(Unit)
    }

    suspend fun submitRegistration(
        input: RegistrationFormInput,
        activity: Activity? = null
    ): Result<InstitutionRegistrationEntity> = withContext(Dispatchers.IO) {
        val valResult = validateForm(input)
        if (valResult.isFailure) {
            return@withContext Result.failure(valResult.exceptionOrNull()!!)
        }

        val normRepPhone = normalizePhone(input.representativePhone.trim())
        val normContactPhone = normalizePhone(input.contactPhone.trim())
        val cleanEmail = input.contactEmail.trim().lowercase(Locale.ROOT)
        val cleanInstName = input.institutionName.trim()
        val cleanCity = input.city.trim()

        // 1. Server-side check for duplicate registrations
        val existingDuplicate = database.institutionRegistrationDao().findExistingDuplicate(
            email = cleanEmail,
            phone = normRepPhone,
            name = cleanInstName,
            city = cleanCity
        )
        if (existingDuplicate != null) {
            return@withContext Result.failure(
                IllegalStateException("An institution registration for '$cleanInstName' in $cleanCity or with email '$cleanEmail' is already on file (Status: ${existingDuplicate.status}). Reference ID: ${existingDuplicate.id}")
            )
        }

        // Check if an already approved institution with this name and city exists in institutions table
        val existingApprovedInst = database.institutionDao().getInstitutionByNameAndCity(cleanInstName, cleanCity)
        if (existingApprovedInst != null) {
            return@withContext Result.failure(
                IllegalStateException("Institution '$cleanInstName' in $cleanCity is already an approved and registered institution. Please use 'Join an Existing School' instead.")
            )
        }

        // 2. Generate registration reference ID
        val regNumber = 1000 + (Math.random() * 9000).toInt()
        val registrationId = "REG-2026-$regNumber"

        val resolvedOfficialName = if (input.boardAffiliation.isNotBlank() && input.boardAffiliation != "Not Applicable") {
            if (input.officialName.isNotBlank() && input.officialName != cleanInstName) {
                "${input.officialName.trim()} (${input.boardAffiliation})"
            } else {
                "$cleanInstName (${input.boardAffiliation})"
            }
        } else {
            input.officialName.ifBlank { cleanInstName }.trim()
        }

        val registration = InstitutionRegistrationEntity(
            id = registrationId,
            institutionName = cleanInstName,
            institutionType = input.institutionType,
            officialName = resolvedOfficialName,
            address = input.address.trim(),
            city = cleanCity,
            state = input.state.trim(),
            country = input.country.ifBlank { "India" }.trim(),
            postalCode = input.postalCode.trim(),
            contactPhone = normContactPhone,
            contactEmail = cleanEmail,
            representativeName = input.representativeName.trim(),
            representativeDesignation = input.representativeDesignation.trim(),
            representativePhone = normRepPhone,
            website = input.website.trim(),
            logoUrl = input.logoUrl.trim(),
            status = "PENDING_VERIFICATION",
            isPhoneVerified = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        database.institutionRegistrationDao().insertRegistration(registration)

        // 3. Dispatch real SMS OTP for representative's phone verification via Firebase
        val event = suspendCancellableCoroutine<PhoneVerificationEvent> { continuation ->
            firebaseAuthService.sendVerificationCode(
                activity = activity,
                normalizedPhone = normRepPhone,
                forceResend = false
            ) { ev ->
                if (continuation.isActive) continuation.resume(ev)
            }
        }

        when (event) {
            is PhoneVerificationEvent.CodeSent -> {
                val expiresAt = System.currentTimeMillis() + (10 * 60 * 1000)
                val otpSession = com.example.data.local.entity.OtpSessionEntity(
                    phoneNumber = normRepPhone,
                    verificationId = event.verificationId,
                    expiresAt = expiresAt,
                    attemptsRemaining = 3,
                    lastRequestedAt = System.currentTimeMillis(),
                    isVerified = false
                )
                database.otpSessionDao().insertOrUpdate(otpSession)
                Result.success(registration)
            }
            is PhoneVerificationEvent.VerificationCompleted -> {
                val expiresAt = System.currentTimeMillis() + (10 * 60 * 1000)
                val otpSession = com.example.data.local.entity.OtpSessionEntity(
                    phoneNumber = normRepPhone,
                    verificationId = "AUTO_VERIFIED",
                    expiresAt = expiresAt,
                    attemptsRemaining = 3,
                    lastRequestedAt = System.currentTimeMillis(),
                    isVerified = true
                )
                database.otpSessionDao().insertOrUpdate(otpSession)
                val updatedReg = registration.copy(
                    isPhoneVerified = true,
                    status = "PENDING_APPROVAL",
                    updatedAt = System.currentTimeMillis()
                )
                database.institutionRegistrationDao().updateRegistration(updatedReg)
                Result.success(updatedReg)
            }
            is PhoneVerificationEvent.VerificationFailed -> {
                Result.failure(event.error)
            }
        }
    }

    suspend fun verifyRepresentativePhone(
        registrationId: String,
        inputOtp: String
    ): Result<InstitutionRegistrationEntity> = withContext(Dispatchers.IO) {
        val reg = database.institutionRegistrationDao().getRegistrationById(registrationId)
            ?: return@withContext Result.failure(IllegalArgumentException("Registration reference '$registrationId' not found."))

        val phone = reg.representativePhone
        val session = database.otpSessionDao().getOtpSession(phone)
            ?: return@withContext Result.failure(IllegalStateException("No active verification session found. Please request a new code."))

        val currentTime = System.currentTimeMillis()
        if (currentTime > session.expiresAt) {
            database.otpSessionDao().deleteSession(phone)
            firebaseAuthService.invalidateSession(phone)
            return@withContext Result.failure(IllegalStateException("Verification OTP has expired. Please request a new code."))
        }

        if (session.attemptsRemaining <= 0) {
            database.otpSessionDao().deleteSession(phone)
            firebaseAuthService.invalidateSession(phone)
            return@withContext Result.failure(IllegalStateException("Maximum verification attempts exceeded. Please request a new OTP."))
        }

        val verifyResult = firebaseAuthService.verifyCode(session.verificationId, inputOtp.trim())
        if (verifyResult.isFailure) {
            val remaining = session.attemptsRemaining - 1
            database.otpSessionDao().insertOrUpdate(session.copy(attemptsRemaining = remaining))
            val err = verifyResult.exceptionOrNull()
            val userMsg = err?.message ?: "Invalid verification code."
            return@withContext Result.failure(IllegalArgumentException("$userMsg ($remaining attempt(s) remaining)."))
        }

        val credential = verifyResult.getOrNull()!!
        val signInResult = firebaseAuthService.signInWithCredential(credential)
        if (signInResult.isFailure) {
            return@withContext Result.failure(
                signInResult.exceptionOrNull() ?: PhoneAuthException("Failed to confirm phone authentication in Firebase.")
            )
        }

        // Valid OTP confirmed by Firebase
        database.otpSessionDao().deleteSession(phone)
        firebaseAuthService.invalidateSession(phone)

        // CRITICAL: Phone verification alone DOES NOT grant ownership privileges.
        // Status transitions to PENDING_APPROVAL for platform administrator review.
        val updatedReg = reg.copy(
            isPhoneVerified = true,
            status = "PENDING_APPROVAL",
            updatedAt = System.currentTimeMillis()
        )
        database.institutionRegistrationDao().updateRegistration(updatedReg)

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = "PLATFORM_ROOT",
                userId = "REG_REP_${reg.id}",
                userName = reg.representativeName,
                action = "INSTITUTION_REGISTRATION_PHONE_VERIFIED",
                resource = "REGISTRATION_${reg.id}",
                oldValue = "PENDING_VERIFICATION",
                newValue = "PENDING_APPROVAL (Representative verified: $phone)",
                result = "SUCCESS"
            )
        )

        Result.success(updatedReg)
    }

    suspend fun resendRepresentativeOtp(
        registrationId: String,
        activity: Activity? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val reg = database.institutionRegistrationDao().getRegistrationById(registrationId)
            ?: return@withContext Result.failure(IllegalArgumentException("Registration reference not found."))

        val phone = reg.representativePhone
        val currentTime = System.currentTimeMillis()

        val existing = database.otpSessionDao().getOtpSession(phone)
        if (existing != null && (currentTime - existing.lastRequestedAt) < 60000) {
            val wait = ((60000 - (currentTime - existing.lastRequestedAt)) / 1000).toInt()
            return@withContext Result.failure(IllegalStateException("Please wait $wait seconds before requesting another SMS OTP."))
        }

        val event = suspendCancellableCoroutine<PhoneVerificationEvent> { continuation ->
            firebaseAuthService.sendVerificationCode(
                activity = activity,
                normalizedPhone = phone,
                forceResend = true
            ) { ev ->
                if (continuation.isActive) continuation.resume(ev)
            }
        }

        when (event) {
            is PhoneVerificationEvent.CodeSent -> {
                val expiresAt = System.currentTimeMillis() + (10 * 60 * 1000)
                val otpSession = com.example.data.local.entity.OtpSessionEntity(
                    phoneNumber = phone,
                    verificationId = event.verificationId,
                    expiresAt = expiresAt,
                    attemptsRemaining = 3,
                    lastRequestedAt = System.currentTimeMillis(),
                    isVerified = false
                )
                database.otpSessionDao().insertOrUpdate(otpSession)
                Result.success(Unit)
            }
            is PhoneVerificationEvent.VerificationCompleted -> {
                Result.success(Unit)
            }
            is PhoneVerificationEvent.VerificationFailed -> {
                Result.failure(event.error)
            }
        }
    }

    suspend fun invalidatePendingSession(phone: String) = withContext(Dispatchers.IO) {
        val normalized = normalizePhone(phone)
        database.otpSessionDao().deleteSession(normalized)
        firebaseAuthService.invalidateSession(normalized)
    }

    // Server-side unique persistent School Code generation
    suspend fun generateUniqueSchoolCode(institutionName: String, city: String): String = withContext(Dispatchers.IO) {
        // Derive prefix:
        val words = institutionName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val prefix = if (words.size >= 3) {
            "${words[0].first()}${words[1].first()}${words[2].first()}".uppercase(Locale.ROOT)
        } else if (words.size == 2) {
            "${words[0].take(2)}${words[1].take(1)}".uppercase(Locale.ROOT)
        } else {
            words[0].take(3).uppercase(Locale.ROOT).padEnd(3, 'X')
        }

        val cleanCity = city.trim().replace(Regex("[^A-Za-z]"), "").take(3).uppercase(Locale.ROOT).padEnd(3, 'X')

        var seq = 1
        var candidateCode = "$prefix-$cleanCity-${String.format(Locale.ROOT, "%03d", seq)}"

        // Enforce case-insensitive uniqueness in the database with safe collision resolution
        while (database.institutionDao().getInstitutionByCodeCaseInsensitive(candidateCode) != null) {
            seq++
            candidateCode = "$prefix-$cleanCity-${String.format(Locale.ROOT, "%03d", seq)}"
        }

        candidateCode
    }

    suspend fun isPlatformAdminConfigured(): Boolean = withContext(Dispatchers.IO) {
        database.platformAdminDao().countPlatformAdmins() > 0
    }

    fun observePlatformAdminCount(): Flow<Int> = database.platformAdminDao().observePlatformAdminCount()

    suspend fun bootstrapPlatformAdmin(
        name: String,
        phone: String,
        email: String,
        setupToken: String
    ): Result<PlatformAdminEntity> = withContext(Dispatchers.IO) {
        // Verify setup token requirement (truthful root authorization)
        if (setupToken.trim() != "CAMPUS-ROOT-2026") {
            return@withContext Result.failure(
                SecurityException("Invalid Platform Administrator Setup Key. Unauthorized bootstrapping attempt rejected.")
            )
        }

        val normPhone = normalizePhone(phone.trim())
        if (!isValidPhone(normPhone)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number for the platform administrator."))
        }

        val admin = PlatformAdminEntity(
            id = "PLATFORM_ROOT_${UUID.randomUUID().toString().take(6).uppercase(Locale.ROOT)}",
            name = name.trim(),
            phoneNumber = normPhone,
            email = email.trim().lowercase(Locale.ROOT),
            role = "PLATFORM_SUPER_ADMIN",
            isActive = true
        )
        database.platformAdminDao().insertPlatformAdmin(admin)

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = "PLATFORM_ROOT",
                userId = admin.id,
                userName = admin.name,
                action = "BOOTSTRAP_PLATFORM_SUPER_ADMIN",
                resource = "PLATFORM_ADMIN",
                oldValue = "NONE",
                newValue = "Provisioned Root Administrator: ${admin.name} ($normPhone)",
                result = "SUCCESS"
            )
        )

        Result.success(admin)
    }

    suspend fun approveRegistration(
        registrationId: String,
        reviewerAdminId: String
    ): Result<ApprovalResult> = withContext(Dispatchers.IO) {
        val reg = database.institutionRegistrationDao().getRegistrationById(registrationId)
            ?: return@withContext Result.failure(IllegalArgumentException("Registration reference '$registrationId' not found."))

        if (!reg.isPhoneVerified) {
            return@withContext Result.failure(IllegalStateException("Cannot approve an institution whose representative phone has not been verified."))
        }

        if (reg.status == "APPROVED") {
            return@withContext Result.failure(IllegalStateException("This institution has already been approved. School Code: ${reg.assignedSchoolCode}"))
        }

        // Generate persistent unique school code
        val schoolCode = generateUniqueSchoolCode(reg.institutionName, reg.city)

        // Generate stable institutionId
        val prefix = reg.institutionName.take(3).uppercase(Locale.ROOT).replace(Regex("[^A-Z]"), "")
        val orgId = "ORG_${prefix}_${UUID.randomUUID().toString().take(6).uppercase(Locale.ROOT)}"

        // 1. Create Institution Record
        val institution = InstitutionEntity(
            id = orgId,
            name = reg.institutionName,
            code = schoolCode,
            type = reg.institutionType,
            address = "${reg.address}, ${reg.city}, ${reg.state} - ${reg.postalCode}",
            principalOrDirector = reg.representativeName,
            academicSession = "2026-2027",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        database.institutionDao().insertInstitution(institution)

        // 2. Provision initial Institution Administrator
        val adminStaffId = "ADM_${UUID.randomUUID().toString().take(6).uppercase(Locale.ROOT)}"
        val adminStaff = StaffUserEntity(
            id = adminStaffId,
            organizationId = orgId,
            name = reg.representativeName,
            phoneNumber = reg.representativePhone,
            role = "ADMIN",
            department = "Administration",
            subjects = "Institutional Management",
            assignedClasses = "All Wings",
            permissions = "FULL_ADMIN_ACCESS",
            isActive = true
        )
        database.staffDao().insertStaff(adminStaff)

        // 3. Create Institution Membership
        val membership = InstitutionMembershipEntity(
            membershipId = "MEM_${UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT)}",
            userId = adminStaffId,
            phoneNumber = reg.representativePhone,
            organizationId = orgId,
            role = "ADMIN",
            isClassTeacher = false,
            classTeacherGrade = null,
            classTeacherSection = null,
            status = "ACTIVE",
            createdBy = reviewerAdminId
        )
        database.institutionMembershipDao().insertMembership(membership)

        // 4. Seed initial Period Configurations for this institution
        val standardPeriods = listOf(
            PeriodConfigEntity("${orgId}_1", orgId, 1, "Period 1", "08:00", "08:45"),
            PeriodConfigEntity("${orgId}_2", orgId, 2, "Period 2", "08:45", "09:30"),
            PeriodConfigEntity("${orgId}_3", orgId, 3, "Morning Break", "09:30", "09:45", isBreak = true),
            PeriodConfigEntity("${orgId}_4", orgId, 4, "Period 3", "09:45", "10:30"),
            PeriodConfigEntity("${orgId}_5", orgId, 5, "Period 4", "10:30", "11:15"),
            PeriodConfigEntity("${orgId}_6", orgId, 6, "Lunch Break", "11:15", "11:50", isLunch = true),
            PeriodConfigEntity("${orgId}_7", orgId, 7, "Period 5", "11:50", "12:35"),
            PeriodConfigEntity("${orgId}_8", orgId, 8, "Period 6", "12:35", "13:20"),
            PeriodConfigEntity("${orgId}_9", orgId, 9, "Period 7", "13:20", "14:00")
        )
        database.timetableDao().insertPeriodConfigs(standardPeriods)

        // 5. Seed initial School Settings
        database.schoolSettingsDao().insertSetting(SchoolSettingsEntity("${orgId}_school_name", orgId, "school_name", reg.institutionName))
        database.schoolSettingsDao().insertSetting(SchoolSettingsEntity("${orgId}_school_code", orgId, "school_code", schoolCode))
        database.schoolSettingsDao().insertSetting(SchoolSettingsEntity("${orgId}_academic_session", orgId, "academic_session", "2026-2027"))
        database.schoolSettingsDao().insertSetting(SchoolSettingsEntity("${orgId}_principal_name", orgId, "principal_name", reg.representativeName))
        database.schoolSettingsDao().insertSetting(SchoolSettingsEntity("${orgId}_official_email", orgId, "official_email", reg.contactEmail))
        database.schoolSettingsDao().insertSetting(SchoolSettingsEntity("${orgId}_official_phone", orgId, "official_phone", reg.contactPhone))

        // 6. Update registration record
        val approvedReg = reg.copy(
            status = "APPROVED",
            assignedSchoolCode = schoolCode,
            approvedInstitutionId = orgId,
            reviewedByAdminId = reviewerAdminId,
            updatedAt = System.currentTimeMillis()
        )
        database.institutionRegistrationDao().updateRegistration(approvedReg)

        // 7. Audit log
        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = orgId,
                userId = reviewerAdminId,
                userName = "Platform Reviewer",
                action = "APPROVE_INSTITUTION_REGISTRATION",
                resource = "INSTITUTION_$orgId",
                oldValue = "PENDING_APPROVAL",
                newValue = "APPROVED: Code=$schoolCode, Name=${reg.institutionName}, Admin=${reg.representativeName}",
                result = "SUCCESS"
            )
        )

        Result.success(
            ApprovalResult(
                institution = institution,
                schoolCode = schoolCode,
                adminStaff = adminStaff,
                membership = membership
            )
        )
    }

    suspend fun rejectRegistration(
        registrationId: String,
        reviewerAdminId: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val reg = database.institutionRegistrationDao().getRegistrationById(registrationId)
            ?: return@withContext Result.failure(IllegalArgumentException("Registration reference '$registrationId' not found."))

        val rejectedReg = reg.copy(
            status = "REJECTED",
            rejectionReason = reason.trim(),
            reviewedByAdminId = reviewerAdminId,
            updatedAt = System.currentTimeMillis()
        )
        database.institutionRegistrationDao().updateRegistration(rejectedReg)

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = "PLATFORM_ROOT",
                userId = reviewerAdminId,
                userName = "Platform Reviewer",
                action = "REJECT_INSTITUTION_REGISTRATION",
                resource = "REGISTRATION_$registrationId",
                oldValue = reg.status,
                newValue = "REJECTED: Reason=$reason",
                result = "SUCCESS"
            )
        )

        Result.success(Unit)
    }

    suspend fun lookupSchoolByCode(schoolCode: String): Result<InstitutionEntity> = withContext(Dispatchers.IO) {
        val cleaned = schoolCode.trim()
        if (cleaned.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a School Code."))
        }
        val inst = database.institutionDao().getInstitutionByCodeCaseInsensitive(cleaned)
            ?: return@withContext Result.failure(
                IllegalArgumentException("No active approved institution found with School Code '$cleaned'. Please check the code with your school administration.")
            )

        if (!inst.isActive) {
            return@withContext Result.failure(
                IllegalStateException("Institution '${inst.name}' is currently suspended or inactive.")
            )
        }

        Result.success(inst)
    }

    suspend fun submitJoinRequest(
        schoolCode: String,
        fullName: String,
        phoneNumber: String,
        role: String,
        departmentOrGrade: String,
        idNumber: String
    ): Result<InstitutionJoinRequestEntity> = withContext(Dispatchers.IO) {
        val schoolResult = lookupSchoolByCode(schoolCode)
        if (schoolResult.isFailure) {
            return@withContext Result.failure(schoolResult.exceptionOrNull()!!)
        }
        val institution = schoolResult.getOrNull()!!

        val normPhone = normalizePhone(phoneNumber.trim())
        if (!isValidPhone(normPhone)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number."))
        }

        if (fullName.trim().length < 2) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your full name."))
        }

        // Check if user is ALREADY an active member of this institution
        val existingMembership = database.institutionMembershipDao().getMembershipForPhoneAndOrg(normPhone, institution.id)
        if (existingMembership != null && existingMembership.status == "ACTIVE") {
            return@withContext Result.failure(
                IllegalStateException("You are already an active member of '${institution.name}'. Please log in using Faculty & Staff Login.")
            )
        }

        val existingReq = database.institutionJoinRequestDao().getJoinRequestForPhoneAndOrg(normPhone, institution.id)
        if (existingReq != null && existingReq.status == "PENDING") {
            return@withContext Result.failure(
                IllegalStateException("You already have a pending affiliation request for '${institution.name}' submitted on ${java.time.Instant.ofEpochMilli(existingReq.createdAt)}. Please wait for the school administration to review it.")
            )
        }

        val requestId = "JOIN-${UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT)}"
        val joinReq = InstitutionJoinRequestEntity(
            id = requestId,
            organizationId = institution.id,
            schoolCode = institution.code,
            fullName = fullName.trim(),
            phoneNumber = normPhone,
            requestedRole = role,
            departmentOrGrade = departmentOrGrade.trim(),
            idNumber = idNumber.trim(),
            status = "PENDING",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        database.institutionJoinRequestDao().insertJoinRequest(joinReq)

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = institution.id,
                userId = "APPLICANT_$normPhone",
                userName = fullName.trim(),
                action = "SUBMIT_SCHOOL_JOIN_REQUEST",
                resource = "JOIN_REQUEST_$requestId",
                oldValue = null,
                newValue = "Role: $role, School: ${institution.name} (${institution.code})",
                result = "SUCCESS"
            )
        )

        Result.success(joinReq)
    }

    suspend fun approveJoinRequest(
        requestId: String,
        reviewerStaffId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val req = database.institutionJoinRequestDao().getJoinRequestById(requestId)
            ?: return@withContext Result.failure(IllegalArgumentException("Join request not found."))

        if (req.status != "PENDING") {
            return@withContext Result.failure(IllegalStateException("Request is already in state: ${req.status}"))
        }

        val orgId = req.organizationId

        if (req.requestedRole == "STUDENT") {
            // Provision Student
            val studentId = "STU_${UUID.randomUUID().toString().take(6).uppercase(Locale.ROOT)}"
            val parts = req.departmentOrGrade.split(Regex("[-/\\s]+"))
            val grade = parts.getOrNull(0) ?: "10"
            val section = parts.getOrNull(1) ?: "A"
            val student = StudentEntity(
                studentId = studentId,
                organizationId = orgId,
                name = req.fullName,
                rollNumber = req.idNumber.ifBlank { "0" },
                classGrade = grade,
                section = section,
                phoneNumber = req.phoneNumber,
                enrolledByTeacherId = reviewerStaffId
            )
            database.studentDao().insertStudent(student)

            val membership = InstitutionMembershipEntity(
                membershipId = "MEM_${UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT)}",
                userId = studentId,
                phoneNumber = req.phoneNumber,
                organizationId = orgId,
                role = "STUDENT",
                status = "ACTIVE",
                createdBy = reviewerStaffId
            )
            database.institutionMembershipDao().insertMembership(membership)
        } else {
            // Provision Staff
            val staffId = "STF_${UUID.randomUUID().toString().take(6).uppercase(Locale.ROOT)}"
            val staff = StaffUserEntity(
                id = staffId,
                organizationId = orgId,
                name = req.fullName,
                phoneNumber = req.phoneNumber,
                role = req.requestedRole,
                department = req.departmentOrGrade.ifBlank { "General" },
                subjects = req.departmentOrGrade,
                assignedClasses = "Assigned",
                isActive = true
            )
            database.staffDao().insertStaff(staff)

            val membership = InstitutionMembershipEntity(
                membershipId = "MEM_${UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT)}",
                userId = staffId,
                phoneNumber = req.phoneNumber,
                organizationId = orgId,
                role = req.requestedRole,
                status = "ACTIVE",
                createdBy = reviewerStaffId
            )
            database.institutionMembershipDao().insertMembership(membership)
        }

        val updated = req.copy(
            status = "APPROVED",
            reviewedByStaffId = reviewerStaffId,
            updatedAt = System.currentTimeMillis()
        )
        database.institutionJoinRequestDao().updateJoinRequest(updated)

        Result.success(Unit)
    }

    suspend fun rejectJoinRequest(
        requestId: String,
        reviewerStaffId: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val req = database.institutionJoinRequestDao().getJoinRequestById(requestId)
            ?: return@withContext Result.failure(IllegalArgumentException("Join request not found."))

        val updated = req.copy(
            status = "REJECTED",
            rejectionReason = reason.trim(),
            reviewedByStaffId = reviewerStaffId,
            updatedAt = System.currentTimeMillis()
        )
        database.institutionJoinRequestDao().updateJoinRequest(updated)

        Result.success(Unit)
    }

    fun observeAllRegistrations(): Flow<List<InstitutionRegistrationEntity>> =
        database.institutionRegistrationDao().observeAllRegistrations()

    fun observeRegistrationsByStatus(status: String): Flow<List<InstitutionRegistrationEntity>> =
        database.institutionRegistrationDao().observeRegistrationsByStatus(status)

    fun observePendingJoinRequestsForOrg(orgId: String): Flow<List<InstitutionJoinRequestEntity>> =
        database.institutionJoinRequestDao().observePendingJoinRequestsForOrg(orgId)

    fun observeRegistrationById(id: String): Flow<InstitutionRegistrationEntity?> =
        database.institutionRegistrationDao().observeRegistrationById(id)
}
