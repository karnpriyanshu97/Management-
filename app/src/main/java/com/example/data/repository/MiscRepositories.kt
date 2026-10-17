package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.SchoolSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NotificationRepository(private val database: AppDatabase) {

    fun getNotificationsForStaff(orgId: String, staffId: String): Flow<List<NotificationEntity>> {
        return database.notificationDao().getNotificationsForStaff(orgId, staffId)
    }

    suspend fun markAsRead(id: String, orgId: String) = withContext(Dispatchers.IO) {
        database.notificationDao().markAsRead(id, orgId)
    }

    suspend fun markAllAsRead(orgId: String, staffId: String) = withContext(Dispatchers.IO) {
        database.notificationDao().markAllAsRead(orgId, staffId)
    }

    suspend fun broadcastNotice(orgId: String, title: String, message: String) = withContext(Dispatchers.IO) {
        database.notificationDao().insertNotification(
            NotificationEntity(
                id = java.util.UUID.randomUUID().toString(),
                organizationId = orgId,
                targetStaffId = "ALL",
                title = title,
                message = message,
                type = "PRINCIPAL_NOTICE"
            )
        )
    }
}

class AuditRepository(private val database: AppDatabase) {
    fun getAuditLogs(orgId: String): Flow<List<AuditLogEntity>> = database.auditLogDao().getAllAuditLogs(orgId)
}

class SchoolSettingsRepository(private val database: AppDatabase) {
    fun getSettings(orgId: String): Flow<List<SchoolSettingsEntity>> = database.schoolSettingsDao().getAllSettings(orgId)

    suspend fun getSetting(orgId: String, key: String, defaultVal: String = ""): String = withContext(Dispatchers.IO) {
        database.schoolSettingsDao().getSetting(orgId, key) ?: defaultVal
    }

    suspend fun setSetting(orgId: String, key: String, value: String) = withContext(Dispatchers.IO) {
        database.schoolSettingsDao().insertSetting(SchoolSettingsEntity(id = "${orgId}_$key", organizationId = orgId, key = key, value = value))
    }
}
