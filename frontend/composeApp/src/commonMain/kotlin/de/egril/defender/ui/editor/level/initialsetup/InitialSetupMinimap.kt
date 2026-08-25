package de.egril.defender.ui.editor.level.initialsetup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.InitialData
import de.egril.defender.editor.TileType
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.FiefType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.SpawnPointType
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val MINIMAP_HEX_RADIUS = 2.0f

/**
 * Placement mode for initial setup elements
 */
enum class PlacementMode {
    DEFENDER, // Towers: BUILD_AREA
    ATTACKER, // Enemies: PATH or SPAWN_POINT
    TRAP, // Traps: PATH
    BARRICADE, // Barricades: PATH
    FIEF, // Fiefs: PATH
    MUSHROOM, // Mushrooms: PATH
}

/**
 * Checks if a position is valid for the given placement mode
 */
fun isValidPlacement(
    position: Position,
    mode: PlacementMode,
    map: EditorMap,
    selectedDefenderType: DefenderType? = null,
    selectedAttackerType: AttackerType? = null,
): Boolean {
    val tileType = map.getTileType(position.x, position.y)
    return when (mode) {
        PlacementMode.DEFENDER -> {
            val isBuildArea = tileType == TileType.BUILD_AREA
            val isFlowingWaterTile = isFlowingWaterTile(position, map)
            if (selectedDefenderType == DefenderType.DWARVEN_MINE) {
                isBuildArea
            } else {
                isBuildArea || isFlowingWaterTile
            }
        }
        PlacementMode.ATTACKER -> {
            val isRegularEnemyTile = tileType == TileType.PATH || tileType == TileType.SPAWN_POINT
            val attackerType = selectedAttackerType ?: return isRegularEnemyTile
            val canUseWater =
                attackerType.canTraverseRiver ||
                    attackerType.canOnlyMoveOnWater ||
                    attackerType.canSpawnOnWater
            if (canUseWater) {
                isRegularEnemyTile || isWaterTile(position, map)
            } else {
                isRegularEnemyTile
            }
        }
        PlacementMode.TRAP -> tileType == TileType.PATH
        PlacementMode.BARRICADE -> tileType == TileType.PATH
        PlacementMode.FIEF -> tileType == TileType.PATH
        PlacementMode.MUSHROOM -> tileType == TileType.PATH
    }
}

private fun isWaterTile(
    position: Position,
    map: EditorMap,
): Boolean {
    val tileType = map.getTileType(position.x, position.y)
    val isWaterSpawn = tileType == TileType.SPAWN_POINT && map.getSpawnPointType(position) == SpawnPointType.WATER
    return tileType == TileType.RIVER || map.getRiverTile(position.x, position.y) != null || isWaterSpawn
}

private fun isFlowingWaterTile(
    position: Position,
    map: EditorMap,
): Boolean {
    val riverTile = map.getRiverTile(position.x, position.y) ?: return false
    return riverTile.flowDirection != RiverFlow.NONE && riverTile.flowDirection != RiverFlow.MAELSTROM
}

private fun hasAdjacentWaterTile(
    position: Position,
    map: EditorMap,
): Boolean =
    position
        .getHexNeighbors()
        .any { neighbor ->
            if (neighbor.x !in 0 until map.width || neighbor.y !in 0 until map.height) {
                return@any false
            }
            val neighborTileType = map.getTileType(neighbor.x, neighbor.y)
            neighborTileType == TileType.RIVER ||
                (neighborTileType == TileType.SPAWN_POINT && map.getSpawnPointType(neighbor) == SpawnPointType.WATER)
        }

/**
 * Data class to hold hexagon geometry calculations
 */
private data class HexGeometry(
    val hexSize: Float,
    val hexWidth: Float,
    val hexHeight: Float,
    val verticalSpacing: Float,
    val offsetXCanvas: Float,
    val offsetYCanvas: Float,
)

/**
 * Calculate hexagon geometry for the minimap
 */
