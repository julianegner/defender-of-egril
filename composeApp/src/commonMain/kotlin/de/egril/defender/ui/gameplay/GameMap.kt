package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import de.egril.defender.model.*
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.ui.*
import de.egril.defender.ui.icon.ExplosionIcon
import de.egril.defender.ui.icon.HoleIcon
import com.hyperether.resources.stringResource
import de.egril.defender.ui.editor.map.MapControlState
import de.egril.defender.ui.editor.map.MapControls
import defender_of_egril.composeapp.generated.resources.*
import de.egril.defender.ui.icon.TestTubeIcon
import de.egril.defender.ui.icon.enemy.EnemyIcon
import de.egril.defender.ui.editor.RiverFlowIndicator
import de.egril.defender.ui.hexagon.BaseGridCell
import de.egril.defender.ui.hexagon.HexagonMinimap
import de.egril.defender.ui.hexagon.HexagonalMapConfig
import de.egril.defender.ui.hexagon.HexagonalMapView
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.sqrt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameGrid(
    gameState: GameState,
    selectedDefenderType: DefenderType?,
    selectedDefenderId: Int?,
    selectedTargetId: Int?,
    selectedTargetPosition: Position?,
    selectedMineAction: MineAction?,
    selectedWizardAction: WizardAction? = null,
    onCellClick: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    // State for pan and zoom
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    var isInitialized by remember { mutableStateOf(false) }
    
    // State for hover position (for tower placement preview)
    var hoveredPosition by remember { mutableStateOf<Position?>(null) }

    val hexSize = 40.dp  // Radius of hexagon (center to corner)

    // Initialize viewport to show spawn points (upper left) instead of center
    LaunchedEffect(containerSize, contentSize) {
        if (!isInitialized && containerSize.width > 0 && containerSize.height > 0 
            && contentSize.width > 0 && contentSize.height > 0) {
            // Calculate the maximum offset to show the left edge (spawn points at x=0)
            // When content is larger than container, we can pan within the range
            val contentWidth = contentSize.width * scale
            val contentHeight = contentSize.height * scale
            
            if (contentWidth > containerSize.width) {
                // Set offsetX to maximum positive value to show left edge (spawn points)
                val maxOffsetX = (contentWidth - containerSize.width) / 2
                offsetX = maxOffsetX
            }
            
            if (contentHeight > containerSize.height) {
                // Set offsetY to maximum positive value to show top edge
                val maxOffsetY = (contentHeight - containerSize.height) / 2
                offsetY = maxOffsetY
            }
            
            isInitialized = true
        }
    }

    // Calculate target circle info for each tile
    // Find the selected defender and track its actions for dependency tracking
    val selectedDefender = gameState.defenders.find { it.id == selectedDefenderId }
    val selectedDefenderActions = selectedDefender?.actionsRemaining?.value
    
    val targetCircleMap = remember(selectedTargetPosition, selectedDefenderId, selectedDefenderActions, gameState.defenders.size) {
        if (selectedTargetPosition == null || selectedDefenderId == null || selectedDefender == null) {
            emptyMap()
        } else {
            val attackType = selectedDefender.type.attackType
            
            // Don't show target circles if the tower has no action points left
            if (selectedDefender.actionsRemaining.value <= 0) {
                emptyMap()
            } else {
                val markerColor = when (attackType) {
                    AttackType.AREA -> Color(0xFFFF5722)  // Deep orange/red for fireball
                    AttackType.LASTING -> Color(0xFF4CAF50)  // Green for acid
                    AttackType.MELEE, AttackType.RANGED -> Color.DarkGray  // DarkGray for single-target
                    AttackType.NONE -> null  // No target circles for special structures
                }
                
                if (markerColor == null) {
                    emptyMap()
                } else {
                    val result = mutableMapOf<Position, TargetCircleInfo>()
                    val areaRadius = selectedDefender.areaEffectRadius
                    val isExtendedArea = areaRadius >= 2
                    
                    // Check if target position has a magical bridge (which cannot be targeted by non-area attacks)
                    val hasMagicalBridge = gameState.isBridgeAt(selectedTargetPosition) &&
                        gameState.getBridgeAt(selectedTargetPosition)?.type == BridgeType.MAGICAL
                    
                    // Check if there's an enemy at the target position
                    val hasEnemy = gameState.attackers.any { 
                        it.position.value == selectedTargetPosition && !it.isDefeated.value 
                    }
                    
                    // Don't show target circles for non-area attacks on magical bridges UNLESS there's an enemy
                    if (hasMagicalBridge && !hasEnemy && attackType != AttackType.AREA && attackType != AttackType.LASTING) {
                        emptyMap()
                    } else {
                        // Central target tile
                        result[selectedTargetPosition] = TargetCircleInfo.CentralTarget(
                            color = markerColor,
                            attackType = attackType,
                            isExtendedArea = isExtendedArea
                        )
                        
                        // For AREA and LASTING attacks, add neighbor tiles that are on the path, or have bridges/enemies
                        if (attackType == AttackType.AREA || attackType == AttackType.LASTING) {
                            if (areaRadius == 1) {
                                // Standard radius 1 - use getHexNeighbors
                                val neighbors = selectedTargetPosition.getHexNeighbors()
                                    .filter { neighbor ->
                                        neighbor.x >= 0 && neighbor.x < gameState.level.gridWidth &&
                                        neighbor.y >= 0 && neighbor.y < gameState.level.gridHeight &&
                                        (gameState.level.isOnPath(neighbor) || 
                                         gameState.isBridgeAt(neighbor) || 
                                         gameState.attackers.any { it.position.value == neighbor && !it.isDefeated.value })
                                    }

                                for (neighbor in neighbors) {
                                    result[neighbor] = TargetCircleInfo.NeighborTarget(
                                        color = markerColor,
                                        attackType = attackType,
                                        centerPosition = selectedTargetPosition,
                                        thisPosition = neighbor,
                                        distanceFromCenter = 1,
                                        isExtendedArea = false
                                    )
                                }
                            } else {
                                // Extended radius 2 (level 20+) - use getHexNeighborsWithinRadius
                                val allNeighbors = selectedTargetPosition.getHexNeighborsWithinRadius(
                                    areaRadius,
                                    gameState.level.gridWidth,
                                    gameState.level.gridHeight
                                ).filter { neighbor ->
                                    gameState.level.isOnPath(neighbor) || 
                                    gameState.isBridgeAt(neighbor) || 
                                    gameState.attackers.any { it.position.value == neighbor && !it.isDefeated.value }
                                }
                                
                                for (neighbor in allNeighbors) {
                                    val distance = selectedTargetPosition.hexDistanceTo(neighbor)
                                    result[neighbor] = TargetCircleInfo.NeighborTarget(
                                        color = markerColor,
                                        attackType = attackType,
                                        centerPosition = selectedTargetPosition,
                                        thisPosition = neighbor,
                                        distanceFromCenter = distance,
                                        isExtendedArea = true
                                    )
                                }
                            }
                        }
                        println("Target circle map: $result")
                        result
                    }
                }
            }
        }
    }

    Box(modifier = modifier
        .onSizeChanged { containerSize = it }
    ) {
        HexagonalMapView(
            gridWidth = gameState.level.gridWidth,
            gridHeight = gameState.level.gridHeight,
            config = HexagonalMapConfig(
                hexSize = hexSize.value,
                enableKeyboardNavigation = true,  // Enable keyboard navigation for gameplay
                enablePanNavigation = true  // Enable pan navigation for gameplay
            ),
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            onScaleChange = { newScale -> scale = newScale },
            onOffsetChange = { newOffsetX, newOffsetY ->
                offsetX = newOffsetX
                offsetY = newOffsetY
            },
            onActualContentSizeChange = { newContentSize ->
                contentSize = newContentSize
            },
            focusTrigger = gameState.phase.value,  // Request focus when game phase changes (e.g., after "Start Battle")
            modifier = Modifier.fillMaxSize()
        ) { position ->
            GridCell(
                position = position,
                gameState = gameState,
                isSelected = selectedDefenderType != null,
                isDefenderSelected = selectedDefenderId?.let { selId ->
                    gameState.defenders.find { it.position.value == position }?.id == selId
                } ?: false,
                isTargetSelected = gameState.attackers.find { it.position.value == position }?.id == selectedTargetId,
                selectedDefenderId = selectedDefenderId,
                selectedTargetPosition = selectedTargetPosition,
                selectedMineAction = selectedMineAction,
                selectedWizardAction = selectedWizardAction,
                targetCircleInfo = targetCircleMap[position],
                onClick = { onCellClick(position) },
                hexSize = hexSize,
                selectedDefenderType = selectedDefenderType,
                hoveredPosition = hoveredPosition,
                onHoverChange = { isHovering ->
                    hoveredPosition = if (isHovering) position else null
                }
            )
        }

        MapControls(
            mapControlState = MapControlState(
                zoomLevel = scale,
                offsetX = offsetX,
                offsetY = offsetY
            ),
            onStateChange = { newState ->
                val newScale = newState.zoomLevel
                val (constrainedX, constrainedY) = constrainMapOffsets(
                    newState.offsetX, 
                    newState.offsetY, 
                    newScale,
                    containerSize,
                    contentSize
                )
                scale = newScale
                offsetX = constrainedX
                offsetY = constrainedY
            }
        ) {
            // Minimap
            Box(
                modifier = Modifier.size(120.dp)
            ) {
                HexagonMinimap(
                    level = gameState.level,
                    config = MinimapConfig(
                        showSpawnPoints = true,
                        showTarget = true,
                        showTowers = true,
                        showEnemies = true,
                        showViewport = true,
                        minimapSizeDp = 120f
                    ),
                    gameState = gameState,
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    containerSize = containerSize,
                    contentSize = contentSize,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun GridCell(
    position: Position,
    gameState: GameState,
    isSelected: Boolean,
    isDefenderSelected: Boolean,
    isTargetSelected: Boolean,
    selectedDefenderId: Int?,
    selectedTargetPosition: Position?,
    selectedMineAction: MineAction?,
    selectedWizardAction: WizardAction? = null,
    targetCircleInfo: TargetCircleInfo?,
    onClick: () -> Unit,
    hexSize: androidx.compose.ui.unit.Dp = 48.dp,
    selectedDefenderType: DefenderType? = null,
    hoveredPosition: Position? = null,
    onHoverChange: ((Boolean) -> Unit)? = null
) {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value
    
    val isSpawnPoint = gameState.level.isSpawnPoint(position)
    val isTarget = gameState.level.isTargetPosition(position)
    val isOnPath = gameState.level.isOnPath(position)
    val isBuildIsland = gameState.level.isBuildIsland(position)
    val isBuildArea = gameState.level.isBuildArea(position)
    val isRiverTile = gameState.level.isRiverTile(position)
    val defender = gameState.defenders.find { it.position.value == position }
    val attacker = gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
    
    // Check for healing effects at this position
    val healingEffect = gameState.healingEffects.find { it.position == position }
    
    // Determine the tile type for background image loading
    val riverTile = gameState.level.getRiverTile(position)
    val isMaelstrom = riverTile?.flowDirection == RiverFlow.MAELSTROM
    
    val tileType = when {
        isSpawnPoint -> de.egril.defender.editor.TileType.SPAWN_POINT
        isTarget -> de.egril.defender.editor.TileType.TARGET
        isRiverTile -> de.egril.defender.editor.TileType.RIVER
        isOnPath -> de.egril.defender.editor.TileType.PATH
        isBuildIsland -> de.egril.defender.editor.TileType.ISLAND
        isBuildArea -> de.egril.defender.editor.TileType.BUILD_AREA
        else -> de.egril.defender.editor.TileType.NO_PLAY
    }
    
    // Get tile background painter (will be null if images are disabled or not available)
    // For ready towers on build areas or islands, don't show tile background to make towers more visible
    val shouldShowTileImage = !(defender != null && defender.isReady && (isBuildArea || isBuildIsland))
    val tilePainter = if (shouldShowTileImage) {
        TileImageProvider.getTilePainter(tileType, isMaelstrom = isMaelstrom)
    } else {
        null
    }

    // Check for field effects at this position
    val fieldEffect = gameState.fieldEffects.find { it.position == position }

    // Check for traps at this position
    val trap = gameState.traps.find { it.position == position }

    // Check if this cell is in range of the selected defender
    val cellIsInRange = selectedDefenderId?.let { defenderId ->
        val selectedDefender = gameState.defenders.find { it.id == defenderId }
        selectedDefender?.let { sel ->
            if (sel.position.value == position) {
                false  // Don't highlight the defender's own cell
            } else {
                val distance = sel.position.value.distanceTo(position)
                distance >= sel.type.minRange && distance <= sel.range
            }
        } ?: false
    } ?: false
    
    // Calculate hover preview for tower placement
    val isHoveringForPreview = hoveredPosition == position && selectedDefenderType != null
    // Include river tiles as buildable (for rafts)
    val isBuildableTile = (isBuildArea || isBuildIsland || isRiverTile) && defender == null && attacker == null
    val showPlacementPreview = isHoveringForPreview && isBuildableTile
    
    // Check if hovered position is buildable (needed for range preview calculation)
    // Include river tiles as buildable (for rafts)
    val hoveredPositionIsBuildable = if (hoveredPosition != null && selectedDefenderType != null) {
        val hoveredIsBuildArea = gameState.level.isBuildArea(hoveredPosition)
        val hoveredIsBuildIsland = gameState.level.isBuildIsland(hoveredPosition)
        val hoveredIsRiver = gameState.level.isRiverTile(hoveredPosition)
        val hoveredHasDefender = gameState.defenders.any { it.position.value == hoveredPosition }
        val hoveredHasAttacker = gameState.attackers.any { it.position.value == hoveredPosition && !it.isDefeated.value }
        (hoveredIsBuildArea || hoveredIsBuildIsland || hoveredIsRiver) && !hoveredHasDefender && !hoveredHasAttacker
    } else {
        false
    }
    
    // Calculate range preview tiles when hovering over a buildable tile with a tower type selected
    // Reuse the same logic as for existing towers (cellIsInRange)
    val isInPreviewRange = if (showPlacementPreview) {
        false  // The hovered tile itself is not in the range
    } else if (selectedDefenderType != null && hoveredPosition != null && hoveredPositionIsBuildable) {
        // Check if this tile is in range of the hovered position (same logic as cellIsInRange)
        val distance = hoveredPosition.distanceTo(position)
        val minRange = selectedDefenderType.minRange
        val maxRange = selectedDefenderType.baseRange
        
        // Only show range on valid target tiles (path or river for area attacks, path only for single-target)
        val hasAreaAttackPreview = selectedDefenderType.attackType == AttackType.AREA || 
                                   selectedDefenderType.attackType == AttackType.LASTING
        val isValidPreviewTargetTile = if (hasAreaAttackPreview) {
            isOnPath || isRiverTile
        } else {
            isOnPath
        }
        
        distance >= minRange && distance <= maxRange && isValidPreviewTargetTile
    } else {
        false
    }

    // Base background color based on area type - ALWAYS visible
    // Build islands + strips adjacent to path allow tower placement
    val baseBackgroundColor = when {
        isBuildIsland -> GamePlayColors.BuildIsland  // Light green for build islands
        isBuildArea -> GamePlayColors.BuildStrip  // Medium green for strips adjacent to path
        isOnPath -> GamePlayColors.Path  // Cream/beige for enemy path
        isRiverTile -> GamePlayColors.River  // Blue for river tiles
        else -> GamePlayColors.NonPlayable  // Light gray for off-path areas (non-playable)
    }

    // Apply slight tint for selection states, but keep base color visible
    // Override with red background for enemy units and colored background for defenders
    // During INITIAL_BUILDING phase, don't apply any selection tints
    // Field effects also modify the background color
    // Special case: Keep river background visible for defenders on rafts
    val backgroundColor = when {
        attacker != null -> if (isDarkMode) GamePlayColors.ErrorDark else GamePlayColors.Error  // Darker red background for enemies in dark mode
        defender != null && isRiverTile -> {
            // Keep river blue background visible for defenders on rafts
            GamePlayColors.River
        }
        defender != null -> {
            when {
                !defender.isReady -> GamePlayColors.Building  // Gray for building
                defender.actionsRemaining.value <= 0 -> if (isDarkMode) GamePlayColors.InfoDark else GamePlayColors.InfoLight  // Darker blue for used actions in dark mode
                else -> if (isDarkMode) GamePlayColors.InfoDark else GamePlayColors.Info  // Darker blue for ready towers in dark mode
            }
        }

        fieldEffect != null -> {
            when (fieldEffect.type) {
                FieldEffectType.FIREBALL -> GamePlayColors.Warning.copy(alpha = 0.5f)  // Orange tint for fireball
                FieldEffectType.ACID -> GamePlayColors.Success.copy(alpha = 0.6f)  // Green tint for acid
            }
        }

        trap != null -> GamePlayColors.Trap.copy(alpha = 0.6f)  // Brown tint for trap
        
        // Tower placement preview - highlight the hovered build tile differently than range tiles
        showPlacementPreview -> GamePlayColors.Yellow.copy(alpha = 0.4f)  // Light yellow for the build tile being hovered
        isInPreviewRange -> GamePlayColors.Success.copy(alpha = 0.2f)  // Very light green for range preview tiles
        
        isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.7f)
        isTargetSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.8f)
        else -> baseBackgroundColor  // No selection highlighting during placement or in initial phase
    }

    // Border color - use borders to indicate entities instead of background
    // For range visualization, show green border on path tiles OR river tiles in range (only if tower has actions)
    val showRange = selectedDefenderId?.let { defenderId ->
        val selectedDefender = gameState.defenders.find { it.id == defenderId }
        selectedDefender?.isReady == true && selectedDefender.actionsRemaining.value > 0
    } ?: false

    // When placing trap, don't show green border on tiles with enemies
    val isTrapPlacement = selectedMineAction == MineAction.BUILD_TRAP || selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP
    val hasEnemy = attacker != null
    val canPlaceTrapHere = !hasEnemy || !isTrapPlacement

    // Check if defender has area attack capability
    val hasAreaAttack = selectedDefenderId?.let { defenderId ->
        val selectedDefender = gameState.defenders.find { it.id == defenderId }
        selectedDefender?.type?.attackType == AttackType.AREA || selectedDefender?.type?.attackType == AttackType.LASTING
    } ?: false

    // River tiles are valid targets for area attacks
    val isValidTargetTile = if (hasAreaAttack) {
        isOnPath || isRiverTile
    } else {
        isOnPath
    }

    val borderColor = when {
        // Tower placement preview - dashed borders for preview (we'll handle this with Canvas later)
        showPlacementPreview -> GamePlayColors.Yellow  // Yellow border for hovered build tile
        isInPreviewRange -> GamePlayColors.Success  // Green border for range preview tiles
        
        cellIsInRange && isValidTargetTile && showRange && canPlaceTrapHere -> GamePlayColors.Success  // Green border for tiles in range (path or river for area attacks)
        isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> GamePlayColors.Yellow  // Yellow border for selected defender (not during initial building)
        isSpawnPoint -> GamePlayColors.WarningDark  // Darker orange border for spawn in dark mode
        isTarget -> GamePlayColors.Success  // Green border for target (adapts to dark mode automatically)
        attacker != null -> GamePlayColors.ErrorDark  // Darker red border for enemies
        defender != null -> if (defender.isReady) GamePlayColors.InfoDark else GamePlayColors.Building  // Darker blue/gray border for towers
        fieldEffect != null -> {
            when (fieldEffect.type) {
                FieldEffectType.FIREBALL -> GamePlayColors.WarningDeep  // Deep orange border for fireball
                FieldEffectType.ACID -> GamePlayColors.Success  // Green border for acid
            }
        }

        trap != null -> GamePlayColors.Trap  // Brown border for trap
        else -> Color.Transparent  // No borders for empty cells
    }

    // Thicker borders for important elements
    val borderWidth = when {
        showPlacementPreview -> 6.dp  // Double thickness for hovered build tile
        isInPreviewRange -> 3.dp  // Medium border for range preview
        isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> 5.dp  // Extra thick border for selected defender (not during initial building)
        cellIsInRange && isValidTargetTile && showRange && canPlaceTrapHere -> 4.dp  // Thick border for cells in range (path or river for area attacks)
        isSpawnPoint || isTarget -> 3.dp
        attacker != null || defender != null -> 3.dp
        fieldEffect != null -> 3.dp  // Thick border for field effects
        else -> 0.dp  // No border for empty cells
    }
    
    // Flag to indicate dashed border (for preview only)
    val useDashedBorder = showPlacementPreview || isInPreviewRange
    
    // Determine if we should use gradient blending
    val useTileImages = de.egril.defender.ui.settings.AppSettings.useTileImages.value
    val useTileSmoothTransitions = de.egril.defender.ui.settings.AppSettings.useTileSmoothTransitions.value
    val shouldUseGradientBlending = useTileImages && useTileSmoothTransitions && tilePainter != null
    
    // Helper function to get tile type for a position
    val getNeighborTileType: (Position) -> de.egril.defender.editor.TileType? = { pos ->
        if (pos.x < 0 || pos.x >= gameState.level.gridWidth || 
            pos.y < 0 || pos.y >= gameState.level.gridHeight) {
            null
        } else {
            when {
                gameState.level.isSpawnPoint(pos) -> de.egril.defender.editor.TileType.SPAWN_POINT
                gameState.level.isTargetPosition(pos) -> de.egril.defender.editor.TileType.TARGET
                gameState.level.isRiverTile(pos) -> de.egril.defender.editor.TileType.RIVER
                gameState.level.isOnPath(pos) -> de.egril.defender.editor.TileType.PATH
                gameState.level.isBuildIsland(pos) -> de.egril.defender.editor.TileType.ISLAND
                gameState.level.isBuildArea(pos) -> de.egril.defender.editor.TileType.BUILD_AREA
                else -> de.egril.defender.editor.TileType.NO_PLAY
            }
        }
    }
    
    // Pre-compute neighbor tile types for gradient blending
    val neighborTileTypes = remember(position, gameState.defenders.size, gameState.level) {
        if (!shouldUseGradientBlending) {
            emptyMap()
        } else {
            val neighbors = position.getHexNeighbors()
            neighbors.mapNotNull { neighborPos ->
                val neighborType = getNeighborTileType(neighborPos)
                if (neighborType != null) {
                    neighborPos to neighborType
                } else {
                    null
                }
            }.toMap()
        }
    }
    
    // Get the actual painters for neighbors (must be done in @Composable context)
    val neighborPainters = neighborTileTypes.mapValues { (pos, type) ->
        // Check if there's a ready defender on this tile (build area or island)
        val neighborDefender = gameState.defenders.find { it.position.value == pos }
        val neighborIsReady = neighborDefender?.isReady == true
        val neighborIsBuildArea = gameState.level.isBuildArea(pos)
        val neighborIsBuildIsland = gameState.level.isBuildIsland(pos)
        val shouldShowNeighborTile = !(neighborDefender != null && neighborIsReady && 
                                       (neighborIsBuildArea || neighborIsBuildIsland))
        
        if (shouldShowNeighborTile) {
            val neighborRiverTile = gameState.level.getRiverTile(pos)
            val neighborIsMaelstrom = neighborRiverTile?.flowDirection == RiverFlow.MAELSTROM
            TileImageProvider.getTilePainter(type, isMaelstrom = neighborIsMaelstrom)
        } else {
            null
        }
    }

    if (shouldUseGradientBlending) {
        GradientBlendedTileCell(
            hexSize = hexSize,
            position = position,
            tileType = tileType,
            backgroundColor = backgroundColor,
            borderColor = if (useDashedBorder) Color.Transparent else borderColor,
            borderWidth = if (useDashedBorder) 0.dp else borderWidth,
            backgroundPainter = tilePainter,
            onClick = onClick,
            onHover = onHoverChange,
            getNeighborTileType = getNeighborTileType,
            getNeighborTilePainter = { pos, _ -> neighborPainters[pos] }
        ) {
            GridCellContent(
                position = position,
                gameState = gameState,
                attacker = attacker,
                healingEffect = healingEffect,
                defender = defender,
                fieldEffect = fieldEffect,
                trap = trap,
                isSpawnPoint = isSpawnPoint,
                isTarget = isTarget,
                isRiverTile = isRiverTile,
                showPlacementPreview = showPlacementPreview,
                selectedDefenderType = selectedDefenderType,
                targetCircleInfo = targetCircleInfo,
                useDashedBorder = useDashedBorder,
                borderColor = borderColor,
                borderWidth = borderWidth,
                hexSize = hexSize
            )
        }
    } else {
        BaseGridCell(
            hexSize = hexSize,
            backgroundColor = backgroundColor,
            borderColor = if (useDashedBorder) Color.Transparent else borderColor,
            borderWidth = if (useDashedBorder) 0.dp else borderWidth,
            backgroundPainter = tilePainter,
            onClick = onClick,
            onHover = onHoverChange
        ) {
            GridCellContent(
                position = position,
                gameState = gameState,
                attacker = attacker,
                healingEffect = healingEffect,
                defender = defender,
                fieldEffect = fieldEffect,
                trap = trap,
                isSpawnPoint = isSpawnPoint,
                isTarget = isTarget,
                isRiverTile = isRiverTile,
                showPlacementPreview = showPlacementPreview,
                selectedDefenderType = selectedDefenderType,
                targetCircleInfo = targetCircleInfo,
                useDashedBorder = useDashedBorder,
                borderColor = borderColor,
                borderWidth = borderWidth,
                hexSize = hexSize
            )
        }
    }
}

/**
 * Content displayed inside a grid cell (separated for reuse between BaseGridCell and GradientBlendedTileCell)
 */
@Composable
private fun BoxScope.GridCellContent(
    position: Position,
    gameState: GameState,
    attacker: Attacker?,
    healingEffect: HealingEffect?,
    defender: Defender?,
    fieldEffect: FieldEffect?,
    trap: Trap?,
    isSpawnPoint: Boolean,
    isTarget: Boolean,
    isRiverTile: Boolean,
    showPlacementPreview: Boolean,
    selectedDefenderType: DefenderType?,
    targetCircleInfo: TargetCircleInfo?,
    useDashedBorder: Boolean,
    borderColor: Color,
    borderWidth: Dp,
    hexSize: Dp
) {
        when {
            attacker != null -> {
                // Use graphical icon for enemy units
                // Key by id, position, level, and currentHealth to force recomposition when any changes
                key(
                    attacker.id,
                    attacker.position.value.x,
                    attacker.position.value.y,
                    attacker.level,
                    attacker.currentHealth.value
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        EnemyIcon(attacker = attacker)
                        // Show healing effect overlay if present
                        if (healingEffect != null) {
                            // Show 3 green "+" symbols in different sizes
                            // Positioned with smaller symbols higher than larger ones
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                // Large + symbol at center
                                Text(
                                    "+",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color(0xFF4CAF50), // Green
                                    fontWeight = FontWeight.Bold
                                )
                                // Medium + symbol - offset left and higher
                                Text(
                                    "+",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color(0xFF4CAF50), // Green
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.offset(x = (-12).dp, y = (-8).dp)
                                )
                                // Small + symbol - offset right and even higher
                                Text(
                                    "+",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color(0xFF4CAF50), // Green
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.offset(x = 12.dp, y = (-16).dp)
                                )
                            }
                        }
                    }
                }
            }

            defender != null -> {
                // Use graphical icon for towers
                // Key by id, position, level and actionsRemaining to force recomposition when these change
                key(
                    defender.id,
                    defender.position.value.x,
                    defender.position.value.y,
                    defender.level.value,
                    defender.actionsRemaining.value,
                    defender.buildTimeRemaining.value,
                    defender.isDisabled.value,
                    defender.disabledTurnsRemaining.value
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        TowerIcon(defender = defender, gameState = gameState)
                        // Show red "XT" overlay if tower is disabled by Red Witch
                        if (defender.isDisabled.value && defender.disabledTurnsRemaining.value > 0) {
                            Text(
                                "${defender.disabledTurnsRemaining.value}T",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            fieldEffect != null -> {
                // Show field effect info
                when (fieldEffect.type) {
                    FieldEffectType.FIREBALL -> {
                        // Show fireball symbol
                        ExplosionIcon(size = 28.dp)
                    }

                    FieldEffectType.ACID -> {
                        // Show acid splash with damage and duration
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            TestTubeIcon(size = 20.dp)
                            Text(
                                "-${fieldEffect.damage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${fieldEffect.turnsRemaining}T",
                                style = MaterialTheme.typography.labelSmall,
                                color = GamePlayColors.Yellow
                            )
                        }
                    }
                }
            }

            trap != null -> {
                // Show trap icon based on trap type
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (trap.type) {
                        TrapType.MAGICAL -> {
                            // Magical trap - show pentagram (no damage display)
                            PentagramIcon(size = 24.dp)
                        }

                        TrapType.DWARVEN -> {
                            // Dwarven trap - show hole icon with damage
                            HoleIcon(size = 20.dp)
                            Text(
                                "-${trap.damage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            isSpawnPoint -> {
                // Show spawn indicator when cell is empty
                Text(
                    stringResource(Res.string.spawn),
                    style = MaterialTheme.typography.labelSmall,
                    color = GamePlayColors.Warning
                )
            }

            isTarget -> {
                // Show target indicator when cell is empty
                Text(
                    stringResource(Res.string.target),
                    style = MaterialTheme.typography.labelSmall,
                    color = GamePlayColors.Success
                )
            }

            isRiverTile -> {
                // Check if there's a bridge at this position
                val bridge = gameState.getBridgeAt(position)
                if (bridge != null) {
                    // Show bridge over river
                    BridgeVisualization(bridge = bridge)
                } else {
                    // Show river flow direction arrows
                    val riverTile = gameState.level.getRiverTile(position)
                    if (riverTile != null) {
                        // Don't show trap icon on maelstrom when tile images are enabled
                        // (the tile_river_maelstrom.png image already shows the maelstrom visually)
                        val useTileImages = AppSettings.useTileImages.value
                        val isMaelstromWithTileImage = riverTile.flowDirection == RiverFlow.MAELSTROM && useTileImages

                        if (!isMaelstromWithTileImage) {
                            RiverFlowIndicator(
                                flowDirection = riverTile.flowDirection,
                                flowSpeed = riverTile.flowSpeed,
                                size = 28.dp
                            )
                        }
                    }
                }
            }
        }

        // Show half-transparent tower icon on hovered build tile
        if (showPlacementPreview && selectedDefenderType != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.5f),  // 50% transparency
                contentAlignment = Alignment.Center
            ) {
                TowerTypeIcon(
                    defenderType = selectedDefenderType,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Draw target circles AFTER other content so they appear on top
        // Inner circles on central target tile, outer ring segments on neighbor tiles
        targetCircleInfo?.let { info ->
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(11f)
            ) {
                when (info) {
                    is TargetCircleInfo.CentralTarget -> {
                        // Draw 3 inner circles on the central target tile
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val center = Offset(centerX, centerY)

                        // Filled inner circle
                        drawCircle(
                            color = info.color,
                            radius = TargetCircleConstants.INNER_CIRCLE_1_RADIUS,
                            center = center
                        )

                        // Two stroke circles
                        drawCircle(
                            color = info.color,
                            radius = TargetCircleConstants.INNER_CIRCLE_2_RADIUS,
                            center = center,
                            style = Stroke(
                                width = TargetCircleConstants.INNER_CIRCLE_STROKE_WIDTH
                            )
                        )

                        drawCircle(
                            color = info.color,
                            radius = TargetCircleConstants.INNER_CIRCLE_3_RADIUS,
                            center = center,
                            style = Stroke(
                                width = TargetCircleConstants.INNER_CIRCLE_STROKE_WIDTH
                            )
                        )
                    }

                    is TargetCircleInfo.NeighborTarget -> {
                        // Draw outer ring segments on neighbor tiles (only for AREA and LASTING)
                        if (info.attackType == AttackType.AREA || info.attackType == AttackType.LASTING) {
                            // Use different radii based on distance from center
                            // Distance 2 (extended area for level 20+) uses larger radii
                            val radius1 = if (info.distanceFromCenter >= 2)
                                TargetCircleConstants.EXTENDED_OUTER_CIRCLE_1_RADIUS
                            else
                                TargetCircleConstants.OUTER_CIRCLE_1_RADIUS
                            val radius2 = if (info.distanceFromCenter >= 2)
                                TargetCircleConstants.EXTENDED_OUTER_CIRCLE_2_RADIUS
                            else
                                TargetCircleConstants.OUTER_CIRCLE_2_RADIUS
                            val radius3 = if (info.distanceFromCenter >= 2)
                                TargetCircleConstants.EXTENDED_OUTER_CIRCLE_3_RADIUS
                            else
                                TargetCircleConstants.OUTER_CIRCLE_3_RADIUS

                            // Draw 3 concentric arc segments
                            CircularSegmentDrawer.drawArcSegment(
                                drawScope = this,
                                color = info.color,
                                radius = radius1,
                                strokeWidth = TargetCircleConstants.OUTER_CIRCLE_STROKE_WIDTH,
                                centerPos = info.centerPosition,
                                neighborPos = info.thisPosition,
                                hexSize = hexSize.value
                            )

                            CircularSegmentDrawer.drawArcSegment(
                                drawScope = this,
                                color = info.color,
                                radius = radius2,
                                strokeWidth = TargetCircleConstants.OUTER_CIRCLE_STROKE_WIDTH,
                                centerPos = info.centerPosition,
                                neighborPos = info.thisPosition,
                                hexSize = hexSize.value
                            )

                            CircularSegmentDrawer.drawArcSegment(
                                drawScope = this,
                                color = info.color,
                                radius = radius3,
                                strokeWidth = TargetCircleConstants.OUTER_CIRCLE_STROKE_WIDTH,
                                centerPos = info.centerPosition,
                                neighborPos = info.thisPosition,
                                hexSize = hexSize.value
                            )
                        }
                    }
                }
            }
        }

        // Draw dashed border for tower placement preview
        if (useDashedBorder) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(12f)
            ) {
                val sqrt3 = sqrt(3.0).toFloat()
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val radius = minOf(size.width, size.height) / 2f

                // Create hexagon path
                val path = Path().apply {
                    // Top point
                    moveTo(centerX, centerY - radius)
                    // Top-right
                    lineTo(centerX + radius * sqrt3 / 2f, centerY - radius / 2f)
                    // Bottom-right
                    lineTo(centerX + radius * sqrt3 / 2f, centerY + radius / 2f)
                    // Bottom point
                    lineTo(centerX, centerY + radius)
                    // Bottom-left
                    lineTo(centerX - radius * sqrt3 / 2f, centerY + radius / 2f)
                    // Top-left
                    lineTo(centerX - radius * sqrt3 / 2f, centerY - radius / 2f)
                    // Close the path
                    close()
                }

                // Draw dashed border
                drawPath(
                    path = path,
                    color = borderColor,
                    style = Stroke(
                        width = borderWidth.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(10f, 5f),  // 10px dash, 5px gap
                            phase = 0f
                        )
                    )
                )
            }
        }
}

/**
 * Visualize a bridge over a river tile
 */
@Composable
fun BridgeVisualization(bridge: Bridge) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Draw bridge arc
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val arcWidth = size.width * 0.8f
            val arcHeight = size.height * 0.4f
            
            // Bridge color based on type
            val bridgeColor = when (bridge.type) {
                BridgeType.WOODEN -> Color(0xFF8B4513)  // Brown
                BridgeType.STONE -> Color(0xFF808080)   // Gray
                BridgeType.MAGICAL -> Color(0xFFFF00FF) // Magenta/purple for magical
            }
            
            // Draw half arc (bridge shape) - opening at bottom
            drawArc(
                color = bridgeColor,
                startAngle = 180f,  // Start from bottom-left
                sweepAngle = 180f,  // Draw top half
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    centerX - arcWidth / 2,
                    centerY - arcHeight / 2
                ),
                size = androidx.compose.ui.geometry.Size(arcWidth, arcHeight),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
            )
            
            // For magical bridges, add sparkle effect above the arc
            if (bridge.type == BridgeType.MAGICAL) {
                // Draw sparkles around the arc (top side)
                val sparklePositions = listOf(
                    androidx.compose.ui.geometry.Offset(centerX - arcWidth / 3, centerY - arcHeight / 2 + 5),
                    androidx.compose.ui.geometry.Offset(centerX, centerY - arcHeight / 2),
                    androidx.compose.ui.geometry.Offset(centerX + arcWidth / 3, centerY - arcHeight / 2 + 5)
                )
                sparklePositions.forEach { pos ->
                    drawCircle(
                        color = Color.White,
                        radius = 2f,
                        center = pos
                    )
                }
            }
        }
        
        // Display health or turn count below the arc
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 4.dp)
        ) {
            when (bridge.type) {
                BridgeType.WOODEN, BridgeType.STONE -> {
                    // Show remaining health
                    Text(
                        text = "${bridge.currentHealth.value}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                BridgeType.MAGICAL -> {
                    // Show remaining turns
                    Text(
                        text = "${bridge.turnsRemaining.value}T",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 13.sp,
                        color = Color(0xFFFFFF00),  // Yellow
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
