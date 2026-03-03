package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import de.egril.defender.model.*
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.ui.*
import de.egril.defender.ui.animations.BarricadeDamageAnimation
import de.egril.defender.ui.animations.BombExplosionAnimation
import de.egril.defender.ui.animations.CoolingAreaAnimation
import de.egril.defender.ui.animations.InstantTowerSpellAnimation
import de.egril.defender.ui.animations.SpellDoubleLevelColor
import de.egril.defender.ui.animations.SpellDoubleReachColor
import de.egril.defender.ui.animations.FearSpellAnimation
import de.egril.defender.ui.animations.FreezeSpellAnimation
import de.egril.defender.ui.animations.GreenWitchHealingAnimation
import de.egril.defender.ui.icon.CrossIcon
import de.egril.defender.ui.icon.BombIcon
import de.egril.defender.ui.icon.ExplosionIcon
import de.egril.defender.ui.icon.GateIcon
import de.egril.defender.ui.icon.HeartIcon
import de.egril.defender.ui.icon.PlusIcon
import de.egril.defender.ui.icon.SwordIcon
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.icon.WoodIcon
import com.hyperether.resources.stringResource
import de.egril.defender.ui.editor.map.MapControlState
import de.egril.defender.ui.editor.map.MapControls
import defender_of_egril.composeapp.generated.resources.*
import de.egril.defender.ui.icon.TestTubeIcon
import de.egril.defender.ui.icon.enemy.EnemyIcon
import de.egril.defender.ui.editor.RiverFlowIndicator
import de.egril.defender.ui.hexagon.BaseGridCell
import de.egril.defender.ui.hexagon.HexagonMinimap
import de.egril.defender.ui.hexagon.HexagonShape
import de.egril.defender.ui.hexagon.HexagonalMapConfig
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.hexagon.HexagonalMapView
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.rememberMapImageState
import kotlin.math.roundToInt
import kotlin.math.sqrt
import de.egril.defender.config.LogConfig

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
    selectedBarricadeAction: BarricadeAction? = null,
    onCellClick: (Position) -> Unit,
    modifier: Modifier = Modifier,
    scrollToPosition: Position? = null,
    onScrollToPositionConsumed: (() -> Unit)? = null
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

    // Scroll to position when requested (e.g. bomb explosion)
    LaunchedEffect(scrollToPosition) {
        if (scrollToPosition != null && containerSize.width > 0 && contentSize.width > 0) {
            // Calculate pixel position of the target hex
            val hexSizePx = hexSize.value
            val hexWidthPx = hexSizePx * kotlin.math.sqrt(3.0).toFloat()
            val hexHeightPx = hexSizePx * 2f
            val colSpacingPx = HexagonalGridConstants.HORIZONTAL_SPACING
            val rowSpacingPx = -hexHeightPx + hexHeightPx * 0.75f + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT
            val oddOffsetPx = hexWidthPx * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO
            val col = scrollToPosition.x
            val row = scrollToPosition.y
            val oddRowOffset = if (row % 2 == 1) oddOffsetPx else 0f
            val cellCenterX = col * (hexWidthPx + colSpacingPx) + hexWidthPx / 2f + oddRowOffset
            val cellCenterY = row * (hexHeightPx + rowSpacingPx) + hexHeightPx / 2f
            // Clamp offset so the cell is centered in the viewport
            val maxOffsetX = maxOf(0f, (contentSize.width * scale - containerSize.width) / 2f)
            val maxOffsetY = maxOf(0f, (contentSize.height * scale - containerSize.height) / 2f)
            val targetOffsetX = (contentSize.width * scale / 2f - cellCenterX * scale).coerceIn(-maxOffsetX, maxOffsetX)
            val targetOffsetY = (contentSize.height * scale / 2f - cellCenterY * scale).coerceIn(-maxOffsetY, maxOffsetY)
            offsetX = targetOffsetX
            offsetY = targetOffsetY
            onScrollToPositionConsumed?.invoke()
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
                        if (LogConfig.ENABLE_UI_LOGGING) {
                        println("Target circle map: $result")
                        }
                        result
                    }
                }
            }
        }
    }

    // Calculate spell area circle preview for ATTACK_AREA, ATTACK_AIMED, FEAR_SPELL, FEAR_SPELL_AREA, BOMB in targeting mode
    val spellAreaTargeting = gameState.spellTargeting.value
    val currentHoveredPosition = hoveredPosition
    val spellAreaCircleMap = remember(currentHoveredPosition, spellAreaTargeting?.activeSpell) {
        val activeSpell = spellAreaTargeting?.activeSpell
        // All spell targeting previews use the same magic (purple) color to distinguish them from tower attacks
        val spellColor = TargetCircleConstants.ATTACK_AREA_SPELL_COLOR
        // Bomb uses a distinct orange/red color to represent fire/explosion
        val bombColor = TargetCircleConstants.BOMB_SPELL_COLOR
        val bombExplosionRange = TargetCircleConstants.BOMB_SPELL_RADIUS
        when {
            (activeSpell == SpellType.ATTACK_AREA || activeSpell == SpellType.ATTACK_AIMED) && currentHoveredPosition != null -> {
                val result = mutableMapOf<Position, TargetCircleInfo>()
                result[currentHoveredPosition] = TargetCircleInfo.CentralTarget(
                    color = spellColor,
                    attackType = AttackType.AREA,
                    isExtendedArea = true
                )
                if (activeSpell == SpellType.ATTACK_AREA) {
                    val allNeighbors = currentHoveredPosition.getHexNeighborsWithinRadius(
                        TargetCircleConstants.ATTACK_AREA_SPELL_RADIUS,
                        gameState.level.gridWidth,
                        gameState.level.gridHeight
                    ).filter { neighbor ->
                        gameState.level.isOnPath(neighbor) ||
                        gameState.isBridgeAt(neighbor) ||
                        gameState.attackers.any { it.position.value == neighbor && !it.isDefeated.value }
                    }
                    for (neighbor in allNeighbors) {
                        val distance = currentHoveredPosition.hexDistanceTo(neighbor)
                        result[neighbor] = TargetCircleInfo.NeighborTarget(
                            color = spellColor,
                            attackType = AttackType.AREA,
                            centerPosition = currentHoveredPosition,
                            thisPosition = neighbor,
                            distanceFromCenter = distance,
                            isExtendedArea = true
                        )
                    }
                }
                result
            }
            activeSpell == SpellType.BOMB && currentHoveredPosition != null -> {
                // Show explosion range (3 hex tiles) around hovered position in orange
                val result = mutableMapOf<Position, TargetCircleInfo>()
                result[currentHoveredPosition] = TargetCircleInfo.CentralTarget(
                    color = bombColor,
                    attackType = AttackType.AREA,
                    isExtendedArea = true
                )
                val allNeighbors = currentHoveredPosition.getHexNeighborsWithinRadius(
                    bombExplosionRange,
                    gameState.level.gridWidth,
                    gameState.level.gridHeight
                )
                for (neighbor in allNeighbors) {
                    val distance = currentHoveredPosition.hexDistanceTo(neighbor)
                    result[neighbor] = TargetCircleInfo.NeighborTarget(
                        color = bombColor,
                        attackType = AttackType.AREA,
                        centerPosition = currentHoveredPosition,
                        thisPosition = neighbor,
                        distanceFromCenter = distance,
                        isExtendedArea = true
                    )
                }
                result
            }
            activeSpell == SpellType.FEAR_SPELL_AREA && currentHoveredPosition != null -> {
                // Area circles in magic color at radius 2 (like ATTACK_AREA)
                val result = mutableMapOf<Position, TargetCircleInfo>()
                result[currentHoveredPosition] = TargetCircleInfo.CentralTarget(
                    color = spellColor,
                    attackType = AttackType.AREA,
                    isExtendedArea = true
                )
                val allNeighbors = currentHoveredPosition.getHexNeighborsWithinRadius(
                    TargetCircleConstants.ATTACK_AREA_SPELL_RADIUS,
                    gameState.level.gridWidth,
                    gameState.level.gridHeight
                ).filter { neighbor ->
                    gameState.level.isOnPath(neighbor) ||
                    gameState.isBridgeAt(neighbor) ||
                    gameState.attackers.any { it.position.value == neighbor && !it.isDefeated.value }
                }
                for (neighbor in allNeighbors) {
                    val distance = currentHoveredPosition.hexDistanceTo(neighbor)
                    result[neighbor] = TargetCircleInfo.NeighborTarget(
                        color = spellColor,
                        attackType = AttackType.AREA,
                        centerPosition = currentHoveredPosition,
                        thisPosition = neighbor,
                        distanceFromCenter = distance,
                        isExtendedArea = true
                    )
                }
                result
            }
            activeSpell == SpellType.FEAR_SPELL && currentHoveredPosition != null -> {
                // Single-target circles on hovered enemy tile in magic color (like tower attack)
                val enemyAtHover = gameState.attackers.find {
                    it.position.value == currentHoveredPosition && !it.isDefeated.value
                }
                if (enemyAtHover != null) {
                    mapOf(currentHoveredPosition to TargetCircleInfo.CentralTarget(
                        color = spellColor,
                        attackType = AttackType.RANGED,
                        isExtendedArea = false
                    ))
                } else {
                    emptyMap()
                }
            }
            else -> emptyMap()
        }
    }

    // Calculate range circles for already-placed bombs (show explosion range, but no center rings)
    val activeBombEffects = gameState.activeSpellEffects.filter { it.spell == SpellType.BOMB && it.position != null }
    val placedBombCircleMap = remember(activeBombEffects.map { it.position }) {
        val bombColor = TargetCircleConstants.BOMB_SPELL_COLOR
        val bombExplosionRange = TargetCircleConstants.BOMB_SPELL_RADIUS
        val result = mutableMapOf<Position, TargetCircleInfo>()
        for (effect in activeBombEffects) {
            val bombPos = effect.position ?: continue
            // Intentionally skip the bomb tile itself (no center rings on placed bombs)
            val allNeighbors = bombPos.getHexNeighborsWithinRadius(
                bombExplosionRange,
                gameState.level.gridWidth,
                gameState.level.gridHeight
            )
            for (neighbor in allNeighbors) {
                if (!result.containsKey(neighbor)) {
                    val distance = bombPos.hexDistanceTo(neighbor)
                    result[neighbor] = TargetCircleInfo.NeighborTarget(
                        color = bombColor,
                        attackType = AttackType.AREA,
                        centerPosition = bombPos,
                        thisPosition = neighbor,
                        distanceFromCenter = distance,
                        isExtendedArea = true
                    )
                }
            }
        }
        result
    }

    val mapId = gameState.level.mapId
    val mapImageState = rememberMapImageState(mapId)
    val mapImagePainter = mapImageState.painter
    val useLevelMapImage = AppSettings.useLevelMapImage.value
    val hasMapImage = mapImagePainter != null && useLevelMapImage
    val isLoadingMapImage = mapImageState.isLoading
    val hexMapSizePx = remember(gameState.level.gridWidth, gameState.level.gridHeight, hexSize) {
        val hexSizePx = hexSize.value
        val hexWidthPx = hexSizePx * sqrt(3.0).toFloat()
        val hexHeightPx = hexSizePx * 2f
        val verticalSpacingPx = hexHeightPx * 0.75f
        val rowSpacingPx = -hexHeightPx + verticalSpacingPx + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT
        val oddOffsetPx = hexWidthPx * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO
        val colSpacingPx = HexagonalGridConstants.HORIZONTAL_SPACING

        val maxOddOffset = if (gameState.level.gridHeight > 1) oddOffsetPx else 0f
        val widthPx = (gameState.level.gridWidth * hexWidthPx) + ((gameState.level.gridWidth - 1) * colSpacingPx) + maxOddOffset
        val heightPx = ((gameState.level.gridHeight - 1) * (hexHeightPx + rowSpacingPx)) + hexHeightPx

        widthPx.roundToInt() to heightPx.roundToInt()
    }

    Box(modifier = modifier
        .onSizeChanged { containerSize = it }
    ) {
        if (useLevelMapImage && isLoadingMapImage) {
            LevelLoadingScreen(modifier = Modifier.fillMaxSize())
        } else {
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
            modifier = Modifier.fillMaxSize(),
            backgroundContent = if (hasMapImage) { { measuredContentSize ->
                val density = androidx.compose.ui.platform.LocalDensity.current
                val targetWidthPx = maxOf(hexMapSizePx.first, measuredContentSize.width)
                val targetHeightPx = maxOf(hexMapSizePx.second, measuredContentSize.height)
                with(density) {
                    androidx.compose.foundation.Image(
                        painter = mapImagePainter,
                        contentDescription = null,
                        modifier = Modifier
                            .requiredWidth(targetWidthPx.toDp())
                            .requiredHeight(targetHeightPx.toDp()),
                        contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
                    )
                }
            } } else null
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
                selectedBarricadeAction = selectedBarricadeAction,
                targetCircleInfo = spellAreaCircleMap[position] ?: targetCircleMap[position] ?: placedBombCircleMap[position],
                onClick = { onCellClick(position) },
                hexSize = hexSize,
                selectedDefenderType = selectedDefenderType,
                hoveredPosition = hoveredPosition,
                onHoverChange = { isHovering ->
                    hoveredPosition = if (isHovering) position else null
                },
                useTransparentBackground = hasMapImage
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
            val density = androidx.compose.ui.platform.LocalDensity.current
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (AppSettings.showMapSizeOverlay.value && contentSize.width > 0 && contentSize.height > 0) {
                    val contentWidthPx = contentSize.width
                    val contentHeightPx = contentSize.height
                    val realWidthPx = hexMapSizePx.first
                    val realHeightPx = hexMapSizePx.second
                    val viewportWidthPx = containerSize.width
                    val viewportHeightPx = containerSize.height
                    val contentWidthDp = with(density) { contentWidthPx.toDp() }
                    val contentHeightDp = with(density) { contentHeightPx.toDp() }
                    val realWidthDp = with(density) { realWidthPx.toDp() }
                    val realHeightDp = with(density) { realHeightPx.toDp() }
                    val viewportWidthDp = with(density) { viewportWidthPx.toDp() }
                    val viewportHeightDp = with(density) { viewportHeightPx.toDp() }
                    Surface(
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp,
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = stringResource(
                                Res.string.debug_map_size_overlay,
                                realWidthDp.value.roundToInt(),
                                realHeightDp.value.roundToInt(),
                                realWidthPx,
                                realHeightPx,
                                contentWidthDp.value.roundToInt(),
                                contentHeightDp.value.roundToInt(),
                                contentWidthPx,
                                contentHeightPx,
                                viewportWidthDp.value.roundToInt(),
                                viewportHeightDp.value.roundToInt(),
                                viewportWidthPx,
                                viewportHeightPx,
                                offsetX.roundToInt(),
                                offsetY.roundToInt()
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

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
                        modifier = Modifier.fillMaxSize(),
                        onViewportDrag = { newOffsetX, newOffsetY ->
                            offsetX = newOffsetX
                            offsetY = newOffsetY
                        }
                    )
                }
            }
        }
        } // end else !isLoadingMapImage
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
    selectedBarricadeAction: BarricadeAction? = null,
    targetCircleInfo: TargetCircleInfo?,
    onClick: () -> Unit,
    hexSize: androidx.compose.ui.unit.Dp = 48.dp,
    selectedDefenderType: DefenderType? = null,
    hoveredPosition: Position? = null,
    onHoverChange: ((Boolean) -> Unit)? = null,
    useTransparentBackground: Boolean = false
) {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value
    
    val isSpawnPoint = gameState.level.isSpawnPoint(position)
    val isTarget = gameState.level.isTargetPosition(position)
    val isOnPath = gameState.level.isOnPath(position)
    val isBuildArea = gameState.level.isBuildArea(position)
    val isRiverTile = gameState.level.isRiverTile(position)
    val defender = gameState.defenders.find { it.position.value == position }
    val attacker = gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
    
    // Check for healing effects at this position
    val healingEffect = gameState.healingEffects.find { it.position == position }
    
    // Check for damage effects at this position
    val damageEffect = gameState.damageEffects.find { it.position == position }
    
    // Determine the tile type for background image loading
    val riverTile = gameState.level.getRiverTile(position)
    val isMaelstrom = riverTile?.flowDirection == RiverFlow.MAELSTROM
    
    val tileType = when {
        isSpawnPoint -> de.egril.defender.editor.TileType.SPAWN_POINT
        isTarget -> de.egril.defender.editor.TileType.TARGET
        isRiverTile -> de.egril.defender.editor.TileType.RIVER
        isOnPath -> de.egril.defender.editor.TileType.PATH
        isBuildArea -> de.egril.defender.editor.TileType.BUILD_AREA
        else -> de.egril.defender.editor.TileType.NO_PLAY
    }
    
    // Get tile background painter (will be null if images are disabled or not available)
    // For ready towers on build areas or islands, don't show tile background to make towers more visible
    val shouldShowTileImage = !(defender != null && defender.isReady && isBuildArea)
    val tilePainter = if (shouldShowTileImage && (!useTransparentBackground || isMaelstrom)) {
        TileImageProvider.getTilePainter(tileType, isMaelstrom = isMaelstrom)
    } else {
        null
    }

    // Check for field effects at this position
    val fieldEffect = gameState.fieldEffects.find { it.position == position }

    // Check for traps at this position
    val trap = gameState.traps.find { it.position == position }
    
    // Check for barricades at this position
    val barricade = gameState.barricades.find { it.position == position }

    // Check if this tile is in a cooling spell area (show snowflake on affected path tiles)
    val isInCoolingArea = (isOnPath || isSpawnPoint) && gameState.activeSpellEffects.any { effect ->
        effect.spell == SpellType.COOLING_SPELL &&
        effect.position != null &&
        position.hexDistanceTo(effect.position) <= 2
    }

    // Check for active bomb spell effect at this position
    val bombEffect = gameState.activeSpellEffects.find {
        it.spell == SpellType.BOMB && it.position == position
    }

    // Check for bomb explosion visual effect at this position
    val bombExplosion = gameState.bombExplosionEffects.find { explosion ->
        explosion.center == position || explosion.affectedPositions.contains(position)
    }

    // Check if this tile is a valid spell target
    val spellTargeting = gameState.spellTargeting.value
    val isValidSpellTarget = if (spellTargeting != null) {
        when (spellTargeting.activeSpell.targetType) {
            de.egril.defender.model.SpellTargetType.ENEMY -> {
                val enemyHere = gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                enemyHere != null && spellTargeting.validTargets.contains(enemyHere)
            }
            de.egril.defender.model.SpellTargetType.TOWER -> {
                val towerHere = gameState.defenders.find { it.position.value == position }
                towerHere != null && spellTargeting.validTargets.contains(towerHere)
            }
            de.egril.defender.model.SpellTargetType.POSITION -> {
                spellTargeting.validTargets.contains(position)
            }
            else -> false
        }
    } else false
    val selectedDefenderForRange = selectedDefenderId?.let { id -> gameState.defenders.find { it.id == id } }
    val hasDoubleReachBuff = selectedDefenderForRange?.let { sel ->
        gameState.activeSpellEffects.any { it.spell == SpellType.DOUBLE_TOWER_REACH && it.defenderId == sel.id }
    } ?: false
    val cellIsInRange = selectedDefenderForRange?.let { sel ->
        if (sel.position.value == position) {
            false  // Don't highlight the defender's own cell
        } else {
            val distance = sel.position.value.distanceTo(position)
            val effectiveRange = if (hasDoubleReachBuff) sel.range * 2 else sel.range
            distance >= sel.type.minRange && distance <= effectiveRange
        }
    } ?: false
    // Tiles that are in range ONLY because of the double-reach spell (beyond normal range)
    val cellIsInDoubleReachOnlyRange = if (hasDoubleReachBuff) {
        val sel = selectedDefenderForRange!!
        if (sel.position.value == position) false
        else {
            val distance = sel.position.value.distanceTo(position)
            distance >= sel.type.minRange && distance > sel.range && distance <= sel.range * 2
        }
    } else false
    
    // Calculate hover preview for tower placement
    val isHoveringForPreview = hoveredPosition == position && selectedDefenderType != null
    // Include river tiles as buildable (for rafts)
    val isBuildableTile = (isBuildArea || isRiverTile) && defender == null && attacker == null
    val showPlacementPreview = isHoveringForPreview && isBuildableTile
    
    // Calculate hover preview for trap placement
    val isHoveringForTrapPreview = hoveredPosition == position
    val isTrapPlacementMode = selectedMineAction == MineAction.BUILD_TRAP || selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP
    
    // Check if this tile is valid for trap placement (on path, in range, no enemy, no existing trap, no field effects)
    val isValidTrapPlacement = if (isTrapPlacementMode && isHoveringForTrapPreview && selectedDefenderId != null) {
        val selectedDefender = gameState.defenders.find { it.id == selectedDefenderId }
        selectedDefender?.let { sel ->
            val distance = sel.position.value.distanceTo(position)
            val hasEnemy = attacker != null
            val hasTrap = trap != null
            val hasFieldEffect = fieldEffect != null
            isOnPath && distance <= sel.range && !hasEnemy && !hasTrap && !hasFieldEffect
        } ?: false
    } else {
        false
    }
    
    val showTrapPreview = isValidTrapPlacement
    
    // Check if hovered position is buildable (needed for range preview calculation)
    // Include river tiles as buildable (for rafts)
    val hoveredPositionIsBuildable = if (hoveredPosition != null && selectedDefenderType != null) {
        val hoveredIsBuildArea = gameState.level.isBuildArea(hoveredPosition)
        val hoveredIsRiver = gameState.level.isRiverTile(hoveredPosition)
        val hoveredHasDefender = gameState.defenders.any { it.position.value == hoveredPosition }
        val hoveredHasAttacker = gameState.attackers.any { it.position.value == hoveredPosition && !it.isDefeated.value }
        (hoveredIsBuildArea || hoveredIsRiver) && !hoveredHasDefender && !hoveredHasAttacker
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
        // For preview, use the actual range at level 1 (baseRange, capped by maxRange if set)
        val maxRange = selectedDefenderType.maxRange?.let { minOf(selectedDefenderType.baseRange, it) } ?: selectedDefenderType.baseRange
        
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
    
    // Barricade placement range detection (3 tiles, yellow borders for empty path tiles)
    val isBarricadePlacement = selectedBarricadeAction == BarricadeAction.BUILD_BARRICADE
    val cellIsInBarricadeRange = if (isBarricadePlacement && selectedDefenderId != null) {
        val selectedDefender = gameState.defenders.find { it.id == selectedDefenderId }
        selectedDefender?.let { sel ->
            // Check if within 3 tiles range
            val distance = sel.position.value.distanceTo(position)
            val isInRange = distance > 0 && distance <= 3
            // Check if empty path tile (no defender, no attacker, can have existing barricade for reinforcement)
            val isEmptyPath = isOnPath && defender == null && attacker == null
            isInRange && isEmptyPath
        } ?: false
    } else {
        false
    }
    
    // Show barricade preview when hovering over valid barricade placement tile
    val showBarricadePreview = isBarricadePlacement && hoveredPosition == position && cellIsInBarricadeRange
    
    // Mine trap placement range detection (path tiles within range of selected mine)
    val isMineTrapPlacement = selectedMineAction == MineAction.BUILD_TRAP
    val cellIsValidForMineTrapPlacement = if (isMineTrapPlacement && selectedDefenderId != null) {
        val selectedDefender = gameState.defenders.find { it.id == selectedDefenderId }
        selectedDefender?.let { sel ->
            val distance = sel.position.value.distanceTo(position)
            val isInRange = distance > 0 && distance <= sel.range
            val isEmptyPath = isOnPath && attacker == null && trap == null && fieldEffect == null
            isInRange && isEmptyPath
        } ?: false
    } else {
        false
    }

    // Magical trap placement range detection (path tiles within range of selected wizard)
    val isMagicalTrapPlacement = selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP
    val cellIsValidForMagicalTrapPlacement = if (isMagicalTrapPlacement && selectedDefenderId != null) {
        val selectedDefender = gameState.defenders.find { it.id == selectedDefenderId }
        selectedDefender?.let { sel ->
            val distance = sel.position.value.distanceTo(position)
            val isInRange = distance > 0 && distance <= sel.range
            val isEmptyPath = isOnPath && attacker == null && trap == null && fieldEffect == null
            isInRange && isEmptyPath
        } ?: false
    } else {
        false
    }

    // Check if this tile should be highlighted as buildable when a tower type is selected
    val isBuildableAndEmpty = selectedDefenderType != null && 
                              isBuildableTile && 
                              !showPlacementPreview  // Don't double-highlight the hovered tile
    
    // Check if this tile has a barricade that can be used as tower base (HP >= 100)
    val canBeUsedAsTowerBase = selectedDefenderType != null && 
                               barricade != null && 
                               barricade.canSupportTower() && 
                               !barricade.hasTower() &&
                               !showPlacementPreview  // Don't double-highlight the hovered tile

    // Base background color based on area type - ALWAYS visible
    // Build areas adjacent to path allow tower placement
    val baseBackgroundColor = when {
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
        
        barricade != null -> Color(0xFF795548).copy(alpha = 0.5f)  // Brown tint for barricade
        
        // Bomb explosion overlay - bright orange/red when explosion is happening
        bombExplosion != null -> Color(0xFFFF3D00).copy(alpha = 0.7f)  // Bright red-orange for explosion

        // Active bomb on tile - dark red/amber tint with countdown
        bombEffect != null -> Color(0xFFFF6F00).copy(alpha = 0.4f)  // Amber tint for bomb

        // Barricade placement range - yellow tint for tiles in range
        cellIsInBarricadeRange -> GamePlayColors.Yellow.copy(alpha = 0.3f)  // Light yellow for barricade placement range
        
        // Tower placement preview - highlight the hovered build tile differently than range tiles
        showPlacementPreview -> GamePlayColors.Yellow.copy(alpha = 0.4f)  // Light yellow for the build tile being hovered
        isInPreviewRange -> GamePlayColors.Success.copy(alpha = 0.2f)  // Very light green for range preview tiles
        
        // Spell targeting highlight - purple tint for valid spell target position tiles
        // Not shown for fear spells (target circles provide the visual indicator)
        isValidSpellTarget &&
            spellTargeting?.activeSpell != SpellType.FEAR_SPELL &&
            spellTargeting?.activeSpell != SpellType.FEAR_SPELL_AREA -> Color(0xFF9C27B0).copy(alpha = 0.25f)  // Light purple for valid spell target positions

        isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.7f)
        isTargetSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.8f)
        else -> baseBackgroundColor  // No selection highlighting during placement or in initial phase
    }

    val finalBackgroundColor = if (useTransparentBackground && attacker == null && defender == null && fieldEffect == null && trap == null && barricade == null && bombEffect == null && bombExplosion == null) {
        Color.Transparent
    } else {
        backgroundColor
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
        
        // Barricade and trap placement range - brown borders (light brown diagonal stripes)
        cellIsInBarricadeRange || cellIsValidForMineTrapPlacement -> GamePlayColors.TrapPlacementHighlight  // Brown border for barricade/trap placement range

        // Magical trap placement range - lilac borders
        cellIsValidForMagicalTrapPlacement -> GamePlayColors.MagicalTrapPlacementHighlight  // Lilac border for magical trap placement range
        
        // Buildable tile highlighting - lighter green borders with dashed line when tower type is selected
        isBuildableAndEmpty || canBeUsedAsTowerBase -> GamePlayColors.BuildableHighlight  // Lighter green border for buildable tiles and tower bases
        
        // Double-reach-only tiles: thin purple solid border
        cellIsInDoubleReachOnlyRange && isValidTargetTile && showRange && canPlaceTrapHere -> SpellDoubleReachColor
        
        cellIsInRange && isValidTargetTile && showRange && canPlaceTrapHere -> GamePlayColors.Success  // Green border for tiles in range (path or river for area attacks)
        isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> GamePlayColors.Yellow  // Yellow border for selected defender (not during initial building)

        // Spell targeting highlight - purple border for valid spell targets (enemies, towers, positions)
        // Not shown for fear spells (target circles provide the visual indicator)
        isValidSpellTarget &&
            spellTargeting?.activeSpell != SpellType.FEAR_SPELL &&
            spellTargeting?.activeSpell != SpellType.FEAR_SPELL_AREA -> Color(0xFF9C27B0)  // Purple border for valid spell targets

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
        barricade != null -> Color(0xFF795548)  // Brown border for barricade
        else -> Color.Transparent  // No borders for empty cells
    }

    // Thicker borders for important elements
    val borderWidth = when {
        showPlacementPreview -> 6.dp  // Double thickness for hovered build tile
        isInPreviewRange -> 3.dp  // Medium border for range preview
        cellIsInBarricadeRange || cellIsValidForMineTrapPlacement || cellIsValidForMagicalTrapPlacement -> 3.dp  // Medium border for trap/barricade placement range
        isBuildableAndEmpty || canBeUsedAsTowerBase -> 3.dp  // Medium border for buildable tiles and tower bases
        isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> 5.dp  // Extra thick border for selected defender (not during initial building)
        cellIsInDoubleReachOnlyRange && isValidTargetTile && showRange && canPlaceTrapHere -> 2.dp  // Thin purple border for double-reach-only tiles
        cellIsInRange && isValidTargetTile && showRange && canPlaceTrapHere -> 4.dp  // Thick border for cells in range (path or river for area attacks)
        isValidSpellTarget &&
            spellTargeting?.activeSpell != SpellType.FEAR_SPELL &&
            spellTargeting?.activeSpell != SpellType.FEAR_SPELL_AREA -> 4.dp  // Thick purple border for valid spell targets
        isSpawnPoint || isTarget -> 3.dp
        attacker != null || defender != null -> 3.dp
        fieldEffect != null -> 3.dp  // Thick border for field effects
        trap != null -> 3.dp  // Thick border for trap
        barricade != null -> 3.dp  // Thick border for barricade
        else -> 0.dp  // No border for empty cells
    }
    
    // Flag to indicate dashed border (for preview and buildable tiles)
    val useDashedBorder = showPlacementPreview || isInPreviewRange || isBuildableAndEmpty || canBeUsedAsTowerBase ||
                          cellIsInBarricadeRange || cellIsValidForMineTrapPlacement || cellIsValidForMagicalTrapPlacement

    val showDiagonalStripes = isBuildableAndEmpty || canBeUsedAsTowerBase ||
                              cellIsInBarricadeRange || cellIsValidForMineTrapPlacement || cellIsValidForMagicalTrapPlacement
    
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
        // Check if there's a ready defender on this tile (build area)
        val neighborDefender = gameState.defenders.find { it.position.value == pos }
        val neighborIsReady = neighborDefender?.isReady == true
        val neighborIsBuildArea = gameState.level.isBuildArea(pos)
        val shouldShowNeighborTile = !(neighborDefender != null && neighborIsReady && neighborIsBuildArea)
        
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
            backgroundColor = finalBackgroundColor,
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
                damageEffect = damageEffect,
                defender = defender,
                fieldEffect = fieldEffect,
                trap = trap,
                barricade = barricade,
                isSpawnPoint = isSpawnPoint,
                isTarget = isTarget,
                isRiverTile = isRiverTile,
                showPlacementPreview = showPlacementPreview,
                showBarricadePreview = showBarricadePreview,
                selectedDefenderType = selectedDefenderType,
                targetCircleInfo = targetCircleInfo,
                useDashedBorder = useDashedBorder,
                borderColor = borderColor,
                borderWidth = borderWidth,
                hexSize = hexSize,
                showTrapPreview = showTrapPreview,
                selectedMineAction = selectedMineAction,
                selectedWizardAction = selectedWizardAction,
                isBuildableAndEmpty = isBuildableAndEmpty,
                canBeUsedAsTowerBase = canBeUsedAsTowerBase,
                showDiagonalStripes = showDiagonalStripes,
                isInCoolingArea = isInCoolingArea,
                bombEffect = bombEffect,
                bombExplosion = bombExplosion
            )
        }
    } else {
        BaseGridCell(
            hexSize = hexSize,
            backgroundColor = finalBackgroundColor,
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
                damageEffect = damageEffect,
                defender = defender,
                fieldEffect = fieldEffect,
                trap = trap,
                barricade = barricade,
                isSpawnPoint = isSpawnPoint,
                isTarget = isTarget,
                isRiverTile = isRiverTile,
                showPlacementPreview = showPlacementPreview,
                showBarricadePreview = showBarricadePreview,
                selectedDefenderType = selectedDefenderType,
                targetCircleInfo = targetCircleInfo,
                useDashedBorder = useDashedBorder,
                borderColor = borderColor,
                borderWidth = borderWidth,
                hexSize = hexSize,
                showTrapPreview = showTrapPreview,
                selectedMineAction = selectedMineAction,
                selectedWizardAction = selectedWizardAction,
                isBuildableAndEmpty = isBuildableAndEmpty,
                canBeUsedAsTowerBase = canBeUsedAsTowerBase,
                showDiagonalStripes = showDiagonalStripes,
                isInCoolingArea = isInCoolingArea,
                bombEffect = bombEffect,
                bombExplosion = bombExplosion
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
    damageEffect: DamageEffect?,
    defender: Defender?,
    fieldEffect: FieldEffect?,
    trap: Trap?,
    barricade: Barricade?,
    isSpawnPoint: Boolean,
    isTarget: Boolean,
    isRiverTile: Boolean,
    showPlacementPreview: Boolean,
    showBarricadePreview: Boolean,
    selectedDefenderType: DefenderType?,
    targetCircleInfo: TargetCircleInfo?,
    useDashedBorder: Boolean,
    borderColor: Color,
    borderWidth: Dp,
    hexSize: Dp,
    showTrapPreview: Boolean = false,
    selectedMineAction: MineAction? = null,
    selectedWizardAction: WizardAction? = null,
    isBuildableAndEmpty: Boolean = false,
    canBeUsedAsTowerBase: Boolean = false,
    showDiagonalStripes: Boolean = false,
    isInCoolingArea: Boolean = false,
    bombEffect: ActiveSpellEffect? = null,
    bombExplosion: BombExplosionEffect? = null
) {
        when {
            attacker != null -> {
                // Use graphical icon for enemy units
                // Key by id, position, level, currentHealth, and movementPenalty to force recomposition when any changes
                key(
                    attacker.id,
                    attacker.position.value.x,
                    attacker.position.value.y,
                    attacker.level,
                    attacker.currentHealth.value,
                    attacker.movementPenalty.value
                ) {
                    // Detect freeze effect before Box so it can be used in modifier for outline
                    val freezeEffect = gameState.activeSpellEffects.find {
                        it.spell == SpellType.FREEZE_SPELL && it.attackerId == attacker.id
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = if (freezeEffect != null)
                            Modifier.border(2.dp, Color.Cyan, RoundedCornerShape(4.dp))
                        else
                            Modifier
                    ) {
                        EnemyIcon(attacker = attacker)
                        // Show healing effect overlay if present
                        if (healingEffect != null) {
                            GreenWitchHealingAnimation(
                                animate = AppSettings.enableAnimations.value,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Show freeze effect overlay
                        if (freezeEffect != null) {
                            FreezeSpellAnimation(
                                animate = AppSettings.enableAnimations.value,
                                modifier = Modifier.fillMaxSize(),
                                animationKey = freezeEffect.attackerId
                            )
                        }
                        // Show fear effect overlay (black scribble cloud at top of icon)
                        val fearEffect = gameState.activeSpellEffects.find { effect ->
                            (effect.spell == SpellType.FEAR_SPELL && effect.attackerId == attacker.id) ||
                            (effect.spell == SpellType.FEAR_SPELL_AREA && effect.position != null &&
                                attacker.position.value.hexDistanceTo(effect.position) <= 2)
                        }
                        if (fearEffect != null) {
                            FearSpellAnimation(
                                animate = AppSettings.enableAnimations.value,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Show barb effect indicators if affected (show up to 5 arrows in center)
                        if (attacker.movementPenalty.value > 0) {
                            val barbCount = minOf(attacker.movementPenalty.value, 5)
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(barbCount) {
                                        Box(
                                            modifier = Modifier.graphicsLayer {
                                                rotationZ = 10f  // Tilt +10 degrees
                                            }
                                        ) {
                                            de.egril.defender.ui.icon.DownArrowIcon(
                                                size = 12.dp,
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            defender != null -> {
                // Use graphical icon for towers
                // Key by id, position, level and actionsRemaining to force recomposition when these change
                val doubleLevelActive = gameState.activeSpellEffects.any {
                    it.spell == SpellType.DOUBLE_TOWER_LEVEL && it.defenderId == defender.id
                }
                key(
                    defender.id,
                    defender.position.value.x,
                    defender.position.value.y,
                    defender.level.value,
                    defender.actionsRemaining.value,
                    defender.buildTimeRemaining.value,
                    defender.isDisabled.value,
                    defender.disabledTurnsRemaining.value,
                    doubleLevelActive
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        TowerIcon(defender = defender, gameState = gameState)
                        // Show Double Tower Level spell animation overlay (same animation as instant tower)
                        if (doubleLevelActive) {
                            InstantTowerSpellAnimation(
                                animate = AppSettings.enableAnimations.value,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Show doubled level as purple text on the tile
                        if (doubleLevelActive) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    "${defender.level.value * 2}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SpellDoubleLevelColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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
                            PentagramIcon(size = GamePlayConstants.TileIconSizes.Trap)
                        }

                        TrapType.DWARVEN -> {
                            // Dwarven trap - show trap icon with damage
                            TrapIcon(size = GamePlayConstants.TileIconSizes.Trap)
                            Text(
                                "-${trap.damage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.offset(y = (-6).dp)
                            )
                        }
                    }
                }
            }
            
            barricade != null -> {
                // Show barricade with HP
                Box(contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Show wood/barricade symbol or gate icon with brown color
                        if (barricade.isGate) {
                            GateIcon(size = GamePlayConstants.TileIconSizes.Barricade)
                        } else {
                            WoodIcon(size = GamePlayConstants.TileIconSizes.Barricade)
                        }
                        // Show health points - moved up for better visibility
                        Text(
                            "${barricade.healthPoints.value} HP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF795548),  // Brown color
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.offset(y = (-12).dp)
                        )
                    }
                    // Show damage effect overlay if present
                    if (damageEffect != null) {
                        BarricadeDamageAnimation(
                            animate = AppSettings.enableAnimations.value,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            
            showBarricadePreview -> {
                // Show see-through barricade preview when hovering
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer(alpha = 0.5f)  // Semi-transparent
                ) {
                    // Show wood/barricade symbol with brown color
                    WoodIcon(size = GamePlayConstants.TileIconSizes.Barricade)
                    // Show "NEW" text for new barricade preview
                    Text(
                        stringResource(Res.string.barricade),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF795548),  // Brown color
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            bombEffect != null -> {
                // Show bomb icon with countdown number overlaid prominently
                Box(contentAlignment = Alignment.Center) {
                    BombIcon(size = 36.dp)
                    // Countdown badge in bottom-right corner of icon
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .background(
                                color = Color(0xFFCC0000),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${bombEffect.turnsRemaining}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = MaterialTheme.typography.labelMedium.fontSize
                        )
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

        // Show cooling spell snowflake animation on affected tiles
        if (isInCoolingArea) {
            CoolingAreaAnimation(
                animate = AppSettings.enableAnimations.value,
                modifier = Modifier.fillMaxSize()
            )
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

        // Show half-transparent trap icon on hovered path tile (when in trap placement mode)
        if (showTrapPreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.5f),  // 50% transparency
                contentAlignment = Alignment.Center
            ) {
                // Show different icon based on trap type
                when {
                    selectedMineAction == MineAction.BUILD_TRAP -> {
                        // Dwarven trap - show trap icon
                        TrapIcon(size = GamePlayConstants.TileIconSizes.TrapPreview)
                    }
                    selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP -> {
                        // Magical trap - show pentagram icon
                        PentagramIcon(size = GamePlayConstants.TileIconSizes.TrapPreview)
                    }
                }
            }
        }

        // Show bomb explosion animation overlay on affected tiles (highest priority, above everything)
        if (bombExplosion != null) {
            BombExplosionAnimation(
                animate = AppSettings.enableAnimations.value,
                modifier = Modifier.fillMaxSize().zIndex(20f)
            )
        }

        // Show small bomb countdown overlay when an enemy is on the same bomb tile
        if (bombEffect != null && attacker != null) {
            Box(
                modifier = Modifier.fillMaxSize().zIndex(15f),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .background(
                            color = Color(0xFFCC0000),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .padding(horizontal = 3.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${bombEffect.turnsRemaining}",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
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

        // Draw diagonal stripes for buildable tiles, tower bases, and placement tiles (trap/barricade/magical trap)
        if (showDiagonalStripes) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(11f)  // Below the dashed border
            ) {
                val sqrt3 = sqrt(3.0).toFloat()
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val radius = minOf(size.width, size.height) / 2f

                // Create hexagon clip path
                val hexPath = Path().apply {
                    moveTo(centerX, centerY - radius)
                    lineTo(centerX + radius * sqrt3 / 2f, centerY - radius / 2f)
                    lineTo(centerX + radius * sqrt3 / 2f, centerY + radius / 2f)
                    lineTo(centerX, centerY + radius)
                    lineTo(centerX - radius * sqrt3 / 2f, centerY + radius / 2f)
                    lineTo(centerX - radius * sqrt3 / 2f, centerY - radius / 2f)
                    close()
                }
                
                // Draw diagonal stripes with clipping
                drawContext.canvas.save()
                drawContext.canvas.clipPath(hexPath)
                
                // Draw diagonal stripes
                val stripeWidth = 8f
                val stripeSpacing = 16f
                val totalSpacing = stripeWidth + stripeSpacing
                val diagonalLength = size.width + size.height
                
                // Start from top-right, go to bottom-left (90 degree rotation)
                var offset = -diagonalLength
                while (offset < diagonalLength) {
                    drawLine(
                        color = borderColor.copy(alpha = 0.8f),  // 80% opacity
                        start = Offset(size.width - offset, 0f),
                        end = Offset(size.width - offset - size.height, size.height),
                        strokeWidth = stripeWidth
                    )
                    offset += totalSpacing
                }
                
                drawContext.canvas.restore()
            }
        }

        // Debug overlay: tile borders by type
        if (AppSettings.showTileBorders.value) {
            val debugBorderColor = when {
                isSpawnPoint -> Color(0xFFFF4400)
                isTarget -> Color(0xFF00DD00)
                isRiverTile -> Color(0xFF0066FF)
                gameState.level.isOnPath(position) -> Color(0xFFFFAA00)
                gameState.level.isBuildArea(position) -> Color(0xFF44BB44)
                else -> Color(0xFF888888)
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(2.dp, debugBorderColor, HexagonShape())
            )
        }

        // Debug overlay: tile position text
        if (AppSettings.showTilePositions.value) {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.8f))
                    .padding(1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${position.x},${position.y}",
                    fontSize = 8.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
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
