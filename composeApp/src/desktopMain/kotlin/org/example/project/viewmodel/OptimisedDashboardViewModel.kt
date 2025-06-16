package org.example.project.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.firebase.repository.HybridAttendanceRepository
import org.example.project.models.AttendanceMetadata
import org.example.project.models.OptimizedDashboardOverview
import org.example.project.models.StudentAttendanceSummary
import org.example.project.utils.DateUtils


class OptimizedDashboardViewModel(
    private val repository: HybridAttendanceRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(OptimizedDashboardUiState())
    val uiState: StateFlow<OptimizedDashboardUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        screenModelScope.launch {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()

            try {
                val currentYear = DateUtils.getCurrentYear()
                val currentMonth = DateUtils.getCurrentMonth()

                // Load data using hybrid approach (fast optimized + fallback)
                val dashboardOverview = repository.getDashboardOverview(currentYear, currentMonth)
                val (subjects, groups) = repository.getAllSubjectsAndGroups(currentYear, currentMonth)

                // Track performance
                val loadTime = System.currentTimeMillis() - startTime
                _performanceMetrics.value = PerformanceMetrics(
                    lastLoadTime = loadTime,
                    isOptimizedPath = dashboardOverview.isOptimized,
                    totalQueries = if (dashboardOverview.isOptimized) 2 else 10, // Estimated
                    cacheHitRate = if (dashboardOverview.isOptimized) 80.0 else 0.0
                )

                _uiState.value = _uiState.value.copy(
                    year = currentYear,
                    month = currentMonth,
                    availableGroups = groups,
                    availableSubjects = subjects,
                    dashboardOverview = dashboardOverview,
                    subjectAttendanceData = emptyMap(), // Will be loaded on demand
                    error = null
                )

                println("Smart Attend: Dashboard loaded in ${loadTime}ms (optimized: ${dashboardOverview.isOptimized})")

                // Load subject data for current filters
                loadSubjectData()

            } catch (e: Exception) {
                println("Smart Attend: Error loading dashboard - ${e.message}")
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterByGroup(group: String?) {
        _uiState.value = _uiState.value.copy(selectedGroup = group)
        loadSubjectData()
    }

    fun filterBySubject(subject: String?) {
        _uiState.value = _uiState.value.copy(selectedSubject = subject)
        loadSubjectData()
    }

    fun changeMonth(year: Int, month: Int) {
        _uiState.value = _uiState.value.copy(
            year = year,
            month = month,
            selectedGroup = null,
            selectedSubject = null
        )
        loadInitialData() // Reload everything for new month
    }

    fun loadMoreStudents(subject: String) {
        val currentData = _uiState.value.subjectAttendanceData[subject]
        if (currentData?.hasMore != true) return

        screenModelScope.launch {
            try {
                val currentState = _uiState.value
                val moreStudents = repository.getSubjectAttendanceOptimized(
                    subject = subject,
                    year = currentState.year,
                    month = currentState.month,
                    group = currentState.selectedGroup,
                    page = currentData.currentPage + 1
                )

                // Combine with existing data
                val updatedSubjectData = currentData.copy(
                    students = currentData.students + moreStudents.students,
                    currentPage = currentData.currentPage + 1,
                    hasMore = moreStudents.students.size == 50 // Assuming page size of 50
                )

                val updatedMap = _uiState.value.subjectAttendanceData.toMutableMap()
                updatedMap[subject] = updatedSubjectData

                _uiState.value = _uiState.value.copy(subjectAttendanceData = updatedMap)

            } catch (e: Exception) {
                println("Smart Attend: Error loading more students - ${e.message}")
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun refreshData() {
        // Clear cache and reload
        _performanceMetrics.value = PerformanceMetrics()
        loadInitialData()
    }

    private fun loadSubjectData() {
        screenModelScope.launch {
            try {
                val currentState = _uiState.value
                val startTime = System.currentTimeMillis()

                // Determine which subjects to load
                val subjectsToLoad = if (currentState.selectedSubject != null) {
                    listOf(currentState.selectedSubject)
                } else {
                    currentState.availableSubjects.take(5) // Load first 5 subjects to start
                }

                val subjectDataMap = mutableMapOf<String, PaginatedSubjectAttendance>()

                for (subject in subjectsToLoad) {
                    val subjectAttendance = repository.getSubjectAttendanceOptimized(
                        subject = subject,
                        year = currentState.year,
                        month = currentState.month,
                        group = currentState.selectedGroup,
                        page = 0,
                        pageSize = 50
                    )

                    subjectDataMap[subject] = PaginatedSubjectAttendance(
                        subject = subject,
                        students = subjectAttendance.students,
                        currentPage = 0,
                        hasMore = subjectAttendance.students.size == 50
                    )
                }

                _uiState.value = currentState.copy(
                    subjectAttendanceData = subjectDataMap,
                    error = null
                )

                val loadTime = System.currentTimeMillis() - startTime
                println("Smart Attend: Subject data loaded in ${loadTime}ms")

            } catch (e: Exception) {
                println("Smart Attend: Error loading subject data - ${e.message}")
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

// Enhanced UI State with optimization data
data class OptimizedDashboardUiState(
    val year: Int = DateUtils.getCurrentYear(),
    val month: Int = DateUtils.getCurrentMonth(),
    val selectedGroup: String? = null,
    val selectedSubject: String? = null,
    val availableGroups: List<String> = emptyList(),
    val availableSubjects: List<String> = emptyList(),
    val dashboardOverview: OptimizedDashboardOverview = OptimizedDashboardOverview(
        metadata = AttendanceMetadata(),
        subjectStats = emptyList(),
        groupStats = emptyList()
    ),
    val subjectAttendanceData: Map<String, PaginatedSubjectAttendance> = emptyMap(),
    val error: String? = null
)

// Paginated subject attendance for better performance
data class PaginatedSubjectAttendance(
    val subject: String,
    val students: List<StudentAttendanceSummary>,
    val currentPage: Int = 0,
    val hasMore: Boolean = false
)

// Performance tracking
data class PerformanceMetrics(
    val lastLoadTime: Long = 0,
    val isOptimizedPath: Boolean = false,
    val totalQueries: Int = 0,
    val cacheHitRate: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)