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
        loadDataFast()
    }

    // FAST: Load everything in parallel
    private fun loadDataFast() {
        screenModelScope.launch {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()

            try {
                val currentYear = DateUtils.getCurrentYear()
                val currentMonth = DateUtils.getCurrentMonth()

                // Get overview first (includes subjects/groups data)
                val dashboardOverview = repository.getDashboardOverviewFast(currentYear, currentMonth)

                // Get subject attendance for all subjects in parallel
                val subjectAttendanceData = repository.getSubjectAttendanceFast(
                    subjects = dashboardOverview.subjectStats.map { it.subject },
                    year = currentYear,
                    month = currentMonth
                )

                _uiState.value = _uiState.value.copy(
                    year = currentYear,
                    month = currentMonth,
                    availableGroups = dashboardOverview.groupStats.map { it.group },
                    availableSubjects = dashboardOverview.subjectStats.map { it.subject },
                    dashboardOverview = dashboardOverview,
                    subjectAttendanceData = subjectAttendanceData,
                    error = null
                )

                val loadTime = System.currentTimeMillis() - startTime
                println("Smart Attend: Dashboard loaded in ${loadTime}ms")

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterByGroup(group: String?) {
        _uiState.value = _uiState.value.copy(selectedGroup = group)
        reloadWithFilters()
    }

    fun filterBySubject(subject: String?) {
        _uiState.value = _uiState.value.copy(selectedSubject = subject)
        applySubjectFilter()
    }

    fun changeMonth(year: Int, month: Int) {
        _uiState.value = _uiState.value.copy(
            year = year,
            month = month,
            selectedGroup = null,
            selectedSubject = null
        )
        loadDataFast()
    }

    fun refreshData() = loadDataFast()

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun reloadWithFilters() {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val state = _uiState.value
                val dashboardOverview = repository.getDashboardOverviewFast(state.year, state.month, state.selectedGroup)
                val subjectAttendanceData = repository.getSubjectAttendanceFast(
                    subjects = dashboardOverview.subjectStats.map { it.subject },
                    year = state.year,
                    month = state.month,
                    group = state.selectedGroup
                )

                _uiState.value = state.copy(
                    dashboardOverview = dashboardOverview,
                    subjectAttendanceData = subjectAttendanceData,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applySubjectFilter() {
        val state = _uiState.value
        val filteredData = if (state.selectedSubject != null) {
            state.subjectAttendanceData.filterKeys { it == state.selectedSubject }
        } else {
            state.subjectAttendanceData
        }
        _uiState.value = state.copy(subjectAttendanceData = filteredData)
    }
}

data class DashboardUiState(
    val year: Int = DateUtils.getCurrentYear(),
    val month: Int = DateUtils.getCurrentMonth(),
    val selectedGroup: String? = null,
    val selectedSubject: String? = null,
    val availableGroups: List<String> = emptyList(),
    val availableSubjects: List<String> = emptyList(),
    val availableMonths: List<Int> = listOf(DateUtils.getCurrentMonth()),
    val dashboardOverview: DashboardOverview = DashboardOverview(0, 0, 0.0, emptyList(), emptyList()),
    val subjectAttendanceData: Map<String, SubjectAttendance> = emptyMap(),
    val error: String? = null
)