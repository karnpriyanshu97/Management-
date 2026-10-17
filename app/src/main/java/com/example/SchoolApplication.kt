package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseSeeder
import com.example.data.local.SessionManager
import com.example.data.repository.AuditRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.MeetingRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.ReplacementRepository
import com.example.data.repository.SchoolSettingsRepository
import com.example.data.repository.StaffRepository
import com.example.data.repository.TeamRepository
import com.example.data.repository.TimetableRepository
import com.example.data.repository.VerificationRepository
import com.example.data.service.AiReplacementEngine
import com.example.data.service.SmsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SchoolApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val sessionManager by lazy { SessionManager(this) }
    val smsService by lazy { SmsService(this) }
    val firebaseAuthService by lazy { smsService.firebaseAuthService }

    val authRepository by lazy { AuthRepository(database, sessionManager, smsService, firebaseAuthService) }
    val timetableRepository by lazy { TimetableRepository(database) }
    val staffRepository by lazy { StaffRepository(database) }
    val aiReplacementEngine by lazy { AiReplacementEngine(database) }
    val replacementRepository by lazy { ReplacementRepository(database, aiReplacementEngine) }
    val verificationRepository by lazy { VerificationRepository(database) }
    val meetingRepository by lazy { MeetingRepository(database) }
    val teamRepository by lazy { TeamRepository(database) }
    val notificationRepository by lazy { NotificationRepository(database) }
    val auditRepository by lazy { AuditRepository(database) }
    val schoolSettingsRepository by lazy { SchoolSettingsRepository(database) }
    val studentRepository by lazy { com.example.data.repository.StudentRepository(database) }
    val registrationRepository by lazy { com.example.data.repository.InstitutionRegistrationRepository(database, smsService) }

    override fun onCreate() {
        super.onCreate()
    }
}
