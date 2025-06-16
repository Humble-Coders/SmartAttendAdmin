package org.example.project.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.models.StudentAttendanceSummary
import org.example.project.models.SubjectAttendance
import org.example.project.ui.theme.AppColors
import org.example.project.ui.theme.AppTypography
import org.example.project.ui.theme.components.StatsCard
import org.example.project.ui.theme.components.StudentCard
import org.example.project.ui.theme.components.getAttendanceColor
import org.example.project.utils.Constants

@Composable
fun SubjectAttendanceSection(
    subject: String,
    subjectData: SubjectAttendance,
    onStudentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Subject Header
            SubjectHeader(
                subject = subject,
                studentCount = subjectData.students.size,
                averageAttendance = if (subjectData.students.isNotEmpty()) {
                    subjectData.students.sumOf { it.stats.percentage } / subjectData.students.size
                } else 0.0
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Subject Statistics
            SubjectStatistics(subjectData = subjectData)

            Spacer(modifier = Modifier.height(24.dp))

            // Students Section
            StudentsSection(
                students = subjectData.students,
                onStudentClick = onStudentClick
            )
        }
    }
}

@Composable
private fun SubjectHeader(
    subject: String,
    studentCount: Int,
    averageAttendance: Double
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        AppColors.Primary.copy(alpha = 0.1f),
                        AppColors.Secondary.copy(alpha = 0.1f)
                    )
                ),
                RoundedCornerShape(12.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = "Subject",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = subject,
                        style = AppTypography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip(
                        icon = Icons.Default.People,
                        text = "$studentCount students",
                        color = AppColors.Secondary
                    )

                    InfoChip(
                        icon = Icons.Default.TrendingUp,
                        text = "${averageAttendance.toInt()}% avg",
                        color = getAttendanceColor(averageAttendance)
                    )
                }
            }

            // Overall Grade Badge
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = getAttendanceColor(averageAttendance)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = Constants.getAttendanceGrade(averageAttendance),
                    style = AppTypography.titleMedium,
                    color = AppColors.Surface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = AppTypography.bodyMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SubjectStatistics(
    subjectData: SubjectAttendance
) {
    if (subjectData.students.isEmpty()) return

    val totalLectures = subjectData.students.sumOf { it.stats.lectureStats.total }
    val attendedLectures = subjectData.students.sumOf { it.stats.lectureStats.attended }
    val lecturePercentage = if (totalLectures > 0) (attendedLectures.toDouble() / totalLectures) * 100 else 0.0

    val totalTutorials = subjectData.students.sumOf { it.stats.tutorialStats.total }
    val attendedTutorials = subjectData.students.sumOf { it.stats.tutorialStats.attended }
    val tutorialPercentage = if (totalTutorials > 0) (attendedTutorials.toDouble() / totalTutorials) * 100 else 0.0

    val totalLabs = subjectData.students.sumOf { it.stats.labStats.total }
    val attendedLabs = subjectData.students.sumOf { it.stats.labStats.attended }
    val labPercentage = if (totalLabs > 0) (attendedLabs.toDouble() / totalLabs) * 100 else 0.0

    Column {
        Text(
            text = "Class Type Performance",
            style = AppTypography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (totalLectures > 0) {
                StatsCard(
                    title = "Lectures",
                    percentage = lecturePercentage,
                    attended = attendedLectures,
                    total = totalLectures,
                    modifier = Modifier.weight(1f),
                    color = AppColors.Primary,
                    icon = Icons.Default.MenuBook
                )
            }

            if (totalTutorials > 0) {
                StatsCard(
                    title = "Tutorials",
                    percentage = tutorialPercentage,
                    attended = attendedTutorials,
                    total = totalTutorials,
                    modifier = Modifier.weight(1f),
                    color = AppColors.Secondary,
                    icon = Icons.Default.School
                )
            }

            if (totalLabs > 0) {
                StatsCard(
                    title = "Labs",
                    percentage = labPercentage,
                    attended = attendedLabs,
                    total = totalLabs,
                    modifier = Modifier.weight(1f),
                    color = AppColors.Accent,
                    icon = Icons.Default.Science
                )
            }
        }
    }
}

@Composable
private fun StudentsSection(
    students: List<StudentAttendanceSummary>,
    onStudentClick: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Students (${students.size})",
                style = AppTypography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )

            if (students.isNotEmpty()) {
                val excellentCount = students.count { it.stats.percentage >= 90 }
                val goodCount = students.count { it.stats.percentage >= 75 }
                val needsImprovementCount = students.count { it.stats.percentage < 65 }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (excellentCount > 0) {
                        StatusBadge(
                            count = excellentCount,
                            label = "Excellent",
                            color = AppColors.Success
                        )
                    }
                    if (goodCount > 0) {
                        StatusBadge(
                            count = goodCount,
                            label = "Good",
                            color = AppColors.Primary
                        )
                    }
                    if (needsImprovementCount > 0) {
                        StatusBadge(
                            count = needsImprovementCount,
                            label = "Needs Attention",
                            color = AppColors.Error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (students.isEmpty()) {
            EmptyStudentsCard()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(350.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(
                    if (students.size <= 6) {
                        (((students.size + 1) / 2) * 180).dp
                    } else {
                        600.dp
                    }
                )
            ) {
                items(students) { student ->
                    StudentCard(
                        student = student,
                        onClick = { onStudentClick(student.rollNumber) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    count: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "$count $label",
            style = AppTypography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyStudentsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardSecondary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.PeopleOutline,
                contentDescription = "No Students",
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No students enrolled",
                style = AppTypography.titleMedium,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Students will appear here once attendance is recorded",
                style = AppTypography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
    }
}