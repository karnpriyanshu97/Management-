package com.example.data.repository

import android.app.Activity
import com.example.data.local.AppDatabase
import com.example.data.local.SessionManager
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.InstitutionEntity
import com.example.data.local.entity.InstitutionMembershipEntity
import com.example.data.local.entity.OtpSessionEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.service.FirebaseAuthService
import com.example.data.service.PhoneAuthException
import com.example.data.service.PhoneVerificationEvent
import com.example.data.service.SmsService
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume

data class PublicFacultyItem(
    val id: String,
    val name: String,
    val designation: String,
    val role: String
)

sealed class AuthenticatedUser {
    data class Staff(val staff: StaffUserEntity, val membership: InstitutionMembershipEntity, val institution: InstitutionEntity) : AuthenticatedUser()
    data class Student(val student: StudentEntity, val membership: InstitutionMembershipEntity, val institution: InstitutionEntity) : AuthenticatedUser()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AuthRepository(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val smsService: SmsService,
    val firebaseAuthService: FirebaseAuthService = smsService.firebaseAuthService
) {

    val activeOrganizationIdFlow: Flow<String?> = sessionManager.activeOrganizationId

    fun observePrimaryInstitution(): Flow<InstitutionEntity?> {
        return database.institutionDao().observePrimaryActiveInstitution()
    }

    fun observeAllInstitutions(): Flow<List<InstitutionEntity>> {
        return database.institutionDao().getAllActiveInstitutions()
    }

    fun observePublicFacultyDirectory(): Flow<List<PublicFacultyItem>> {
        return database.staffDao().getAllActiveStaffGlobal().map { staffList ->
            staffList.map { staff ->
                PublicFacultyItem(
                    id = staff.id,
                    name = staff.name,
                    designation = staff.department.ifBlank { staff.role },
                    role = staff.role
                )
            }
        }
    }

    val activeInstitutionFlow: Flow<InstitutionEntity?> = sessionManager.activeOrganizationId.flatMapLatest { orgId ->
        if (orgId != null) database.institutionDao().observeInstitutionById(orgId)
        else flowOf(null)
    }

    val userMembershipsFlow: Flow<List<InstitutionMembershipEntity>> = sessionManager.currentPhone.flatMapLatest { phone ->
        if (phone != null) database.institutionMembershipDao().observeActiveMembershipsForPhone(phone)
        else flowOf(emptyList())
    }

    val activeMembershipFlow: Flow<InstitutionMembershipEntity?> = combine(
        sessionManager.currentPhone,
        sessionManager.activeOrganizationId
    ) { phone, orgId ->
        if (phone != null && orgId != null) {
            database.institutionMembershipDao().getMembershipForPhoneAndOrg(phone, orgId)
        } else null
    }

    val currentUserFlow: Flow<StaffUserEntity?> = combine(
        sessionManager.currentPhone,
        sessionManager.activeOrganizationId
    ) { phone, orgId ->
        if (phone != null && orgId != null) {
            database.staffDao().getStaffByPhone(phone, orgId)
        } else null
    }

    val currentStudentFlow: Flow<StudentEntity?> = combine(
        sessionManager.currentUserId,
        sessionManager.activeOrganizationId
    ) { userId, orgId ->
        if (userId != null && orgId != null) {
            database.studentDao().getStudentById(orgId, userId)
        } else null
    }

    fun isUserLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun getActiveOrgId(): String? = sessionManager.activeOrganizationId.value

    /**
     * Dispatches real SMS OTP verification code via Firebase Phone Authentication.
     * Enforces active school enrollment and a 60-second cooldown prior to dispatch.
     */
    suspend fun sendOtp(
        rawPhone: String,
        activity: Activity? = null,
        forceResend: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(rawPhone)
        if (!isValidPhoneNumber(normalized)) {
            return@withContext Result.failure(
                IllegalArgumentException("Please enter a valid international mobile number in E.164 format (e.g., +91 98765 43210).")
            )
        }

        // REAL SCHOOL MEMBERSHIP CHECK:
        // Must search the database for an ACTIVE staff or student membership.
        // If no matching membership exists, reject immediately without dispatching OTP.
        val activeMemberships = database.institutionMembershipDao().getActiveMembershipsForPhone(normalized)
        if (activeMemberships.isEmpty()) {
            return@withContext Result.failure(
                SecurityException("No staff or student account is registered with this mobile number. Please contact your Class Teacher or school administration.")
            )
        }

        val existingSession = database.otpSessionDao().getOtpSession(normalized)
        val currentTime = System.currentTimeMillis()

        // 60-second resend cooldown
        if (existingSession != null && (currentTime - existingSession.lastRequestedAt) < 60000) {
            val waitSeconds = ((60000 - (currentTime - existingSession.lastRequestedAt)) / 1000).toInt()
            return@withContext Result.failure(
                IllegalStateException("Please wait $waitSeconds seconds before requesting another SMS code.")
            )
        }

        // Dispatch real SMS via Firebase Phone Auth
        val event = suspendCancellableCoroutine<PhoneVerificationEvent> { continuation ->
            firebaseAuthService.sendVerificationCode(
                activity = activity,
                normalizedPhone = normalized,
                forceResend = forceResend
            ) { ev ->
                if (continuation.isActive) {
                    continuation.resume(ev)
                }
            }
        }

        when (event) {
            is PhoneVerificationEvent.CodeSent -> {
                val expiresAt = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutes session validity
                val newSession = OtpSessionEntity(
                    phoneNumber = normalized,
                    verificationId = event.verificationId,
                    expiresAt = expiresAt,
                    attemptsRemaining = 3,
                    lastRequestedAt = System.currentTimeMillis(),
                    isVerified = false
                )
                database.otpSessionDao().insertOrUpdate(newSession)
                Result.success(maskPhoneNumber(normalized))
            }
            is PhoneVerificationEvent.VerificationCompleted -> {
                val expiresAt = System.currentTimeMillis() + (10 * 60 * 1000)
                val verifiedSession = OtpSessionEntity(
                    phoneNumber = normalized,
                    verificationId = "AUTO_VERIFIED",
                    expiresAt = expiresAt,
                    attemptsRemaining = 3,
                    lastRequestedAt = System.currentTimeMillis(),
                    isVerified = true
                )
                database.otpSessionDao().insertOrUpdate(verifiedSession)
                Result.success(maskPhoneNumber(normalized))
            }
            is PhoneVerificationEvent.VerificationFailed -> {
                Result.failure(event.error)
            }
        }
    }

    /**
     * Verifies the user-entered 6-digit SMS code against Firebase Phone Auth.
     * Enforces session expiration, maximum 3 attempts, and authenticates the user upon success.
     */
    suspend fun verifyOtp(
        rawPhone: String,
        inputOtp: String
    ): Result<AuthenticatedUser> = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(rawPhone)
        val session = database.otpSessionDao().getOtpSession(normalized)
            ?: return@withContext Result.failure(
                IllegalStateException("No active verification session found. Please request a new SMS code.")
            )

        val currentTime = System.currentTimeMillis()
        if (currentTime > session.expiresAt) {
            database.otpSessionDao().deleteSession(normalized)
            firebaseAuthService.invalidateSession(normalized)
            return@withContext Result.failure(
                IllegalStateException("SMS verification code has expired. Please request a new code.")
            )
        }

        if (session.attemptsRemaining <= 0) {
            database.otpSessionDao().deleteSession(normalized)
            firebaseAuthService.invalidateSession(normalized)
            return@withContext Result.failure(
                IllegalStateException("Maximum verification attempts exceeded. Please request a new verification code.")
            )
        }

        // Verify with Firebase Phone Authentication
        val verifyResult = firebaseAuthService.verifyCode(session.verificationId, inputOtp.trim())
        if (verifyResult.isFailure) {
            val remaining = session.attemptsRemaining - 1
            database.otpSessionDao().insertOrUpdate(
                session.copy(attemptsRemaining = remaining)
            )
            val err = verifyResult.exceptionOrNull()
            val userMsg = err?.message ?: "Invalid verification code."
            return@withContext Result.failure(
                IllegalArgumentException("$userMsg ($remaining attempt(s) remaining).")
            )
        }

        val credential = verifyResult.getOrNull()!!
        val signInResult = firebaseAuthService.signInWithCredential(credential)
        if (signInResult.isFailure) {
            return@withContext Result.failure(
                signInResult.exceptionOrNull() ?: PhoneAuthException("Failed to establish authenticated session.")
            )
        }

        // Correct OTP verified and Firebase Phone Auth confirmed
        database.otpSessionDao().deleteSession(normalized)
        firebaseAuthService.invalidateSession(normalized)

        // Backend checks active institution memberships
        val activeMemberships = database.institutionMembershipDao().getActiveMembershipsForPhone(normalized)
        if (activeMemberships.isEmpty()) {
            return@withContext Result.failure(
                SecurityException("Your verified phone number is not enrolled in an active institution. Please contact your Class Teacher or school administration.")
            )
        }

        val primaryMembership = activeMemberships.first()
        val institution = database.institutionDao().getInstitutionById(primaryMembership.organizationId)
            ?: return@withContext Result.failure(IllegalStateException("Institution record not found."))

        sessionManager.createSession(
            userId = primaryMembership.userId,
            phoneNumber = normalized,
            organizationId = primaryMembership.organizationId,
            membershipId = primaryMembership.membershipId
        )

        val authenticatedUser = if (primaryMembership.role == "STUDENT") {
            val student = database.studentDao().getStudentById(primaryMembership.organizationId, primaryMembership.userId)
                ?: return@withContext Result.failure(IllegalStateException("Student record not found in institution."))
            AuthenticatedUser.Student(student, primaryMembership, institution)
        } else {
            val staff = database.staffDao().getStaffById(primaryMembership.userId, primaryMembership.organizationId)
                ?: return@withContext Result.failure(IllegalStateException("Staff profile not found in institution."))
            AuthenticatedUser.Staff(staff, primaryMembership, institution)
        }

        // Audit Log
        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = primaryMembership.organizationId,
                userId = primaryMembership.userId,
                userName = when (authenticatedUser) {
                    is AuthenticatedUser.Staff -> authenticatedUser.staff.name
                    is AuthenticatedUser.Student -> authenticatedUser.student.name
                },
                action = "LOGIN_PHONE_OTP_FIREBASE",
                resource = "AUTH",
                oldValue = null,
                newValue = "Firebase Verified Session Created (${institution.name})",
                result = "SUCCESS"
            )
        )

        Result.success(authenticatedUser)
    }

    /**
     * Invalidates any active pending verification session when a user changes their phone number
     * or navigates away from the verification screen.
     */
    suspend fun invalidatePendingSession(rawPhone: String) = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(rawPhone)
        database.otpSessionDao().deleteSession(normalized)
        firebaseAuthService.invalidateSession(normalized)
    }

    suspend fun switchActiveInstitution(targetOrgId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentPhone = sessionManager.currentPhone.value
            ?: return@withContext Result.failure(IllegalStateException("No authenticated session found."))

        val membership = database.institutionMembershipDao().getMembershipForPhoneAndOrg(currentPhone, targetOrgId)
        if (membership == null || membership.status != "ACTIVE") {
            return@withContext Result.failure(SecurityException("Access Denied: You do not have an active membership in institution $targetOrgId"))
        }

        val institution = database.institutionDao().getInstitutionById(targetOrgId)
            ?: return@withContext Result.failure(IllegalStateException("Institution not found."))

        sessionManager.switchInstitution(targetOrgId, membership.userId, membership.membershipId)

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = targetOrgId,
                userId = membership.userId,
                userName = "User",
                action = "SWITCH_INSTITUTION",
                resource = "SESSION",
                oldValue = null,
                newValue = "Entered environment: ${institution.name} ($targetOrgId)",
                result = "SUCCESS"
            )
        )

        Result.success(Unit)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val currentUserId = sessionManager.currentUserId.value
        val orgId = sessionManager.activeOrganizationId.value ?: "GLOBAL"
        if (currentUserId != null) {
            database.auditLogDao().insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    organizationId = orgId,
                    userId = currentUserId,
                    userName = "User",
                    action = "SIGN_OUT",
                    resource = "AUTH",
                    oldValue = "Active Session",
                    newValue = "Signed Out",
                    result = "SUCCESS"
                )
            )
        }
        firebaseAuthService.getFirebaseAuth()?.signOut()
        sessionManager.clearSession()
    }

    /**
     * Normalizes a raw phone number into standard international E.164 format.
     * Retains international country dial codes (e.g., +1, +44, +971, +91).
     */
    fun normalizePhoneNumber(phone: String): String {
        val trimmed = phone.trim()
        val cleaned = trimmed.replace(Regex("[^0-9+]"), "")
        return when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("91") && cleaned.length == 12 -> "+$cleaned"
            cleaned.startsWith("1") && cleaned.length == 11 -> "+$cleaned"
            cleaned.length == 10 -> "+91$cleaned" // Default backward compatibility for 10-digit Indian numbers
            else -> if (cleaned.isNotBlank()) "+$cleaned" else ""
        }
    }

    /**
     * Validates whether a phone number matches standard E.164 international notation.
     * Requires a '+' prefix followed by 7 to 15 digits (ITU-T E.164).
     */
    fun isValidPhoneNumber(normalized: String): Boolean {
        return normalized.matches(Regex("^\\+[1-9]\\d{6,14}$"))
    }

    /**
     * Masks an international phone number for secure UI display.
     * Displays country code and trailing digits while hiding intermediate digits.
     */
    fun maskPhoneNumber(normalized: String): String {
        val digits = normalized.replace("+", "")
        return if (digits.length >= 8) {
            val prefixLen = if (normalized.startsWith("+91")) 5 else 4
            val prefix = normalized.take(prefixLen)
            val suffix = normalized.takeLast(2)
            "$prefix •••• ••$suffix"
        } else {
            normalized
        }
    }
}
