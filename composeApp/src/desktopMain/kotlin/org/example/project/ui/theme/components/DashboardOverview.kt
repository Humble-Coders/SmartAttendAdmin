package org.example.project.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.models.DashboardOverview
import org.example.project.models.GroupOverview
import org.example.project.models.SubjectOverview
import org.example.project.ui.theme.AppColors
import org.example.project.ui.theme.AppTypography


@Composable
fun DashboardOverviewSection(
    overview: DashboardOverview,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Overall Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OverviewCard(
                title = "Total Students",
                value = overview.totalStudents.toString(),
                icon = Icons.Default.People,
                color = AppColors.Primary,
                modifier = Modifier.weight(1f)
            )

            OverviewCard(
                title = "Total Classes",
                value = overview.totalClasses.toString(),
                icon = Icons.Default.Class,
                color = AppColors.Secondary,
                modifier = Modifier.weight(1f)
            )

            OverviewCard(
                title = "Overall Attendance",
                value = "${overview.overallAttendance.toInt()}%",
                icon = Icons.Default.TrendingUp,
                color = getAttendanceColor(overview.overallAttendance),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Subject Performance
        if (overview.subjectStats.isNotEmpty()) {
            SubjectPerformanceSection(
                subjects = overview.subjectStats,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Group Performance
        if (overview.groupStats.isNotEmpty()) {
            GroupPerformanceSection(
                groups = overview.groupStats,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OverviewCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.1f),
                            color.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = value,
                        style = AppTypography.headlineLarge,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    style = AppTypography.titleMedium,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SubjectPerformanceSection(
    subjects: List<SubjectOverview>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = "Subject Performance",
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Subject Performance",
                    style = AppTypography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(subjects) { subject ->
                    SubjectPerformanceCard(subject)
                }
            }
        }
    }
}

@Composable
private fun SubjectPerformanceCard(
    subject: SubjectOverview
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = getAttendanceColor(subject.averageAttendance).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .width(140.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = subject.subject,
                style = AppTypography.titleMedium,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${subject.averageAttendance.toInt()}%",
                style = AppTypography.headlineMedium,
                color = getAttendanceColor(subject.averageAttendance),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${subject.totalClasses} classes",
                style = AppTypography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun GroupPerformanceSection(
    groups: List<GroupOverview>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = "Group Performance",
                    tint = AppColors.Secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Group Performance",
                    style = AppTypography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups) { group ->
                    GroupPerformanceCard(group)
                }
            }
        }
    }
}

@Composable
private fun GroupPerformanceCard(
    group: GroupOverview
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = getAttendanceColor(group.averageAttendance).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .width(120.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = group.group,
                style = AppTypography.titleMedium,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${group.averageAttendance.toInt()}%",
                style = AppTypography.headlineMedium,
                color = getAttendanceColor(group.averageAttendance),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${group.totalClasses} classes",
                style = AppTypography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}