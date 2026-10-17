package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.PeriodConfigEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.data.local.entity.TimetableEntity
import com.example.model.PeriodSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class TimetableRepository(private val database: AppDatabase) {

    fun getPeriodConfigs(orgId: String): Flow<List<PeriodConfigEntity>> {
        return database.timetableDao().getAllPeriodConfigs(orgId)
    }

    fun getTimetableForDay(orgId: String, dayOfWeek: Int): Flow<List<TimetableEntity>> {
        return database.timetableDao().getTimetableForDay(orgId, dayOfWeek)
    }

    fun getTimetableForTeacher(orgId: String, teacherId: String, dayOfWeek: Int): Flow<List<TimetableEntity>> {
        return database.timetableDao().getTimetableForTeacher(orgId, teacherId, dayOfWeek)
    }

    fun getTimetableForClass(orgId: String, grade: String, section: String, dayOfWeek: Int): Flow<List<TimetableEntity>> {
        return database.timetableDao().getTimetableForClass(orgId, grade, section, dayOfWeek)
    }

    suspend fun updatePeriodTiming(
        orgId: String,
        periodNumber: Int,
        label: String,
        startTime: String,
        endTime: String,
        isBreak: Boolean,
        isLunch: Boolean,
        actor: StaffUserEntity
    ) = withContext(Dispatchers.IO) {
        val existing = database.timetableDao().getAllPeriodConfigsList(orgId).firstOrNull { it.periodNumber == periodNumber }
        val oldVal = existing?.let { "${it.label} (${it.startTime} - ${it.endTime})" } ?: "None"
        val newVal = "$label ($startTime - $endTime)"

        database.timetableDao().updatePeriodConfig(
            PeriodConfigEntity(
                id = "${orgId}_$periodNumber",
                organizationId = orgId,
                periodNumber = periodNumber,
                label = label,
                startTime = startTime,
                endTime = endTime,
                isBreak = isBreak,
                isLunch = isLunch
            )
        )

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = orgId,
                userId = actor.id,
                userName = actor.name,
                action = "UPDATE_PERIOD_TIMING",
                resource = "PERIOD_$periodNumber",
                oldValue = oldVal,
                newValue = newVal,
                result = "SUCCESS"
            )
        )
    }

    fun getCurrentDayOfWeek(): Int {
        val day = LocalDate.now().dayOfWeek
        return when (day) {
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
            DayOfWeek.SUNDAY -> 1 // Default to Mon for weekend viewing
        }
    }

    fun calculateCurrentPeriod(periods: List<PeriodConfigEntity>, currentTime: LocalTime = LocalTime.now()): PeriodConfigEntity? {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        for (period in periods) {
            try {
                val start = LocalTime.parse(period.startTime, timeFormatter)
                val end = LocalTime.parse(period.endTime, timeFormatter)
                if ((currentTime == start || currentTime.isAfter(start)) && currentTime.isBefore(end)) {
                    return period
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun getNextPeriod(periods: List<PeriodConfigEntity>, currentPeriodNumber: Int): PeriodConfigEntity? {
        return periods.firstOrNull { it.periodNumber > currentPeriodNumber && !it.isBreak && !it.isLunch }
    }
}
