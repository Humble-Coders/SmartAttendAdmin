package org.example.project.models

data class AttendanceRecord(
    val date: String = "",
    val deviceRoom: String = "",
    val group: String = "",
    val isExtra: Boolean = false,
    val present: Boolean = false,
    val rollNumber: String = "",
    val subject: String = "",
    val timestamp: String = "",
    val type: String = ""
)

data class AttendanceStats(
    val totalClasses: Int = 0,
    val attendedClasses: Int = 0,
    val percentage: Double = 0.0,
    val lectureStats: SubjectTypeStats = SubjectTypeStats(),
    val tutorialStats: SubjectTypeStats = SubjectTypeStats(),
    val labStats: SubjectTypeStats = SubjectTypeStats()
)

data class SubjectTypeStats(
    val total: Int = 0,
    val attended: Int = 0,
    val percentage: Double = 0.0
)

data class SubjectAttendance(
    val subject: String,
    val students: List<StudentAttendanceSummary>
)

data class StudentAttendanceSummary(
    val rollNumber: String,
    val name: String,
    val stats: AttendanceStats
)

data class DashboardOverview(
    val totalStudents: Int,
    val totalClasses: Int,
    val overallAttendance: Double,
    val subjectStats: List<SubjectOverview>,
    val groupStats: List<GroupOverview>
)

data class SubjectOverview(
    val subject: String,
    val totalClasses: Int,
    val averageAttendance: Double
)

data class GroupOverview(
    val group: String,
    val totalClasses: Int,
    val averageAttendance: Double
)

// Total classes for each subject from subjects collection
data class SubjectTotals(
    val lectTotal: Int = 0,
    val labTotal: Int = 0,
    val tutTotal: Int = 0
) {
    val totalClasses: Int get() = lectTotal + labTotal + tutTotal
}