package org.example.project.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.example.project.data.firebase.FirebaseService
import org.example.project.data.firebase.repository.AttendanceRepository
import org.example.project.models.AttendanceStats
import org.example.project.ui.theme.AppColors
import org.example.project.ui.theme.AppTypography
import org.example.project.ui.theme.components.StatsCard
import org.example.project.ui.theme.components.getAttendanceColor
import org.example.project.utils.Constants
import org.example.project.utils.DateUtils
import org.example.project.viewmodel.StudentDetailUiState
import org.example.project.viewmodel.StudentDetailViewModel


class StudentDetailScreen(private val rollNumber: String) : Screen {
    @Composable
    override fun Content() {
        val firebaseService = remember { FirebaseService() }
        val repository = remember { AttendanceRepository(firebaseService) }
        val viewModel = rememberScreenModel { StudentDetailViewModel(repository) }
        val navigator = LocalNavigator.currentOrThrow

        val uiState by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        var isInitialized by remember { mutableStateOf(false) }

        // Initialize Firebase and load student data
        LaunchedEffect(rollNumber) {
            if (!isInitialized) {
                isInitialized = firebaseService.initialize()
            }
            if (isInitialized) {
                viewModel.loadStudentData(rollNumber)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (!isInitialized) {
                InitializingScreen()
            } else {
                StudentDetailContent(
                    rollNumber = rollNumber,
                    uiState = uiState,
                    isLoading = isLoading,
                    onBackClick = { navigator.pop() },
                    onMonthChanged = viewModel::changeMonth,
                    onErrorDismiss = viewModel::clearError
                )
            }
        }
    }
}

@Composable
private fun InitializingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AppColors.GradientStart, AppColors.GradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = AppColors.Surface,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Loading student details...",
                style = AppTypography.bodyLarge,
                color = AppColors.Surface.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentDetailContent(
    rollNumber: String,
    uiState: StudentDetailUiState,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onMonthChanged: (Int, Int) -> Unit,
    onErrorDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp)
    ) {
        // Header
        StudentDetailHeader(
            rollNumber = rollNumber,
            year = uiState.year,
            month = uiState.month,
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error handling
        uiState.error?.let { error ->
            ErrorCard(
                error = error,
                onDismiss = onErrorDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        // Content
        if (isLoading) {
            LoadingContent()
        } else if (uiState.subjectWiseAttendance.isEmpty()) {
            EmptyStateCard(rollNumber, uiState.year, uiState.month)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Overall summary
                item {
                    OverallSummaryCard(uiState.subjectWiseAttendance)
                }

                // Subject-wise details
                items(uiState.subjectWiseAttendance.entries.toList()) { (subject, stats) ->
                    SubjectDetailCard(subject, stats)
                }
            }
        }
    }
}

@Composable
private fun StudentDetailHeader(
    rollNumber: String,
    year: Int,
    month: Int,
    onBackClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(AppColors.GradientStart, AppColors.GradientEnd)
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = AppColors.Surface.copy(alpha = 0.2f)
                    )
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.Surface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Student Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AppColors.Surface.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = AppColors.Surface,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Student Details",
                        style = AppTypography.titleLarge,
                        color = AppColors.Surface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = rollNumber,
                        style = AppTypography.headlineMedium,
                        color = AppColors.Surface,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.Surface.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = AppColors.Surface,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = DateUtils.getDateRange(year, month),
                            style = AppTypography.titleMedium,
                            color = AppColors.Surface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallSummaryCard(subjectWiseAttendance: Map<String, AttendanceStats>) {
    val totalOverall = subjectWiseAttendance.values.sumOf { it.totalClasses }
    val attendedOverall = subjectWiseAttendance.values.sumOf { it.attendedClasses }
    val overallPercentage = if (totalOverall > 0) (attendedOverall.toDouble() / totalOverall) * 100 else 0.0

    val totalLectures = subjectWiseAttendance.values.sumOf { it.lectureStats.total }
    val attendedLectures = subjectWiseAttendance.values.sumOf { it.lectureStats.attended }
    val lecturePercentage = if (totalLectures > 0) (attendedLectures.toDouble() / totalLectures) * 100 else 0.0

    val totalTutorials = subjectWiseAttendance.values.sumOf { it.tutorialStats.total }
    val attendedTutorials = subjectWiseAttendance.values.sumOf { it.tutorialStats.attended }
    val tutorialPercentage = if (totalTutorials > 0) (attendedTutorials.toDouble() / totalTutorials) * 100 else 0.0

    val totalLabs = subjectWiseAttendance.values.sumOf { it.labStats.total }
    val attendedLabs = subjectWiseAttendance.values.sumOf { it.labStats.attended }
    val labPercentage = if (totalLabs > 0) (attendedLabs.toDouble() / totalLabs) * 100 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = "Overall Summary",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Overall Summary",
                        style = AppTypography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = getAttendanceColor(overallPercentage)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = Constants.getAttendanceGrade(overallPercentage),
                        style = AppTypography.titleMedium,
                        color = AppColors.Surface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatsCard(
                    title = "Overall",
                    percentage = overallPercentage,
                    attended = attendedOverall,
                    total = totalOverall,
                    modifier = Modifier.weight(1f),
                    color = getAttendanceColor(overallPercentage),
                    icon = Icons.Default.School
                )

                if (totalLectures > 0) {
                    StatsCard(
                        title = "Lectures",
                        percentage = lecturePercentage,
                        attended = attendedLectures,
                        total = totalLectures,
                        modifier = Modifier.weight(1f),
                        color = getAttendanceColor(lecturePercentage),
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
                        color = getAttendanceColor(tutorialPercentage),
                        icon = Icons.Default.Quiz
                    )
                }

                if (totalLabs > 0) {
                    StatsCard(
                        title = "Labs",
                        percentage = labPercentage,
                        attended = attendedLabs,
                        total = totalLabs,
                        modifier = Modifier.weight(1f),
                        color = getAttendanceColor(labPercentage),
                        icon = Icons.Default.Science
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectDetailCard(subject: String, stats: AttendanceStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = "Subject",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subject,
                        style = AppTypography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = getAttendanceColor(stats.percentage).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${stats.percentage.toInt()}%",
                        style = AppTypography.titleMedium,
                        color = getAttendanceColor(stats.percentage),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    title = "Overall",
                    percentage = stats.percentage,
                    attended = stats.attendedClasses,
                    total = stats.totalClasses,
                    modifier = Modifier.weight(1f),
                    color = getAttendanceColor(stats.percentage)
                )

                if (stats.lectureStats.total > 0) {
                    StatsCard(
                        title = "Lectures",
                        percentage = stats.lectureStats.percentage,
                        attended = stats.lectureStats.attended,
                        total = stats.lectureStats.total,
                        modifier = Modifier.weight(1f),
                        color = getAttendanceColor(stats.lectureStats.percentage)
                    )
                }

                if (stats.tutorialStats.total > 0) {
                    StatsCard(
                        title = "Tutorials",
                        percentage = stats.tutorialStats.percentage,
                        attended = stats.tutorialStats.attended,
                        total = stats.tutorialStats.total,
                        modifier = Modifier.weight(1f),
                        color = getAttendanceColor(stats.tutorialStats.percentage)
                    )
                }

                if (stats.labStats.total > 0) {
                    StatsCard(
                        title = "Labs",
                        percentage = stats.labStats.percentage,
                        attended = stats.labStats.attended,
                        total = stats.labStats.total,
                        modifier = Modifier.weight(1f),
                        color = getAttendanceColor(stats.labStats.percentage)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = AppColors.Primary,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading student data...",
                style = AppTypography.bodyLarge,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppColors.Error.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    tint = AppColors.Error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = error,
                    style = AppTypography.bodyMedium,
                    color = AppColors.Error
                )
            }

            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = AppColors.Error)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(rollNumber: String, year: Int, month: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = "No Data",
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No attendance records found",
                style = AppTypography.titleLarge,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "No records found for $rollNumber in ${DateUtils.getDateRange(year, month)}",
                style = AppTypography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
    }
}

