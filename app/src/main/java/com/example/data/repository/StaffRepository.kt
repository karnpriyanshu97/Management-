package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.InstitutionMembershipEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.model.StaffStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class StaffLiveAvailability(
    val staff: StaffUserEntity,
    val status: StaffStatus,
    val currentActivityDetails: String, // e.g. "Teaching 10-A English in Room 401"
    val operationalLocation: String     // e.g. "Room 401", "Staff Room", "Conference Suite"
)

class StaffRepository(private val database: AppDatabase) {

    fun getAllStaff(orgId: String): Flow<List<StaffUserEntity>> = database.staffDao().getAllStaff(orgId)
    fun getActiveStaff(orgId: String): Flow<List<StaffUserEntity>> = database.staffDao().getActiveStaff(orgId)

    suspend fun getStaffById(id: String, orgId: String): StaffUserEntity? = withContext(Dispatchers.IO) {
        database.staffDao().getStaffById(id, orgId)
    }

    suspend fun setLeaveStatus(id: String, orgId: String, onLeave: Boolean, actor: StaffUserEntity) = withContext(Dispatchers.IO) {
        val target = database.staffDao().getStaffById(id, orgId) ?: return@withContext
        database.staffDao().setLeaveStatus(id, orgId, onLeave)

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = orgId,
                userId = actor.id,
                userName = actor.name,
                action = if (onLeave) "MARK_STAFF_LEAVE" else "REVOKE_STAFF_LEAVE",
                resource = "STAFF_${target.id}",
                oldValue = "OnLeave: ${target.isOnLeave}",
                newValue = "OnLeave: $onLeave",
                result = "SUCCESS"
            )
        )
    }

    suspend fun updateStaffDetails(staff: StaffUserEntity, actor: StaffUserEntity) = withContext(Dispatchers.IO) {
        val oldStaff = database.staffDao().getStaffById(staff.id, staff.organizationId)
        database.staffDao().updateStaff(staff)

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = staff.organizationId,
                userId = actor.id,
                userName = actor.name,
                action = "UPDATE_STAFF_PROFILE",
                resource = "STAFF_${staff.id}",
                oldValue = "Role: ${oldStaff?.role}, Dept: ${oldStaff?.department}",
                newValue = "Role: ${staff.role}, Dept: ${staff.department}, Active: ${staff.isActive}",
                result = "SUCCESS"
            )
        )
    }

    suspend fun createStaff(staff: StaffUserEntity, actor: StaffUserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = database.staffDao().getStaffByPhone(staff.phoneNumber, staff.organizationId)
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("Staff member with phone ${staff.phoneNumber} already exists in this institution."))
        }
        database.staffDao().insertStaff(staff)

        // Create membership
        val membership = InstitutionMembershipEntity(
            membershipId = UUID.randomUUID().toString(),
            userId = staff.id,
            phoneNumber = staff.phoneNumber,
            organizationId = staff.organizationId,
            role = staff.role,
            isClassTeacher = false,
            status = "ACTIVE",
            createdBy = actor.id
        )
        database.institutionMembershipDao().insertMembership(membership)

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = staff.organizationId,
                userId = actor.id,
                userName = actor.name,
                action = "CREATE_STAFF_MEMBER",
                resource = "STAFF_${staff.id}",
                oldValue = null,
                newValue = "Name: ${staff.name}, Phone: ${staff.phoneNumber}, Role: ${staff.role}",
                result = "SUCCESS"
            )
        )
        Result.success(Unit)
    }

    suspend fun evaluateStaffLiveStatus(
        staff: StaffUserEntity,
        dayOfWeek: Int,
        currentPeriodNumber: Int?,
        dateStr: String = LocalDate.now().toString(),
        currentTime: LocalTime = LocalTime.now()
    ): StaffLiveAvailability = withContext(Dispatchers.IO) {
        if (!staff.isActive) {
            return@withContext StaffLiveAvailability(
                staff = staff,
                status = StaffStatus.ON_LEAVE,
                currentActivityDetails = "Inactive Profile",
                operationalLocation = "Off Campus"
            )
        }

        if (staff.isOnLeave) {
            return@withContext StaffLiveAvailability(
                staff = staff,
                status = StaffStatus.ON_LEAVE,
                currentActivityDetails = "Marked Absent / On Approved Leave Today",
                operationalLocation = "Off Duty"
            )
        }

        // Check if in an active meeting at currentTime
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        val meetings = database.meetingDao().getMeetingsForDate(staff.organizationId, dateStr)
        val staffMeeting = meetings.firstOrNull { meeting ->
            meeting.status != "CANCELLED" &&
                    (meeting.organizerId == staff.id || meeting.participantIds.split(",").map { it.trim() }.contains(staff.id)) &&
                    runCatching {
                        val mStart = LocalTime.parse(meeting.startTime, timeFmt)
                        val mEnd = LocalTime.parse(meeting.endTime, timeFmt)
                        (currentTime == mStart || currentTime.isAfter(mStart)) && currentTime.isBefore(mEnd)
                    }.getOrDefault(false)
        }

        if (staffMeeting != null) {
            return@withContext StaffLiveAvailability(
                staff = staff,
                status = StaffStatus.IN_MEETING,
                currentActivityDetails = "In Meeting: ${staffMeeting.title}",
                operationalLocation = staffMeeting.location
            )
        }

        if (currentPeriodNumber == null) {
            return@withContext StaffLiveAvailability(
                staff = staff,
                status = StaffStatus.FREE,
                currentActivityDetails = "School Hours Transition / Break",
                operationalLocation = "Campus / Staff Room"
            )
        }

        // Check replacement duty
        val replacement = database.replacementDao().getTeacherApprovedReplacementSlot(staff.organizationId, dateStr, currentPeriodNumber, staff.id)
        if (replacement != null) {
            return@withContext StaffLiveAvailability(
                staff = staff,
                status = StaffStatus.REPLACEMENT,
                currentActivityDetails = "Substitution: Class ${replacement.grade}-${replacement.section} (${replacement.subject})",
                operationalLocation = "Class ${replacement.grade}-${replacement.section}"
            )
        }

        // Check active teaching class in timetable
        val timetableSlot = database.timetableDao().getTeacherActiveSlot(staff.organizationId, staff.id, dayOfWeek, currentPeriodNumber)
        if (timetableSlot != null) {
            return@withContext StaffLiveAvailability(
                staff = staff,
                status = StaffStatus.TEACHING,
                currentActivityDetails = "Teaching: Class ${timetableSlot.grade}-${timetableSlot.section} • ${timetableSlot.subject}",
                operationalLocation = timetableSlot.roomNo
            )
        }

        // Otherwise Free
        return@withContext StaffLiveAvailability(
            staff = staff,
            status = StaffStatus.FREE,
            currentActivityDetails = "No assigned class in Period $currentPeriodNumber",
            operationalLocation = "Staff Common Room"
        )
    }
}
