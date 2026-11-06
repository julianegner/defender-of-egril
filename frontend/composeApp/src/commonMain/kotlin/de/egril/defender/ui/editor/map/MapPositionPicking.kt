package de.egril.defender.ui.editor.map

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.Position
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.pick_position_on_map
import defender_of_egril.composeapp.generated.resources.pick_position_stop

internal val REPLACE_HIGHLIGHT_COLOR = Color(0xFFFF4081)
internal val COPY_HIGHLIGHT_COLOR = Color(0xFFFFEB3B)
internal val PASTE_HIGHLIGHT_COLOR = Color(0xFF69F0AE)
internal val RING_START_HIGHLIGHT_COLOR = Color(0xFF40C4FF)

/** Coordinate fields in the map editor that can be filled by clicking a tile on the map. */
enum class PositionPickTarget {
    REPLACE_FROM,
    REPLACE_TO,
    COPY_FROM,
    COPY_TO,
    PASTE_AT,
    RING_START,
    ;

    /** A "from" corner is followed by its "to" corner so both can be picked in sequence. */
    fun nextPickTarget(): PositionPickTarget? =
        when (this) {
            REPLACE_FROM -> REPLACE_TO
            COPY_FROM -> COPY_TO
            else -> null
        }
}

/** Frames a coordinate row in the area's map color while it is the active pick target. */
internal fun Modifier.pickTargetBorder(active: Boolean, color: Color): Modifier =
    if (active) {
        this.border(2.dp, color, RoundedCornerShape(6.dp)).padding(4.dp)
    } else {
        this
    }

/** Button that arms (or disarms) map picking for one coordinate pair. */
@Composable
internal fun PositionPickButton(active: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(
                if (active) Res.string.pick_position_stop else Res.string.pick_position_on_map,
            ),
        )
    }
}

/** Arms [clicked], or disarms it when it is already the active target. */
fun togglePickTarget(current: PositionPickTarget?, clicked: PositionPickTarget): PositionPickTarget? =
    if (current == clicked) null else clicked

fun parsePosition(xText: String, yText: String): Position? {
    val x = xText.toIntOrNull() ?: return null
    val y = yText.toIntOrNull() ?: return null
    return Position(x, y)
}

/** All tiles of the axis-aligned rectangle spanned by two corners, regardless of their order. */
fun rectangleTiles(from: Position?, to: Position?): Set<Position> {
    if (from == null || to == null) return emptySet()
    val minX = minOf(from.x, to.x)
    val maxX = maxOf(from.x, to.x)
    val minY = minOf(from.y, to.y)
    val maxY = maxOf(from.y, to.y)
    return buildSet {
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                add(Position(x, y))
            }
        }
    }
}
