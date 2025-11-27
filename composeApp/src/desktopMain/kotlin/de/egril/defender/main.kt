package de.egril.defender

import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import org.jetbrains.skia.Image

fun main() = application {
    val iconPainter by produceState<BitmapPainter?>(null) {
        val iconBytes = Thread.currentThread().contextClassLoader
            .getResourceAsStream("drawable/black-shield.png")
            ?.readBytes()
        if (iconBytes != null) {
            val image = Image.makeFromEncoded(iconBytes)
            value = BitmapPainter(image.toComposeImageBitmap())
        }
    }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Defender of Egril",
        state = WindowState(placement = WindowPlacement.Maximized),
        icon = iconPainter,
    ) {
        App()
    }
}
