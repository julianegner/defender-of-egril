package de.egril.defender.ui.gameplay

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import de.egril.defender.config.LogConfig
import de.egril.defender.game.EnemyMovementSystem
import de.egril.defender.game.FreyaShieldWallArc
import de.egril.defender.game.PathfindingSystem
import de.egril.defender.game.freyaShieldWallArcs
import de.egril.defender.game.obsidianProtectorAuraPositions
import de.egril.defender.model.*
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.ui.*
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.rememberMapImageState
import de.egril.defender.ui.rememberMapTileZoneImageState
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Values derived from [GameGrid]'s inputs and game state. Read eagerly by the grid rendering.
 */
internal class GameGridDerivedState(
    val freyaShieldWallArcs: List<FreyaShieldWallArc>,
    val obsidianProtectorAuraPositions: Set<Position>,
    val selectedEnemyPathPositions: Set<Position>,
    val targetCircleMap: Map<Position, TargetCircleInfo>,
    val spellAreaCircleMap: Map<Position, TargetCircleInfo>,
    val placedBombCircleMap: MutableMap<Position, TargetCircleInfo>,
    val tileZoneImageState: MapTileZoneImageState,
    val mapImagePainter: Painter?,
    val useLevelMapImage: Boolean,
    val hasMapImage: Boolean,
    val hasEventMapImages: Boolean,
    val isLoadingMapImage: Boolean,
    val hexMapSizePx: Pair<Int, Int>,
    val defendersByPosition: Map<Position, Defender>,
    val activeAttackersByPosition: Map<Position, Attacker>,
    val dangerousAttackerPositions: MutableSet<Position>,
    val selectedDefenderForGrid: Defender?,
    val barricadeTowerBasePositions: MutableSet<Position>,
    val placementPreviewPositions: Set<Position>,
    val hoveredPositionIsBuildableForGrid: Boolean,
    val supportObjectPlacementPositions: Set<Position>,
    val supportFiefPlacementPositions: Set<Position>,
    val healingEffectsByPosition: Map<Position, HealingEffect>,
    val damageEffectsByPosition: Map<Position, DamageEffect>,
    val defeatedEnemyEffectsByPosition: Map<Position, EnemyDeathEffect>,
    val coinGainEffectsByPosition: Map<Position, CoinGainEffect>,
    val towerAttackEffectsByTargetPosition: Map<Position, TowerAttackEffect>,
    val fieldEffectsByPosition: Map<Position, FieldEffect>,
    val trapsByPositionMap: Map<Position, Trap>,
    val barricadesByPositionMap: Map<Position, Barricade>,
    val constructionCompleteEffectsByPosition: Map<Position, TowerConstructionEffect>,
    val enemySpawnEffectsByPosition: Map<Position, EnemySpawnEffect>,
    val trapTriggerEffectsByPosition: Map<Position, TrapTriggerEffect>,
    val enemyMoveEffectsByPosition: Map<Position, EnemyMoveEffect>,
    val dragonLevelChangeEffectsByPosition: Map<Position, DragonLevelChangeEffect>,
    val mineDigEffectsByPosition: Map<Position, MineDigEffect>,
    val fiefsByPositionMap: Map<Position, Fief>,
    val mushroomsByPositionMap: Map<Position, Mushroom>,
    val inactiveGargoylesByPositionMap: Map<Position, Gargoyle>,
    val arrowAttackTargetPositions: MutableSet<Position>,
    val ballistaAttackTargetPositions: MutableSet<Position>,
    val bowAttackTargetPositions: MutableSet<Position>,
    val spearAttackTargetPositions: MutableSet<Position>,
    val pikeAttackTargetPositions: MutableSet<Position>,
    val wizardAttackTargetPositions: MutableSet<Position>,
    val alchemyAttackTargetPositions: MutableSet<Position>,
    val wizardAttackAreaPositions: MutableSet<Position>,
    val alchemyAttackAreaPositions: MutableSet<Position>,
    val arrowAttackEffectsByAffectedPosition: MutableMap<Position, ArrowAttackEffect>,
    val onCellClickState: State<(Position) -> Unit>,
    val visibleZoneIdsByPosition: Map<Position, String>,
    val movedZoneTiles: Map<String, List<Pair<Position, Position>>>,
)

