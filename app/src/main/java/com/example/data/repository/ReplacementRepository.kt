package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.ReplacementDutyEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.data.service.AiRecommendationResult
import com.example.data.service.AiReplacementEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

class ReplacementRepository(
    private val database: AppDatabase,
    private val aiEngine: AiReplacementEngine
) {
    private val assignmentMutex = Mutex()

    fun getAllReplacements(orgId: String): Flow<List<ReplacementDutyEntity>> {
        return database.replacementDao().getAllReplacements(orgId)
    }

    fun getReplacementsForDate(orgId: String, date: String = LocalDate.now().toString()): Flow<List<ReplacementDutyEntity>> {
        return database.replacementDao().getReplacementsForDate(orgId, date)
    }

    fun getReplacementsForTeacher(orgId: String, teacherId: String, date: String = LocalDate.now().toString()): Flow<List<ReplacementDutyEntity>> {
        return database.replacementDao().getReplacementsForTeacher(orgId, teacherId, date)
    }

    suspend fun getAiRecommendation(
        orgId: String,
        absentTeacherId: String,
        grade: String,
        section: String,
        subject: String,
        periodNumber: Int,
        dayOfWeek: Int,
        date: String = LocalDate.now().toString()
    ): Result<AiRecommendationResult> = withContext(Dispatchers.IO) {
        val absentTeacher = database.staffDao().getStaffById(absentTeacherId, orgId)
            ?: return@withContext Result.failure(IllegalArgumentException("Absent teacher record not found in this institution"))

        val result = aiEngine.findAndRankReplacements(
            absentTeacher = absentTeacher,
            grade = grade,
            section = section,
            subject = subject,
            periodNumber = periodNumber,
            dayOfWeek = dayOfWeek,
            dateStr = date
        )
        Result.success(result)
    }

    suspend fun createAndApproveReplacement(
        orgId: String,
        absentTeacherId: String,
        assignedTeacherId: String,
        grade: String,
        section: String,
        subject: String,
        periodNumber: Int,
        dayOfWeek: Int,
        reason: String,
        actor: StaffUserEntity,
        date: String = LocalDate.now().toString()
    ): Result<ReplacementDutyEntity> = withContext(Dispatchers.IO) {
        assignmentMutex.withLock {
            val assignedTeacher = database.staffDao().getStaffById(assignedTeacherId, orgId)
                ?: return@withContext Result.failure(IllegalArgumentException("Assigned teacher not found in this institution"))

            val absentTeacher = database.staffDao().getStaffById(absentTeacherId, orgId)
                ?: return@withContext Result.failure(IllegalArgumentException("Absent teacher not found in this institution"))

            // Verify assigned teacher is not on leave
            if (assignedTeacher.isOnLeave || !assignedTeacher.isActive) {
                return@withContext Result.failure(IllegalStateException("${assignedTeacher.name} is on leave or inactive and cannot be assigned."))
            }

            // Verify teacher is not already teaching in this period
            val regularSlot = database.timetableDao().getTeacherActiveSlot(orgId, assignedTeacherId, dayOfWeek, periodNumber)
            if (regularSlot != null) {
                return@withContext Result.failure(IllegalStateException("${assignedTeacher.name} is already teaching Class ${regularSlot.grade}-${regularSlot.section} in Period $periodNumber."))
            }

            // Verify teacher is not already booked for another replacement in this period
            val existingDuty = database.replacementDao().getTeacherApprovedReplacementSlot(orgId, date, periodNumber, assignedTeacherId)
            if (existingDuty != null) {
                return@withContext Result.failure(IllegalStateException("${assignedTeacher.name} is already assigned to cover Class ${existingDuty.grade}-${existingDuty.section} in Period $periodNumber."))
            }

            val dutyId = UUID.randomUUID().toString()
            val duty = ReplacementDutyEntity(
                id = dutyId,
                organizationId = orgId,
                date = date,
                periodNumber = periodNumber,
                grade = grade,
                section = section,
                subject = subject,
                absentTeacherId = absentTeacherId,
                assignedTeacherId = assignedTeacherId,
                status = "APPROVED",
                aiRecommendationReason = reason,
                requestedBy = actor.id,
                approvedBy = actor.id
            )

            try {
                database.replacementDao().insertReplacement(duty)
            } catch (e: Exception) {
                return@withContext Result.failure(IllegalStateException("Failed to assign replacement: ${e.message}"))
            }

            // Record audit log
            database.auditLogDao().insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    organizationId = orgId,
                    userId = actor.id,
                    userName = actor.name,
                    action = "ASSIGN_REPLACEMENT_DUTY",
                    resource = "REPLACEMENT_$dutyId",
                    oldValue = "Absent: ${absentTeacher.name}",
                    newValue = "Assigned: ${assignedTeacher.name} to Class $grade-$section ($subject) Period $periodNumber",
                    result = "SUCCESS"
                )
            )

            // Notify replacement teacher
            database.notificationDao().insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    organizationId = orgId,
                    targetStaffId = assignedTeacherId,
                    title = "Replacement Duty Assigned",
                    message = "You have been assigned to cover Class $grade-$section ($subject) in Period $periodNumber for ${absentTeacher.name}.",
                    type = "REPLACEMENT_ASSIGNMENT",
                    relatedRecordId = dutyId
                )
            )

            // Notify absent teacher
            database.notificationDao().insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    organizationId = orgId,
                    targetStaffId = absentTeacherId,
                    title = "Class Coverage Arranged",
                    message = "${assignedTeacher.name} has been assigned to cover your Class $grade-$section in Period $periodNumber.",
                    type = "REPLACEMENT_STATUS",
                    relatedRecordId = dutyId
                )
            )

            Result.success(duty)
        }
    }
}
