package org.example.project.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.models.StudentAttendanceSummary
import org.example.project.models.SubjectTypeStats
import org.example.project.ui.theme.AppColors
import org.example.project.ui.theme.AppTypography
import org.example.project.utils.Constants


@Composable
fun StudentCard(
    student: StudentAttendanceSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Student Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Student Avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(AppColors.Primary, AppColors.PrimaryDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.rollNumber.takeLast(2),
                            style = AppTypography.titleMedium,
                            color = AppColors.Surface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = student.rollNumber,
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = student.name,
                            style = AppTypography.bodyMedium,
                            color = AppColors.TextSecondary
                        )

                        // Attendance Grade Badge
                        Box(
                            modifier = Modifier
                                .background(
                                    getAttendanceColor(student.stats.percentage).copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = Constants.getAttendanceGrade(student.stats.percentage),
                                style = AppTypography.bodySmall,
                                color = getAttendanceColor(student.stats.percentage),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Overall Percentage
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${student.stats.percentage.toInt()}%",
                        style = AppTypography.headlineMedium,
                        color = getAttendanceColor(student.stats.percentage),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${student.stats.attendedClasses}/${student.stats.totalClasses}",
                        style = AppTypography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Class Type Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (student.stats.lectureStats.total > 0) {
                    AttendanceTypeChip(
                        label = "Lectures",
                        stats = student.stats.lectureStats,
                        icon = Icons.Default.MenuBook,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (student.stats.tutorialStats.total > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AttendanceTypeChip(
                        label = "Tutorials",
                        stats = student.stats.tutorialStats,
                        icon = Icons.Default.School,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (student.stats.labStats.total > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AttendanceTypeChip(
                        label = "Labs",
                        stats = student.stats.labStats,
                        icon = Icons.Default.Science,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { (student.stats.percentage / 100).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = getAttendanceColor(student.stats.percentage),
                trackColor = AppColors.Divider
            )
        }
    }
}

@Composable
private fun AttendanceTypeChip(
    label: String,
    stats: SubjectTypeStats,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = getAttendanceColor(stats.percentage).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = getAttendanceColor(stats.percentage),
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                style = AppTypography.bodySmall,
                color = AppColors.TextSecondary
            )

            Text(
                text = "${stats.attended}/${stats.total}",
                style = AppTypography.bodySmall,
                color = getAttendanceColor(stats.percentage),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun getAttendanceColor(percentage: Double): Color = when {
    percentage >= Constants.AttendanceThresholds.EXCELLENT -> AppColors.Success
    percentage >= Constants.AttendanceThresholds.GOOD -> Color(0xFF059669) // Darker green
    percentage >= Constants.AttendanceThresholds.SATISFACTORY -> AppColors.Warning
    percentage >= Constants.AttendanceThresholds.NEEDS_IMPROVEMENT -> Color(0xFFEA580C) // Darker orange
    else -> AppColors.Error
}