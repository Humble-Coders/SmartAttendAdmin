package org.example.project

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import org.example.project.ui.theme.AppColors
import org.example.project.ui.theme.screens.DashboardScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppColors.Primary,
            primaryContainer = AppColors.PrimaryLight,
            secondary = AppColors.Secondary,
            secondaryContainer = AppColors.Accent,
            background = AppColors.Background,
            surface = AppColors.Surface,
            surfaceVariant = AppColors.SurfaceVariant,
            onSurface = AppColors.OnSurface,
            onBackground = AppColors.OnBackground,
            onPrimary = AppColors.Surface,
            onSecondary = AppColors.Surface,
            error = AppColors.Error,
            outline = AppColors.Border,
            outlineVariant = AppColors.Divider
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppColors.Background
        ) {
            Navigator(DashboardScreen())
        }
    }
}