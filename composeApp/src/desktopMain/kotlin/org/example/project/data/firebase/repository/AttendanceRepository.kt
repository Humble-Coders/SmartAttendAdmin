package org.example.project.data.firebase.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.example.project.data.firebase.FirebaseService
import org.example.project.models.*

class AttendanceRepository(private val firebaseService: FirebaseService) {

    // FAST: Get all dashboard data in one go using async
    suspend fun getDashboardOverviewFast(
        year: Int,
        month: Int,
        group: String? = null
    ): DashboardOverview = coroutineScope {
        val dashboardData = firebaseService.getDashboardDataFast(year, month, group)

        val records = dashboardData.records
        val totalStudents = records.map { it.rollNumber }.distinct().size
        val totalClasses = records.size
        val presentCount = records.count { it.present }
        val overallAttendance = if (totalClasses > 0) (presentCount.toDouble() / totalClasses) * 100 else 0.0

        // Calculate subject stats
        val subjectStats = records.groupBy { it.subject }.map { (subject, subjectRecords) ->
            val subjectPresent = subjectRecords.count { it.present }
            val subjectTotal = subjectRecords.size
            val subjectPercentage = if (subjectTotal > 0) (subjectPresent.toDouble() / subjectTotal) * 100 else 0.0

            SubjectOverview(subject, subjectTotal, subjectPercentage)
        }.sortedBy { it.subject }

        // Calculate group stats
        val groupStats = records.groupBy { it.group }.map { (groupName, groupRecords) ->
            val groupPresent = groupRecords.count { it.present }
            val groupTotal = groupRecords.size
            val groupPercentage = if (groupTotal > 0) (groupPresent.toDouble() / groupTotal) * 100 else 0.0

            GroupOverview(groupName, groupTotal, groupPercentage)
        }.sortedBy { it.group }

        DashboardOverview(totalStudents, totalClasses, overallAttendance, subjectStats, groupStats)
    }

    // FAST: Get subject attendance using parallel processing
    suspend fun getSubjectAttendanceFast(
        subjects: List<String>,
        year: Int,
        month: Int,
        group: String? = null
    ): Map<String, SubjectAttendance> = coroutineScope {
        // Process subjects in parallel
        val subjectDeferred = subjects.map { subject ->
            async {
                val records = firebaseService.getAttendanceRecords(year, month, group, subject)
                val studentGroups = records.groupBy { it.rollNumber }

                val students = studentGroups.map { (rollNumber, studentRecords) ->
                    val stats = calculateAttendanceStats(studentRecords)
                    StudentAttendanceSummary(rollNumber, "Student $rollNumber", stats)
                }.sortedBy { it.rollNumber }

                subject to SubjectAttendance(subject, students)
            }
        }

        subjectDeferred.awaitAll().toMap()
    }

    suspend fun getStudentDetailedAttendance(
        rollNumber: String,
        year: Int,
        month: Int
    ): Map<String, AttendanceStats> = coroutineScope {
        val records = firebaseService.getAttendanceRecords(year, month).filter { it.rollNumber == rollNumber }
        records.groupBy { it.subject }.mapValues { (_, subjectRecords) ->
            calculateAttendanceStats(subjectRecords)
        }
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

        return AttendanceStats(totalClasses, attendedClasses, percentage, lectureStats, tutorialStats, labStats)
    }

    private fun calculateSubjectTypeStats(records: List<AttendanceRecord>): SubjectTypeStats {
        val total = records.size
        val attended = records.count { it.present }
        val percentage = if (total > 0) (attended.toDouble() / total) * 100 else 0.0
        return SubjectTypeStats(total, attended, percentage)
    }
}