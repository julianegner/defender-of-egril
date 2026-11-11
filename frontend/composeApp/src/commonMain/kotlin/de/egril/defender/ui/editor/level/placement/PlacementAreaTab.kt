package de.egril.defender.ui.editor.level.placement

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.placement_area_add_buildable
import defender_of_egril.composeapp.generated.resources.placement_area_clear
import defender_of_egril.composeapp.generated.resources.placement_area_hint
import defender_of_egril.composeapp.generated.resources.placement_area_mode_erase
import defender_of_egril.composeapp.generated.resources.placement_area_mode_rectangle
import defender_of_egril.composeapp.generated.resources.placement_area_no_map
import defender_of_egril.composeapp.generated.resources.placement_area_tooltips_toggle
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.InitialData
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.ui.editor.level.initialsetup.HexGeometry
import de.egril.defender.ui.editor.level.initialsetup.calculateHexGeometry
import de.egril.defender.ui.editor.level.initialsetup.drawHexagon
import de.egril.defender.ui.editor.level.initialsetup.drawInitialSetupElements
import de.egril.defender.ui.editor.level.initialsetup.isWaterTile
import de.egril.defender.ui.editor.map.rectangleTiles
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.floor

/** Only NO_PLAY tiles are excluded; towers, traps and barricades all sit on playable tiles. */
private fun isMarkable(map: EditorMap, position: Position): Boolean =
    map.getTileType(position.x, position.y) != TileType.NO_PLAY

/** All BUILD_AREA tiles of the map, where towers can be placed. */
private fun buildableTiles(map: EditorMap): List<Position> =
    buildList {
        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                if (map.getTileType(x, y) == TileType.BUILD_AREA) add(Position(x, y))
            }
        }
    }

private fun rowOffsetX(geometry: HexGeometry, row: Int): Float =
    if (row % 2 == 1) geometry.hexWidth / 2 else 0f

private fun hexCenter(geometry: HexGeometry, col: Int, row: Int): Offset =
    Offset(
        geometry.offsetXCanvas + col * geometry.hexWidth + rowOffsetX(geometry, row) + geometry.hexWidth / 2,
        geometry.offsetYCanvas + row * geometry.verticalSpacing + geometry.hexHeight / 2,
    )

/** Nearest hex to a canvas point, searched among the neighbouring rows and columns. */
private fun hexAt(geometry: HexGeometry, map: EditorMap, point: Offset): Position? {
    val approxRow = floor((point.y - geometry.offsetYCanvas) / geometry.verticalSpacing).toInt()
    var best: Position? = null
    var bestDistance = Float.MAX_VALUE
    for (row in (approxRow - 1)..(approxRow + 1)) {
        if (row !in 0 until map.height) continue
        val approxCol = floor((point.x - geometry.offsetXCanvas - rowOffsetX(geometry, row)) / geometry.hexWidth).toInt()
        for (col in (approxCol - 1)..(approxCol + 1)) {
            if (col !in 0 until map.width) continue
            val center = hexCenter(geometry, col, row)
            val dx = point.x - center.x
            val dy = point.y - center.y
            val distance = dx * dx + dy * dy
            if (distance < bestDistance) {
                bestDistance = distance
                best = Position(col, row)
            }
        }
    }
    return best
}

/** Hexes touched by a straight drag segment, so fast drags do not skip tiles. */
private fun hexesAlongSegment(geometry: HexGeometry, map: EditorMap, from: Offset, to: Offset): Set<Position> {
    val length = (to - from).getDistance()
    val steps = maxOf(1, (length / (geometry.hexWidth / 2f)).toInt())
    return buildSet {
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            hexAt(geometry, map, from + (to - from) * t)?.let { add(it) }
        }
    }
}

/**
 * Editor tab for the level's placement area. Drag paints tiles, Shift+drag or the
 * erase toggle removes them, and the rectangle toggle marks a rectangle between press and release.
 * An empty area means towers, traps and barricades may be placed anywhere.
 */
