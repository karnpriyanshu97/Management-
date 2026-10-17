package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.data.local.entity.TeamPostEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class TeamRepository(private val database: AppDatabase) {

    fun getAllTeams(orgId: String): Flow<List<TeamPostEntity>> = database.teamDao().getAllTeams(orgId)
    fun getActiveTeams(orgId: String): Flow<List<TeamPostEntity>> = database.teamDao().getAllActiveTeams(orgId)

    suspend fun createTeam(
        orgId: String,
        title: String,
        description: String,
        leaderId: String,
        memberIds: List<String>,
        workingHours: String,
        responsibilities: String,
        actor: StaffUserEntity
    ): Result<TeamPostEntity> = withContext(Dispatchers.IO) {
        if (title.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Team/Post title is required."))
        }
        val teamId = UUID.randomUUID().toString()
        val membersStr = memberIds.joinToString(",")

        val team = TeamPostEntity(
            id = teamId,
            organizationId = orgId,
            title = title.trim(),
            description = description.trim(),
            leaderId = leaderId,
            memberIds = membersStr,
            workingHours = workingHours.trim(),
            responsibilities = responsibilities.trim(),
            isActive = true
        )

        database.teamDao().insertTeam(team)

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = orgId,
                userId = actor.id,
                userName = actor.name,
                action = "CREATE_TEAM_POST",
                resource = "TEAM_$teamId",
                oldValue = null,
                newValue = "Title: $title, Leader: $leaderId, Members: ${memberIds.size}",
                result = "SUCCESS"
            )
        )

        // Notify leader and members
        val allTargetIds = (memberIds + leaderId).distinct()
        for (targetId in allTargetIds) {
            database.notificationDao().insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    organizationId = orgId,
                    targetStaffId = targetId,
                    title = "Assigned to Team: $title",
                    message = "You have been appointed to '$title'. Hours: $workingHours. Please review assigned operational responsibilities.",
                    type = "ADMIN_POST_ASSIGNMENT",
                    relatedRecordId = teamId
                )
            )
        }

        Result.success(team)
    }

    suspend fun updateTeam(
        team: TeamPostEntity,
        actor: StaffUserEntity
    ) = withContext(Dispatchers.IO) {
        database.teamDao().updateTeam(team)

        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                organizationId = team.organizationId,
                userId = actor.id,
                userName = actor.name,
                action = "UPDATE_TEAM_POST",
                resource = "TEAM_${team.id}",
                oldValue = null,
                newValue = "Updated team ${team.title}, active: ${team.isActive}",
                result = "SUCCESS"
            )
        )
    }
}
