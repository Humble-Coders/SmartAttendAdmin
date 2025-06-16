package org.example.project.data.firebase.repository

import org.example.project.data.firebase.FirebaseService
import org.example.project.models.AttendanceRecord
import org.example.project.models.AttendanceStats
import org.example.project.models.DashboardOverview
import org.example.project.models.GroupOverview
import org.example.project.models.StudentAttendanceSummary
import org.example.project.models.SubjectAttendance
import org.example.project.models.SubjectOverview
import org.example.project.models.SubjectTypeStats

class AttendanceRepository(private val firebaseService: FirebaseService) {

    suspend fun getSubjectAttendance(
        subject: String,
        year: Int,
        month: Int,
        group: String? = null
    ): SubjectAttendance {
        val records = firebaseService.getAttendanceRecords(year, month, group, subject)
        val studentGroups = records.groupBy { it.rollNumber }

        val students = studentGroups.map { (rollNumber, studentRecords) ->
            val stats = calculateAttendanceStats(studentRecords)
            StudentAttendanceSummary(
                rollNumber = rollNumber,
                name = getStudentName(rollNumber),
                stats = stats
            )
        }.sortedBy { it.rollNumber }

        return SubjectAttendance(subject, students)
    }

    suspend fun getStudentDetailedAttendance(
        rollNumber: String,
        year: Int,
        month: Int
    ): Map<String, AttendanceStats> {
        val records = firebaseService.getStudentAttendance(rollNumber, year, month)
        return records.groupBy { it.subject }.mapValues { (_, subjectRecords) ->
            calculateAttendanceStats(subjectRecords)
        }
    }

    suspend fun getDashboardStats(
        year: Int,
        month: Int,
        group: String? = null
    ): Map<String, SubjectAttendance> {
        val subjects = firebaseService.getAllSubjects(year, month)
        return subjects.associateWith { subject ->
            getSubjectAttendance(subject, year, month, group)
        }
    }

    suspend fun getDashboardOverview(
        year: Int,
        month: Int,
        group: String? = null
    ): DashboardOverview {
        val records = firebaseService.getAttendanceRecords(year, month, group)

        val totalStudents = records.map { it.rollNumber }.distinct().size
        val totalClasses = records.size
        val presentCount = records.count { it.present }
        val overallAttendance = if (totalClasses > 0) (presentCount.toDouble() / totalClasses) * 100 else 0.0

        val subjectStats = records.groupBy { it.subject }.map { (subject, subjectRecords) ->
            val subjectPresent = subjectRecords.count { it.present }
            val subjectTotal = subjectRecords.size
            val subjectPercentage = if (subjectTotal > 0) (subjectPresent.toDouble() / subjectTotal) * 100 else 0.0

            SubjectOverview(
                subject = subject,
                totalClasses = subjectTotal,
                averageAttendance = subjectPercentage
            )
        }.sortedBy { it.subject }

        val groupStats = records.groupBy { it.group }.map { (groupName, groupRecords) ->
            val groupPresent = groupRecords.count { it.present }
            val groupTotal = groupRecords.size
            val groupPercentage = if (groupTotal > 0) (groupPresent.toDouble() / groupTotal) * 100 else 0.0

            GroupOverview(
                group = groupName,
                totalClasses = groupTotal,
                averageAttendance = groupPercentage
            )
        }.sortedBy { it.group }

        return DashboardOverview(
            totalStudents = totalStudents,
            totalClasses = totalClasses,
            overallAttendance = overallAttendance,
            subjectStats = subjectStats,
            groupStats = groupStats
        )
    }

    private fun calculateAttendanceStats(records: List<AttendanceRecord>): AttendanceStats {
        val lectureRecords = records.filter { it.type == "lect" }
        val tutorialRecords = records.filter { it.type == "tut" }
        val labRecords = records.filter { it.type == "lab" }

        val lectureStats = calculateSubjectTypeStats(lectureRecords)
        val tutorialStats = calculateSubjectTypeStats(tutorialRecords)
        val labStats = calculateSubjectTypeStats(labRecords)

        val totalClasses = records.size
        val attendedClasses = records.count { it.present }
        val percentage = if (totalClasses > 0) (attendedClasses.toDouble() / totalClasses) * 100 else 0.0

        return AttendanceStats(
            totalClasses = totalClasses,
            attendedClasses = attendedClasses,
            percentage = percentage,
            lectureStats = lectureStats,
            tutorialStats = tutorialStats,
            labStats = labStats
        )
    }

    private fun calculateSubjectTypeStats(records: List<AttendanceRecord>): SubjectTypeStats {
        val total = records.size
        val attended = records.count { it.present }
        val percentage = if (total > 0) (attended.toDouble() / total) * 100 else 0.0

        return SubjectTypeStats(total, attended, percentage)
    }

    private fun getStudentName(rollNumber: String): String {
        // Enhanced student name logic - you can improve this by creating a students collection
        return "Student $rollNumber"
    }

    suspend fun getAvailableMonths(year: Int): List<Int> {
        return firebaseService.getAvailableMonths(year)
    }

    suspend fun getAllGroups(year: Int, month: Int): List<String> {
        return firebaseService.getAllGroups(year, month)
    }

    suspend fun getAllSubjects(year: Int, month: Int): List<String> {
        return firebaseService.getAllSubjects(year, month)
    }
}