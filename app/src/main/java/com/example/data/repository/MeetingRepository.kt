package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.MeetingEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.StaffUserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class MeetingRepository(private val database: AppDatabase) {

    fun getAllMeetings(orgId: String): Flow<List<MeetingEntity>> {
        return database.meetingDao().getAllMeetings(orgId)
    }

    suspend fun getMeetingsForDate(orgId: String, date: String = LocalDate.now().toString()): List<MeetingEntity> = withContext(Dispatchers.IO) {
        database.meetingDao().getMeetingsForDate(orgId, date)
    }

    suspend fun createMeeting(
        orgId: String,
        title: String,
        description: String,
        date: String,
        startTime: String,
        endTime: String,
        location: String,
        priority: String,
        instructions: String?,
        participantIds: List<String>,
        organizer: StaffUserEntity
    ): Result<MeetingEntity> = withContext(Dispatchers.IO) {
        if (title.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Meeting title is required."))
        }
        if (participantIds.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Please select at least one staff member to attend."))
        }

        // Validate time format and order
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        try {
            val start = LocalTime.parse(startTime, timeFmt)
            val end = LocalTime.parse(endTime, timeFmt)
            if (!end.isAfter(start)) {
                return@withContext Result.failure(IllegalArgumentException("End time must be after start time."))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(IllegalArgumentException("Invalid time format. Please use HH:mm."))
        }

        val meetingId = UUID.randomUUID().toString()
        val participantListStr = participantIds.joinToString(",")

        val meeting = MeetingEntity(
            id = meetingId,
            organizationId = orgId,
            title = title.trim(),
            description = description.trim(),
            date = date,
            startTime = startTime,
            endTime = endTime,
            location = location.trim(),
            organizerId = organizer.id,
            organizerName = organizer.name,
            participantIds = participantListStr,
            priority = priority,
            status = "SCHEDULED",
            instructions = instructions?.trim(),
            createdAt = System.currentTimeMillis()
        )

        database.meetingDao().insertMeeting(meeting)

        // Record Audit Log
        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = orgId,
                userId = organizer.id,
                userName = organizer.name,
                action = "CREATE_MEETING",
                resource = "MEETING_$meetingId",
                oldValue = null,
                newValue = "$title on $date from $startTime to $endTime with ${participantIds.size} attendees",
                result = "SUCCESS"
            )
        )

        // Send notifications to all selected participants
        for (pid in participantIds) {
            database.notificationDao().insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    organizationId = orgId,
                    targetStaffId = pid,
                    title = "Meeting Invitation: $title",
                    message = "${organizer.name} has scheduled '$title' on $date at $startTime at $location. Priority: $priority.",
                    type = "MEETING_INVITATION",
                    relatedRecordId = meetingId
                )
            )
        }

        Result.success(meeting)
    }

    suspend fun cancelMeeting(orgId: String, meetingId: String, actor: StaffUserEntity) = withContext(Dispatchers.IO) {
        val all = database.meetingDao().getMeetingsForDate(orgId, LocalDate.now().toString())
        val existing = all.firstOrNull { it.id == meetingId } ?: return@withContext
        val updated = existing.copy(status = "CANCELLED")
        database.meetingDao().updateMeeting(updated)

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = orgId,
                userId = actor.id,
                userName = actor.name,
                action = "CANCEL_MEETING",
                resource = "MEETING_$meetingId",
                oldValue = "Status: ${existing.status}",
                newValue = "Status: CANCELLED",
                result = "SUCCESS"
            )
        )

        // Notify participants
        val pids = existing.participantIds.split(",").map { it.trim() }
        for (pid in pids) {
            database.notificationDao().insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    organizationId = orgId,
                    targetStaffId = pid,
                    title = "Meeting Cancelled: ${existing.title}",
                    message = "The scheduled meeting '${existing.title}' for ${existing.date} at ${existing.startTime} has been cancelled.",
                    type = "MEETING_INVITATION",
                    relatedRecordId = meetingId
                )
            )
        }
    }
}
