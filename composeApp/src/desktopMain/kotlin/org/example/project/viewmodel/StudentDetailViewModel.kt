package org.example.project.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.firebase.repository.AttendanceRepository
import org.example.project.models.AttendanceStats
import org.example.project.utils.DateUtils


class StudentDetailViewModel(
    private val repository: AttendanceRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(StudentDetailUiState())
    val uiState: StateFlow<StudentDetailUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadStudentData(rollNumber: String) {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val currentYear = DateUtils.getCurrentYear()
                val currentMonth = DateUtils.getCurrentMonth()

                val attendanceData = repository.getStudentDetailedAttendance(
                    rollNumber, currentYear, currentMonth
                )

                _uiState.value = _uiState.value.copy(
                    rollNumber = rollNumber,
                    year = currentYear,
                    month = currentMonth,
                    subjectWiseAttendance = attendanceData,
                    error = null
                )

                println("Smart Attend: Student data loaded for $rollNumber - ${attendanceData.size} subjects")
            } catch (e: Exception) {
                println("Smart Attend: Error loading student data - ${e.message}")
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changeMonth(year: Int, month: Int) {
        val currentRollNumber = _uiState.value.rollNumber
        if (currentRollNumber.isNotEmpty()) {
            screenModelScope.launch {
                _isLoading.value = true
                try {
                    val attendanceData = repository.getStudentDetailedAttendance(
                        currentRollNumber, year, month
                    )

                    _uiState.value = _uiState.value.copy(
                        year = year,
                        month = month,
                        subjectWiseAttendance = attendanceData,
                        error = null
                    )
                } catch (e: Exception) {
                    println("Smart Attend: Error changing month - ${e.message}")
                    _uiState.value = _uiState.value.copy(error = e.message)
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class StudentDetailUiState(
    val rollNumber: String = "",
    val year: Int = DateUtils.getCurrentYear(),
    val month: Int = DateUtils.getCurrentMonth(),
    val subjectWiseAttendance: Map<String, AttendanceStats> = emptyMap(),
    val error: String? = null
)