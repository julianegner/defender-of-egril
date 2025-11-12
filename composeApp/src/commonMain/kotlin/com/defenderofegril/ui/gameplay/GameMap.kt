package com.defenderofegril.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.defenderofegril.model.*
import com.defenderofegril.ui.*
import com.defenderofegril.ui.icon.ExplosionIcon
import com.defenderofegril.ui.icon.HoleIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import com.defenderofegril.ui.icon.TestTubeIcon
import com.defenderofegril.ui.icon.enemy.EnemyIcon
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
    onCellClick: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    // State for pan and zoom
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var actualContentSize by remember { mutableStateOf(IntSize.Zero) }

    val hexSize = 40.dp  // Radius of hexagon (center to corner)

    // Calculate hex dimensions for pointy-top hexagons
    val sqrt3 = sqrt(3.0).toFloat()
    val hexWidth = hexSize.value * sqrt3  // Width of hexagon (flat-to-flat)
    val hexHeight = hexSize.value * 2f    // Height of hexagon (point-to-point)

    // For pointy-top hexagons, vertical spacing between centers is 3/4 of height
    val verticalSpacing = hexHeight * 0.75f

    // Odd rows are offset to the right to create hexagonal grid pattern
    val oddRowOffset = hexWidth * 0.42f

    // Calculate total grid dimensions
    // Need enough width for all hexagons without compression
    // Each hexagon is hexWidth wide, we have gridWidth hexagons per row
    // Odd rows add oddRowOffset padding at the start
    // Add generous buffer (3 extra hexWidths) to ensure no compression
    val totalGridWidth = ((gameState.level.gridWidth + 3) * hexWidth + oddRowOffset).dp
    val totalGridHeight = ((gameState.level.gridHeight) * verticalSpacing + hexHeight).dp

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .mouseWheelZoom(
                containerSize = containerSize,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                onScaleChange = { newScale -> scale = newScale },
                onOffsetChange = { newOffsetX, newOffsetY -> 
                    offsetX = newOffsetX
                    offsetY = newOffsetY
                }
            )
            // Combined gesture handling for pan and pinch-zoom
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Apply zoom (for pinch gestures on mobile)
                    if (zoom != 1f) {
                        scale = (scale * zoom).coerceIn(0.5f, 3f)
                    }

                    // Apply pan
                    offsetX += pan.x
                    offsetY += pan.y

                    // Constrain pan to keep content visible
                    // Center the content initially and allow symmetric panning to see all edges
                    // Use actualContentSize which is the measured size of the Column
                    val contentWidth = actualContentSize.width * scale
                    val contentHeight = actualContentSize.height * scale

                    val maxOffsetX = if (contentWidth > containerSize.width) {
                        (contentWidth - containerSize.width) / 2  // Half the overflow for symmetric panning
                    } else {
                        (containerSize.width * (scale - 1) / 2).coerceAtLeast(0f)
                    }

                    val maxOffsetY = if (contentHeight > containerSize.height) {
                        (contentHeight - containerSize.height) / 2  // Half the overflow for symmetric panning
                    } else {
                        (containerSize.height * (scale - 1) / 2).coerceAtLeast(0f)
                    }

                    // Allow symmetric panning: +maxOffset (left/top edge) to -maxOffset (right/bottom edge)
                    offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                    offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                }
            }
    ) {
        // Map content with pan and zoom applied
        // Use layout modifier to allow Column to exceed parent bounds
        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    // Measure with infinite constraints to prevent compression
                    val placeable = measurable.measure(
                        constraints.copy(
                            maxWidth = Constraints.Infinity,
                            maxHeight = Constraints.Infinity
                        )
                    )
                    // Capture the actual content size for pan calculations
                    actualContentSize = IntSize(placeable.width, placeable.height)
                    // Report the actual size to parent (for proper container sizing)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            verticalArrangement = Arrangement.spacedBy((-hexHeight + verticalSpacing - 7f).dp)
        ) {
            for (y in 0 until gameState.level.gridHeight) {
                Row(
                    modifier = Modifier
                        .padding(
                            start = if (y % 2 == 1) (hexWidth * 0.42f).dp else 0.dp
                        )
                        .offset(y = (-(y-1)).dp),
                    horizontalArrangement = Arrangement.spacedBy((-10).dp)
                ) {
                    for (x in 0 until gameState.level.gridWidth) {
                        val position = Position(x, y)
                        GridCell(
                            position = position,
                            gameState = gameState,
                            isSelected = selectedDefenderType != null,
                            isDefenderSelected = selectedDefenderId?.let { selId ->
                                gameState.defenders.find { it.position == position }?.id == selId
                            } ?: false,
                            isTargetSelected = gameState.attackers.find { it.position.value == position }?.id == selectedTargetId,
                            selectedDefenderId = selectedDefenderId,
                            selectedTargetPosition = selectedTargetPosition,
                            selectedMineAction = selectedMineAction,
                            onClick = { onCellClick(position) },
                            hexSize = hexSize
                        )
                    }
                }
            }
        }

        // Overlay for target circles - drawn at GameGrid level to avoid clipping and ensure visibility
        TargetCirclesOverlay(
            selectedTargetPosition = selectedTargetPosition,
            selectedDefenderId = selectedDefenderId,
            gameState = gameState,
            hexWidth = hexWidth,
            hexHeight = hexHeight,
            verticalSpacing = verticalSpacing,
            oddRowOffset = oddRowOffset,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY
        )

        // Minimap - shown when zoomed in
        if (scale > 1.1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(120.dp)
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
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun TargetCirclesOverlay(
    selectedTargetPosition: Position?,
    selectedDefenderId: Int?,
    gameState: GameState,
    hexWidth: Float,
    hexHeight: Float,
    verticalSpacing: Float,
    oddRowOffset: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    if (selectedTargetPosition == null || selectedDefenderId == null) return
    
    val selectedDefender = remember(selectedDefenderId, gameState.defenders.size) {
        gameState.defenders.find { it.id == selectedDefenderId }
    }
    
    val attackType = selectedDefender?.type?.attackType
    val markerColor = when (attackType) {
        AttackType.AREA -> Color(0xFFFF5722)  // Deep orange/red for fireball
        AttackType.LASTING -> Color(0xFF4CAF50)  // Green for acid
        AttackType.MELEE, AttackType.RANGED -> Color.DarkGray  // DarkGray for single-target
        else -> null
    }
    
    markerColor?.let { color ->
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(11f)  // Above grid cells but below minimap
        ) {
            // Calculate position in untransformed coordinate space
            val targetX = selectedTargetPosition.x
            val targetY = selectedTargetPosition.y
            
            // Match the exact grid layout calculations
            val horizontalSpacing = -10f
            val verticalSpacingValue = -hexHeight + verticalSpacing - 7f
            
            // Calculate center position in grid coordinate space
            var centerX = targetX * (hexWidth + horizontalSpacing) * 2 + hexWidth
            if (targetY % 2 == 1) {
                centerX += hexWidth // / 2f - horizontalSpacing//oddRowOffset
            } else {
                centerX += 10f
            }
            centerX -= 14f


            // todo fix Y position offset
            var centerY = targetY * (hexHeight + verticalSpacingValue) * 2 + hexHeight / 2f
            centerY += (-(targetY - 1))

            centerY += 35f
            
            // Apply scale and offset transformations manually
            // val transformedX = centerX * scale + offsetX
            // val transformedY = centerY * scale + offsetY
            // val transformedCenter = Offset(transformedX, transformedY)

            val transformedCenter = Offset(
                x = centerX * scale + offsetX,
                y = centerY * scale + offsetY
            )
            
            // Draw base 3 circles (all attack types) with scaled radii
            drawCircle(
                color = color,
                radius = 6f * scale,
                center = transformedCenter
            )
            
            drawCircle(
                color = color,
                radius = 14f * scale,
                center = transformedCenter,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * scale)
            )
            
            drawCircle(
                color = color,
                radius = 22f * scale,
                center = transformedCenter,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * scale)
            )
            
            // For area attacks, draw 3 additional large circles
            if (attackType == AttackType.AREA || attackType == AttackType.LASTING) {
                drawCircle(
                    color = color,
                    radius = 100f * scale,
                    center = transformedCenter,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * scale)
                )
                
                drawCircle(
                    color = color,
                    radius = 130f * scale,
                    center = transformedCenter,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * scale)
                )
                
                drawCircle(
                    color = color,
                    radius = 160f * scale,
                    center = transformedCenter,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * scale)
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
    onClick: () -> Unit,
    hexSize: androidx.compose.ui.unit.Dp = 48.dp
) {
    val isDarkMode = com.defenderofegril.ui.settings.AppSettings.isDarkMode.value
    
    val isSpawnPoint = gameState.level.isSpawnPoint(position)
    val isTarget = position == gameState.level.targetPosition
    val isOnPath = gameState.level.isOnPath(position)
    val isBuildIsland = gameState.level.isBuildIsland(position)
    val isBuildArea = gameState.level.isBuildArea(position)
    val defender = gameState.defenders.find { it.position == position }
    val attacker = gameState.attackers.find { it.position.value == position && !it.isDefeated.value }

    // Check for field effects at this position
    val fieldEffect = gameState.fieldEffects.find { it.position == position }

    // Check for traps at this position
    val trap = gameState.traps.find { it.position == position }

    // Check if this cell is in range of the selected defender
    val cellIsInRange = selectedDefenderId?.let { defenderId ->
        val selectedDefender = gameState.defenders.find { it.id == defenderId }
        selectedDefender?.let { sel ->
            if (sel.position == position) {
                false  // Don't highlight the defender's own cell
            } else {
                val distance = sel.position.distanceTo(position)
                distance >= sel.type.minRange && distance <= sel.range
            }
        } ?: false
    } ?: false

    // Base background color based on area type - ALWAYS visible
    // Build islands + strips adjacent to path allow tower placement
    val baseBackgroundColor = when {
        isBuildIsland -> GamePlayColors.BuildIsland  // Light green for build islands
        isBuildArea -> GamePlayColors.BuildStrip  // Medium green for strips adjacent to path
        isOnPath -> GamePlayColors.Path  // Cream/beige for enemy path
        else -> GamePlayColors.NonPlayable  // Light gray for off-path areas (non-playable)
    }

    // Apply slight tint for selection states, but keep base color visible
    // Override with red background for enemy units and colored background for defenders
    // During INITIAL_BUILDING phase, don't apply any selection tints
    // Field effects also modify the background color
    val backgroundColor = when {
        attacker != null -> if (isDarkMode) GamePlayColors.ErrorDark else GamePlayColors.Error  // Darker red background for enemies in dark mode
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
        isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.7f)
        isTargetSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.8f)
        else -> baseBackgroundColor  // No selection highlighting during placement or in initial phase
    }

    // Border color - use borders to indicate entities instead of background
    // For range visualization, show green border on path tiles in range (only if tower has actions)
    val showRange = selectedDefenderId?.let { defenderId ->
        val selectedDefender = gameState.defenders.find { it.id == defenderId }
        selectedDefender?.isReady == true && selectedDefender.actionsRemaining.value > 0
    } ?: false

    // When placing trap, don't show green border on tiles with enemies
    val isTrapPlacement = selectedMineAction == MineAction.BUILD_TRAP
    val hasEnemy = attacker != null
    val canPlaceTrapHere = !hasEnemy || !isTrapPlacement

    val borderColor = when {
        cellIsInRange && isOnPath && showRange && canPlaceTrapHere -> GamePlayColors.Success  // Green border for tiles in range (exclude enemy tiles during trap placement)
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
        isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> 5.dp  // Extra thick border for selected defender (not during initial building)
        cellIsInRange && isOnPath && showRange && canPlaceTrapHere -> 4.dp  // Thick border for cells in range (exclude enemy tiles during trap placement)
        isSpawnPoint || isTarget -> 3.dp
        attacker != null || defender != null -> 3.dp
        fieldEffect != null -> 3.dp  // Thick border for field effects
        else -> 0.dp  // No border for empty cells
    }

    // Calculate hex dimensions for proper sizing
    val sqrt3 = sqrt(3.0).toFloat()
    val hexWidth = hexSize.value * sqrt3
    val hexHeight = hexSize.value * 2f

    Box(
        modifier = Modifier
            .width((hexWidth).dp)
            .height((hexHeight).dp)
            .clip(HexagonShape())
            .background(backgroundColor)
            .border(borderWidth, borderColor, HexagonShape())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            attacker != null -> {
                // Use graphical icon for enemy units
                // Key by id, position, level, and currentHealth to force recomposition when any changes
                key(attacker.id, attacker.position.value.x, attacker.position.value.y, attacker.level, attacker.currentHealth.value) {
                    EnemyIcon(attacker = attacker)
                }
            }

            defender != null -> {
                // Use graphical icon for towers
                // Key by id, level and actionsRemaining to force recomposition when these change
                key(defender.id, defender.level, defender.actionsRemaining.value, defender.buildTimeRemaining.value) {
                    TowerIcon(defender = defender, gameState = gameState)
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
                // Show trap icon with damage (no duration since traps are permanent until triggered)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    HoleIcon(size = 20.dp)
                    Text(
                        "-${trap.damage}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            isSpawnPoint -> {
                // Show spawn indicator when cell is empty
                Text(stringResource(Res.string.spawn), style = MaterialTheme.typography.labelSmall, color = GamePlayColors.Warning)
            }

            isTarget -> {
                // Show target indicator when cell is empty
                Text(stringResource(Res.string.target), style = MaterialTheme.typography.labelSmall, color = GamePlayColors.Success)
            }
        }
    }
}
