package org.example.project.data.firebase.repository


import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.example.project.data.firebase.FirebaseService
import org.example.project.data.firebase.OptimizedFirebaseService
import org.example.project.models.AttendanceMetadata
import org.example.project.models.AttendanceRecord
import org.example.project.models.AttendanceStats
import org.example.project.models.DashboardOverview
import org.example.project.models.GroupOverview
import org.example.project.models.OptimizedDashboardOverview
import org.example.project.models.StudentAttendanceSummary
import org.example.project.models.SubjectAttendance
import org.example.project.models.SubjectOverview
import org.example.project.models.SubjectStats
import org.example.project.models.SubjectTypeStats

class HybridAttendanceRepository(
    private val legacyService: FirebaseService,
    private val optimizedService: OptimizedFirebaseService
) {

    // Try optimized first, fallback to legacy
    suspend fun getDashboardOverview(
        year: Int,
        month: Int,
        group: String? = null
    ): OptimizedDashboardOverview = coroutineScope {
        val startTime = System.currentTimeMillis()

        try {
            // Try optimized approach first
            val metadataDeferred = async { optimizedService.getAttendanceMetadata(year, month) }
            val subjectStatsDeferred = async { optimizedService.getSubjectStatsOptimized(year, month, group) }

            val metadata = metadataDeferred.await()
            val subjectStats = subjectStatsDeferred.await()

            // If we got data from optimized collections, use it
            if (metadata.subjects.isNotEmpty() || subjectStats.isNotEmpty()) {
                val groupStats = calculateGroupStatsFromSubjectStats(subjectStats)

                return@coroutineScope OptimizedDashboardOverview(
                    metadata = metadata,
                    subjectStats = subjectStats,
                    groupStats = groupStats,
                    isOptimized = true,
                    loadTime = System.currentTimeMillis() - startTime
                )
            }
        } catch (e: Exception) {
            println("Smart Attend: Optimized query failed, falling back to legacy: ${e.message}")
        }

        // Fallback to legacy approach
        println("Smart Attend: Using legacy data loading...")
        val legacyOverview = getLegacyDashboardOverview(year, month, group)

        OptimizedDashboardOverview(
            metadata = AttendanceMetadata(
                subjects = emptyList(),
                groups = emptyList(),
                totalStudents = legacyOverview.totalStudents,
                totalClasses = legacyOverview.totalClasses,
                overallAttendanceRate = legacyOverview.overallAttendance
            ),
            subjectStats = emptyList(),
            groupStats = legacyOverview.groupStats,
            isOptimized = false,
            loadTime = System.currentTimeMillis() - startTime
        )
    }

    suspend fun getSubjectAttendanceOptimized(
        subject: String,
        year: Int,
        month: Int,
        group: String? = null,
        page: Int = 0,
        pageSize: Int = 50
    ): SubjectAttendance {
        return try {
            // Try optimized paginated approach
            val paginatedStudents = optimizedService.getStudentsPaginated(
                subject = subject,
                year = year,
                month = month,
                group = group,
                limit = pageSize,
                lastRollNumber = null // You'd implement proper pagination here
            )

            if (paginatedStudents.students.isNotEmpty()) {
                SubjectAttendance(subject, paginatedStudents.students)
            } else {
                // Fallback to legacy
                getLegacySubjectAttendance(subject, year, month, group)
            }
        } catch (e: Exception) {
            println("Smart Attend: Optimized subject query failed, using legacy: ${e.message}")
            getLegacySubjectAttendance(subject, year, month, group)
        }
    }

    suspend fun getStudentDetailedAttendanceOptimized(
        rollNumber: String,
        year: Int,
        month: Int
    ): Map<String, AttendanceStats> {
        return try {
            // Try optimized approach
            val studentStats = optimizedService.getStudentStatsOptimized(rollNumber, year, month)

            if (studentStats != null) {
                studentStats.subjects.mapValues { (_, subjectStats) ->
                    AttendanceStats(
                        totalClasses = subjectStats.totalClasses,
                        attendedClasses = subjectStats.attended,
                        percentage = subjectStats.percentage,
                        lectureStats = subjectStats.lectureStats,
                        tutorialStats = subjectStats.tutorialStats,
                        labStats = subjectStats.labStats
                    )
                }
            } else {
                // Fallback to legacy
                getLegacyStudentAttendance(rollNumber, year, month)
            }
        } catch (e: Exception) {
            println("Smart Attend: Optimized student query failed, using legacy: ${e.message}")
            getLegacyStudentAttendance(rollNumber, year, month)
        }
    }

    suspend fun getAllSubjectsAndGroups(year: Int, month: Int): Pair<List<String>, List<String>> {
        return try {
            // Try optimized metadata first
            val metadata = optimizedService.getAttendanceMetadata(year, month)

            if (metadata.subjects.isNotEmpty() || metadata.groups.isNotEmpty()) {
                Pair(metadata.subjects, metadata.groups)
            } else {
                // Fallback to legacy
                val subjects = legacyService.getAllSubjects(year, month)
                val groups = legacyService.getAllGroups(year, month)
                Pair(subjects, groups)
            }
        } catch (e: Exception) {
            println("Smart Attend: Error fetching subjects/groups: ${e.message}")
            Pair(emptyList(), emptyList())
        }
    }

    // LEGACY FALLBACK METHODS
    private suspend fun getLegacyDashboardOverview(
        year: Int,
        month: Int,
        group: String? = null
    ): DashboardOverview {
        val records = legacyService.getAttendanceRecords(year, month, group)

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

    private suspend fun getLegacySubjectAttendance(
        subject: String,
        year: Int,
        month: Int,
        group: String? = null
    ): SubjectAttendance {
        val records = legacyService.getAttendanceRecords(year, month, group, subject)
        val studentGroups = records.groupBy { it.rollNumber }

        val students = studentGroups.map { (rollNumber, studentRecords) ->
            val stats = calculateAttendanceStats(studentRecords)
            StudentAttendanceSummary(
                rollNumber = rollNumber,
                name = "Student $rollNumber",
                stats = stats
            )
        }.sortedBy { it.rollNumber }

        return SubjectAttendance(subject, students)
    }

    private suspend fun getLegacyStudentAttendance(
        rollNumber: String,
        year: Int,
        month: Int
    ): Map<String, AttendanceStats> {
        val records = legacyService.getStudentAttendance(rollNumber, year, month)
        return records.groupBy { it.subject }.mapValues { (_, subjectRecords) ->
            calculateAttendanceStats(subjectRecords)
        }
    }

    // HELPER METHODS
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

    private fun calculateGroupStatsFromSubjectStats(subjectStats: List<SubjectStats>): List<GroupOverview> {
        return subjectStats.groupBy { it.group }.map { (group, groupSubjects) ->
            val totalClasses = groupSubjects.sumOf { it.totalClasses }
            val averageAttendance = if (groupSubjects.isNotEmpty()) {
                groupSubjects.sumOf { it.attendanceRate } / groupSubjects.size
            } else 0.0

            GroupOverview(
                group = group,
                totalClasses = totalClasses,
                averageAttendance = averageAttendance
            )
        }.sortedBy { it.group }
    }
}