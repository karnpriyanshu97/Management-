package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "institutions")
data class InstitutionEntity(
    @PrimaryKey val id: String, // e.g. "ORG_DPA", "ORG_DAV", "ORG_ABC"
    val name: String,
    val code: String,
    val type: String, // "SCHOOL", "COACHING"
    val address: String,
    val principalOrDirector: String,
    val academicSession: String = "2026-2027",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "institution_memberships",
    indices = [
        Index(value = ["phoneNumber", "organizationId"], unique = true),
        Index(value = ["organizationId", "role"])
    ]
)
data class InstitutionMembershipEntity(
    @PrimaryKey val membershipId: String,
    val userId: String,
    val phoneNumber: String,
    val organizationId: String,
    val role: String, // SUPER_ADMIN, PRINCIPAL, ADMIN, TEACHER, STAFF, STUDENT
    val isClassTeacher: Boolean = false,
    val classTeacherGrade: String? = null,
    val classTeacherSection: String? = null,
    val status: String = "ACTIVE", // ACTIVE, PENDING, INACTIVE
    val createdBy: String = "SYSTEM",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "students",
    indices = [
        Index(value = ["organizationId", "classGrade", "section", "rollNumber"], unique = true),
        Index(value = ["organizationId", "studentId"], unique = true),
        Index(value = ["phoneNumber"])
    ]
)
data class StudentEntity(
    @PrimaryKey val studentId: String,
    val organizationId: String,
    val name: String,
    val rollNumber: String,
    val classGrade: String,
    val section: String,
    val phoneNumber: String,
    val parentName: String = "",
    val parentPhone: String = "",
    val emergencyContact: String = "",
    val status: String = "ACTIVE", // ACTIVE, INACTIVE, TRANSFERRED
    val enrolledByTeacherId: String,
    val enrollmentDate: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "staff_users",
    indices = [
        Index(value = ["phoneNumber", "organizationId"], unique = true),
        Index(value = ["organizationId"])
    ]
)
data class StaffUserEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "ORG_DPA",
    val name: String,
    val phoneNumber: String,
    val role: String,
    val department: String,
    val subjects: String,
    val assignedClasses: String,
    val profilePhotoUrl: String = "",
    val isActive: Boolean = true,
    val isOnLeave: Boolean = false,
    val permissions: String = "",
    val createdDate: Long = System.currentTimeMillis(),
    val lastActiveDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "period_configs",
    indices = [Index(value = ["organizationId", "periodNumber"], unique = true)]
)
data class PeriodConfigEntity(
    @PrimaryKey val id: String, // "${organizationId}_${periodNumber}"
    val organizationId: String = "ORG_DPA",
    val periodNumber: Int,
    val label: String,
    val startTime: String, // HH:mm format
    val endTime: String,   // HH:mm format
    val isBreak: Boolean = false,
    val isLunch: Boolean = false
)

@Entity(
    tableName = "timetable_entries",
    indices = [
        Index(value = ["organizationId", "dayOfWeek", "periodNumber", "grade", "section"], unique = true),
        Index(value = ["organizationId", "dayOfWeek", "periodNumber", "teacherId"])
    ]
)
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: String = "ORG_DPA",
    val dayOfWeek: Int, // 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat
    val periodNumber: Int,
    val grade: String,
    val section: String,
    val subject: String,
    val teacherId: String,
    val roomNo: String
)