private fun calculateHexGeometry(
    mapWidth: Int,
    mapHeight: Int,
    canvasWidth: Float,
    canvasHeight: Float,
): HexGeometry {
    val baseHexSize = MINIMAP_HEX_RADIUS
    val baseHexWidth = (sqrt(3.0) * baseHexSize).toFloat()
    val baseHexHeight = 2.0f * baseHexSize
    val baseVerticalSpacing = baseHexHeight * 0.75f
    val totalMapWidth = (mapWidth) * baseHexWidth + baseHexWidth / 2
    val totalMapHeight = (mapHeight - 1) * baseVerticalSpacing + baseHexHeight
    val padding = 4f
    val scaleX = (canvasWidth - padding * 2) / totalMapWidth
    val scaleY = (canvasHeight - padding * 2) / totalMapHeight
    val mapScale = minOf(scaleX, scaleY)
    val hexSize = baseHexSize * mapScale
    val hexWidth = baseHexWidth * mapScale
    val hexHeight = baseHexHeight * mapScale
    val verticalSpacing = baseVerticalSpacing * mapScale
    val scaledMapWidth = totalMapWidth * mapScale
    val scaledMapHeight = totalMapHeight * mapScale
    val offsetXCanvas = (canvasWidth - scaledMapWidth) / 2
    val offsetYCanvas = (canvasHeight - scaledMapHeight) / 2

    return HexGeometry(hexSize, hexWidth, hexHeight, verticalSpacing, offsetXCanvas, offsetYCanvas)
}

/**
 * Minimap for selecting positions in initial setup
 * Enhanced to show existing placements and support removal
 */
