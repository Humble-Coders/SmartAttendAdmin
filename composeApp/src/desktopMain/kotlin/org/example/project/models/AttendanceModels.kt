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
    val type: String = "" // lect, tut, lab
)

data class Student(
    val rollNumber: String,
    val name: String = "",
    val group: String = "",
    val attendanceRecords: List<AttendanceRecord> = emptyList()
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

// Dashboard Overview Models
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

// Metadata for quick dashboard loading
data class AttendanceMetadata(
    val subjects: List<String> = emptyList(),
    val groups: List<String> = emptyList(),
    val totalStudents: Int = 0,
    val totalClasses: Int = 0,
    val overallAttendanceRate: Double = 0.0,
    val lastUpdated: String = ""
)

// Pre-calculated subject statistics
data class SubjectStats(
    val subject: String = "",
    val group: String = "",
    val totalStudents: Int = 0,
    val totalClasses: Int = 0,
    val presentCount: Int = 0,
    val attendanceRate: Double = 0.0,
    val lectureStats: SubjectTypeStats = SubjectTypeStats(),
    val tutorialStats: SubjectTypeStats = SubjectTypeStats(),
    val labStats: SubjectTypeStats = SubjectTypeStats()
)

// Pre-aggregated student data
data class StudentAggregatedStats(
    val rollNumber: String = "",
    val year: Int = 0,
    val month: Int = 0,
    val group: String = "",
    val subjects: Map<String, SubjectAttendanceStats> = emptyMap(),
    val overallStats: AttendanceStats = AttendanceStats()
)

// Individual subject stats for a student
data class SubjectAttendanceStats(
    val totalClasses: Int = 0,
    val attended: Int = 0,
    val percentage: Double = 0.0,
    val lectureStats: SubjectTypeStats = SubjectTypeStats(),
    val tutorialStats: SubjectTypeStats = SubjectTypeStats(),
    val labStats: SubjectTypeStats = SubjectTypeStats()
)

// Paginated results
data class PaginatedStudents(
    val students: List<StudentAttendanceSummary>,
    val hasMore: Boolean,
    val lastRollNumber: String? = null,
    val totalCount: Int = 0
)

data class PaginatedAttendanceResult(
    val records: List<AttendanceRecord>,
    val hasMore: Boolean,
    val lastDocumentId: String? = null,
    val totalCount: Int = 0
)

// Cache data structures
data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis()
)

// Enhanced dashboard overview with optimization metadata
data class OptimizedDashboardOverview(
    val metadata: AttendanceMetadata,
    val subjectStats: List<SubjectStats>,
    val groupStats: List<GroupOverview>,
    val isOptimized: Boolean = true,
    val loadTime: Long = 0
)