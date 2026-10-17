package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.ClassroomVerificationDao
import com.example.data.local.dao.InstitutionDao
import com.example.data.local.dao.InstitutionJoinRequestDao
import com.example.data.local.dao.InstitutionMembershipDao
import com.example.data.local.dao.InstitutionRegistrationDao
import com.example.data.local.dao.MeetingDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.OtpSessionDao
import com.example.data.local.dao.PlatformAdminDao
import com.example.data.local.dao.ReplacementDao
import com.example.data.local.dao.SchoolSettingsDao
import com.example.data.local.dao.StaffDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.dao.TeamDao
import com.example.data.local.dao.TimetableDao
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

@Database(
    entities = [
        InstitutionEntity::class,
        InstitutionMembershipEntity::class,
        StudentEntity::class,
        StaffUserEntity::class,
        PeriodConfigEntity::class,
        TimetableEntity::class,
        ReplacementDutyEntity::class,
        ClassroomVerificationEntity::class,
        MeetingEntity::class,
        TeamPostEntity::class,
        NotificationEntity::class,
        AuditLogEntity::class,
        SchoolSettingsEntity::class,
        OtpSessionEntity::class,
        InstitutionRegistrationEntity::class,
        PlatformAdminEntity::class,
        InstitutionJoinRequestEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun institutionDao(): InstitutionDao
    abstract fun institutionMembershipDao(): InstitutionMembershipDao
    abstract fun studentDao(): StudentDao
    abstract fun staffDao(): StaffDao
    abstract fun timetableDao(): TimetableDao
    abstract fun replacementDao(): ReplacementDao
    abstract fun classroomVerificationDao(): ClassroomVerificationDao
    abstract fun meetingDao(): MeetingDao
    abstract fun teamDao(): TeamDao
    abstract fun notificationDao(): NotificationDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun schoolSettingsDao(): SchoolSettingsDao
    abstract fun otpSessionDao(): OtpSessionDao
    abstract fun institutionRegistrationDao(): InstitutionRegistrationDao
    abstract fun platformAdminDao(): PlatformAdminDao
    abstract fun institutionJoinRequestDao(): InstitutionJoinRequestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "edupulse_multi_tenant_ops.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
