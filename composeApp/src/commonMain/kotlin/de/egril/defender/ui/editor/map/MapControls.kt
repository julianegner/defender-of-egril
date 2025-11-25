package de.egril.defender.ui.editor.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.egril.defender.ui.ControlPad
import de.egril.defender.ui.ZoomControls

data class MapControlState(
    val zoomLevel: Float,
    val offsetX: Float,
    val offsetY: Float
)

@Composable
fun BoxScope.MapControls(
    mapControlState: MapControlState,
    onStateChange: (MapControlState) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.BottomEnd)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (de.egril.defender.ui.settings.AppSettings.showControlPad.value) {
                // Directional pad
                ControlPad(
                    onUp = {
                        onStateChange(mapControlState.copy(offsetY = mapControlState.offsetY + 30f))
                    },
                    onDown = {
                        onStateChange(mapControlState.copy(offsetY = mapControlState.offsetY - 30f))
                    },
                    onLeft = {
                        onStateChange(mapControlState.copy(offsetX = mapControlState.offsetX + 30f))
                    },
                    onRight = {
                        onStateChange(mapControlState.copy(offsetX = mapControlState.offsetX - 30f))
                    }
                )

                // Zoom controls
                ZoomControls(
                    onZoomIn = {
                        onStateChange(mapControlState.copy(zoomLevel = (mapControlState.zoomLevel + 0.1f).coerceIn(0.5f, 3.0f)))
                    },
                    onZoomOut = {
                        onStateChange(mapControlState.copy(zoomLevel = (mapControlState.zoomLevel - 0.1f).coerceIn(0.5f, 3.0f)))
                    }
                )
            }
            content()
        }
    }
}
