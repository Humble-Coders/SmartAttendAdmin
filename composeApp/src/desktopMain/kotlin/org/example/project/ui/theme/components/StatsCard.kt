package org.example.project.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.ui.theme.AppColors
import org.example.project.ui.theme.AppTypography

@Composable
fun StatsCard(
    title: String,
    percentage: Double,
    attended: Int,
    total: Int,
    modifier: Modifier = Modifier,
    color: Color = AppColors.Primary,
    icon: ImageVector? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    style = AppTypography.titleMedium,
                    color = AppColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${percentage.toInt()}%",
                style = AppTypography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$attended / $total classes",
                style = AppTypography.bodyMedium,
                color = AppColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Enhanced progress bar with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.Divider)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((percentage / 100).toFloat())
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(color, color.copy(alpha = 0.8f))
                            )
                        )
                )
            }
        }
    }
}