@Composable
fun PlacementAreaTab(
    initialData: InitialData,
    onInitialDataChange: (InitialData) -> Unit,
    map: EditorMap?,
) {
    if (map == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.placement_area_no_map))
        }
        return
    }

    var eraseMode by remember { mutableStateOf(false) }
    var rectangleMode by remember { mutableStateOf(false) }
    var previewTiles by remember { mutableStateOf<Set<Position>>(emptySet()) }
    // Not persisted: the tooltip toggle resets to ON whenever the tab is opened.
    var showTooltips by remember { mutableStateOf(true) }
    var hoveredHex by remember { mutableStateOf<Position?>(null) }
    var hoverPoint by remember { mutableStateOf<Offset?>(null) }
    var tooltipHeight by remember { mutableStateOf(0) }

    val areaSet = initialData.placementAreas.toSet()
    val isDarkMode = AppSettings.isDarkMode.value

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(Res.string.placement_area_hint), style = MaterialTheme.typography.bodySmall)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleButton(
                label = stringResource(Res.string.placement_area_mode_erase),
                selected = eraseMode,
                onClick = { eraseMode = !eraseMode },
            )
            ToggleButton(
                label = stringResource(Res.string.placement_area_mode_rectangle),
                selected = rectangleMode,
                onClick = { rectangleMode = !rectangleMode },
            )
            OutlinedButton(
                onClick = { onInitialDataChange(initialData.copy(placementAreas = emptyList())) },
                enabled = areaSet.isNotEmpty(),
            ) {
                Text(stringResource(Res.string.placement_area_clear))
            }
            val buildable = map?.let { buildableTiles(it) }.orEmpty()
            Button(
                onClick = {
                    onInitialDataChange(initialData.copy(placementAreas = (areaSet + buildable).toList()))
                },
                enabled = buildable.any { it !in areaSet },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Text(stringResource(Res.string.placement_area_add_buildable))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.placement_area_tooltips_toggle), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = showTooltips, onCheckedChange = { showTooltips = it })
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(map) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.first()
                                when (event.type) {
                                    PointerEventType.Move, PointerEventType.Enter -> {
                                        hoverPoint = change.position
                                        hoveredHex = hexAt(
                                            calculateHexGeometry(map.width, map.height, size.width.toFloat(), size.height.toFloat()),
                                            map,
                                            change.position,
                                        )
                                    }
                                    PointerEventType.Exit -> {
                                        hoverPoint = null
                                        hoveredHex = null
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(map, initialData, eraseMode, rectangleMode) {
                        awaitEachGesture {
                            // Hover moves arrive without a button press; only a primary-button down starts a gesture.
                            var firstEvent: PointerEvent
                            do {
                                firstEvent = awaitPointerEvent()
                            } while (!(firstEvent.buttons.isPrimaryPressed && firstEvent.changes.first().changedToDown()))
                            val down = firstEvent.changes.first()
                            val geometry = calculateHexGeometry(map.width, map.height, size.width.toFloat(), size.height.toFloat())
                            val erase = eraseMode || firstEvent.keyboardModifiers.isShiftPressed
                            val startHex = hexAt(geometry, map, down.position)
                            var lastPoint = down.position
                            val gestureTiles = linkedSetOf<Position>()
                            startHex?.let { gestureTiles.add(it) }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                change.consume()
                                if (rectangleMode) {
                                    val endHex = hexAt(geometry, map, change.position)
                                    previewTiles = rectangleTiles(startHex, endHex).filter { isMarkable(map, it) }.toSet()
                                } else {
                                    gestureTiles.addAll(hexesAlongSegment(geometry, map, lastPoint, change.position))
                                    previewTiles = gestureTiles.toSet()
                                }
                                lastPoint = change.position
                            }

                            val finalTiles = if (rectangleMode) {
                                rectangleTiles(startHex, hexAt(geometry, map, lastPoint))
                            } else {
                                gestureTiles
                            }
                            val valid = finalTiles.filter { isMarkable(map, it) }.toSet()
                            if (valid.isNotEmpty()) {
                                val updated = if (erase) areaSet - valid else areaSet + valid
                                onInitialDataChange(initialData.copy(placementAreas = updated.toList()))
                            }
                            previewTiles = emptySet()
                        }
                    },
            ) {
                val geometry = calculateHexGeometry(map.width, map.height, size.width, size.height)
                for (row in 0 until map.height) {
                    for (col in 0 until map.width) {
                        val pos = Position(col, row)
                        val center = hexCenter(geometry, col, row)
                        val base = when (map.getTileType(col, row)) {
                            TileType.NO_PLAY -> Color(0xFF2B2B2B)
                            TileType.BUILD_AREA -> if (isDarkMode) Color(0xFF2E5C1A) else Color(0xFF90EE90)
                            else -> when {
                                isWaterTile(pos, map) -> if (isDarkMode) Color(0xFF0D47A1) else Color(0xFF42A5F5)
                                map.getTileType(col, row) == TileType.SPAWN_POINT ->
                                    if (isDarkMode) Color(0xFF8B0000) else Color(0xFFDC143C)
                                map.getTileType(col, row) == TileType.TARGET ->
                                    if (isDarkMode) Color(0xFF1E3A8A) else Color(0xFF4169E1)
                                map.getTileType(col, row) == TileType.PATH ->
                                    if (isDarkMode) Color(0xFF3E3528) else Color(0xFF8B4513)
                                else -> if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFF808080)
                            }
                        }
                        drawHexagon(center.x, center.y, geometry.hexSize, base)
                        if (pos in areaSet) {
                            drawHexagon(center.x, center.y, geometry.hexSize, Color(0x66FFD700))
                        }
                        if (pos in previewTiles) {
                            drawHexagon(center.x, center.y, geometry.hexSize, Color(0x6600FFFF))
                        }
                    }
                }
                drawInitialSetupElements(geometry, initialData, null, null)
            }

            val hex = hoveredHex
            val point = hoverPoint
            if (showTooltips && hex != null && point != null) {
                val tooltipText = placementTooltipText(initialData, map, hex)
                if (tooltipText != null) {
                    Text(
                        text = tooltipText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier
                            .onSizeChanged { tooltipHeight = it.height }
                            .offset { IntOffset(point.x.toInt() + 16, point.y.toInt() - tooltipHeight - 16) }
                            .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(4.dp))
                            .padding(6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}
