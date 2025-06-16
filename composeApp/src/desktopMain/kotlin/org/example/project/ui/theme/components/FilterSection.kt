package org.example.project.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import org.example.project.ui.theme.AppColors
import org.example.project.ui.theme.AppTypography
import org.example.project.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    groups: List<String>,
    subjects: List<String>,
    selectedGroup: String?,
    selectedSubject: String?,
    year: Int,
    month: Int,
    availableMonths: List<Int>,
    onGroupSelected: (String?) -> Unit,
    onSubjectSelected: (String?) -> Unit,
    onMonthChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(AppColors.GradientStart, AppColors.GradientEnd)
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = AppColors.Surface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Filters & Controls",
                        style = AppTypography.titleLarge,
                        color = AppColors.Surface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Date Picker
            DatePickerSection(
                year = year,
                month = month,
                availableMonths = availableMonths,
                onMonthChanged = onMonthChanged
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Group Filter
            FilterChipSection(
                title = "Groups",
                items = groups,
                selectedItem = selectedGroup,
                onItemSelected = onGroupSelected,
                icon = Icons.Default.Group
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subject Filter
            FilterChipSection(
                title = "Subjects",
                items = subjects,
                selectedItem = selectedSubject,
                onItemSelected = onSubjectSelected,
                icon = Icons.Default.Book
            )
        }
    }
}

@Composable
private fun DatePickerSection(
    year: Int,
    month: Int,
    availableMonths: List<Int>,
    onMonthChanged: (Int, Int) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = "Date",
                tint = AppColors.Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Academic Period",
                style = AppTypography.titleMedium,
                color = AppColors.TextPrimary
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableMonths) { monthNum ->
                val isSelected = monthNum == month
                FilterChip(
                    onClick = { onMonthChanged(year, monthNum) },
                    label = {
                        Text(
                            DateUtils.getShortMonthName(monthNum),
                            color = if (isSelected) AppColors.Surface else AppColors.Primary
                        )
                    },
                    selected = isSelected,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.Primary,
                        selectedLabelColor = AppColors.Surface,
                        containerColor = AppColors.CardPrimary,
                        labelColor = AppColors.Primary
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipSection(
    title: String,
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String?) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = AppColors.Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = AppTypography.titleMedium,
                color = AppColors.TextPrimary
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val isSelected = selectedItem == null
                FilterChip(
                    onClick = { onItemSelected(null) },
                    label = {
                        Text(
                            "All",
                            color = if (isSelected) AppColors.Surface else AppColors.Primary
                        )
                    },
                    selected = isSelected,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.Primary,
                        selectedLabelColor = AppColors.Surface,
                        containerColor = AppColors.CardPrimary,
                        labelColor = AppColors.Primary
                    )
                )
            }

            items(items) { item ->
                val isSelected = selectedItem == item
                FilterChip(
                    onClick = { onItemSelected(item) },
                    label = {
                        Text(
                            item,
                            color = if (isSelected) AppColors.Surface else AppColors.Primary
                        )
                    },
                    selected = isSelected,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.Primary,
                        selectedLabelColor = AppColors.Surface,
                        containerColor = AppColors.CardPrimary,
                        labelColor = AppColors.Primary
                    )
                )
            }
        }
    }
}