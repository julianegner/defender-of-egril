package de.egril.defender

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.FrameWindowScope
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import de.egril.defender.utils.WindowCloseHandler
import org.jetbrains.skia.Image
import java.awt.Dimension

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
    
    // State to control whether to show unsaved changes dialog
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    
    Window(
        onCloseRequest = {
            // Check for unsaved changes before closing
            if (WindowCloseHandler.hasUnsavedChanges()) {
                showUnsavedChangesDialog = true
            } else {
                exitApplication()
            }
        },
        title = "Defender of Egril",
        state = WindowState(placement = WindowPlacement.Maximized, size = DpSize(1024.dp, 768.dp)),
        icon = iconPainter,
    ) {
        // Set minimum window size to ensure tower buttons remain visible
        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(1024, 768)
        }
        
        App()
        
        // Show unsaved changes dialog when trying to close window
        if (showUnsavedChangesDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showUnsavedChangesDialog = false 
                },
                title = { Text(stringResource(Res.string.unsaved_changes_title)) },
                text = {
                    Text(
                        stringResource(Res.string.unsaved_changes_message),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showUnsavedChangesDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(stringResource(Res.string.cancel))
                        }
                        Button(
                            onClick = {
                                showUnsavedChangesDialog = false
                                exitApplication()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(Res.string.discard_changes))
                        }
                        Button(
                            onClick = {
                                WindowCloseHandler.saveGame()
                                showUnsavedChangesDialog = false
                                exitApplication()
                            }
                        ) {
                            Text(stringResource(Res.string.save_and_exit))
                        }
                    }
                }
            )
        }
    }
}
