package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.ClassroomVerificationEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.StaffUserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

class VerificationRepository(private val database: AppDatabase) {

    fun getAllVerifications(orgId: String): Flow<List<ClassroomVerificationEntity>> {
        return database.classroomVerificationDao().getAllVerifications(orgId)
    }

    fun getVerificationsForDateAndPeriod(
        orgId: String,
        date: String = LocalDate.now().toString(),
        periodNumber: Int
    ): Flow<List<ClassroomVerificationEntity>> {
        return database.classroomVerificationDao().getVerificationsForDateAndPeriod(orgId, date, periodNumber)
    }

    suspend fun getVerification(
        orgId: String,
        grade: String,
        section: String,
        periodNumber: Int,
        date: String = LocalDate.now().toString()
    ): ClassroomVerificationEntity? = withContext(Dispatchers.IO) {
        database.classroomVerificationDao().getVerification(orgId, date, periodNumber, grade, section)
    }

    suspend fun submitClassroomVerification(
        orgId: String,
        teacher: StaffUserEntity,
        grade: String,
        section: String,
        periodNumber: Int,
        typedRoomNo: String,
        scheduledRoomNo: String,
        photoPath: String? = null,
        date: String = LocalDate.now().toString()
    ): Result<ClassroomVerificationEntity> = withContext(Dispatchers.IO) {
        if (typedRoomNo.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Typed Room Number is required for verification."))
        }

        val cleanTyped = typedRoomNo.trim()
        val cleanScheduled = scheduledRoomNo.trim()
        val isMismatch = !cleanTyped.equals(cleanScheduled, ignoreCase = true) &&
                !cleanTyped.contains(cleanScheduled.replace("Room", "").trim(), ignoreCase = true)

        val status = if (isMismatch) "MISMATCH_FLAGGED" else "VERIFIED"
        val recordId = UUID.randomUUID().toString()

        val verification = ClassroomVerificationEntity(
            id = recordId,
            organizationId = orgId,
            date = date,
            periodNumber = periodNumber,
            grade = grade,
            section = section,
            teacherId = teacher.id,
            typedRoomNo = cleanTyped,
            scheduledRoomNo = cleanScheduled,
            status = status,
            photoPath = photoPath,
            ocrExtractedText = cleanTyped,
            timestamp = System.currentTimeMillis()
        )

        database.classroomVerificationDao().insertVerification(verification)

        // Audit Log
        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = orgId,
                userId = teacher.id,
                userName = teacher.name,
                action = if (isMismatch) "CLASS_VERIFIED_MISMATCH" else "CLASS_VERIFIED_CONFIRMED",
                resource = "CLASS_${grade}_$section",
                oldValue = "Scheduled: $scheduledRoomNo",
                newValue = "Verified: $cleanTyped (Status: $status)",
                result = "SUCCESS"
            )
        )

        // If mismatch or flagged, notify administrators
        if (isMismatch) {
            database.notificationDao().insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    organizationId = orgId,
                    targetStaffId = "ALL",
                    title = "Room Location Alert: Class $grade-$section",
                    message = "${teacher.name} verified Class $grade-$section in '$cleanTyped' instead of scheduled '$scheduledRoomNo'.",
                    type = "CLASSROOM_VERIFICATION_ALERT",
                    relatedRecordId = recordId
                )
            )
        }

        Result.success(verification)
    }
}
