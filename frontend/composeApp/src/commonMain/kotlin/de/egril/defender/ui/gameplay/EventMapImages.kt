package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import de.egril.defender.config.GameLogBuffer
import de.egril.defender.model.EventMapImage
import de.egril.defender.ui.MapImageProvider
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

internal data class EventMapImageBounds(
    val offset: Offset,
    val size: Size,
)

/** Uses the grid's row spacing, including its per-row visual correction. */
internal fun eventMapImageBounds(
    image: EventMapImage,
    hexSize: Float,
): EventMapImageBounds {
    val tileWidth = hexSize * sqrt(3f)
    val tileHeight = hexSize * 2f
    return EventMapImageBounds(
        offset =
            Offset(
                image.x * (tileWidth + HexagonalGridConstants.HORIZONTAL_SPACING),
                image.y * (tileHeight * 0.75f + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT - 1f) + 1f,
            ),
        size = Size(image.width * tileWidth, image.height * tileHeight),
    )
}

/**
 * Decorative, non-interactive map images. The caller places this layer after terrain/zone
 * backgrounds and before gameplay content, inside the map's shared pan/zoom transform.
 */
@Composable
internal fun EventMapImages(
    images: List<EventMapImage>,
    hexSize: Float,
    modifier: Modifier = Modifier,
) {
    val fileNames = images.map { it.fileName }.distinct()
    var painters by remember(fileNames) { mutableStateOf<Map<String, Painter>>(emptyMap()) }
    LaunchedEffect(fileNames) {
        painters =
            withContext(Dispatchers.Default) {
                buildMap {
                    for (fileName in fileNames) {
                        val bytes = MapImageProvider.loadEventMapImageBytes(fileName)
                        val bitmap = bytes?.let { MapImageProvider.decodeImageBitmap(it) }
                        if (bitmap == null) {
                            GameLogBuffer.log("EVENT", "Could not load map image '$fileName'")
                        } else {
                            put(fileName, BitmapPainter(bitmap))
                        }
                    }
                }
            }
    }
    Canvas(modifier) {
        for (image in images) {
            val painter = painters[image.fileName] ?: continue
            val bounds = eventMapImageBounds(image, hexSize)
            translate(bounds.offset.x * density, bounds.offset.y * density) {
                with(painter) {
                    draw(Size(bounds.size.width * density, bounds.size.height * density))
                }
            }
        }
    }
}
