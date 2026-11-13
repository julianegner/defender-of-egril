package de.egril.defender.ui.editor.level.initialsetup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.InitialData
import de.egril.defender.editor.TileType
import de.egril.defender.model.AttackerType
import de.egril.defender.model.BridgeType
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
    BRIDGE, // Bridges: RIVER
    FIEF, // Fiefs: PATH
    MUSHROOM, // Mushrooms: PATH
    GARGOYLE, // Gargoyles: PATH or BUILD_AREA
    PORTAL, // Portals: any PATH/SPAWN_POINT/TARGET tile
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
        PlacementMode.BRIDGE -> tileType == TileType.RIVER || map.getRiverTile(position.x, position.y) != null
        PlacementMode.FIEF -> tileType == TileType.PATH
        PlacementMode.MUSHROOM -> tileType == TileType.PATH
        PlacementMode.GARGOYLE -> tileType == TileType.PATH || tileType == TileType.BUILD_AREA
        PlacementMode.PORTAL -> tileType == TileType.PATH || tileType == TileType.SPAWN_POINT || tileType == TileType.TARGET
    }
}

/**
 * Terrain rule for placing a fief of [fiefType] in the editor. Fisher boats float on river tiles
 * (not water spawn points); fishers need a path tile next to water; all other fiefs need a path tile.
 */
fun isValidFiefPlacement(
    position: Position,
    fiefType: FiefType,
    map: EditorMap,
): Boolean {
    val tileType = map.getTileType(position.x, position.y)
    return when (fiefType) {
        FiefType.FISHER_BOAT -> isBoatWaterTile(position, map) && tileType != TileType.SPAWN_POINT
        FiefType.FISHER -> tileType == TileType.PATH && hasAdjacentWaterTile(position, map)
        else -> tileType == TileType.PATH
    }
}

/** Maelstrom tiles swallow boats, so they are not valid boat positions. */
fun isBoatWaterTile(
    position: Position,
    map: EditorMap,
): Boolean =
    isWaterTile(position, map) &&
        map.getRiverTile(position.x, position.y)?.flowDirection != RiverFlow.MAELSTROM

internal fun isWaterTile(
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
internal data class HexGeometry(
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
internal fun calculateHexGeometry(
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
    pendingPortalEntry: Position? = null,
    selectedElement: de.egril.defender.ui.editor.level.initialsetup.SelectedElement? = null,
    onTileClick: (Position) -> Unit = {},
    onTileHover: (Position?) -> Unit = {},
) {
    if (!map.canRenderMinimap()) return

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
        val placementAreaSet = initialData.placementAreas.toSet()

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
                val hasBridge = initialData.bridges.any { it.position == pos }
                val hasFief = initialData.fiefs.any { it.position == pos }
                val hasMushroom = initialData.mushrooms.any { it.position == pos }
                val hasGargoyle = initialData.gargoyles.any { it.position == pos }
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
                                (isTowerBase && !hasDefender && selectedDefenderType != DefenderType.DWARVEN_MINE)
                        PlacementMode.FIEF -> selectedFiefType?.let { isValidFiefPlacement(pos, it, map) } == true
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
                        is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Bridge ->
                            selectedElement.bridge.position == pos
                        is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Fief ->
                            selectedElement.fief.position == pos
                        is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Mushroom ->
                            selectedElement.mushroom.position == pos
                        is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Gargoyle ->
                            selectedElement.gargoyle.position == pos
                        is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Portal ->
                            selectedElement.portal.entryPosition == pos ||
                                selectedElement.portal.exitPosition == pos
                        null -> false
                    }

                // Validation checks for placement conflicts
                // Rule: Only one element (tower, trap, barricade, OR unit) is possible on a tile,
                //       except towers can be placed on top of barricades that support towers (HP >= 100)
                val hasAnyElement = hasDefender || hasAttacker || hasTrap || hasBarricade || hasBridge || hasFief || hasMushroom || hasGargoyle
                val hasConflict =
                    when (placementMode) {
                        PlacementMode.DEFENDER ->
                            // Allow tower on tower base as long as no tower is already there
                            if (isTowerBase && !hasDefender && selectedDefenderType != DefenderType.DWARVEN_MINE) false else hasAnyElement
                        PlacementMode.ATTACKER, PlacementMode.TRAP, PlacementMode.BARRICADE,
                        PlacementMode.BRIDGE, PlacementMode.FIEF, PlacementMode.MUSHROOM,
                        PlacementMode.GARGOYLE,
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
                if (pos in placementAreaSet) {
                    // Translucent overlay marks tiles where the player may place towers, traps and barricades
                    drawHexagon(centerX, centerY, geometry.hexSize, Color(0x66FFD700))
                }
            }
        }

        drawInitialSetupElements(geometry, initialData, selectedElement, pendingPortalEntry)
    }

    LaunchedEffect(hoveredPosition) {
        onTileHover(hoveredPosition)
    }
}

