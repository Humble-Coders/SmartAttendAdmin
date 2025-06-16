package org.example.project.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.example.project.data.firebase.FirebaseService
import org.example.project.data.firebase.repository.AttendanceRepository
import org.example.project.ui.theme.AppColors
import org.example.project.ui.theme.AppTypography
import org.example.project.ui.theme.components.DashboardOverviewSection
import org.example.project.ui.theme.components.FilterSection
import org.example.project.utils.Constants
import org.example.project.utils.DateUtils
import org.example.project.viewmodel.DashboardUiState
import org.example.project.viewmodel.DashboardViewModel


class DashboardScreen : Screen {
    @Composable
    override fun Content() {
        val firebaseService = remember { FirebaseService() }
        val repository = remember { AttendanceRepository(firebaseService) }
        val viewModel = rememberScreenModel { DashboardViewModel(repository) }
        val navigator = LocalNavigator.currentOrThrow

        val uiState by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        var isInitialized by remember { mutableStateOf(false) }

        // Initialize Firebase
        LaunchedEffect(Unit) {
            if (!isInitialized) {
                isInitialized = firebaseService.initialize()
                if (!isInitialized) {
                    println("Smart Attend: Failed to initialize Firebase")
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (!isInitialized) {
                InitializingScreen()
            } else {
                DashboardContent(
                    uiState = uiState,
                    isLoading = isLoading,
                    onGroupSelected = viewModel::filterByGroup,
                    onSubjectSelected = viewModel::filterBySubject,
                    onMonthChanged = viewModel::changeMonth,
                    onRefresh = viewModel::refreshData,
                    onStudentClick = { rollNumber ->
                        navigator.push(StudentDetailScreen(rollNumber))
                    },
                    onErrorDismiss = viewModel::clearError
                )
            }
        }
    }
}

@Composable
fun InitializingScreen() {
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
            // App Logo/Icon
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = "Smart Attend",
                    tint = AppColors.Primary,
                    modifier = Modifier
                        .size(80.dp)
                        .padding(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = Constants.APP_NAME,
                style = AppTypography.headlineLarge,
                color = AppColors.Surface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Teacher Dashboard",
                style = AppTypography.titleLarge,
                color = AppColors.Surface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = AppColors.Surface,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Initializing Firebase connection...",
                style = AppTypography.bodyLarge,
                color = AppColors.Surface.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    isLoading: Boolean,
    onGroupSelected: (String?) -> Unit,
    onSubjectSelected: (String?) -> Unit,
    onMonthChanged: (Int, Int) -> Unit,
    onRefresh: () -> Unit,
    onStudentClick: (String) -> Unit,
    onErrorDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp)
    ) {
        // Header Section
        HeaderSection(
            year = uiState.year,
            month = uiState.month,
            onRefresh = onRefresh,
            isLoading = isLoading
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

        // Main Content
        if (isLoading) {
            LoadingContent()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Filter Section
                item {
                    FilterSection(
                        groups = uiState.availableGroups,
                        subjects = uiState.availableSubjects,
                        selectedGroup = uiState.selectedGroup,
                        selectedSubject = uiState.selectedSubject,
                        year = uiState.year,
                        month = uiState.month,
                        availableMonths = uiState.availableMonths,
                        onGroupSelected = onGroupSelected,
                        onSubjectSelected = onSubjectSelected,
                        onMonthChanged = onMonthChanged
                    )
                }

                // Dashboard Overview
                item {
                    DashboardOverviewSection(overview = uiState.dashboardOverview)
                }

                // Subject-wise Attendance
                items(uiState.subjectAttendanceData.entries.toList()) { (subject, subjectData) ->
                    SubjectAttendanceSection(
                        subject = subject,
                        subjectData = subjectData,
                        onStudentClick = onStudentClick
                    )
                }

                // Empty State
                if (uiState.subjectAttendanceData.isEmpty() && !isLoading) {
                    item {
                        EmptyStateCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    year: Int,
    month: Int,
    onRefresh: () -> Unit,
    isLoading: Boolean
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Constants.APP_NAME,
                        style = AppTypography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Surface
                    )
                    Text(
                        text = "Attendance Management Dashboard",
                        style = AppTypography.titleMedium,
                        color = AppColors.Surface.copy(alpha = 0.8f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                Icons.Default.DateRange,
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

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AppColors.Surface
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = AppColors.Surface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
 fun LoadingContent() {
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
                text = "Loading attendance data...",
                style = AppTypography.bodyLarge,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
 fun ErrorCard(
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
 fun EmptyStateCard() {
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
                text = "Try adjusting your filters or check back later",
                style = AppTypography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
    }
}