package org.example.project

// File: composeApp/src/desktopMain/kotlin/main.kt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.navigator.Navigator
import org.example.project.ui.theme.AppColors
import org.example.project.ui.theme.screens.DashboardScreen
import org.example.project.ui.theme.screens.OptimizedDashboardScreen

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
            Navigator(OptimizedDashboardScreen())
        }
    }
}