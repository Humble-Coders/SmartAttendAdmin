package org.example.project.utils

import kotlinx.datetime.*
import java.time.format.TextStyle
import java.util.*

object DateUtils {
    fun getCurrentYear(): Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year

    fun getCurrentMonth(): Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber

    fun getMonthName(month: Int): String {
        return java.time.Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())
    }

    fun getShortMonthName(month: Int): String {
        return java.time.Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

    fun formatDate(dateString: String): String {
        return try {
            val localDate = LocalDate.parse(dateString)
            "${localDate.dayOfMonth} ${getShortMonthName(localDate.monthNumber)}, ${localDate.year}"
        } catch (e: Exception) {
            dateString
        }
    }

    fun getDateRange(year: Int, month: Int): String {
        val monthName = getMonthName(month)
        return "$monthName $year"
    }

    fun getAllMonths(): List<Pair<Int, String>> {
        return (1..12).map { month ->
            month to getMonthName(month)
        }
    }

    fun getYearRange(): List<Int> {
        val currentYear = getCurrentYear()
        return (currentYear - 2..currentYear + 1).toList()
    }
}