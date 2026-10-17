package com.example.model

enum class UserRole(val displayName: String, val level: Int) {
    SUPER_ADMIN("Super Admin", 5),
    PRINCIPAL("Principal", 4),
    ADMIN("Administrator", 3),
    TEACHER("Teacher", 2),
    STAFF("Staff", 1),
    STUDENT("Student", 0);

    companion object {
        fun fromString(value: String): UserRole {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: STUDENT
        }
    }
}

enum class MembershipStatus {
    ACTIVE,
    PENDING,
    INACTIVE
}

data class InstitutionInfo(
    val id: String,
    val name: String,
    val code: String,
    val type: String, // "SCHOOL" or "COACHING"
    val address: String,
    val principalOrDirector: String,
    val academicSession: String
)

data class UserMembershipInfo(
    val membershipId: String,
    val organizationId: String,
    val organizationName: String,
    val organizationType: String,
    val role: UserRole,
    val isClassTeacher: Boolean,
    val classTeacherGrade: String?,
    val classTeacherSection: String?,
    val status: MembershipStatus,
    val studentDetails: StudentProfileInfo? = null
)

data class StudentProfileInfo(
    val studentId: String,
    val rollNumber: String,
    val classGrade: String,
    val section: String,
    val parentName: String,
    val parentPhone: String
)

enum class StaffStatus(val label: String) {
    FREE("Free"),
    TEACHING("Teaching"),
    REPLACEMENT("Replacement Duty"),
    IN_MEETING("In Meeting"),
    ON_LEAVE("On Leave / Absent"),
    SPECIAL_DUTY("Special Duty")
}

enum class ReplacementStatus {
    PENDING,
    APPROVED,
    REJECTED,
    COMPLETED
}

enum class VerificationStatus {
    PENDING,
    VERIFIED,
    MISMATCH_FLAGGED
}

enum class MeetingPriority {
    NORMAL,
    HIGH,
    URGENT
}

enum class NotificationType {
    REPLACEMENT_ASSIGNMENT,
    REPLACEMENT_STATUS,
    MEETING_INVITATION,
    CLASSROOM_VERIFICATION_ALERT,
    PRINCIPAL_NOTICE,
    ADMIN_POST_ASSIGNMENT
}

data class PeriodSlot(
    val periodNumber: Int,
    val label: String,
    val startTime: String, // HH:mm
    val endTime: String,   // HH:mm
    val isBreak: Boolean = false,
    val isLunch: Boolean = false
) {
    val displayRange: String get() = "$startTime - $endTime"
}

data class ClassroomLiveStatus(
    val grade: String,
    val section: String,
    val currentPeriod: Int,
    val subject: String,
    val teacherId: String,
    val teacherName: String,
    val scheduledRoomNo: String,
    val verifiedRoomNo: String? = null,
    val isVerified: Boolean = false,
    val verificationTime: String? = null,
    val hasReplacement: Boolean = false,
    val replacementTeacherName: String? = null
)
