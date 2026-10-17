package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.ClassroomVerificationEntity
import com.example.data.local.entity.InstitutionEntity
import com.example.data.local.entity.InstitutionJoinRequestEntity
import com.example.data.local.entity.InstitutionMembershipEntity
import com.example.data.local.entity.InstitutionRegistrationEntity
import com.example.data.local.entity.MeetingEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.OtpSessionEntity
import com.example.data.local.entity.PeriodConfigEntity
import com.example.data.local.entity.PlatformAdminEntity
import com.example.data.local.entity.ReplacementDutyEntity
import com.example.data.local.entity.SchoolSettingsEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.TeamPostEntity
import com.example.data.local.entity.TimetableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstitutionDao {
    @Query("SELECT * FROM institutions WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveInstitutions(): Flow<List<InstitutionEntity>>

    @Query("SELECT * FROM institutions WHERE id = :id LIMIT 1")
    suspend fun getInstitutionById(id: String): InstitutionEntity?

    @Query("SELECT * FROM institutions WHERE id = :id LIMIT 1")
    fun observeInstitutionById(id: String): Flow<InstitutionEntity?>

    @Query("SELECT * FROM institutions WHERE code = :code LIMIT 1")
    suspend fun getInstitutionByCode(code: String): InstitutionEntity?

    @Query("SELECT * FROM institutions WHERE LOWER(code) = LOWER(:code) LIMIT 1")
    suspend fun getInstitutionByCodeCaseInsensitive(code: String): InstitutionEntity?

    @Query("SELECT * FROM institutions WHERE LOWER(name) = LOWER(:name) AND LOWER(address) LIKE '%' || LOWER(:city) || '%' LIMIT 1")
    suspend fun getInstitutionByNameAndCity(name: String, city: String): InstitutionEntity?

    @Query("SELECT * FROM institutions WHERE isActive = 1 ORDER BY createdAt ASC LIMIT 1")
    fun observePrimaryActiveInstitution(): Flow<InstitutionEntity?>

    @Query("SELECT * FROM institutions WHERE isActive = 1 ORDER BY createdAt ASC LIMIT 1")
    suspend fun getPrimaryActiveInstitution(): InstitutionEntity?

    @Query("DELETE FROM institutions WHERE id = :id OR name = 'Delhi Public Academy'")
    suspend fun deleteInstitutionById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstitution(institution: InstitutionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstitutions(institutions: List<InstitutionEntity>)
}

@Dao
interface InstitutionMembershipDao {
    @Query("SELECT * FROM institution_memberships WHERE phoneNumber = :phone AND status = 'ACTIVE'")
    fun observeActiveMembershipsForPhone(phone: String): Flow<List<InstitutionMembershipEntity>>

    @Query("SELECT * FROM institution_memberships WHERE phoneNumber = :phone AND status = 'ACTIVE'")
    suspend fun getActiveMembershipsForPhone(phone: String): List<InstitutionMembershipEntity>

    @Query("SELECT * FROM institution_memberships WHERE phoneNumber = :phone AND organizationId = :orgId LIMIT 1")
    suspend fun getMembershipForPhoneAndOrg(phone: String, orgId: String): InstitutionMembershipEntity?

    @Query("SELECT * FROM institution_memberships WHERE userId = :userId AND organizationId = :orgId LIMIT 1")
    suspend fun getMembershipForUserAndOrg(userId: String, orgId: String): InstitutionMembershipEntity?

    @Query("SELECT * FROM institution_memberships WHERE organizationId = :orgId AND isClassTeacher = 1 AND classTeacherGrade = :grade AND classTeacherSection = :section AND status = 'ACTIVE' LIMIT 1")
    suspend fun getClassTeacherMembership(orgId: String, grade: String, section: String): InstitutionMembershipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembership(membership: InstitutionMembershipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemberships(memberships: List<InstitutionMembershipEntity>)

    @Update
    suspend fun updateMembership(membership: InstitutionMembershipEntity)

    @Query("DELETE FROM institution_memberships WHERE phoneNumber IN (:phones)")
    suspend fun deleteMembershipsByPhones(phones: List<String>)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE organizationId = :orgId AND classGrade = :grade AND section = :section AND status = 'ACTIVE' ORDER BY CAST(rollNumber AS INTEGER) ASC, rollNumber ASC")
    fun getStudentsForClass(orgId: String, grade: String, section: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE organizationId = :orgId AND status = 'ACTIVE' ORDER BY classGrade ASC, section ASC, rollNumber ASC")
    fun getAllStudentsForOrg(orgId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE organizationId = :orgId AND studentId = :studentId LIMIT 1")
    suspend fun getStudentById(orgId: String, studentId: String): StudentEntity?

    @Query("SELECT * FROM students WHERE organizationId = :orgId AND classGrade = :grade AND section = :section AND rollNumber = :rollNumber LIMIT 1")
    suspend fun getStudentByRoll(orgId: String, grade: String, section: String, rollNumber: String): StudentEntity?

    @Query("SELECT * FROM students WHERE organizationId = :orgId AND phoneNumber = :phone LIMIT 1")
    suspend fun getStudentByPhone(orgId: String, phone: String): StudentEntity?

    @Query("SELECT * FROM students WHERE studentId = :studentId LIMIT 1")
    suspend fun getStudentProfile(studentId: String): StudentEntity?

    @Query("SELECT * FROM students WHERE studentId = :studentId LIMIT 1")
    fun observeStudentProfile(studentId: String): Flow<StudentEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("SELECT COUNT(*) FROM students WHERE organizationId = :orgId AND classGrade = :grade AND section = :section AND status = 'ACTIVE'")
    suspend fun countStudentsInClass(orgId: String, grade: String, section: String): Int

    @Query("SELECT COUNT(*) FROM students WHERE organizationId = :orgId AND status = 'ACTIVE'")
    suspend fun countTotalStudents(orgId: String): Int
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff_users WHERE organizationId = :orgId ORDER BY name ASC")
    fun getAllStaff(orgId: String): Flow<List<StaffUserEntity>>

    @Query("SELECT * FROM staff_users WHERE organizationId = :orgId AND isActive = 1 ORDER BY name ASC")
    fun getActiveStaff(orgId: String): Flow<List<StaffUserEntity>>

    @Query("SELECT * FROM staff_users WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveStaffGlobal(): Flow<List<StaffUserEntity>>

    @Query("DELETE FROM staff_users WHERE phoneNumber IN (:phones)")
    suspend fun deleteStaffByPhones(phones: List<String>)

    @Query("SELECT * FROM staff_users WHERE id = :id AND organizationId = :orgId LIMIT 1")
    suspend fun getStaffById(id: String, orgId: String): StaffUserEntity?

    @Query("SELECT * FROM staff_users WHERE id = :id LIMIT 1")
    suspend fun getStaffByIdGlobal(id: String): StaffUserEntity?

    @Query("SELECT * FROM staff_users WHERE phoneNumber = :phone AND organizationId = :orgId LIMIT 1")
    suspend fun getStaffByPhone(phone: String, orgId: String): StaffUserEntity?

    @Query("SELECT * FROM staff_users WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getStaffByPhoneGlobal(phone: String): StaffUserEntity?

    @Query("SELECT * FROM staff_users WHERE phoneNumber = :phone AND organizationId = :orgId LIMIT 1")
    fun observeStaffByPhone(phone: String, orgId: String): Flow<StaffUserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffList(staffList: List<StaffUserEntity>)

    @Update
    suspend fun updateStaff(staff: StaffUserEntity)

    @Query("UPDATE staff_users SET isOnLeave = :onLeave WHERE id = :id AND organizationId = :orgId")
    suspend fun setLeaveStatus(id: String, orgId: String, onLeave: Boolean)

    @Query("SELECT COUNT(*) FROM staff_users WHERE organizationId = :orgId")
    suspend fun countStaff(orgId: String): Int

    @Query("SELECT COUNT(*) FROM staff_users")
    suspend fun countAllStaff(): Int
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM period_configs WHERE organizationId = :orgId ORDER BY periodNumber ASC")
    fun getAllPeriodConfigs(orgId: String): Flow<List<PeriodConfigEntity>>

    @Query("SELECT * FROM period_configs WHERE organizationId = :orgId ORDER BY periodNumber ASC")
    suspend fun getAllPeriodConfigsList(orgId: String): List<PeriodConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriodConfigs(configs: List<PeriodConfigEntity>)

    @Update
    suspend fun updatePeriodConfig(config: PeriodConfigEntity)

    @Query("SELECT * FROM timetable_entries WHERE organizationId = :orgId AND dayOfWeek = :dayOfWeek ORDER BY grade, section, periodNumber ASC")
    fun getTimetableForDay(orgId: String, dayOfWeek: Int): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable_entries WHERE organizationId = :orgId AND teacherId = :teacherId AND dayOfWeek = :dayOfWeek ORDER BY periodNumber ASC")
    fun getTimetableForTeacher(orgId: String, teacherId: String, dayOfWeek: Int): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable_entries WHERE organizationId = :orgId AND grade = :grade AND section = :section AND dayOfWeek = :dayOfWeek ORDER BY periodNumber ASC")
    fun getTimetableForClass(orgId: String, grade: String, section: String, dayOfWeek: Int): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable_entries WHERE organizationId = :orgId AND teacherId = :teacherId AND dayOfWeek = :dayOfWeek AND periodNumber = :periodNumber LIMIT 1")
    suspend fun getTeacherActiveSlot(orgId: String, teacherId: String, dayOfWeek: Int, periodNumber: Int): TimetableEntity?

    @Query("SELECT * FROM timetable_entries WHERE organizationId = :orgId AND dayOfWeek = :dayOfWeek AND periodNumber = :periodNumber AND grade = :grade AND section = :section LIMIT 1")
    suspend fun getClassSlot(orgId: String, dayOfWeek: Int, periodNumber: Int, grade: String, section: String): TimetableEntity?

    @Query("SELECT * FROM timetable_entries WHERE organizationId = :orgId")
    suspend fun getAllTimetableForOrg(orgId: String): List<TimetableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetable(entries: List<TimetableEntity>)
}

@Dao
interface ReplacementDao {
    @Query("SELECT * FROM replacement_duties WHERE organizationId = :orgId ORDER BY createdAt DESC")
    fun getAllReplacements(orgId: String): Flow<List<ReplacementDutyEntity>>

    @Query("SELECT * FROM replacement_duties WHERE organizationId = :orgId AND date = :date ORDER BY periodNumber ASC")
    fun getReplacementsForDate(orgId: String, date: String): Flow<List<ReplacementDutyEntity>>

    @Query("SELECT * FROM replacement_duties WHERE organizationId = :orgId AND (assignedTeacherId = :teacherId OR absentTeacherId = :teacherId) AND date = :date")
    fun getReplacementsForTeacher(orgId: String, teacherId: String, date: String): Flow<List<ReplacementDutyEntity>>

    @Query("SELECT * FROM replacement_duties WHERE organizationId = :orgId AND date = :date AND periodNumber = :periodNumber AND assignedTeacherId = :teacherId AND status = 'APPROVED' LIMIT 1")
    suspend fun getTeacherApprovedReplacementSlot(orgId: String, date: String, periodNumber: Int, teacherId: String): ReplacementDutyEntity?

    @Query("SELECT COUNT(*) FROM replacement_duties WHERE organizationId = :orgId AND assignedTeacherId = :teacherId AND date = :date AND status = 'APPROVED'")
    suspend fun getReplacementCountForTeacher(orgId: String, teacherId: String, date: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReplacement(replacement: ReplacementDutyEntity)

    @Update
    suspend fun updateReplacement(replacement: ReplacementDutyEntity)
}

@Dao
interface ClassroomVerificationDao {
    @Query("SELECT * FROM classroom_verifications WHERE organizationId = :orgId ORDER BY timestamp DESC")
    fun getAllVerifications(orgId: String): Flow<List<ClassroomVerificationEntity>>

    @Query("SELECT * FROM classroom_verifications WHERE organizationId = :orgId AND date = :date AND periodNumber = :periodNumber")
    fun getVerificationsForDateAndPeriod(orgId: String, date: String, periodNumber: Int): Flow<List<ClassroomVerificationEntity>>

    @Query("SELECT * FROM classroom_verifications WHERE organizationId = :orgId AND date = :date AND periodNumber = :periodNumber AND grade = :grade AND section = :section LIMIT 1")
    suspend fun getVerification(orgId: String, date: String, periodNumber: Int, grade: String, section: String): ClassroomVerificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(verification: ClassroomVerificationEntity)
}

@Dao
interface MeetingDao {
    @Query("SELECT * FROM meetings WHERE organizationId = :orgId ORDER BY date DESC, startTime DESC")
    fun getAllMeetings(orgId: String): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE organizationId = :orgId AND date = :date ORDER BY startTime ASC")
    suspend fun getMeetingsForDate(orgId: String, date: String): List<MeetingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: MeetingEntity)

    @Update
    suspend fun updateMeeting(meeting: MeetingEntity)
}

@Dao
interface TeamDao {
    @Query("SELECT * FROM team_posts WHERE organizationId = :orgId AND isActive = 1 ORDER BY title ASC")
    fun getAllActiveTeams(orgId: String): Flow<List<TeamPostEntity>>

    @Query("SELECT * FROM team_posts WHERE organizationId = :orgId ORDER BY createdAt DESC")
    fun getAllTeams(orgId: String): Flow<List<TeamPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamPostEntity)

    @Update
    suspend fun updateTeam(team: TeamPostEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE organizationId = :orgId AND (targetStaffId = :staffId OR targetStaffId = 'ALL') ORDER BY timestamp DESC")
    fun getNotificationsForStaff(orgId: String, staffId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id AND organizationId = :orgId")
    suspend fun markAsRead(id: String, orgId: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE organizationId = :orgId AND (targetStaffId = :staffId OR targetStaffId = 'ALL')")
    suspend fun markAllAsRead(orgId: String, staffId: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs WHERE organizationId = :orgId ORDER BY timestamp DESC LIMIT 200")
    fun getAllAuditLogs(orgId: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)
}

@Dao
interface SchoolSettingsDao {
    @Query("SELECT * FROM school_settings WHERE organizationId = :orgId")
    fun getAllSettings(orgId: String): Flow<List<SchoolSettingsEntity>>

    @Query("SELECT value FROM school_settings WHERE organizationId = :orgId AND `key` = :key LIMIT 1")
    suspend fun getSetting(orgId: String, key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SchoolSettingsEntity)
}

@Dao
interface OtpSessionDao {
    @Query("SELECT * FROM otp_sessions WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getOtpSession(phoneNumber: String): OtpSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(session: OtpSessionEntity)

    @Query("DELETE FROM otp_sessions WHERE phoneNumber = :phoneNumber")
    suspend fun deleteSession(phoneNumber: String)
}

@Dao
interface InstitutionRegistrationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: InstitutionRegistrationEntity)

    @Update
    suspend fun updateRegistration(registration: InstitutionRegistrationEntity)

    @Query("SELECT * FROM institution_registrations WHERE id = :id LIMIT 1")
    suspend fun getRegistrationById(id: String): InstitutionRegistrationEntity?

    @Query("SELECT * FROM institution_registrations WHERE id = :id LIMIT 1")
    fun observeRegistrationById(id: String): Flow<InstitutionRegistrationEntity?>

    @Query("SELECT * FROM institution_registrations WHERE representativePhone = :phone ORDER BY createdAt DESC LIMIT 1")
    suspend fun getRegistrationByPhone(phone: String): InstitutionRegistrationEntity?

    @Query("SELECT * FROM institution_registrations WHERE contactEmail = :email ORDER BY createdAt DESC LIMIT 1")
    suspend fun getRegistrationByEmail(email: String): InstitutionRegistrationEntity?

    @Query("SELECT * FROM institution_registrations ORDER BY createdAt DESC")
    fun observeAllRegistrations(): Flow<List<InstitutionRegistrationEntity>>

    @Query("SELECT * FROM institution_registrations ORDER BY createdAt DESC")
    suspend fun getAllRegistrations(): List<InstitutionRegistrationEntity>

    @Query("SELECT * FROM institution_registrations WHERE status = :status ORDER BY createdAt DESC")
    fun observeRegistrationsByStatus(status: String): Flow<List<InstitutionRegistrationEntity>>

    @Query("SELECT * FROM institution_registrations WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getRegistrationsByStatus(status: String): List<InstitutionRegistrationEntity>

    @Query("SELECT * FROM institution_registrations WHERE (LOWER(contactEmail) = LOWER(:email) OR representativePhone = :phone OR (LOWER(institutionName) = LOWER(:name) AND LOWER(city) = LOWER(:city))) AND status IN ('PENDING_VERIFICATION', 'PENDING_APPROVAL', 'APPROVED') LIMIT 1")
    suspend fun findExistingDuplicate(email: String, phone: String, name: String, city: String): InstitutionRegistrationEntity?

    @Query("SELECT COUNT(*) FROM institution_registrations WHERE status = 'PENDING_APPROVAL'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM institution_registrations WHERE status = 'PENDING_APPROVAL'")
    suspend fun countPendingApprovals(): Int
}

@Dao
interface PlatformAdminDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatformAdmin(admin: PlatformAdminEntity)

    @Query("SELECT * FROM platform_admins WHERE phoneNumber = :phone AND isActive = 1 LIMIT 1")
    suspend fun getPlatformAdminByPhone(phone: String): PlatformAdminEntity?

    @Query("SELECT * FROM platform_admins WHERE phoneNumber = :phone AND isActive = 1 LIMIT 1")
    fun observePlatformAdminByPhone(phone: String): Flow<PlatformAdminEntity?>

    @Query("SELECT COUNT(*) FROM platform_admins WHERE isActive = 1")
    suspend fun countPlatformAdmins(): Int

    @Query("SELECT COUNT(*) FROM platform_admins WHERE isActive = 1")
    fun observePlatformAdminCount(): Flow<Int>

    @Query("SELECT * FROM platform_admins WHERE isActive = 1 ORDER BY createdAt ASC")
    fun observeAllPlatformAdmins(): Flow<List<PlatformAdminEntity>>

    @Query("SELECT * FROM platform_admins WHERE isActive = 1 ORDER BY createdAt ASC")
    suspend fun getAllPlatformAdmins(): List<PlatformAdminEntity>
}

@Dao
interface InstitutionJoinRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJoinRequest(request: InstitutionJoinRequestEntity)

    @Update
    suspend fun updateJoinRequest(request: InstitutionJoinRequestEntity)

    @Query("SELECT * FROM institution_join_requests WHERE id = :id LIMIT 1")
    suspend fun getJoinRequestById(id: String): InstitutionJoinRequestEntity?

    @Query("SELECT * FROM institution_join_requests WHERE phoneNumber = :phone AND organizationId = :orgId LIMIT 1")
    suspend fun getJoinRequestForPhoneAndOrg(phone: String, orgId: String): InstitutionJoinRequestEntity?

    @Query("SELECT * FROM institution_join_requests WHERE organizationId = :orgId ORDER BY createdAt DESC")
    fun observeJoinRequestsForOrg(orgId: String): Flow<List<InstitutionJoinRequestEntity>>

    @Query("SELECT * FROM institution_join_requests WHERE organizationId = :orgId AND status = 'PENDING' ORDER BY createdAt DESC")
    fun observePendingJoinRequestsForOrg(orgId: String): Flow<List<InstitutionJoinRequestEntity>>

    @Query("SELECT * FROM institution_join_requests WHERE organizationId = :orgId ORDER BY createdAt DESC")
    suspend fun getJoinRequestsForOrg(orgId: String): List<InstitutionJoinRequestEntity>
}