/**
 * Draws the initial setup elements (towers, enemies, traps, barricades, bridges, fiefs,
 * mushrooms, gargoyles and portals) on top of the already drawn tiles.
 */
internal fun DrawScope.drawInitialSetupElements(
    geometry: HexGeometry,
    initialData: InitialData,
    selectedElement: SelectedElement?,
    pendingPortalEntry: Position?,
) {
    fun centerOf(pos: Position): Offset {
        val offsetXHex = if (pos.y % 2 == 1) geometry.hexWidth / 2 else 0.0f
        return Offset(
            geometry.offsetXCanvas + pos.x * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2,
            geometry.offsetYCanvas + pos.y * geometry.verticalSpacing + geometry.hexHeight / 2,
        )
    }

    val iconSize = geometry.hexSize * 1.2f

    initialData.defenders.forEach { defender ->
        drawCircle(color = Color(0xFF2196F3), radius = iconSize / 2, center = centerOf(defender.position))
    }

    initialData.attackers.forEach { attacker ->
        drawCircle(color = Color(0xFFFF0000), radius = iconSize / 2, center = centerOf(attacker.position))
    }

    initialData.traps.forEach { trap ->
        val center = centerOf(trap.position)
        val path =
            Path().apply {
                moveTo(center.x, center.y - iconSize / 2)
                lineTo(center.x + iconSize / 2, center.y + iconSize / 2)
                lineTo(center.x - iconSize / 2, center.y + iconSize / 2)
                close()
            }
        drawPath(
            path = path,
            color = if (trap.type == "MAGICAL") Color(0xFF9C27B0) else Color(0xFF795548),
        )
    }

    initialData.barricades.forEach { barricade ->
        val center = centerOf(barricade.position)
        drawRect(
            color = Color(0xFF8D6E63),
            topLeft = Offset(center.x - iconSize / 2, center.y - iconSize / 2),
            size = Size(iconSize, iconSize),
        )
    }

    initialData.bridges.forEach { bridge ->
        val center = centerOf(bridge.position)
        val color =
            when (bridge.type) {
                BridgeType.WOODEN -> Color(0xFF8B4513)
                BridgeType.STONE -> Color(0xFF808080)
                BridgeType.MAGICAL -> Color(0xFFFF00FF)
            }
        val arcWidth = iconSize
        val arcHeight = iconSize * 0.6f
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - arcWidth / 2, center.y - arcHeight / 2 - iconSize * 0.15f),
            size = Size(arcWidth, arcHeight),
            style = Stroke(width = 1.6f),
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - arcWidth / 2, center.y - arcHeight / 2 + iconSize * 0.15f),
            size = Size(arcWidth, arcHeight),
            style = Stroke(width = 1.6f),
        )
    }

    initialData.fiefs.forEach { fief ->
        drawCircle(color = Color(0xFF4CAF50), radius = iconSize / 2, center = centerOf(fief.position))
    }

    initialData.mushrooms.forEach { mushroom ->
        drawCircle(color = Color(0xFFFF8C00), radius = iconSize / 2, center = centerOf(mushroom.position))
    }

    initialData.gargoyles.forEach { gargoyle ->
        val center = centerOf(gargoyle.position)
        drawCircle(color = Color(0xFF7A7A7A), radius = iconSize / 2, center = center)
        val isGargoyleSelected = (selectedElement as? SelectedElement.Gargoyle)?.gargoyle?.position == gargoyle.position
        if (isGargoyleSelected) {
            drawCircle(color = Color(0xFFFFD700), radius = iconSize / 2, center = center, style = Stroke(width = 3f))
        }
    }

    initialData.portals.forEach { portal ->
        val entry = centerOf(portal.entryPosition)
        val exit = centerOf(portal.exitPosition)
        drawLine(color = Color(0xFF8888FF), start = entry, end = exit, strokeWidth = 1.5f)
        drawCircle(color = Color(0xFF3366FF), radius = iconSize / 2, center = entry)
        drawCircle(color = Color(0xFFFF6600), radius = iconSize / 2, center = exit)
    }

    pendingPortalEntry?.let { pending ->
        drawCircle(color = Color(0x883366FF), radius = iconSize / 2, center = centerOf(pending))
    }
}

/**
 * Helper function to draw a hexagon (pointy-top orientation)
 */
internal fun DrawScope.drawHexagon(
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
