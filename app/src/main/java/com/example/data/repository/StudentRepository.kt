package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.InstitutionMembershipEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.data.local.entity.StudentEntity
import com.example.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class StudentRepository(private val database: AppDatabase) {

    fun getStudentsForClass(orgId: String, grade: String, section: String): Flow<List<StudentEntity>> {
        return database.studentDao().getStudentsForClass(orgId, grade, section)
    }

    fun getAllStudentsForOrg(orgId: String): Flow<List<StudentEntity>> {
        return database.studentDao().getAllStudentsForOrg(orgId)
    }

    suspend fun getStudentProfile(studentId: String): StudentEntity? = withContext(Dispatchers.IO) {
        database.studentDao().getStudentProfile(studentId)
    }

    fun observeStudentProfile(studentId: String): Flow<StudentEntity?> {
        return database.studentDao().observeStudentProfile(studentId)
    }

    suspend fun enrollStudent(
        actorUser: StaffUserEntity,
        activeOrgId: String,
        name: String,
        studentId: String,
        rollNumber: String,
        grade: String,
        section: String,
        studentPhone: String,
        parentName: String,
        parentPhone: String,
        emergencyContact: String
    ): Result<StudentEntity> = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanStudentId = studentId.trim()
        val cleanRoll = rollNumber.trim()
        val cleanGrade = grade.trim()
        val cleanSection = section.trim()
        val cleanPhone = if (studentPhone.startsWith("+91")) studentPhone.trim() else "+91${studentPhone.replace(Regex("[^0-9]"), "").takeLast(10)}"

        if (cleanName.isBlank() || cleanStudentId.isBlank() || cleanRoll.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Student name, ID, and roll number are mandatory."))
        }

        // 1. Verify actor has active membership in this organization
        val actorMembership = database.institutionMembershipDao().getMembershipForPhoneAndOrg(actorUser.phoneNumber, activeOrgId)
            ?: return@withContext Result.failure(SecurityException("Security Violation: You do not possess an active membership in this institution."))

        val actorRole = UserRole.fromString(actorMembership.role)

        // 2. Class Teacher Authorization Check
        if (actorRole == UserRole.TEACHER) {
            val isAuthorizedClassTeacher = actorMembership.isClassTeacher &&
                    actorMembership.classTeacherGrade.equals(cleanGrade, ignoreCase = true) &&
                    actorMembership.classTeacherSection.equals(cleanSection, ignoreCase = true)

            if (!isAuthorizedClassTeacher) {
                // Log security audit violation
                database.auditLogDao().insertAuditLog(
                    AuditLogEntity(
                        id = UUID.randomUUID().toString(),
                        organizationId = activeOrgId,
                        userId = actorUser.id,
                        userName = actorUser.name,
                        action = "UNAUTHORIZED_STUDENT_ENROLLMENT_ATTEMPT",
                        resource = "CLASS_${cleanGrade}_${cleanSection}",
                        oldValue = "Actor Class Teacher of: ${actorMembership.classTeacherGrade}-${actorMembership.classTeacherSection}",
                        newValue = "Attempted enrollment for: $cleanGrade-$cleanSection",
                        result = "DENIED"
                    )
                )

                val assignedMsg = if (actorMembership.isClassTeacher) {
                    "Class ${actorMembership.classTeacherGrade}-${actorMembership.classTeacherSection}"
                } else {
                    "no class (Subject Teacher only)"
                }
                return@withContext Result.failure(
                    SecurityException("Authorization Denied: You are assigned as Class Teacher for $assignedMsg. You cannot enroll students in Class $cleanGrade-$cleanSection.")
                )
            }
        } else if (actorRole !in listOf(UserRole.ADMIN, UserRole.PRINCIPAL, UserRole.SUPER_ADMIN)) {
            return@withContext Result.failure(SecurityException("Authorization Denied: Only assigned Class Teachers or Administrators can enroll students."))
        }

        // 3. Duplicate / Fraud Protection Check
        val existingWithRoll = database.studentDao().getStudentByRoll(activeOrgId, cleanGrade, cleanSection, cleanRoll)
        if (existingWithRoll != null) {
            return@withContext Result.failure(
                IllegalStateException("Enrollment Conflict: A student with Roll Number '$cleanRoll' already exists in Class $cleanGrade-$cleanSection (${existingWithRoll.name}).")
            )
        }

        val existingWithId = database.studentDao().getStudentById(activeOrgId, cleanStudentId)
        if (existingWithId != null) {
            return@withContext Result.failure(
                IllegalStateException("Enrollment Conflict: A student with ID '$cleanStudentId' is already registered in this institution (${existingWithId.name}).")
            )
        }

        // 4. Insert Student Record
        val student = StudentEntity(
            studentId = cleanStudentId,
            organizationId = activeOrgId,
            name = cleanName,
            rollNumber = cleanRoll,
            classGrade = cleanGrade,
            section = cleanSection,
            phoneNumber = cleanPhone,
            parentName = parentName.trim(),
            parentPhone = parentPhone.trim(),
            emergencyContact = emergencyContact.trim(),
            status = "ACTIVE",
            enrolledByTeacherId = actorUser.id,
            enrollmentDate = System.currentTimeMillis()
        )
        database.studentDao().insertStudent(student)

        // 5. Create or Update Student's Institution Membership
        val existingMembership = database.institutionMembershipDao().getMembershipForPhoneAndOrg(cleanPhone, activeOrgId)
        if (existingMembership == null) {
            val studentMembership = InstitutionMembershipEntity(
                membershipId = UUID.randomUUID().toString(),
                userId = cleanStudentId,
                phoneNumber = cleanPhone,
                organizationId = activeOrgId,
                role = "STUDENT",
                status = "ACTIVE",
                createdBy = actorUser.id
            )
            database.institutionMembershipDao().insertMembership(studentMembership)
        }

        // 6. Record Audit Log
        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = activeOrgId,
                userId = actorUser.id,
                userName = actorUser.name,
                action = "STUDENT_ENROLLED",
                resource = "STUDENT_${cleanStudentId}",
                oldValue = null,
                newValue = "Name: $cleanName, Roll: $cleanRoll, Class: $cleanGrade-$cleanSection, Enrolled by: ${actorUser.name}",
                result = "SUCCESS"
            )
        )

        // 7. Send In-App Notification
        database.notificationDao().insertNotification(
            NotificationEntity(
                id = UUID.randomUUID().toString(),
                organizationId = activeOrgId,
                targetStaffId = actorUser.id,
                title = "Student Enrolled: $cleanName",
                message = "Successfully registered student $cleanName (Roll $cleanRoll) in Class $cleanGrade-$cleanSection.",
                type = "PRINCIPAL_NOTICE",
                relatedRecordId = cleanStudentId
            )
        )

        Result.success(student)
    }

    suspend fun assignClassTeacher(
        actorUser: StaffUserEntity,
        activeOrgId: String,
        newTeacherId: String,
        grade: String,
        section: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val actorMembership = database.institutionMembershipDao().getMembershipForPhoneAndOrg(actorUser.phoneNumber, activeOrgId)
            ?: return@withContext Result.failure(SecurityException("Unauthorized access"))

        val actorRole = UserRole.fromString(actorMembership.role)
        if (actorRole !in listOf(UserRole.ADMIN, UserRole.PRINCIPAL, UserRole.SUPER_ADMIN)) {
            return@withContext Result.failure(SecurityException("Only Administrators or the Principal can reassign Class Teachers."))
        }

        val targetTeacher = database.staffDao().getStaffById(newTeacherId, activeOrgId)
            ?: return@withContext Result.failure(IllegalArgumentException("Target faculty member not found in this institution."))

        // 1. Revoke existing Class Teacher for this class and section
        val existingClassTeacherMembership = database.institutionMembershipDao().getClassTeacherMembership(activeOrgId, grade, section)
        if (existingClassTeacherMembership != null) {
            database.institutionMembershipDao().updateMembership(
                existingClassTeacherMembership.copy(
                    isClassTeacher = false,
                    classTeacherGrade = null,
                    classTeacherSection = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // 2. Assign new Class Teacher
        val targetMembership = database.institutionMembershipDao().getMembershipForPhoneAndOrg(targetTeacher.phoneNumber, activeOrgId)
        if (targetMembership != null) {
            database.institutionMembershipDao().updateMembership(
                targetMembership.copy(
                    isClassTeacher = true,
                    classTeacherGrade = grade,
                    classTeacherSection = section,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = activeOrgId,
                userId = actorUser.id,
                userName = actorUser.name,
                action = "CLASS_TEACHER_REASSIGNED",
                resource = "CLASS_${grade}_$section",
                oldValue = "Old Class Teacher: ${existingClassTeacherMembership?.userId ?: "None"}",
                newValue = "New Class Teacher: ${targetTeacher.name} ($newTeacherId)",
                result = "SUCCESS"
            )
        )

        Result.success(Unit)
    }

    suspend fun deactivateStudentMembership(
        actorUser: StaffUserEntity,
        activeOrgId: String,
        studentId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val student = database.studentDao().getStudentById(activeOrgId, studentId)
            ?: return@withContext Result.failure(IllegalArgumentException("Student not found"))

        database.studentDao().updateStudent(student.copy(status = "INACTIVE", updatedAt = System.currentTimeMillis()))

        val membership = database.institutionMembershipDao().getMembershipForPhoneAndOrg(student.phoneNumber, activeOrgId)
        if (membership != null) {
            database.institutionMembershipDao().updateMembership(membership.copy(status = "INACTIVE", updatedAt = System.currentTimeMillis()))
        }

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = activeOrgId,
                userId = actorUser.id,
                userName = actorUser.name,
                action = "STUDENT_DEACTIVATED",
                resource = "STUDENT_$studentId",
                oldValue = "Status: ACTIVE",
                newValue = "Status: INACTIVE",
                result = "SUCCESS"
            )
        )

        Result.success(Unit)
    }
}
