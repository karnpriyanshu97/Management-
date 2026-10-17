package com.example.data.local

import com.example.data.local.entity.InstitutionEntity
import com.example.data.local.entity.InstitutionMembershipEntity
import com.example.data.local.entity.MeetingEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.PeriodConfigEntity
import com.example.data.local.entity.SchoolSettingsEntity
import com.example.data.local.entity.StaffUserEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.TeamPostEntity
import com.example.data.local.entity.TimetableEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

object DatabaseSeeder {

    suspend fun seedDatabaseIfEmpty(database: AppDatabase) = withContext(Dispatchers.IO) {
        val dpa = database.institutionDao().getInstitutionById("ORG_DPA")
        if (dpa != null) return@withContext

        // 1. Seed Multiple Institutions
        val institutions = listOf(
            InstitutionEntity(
                id = "ORG_DPA",
                name = "Delhi Public Academy",
                code = "DPA-ND-10492",
                type = "SCHOOL",
                address = "Sector 14, R.K. Puram, New Delhi",
                principalOrDirector = "Prof. Meenakshi Sundaram",
                academicSession = "2026-2027"
            ),
            InstitutionEntity(
                id = "ORG_DAV",
                name = "DAV Centenary Public School",
                code = "DAV-DEL-001",
                type = "SCHOOL",
                address = "Vikas Marg, Preet Vihar, Delhi",
                principalOrDirector = "Dr. Harish Chandra",
                academicSession = "2026-2027"
            ),
            InstitutionEntity(
                id = "ORG_ABC",
                name = "ABC IIT-JEE & Medical Coaching Academy",
                code = "ABC-KOTA-501",
                type = "COACHING",
                address = "Institutional Area, Kalu Sarai, New Delhi",
                principalOrDirector = "Er. Sanjeev Kapoor",
                academicSession = "2026-2027"
            )
        )
        database.institutionDao().insertInstitutions(institutions)

        // 2. Seed Period Configurations for each institution
        val orgs = listOf("ORG_DPA", "ORG_DAV", "ORG_ABC")
        for (orgId in orgs) {
            val periods = listOf(
                PeriodConfigEntity("${orgId}_1", orgId, 1, "Period 1", "08:00", "08:45", isBreak = false, isLunch = false),
                PeriodConfigEntity("${orgId}_2", orgId, 2, "Period 2", "08:45", "09:30", isBreak = false, isLunch = false),
                PeriodConfigEntity("${orgId}_3", orgId, 3, "Morning Recess", "09:30", "09:45", isBreak = true, isLunch = false),
                PeriodConfigEntity("${orgId}_4", orgId, 4, "Period 3", "09:45", "10:30", isBreak = false, isLunch = false),
                PeriodConfigEntity("${orgId}_5", orgId, 5, "Period 4", "10:30", "11:15", isBreak = false, isLunch = false),
                PeriodConfigEntity("${orgId}_6", orgId, 6, "Lunch Break", "11:15", "11:50", isBreak = false, isLunch = true),
                PeriodConfigEntity("${orgId}_7", orgId, 7, "Period 5", "11:50", "12:35", isBreak = false, isLunch = false),
                PeriodConfigEntity("${orgId}_8", orgId, 8, "Period 6", "12:35", "13:20", isBreak = false, isLunch = false),
                PeriodConfigEntity("${orgId}_9", orgId, 9, "Period 7", "13:20", "14:00", isBreak = false, isLunch = false)
            )
            database.timetableDao().insertPeriodConfigs(periods)

            database.schoolSettingsDao().insertSetting(SchoolSettingsEntity("${orgId}_school_name", orgId, "school_name", institutions.first { it.id == orgId }.name))
            database.schoolSettingsDao().insertSetting(SchoolSettingsEntity("${orgId}_school_code", orgId, "school_code", institutions.first { it.id == orgId }.code))
            database.schoolSettingsDao().insertSetting(SchoolSettingsEntity("${orgId}_academic_session", orgId, "academic_session", "2026-2027"))
            database.schoolSettingsDao().insertSetting(SchoolSettingsEntity("${orgId}_principal_name", orgId, "principal_name", institutions.first { it.id == orgId }.principalOrDirector))
        }

        // 3. Seed Staff Members for Institution 1 (ORG_DPA)
        val dpaStaff = listOf(
            StaffUserEntity("DIR001", "ORG_DPA", "Dr. Arvind Swaminathan", "+919876543210", "SUPER_ADMIN", "Administration", "Institutional Management", "Board of Governors"),
            StaffUserEntity("PRN001", "ORG_DPA", "Prof. Meenakshi Sundaram", "+919876543211", "PRINCIPAL", "Leadership", "Educational Administration", "All Wings"),
            StaffUserEntity("ADM001", "ORG_DPA", "Vikram Malhotra", "+919876543212", "ADMIN", "Operations", "School Operations", "Secondary Wing"),
            StaffUserEntity("TCH101", "ORG_DPA", "Mrs. Sunita Sharma", "+919876543213", "TEACHER", "English", "English, Literature", "9-A, 10-A, 10-B"),
            StaffUserEntity("TCH102", "ORG_DPA", "Mr. Rajesh Verma", "+919876543214", "TEACHER", "Mathematics", "Mathematics, Statistics", "8-A, 10-B, 11-A"),
            StaffUserEntity("TCH103", "ORG_DPA", "Dr. Ananya Roy", "+919876543215", "TEACHER", "Science", "Physics, Science", "9-B, 10-A, 11-B"),
            StaffUserEntity("TCH104", "ORG_DPA", "Mr. Amit Patel", "+919876543216", "TEACHER", "Social Science", "History, Civics", "7-A, 8-B, 10-C"),
            StaffUserEntity("TCH105", "ORG_DPA", "Mrs. Priya Nambiar", "+919876543217", "TEACHER", "Science", "Chemistry, Biology", "6-A, 9-C, 11-A"),
            StaffUserEntity("TCH106", "ORG_DPA", "Mr. Sanjay Kulkarni", "+919876543218", "TEACHER", "Computer Science", "Informatics, AI", "8-A, 9-B, 11-C"),
            StaffUserEntity("TCH107", "ORG_DPA", "Ms. Kavita Reddy", "+919876543219", "TEACHER", "Languages", "Hindi, Sanskrit", "5-A, 6-B, 7-A"),
            StaffUserEntity("TCH108", "ORG_DPA", "Mr. David Fernandes", "+919876543220", "TEACHER", "Physical Education", "Sports, Health", "All Wings"),
            StaffUserEntity("STF001", "ORG_DPA", "Ramesh Kumar", "+919876543221", "STAFF", "Campus Security", "Facility Management", "General Campus")
        )
        database.staffDao().insertStaffList(dpaStaff)

        // Seed Staff for Institution 2 (ORG_DAV)
        val davStaff = listOf(
            StaffUserEntity("PRN002", "ORG_DAV", "Dr. Harish Chandra", "+919876543241", "PRINCIPAL", "Leadership", "School Administration", "All Wings"),
            StaffUserEntity("ADM002", "ORG_DAV", "Neha Gupta", "+919876543242", "ADMIN", "Administration", "Academics", "Senior Wing"),
            StaffUserEntity("TCH201", "ORG_DAV", "Mr. Alok Pandey", "+919876543243", "TEACHER", "Physics", "Physics, Science", "11-A, 12-A")
        )
        database.staffDao().insertStaffList(davStaff)

        // Seed Staff for Institution 3 (ORG_ABC Coaching)
        val abcStaff = listOf(
            StaffUserEntity("DIR003", "ORG_ABC", "Er. Sanjeev Kapoor", "+919876543261", "PRINCIPAL", "Executive Management", "Academic Strategy", "All Batches"),
            StaffUserEntity("ADM003", "ORG_ABC", "Pooja Batra", "+919876543262", "ADMIN", "Admissions & Operations", "Coaching Operations", "All Batches"),
            StaffUserEntity("TCH301", "ORG_ABC", "Prof. K. K. Anand", "+919876543263", "TEACHER", "Physics (JEE Advanced)", "Advanced Mechanics, Electrodynamics", "Batch JEE-A, Batch JEE-B")
        )
        database.staffDao().insertStaffList(abcStaff)

        // 4. Seed Institution Memberships with Class Teacher assignments
        val memberships = mutableListOf<InstitutionMembershipEntity>()

        // ORG_DPA Memberships
        for (staff in dpaStaff) {
            val isClassTeacher = staff.id == "TCH101" || staff.id == "TCH102"
            val ctGrade = when (staff.id) {
                "TCH101" -> "10"
                "TCH102" -> "10"
                else -> null
            }
            val ctSection = when (staff.id) {
                "TCH101" -> "A"
                "TCH102" -> "B"
                else -> null
            }

            memberships.add(
                InstitutionMembershipEntity(
                    membershipId = UUID.randomUUID().toString(),
                    userId = staff.id,
                    phoneNumber = staff.phoneNumber,
                    organizationId = "ORG_DPA",
                    role = staff.role,
                    isClassTeacher = isClassTeacher,
                    classTeacherGrade = ctGrade,
                    classTeacherSection = ctSection,
                    status = "ACTIVE",
                    createdBy = "SYSTEM"
                )
            )
        }

        // ORG_DAV Memberships
        for (staff in davStaff) {
            val isClassTeacher = staff.id == "TCH201"
            memberships.add(
                InstitutionMembershipEntity(
                    membershipId = UUID.randomUUID().toString(),
                    userId = staff.id,
                    phoneNumber = staff.phoneNumber,
                    organizationId = "ORG_DAV",
                    role = staff.role,
                    isClassTeacher = isClassTeacher,
                    classTeacherGrade = if (isClassTeacher) "11" else null,
                    classTeacherSection = if (isClassTeacher) "A" else null,
                    status = "ACTIVE",
                    createdBy = "SYSTEM"
                )
            )
        }

        // ORG_ABC Memberships
        for (staff in abcStaff) {
            val isMentor = staff.id == "TCH301"
            memberships.add(
                InstitutionMembershipEntity(
                    membershipId = UUID.randomUUID().toString(),
                    userId = staff.id,
                    phoneNumber = staff.phoneNumber,
                    organizationId = "ORG_ABC",
                    role = staff.role,
                    isClassTeacher = isMentor,
                    classTeacherGrade = if (isMentor) "JEE" else null,
                    classTeacherSection = if (isMentor) "A" else null,
                    status = "ACTIVE",
                    createdBy = "SYSTEM"
                )
            )
        }

        // 5. Seed Pre-Enrolled Students & their Memberships
        val students = listOf(
            // DPA Students
            StudentEntity("STU_DPA_101", "ORG_DPA", "Aarav Sharma", "101", "10", "A", "+919876543230", "Deepak Sharma", "+919876543280", "+919876543280", "ACTIVE", "TCH101"),
            StudentEntity("STU_DPA_102", "ORG_DPA", "Rohan Verma", "102", "10", "A", "+919876543231", "Sunil Verma", "+919876543281", "+919876543281", "ACTIVE", "TCH101"),
            StudentEntity("STU_DPA_201", "ORG_DPA", "Priya Singh", "201", "10", "B", "+919876543232", "Manish Singh", "+919876543282", "+919876543282", "ACTIVE", "TCH102"),

            // DAV Students
            StudentEntity("STU_DAV_1101", "ORG_DAV", "Aditya Saxena", "1101", "11", "A", "+919876543250", "Naveen Saxena", "+919876543283", "+919876543283", "ACTIVE", "TCH201"),

            // ABC Coaching Students (Aarav Sharma is DUAL-ENROLLED in both DPA School and ABC Coaching!)
            StudentEntity("STU_ABC_501", "ORG_ABC", "Aarav Sharma", "501", "JEE", "A", "+919876543230", "Deepak Sharma", "+919876543280", "+919876543280", "ACTIVE", "TCH301"),
            StudentEntity("STU_ABC_502", "ORG_ABC", "Tanvi Mathur", "502", "JEE", "A", "+919876543270", "Rajesh Mathur", "+919876543284", "+919876543284", "ACTIVE", "TCH301")
        )
        database.studentDao().insertStudents(students)

        // Seed Student Memberships
        for (stu in students) {
            memberships.add(
                InstitutionMembershipEntity(
                    membershipId = UUID.randomUUID().toString(),
                    userId = stu.studentId,
                    phoneNumber = stu.phoneNumber,
                    organizationId = stu.organizationId,
                    role = "STUDENT",
                    status = "ACTIVE",
                    createdBy = stu.enrolledByTeacherId
                )
            )
        }
        database.institutionMembershipDao().insertMemberships(memberships)

        // 6. Seed Timetables for each institution
        val timetableEntries = mutableListOf<TimetableEntity>()
        val teachingPeriods = listOf(1, 2, 4, 5, 7, 8, 9)

        // DPA Timetable (Grades UKG - 12)
        val gradesDPA = listOf("UKG", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
        val dpaTeachers = dpaStaff.filter { it.role == "TEACHER" }
        for (day in 1..5) {
            for (grade in gradesDPA) {
                val sections = if (grade in listOf("9", "10", "11", "12")) listOf("A", "B", "C") else listOf("A", "B")
                for (section in sections) {
                    for ((pIdx, periodNum) in teachingPeriods.withIndex()) {
                        val teacherIdx = (grade.hashCode() + section.hashCode() + periodNum + day).mod(dpaTeachers.size)
                        val teacher = dpaTeachers[teacherIdx]
                        val subject = teacher.subjects.split(",").first().trim()
                        val roomNo = when (grade) {
                            "UKG", "1", "2" -> "Room 10${(pIdx % 4) + 1}"
                            "3", "4", "5" -> "Room 20${(pIdx % 4) + 1}"
                            "6", "7", "8" -> "Room 30${(pIdx % 4) + 1}"
                            "9", "10" -> "Room 40${(pIdx % 4) + 1}"
                            else -> "Room 50${(pIdx % 4) + 1}"
                        }
                        timetableEntries.add(
                            TimetableEntity(
                                organizationId = "ORG_DPA",
                                dayOfWeek = day,
                                periodNumber = periodNum,
                                grade = grade,
                                section = section,
                                subject = subject,
                                teacherId = teacher.id,
                                roomNo = roomNo
                            )
                        )
                    }
                }
            }
        }

        // DAV Timetable (Grade 11-A)
        for (day in 1..5) {
            for ((pIdx, periodNum) in teachingPeriods.withIndex()) {
                timetableEntries.add(
                    TimetableEntity(
                        organizationId = "ORG_DAV",
                        dayOfWeek = day,
                        periodNumber = periodNum,
                        grade = "11",
                        section = "A",
                        subject = if (pIdx % 2 == 0) "Physics" else "Chemistry",
                        teacherId = "TCH201",
                        roomNo = "DAV Room 201"
                    )
                )
            }
        }

        // ABC Coaching Timetable (Batch JEE-A)
        for (day in 1..5) {
            for ((pIdx, periodNum) in teachingPeriods.withIndex()) {
                timetableEntries.add(
                    TimetableEntity(
                        organizationId = "ORG_ABC",
                        dayOfWeek = day,
                        periodNumber = periodNum,
                        grade = "JEE",
                        section = "A",
                        subject = if (pIdx % 2 == 0) "Advanced Mechanics" else "Electromagnetism",
                        teacherId = "TCH301",
                        roomNo = "Hall Alpha"
                    )
                )
            }
        }
        database.timetableDao().insertTimetable(timetableEntries)

        // 7. Seed Teams for DPA
        val dpaTeams = listOf(
            TeamPostEntity(
                id = UUID.randomUUID().toString(),
                organizationId = "ORG_DPA",
                title = "Student Dispersal & Bus Bay Safety Team",
                description = "Ensures safe, disciplined departure of junior and senior students at dismissal time.",
                leaderId = "TCH108",
                memberIds = "TCH108,TCH101,TCH104,STF001",
                workingHours = "13:45 - 14:30",
                responsibilities = "1. Station at gate 1 & gate 2\n2. Monitor student bus embarkation\n3. Coordinate traffic flow"
            )
        )
        for (team in dpaTeams) {
            database.teamDao().insertTeam(team)
        }

        // 8. Seed Meetings for DPA and ABC
        val todayStr = LocalDate.now().toString()
        database.meetingDao().insertMeeting(
            MeetingEntity(
                id = UUID.randomUUID().toString(),
                organizationId = "ORG_DPA",
                title = "Dispersal Planning & Campus Safety Protocol",
                description = "Reviewing safety measures for student dispersal.",
                date = todayStr,
                startTime = "14:15",
                endTime = "14:45",
                location = "Principal's Conference Suite (Room 101)",
                organizerId = "PRN001",
                organizerName = "Prof. Meenakshi Sundaram",
                participantIds = "TCH108,TCH101,TCH104,STF001,ADM001",
                priority = "HIGH",
                status = "SCHEDULED"
            )
        )
        database.meetingDao().insertMeeting(
            MeetingEntity(
                id = UUID.randomUUID().toString(),
                organizationId = "ORG_ABC",
                title = "JEE Advanced Mock Test Analysis",
                description = "Evaluation of student question attempts and syllabus pacing.",
                date = todayStr,
                startTime = "16:00",
                endTime = "17:00",
                location = "Director's Boardroom",
                organizerId = "DIR003",
                organizerName = "Er. Sanjeev Kapoor",
                participantIds = "TCH301,ADM003",
                priority = "HIGH",
                status = "SCHEDULED"
            )
        )

        // 9. Seed Notifications
        database.notificationDao().insertNotification(
            NotificationEntity(
                id = UUID.randomUUID().toString(),
                organizationId = "ORG_DPA",
                targetStaffId = "ALL",
                title = "Daily Operations Active",
                message = "Welcome to Delhi Public Academy. Please ensure classroom verification is completed promptly.",
                type = "PRINCIPAL_NOTICE"
            )
        )
        database.notificationDao().insertNotification(
            NotificationEntity(
                id = UUID.randomUUID().toString(),
                organizationId = "ORG_ABC",
                targetStaffId = "ALL",
                title = "Mock Test Series Schedule",
                message = "All enrolled students must register for the upcoming All-India JEE Mock Exam by Friday.",
                type = "PRINCIPAL_NOTICE"
            )
        )
    }
}
