package org.example.project

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.project.utils.Constants

fun main() = application {
    Window(
        title = "${Constants.APP_NAME} - High-Performance Dashboard v${Constants.APP_VERSION}",
        state = rememberWindowState(
            width = Constants.WINDOW_WIDTH.dp,
            height = Constants.WINDOW_HEIGHT.dp
        ),
        onCloseRequest = ::exitApplication
    ) {
        App()
    }
}