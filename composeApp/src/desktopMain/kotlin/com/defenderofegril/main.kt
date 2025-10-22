package com.defenderofegril

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Defender of Egril",
    ) {
        App()
    }
}