@Composable
internal fun rememberGameGridDerivedState(
    gameState: GameState,
    selectedDefenderType: DefenderType?,
    selectedDefenderId: Int?,
    selectedAttackerId: Int?,
    selectedTargetPosition: Position?,
    selectedSupportObject: SupportObjectType?,
    selectedSupportFief: FiefType?,
    onCellClick: (Position) -> Unit,
    hoveredPosition: Position?,
    hexSize: Dp,
): GameGridDerivedState {
    // Calculate target circle info for each tile
    // Find the selected defender and track its actions for dependency tracking
    val selectedDefender = gameState.defenders.find { it.id == selectedDefenderId }
    val selectedDefenderActions = selectedDefender?.actionsRemaining?.value
    val freyaShieldWallArcs = gameState.freyaShieldWallArcs()
    val obsidianProtectorAuraPositions = gameState.obsidianProtectorAuraPositions()

    val selectedEnemyPathPositions: Set<Position> by remember(
        selectedAttackerId,
        gameState.attackers.size,
        gameState.attackers.map { it.position.value },
        gameState.turnNumber.value,
        AppSettings.showEnemyPathfinding.value,
    ) {
        derivedStateOf {
            if (!AppSettings.showEnemyPathfinding.value || selectedAttackerId == null) {
                emptySet()
            } else {
                val selectedEnemy =
                    gameState.attackers.find { it.id == selectedAttackerId && !it.isDefeated.value }
                        ?: return@derivedStateOf emptySet()
                plannedEnemyPathForDisplay(gameState, selectedEnemy)?.drop(1)?.toSet() ?: emptySet()
            }
        }
    }

    val targetCircleMap =
        remember(selectedTargetPosition, selectedDefenderId, selectedDefenderActions, gameState.defenders.size) {
            if (selectedTargetPosition == null || selectedDefenderId == null || selectedDefender == null) {
                emptyMap()
            } else {
                val attackType = selectedDefender.type.attackType

                // Don't show target circles if the tower has no action points left
                if (selectedDefender.actionsRemaining.value <= 0) {
                    emptyMap()
                } else {
                    val markerColor =
                        when (attackType) {
                            AttackType.AREA -> Color(0xFFFF5722) // Deep orange/red for fireball
                            AttackType.LASTING -> Color(0xFF4CAF50) // Green for acid
                            AttackType.MELEE, AttackType.RANGED -> Color.DarkGray // DarkGray for single-target
                            AttackType.NONE -> null // No target circles for special structures
                        }

                    if (markerColor == null) {
                        emptyMap()
                    } else {
                        val result = mutableMapOf<Position, TargetCircleInfo>()
                        val areaRadius = selectedDefender.areaEffectRadius
                        val isExtendedArea = areaRadius >= 2

                        // Check if target position has a magical bridge (which cannot be targeted by non-area attacks)
                        val hasMagicalBridge =
                            gameState.isBridgeAt(selectedTargetPosition) &&
                                gameState.getBridgeAt(selectedTargetPosition)?.type == BridgeType.MAGICAL

                        // Check if there's an enemy at the target position
                        val hasEnemy =
                            gameState.attackers.any {
                                it.position.value == selectedTargetPosition && !it.isDefeated.value
                            }

                        // Don't show target circles for non-area attacks on magical bridges UNLESS there's an enemy
                        if (hasMagicalBridge && !hasEnemy && attackType != AttackType.AREA && attackType != AttackType.LASTING) {
                            emptyMap()
                        } else {
                            val isSingleTargetBridgeOnly =
                                (attackType == AttackType.MELEE || attackType == AttackType.RANGED) &&
                                    gameState.isBridgeAt(selectedTargetPosition) &&
                                    !hasEnemy
                            val effectiveMarkerColor = if (isSingleTargetBridgeOnly) Color.LightGray else markerColor

                            // Central target tile
                            result[selectedTargetPosition] =
                                TargetCircleInfo.CentralTarget(
                                    color = effectiveMarkerColor,
                                    attackType = attackType,
                                    isExtendedArea = isExtendedArea,
                                )

                            // For AREA and LASTING attacks, add neighbor tiles that are on the path, or have bridges/enemies
                            if (attackType == AttackType.AREA || attackType == AttackType.LASTING) {
                                if (areaRadius == 1) {
                                    // Standard radius 1 - use getHexNeighbors
                                    val neighbors =
                                        selectedTargetPosition
                                            .getHexNeighbors()
                                            .filter { neighbor ->
                                                neighbor.x >= 0 &&
                                                    neighbor.x < gameState.level.gridWidth &&
                                                    neighbor.y >= 0 &&
                                                    neighbor.y < gameState.level.gridHeight &&
                                                    (
                                                        gameState.level.isOnPath(neighbor) ||
                                                            gameState.isBridgeAt(neighbor) ||
                                                            gameState.attackers.any {
                                                                it.position.value == neighbor && !it.isDefeated.value
                                                            }
                                                    )
                                            }

                                    for (neighbor in neighbors) {
                                        result[neighbor] =
                                            TargetCircleInfo.NeighborTarget(
                                                color = markerColor,
                                                attackType = attackType,
                                                centerPosition = selectedTargetPosition,
                                                thisPosition = neighbor,
                                                distanceFromCenter = 1,
                                                isExtendedArea = false,
                                            )
                                    }
                                } else {
                                    // Extended radius 2 (level 20+) - use getHexNeighborsWithinRadius
                                    val allNeighbors =
                                        selectedTargetPosition
                                            .getHexNeighborsWithinRadius(
                                                areaRadius,
                                                gameState.level.gridWidth,
                                                gameState.level.gridHeight,
                                            ).filter { neighbor ->
                                                gameState.level.isOnPath(neighbor) ||
                                                    gameState.isBridgeAt(neighbor) ||
                                                    gameState.attackers.any { it.position.value == neighbor && !it.isDefeated.value }
                                            }

                                    for (neighbor in allNeighbors) {
                                        val distance = selectedTargetPosition.hexDistanceTo(neighbor)
                                        result[neighbor] =
                                            TargetCircleInfo.NeighborTarget(
                                                color = markerColor,
                                                attackType = attackType,
                                                centerPosition = selectedTargetPosition,
                                                thisPosition = neighbor,
                                                distanceFromCenter = distance,
                                                isExtendedArea = true,
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
    val spellAreaCircleMap =
        remember(currentHoveredPosition, spellAreaTargeting?.activeSpell) {
            val activeSpell = spellAreaTargeting?.activeSpell
            // All spell targeting previews use the same magic (purple) color to distinguish them from tower attacks
            val spellColor = TargetCircleConstants.ATTACK_AREA_SPELL_COLOR
            // Bomb uses a distinct orange/red color to represent fire/explosion
            val bombColor = TargetCircleConstants.BOMB_SPELL_COLOR
            val bombExplosionRange = TargetCircleConstants.BOMB_SPELL_RADIUS
            when {
                (activeSpell == SpellType.ATTACK_AREA || activeSpell == SpellType.ATTACK_AIMED) && currentHoveredPosition != null -> {
                    val result = mutableMapOf<Position, TargetCircleInfo>()
                    result[currentHoveredPosition] =
                        TargetCircleInfo.CentralTarget(
                            color = spellColor,
                            attackType = AttackType.AREA,
                            isExtendedArea = true,
                        )
                    if (activeSpell == SpellType.ATTACK_AREA) {
                        val allNeighbors =
                            currentHoveredPosition
                                .getHexNeighborsWithinRadius(
                                    TargetCircleConstants.ATTACK_AREA_SPELL_RADIUS,
                                    gameState.level.gridWidth,
                                    gameState.level.gridHeight,
                                ).filter { neighbor ->
                                    gameState.level.isOnPath(neighbor) ||
                                        gameState.isBridgeAt(neighbor) ||
                                        gameState.attackers.any { it.position.value == neighbor && !it.isDefeated.value }
                                }
                        for (neighbor in allNeighbors) {
                            val distance = currentHoveredPosition.hexDistanceTo(neighbor)
                            result[neighbor] =
                                TargetCircleInfo.NeighborTarget(
                                    color = spellColor,
                                    attackType = AttackType.AREA,
                                    centerPosition = currentHoveredPosition,
                                    thisPosition = neighbor,
                                    distanceFromCenter = distance,
                                    isExtendedArea = true,
                                )
                        }
                    }
                    result
                }
                activeSpell == SpellType.BOMB && currentHoveredPosition != null -> {
                    // Show explosion range (3 hex tiles) around hovered position in orange
                    val result = mutableMapOf<Position, TargetCircleInfo>()
                    result[currentHoveredPosition] =
                        TargetCircleInfo.CentralTarget(
                            color = bombColor,
                            attackType = AttackType.AREA,
                            isExtendedArea = true,
                        )
                    val allNeighbors =
                        currentHoveredPosition.getHexNeighborsWithinRadius(
                            bombExplosionRange,
                            gameState.level.gridWidth,
                            gameState.level.gridHeight,
                        )
                    for (neighbor in allNeighbors) {
                        val distance = currentHoveredPosition.hexDistanceTo(neighbor)
                        result[neighbor] =
                            TargetCircleInfo.NeighborTarget(
                                color = bombColor,
                                attackType = AttackType.AREA,
                                centerPosition = currentHoveredPosition,
                                thisPosition = neighbor,
                                distanceFromCenter = distance,
                                isExtendedArea = true,
                            )
                    }
                    result
                }
                activeSpell == SpellType.FEAR_SPELL_AREA && currentHoveredPosition != null -> {
                    // Area circles in magic color at radius 2 (like ATTACK_AREA)
                    val result = mutableMapOf<Position, TargetCircleInfo>()
                    result[currentHoveredPosition] =
                        TargetCircleInfo.CentralTarget(
                            color = spellColor,
                            attackType = AttackType.AREA,
                            isExtendedArea = true,
                        )
                    val allNeighbors =
                        currentHoveredPosition
                            .getHexNeighborsWithinRadius(
                                TargetCircleConstants.ATTACK_AREA_SPELL_RADIUS,
                                gameState.level.gridWidth,
                                gameState.level.gridHeight,
                            ).filter { neighbor ->
                                gameState.level.isOnPath(neighbor) ||
                                    gameState.isBridgeAt(neighbor) ||
                                    gameState.attackers.any { it.position.value == neighbor && !it.isDefeated.value }
                            }
                    for (neighbor in allNeighbors) {
                        val distance = currentHoveredPosition.hexDistanceTo(neighbor)
                        result[neighbor] =
                            TargetCircleInfo.NeighborTarget(
                                color = spellColor,
                                attackType = AttackType.AREA,
                                centerPosition = currentHoveredPosition,
                                thisPosition = neighbor,
                                distanceFromCenter = distance,
                                isExtendedArea = true,
                            )
                    }
                    result
                }
                activeSpell == SpellType.COOLING_SPELL && currentHoveredPosition != null -> {
                    // Show turquoise circles at radius 2 around hovered position, only for path/spawn tiles
                    val coolingColor = TargetCircleConstants.COOLING_SPELL_COLOR
                    val result = mutableMapOf<Position, TargetCircleInfo>()
                    if (gameState.level.isEnemyTraversable(currentHoveredPosition)) {
                        result[currentHoveredPosition] =
                            TargetCircleInfo.CentralTarget(
                                color = coolingColor,
                                attackType = AttackType.AREA,
                                isExtendedArea = true,
                            )
                    }
                    val allNeighbors =
                        currentHoveredPosition
                            .getHexNeighborsWithinRadius(
                                TargetCircleConstants.COOLING_SPELL_RADIUS,
                                gameState.level.gridWidth,
                                gameState.level.gridHeight,
                            ).filter { neighbor ->
                                gameState.level.isEnemyTraversable(neighbor)
                            }
                    for (neighbor in allNeighbors) {
                        val distance = currentHoveredPosition.hexDistanceTo(neighbor)
                        result[neighbor] =
                            TargetCircleInfo.NeighborTarget(
                                color = coolingColor,
                                attackType = AttackType.AREA,
                                centerPosition = currentHoveredPosition,
                                thisPosition = neighbor,
                                distanceFromCenter = distance,
                                isExtendedArea = true,
                            )
                    }
                    result
                }
                activeSpell == SpellType.FEAR_SPELL && currentHoveredPosition != null -> {
                    // Single-target circles on hovered enemy tile in magic color (like tower attack)
                    val enemyAtHover =
                        gameState.attackers.find {
                            it.position.value == currentHoveredPosition && !it.isDefeated.value
                        }
                    if (enemyAtHover != null) {
                        mapOf(
                            currentHoveredPosition to
                                TargetCircleInfo.CentralTarget(
                                    color = spellColor,
                                    attackType = AttackType.RANGED,
                                    isExtendedArea = false,
                                ),
                        )
                    } else {
                        emptyMap()
                    }
                }
                else -> emptyMap()
            }
        }

    // Calculate range circles for already-placed bombs (show explosion range, but no center rings)
    val activeBombEffects = gameState.activeSpellEffects.filter { it.spell == SpellType.BOMB && it.position != null }
    val placedBombCircleMap =
        remember(activeBombEffects.map { it.position }) {
            val bombColor = TargetCircleConstants.BOMB_SPELL_COLOR
            val bombExplosionRange = TargetCircleConstants.BOMB_SPELL_RADIUS
            val result = mutableMapOf<Position, TargetCircleInfo>()
            for (effect in activeBombEffects) {
                val bombPos = effect.position ?: continue
                // Intentionally skip the bomb tile itself (no center rings on placed bombs)
                val allNeighbors =
                    bombPos.getHexNeighborsWithinRadius(
                        bombExplosionRange,
                        gameState.level.gridWidth,
                        gameState.level.gridHeight,
                    )
                for (neighbor in allNeighbors) {
                    if (!result.containsKey(neighbor)) {
                        val distance = bombPos.hexDistanceTo(neighbor)
                        result[neighbor] =
                            TargetCircleInfo.NeighborTarget(
                                color = bombColor,
                                attackType = AttackType.AREA,
                                centerPosition = bombPos,
                                thisPosition = neighbor,
                                distanceFromCenter = distance,
                                isExtendedArea = true,
                            )
                    }
                }
            }
            result
        }

    val mapId = gameState.level.mapId
    val mapImageState = rememberMapImageState(mapId)
    val tileZoneImageState = rememberMapTileZoneImageState(mapId, gameState.level.tileZones)
    val mapImagePainter = mapImageState.painter
    val useLevelMapImage = AppSettings.useLevelMapImage.value
    val hasMapImage = mapImagePainter != null && useLevelMapImage
    val hasEventMapImages = gameState.activeEventMapImages.isNotEmpty()
    val isLoadingMapImage = mapImageState.isLoading || tileZoneImageState.isLoading
    val hexMapSizePx =
        remember(gameState.level.gridWidth, gameState.level.gridHeight, hexSize) {
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

    // Pre-compute position lookup maps for O(1) per-cell lookups.
    // On large maps (e.g. 80×80 = 6 400 tiles) replacing O(n) list scans with O(1) map
    // lookups gives a significant speedup when all cells recompose (e.g. after clicking a
    // tower-buy button or placing a tower).
    //
    // Use derivedStateOf so that these maps are only rebuilt when the defenders/attackers
    // SnapshotStateLists actually change — not on every unrelated state change (e.g. coins
    // updating).  derivedStateOf tracks the State reads inside its lambda automatically, so
    // no explicit remember key is needed for the list-based maps.
    val defendersByPosition by remember {
        derivedStateOf { gameState.defenders.associateBy { it.position.value } }
    }
    val activeAttackersByPosition by remember {
        derivedStateOf {
            gameState.attackers
                .filter { !it.isDefeated.value }
                .associateBy { it.position.value }
        }
    }
    val dangerousAttackerPositions by remember {
        derivedStateOf {
            val movementSystem = EnemyMovementSystem(gameState, PathfindingSystem(gameState))
            gameState.attackers
                .filter { movementSystem.canReachTargetNextTurn(it) }
                .mapTo(mutableSetOf()) { it.position.value }
        }
    }

    // Pre-compute the selected defender once (replaces 6+ O(n) searches per GridCell).
    // selectedDefenderId is a plain Int? parameter (not a State), so derivedStateOf cannot
    // track it automatically — include it as a remember key so the derived state is
    // recreated when the selection changes.
    val selectedDefenderForGrid by remember(selectedDefenderId) {
        derivedStateOf { selectedDefenderId?.let { id -> gameState.defenders.find { it.id == id } } }
    }

    // Pre-compute the set of positions that are structurally buildable (build areas or flowing
    // river tiles). This is static for a given level — it never changes during gameplay.
    // Level.buildAreas is already a Set<Position>, so the union is O(|riverTiles|) at most.
    val structurallyBuildablePositions =
        remember(gameState.level) {
            val flowingRiver =
                gameState.level.riverTiles.entries
                    .filter { (_, rt) -> rt.flowDirection != RiverFlow.NONE && rt.flowDirection != RiverFlow.MAELSTROM }
                    .mapTo(mutableSetOf()) { (pos, _) -> pos }
            gameState.level.buildAreas + flowingRiver
        }

    // Subset of structurally buildable positions that are currently unoccupied and valid for the
    // currently selected defender type (mine constraints, bridge-on-river constraints, etc.).
    val buildableEmptyPositions: Set<Position> by remember(gameState.level, selectedDefenderType) {
        derivedStateOf<Set<Position>> {
            val selectedType = selectedDefenderType ?: return@derivedStateOf emptySet<Position>()
            structurallyBuildablePositions.filterTo(mutableSetOf()) { pos ->
                if (defendersByPosition.containsKey(pos) || activeAttackersByPosition.containsKey(pos)) {
                    return@filterTo false
                }
                if (gameState.level.isSpawnPoint(pos) || gameState.level.isTargetPosition(pos)) {
                    return@filterTo false
                }
                if (!gameState.level.isPlacementAllowed(pos)) return@filterTo false
                val isRiverPlacement = gameState.level.isRiverTile(pos)
                if (isRiverPlacement) {
                    if (selectedType == DefenderType.DWARVEN_MINE) return@filterTo false
                    if (gameState.isBridgeAt(pos)) return@filterTo false
                }
                true
            }
        }
    }

    // Set of positions where a barricade currently supports tower placement
    // (healthPoints >= 100 AND no tower already placed on it).
    // derivedStateOf reads barricade.canSupportTower() and barricade.hasTower(), which
    // read MutableState values — so this re-evaluates when barricade HP or tower status changes.
    // remember(gameState.level) re-creates on level change for consistency with the other sets.
    val barricadeTowerBasePositions by remember(gameState.level) {
        derivedStateOf {
            gameState.barricades
                .filter { b -> b.canSupportTower() && !b.hasTower() && gameState.level.isPlacementAllowed(b.position) }
                .mapTo(mutableSetOf()) { it.position }
        }
    }

    // Auto tower base levels: free PATH tiles are valid tower spots when the surcharge is affordable.
    // The occupancy and spawn-distance rules live in GameState.isAutoTowerBasePathSpot.
    val autoTowerBasePathPositions: Set<Position> by remember(selectedDefenderType, gameState.level) {
        derivedStateOf<Set<Position>> {
            val selectedType = selectedDefenderType
            if (selectedType == null || !gameState.canPlaceDefender(selectedType, gameState.level.autoTowerBaseExtraCost)) {
                emptySet<Position>()
            } else {
                gameState.level.pathCells.filterTo(mutableSetOf()) { pos ->
                    gameState.level.isPlacementAllowed(pos) && gameState.isAutoTowerBasePathSpot(selectedType, pos)
                }
            }
        }
    }

    val placementPreviewPositions: Set<Position> by remember(selectedDefenderType, gameState.level, autoTowerBasePathPositions) {
        derivedStateOf<Set<Position>> {
            if (selectedDefenderType == null) {
                emptySet<Position>()
            } else if (selectedDefenderType == DefenderType.DWARVEN_MINE) {
                buildableEmptyPositions
            } else {
                buildableEmptyPositions + barricadeTowerBasePositions + autoTowerBasePathPositions
            }
        }
    }

    // Pre-compute whether the hovered position is buildable. Uses buildableEmptyPositions
    // (O(1) Set.contains) instead of the previous 5-step manual check.
    val hoveredPositionIsBuildableForGrid =
        selectedDefenderType != null &&
            hoveredPosition != null &&
            placementPreviewPositions.contains(hoveredPosition)

    // Valid tiles for placing the currently selected support object (barricade / trap / magical
    // trap). Computed once per selection change so the per-cell hover preview below is an O(1)
    // Set lookup. Empty when no support object is being placed.
    val supportObjectPlacementPositions: Set<Position> by remember(gameState.level, selectedSupportObject) {
        derivedStateOf {
            selectedSupportObject
                ?.let { supportObjectPlacementTiles(gameState, it).filterTo(HashSet()) { pos -> gameState.level.isPlacementAllowed(pos) } }
                ?: emptySet()
        }
    }

    // Valid tiles for placing a fief support token. Empty when no fief is being placed.
    val supportFiefPlacementPositions: Set<Position> by remember(gameState.level, selectedSupportFief) {
        derivedStateOf {
            selectedSupportFief
                ?.let { supportFiefPlacementTiles(gameState, it).toHashSet() }
                ?: emptySet()
        }
    }

    // Pre-compute O(1) position-keyed lookup maps to replace O(N) .find{} / .any{} scans per tile.
    // derivedStateOf ensures each map is only rebuilt when its source list actually changes.
    // Compose's strong-skipping then skips cells whose resolved value remains null.
    val healingEffectsByPosition by remember {
        derivedStateOf { gameState.healingEffects.associateBy { it.position } }
    }
    val damageEffectsByPosition by remember {
        derivedStateOf { gameState.damageEffects.associateBy { it.position } }
    }
    val defeatedEnemyEffectsByPosition by remember {
        derivedStateOf { gameState.defeatedEnemyEffects.associateBy { it.position } }
    }
    val coinGainEffectsByPosition by remember {
        derivedStateOf { gameState.coinGainEffects.associateBy { it.position } }
    }
    val towerAttackEffectsByTargetPosition by remember {
        derivedStateOf { gameState.towerAttackEffects.associateBy { it.targetPosition } }
    }
    val fieldEffectsByPosition by remember {
        derivedStateOf { gameState.fieldEffects.associateBy { it.position } }
    }
    val trapsByPositionMap by remember {
        derivedStateOf { gameState.traps.associateBy { it.position } }
    }
    val barricadesByPositionMap by remember {
        derivedStateOf { gameState.barricades.associateBy { it.position } }
    }
    val constructionCompleteEffectsByPosition by remember {
        derivedStateOf { gameState.constructionCompleteEffects.associateBy { it.position } }
    }
    val enemySpawnEffectsByPosition by remember {
        derivedStateOf { gameState.enemySpawnEffects.associateBy { it.position } }
    }
    val trapTriggerEffectsByPosition by remember {
        derivedStateOf { gameState.trapTriggerEffects.associateBy { it.position } }
    }
    val enemyMoveEffectsByPosition by remember {
        derivedStateOf { gameState.enemyMoveEffects.associateBy { it.position } }
    }
    val dragonLevelChangeEffectsByPosition by remember {
        derivedStateOf { gameState.dragonLevelChangeEffects.associateBy { it.position } }
    }
    val mineDigEffectsByPosition by remember {
        derivedStateOf { gameState.mineDigEffects.associateBy { it.position } }
    }
    val fiefsByPositionMap by remember {
        derivedStateOf { gameState.fiefs.associateBy { it.position } }
    }
    val mushroomsByPositionMap by remember {
        derivedStateOf { gameState.mushrooms.associateBy { it.position } }
    }
    // Only inactive (stone) gargoyles are drawn as tile content; active ones are attackers.
    val inactiveGargoylesByPositionMap by remember {
        derivedStateOf { gameState.gargoyles.filter { !it.isActive.value }.associateBy { it.position.value } }
    }
    // Attack target position sets for O(1) membership checks
    val arrowAttackTargetPositions by remember {
        derivedStateOf { gameState.arrowAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    val ballistaAttackTargetPositions by remember {
        derivedStateOf { gameState.ballistaAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    val bowAttackTargetPositions by remember {
        derivedStateOf { gameState.bowAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    val spearAttackTargetPositions by remember {
        derivedStateOf { gameState.spearAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    val pikeAttackTargetPositions by remember {
        derivedStateOf { gameState.pikeAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    val wizardAttackTargetPositions by remember {
        derivedStateOf { gameState.wizardAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    val alchemyAttackTargetPositions by remember {
        derivedStateOf { gameState.alchemyAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    // Expanded area sets (wizard radius 1, alchemy radius 2) for AoE attack suppression
    val wizardAttackAreaPositions by remember {
        derivedStateOf {
            val result = mutableSetOf<Position>()
            gameState.wizardAttackEffects.forEach { effect ->
                result.add(effect.targetPosition)
                effect.targetPosition.getHexNeighbors().forEach { result.add(it) }
            }
            result
        }
    }
    val alchemyAttackAreaPositions by remember {
        derivedStateOf {
            val result = mutableSetOf<Position>()
            gameState.alchemyAttackEffects.forEach { effect ->
                for (dx in -2..2) {
                    for (dy in -2..2) {
                        val candidate = Position(effect.targetPosition.x + dx, effect.targetPosition.y + dy)
                        if (effect.targetPosition.hexDistanceTo(candidate) <= 2) {
                            result.add(candidate)
                        }
                    }
                }
            }
            result
        }
    }
    // Pre-compute map of position -> ArrowAttackEffect covering source, target, and path tiles.
    // Uses the same linear-interpolation algorithm as the private isOnArrowLinePath() function so
    // the set of covered tiles is identical to the original per-tile computation.
    val arrowAttackEffectsByAffectedPosition by remember {
        derivedStateOf {
            val result = mutableMapOf<Position, ArrowAttackEffect>()
            gameState.arrowAttackEffects.forEach { effect ->
                result[effect.sourcePosition] = effect
                result[effect.targetPosition] = effect
                val dx = effect.targetPosition.x - effect.sourcePosition.x
                val dy = effect.targetPosition.y - effect.sourcePosition.y
                val steps = maxOf(abs(dx), abs(dy))
                for (step in 1 until steps) {
                    val t = step.toFloat() / steps
                    val ix = (effect.sourcePosition.x + dx * t).roundToInt()
                    val iy = (effect.sourcePosition.y + dy * t).roundToInt()
                    result.getOrPut(Position(ix, iy)) { effect }
                }
            }
            result
        }
    }

    // Stable reference to onCellClick via rememberUpdatedState.
    //
    // Why this matters for performance:
    //   HexagonalMapView calls `content(position)` in a plain for-loop — a @Composable lambda.
    //   Inside that lambda, `onClick = { onCellClick(position) }` creates a NEW Function0 object
    //   on every invocation.  Compose's strong-skipping compares GridCell parameters by identity;
    //   a new lambda object always differs → GridCell is NEVER skipped → all 6,400 bodies run.
    //
    //   By using rememberUpdatedState we get a stable State<> reference that can be captured
    //   once (inside remember(position)) and read at call-time without becoming stale.
    val onCellClickState = rememberUpdatedState(onCellClick)
    val activeZoneIds = gameState.activeTileZoneIds.toList()
    val movedRegionsSnapshot = gameState.movedTileRegions.toMap()
    val visibleZoneIdsByPosition =
        remember(gameState.level.tileZones, activeZoneIds, movedRegionsSnapshot) {
            buildMap {
                activeZoneIds.forEach { id ->
                    gameState.level.tileZones.firstOrNull { it.id == id }?.tiles?.keys?.forEach { position ->
                        put(position, id)
                    }
                }
                // Moved movable regions are never activated, but their tiles still show their zone image.
                movedRegionsSnapshot.forEach { (id, tiles) ->
                    tiles.keys.forEach { position -> put(position, id) }
                }
            }
        }
    val movedZoneTiles =
        remember(movedRegionsSnapshot) {
            movedRegionsSnapshot.mapValues { (_, tiles) -> tiles.map { (current, tile) -> current to tile.homePosition } }
        }

    return GameGridDerivedState(
        freyaShieldWallArcs = freyaShieldWallArcs,
        obsidianProtectorAuraPositions = obsidianProtectorAuraPositions,
        selectedEnemyPathPositions = selectedEnemyPathPositions,
        targetCircleMap = targetCircleMap,
        spellAreaCircleMap = spellAreaCircleMap,
        placedBombCircleMap = placedBombCircleMap,
        tileZoneImageState = tileZoneImageState,
        mapImagePainter = mapImagePainter,
        useLevelMapImage = useLevelMapImage,
        hasMapImage = hasMapImage,
        hasEventMapImages = hasEventMapImages,
        isLoadingMapImage = isLoadingMapImage,
        hexMapSizePx = hexMapSizePx,
        defendersByPosition = defendersByPosition,
        activeAttackersByPosition = activeAttackersByPosition,
        dangerousAttackerPositions = dangerousAttackerPositions,
        selectedDefenderForGrid = selectedDefenderForGrid,
        barricadeTowerBasePositions = barricadeTowerBasePositions,
        placementPreviewPositions = placementPreviewPositions,
        hoveredPositionIsBuildableForGrid = hoveredPositionIsBuildableForGrid,
        supportObjectPlacementPositions = supportObjectPlacementPositions,
        supportFiefPlacementPositions = supportFiefPlacementPositions,
        healingEffectsByPosition = healingEffectsByPosition,
        damageEffectsByPosition = damageEffectsByPosition,
        defeatedEnemyEffectsByPosition = defeatedEnemyEffectsByPosition,
        coinGainEffectsByPosition = coinGainEffectsByPosition,
        towerAttackEffectsByTargetPosition = towerAttackEffectsByTargetPosition,
        fieldEffectsByPosition = fieldEffectsByPosition,
        trapsByPositionMap = trapsByPositionMap,
        barricadesByPositionMap = barricadesByPositionMap,
        constructionCompleteEffectsByPosition = constructionCompleteEffectsByPosition,
        enemySpawnEffectsByPosition = enemySpawnEffectsByPosition,
        trapTriggerEffectsByPosition = trapTriggerEffectsByPosition,
        enemyMoveEffectsByPosition = enemyMoveEffectsByPosition,
        dragonLevelChangeEffectsByPosition = dragonLevelChangeEffectsByPosition,
        mineDigEffectsByPosition = mineDigEffectsByPosition,
        fiefsByPositionMap = fiefsByPositionMap,
        mushroomsByPositionMap = mushroomsByPositionMap,
        inactiveGargoylesByPositionMap = inactiveGargoylesByPositionMap,
        arrowAttackTargetPositions = arrowAttackTargetPositions,
        ballistaAttackTargetPositions = ballistaAttackTargetPositions,
        bowAttackTargetPositions = bowAttackTargetPositions,
        spearAttackTargetPositions = spearAttackTargetPositions,
        pikeAttackTargetPositions = pikeAttackTargetPositions,
        wizardAttackTargetPositions = wizardAttackTargetPositions,
        alchemyAttackTargetPositions = alchemyAttackTargetPositions,
        wizardAttackAreaPositions = wizardAttackAreaPositions,
        alchemyAttackAreaPositions = alchemyAttackAreaPositions,
        arrowAttackEffectsByAffectedPosition = arrowAttackEffectsByAffectedPosition,
        onCellClickState = onCellClickState,
        visibleZoneIdsByPosition = visibleZoneIdsByPosition,
        movedZoneTiles = movedZoneTiles,
    )
}
