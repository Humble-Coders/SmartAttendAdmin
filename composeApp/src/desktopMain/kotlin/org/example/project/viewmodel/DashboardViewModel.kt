package org.example.project.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.firebase.repository.AttendanceRepository
import org.example.project.models.DashboardOverview
import org.example.project.models.SubjectAttendance
import org.example.project.utils.DateUtils

class DashboardViewModel(
    private val repository: AttendanceRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val currentYear = DateUtils.getCurrentYear()
                val currentMonth = DateUtils.getCurrentMonth()

                // Load all basic data
                val groups = repository.getAllGroups(currentYear, currentMonth)
                val subjects = repository.getAllSubjects(currentYear, currentMonth)
                val availableMonths = repository.getAvailableMonths(currentYear)
                val dashboardOverview = repository.getDashboardOverview(currentYear, currentMonth)
                val dashboardStats = repository.getDashboardStats(currentYear, currentMonth)

                _uiState.value = _uiState.value.copy(
                    year = currentYear,
                    month = currentMonth,
                    availableGroups = groups,
                    availableSubjects = subjects,
                    availableMonths = availableMonths,
                    dashboardOverview = dashboardOverview,
                    subjectAttendanceData = dashboardStats,
                    error = null
                )

                println("Smart Attend: Dashboard loaded - ${subjects.size} subjects, ${groups.size} groups")
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
        loadData()
    }

    fun filterBySubject(subject: String?) {
        _uiState.value = _uiState.value.copy(selectedSubject = subject)
        loadData()
    }

    fun changeMonth(year: Int, month: Int) {
        _uiState.value = _uiState.value.copy(
            year = year,
            month = month,
            selectedGroup = null,
            selectedSubject = null
        )
        loadData()
    }

    fun refreshData() {
        loadData()
    }

    private fun loadData() {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val currentState = _uiState.value

                // Load filtered data
                val groups = repository.getAllGroups(currentState.year, currentState.month)
                val subjects = repository.getAllSubjects(currentState.year, currentState.month)
                val availableMonths = repository.getAvailableMonths(currentState.year)
                val dashboardOverview = repository.getDashboardOverview(
                    currentState.year,
                    currentState.month,
                    currentState.selectedGroup
                )
                val dashboardStats = repository.getDashboardStats(
                    currentState.year,
                    currentState.month,
                    currentState.selectedGroup
                )

                // Apply subject filter if selected
                val filteredStats = if (currentState.selectedSubject != null) {
                    dashboardStats.filterKeys { it == currentState.selectedSubject }
                } else {
                    dashboardStats
                }

                _uiState.value = currentState.copy(
                    availableGroups = groups,
                    availableSubjects = subjects,
                    availableMonths = availableMonths,
                    dashboardOverview = dashboardOverview,
                    subjectAttendanceData = filteredStats,
                    error = null
                )
            } catch (e: Exception) {
                println("Smart Attend: Error loading filtered data - ${e.message}")
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun exportData(format: ExportFormat) {
        screenModelScope.launch {
            try {
                // TODO: Implement export functionality
                println("Smart Attend: Exporting data in $format format")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Export failed: ${e.message}")
            }
        }
    }
}

data class DashboardUiState(
    val year: Int = DateUtils.getCurrentYear(),
    val month: Int = DateUtils.getCurrentMonth(),
    val selectedGroup: String? = null,
    val selectedSubject: String? = null,
    val availableGroups: List<String> = emptyList(),
    val availableSubjects: List<String> = emptyList(),
    val availableMonths: List<Int> = emptyList(),
    val dashboardOverview: DashboardOverview = DashboardOverview(0, 0, 0.0, emptyList(), emptyList()),
    val subjectAttendanceData: Map<String, SubjectAttendance> = emptyMap(),
    val error: String? = null
)

enum class ExportFormat {
    PDF, CSV, EXCEL
}