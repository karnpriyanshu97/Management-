package com.example.data.service

import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.entity.StaffUserEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.util.concurrent.TimeUnit

data class ReplacementCandidate(
    val teacher: StaffUserEntity,
    val score: Int,
    val matchReason: String,
    val currentReplacementsToday: Int,
    val subjectMatch: Boolean,
    val gradeSuitability: Boolean
)

data class AiRecommendationResult(
    val topCandidate: ReplacementCandidate?,
    val alternativeCandidates: List<ReplacementCandidate>,
    val explanation: String
)

class AiReplacementEngine(private val database: AppDatabase) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun findAndRankReplacements(
        absentTeacher: StaffUserEntity,
        grade: String,
        section: String,
        subject: String,
        periodNumber: Int,
        dayOfWeek: Int,
        dateStr: String = LocalDate.now().toString()
    ): AiRecommendationResult = withContext(Dispatchers.IO) {
        val orgId = absentTeacher.organizationId
        val candidatePool = mutableListOf<ReplacementCandidate>()

        // Get all active teachers in this organization except absentTeacher
        val allOrgTimetable = database.timetableDao().getAllTimetableForOrg(orgId)
        val allTeacherIds = allOrgTimetable.map { it.teacherId }.distinct()
        val allTeachers = allTeacherIds.mapNotNull { database.staffDao().getStaffById(it, orgId) }
            .filter { it.isActive && !it.isOnLeave && it.id != absentTeacher.id && it.role == "TEACHER" }

        for (teacher in allTeachers) {
            // 1. Is teacher already teaching in this period in this institution?
            val isTeaching = database.timetableDao().getTeacherActiveSlot(orgId, teacher.id, dayOfWeek, periodNumber) != null
            if (isTeaching) continue

            // 2. Is teacher already assigned to an approved replacement in this period in this institution?
            val hasReplacementDuty = database.replacementDao().getTeacherApprovedReplacementSlot(orgId, dateStr, periodNumber, teacher.id) != null
            if (hasReplacementDuty) continue

            // 3. Count replacements assigned today
            val todayReplacementsCount = database.replacementDao().getReplacementCountForTeacher(orgId, teacher.id, dateStr)

            // 4. Calculate score & criteria
            var score = 100
            score -= (todayReplacementsCount * 25)

            // Subject affinity
            val teacherSubjects = teacher.subjects.lowercase()
            val targetSubject = subject.lowercase()
            val isSubjectMatch = teacherSubjects.contains(targetSubject) ||
                    (targetSubject.contains("science") && (teacherSubjects.contains("physics") || teacherSubjects.contains("chemistry") || teacherSubjects.contains("biology"))) ||
                    (targetSubject.contains("math") && teacherSubjects.contains("math")) ||
                    (targetSubject.contains("english") && teacherSubjects.contains("literature"))

            if (isSubjectMatch) {
                score += 50
            }

            // Grade familiarity
            val assignedGrades = teacher.assignedClasses
            val isGradeMatch = assignedGrades.contains(grade) || assignedGrades.contains("All Wings")
            if (isGradeMatch) {
                score += 30
            }

            val reasonBuilder = StringBuilder()
            reasonBuilder.append("Verified free in Period $periodNumber. ")
            if (isSubjectMatch) {
                reasonBuilder.append("Subject specialist in $subject. ")
            } else {
                reasonBuilder.append("Available faculty from ${teacher.department} department. ")
            }
            if (isGradeMatch) {
                reasonBuilder.append("Teaches Grade $grade syllabus regularly. ")
            }
            reasonBuilder.append("Has handled $todayReplacementsCount replacement(s) today.")

            candidatePool.add(
                ReplacementCandidate(
                    teacher = teacher,
                    score = score,
                    matchReason = reasonBuilder.toString(),
                    currentReplacementsToday = todayReplacementsCount,
                    subjectMatch = isSubjectMatch,
                    gradeSuitability = isGradeMatch
                )
            )
        }

        candidatePool.sortByDescending { it.score }

        val top = candidatePool.firstOrNull()
        val alternatives = candidatePool.drop(1)

        if (top == null) {
            return@withContext AiRecommendationResult(
                topCandidate = null,
                alternativeCandidates = emptyList(),
                explanation = "No free teachers found for Period $periodNumber in this institution. All faculty are either teaching, on leave, or on prior duty."
            )
        }

        // Try AI enhancement for pedagogical reasoning if API key is present
        val geminiKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        var aiExplanation = "${top.teacher.name} is the optimal replacement for Class $grade-$section ($subject). " +
                "They are free in Period $periodNumber with zero overlapping duties, " +
                "${if (top.subjectMatch) "has direct subject expertise in $subject" else "belongs to ${top.teacher.department}"}, " +
                "and maintains a low substitution load ($top.currentReplacementsToday today)."

        if (geminiKey.isNotBlank() && !geminiKey.contains("MY_GEMINI_API_KEY") && geminiKey != "null") {
            try {
                val promptText = "In 2 concise sentences, provide an administrative justification for assigning teacher ${top.teacher.name} (Department: ${top.teacher.department}, Subjects: ${top.teacher.subjects}) to cover Period $periodNumber for absent teacher ${absentTeacher.name} in Class $grade-$section ($subject). Mention conflict-free status and subject alignment."
                val response = callGeminiRest(geminiKey, promptText)
                if (!response.isNullOrBlank()) {
                    aiExplanation = response.trim()
                }
            } catch (_: Exception) {
                // Graceful fallback to heuristic explanation
            }
        }

        AiRecommendationResult(
            topCandidate = top,
            alternativeCandidates = alternatives,
            explanation = aiExplanation
        )
    }

    private fun callGeminiRest(apiKey: String, prompt: String): String? {
        val jsonPayload = """
            {
              "contents": [
                {
                  "parts": [
                    {"text": "${prompt.replace("\"", "\\\"").replace("\n", " ")}"}
                  ]
                }
              ]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { resp ->
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string() ?: return null
                val marker = "\"text\": \""
                val start = bodyStr.indexOf(marker)
                if (start != -1) {
                    val end = bodyStr.indexOf("\"", start + marker.length)
                    if (end != -1) {
                        return bodyStr.substring(start + marker.length, end).replace("\\n", "\n").replace("\\\"", "\"")
                    }
                }
            }
        }
        return null
    }
}
