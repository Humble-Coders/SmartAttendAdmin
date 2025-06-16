package org.example.project.utils

object Constants {
    const val APP_NAME = "Smart Attend"
    const val APP_VERSION = "1.0.0"
    const val WINDOW_WIDTH = 1400
    const val WINDOW_HEIGHT = 900

    object AttendanceType {
        const val LECTURE = "lect"
        const val TUTORIAL = "tut"
        const val LAB = "lab"
    }

    object AttendanceTypeDisplay {
        const val LECTURE = "Lecture"
        const val TUTORIAL = "Tutorial"
        const val LAB = "Lab"
    }

    fun getDisplayType(type: String): String = when(type) {
        AttendanceType.LECTURE -> AttendanceTypeDisplay.LECTURE
        AttendanceType.TUTORIAL -> AttendanceTypeDisplay.TUTORIAL
        AttendanceType.LAB -> AttendanceTypeDisplay.LAB
        else -> type.replaceFirstChar { it.uppercase() }
    }

    fun getTypeIcon(type: String): String = when(type) {
        AttendanceType.LECTURE -> "📚"
        AttendanceType.TUTORIAL -> "👨‍🏫"
        AttendanceType.LAB -> "🔬"
        else -> "📋"
    }

    object AttendanceThresholds {
        const val EXCELLENT = 90.0
        const val GOOD = 75.0
        const val SATISFACTORY = 65.0
        const val NEEDS_IMPROVEMENT = 50.0
    }

    fun getAttendanceGrade(percentage: Double): String = when {
        percentage >= AttendanceThresholds.EXCELLENT -> "Excellent"
        percentage >= AttendanceThresholds.GOOD -> "Good"
        percentage >= AttendanceThresholds.SATISFACTORY -> "Satisfactory"
        percentage >= AttendanceThresholds.NEEDS_IMPROVEMENT -> "Needs Improvement"
        else -> "Poor"
    }
}