@Composable
fun InitialSetupMinimap(
    map: EditorMap,
    placementMode: PlacementMode?,
    selectedDefenderType: DefenderType? = null,
    selectedAttackerType: AttackerType? = null,
    selectedFiefType: FiefType? = null,
    initialData: InitialData = InitialData.EMPTY,
    selectedElement: de.egril.defender.ui.editor.level.initialsetup.SelectedElement? = null,
    onTileClick: (Position) -> Unit = {},
) {
    val isDarkMode = AppSettings.isDarkMode.value
    var hoveredPosition by remember { mutableStateOf<Position?>(null) }

    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(placementMode, initialData) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val offset = event.changes.firstOrNull()?.position ?: continue

                            when (event.type) {
                                PointerEventType.Move, PointerEventType.Enter -> {
                                    val geometry = calculateHexGeometry(map.width, map.height, size.width.toFloat(), size.height.toFloat())
                                    val hitRadius = geometry.hexHeight / 2
                                    val hitRadiusSquared = hitRadius * hitRadius

                                    var newHoveredPosition: Position? = null
                                    var minDistanceSquared = Float.MAX_VALUE

                                    for (row in 0 until map.height) {
                                        for (col in 0 until map.width) {
                                            val pos = Position(col, row)
                                            val offsetXHex = if (row % 2 == 1) geometry.hexWidth / 2 else 0.0f
                                            val centerX =
                                                geometry.offsetXCanvas + col * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
                                            val centerY = geometry.offsetYCanvas + row * geometry.verticalSpacing + geometry.hexHeight / 2
                                            val dx = offset.x - centerX
                                            val dy = offset.y - centerY
                                            val distanceSquared = dx * dx + dy * dy
                                            if (distanceSquared < hitRadiusSquared && distanceSquared < minDistanceSquared) {
                                                minDistanceSquared = distanceSquared
                                                newHoveredPosition = pos
                                            }
                                        }
                                    }
                                    hoveredPosition = newHoveredPosition
                                }
                                PointerEventType.Exit -> {
                                    hoveredPosition = null
                                }
                            }
                        }
                    }
                }.pointerInput(placementMode, initialData) {
                    detectTapGestures { offset ->
                        val geometry = calculateHexGeometry(map.width, map.height, size.width.toFloat(), size.height.toFloat())
                        val hitRadius = geometry.hexHeight / 2
                        val hitRadiusSquared = hitRadius * hitRadius

                        var clickedPosition: Position? = null
                        var minDistanceSquared = Float.MAX_VALUE

                        for (row in 0 until map.height) {
                            for (col in 0 until map.width) {
                                val pos = Position(col, row)
                                val offsetXHex = if (row % 2 == 1) geometry.hexWidth / 2 else 0.0f
                                val centerX = geometry.offsetXCanvas + col * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
                                val centerY = geometry.offsetYCanvas + row * geometry.verticalSpacing + geometry.hexHeight / 2

                                val dx = offset.x - centerX
                                val dy = offset.y - centerY
                                val distanceSquared = dx * dx + dy * dy

                                if (distanceSquared < hitRadiusSquared && distanceSquared < minDistanceSquared) {
                                    minDistanceSquared = distanceSquared
                                    clickedPosition = pos
                                }
                            }
                        }

                        clickedPosition?.let { onTileClick(it) }
                    }
                },
    ) {
        val geometry = calculateHexGeometry(map.width, map.height, size.width, size.height)

        // Draw tiles
        for (row in 0 until map.height) {
            for (col in 0 until map.width) {
                val tileType = map.tiles.getOrElse("$col,$row") { TileType.NO_PLAY }
                val pos = Position(col, row)

                val offsetXHex = if (row % 2 == 1) geometry.hexWidth / 2 else 0.0f
                val centerX = geometry.offsetXCanvas + col * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
                val centerY = geometry.offsetYCanvas + row * geometry.verticalSpacing + geometry.hexHeight / 2

                val hasDefender = initialData.defenders.any { it.position == pos }
                val hasAttacker = initialData.attackers.any { it.position == pos }
                val hasTrap = initialData.traps.any { it.position == pos }
                val hasBarricade = initialData.barricades.any { it.position == pos }
                val hasFief = initialData.fiefs.any { it.position == pos }
                val hasMushroom = initialData.mushrooms.any { it.position == pos }
                val barricadeAtPos = initialData.barricades.find { it.position == pos }
                val isTowerBase = barricadeAtPos?.canSupportTower() == true

                val isValidForPlacement =
                    when (placementMode) {
                        PlacementMode.DEFENDER ->
                            isValidPlacement(
                                pos,
                                placementMode,
                                map,
                                selectedDefenderType = selectedDefenderType,
                            ) ||
                                (isTowerBase && !hasDefender)
                        PlacementMode.FIEF -> {
                            val isPath = isValidPlacement(pos, placementMode, map)
                            val isFisher = selectedFiefType == FiefType.FISHER
                            if (isPath && isFisher) {
                                hasAdjacentWaterTile(pos, map)
                            } else {
                                isPath
                            }
                        }
                        else ->
                            placementMode?.let {
                                isValidPlacement(
                                    pos,
                                    it,
                                    map,
                                    selectedDefenderType = selectedDefenderType,
                                    selectedAttackerType = selectedAttackerType,
                                )
                            } ?: false
                    }
                val isHovered = pos == hoveredPosition
                val isSelected =
                    when (selectedElement) {
                        is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Defender ->
                            selectedElement.defender.position ==
                                pos
                        is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Attacker ->
                            selectedElement.attacker.position ==
                                pos
                        is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Trap -> selectedElement.trap.position == pos
                        is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Barricade ->
                            selectedElement.barricade.position ==
                                pos
                        is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Fief ->
                            selectedElement.fief.position == pos
                        is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Mushroom ->
                            selectedElement.mushroom.position == pos
                        null -> false
                    }

                // Validation checks for placement conflicts
                // Rule: Only one element (tower, trap, barricade, OR unit) is possible on a tile,
                //       except towers can be placed on top of barricades that support towers (HP >= 100)
                val hasAnyElement = hasDefender || hasAttacker || hasTrap || hasBarricade || hasFief || hasMushroom
                val hasConflict =
                    when (placementMode) {
                        PlacementMode.DEFENDER ->
                            // Allow tower on tower base as long as no tower is already there
                            if (isTowerBase && !hasDefender) false else hasAnyElement
                        PlacementMode.ATTACKER, PlacementMode.TRAP, PlacementMode.BARRICADE,
                        PlacementMode.FIEF, PlacementMode.MUSHROOM,
                        -> hasAnyElement
                        else -> false
                    }

                val color =
                    when {
                        isSelected -> Color(0xFFFFD700) // Gold for selected element
                        hasConflict && isHovered && placementMode != null -> Color(0xFFFF4444) // Red for invalid placement
                        isHovered && isValidForPlacement -> Color(0xFF00FFFF) // Cyan for valid hover
                        tileType == TileType.BUILD_AREA -> if (isDarkMode) Color(0xFF2E5C1A) else Color(0xFF90EE90) // Always show BUILD_AREA in green (same as tower placement)
                        isWaterTile(pos, map) -> if (isDarkMode) Color(0xFF0D47A1) else Color(0xFF42A5F5)
                        tileType == TileType.SPAWN_POINT -> if (isDarkMode) Color(0xFF8B0000) else Color(0xFFDC143C)
                        tileType == TileType.TARGET -> if (isDarkMode) Color(0xFF1E3A8A) else Color(0xFF4169E1)
                        tileType == TileType.PATH -> if (isDarkMode) Color(0xFF3E3528) else Color(0xFF8B4513)
                        else -> if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFF808080)
                    }

                drawHexagon(centerX, centerY, geometry.hexSize, color)
            }
        }

        // Draw existing placements on top
        val iconSize = geometry.hexSize * 1.2f

        // Draw defenders (towers)
        initialData.defenders.forEach { defender ->
            val offsetXHex = if (defender.position.y % 2 == 1) geometry.hexWidth / 2 else 0.0f
            val centerX = geometry.offsetXCanvas + defender.position.x * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
            val centerY = geometry.offsetYCanvas + defender.position.y * geometry.verticalSpacing + geometry.hexHeight / 2

            // Draw blue circle for tower
            drawCircle(
                color = Color(0xFF2196F3),
                radius = iconSize / 2,
                center = Offset(centerX, centerY),
            )
        }

        // Draw attackers (enemies)
        initialData.attackers.forEach { attacker ->
            val offsetXHex = if (attacker.position.y % 2 == 1) geometry.hexWidth / 2 else 0.0f
            val centerX = geometry.offsetXCanvas + attacker.position.x * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
            val centerY = geometry.offsetYCanvas + attacker.position.y * geometry.verticalSpacing + geometry.hexHeight / 2

            // Draw red circle for enemy
            drawCircle(
                color = Color(0xFFFF0000),
                radius = iconSize / 2,
                center = Offset(centerX, centerY),
            )
        }

        // Draw traps
        initialData.traps.forEach { trap ->
            val offsetXHex = if (trap.position.y % 2 == 1) geometry.hexWidth / 2 else 0.0f
            val centerX = geometry.offsetXCanvas + trap.position.x * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
            val centerY = geometry.offsetYCanvas + trap.position.y * geometry.verticalSpacing + geometry.hexHeight / 2

            // Draw triangle for trap
            val path =
                Path().apply {
                    moveTo(centerX, centerY - iconSize / 2)
                    lineTo(centerX + iconSize / 2, centerY + iconSize / 2)
                    lineTo(centerX - iconSize / 2, centerY + iconSize / 2)
                    close()
                }
            drawPath(
                path = path,
                color = if (trap.type == "MAGICAL") Color(0xFF9C27B0) else Color(0xFF795548),
            )
        }

        // Draw barricades
        initialData.barricades.forEach { barricade ->
            val offsetXHex = if (barricade.position.y % 2 == 1) geometry.hexWidth / 2 else 0.0f
            val centerX = geometry.offsetXCanvas + barricade.position.x * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
            val centerY = geometry.offsetYCanvas + barricade.position.y * geometry.verticalSpacing + geometry.hexHeight / 2

            // Draw square for barricade
            drawRect(
                color = Color(0xFF8D6E63),
                topLeft = Offset(centerX - iconSize / 2, centerY - iconSize / 2),
                size = Size(iconSize, iconSize),
            )
        }

        // Draw fiefs
        initialData.fiefs.forEach { fief ->
            val offsetXHex = if (fief.position.y % 2 == 1) geometry.hexWidth / 2 else 0.0f
            val centerX =
                geometry.offsetXCanvas + fief.position.x * geometry.hexWidth +
                    offsetXHex + geometry.hexWidth / 2
            val centerY =
                geometry.offsetYCanvas + fief.position.y * geometry.verticalSpacing +
                    geometry.hexHeight / 2

            drawCircle(
                color = Color(0xFF4CAF50),
                radius = iconSize / 2,
                center = Offset(centerX, centerY),
            )
        }

        // Draw mushrooms
        initialData.mushrooms.forEach { mushroom ->
            val offsetXHex = if (mushroom.position.y % 2 == 1) geometry.hexWidth / 2 else 0.0f
            val centerX =
                geometry.offsetXCanvas + mushroom.position.x * geometry.hexWidth +
                    offsetXHex + geometry.hexWidth / 2
            val centerY =
                geometry.offsetYCanvas + mushroom.position.y * geometry.verticalSpacing +
                    geometry.hexHeight / 2

            drawCircle(
                color = Color(0xFFFF8C00), // Orange for mushrooms
                radius = iconSize / 2,
                center = Offset(centerX, centerY),
            )
        }
    }
}

/**
 * Helper function to draw a hexagon (pointy-top orientation)
 */
private fun DrawScope.drawHexagon(
    centerX: Float,
    centerY: Float,
    radius: Float,
    color: Color,
) {
    val path = Path()
    for (i in 0..5) {
        val angle = PI * (60.0 * i - 30.0) / 180.0
        val x = centerX + (radius * cos(angle)).toFloat()
        val y = centerY + (radius * sin(angle)).toFloat()

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    drawPath(path, color)
}
