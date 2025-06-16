package org.example.project.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import org.example.project.data.firebase.OptimizedFirebaseService
import org.example.project.data.firebase.repository.HybridAttendanceRepository
import org.example.project.models.DashboardOverview
import org.example.project.models.OptimizedDashboardOverview
import org.example.project.models.SubjectAttendance
import org.example.project.models.SubjectOverview
import org.example.project.ui.theme.AppColors
import org.example.project.ui.theme.AppTypography
import org.example.project.ui.theme.components.DashboardOverviewSection
import org.example.project.ui.theme.components.FilterSection
import org.example.project.utils.Constants
import org.example.project.utils.DateUtils
import org.example.project.viewmodel.OptimizedDashboardUiState
import org.example.project.viewmodel.OptimizedDashboardViewModel
import org.example.project.viewmodel.PaginatedSubjectAttendance
import org.example.project.viewmodel.PerformanceMetrics


class OptimizedDashboardScreen : Screen {
    @Composable
    override fun Content() {
        val legacyService = remember { FirebaseService() }
        val optimizedService = remember { OptimizedFirebaseService() }
        val repository = remember { HybridAttendanceRepository(legacyService, optimizedService) }
        val viewModel = rememberScreenModel { OptimizedDashboardViewModel(repository) }
        val navigator = LocalNavigator.currentOrThrow

        val uiState by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val performanceMetrics by viewModel.performanceMetrics.collectAsState()

        var isInitialized by remember { mutableStateOf(false) }

        // Initialize Firebase services
        LaunchedEffect(Unit) {
            if (!isInitialized) {
                val legacyInit = legacyService.initialize()
                val optimizedInit = optimizedService.initialize()
                isInitialized = legacyInit || optimizedInit

                if (!isInitialized) {
                    println("Smart Attend: Failed to initialize Firebase services")
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (!isInitialized) {
                InitializingScreen()
            } else {
                OptimizedDashboardContent(
                    uiState = uiState,
                    isLoading = isLoading,
                    performanceMetrics = performanceMetrics,
                    onGroupSelected = viewModel::filterByGroup,
                    onSubjectSelected = viewModel::filterBySubject,
                    onMonthChanged = viewModel::changeMonth,
                    onRefresh = viewModel::refreshData,
                    onLoadMoreStudents = viewModel::loadMoreStudents,
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
private fun OptimizedDashboardContent(
    uiState: OptimizedDashboardUiState,
    isLoading: Boolean,
    performanceMetrics: PerformanceMetrics,
    onGroupSelected: (String?) -> Unit,
    onSubjectSelected: (String?) -> Unit,
    onMonthChanged: (Int, Int) -> Unit,
    onRefresh: () -> Unit,
    onLoadMoreStudents: (String) -> Unit,
    onStudentClick: (String) -> Unit,
    onErrorDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp)
    ) {
        // Enhanced Header with Performance Info
        EnhancedHeaderSection(
            year = uiState.year,
            month = uiState.month,
            performanceMetrics = performanceMetrics,
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
                        availableMonths = listOf(6), // You'd get this from metadata
                        onGroupSelected = onGroupSelected,
                        onSubjectSelected = onSubjectSelected,
                        onMonthChanged = onMonthChanged
                    )
                }

                // Optimized Dashboard Overview
                item {
                    OptimizedDashboardOverviewSection(
                        overview = uiState.dashboardOverview,
                        performanceMetrics = performanceMetrics
                    )
                }

                // Subject-wise Attendance (Paginated)
                items(uiState.subjectAttendanceData.entries.toList()) { (subject, paginatedData) ->
                    PaginatedSubjectAttendanceSection(
                        subject = subject,
                        paginatedData = paginatedData,
                        onStudentClick = onStudentClick,
                        onLoadMore = { onLoadMoreStudents(subject) }
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
private fun EnhancedHeaderSection(
    year: Int,
    month: Int,
    performanceMetrics: PerformanceMetrics,
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
            Column {
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
                            text = "High-Performance Dashboard",
                            style = AppTypography.titleMedium,
                            color = AppColors.Surface.copy(alpha = 0.8f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Performance indicator
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = AppColors.Surface.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (performanceMetrics.isOptimizedPath) Icons.Default.Speed else Icons.Default.Warning,
                                    contentDescription = "Performance",
                                    tint = if (performanceMetrics.isOptimizedPath) AppColors.Success else AppColors.Warning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${performanceMetrics.lastLoadTime}ms",
                                    style = AppTypography.bodySmall,
                                    color = AppColors.Surface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Date display
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

                        // Refresh button
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

                // Performance metrics row
                if (performanceMetrics.lastLoadTime > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PerformanceChip(
                            label = "Mode",
                            value = if (performanceMetrics.isOptimizedPath) "Optimized" else "Legacy",
                            color = if (performanceMetrics.isOptimizedPath) AppColors.Success else AppColors.Warning
                        )

                        PerformanceChip(
                            label = "Queries",
                            value = performanceMetrics.totalQueries.toString(),
                            color = AppColors.Info
                        )

                        if (performanceMetrics.cacheHitRate > 0) {
                            PerformanceChip(
                                label = "Cache Hit",
                                value = "${performanceMetrics.cacheHitRate.toInt()}%",
                                color = AppColors.Success
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceChip(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = AppTypography.bodySmall,
                color = AppColors.Surface.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = AppTypography.bodySmall,
                color = AppColors.Surface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OptimizedDashboardOverviewSection(
    overview: OptimizedDashboardOverview,
    performanceMetrics: PerformanceMetrics
) {
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
                Text(
                    text = "Dashboard Overview",
                    style = AppTypography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )

                // Optimization badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (overview.isOptimized) AppColors.Success else AppColors.Warning
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (overview.isOptimized) "⚡ Optimized" else "🐌 Legacy",
                        style = AppTypography.bodySmall,
                        color = AppColors.Surface,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Use existing DashboardOverviewSection but with optimized data
            if (overview.isOptimized) {
                // Show optimized overview
                DashboardOverviewSection(
                    overview = DashboardOverview(
                        totalStudents = overview.metadata.totalStudents,
                        totalClasses = overview.metadata.totalClasses,
                        overallAttendance = overview.metadata.overallAttendanceRate,
                        subjectStats = overview.subjectStats.map { subjectStat ->
                            SubjectOverview(
                                subject = subjectStat.subject,
                                totalClasses = subjectStat.totalClasses,
                                averageAttendance = subjectStat.attendanceRate
                            )
                        },
                        groupStats = overview.groupStats
                    )
                )
            } else {
                // Show legacy overview
                DashboardOverviewSection(
                    overview = DashboardOverview(
                        totalStudents = overview.metadata.totalStudents,
                        totalClasses = overview.metadata.totalClasses,
                        overallAttendance = overview.metadata.overallAttendanceRate,
                        subjectStats = emptyList(),
                        groupStats = overview.groupStats
                    )
                )
            }
        }
    }
}

@Composable
private fun PaginatedSubjectAttendanceSection(
    subject: String,
    paginatedData: PaginatedSubjectAttendance,
    onStudentClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    SubjectAttendanceSection(
        subject = subject,
        subjectData = SubjectAttendance(subject, paginatedData.students),
        onStudentClick = onStudentClick
    )

    // Load more button if there are more students
    if (paginatedData.hasMore) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = onLoadMore,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.Primary
                )
            ) {
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = "Load More",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load More Students")
            }
        }
    }
}