@Entity(
    tableName = "replacement_duties",
    indices = [
        Index(value = ["organizationId", "date", "periodNumber", "assignedTeacherId"]),
        Index(value = ["organizationId", "date", "periodNumber", "grade", "section"])
    ]
)
data class ReplacementDutyEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "ORG_DPA",
    val date: String, // yyyy-MM-dd
    val periodNumber: Int,
    val grade: String,
    val section: String,
    val subject: String,
    val absentTeacherId: String,
    val assignedTeacherId: String,
    val status: String, // PENDING, APPROVED, REJECTED, COMPLETED
    val aiRecommendationReason: String,
    val requestedBy: String,
    val approvedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "classroom_verifications",
    indices = [
        Index(value = ["organizationId", "date", "periodNumber", "grade", "section"])
    ]
)
data class ClassroomVerificationEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "ORG_DPA",
    val date: String,
    val periodNumber: Int,
    val grade: String,
    val section: String,
    val teacherId: String,
    val typedRoomNo: String,
    val scheduledRoomNo: String,
    val status: String, // VERIFIED, MISMATCH_FLAGGED
    val photoPath: String? = null,
    val ocrExtractedText: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "meetings",
    indices = [Index(value = ["organizationId", "date"])]
)
data class MeetingEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "ORG_DPA",
    val title: String,
    val description: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val organizerId: String,
    val organizerName: String,
    val participantIds: String, // comma-separated staff IDs
    val priority: String, // NORMAL, HIGH, URGENT
    val status: String, // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    val instructions: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "team_posts",
    indices = [Index(value = ["organizationId", "isActive"])]
)
data class TeamPostEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "ORG_DPA",
    val title: String,
    val description: String,
    val leaderId: String,
    val memberIds: String, // comma-separated staff IDs
    val workingHours: String,
    val responsibilities: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["organizationId", "targetStaffId", "isRead"])]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "ORG_DPA",
    val targetStaffId: String, // Specific ID or "ALL"
    val title: String,
    val message: String,
    val type: String,
    val relatedRecordId: String? = null,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["organizationId", "timestamp"])]
)
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "ORG_DPA",
    val userId: String,
    val userName: String,
    val action: String,
    val resource: String,
    val oldValue: String? = null,
    val newValue: String? = null,
    val result: String = "SUCCESS", // SUCCESS, DENIED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "school_settings",
    indices = [Index(value = ["organizationId", "key"], unique = true)]
)
data class SchoolSettingsEntity(
    @PrimaryKey val id: String, // "${organizationId}_${key}"
    val organizationId: String = "ORG_DPA",
    val key: String,
    val value: String
)

@Entity(tableName = "otp_sessions")
data class OtpSessionEntity(
    @PrimaryKey val phoneNumber: String,
    val verificationId: String,
    val expiresAt: Long,
    val attemptsRemaining: Int = 3,
    val lastRequestedAt: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false
)

@Entity(
    tableName = "institution_registrations",
    indices = [
        Index(value = ["contactEmail"]),
        Index(value = ["representativePhone"]),
        Index(value = ["status"]),
        Index(value = ["assignedSchoolCode"], unique = true)
    ]
)
data class InstitutionRegistrationEntity(
    @PrimaryKey val id: String, // e.g. "REG-2026-XXXX"
    val institutionName: String,
    val institutionType: String, // "SCHOOL", "COACHING"
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
    val status: String = "PENDING_VERIFICATION", // PENDING_VERIFICATION, PENDING_APPROVAL, APPROVED, REJECTED, SUSPENDED
    val isPhoneVerified: Boolean = false,
    val assignedSchoolCode: String? = null,
    val approvedInstitutionId: String? = null,
    val rejectionReason: String? = null,
    val reviewedByAdminId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "platform_admins",
    indices = [Index(value = ["phoneNumber"], unique = true)]
)
data class PlatformAdminEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String,
    val role: String = "PLATFORM_SUPER_ADMIN", // PLATFORM_SUPER_ADMIN, PLATFORM_VERIFIER
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "institution_join_requests",
    indices = [
        Index(value = ["organizationId", "phoneNumber"], unique = true),
        Index(value = ["organizationId", "status"])
    ]
)
data class InstitutionJoinRequestEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val schoolCode: String,
    val fullName: String,
    val phoneNumber: String,
    val requestedRole: String, // TEACHER, STAFF, STUDENT
    val departmentOrGrade: String,
    val idNumber: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val rejectionReason: String? = null,
    val reviewedByStaffId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
