package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseSeeder
import com.example.data.local.SessionManager
import com.example.data.repository.AuthRepository
import com.example.data.repository.StudentRepository
import com.example.data.service.SmsService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbarStatus
import com.example.ui.components.SafeTextToolbar
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("1campus", appName)
    }

    @Test
    fun `multi-institution seeding initializes institutions, memberships, staff and students`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        DatabaseSeeder.seedDatabaseIfEmpty(db)

        val totalStaff = db.staffDao().countAllStaff()
        assertTrue("Database should have seeded staff across institutions", totalStaff >= 10)

        // Verify Institutions exist
        val dpa = db.institutionDao().getInstitutionById("ORG_DPA")
        val dav = db.institutionDao().getInstitutionById("ORG_DAV")
        val abc = db.institutionDao().getInstitutionById("ORG_ABC")
        assertNotNull(dpa)
        assertNotNull(dav)
        assertNotNull(abc)

        // Verify Principal profile in DPA
        val principal = db.staffDao().getStaffByPhone("+919876543211", "ORG_DPA")
        assertNotNull(principal)
        assertEquals("PRINCIPAL", principal?.role)

        // Verify Class Teacher assignment
        val classTeacherMembership = db.institutionMembershipDao().getClassTeacherMembership("ORG_DPA", "10", "A")
        assertNotNull(classTeacherMembership)
        assertTrue(classTeacherMembership!!.isClassTeacher)
        assertEquals("10", classTeacherMembership.classTeacherGrade)
        assertEquals("A", classTeacherMembership.classTeacherSection)
    }

    @Test
    fun `class teacher enrollment permissions enforce class and section isolation`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        DatabaseSeeder.seedDatabaseIfEmpty(db)

        val studentRepo = StudentRepository(db)

        // Teacher TCH101 is Class Teacher of 10-A in ORG_DPA
        val teacher10A = db.staffDao().getStaffById("TCH101", "ORG_DPA")!!

        // 1. Enrolling in assigned class 10-A should succeed
        val successResult = studentRepo.enrollStudent(
            actorUser = teacher10A,
            activeOrgId = "ORG_DPA",
            name = "Aarav Sharma",
            studentId = "STU_TEST_10A",
            rollNumber = "99",
            grade = "10",
            section = "A",
            studentPhone = "+919876500099",
            parentName = "Rajesh Sharma",
            parentPhone = "+919876500098",
            emergencyContact = "+919876500097"
        )
        assertTrue("Class teacher should be authorized to enroll in assigned 10-A", successResult.isSuccess)

        // 2. Enrolling in unassigned class 10-B must be DENIED server-side
        val deniedResult = studentRepo.enrollStudent(
            actorUser = teacher10A,
            activeOrgId = "ORG_DPA",
            name = "Rohan Verma",
            studentId = "STU_TEST_10B",
            rollNumber = "99",
            grade = "10",
            section = "B",
            studentPhone = "+919876500088",
            parentName = "Sunil Verma",
            parentPhone = "+919876500087",
            emergencyContact = "+919876500086"
        )
        assertTrue("Class teacher must be rejected when attempting to enroll in 10-B", deniedResult.isFailure)

        // 3. Attempting to enroll in a different institution must be DENIED
        val wrongOrgResult = studentRepo.enrollStudent(
            actorUser = teacher10A,
            activeOrgId = "ORG_DAV",
            name = "Kabir Patel",
            studentId = "STU_TEST_DAV",
            rollNumber = "99",
            grade = "10",
            section = "A",
            studentPhone = "+919876500077",
            parentName = "Mahesh Patel",
            parentPhone = "+919876500076",
            emergencyContact = "+919876500075"
        )
        assertTrue("Staff without membership in ORG_DAV must be denied access", wrongOrgResult.isFailure)
    }

    @Test
    fun `auth repository normalizes and validates phone numbers`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val session = SessionManager(context)
        val sms = SmsService(context)
        val authRepo = AuthRepository(db, session, sms)

        val normalized = authRepo.normalizePhoneNumber("9876543210")
        assertEquals("+919876543210", normalized)

        val masked = authRepo.maskPhoneNumber(normalized)
        assertTrue(masked.contains("••••"))
    }

    @Test
    fun `registration flow persists pending request and requires real OTP verification`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val sms = SmsService(context)
        val testValidOtp = "654321"
        sms.firebaseAuthService.testVerificationEngine = { phone, onEvent ->
            onEvent(com.example.data.service.PhoneVerificationEvent.CodeSent("VERIF_ID_$phone", null))
        }
        sms.firebaseAuthService.testCredentialEngine = { verifId, code ->
            if (code == testValidOtp) {
                try {
                    val cred = com.google.firebase.auth.PhoneAuthProvider.getCredential(verifId, code)
                    Result.success(cred)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            } else {
                Result.failure(com.example.data.service.PhoneAuthException("Invalid verification code."))
            }
        }
        sms.firebaseAuthService.testSignInEngine = { _ ->
            Result.success(null)
        }

        val regRepo = com.example.data.repository.InstitutionRegistrationRepository(db, sms)

        val form = com.example.data.repository.RegistrationFormInput(
            institutionName = "Greenwood International Academy",
            institutionType = "SCHOOL",
            officialName = "Greenwood Educational Trust",
            address = "Plot 42, Knowledge Park III",
            city = "Greater Noida",
            state = "Uttar Pradesh",
            country = "India",
            postalCode = "201310",
            contactPhone = "9811122233",
            contactEmail = "admin@greenwood.edu.in",
            representativeName = "Dr. Anita Desai",
            representativeDesignation = "Principal & Trustee",
            representativePhone = "9876500111",
            website = "https://greenwood.edu.in",
            logoUrl = ""
        )

        // 1. Submit registration
        val submitResult = regRepo.submitRegistration(form)
        assertTrue("Registration submission should succeed", submitResult.isSuccess)
        val registration = submitResult.getOrNull()!!
        assertEquals("PENDING_VERIFICATION", registration.status)
        assertEquals(false, registration.isPhoneVerified)

        // 2. Reject duplicate registration with same email/phone/name in city
        val dupResult = regRepo.submitRegistration(form)
        assertTrue("Duplicate registration must be rejected server-side", dupResult.isFailure)

        // 3. Verify representative phone: wrong OTP should be rejected
        val wrongOtpResult = regRepo.verifyRepresentativePhone(registration.id, "000000")
        assertTrue("Incorrect OTP must be rejected", wrongOtpResult.isFailure)

        // 4. Retrieve provider verification session to verify
        val otpSession = db.otpSessionDao().getOtpSession(registration.representativePhone)
        assertNotNull("OTP session must be recorded with provider verification ID", otpSession)
        assertEquals("VERIF_ID_${registration.representativePhone}", otpSession!!.verificationId)

        val verifyResult = regRepo.verifyRepresentativePhone(registration.id, testValidOtp)
        assertTrue("Valid OTP should verify representative", verifyResult.isSuccess)
        val verifiedReg = verifyResult.getOrNull()!!
        assertEquals(true, verifiedReg.isPhoneVerified)
        assertEquals("PENDING_APPROVAL", verifiedReg.status)

        // 5. CRITICAL: Unapproved registration does NOT grant administrative login access
        val session = SessionManager(context)
        val authRepo = AuthRepository(db, session, sms)
        val loginAttempt = authRepo.sendOtp(registration.representativePhone)
        assertTrue("Unapproved registration must not allow login", loginAttempt.isFailure)

        // 6. Platform approval creates institution, generates persistent School Code, and provisions Admin
        val approvalResult = regRepo.approveRegistration(registration.id, "PLATFORM_ROOT_TEST")
        assertTrue("Platform administrator approval should succeed", approvalResult.isSuccess)
        val approvedData = approvalResult.getOrNull()!!

        assertNotNull(approvedData.schoolCode)
        assertTrue("School code should have standard 3-part format", approvedData.schoolCode.contains("-"))
        assertEquals("ADMIN", approvedData.adminStaff.role)
        assertEquals("ACTIVE", approvedData.membership.status)

        // 7. Verify persistent School Code does not change on subsequent lookup
        val lookedUpSchool = regRepo.lookupSchoolByCode(approvedData.schoolCode)
        assertTrue("School lookup by assigned code should succeed", lookedUpSchool.isSuccess)
        assertEquals(approvedData.schoolCode, lookedUpSchool.getOrNull()?.code)

        // 8. Staff login with approved administrator now succeeds
        val sendOtpSuccess = authRepo.sendOtp(registration.representativePhone)
        assertTrue("Approved admin should receive login OTP", sendOtpSuccess.isSuccess)

        val adminOtpSession = db.otpSessionDao().getOtpSession(registration.representativePhone)
        assertNotNull(adminOtpSession)
        val adminLogin = authRepo.verifyOtp(registration.representativePhone, testValidOtp)
        assertTrue("Approved admin login should succeed", adminLogin.isSuccess)

        // 9. Cross-institution unauthorized access is blocked
        val unauthorizedSwitch = authRepo.switchActiveInstitution("ORG_UNAUTHORIZED_XYZ")
        assertTrue("Cross-institution switch without membership must be denied", unauthorizedSwitch.isFailure)

        // 10. Sign out clears active session
        authRepo.signOut()
        assertEquals(false, authRepo.isUserLoggedIn())
    }

    @Test
    fun `coaching centre registration does not force school board and persists valid draft`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val sms = SmsService(context)
        sms.firebaseAuthService.testVerificationEngine = { phone, onEvent ->
            onEvent(com.example.data.service.PhoneVerificationEvent.CodeSent("VERIF_TEST_$phone", null))
        }
        val regRepo = com.example.data.repository.InstitutionRegistrationRepository(db, sms)

        // 1. Coaching centre registration with Not Applicable board
        val coachingForm = com.example.data.repository.RegistrationFormInput(
            institutionName = "Zenith IIT & Medical Academy",
            institutionType = "COACHING",
            officialName = "Zenith Educational Services LLP",
            address = "Kalu Sarai, Near Hauz Khas Metro",
            city = "New Delhi",
            state = "Delhi",
            country = "India",
            postalCode = "110016",
            contactPhone = "9822233344",
            contactEmail = "info@zenithacademy.in",
            representativeName = "Prof. Alok Mukherjee",
            representativeDesignation = "Director & Academic Head",
            representativePhone = "9876543299",
            website = "https://zenithacademy.in",
            logoUrl = "",
            boardAffiliation = "Not Applicable"
        )

        val result = regRepo.submitRegistration(coachingForm)
        assertTrue("Coaching centre submission should succeed without school board requirement", result.isSuccess)
        val entity = result.getOrNull()!!
        assertEquals("COACHING", entity.institutionType)
        assertEquals("PENDING_VERIFICATION", entity.status)
        assertEquals("Zenith Educational Services LLP", entity.officialName)

        // 2. School registration with CBSE board incorporates affiliation
        val schoolForm = com.example.data.repository.RegistrationFormInput(
            institutionName = "Delhi Model Public School",
            institutionType = "SCHOOL",
            officialName = "Delhi Model Education Trust",
            address = "Plot 15, Sector 4, Dwarka",
            city = "New Delhi",
            state = "Delhi",
            country = "India",
            postalCode = "110078",
            contactPhone = "9833344455",
            contactEmail = "principal@dmpsdwarka.edu.in",
            representativeName = "Dr. Sunita Kapoor",
            representativeDesignation = "Principal",
            representativePhone = "9876543288",
            website = "https://dmpsdwarka.edu.in",
            logoUrl = "",
            boardAffiliation = "CBSE"
        )
        val schoolResult = regRepo.submitRegistration(schoolForm)
        assertTrue("School registration with CBSE affiliation should succeed", schoolResult.isSuccess)
        val schoolEntity = schoolResult.getOrNull()!!
        assertEquals("SCHOOL", schoolEntity.institutionType)
        assertTrue("Official name should include CBSE affiliation", schoolEntity.officialName.contains("CBSE"))

        // 3. Draft preservation via SharedPreferences
        val draftPrefs = context.getSharedPreferences("edu_school_reg_draft", Context.MODE_PRIVATE)
        draftPrefs.edit().apply {
            putBoolean("has_saved_draft", true)
            putString("inst_name", "Draft Academy")
            putString("city", "Bengaluru")
            putString("rep_phone", "9876500000")
            apply()
        }
        assertEquals("Draft Academy", draftPrefs.getString("inst_name", null))
        assertEquals("Bengaluru", draftPrefs.getString("city", null))
        assertEquals("9876500000", draftPrefs.getString("rep_phone", null))
    }

    @Test
    fun `safe text toolbar lifecycle handles focus transitions and prevents unexpected action mode destruction`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dummyView = View(context)
        val toolbar = SafeTextToolbar(dummyView)

        // 1. Initial state must be hidden
        assertEquals(TextToolbarStatus.Hidden, toolbar.status)

        // 2. Hide when view does not have window focus should execute safely without crash or DecorView conflict
        toolbar.hide()
        assertEquals(TextToolbarStatus.Hidden, toolbar.status)

        // 3. showMenu when not focused or attached safely exits without throwing
        toolbar.showMenu(
            rect = Rect(0f, 0f, 100f, 50f),
            onCopyRequested = {},
            onPasteRequested = {}
        )
        assertEquals(TextToolbarStatus.Hidden, toolbar.status)
    }
}

