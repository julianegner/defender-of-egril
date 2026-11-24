package de.egril.defender.ui.gameplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hyperether.resources.stringResource
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.config.LogConfig
import de.egril.defender.game.BridgeSystem
import de.egril.defender.game.EnemyMovementSystem
import de.egril.defender.game.PathfindingSystem
import de.egril.defender.game.freyaShieldWallArcs
import de.egril.defender.game.isProtectedByObsidianProtector
import de.egril.defender.game.obsidianProtectorAuraPositions
import de.egril.defender.model.*
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.ui.*
import de.egril.defender.ui.animations.AlchemyAttackOverlay
import de.egril.defender.ui.animations.AlchemyIdleAnimation
import de.egril.defender.ui.animations.ArrowAttackAnimation
import de.egril.defender.ui.animations.BallistaAttackOverlay
import de.egril.defender.ui.animations.BarricadeDamageAnimation
import de.egril.defender.ui.animations.BombExplosionAnimation
import de.egril.defender.ui.animations.BowAttackOverlay
import de.egril.defender.ui.animations.CoinFlightController
import de.egril.defender.ui.animations.CoinGainAnimation
import de.egril.defender.ui.animations.DragonLevelChangeAnimation
import de.egril.defender.ui.animations.DragonTargetAnimation
import de.egril.defender.ui.animations.EnemyDeathAnimation
import de.egril.defender.ui.animations.EnemyMoveAnimation
import de.egril.defender.ui.animations.EnemySpawnAnimation
import de.egril.defender.ui.animations.FearSpellAnimation
import de.egril.defender.ui.animations.FreezeSpellAnimation
import de.egril.defender.ui.animations.GarokkWarCryOverlay
import de.egril.defender.ui.animations.GreenWitchHealingAnimation
import de.egril.defender.ui.animations.InstantTowerSpellAnimation
import de.egril.defender.ui.animations.MineDigAnimation
import de.egril.defender.ui.animations.MushroomBuffAnimation
import de.egril.defender.ui.animations.PikeAttackOverlay
import de.egril.defender.ui.animations.RocketAttackOverlay
import de.egril.defender.ui.animations.SkyIsFallingAnimation
import de.egril.defender.ui.animations.SnotlingCannonThrowOverlay
import de.egril.defender.ui.animations.SpearAttackOverlay
import de.egril.defender.ui.animations.SpellDoubleReachColor
import de.egril.defender.ui.animations.TowerAttackImpactAnimation
import de.egril.defender.ui.animations.TowerConstructionCompleteAnimation
import de.egril.defender.ui.animations.TowerReadyPulseAnimation
import de.egril.defender.ui.animations.TrapTriggerAnimation
import de.egril.defender.ui.animations.WaterFlowAnimation
import de.egril.defender.ui.animations.WizardAttackOverlay
import de.egril.defender.ui.animations.WizardIdleAnimation
import de.egril.defender.ui.editor.RiverFlowIndicator
import de.egril.defender.ui.editor.map.MapControlState
import de.egril.defender.ui.editor.map.MapControls
import de.egril.defender.ui.hexagon.HexagonMinimap
import de.egril.defender.ui.hexagon.HexagonShape
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.hexagon.HexagonalMapConfig
import de.egril.defender.ui.hexagon.HexagonalMapView
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.ui.icon.BombIcon
import de.egril.defender.ui.icon.CrossIcon
import de.egril.defender.ui.icon.ExplosionIcon
import de.egril.defender.ui.icon.FisherBoatIcon
import de.egril.defender.ui.icon.GateIcon
import de.egril.defender.ui.icon.MushroomIcon
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.icon.TestTubeIcon
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.icon.WebIcon
import de.egril.defender.ui.icon.WoodIcon
import de.egril.defender.ui.icon.enemy.EnemyAttackPreview
import de.egril.defender.ui.icon.enemy.EnemyAttackPreviewIcon
import de.egril.defender.ui.icon.enemy.EnemyIcon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import de.egril.defender.ui.icon.enemy.GargoyleIcon
import de.egril.defender.ui.icon.enemy.shouldShowSeafaringPirateBarge
import de.egril.defender.ui.rememberMapImageState
import de.egril.defender.ui.rememberMapTileZoneImageState
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
internal fun rememberDisplayedEnemyHealth(attacker: Attacker?): MutableState<Int> = remember(attacker?.id) { mutableStateOf(attacker?.currentHealth?.value ?: 0) }

@Composable
fun GridCell(
    position: Position,
    gameState: GameState,
    defender: Defender?,
    attacker: Attacker?,
    isDangerous: Boolean = false,
    selectedDefender: Defender?,
    // Read lazily in the draw phase so zoom changes do not recompose every cell.
    mapZoom: () -> Float,
    // isHovering and isInPreviewRange replace the old hoveredPosition: Position? and
    // hoveredPositionIsBuildable: Boolean parameters.  Passing per-cell Booleans means only
    // the 1-2 cells whose hover state actually changed receive a different value and need to
    // recompose; the remaining 6,398 cells keep the same `false` value and are skipped.
    isHovering: Boolean,
    isInPreviewRange: Boolean,
    // showPlacementPreview, isBuildableAndEmpty, canBeUsedAsTowerBase, and previewDefenderType
    // replace selectedDefenderType: DefenderType?.  Pre-computing per-cell Booleans in GameGrid's
    // content lambda means non-buildable cells (which stay `false`) are skipped by Compose when
    // selectedDefenderType changes (buy button click) — only ~10 % of cells recompose.
    showPlacementPreview: Boolean,
    isBuildableAndEmpty: Boolean,
    canBeUsedAsTowerBase: Boolean,
    // previewDefenderType is non-null only for the 1 cell showing the placement preview icon.
    // All other cells receive null and are not marked for recomposition when the selection changes.
    previewDefenderType: DefenderType?,
    // True only for the single tile under the keyboard placement/targeting cursor (support object
    // placement or spell targeting). Renders a distinct bright cursor border/tint so keyboard users
    // can see which tile the place key will act on.
    isKeyboardPlacementCursor: Boolean = false,
    // Non-null only for the single hovered tile that is a valid placement target for the currently
    // selected support object. Drives the ghost preview so support-placed barricades/traps show the
    // same preview as their tower-placed counterparts.
    supportObjectPreviewType: SupportObjectType? = null,
    // Non-null for every tile that is a valid placement target for the currently selected support
    // object (barricade / dwarven trap / magical trap). Drives the same range-style tile highlight
    // (border + diagonal stripes) that a tower shows when placing the equivalent item, but without
    // the tower's range restriction.
    supportObjectPlacementHighlightType: SupportObjectType? = null,
    // True when a fief support token is selected and this tile is a valid placement target.
    supportFiefPlacementHighlight: Boolean = false,
    isDefenderSelected: Boolean,
    isTargetSelected: Boolean,
    isInSelectedEnemyPath: Boolean = false,
    isInObsidianProtectorAura: Boolean = false,
    selectedDefenderId: Int?,
    selectedMineAction: MineAction?,
    selectedWizardAction: WizardAction? = null,
    selectedBarricadeAction: BarricadeAction? = null,
    targetCircleInfo: TargetCircleInfo?,
    // True when this tile is affected by the currently selected tower attack (the selected target
    // for single-target attacks, or a tile within the blast area for AREA/LASTING attacks). Used to
    // gate the enemy attack damage/lethality/immunity preview (issue #591).
    isSelectedAttackTarget: Boolean = false,
    onClick: () -> Unit,
    hexSize: androidx.compose.ui.unit.Dp = 48.dp,
    onHoverChange: ((Boolean) -> Unit)? = null,
    useTransparentBackground: Boolean = false,
    terrainIsSeparateLayer: Boolean = false,
    // Sandbox runtime map edits mutate the non-observable `level` field in place, so Compose cannot
    // detect them by reading gameState.level. These two parameters carry the repainted tile type and
    // river flow for this exact cell; when either changes (e.g. river -> different river flow, or
    // river -> NO_PLAY) the parameter comparison differs and Compose recomposes this cell immediately,
    // re-reading the updated level. Without them a repaint that leaves other parameters unchanged
    // (notably repainting one river tile over another) would be skipped and never re-render.
    sandboxPaintedType: de.egril.defender.editor.TileType? = null,
    sandboxPaintedRiverTile: RiverTile? = null,
    // Pre-resolved effect values passed from GameGrid (O(1) map lookups instead of O(N) searches)
    healingEffect: HealingEffect? = null,
    damageEffect: DamageEffect? = null,
    deathEffect: EnemyDeathEffect? = null,
    coinGainEffect: CoinGainEffect? = null,
    towerAttackEffect: TowerAttackEffect? = null,
    fieldEffect: FieldEffect? = null,
    trap: Trap? = null,
    barricade: Barricade? = null,
    fief: de.egril.defender.model.Fief? = null,
    mushroom: de.egril.defender.model.Mushroom? = null,
    gargoyle: de.egril.defender.model.Gargoyle? = null,
    constructionCompleteEffect: TowerConstructionEffect? = null,
    enemySpawnEffect: EnemySpawnEffect? = null,
    trapTriggerEffect: TrapTriggerEffect? = null,
    enemyMoveEffect: EnemyMoveEffect? = null,
    dragonLevelChangeEffect: DragonLevelChangeEffect? = null,
    mineDigEffect: MineDigEffect? = null,
    arrowAttackEffect: ArrowAttackEffect? = null,
    isArrowTargetTile: Boolean = false,
    isBallistaTargetTile: Boolean = false,
    isBowTargetTile: Boolean = false,
    isSpearTargetTile: Boolean = false,
    isPikeTargetTile: Boolean = false,
    isWizardTargetTile: Boolean = false,
    isAlchemyTargetTile: Boolean = false,
    isInWizardAttackArea: Boolean = false,
    isInAlchemyAttackArea: Boolean = false,
) {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value

    val isSpawnPoint = gameState.level.isSpawnPoint(position)
    val isTarget = gameState.level.isTargetPosition(position)
    val isNextOrderedTarget = gameState.getNextSingleHitTargetPosition() == position
    val isOnPath = gameState.level.isOnPath(position)
    val isBuildArea = gameState.level.isBuildArea(position)
    val isRiverTile = gameState.level.isRiverTile(position)
    // Shorthand combinations used for attack targeting and spell area checks
    val isEnemyTraversable = isOnPath || isSpawnPoint
    val isEnemyOccupiable = isOnPath || isSpawnPoint || isRiverTile
    // defender and attacker are now passed as parameters (pre-computed in GameGrid)

    // Check if a dragon is targeting the mine at this position
    val dragonIsTargetingMine =
        defender != null &&
            defender.type == DefenderType.DWARVEN_MINE &&
            gameState.attackers.any { it.targetMineId.value == defender.id && !it.isDefeated.value }

    // Determine the tile type for background image loading. Prefer the sandbox-painted river tile so
    // a runtime repaint (which mutates the non-observable level in place) is reflected immediately.
    val riverTile = displayedRiverTile(gameState.level.getRiverTile(position), sandboxPaintedRiverTile)
    val isMaelstrom = riverTile?.flowDirection == RiverFlow.MAELSTROM

    val tileType =
        sandboxPaintedType
            ?: when {
                isSpawnPoint -> de.egril.defender.editor.TileType.SPAWN_POINT
                isTarget -> de.egril.defender.editor.TileType.TARGET
                isRiverTile -> de.egril.defender.editor.TileType.RIVER
                isOnPath -> de.egril.defender.editor.TileType.PATH
                isBuildArea -> de.egril.defender.editor.TileType.BUILD_AREA
                else -> de.egril.defender.editor.TileType.NO_PLAY
            }

    // Get tile background painter (will be null if images are disabled or not available)
    // Suppress tile image when unit backgrounds are ON so the colored background is visible:
    // - For enemy tiles: the red background must not be covered by the tile texture.
    // - For ready towers on build areas: use the colored tower background instead.
    // When unit backgrounds are OFF (transparent), always show the tile image so units blend into terrain.
    val shouldShowTileImage =
        !(
            AppSettings.showUnitTowerBackground.value &&
                (attacker != null || (defender != null && defender.isReady && isBuildArea))
        )
    val tilePainter =
        if (shouldShowTileImage && (!useTransparentBackground || (isMaelstrom && !terrainIsSeparateLayer))) {
            TileImageProvider.getTilePainter(tileType, isMaelstrom = isMaelstrom)
        } else {
            null
        }

    // When animations are enabled, delay showing fireball / acid field-effect markers on the tile
    // until the attack animation has finished (projectile flight + impact flash).
    // When animations are off, or when the effect is from a prior turn (no matching attack overlay
    // is active), show it immediately.
    val animate = AppSettings.enableAnimations.value
    var showFieldEffect by remember { mutableStateOf(false) }
    if (fieldEffect != null || isInWizardAttackArea || isInAlchemyAttackArea) {
        LaunchedEffect(
            fieldEffect?.position?.x,
            fieldEffect?.position?.y,
            fieldEffect?.type,
            fieldEffect?.defenderId,
            isInWizardAttackArea,
            isInAlchemyAttackArea,
            animate,
        ) {
            when {
                fieldEffect == null -> showFieldEffect = false
                animate && isInWizardAttackArea -> {
                    showFieldEffect = false
                    kotlinx.coroutines.delay(
                        GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS +
                            GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS,
                    )
                    showFieldEffect = true
                }
                animate && isInAlchemyAttackArea -> {
                    showFieldEffect = false
                    kotlinx.coroutines.delay(
                        GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS +
                            GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS,
                    )
                    showFieldEffect = true
                }
                else -> showFieldEffect = true
            }
        }
    } else {
        SideEffect { showFieldEffect = false }
    }
    // Use effectiveFieldEffect for all visual rendering; raw fieldEffect is still used for
    // gameplay logic (trap placement detection etc.) so the game state stays accurate.
    val effectiveFieldEffect: FieldEffect? = if (showFieldEffect) fieldEffect else null

    val gridCellState =
        GridCellState(
            position = position,
            gameState = gameState,
            defender = defender,
            attacker = attacker,
            selectedDefender = selectedDefender,
            selectedDefenderId = selectedDefenderId,
            isHovering = isHovering,
            isEnemyTraversable = isEnemyTraversable,
            isOnPath = isOnPath,
            fieldEffect = fieldEffect,
            trap = trap,
            fief = fief,
            mushroom = mushroom,
            gargoyle = gargoyle,
            targetCircleInfo = targetCircleInfo,
            selectedMineAction = selectedMineAction,
            selectedWizardAction = selectedWizardAction,
            selectedBarricadeAction = selectedBarricadeAction,
            isSelectedAttackTarget = isSelectedAttackTarget,
        )
    val isInCoolingArea = gridCellState.isInCoolingArea
    val coolingAreaTurnsRemaining = gridCellState.coolingAreaTurnsRemaining
    val isCoolingSpellPreview = gridCellState.isCoolingSpellPreview
    val bombEffect = gridCellState.bombEffect
    val bombExplosion = gridCellState.bombExplosion
    val spellTargeting = gridCellState.spellTargeting
    val isValidSpellTarget = gridCellState.isValidSpellTarget
    val cellIsInRange = gridCellState.cellIsInRange
    val cellIsInDoubleReachOnlyRange = gridCellState.cellIsInDoubleReachOnlyRange

    val attackPreview = gridCellState.attackPreview

    val showTrapPreview = gridCellState.showTrapPreview

    // showPlacementPreview, isBuildableAndEmpty, and canBeUsedAsTowerBase are pre-computed
    // in GameGrid's content lambda and passed as parameters (see GameGrid { position -> } block).

    val cellIsInBarricadeRange = gridCellState.cellIsInBarricadeRange
    val showBarricadePreview = gridCellState.showBarricadePreview
    val cellIsValidForMineTrapPlacement = gridCellState.cellIsValidForMineTrapPlacement
    val cellIsValidForMagicalTrapPlacement = gridCellState.cellIsValidForMagicalTrapPlacement

    // Support-object placement highlight (barricade / dwarven trap / magical trap placed from the
    // support bar). Mirrors the tower-placement range highlight above, but the set of valid tiles
    // is supplied by the caller (supportObjectPlacementHighlightType) and has no range restriction.
    val cellIsSupportBarricadePlacement = supportObjectPlacementHighlightType == SupportObjectType.BARRICADE
    val cellIsSupportDwarvenTrapPlacement = supportObjectPlacementHighlightType == SupportObjectType.DWARVEN_TRAP
    val cellIsSupportMagicalTrapPlacement = supportObjectPlacementHighlightType == SupportObjectType.MAGICAL_TRAP

    // Base background color based on area type - ALWAYS visible
    // Build areas adjacent to path allow tower placement
    val baseBackgroundColor = terrainBackgroundColor(gameState.level, position)

    // Check if attacker on this tile is frozen (freeze spell)
    val attackerIsFrozen =
        attacker != null &&
            gameState.activeSpellEffects.any {
                it.spell == SpellType.FREEZE_SPELL && it.attackerId == attacker.id
            }

    // Check if cooling spell reduces this attacker's movement to 0
    val coolingReducesAttackerToZero =
        attacker != null &&
            isInCoolingArea &&
            run {
                val penalizedSpeed = maxOf(1, attacker.currentBaseMovementSpeed - attacker.movementPenalty.value)
                maxOf(0, penalizedSpeed - 1) == 0
            }

    // Monotonic attack trigger counter: increments on every individual attack, even when the same
    // tile is attacked multiple times in a turn (bypasses the per-tile deduplication in
    // towerAttackEffects).  Using this as a LaunchedEffect key guarantees the suppression
    // coroutine re-fires for every new attack, not just the first one per tile.
    val towerAttackCount =
        if (animate) {
            gameState.attackTriggerCount.value
        } else {
            0
        }

    // Suppress the red background on enemy tiles during attack animations.
    // * Directly targeted / AoE tiles: suppress for the precise projectile flight + impact window.
    // * All other live enemy tiles: suppress for the maximum possible animation duration so all
    //   enemies visually "de-highlight" while any attack is running, then restore automatically.
    //   towerAttackCount is used as a key so this fires for EVERY new attack (unlike a boolean
    //   which only changes from false → true once per turn).
    val anyTowerAttackActive = towerAttackCount > 0
    var suppressEnemyBackground by remember { mutableStateOf(false) }
    if (attacker != null) {
        LaunchedEffect(
            towerAttackEffect?.turnNumber,
            towerAttackEffect?.targetPosition,
            isInWizardAttackArea,
            isInAlchemyAttackArea,
            towerAttackCount,
            animate,
        ) {
            if (!animate) {
                suppressEnemyBackground = false
                return@LaunchedEffect
            }
            val hasDirectAttack = towerAttackEffect != null
            val hasAoEAttack = isInWizardAttackArea || isInAlchemyAttackArea
            if (hasDirectAttack || hasAoEAttack) {
                // Directly targeted or AoE tile: use precise per-tower-type timing.
                suppressEnemyBackground = true
                val flightDelay: Long =
                    when {
                        towerAttackEffect != null && isArrowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isBallistaTargetTile -> GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isBowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isSpearTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isPikeTargetTile -> GamePlayConstants.AnimationTimings.PIKE_EXTEND_DELAY_MS
                        towerAttackEffect != null && isWizardTargetTile -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isAlchemyTargetTile -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                        isInWizardAttackArea -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                        isInAlchemyAttackArea -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                        else -> 0L
                    }
                kotlinx.coroutines.delay(flightDelay + GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS)
                suppressEnemyBackground = false
            } else if (anyTowerAttackActive) {
                // Non-targeted live enemy tile: suppress for the maximum possible animation duration
                // so it de-highlights with all other enemies, then restores automatically.
                suppressEnemyBackground = true
                kotlinx.coroutines.delay(
                    GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS +
                        GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS,
                )
                suppressEnemyBackground = false
            } else {
                suppressEnemyBackground = false
            }
        }
    } else {
        SideEffect {
            suppressEnemyBackground = false
        }
    }

    val isDeathEffectActive = deathEffect != null && attacker == null
    val enemyBgSuppressed = suppressEnemyBackground

    // Apply slight tint for selection states, but keep base color visible
    // Override with red background for enemy units and colored background for defenders
    // During INITIAL_BUILDING phase, don't apply any selection tints
    // Field effects also modify the background color
    // Special case: Keep river background visible for defenders on rafts
    val backgroundColor =
        when {
            // Keyboard placement/targeting cursor — bright cyan tint so the active tile stands out.
            isKeyboardPlacementCursor -> Color(0xFF00E5FF).copy(alpha = 0.45f)
            isInSelectedEnemyPath -> Color(0xFF00E5FF).copy(alpha = 0.28f)
            attackerIsFrozen || coolingReducesAttackerToZero -> TargetCircleConstants.COOLING_SPELL_COLOR.copy(alpha = 0.5f) // Turquoise background for frozen/cooled-to-zero enemies
            isInObsidianProtectorAura -> Color(0xFF6D3B91).copy(alpha = 0.34f)
            attacker != null && enemyBgSuppressed -> if (useTransparentBackground) Color.Transparent else baseBackgroundColor
            attacker != null ->
                if (AppSettings.showUnitTowerBackground.value) {
                    GamePlayColors.Error
                } else if (useTransparentBackground) {
                    Color.Transparent
                } else {
                    baseBackgroundColor
                }
            defender != null && isRiverTile -> {
                // Keep river blue background visible for defenders on rafts only when unit backgrounds are enabled.
                // When unit backgrounds are off: transparent if level map is shown (shows through), otherwise river color.
                if (!AppSettings.showUnitTowerBackground.value && useTransparentBackground) Color.Transparent else GamePlayColors.River
            }
            defender != null ->
                if (AppSettings.showUnitTowerBackground.value) {
                    when {
                        !defender.isReady -> GamePlayColors.Building
                        defender.actionsRemaining.value > 0 -> GamePlayColors.Info
                        else -> GamePlayColors.InfoLight
                    }
                } else {
                    // When unit backgrounds are off: transparent if level map is shown (level map visible through tile),
                    // otherwise use terrain color so the tower tile blends with its surroundings instead of showing
                    // the Material theme's white surface color.
                    if (useTransparentBackground) Color.Transparent else baseBackgroundColor
                }

            effectiveFieldEffect != null -> {
                when (effectiveFieldEffect.type) {
                    FieldEffectType.FIREBALL -> GamePlayColors.Warning.copy(alpha = 0.5f) // Orange tint for fireball
                    FieldEffectType.ACID -> GamePlayColors.Success.copy(alpha = 0.6f) // Green tint for acid
                    FieldEffectType.WEB -> Color(0xFF8E7CC3).copy(alpha = 0.5f) // Violet tint for spider web
                    FieldEffectType.BURNING_TILE -> Color(0xFFFF4500).copy(alpha = 0.55f) // Red-orange for burning tile
                    FieldEffectType.SHADOW_FOG -> Color(0xFF2A003A).copy(alpha = 0.65f) // Black-violet fog
                }
            }

            trap != null -> GamePlayColors.Trap.copy(alpha = 0.6f) // Brown tint for trap

            barricade != null -> Color(0xFF795548).copy(alpha = 0.5f) // Brown tint for barricade

            // Bomb explosion overlay - bright orange/red when explosion is happening
            bombExplosion != null -> Color(0xFFFF3D00).copy(alpha = 0.7f) // Bright red-orange for explosion

            // Active bomb on tile - dark red/amber tint with countdown
            bombEffect != null -> Color(0xFFFF6F00).copy(alpha = 0.4f) // Amber tint for bomb

            // Barricade placement range - yellow tint for tiles in range
            cellIsInBarricadeRange || cellIsSupportBarricadePlacement -> GamePlayColors.Yellow.copy(alpha = 0.3f) // Light yellow for barricade placement range

            // Tower placement preview - highlight the hovered build tile differently than range tiles
            showPlacementPreview -> GamePlayColors.Yellow.copy(alpha = 0.4f) // Light yellow for the build tile being hovered
            isInPreviewRange -> GamePlayColors.Success.copy(alpha = 0.2f) // Very light green for range preview tiles

            // Spell targeting highlight - purple tint for valid spell target position tiles
            // Not shown for fear spells (target circles provide the visual indicator)
            isValidSpellTarget &&
                spellTargeting?.activeSpell != SpellType.FEAR_SPELL &&
                spellTargeting?.activeSpell != SpellType.FEAR_SPELL_AREA -> Color(0xFF9C27B0).copy(alpha = 0.25f) // Light purple for valid spell target positions

            isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.7f)
            isTargetSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.8f)
            else -> baseBackgroundColor // No selection highlighting during placement or in initial phase
        }

    val finalBackgroundColor =
        if (useTransparentBackground &&
            attacker == null &&
            defender == null &&
            effectiveFieldEffect == null &&
            trap == null &&
            barricade == null &&
            bombEffect == null &&
            bombExplosion == null
        ) {
            Color.Transparent
        } else {
            backgroundColor
        }

    // Border color - use borders to indicate entities instead of background
    // For range visualization, show green border on path tiles OR river tiles in range (only if tower has actions)
    val showRange =
        if (selectedDefenderId != null) {
            selectedDefender?.isReady == true && selectedDefender.actionsRemaining.value > 0
        } else {
            false
        }

    // When placing trap, don't show green border on tiles with enemies
    val isTrapPlacement = selectedMineAction == MineAction.BUILD_TRAP || selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP
    val hasEnemy = attacker != null
    val canPlaceTrapHere = !hasEnemy || !isTrapPlacement

    // Check if defender has area attack capability
    val hasAreaAttack =
        if (selectedDefenderId != null) {
            selectedDefender?.type?.attackType == AttackType.AREA || selectedDefender?.type?.attackType == AttackType.LASTING
        } else {
            false
        }

    // Enemy-occupiable tiles are valid targets for area attacks; enemy-traversable + bridges for single-target
    val isValidTargetTile =
        if (hasAreaAttack) {
            isEnemyOccupiable
        } else {
            isEnemyTraversable || gameState.isBridgeAt(position)
        }

    val borderColor =
        when {
            // Keyboard placement/targeting cursor — bright cyan border for the active tile.
            isKeyboardPlacementCursor -> Color(0xFF00B8D4)
            isInSelectedEnemyPath -> Color(0xFF00B8D4)
            // Tower placement preview - dashed borders for preview (we'll handle this with Canvas later)
            showPlacementPreview -> GamePlayColors.Yellow // Yellow border for hovered build tile
            isInPreviewRange -> GamePlayColors.Success // Green border for range preview tiles

            // Barricade and trap placement range - brown borders (light brown diagonal stripes)
            cellIsInBarricadeRange ||
                cellIsValidForMineTrapPlacement ||
                cellIsSupportBarricadePlacement ||
                cellIsSupportDwarvenTrapPlacement -> GamePlayColors.TrapPlacementHighlight // Brown border for barricade/trap placement range

            // Magical trap placement range - lilac borders
            cellIsValidForMagicalTrapPlacement || cellIsSupportMagicalTrapPlacement -> GamePlayColors.MagicalTrapPlacementHighlight // Lilac border for magical trap placement range

            // Buildable tile highlighting - lighter green borders with dashed line when tower type is selected
            isBuildableAndEmpty || canBeUsedAsTowerBase -> GamePlayColors.BuildableHighlight // Lighter green border for buildable tiles and tower bases

            // Double-reach-only tiles: thin purple solid border
            cellIsInDoubleReachOnlyRange && isValidTargetTile && showRange && canPlaceTrapHere -> SpellDoubleReachColor

            cellIsInRange && isValidTargetTile && showRange && canPlaceTrapHere -> GamePlayColors.Success // Green border for tiles in range (path or river for area attacks)
            isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> GamePlayColors.Yellow // Yellow border for selected defender (not during initial building)

            // Spell targeting highlight - purple border for valid spell targets (enemies, towers, positions)
            // Not shown for fear spells (target circles provide the visual indicator)
            isValidSpellTarget &&
                spellTargeting?.activeSpell != SpellType.FEAR_SPELL &&
                spellTargeting?.activeSpell != SpellType.FEAR_SPELL_AREA -> Color(0xFF9C27B0) // Purple border for valid spell targets
            isDangerous -> GamePlayColors.Error

            isSpawnPoint -> GamePlayColors.WarningDark // Darker orange border for spawn in dark mode
            isNextOrderedTarget -> Color(0xFFFFC107)
            isTarget -> GamePlayColors.Success // Green border for target (adapts to dark mode automatically)
            attacker != null && !enemyBgSuppressed -> if (AppSettings.showUnitTowerBackground.value) GamePlayColors.ErrorDark else Color.Transparent // Darker red border for enemies (only when background enabled)
            defender != null ->
                if (AppSettings.showUnitTowerBackground.value) {
                    if (defender.isReady) GamePlayColors.InfoDark else GamePlayColors.Building
                } else {
                    Color.Transparent
                } // Darker blue/gray border for towers (only when background enabled)
            effectiveFieldEffect != null -> {
                when (effectiveFieldEffect.type) {
                    FieldEffectType.FIREBALL -> GamePlayColors.WarningDeep // Deep orange border for fireball
                    FieldEffectType.ACID -> GamePlayColors.Success // Green border for acid
                    FieldEffectType.WEB -> Color(0xFF5E35B1) // Deep violet border for spider web
                    FieldEffectType.BURNING_TILE -> Color(0xFFCC2200) // Deep red border for burning tile
                    FieldEffectType.SHADOW_FOG -> Color(0xFF4A148C) // Deep purple border for shadow fog
                }
            }

            trap != null -> GamePlayColors.Trap // Brown border for trap
            barricade != null -> Color(0xFF795548) // Brown border for barricade
            else -> Color.Transparent // No borders for empty cells
        }

    // Thicker borders for important elements
    val borderWidth =
        when {
            isKeyboardPlacementCursor -> 6.dp // Prominent border for the keyboard placement/targeting cursor
            isInSelectedEnemyPath -> 4.dp
            showPlacementPreview -> 6.dp // Double thickness for hovered build tile
            isInPreviewRange -> 3.dp // Medium border for range preview
            cellIsInBarricadeRange ||
                cellIsValidForMineTrapPlacement ||
                cellIsValidForMagicalTrapPlacement ||
                cellIsSupportBarricadePlacement ||
                cellIsSupportDwarvenTrapPlacement ||
                cellIsSupportMagicalTrapPlacement -> 3.dp // Medium border for trap/barricade placement range
            isBuildableAndEmpty || canBeUsedAsTowerBase -> 3.dp // Medium border for buildable tiles and tower bases
            isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> 5.dp // Extra thick border for selected defender (not during initial building)
            cellIsInDoubleReachOnlyRange && isValidTargetTile && showRange && canPlaceTrapHere -> 2.dp // Thin purple border for double-reach-only tiles
            cellIsInRange && isValidTargetTile && showRange && canPlaceTrapHere -> 4.dp // Thick border for cells in range (path or river for area attacks)
            isValidSpellTarget &&
                spellTargeting?.activeSpell != SpellType.FEAR_SPELL &&
                spellTargeting?.activeSpell != SpellType.FEAR_SPELL_AREA -> 4.dp // Thick purple border for valid spell targets
            isDangerous -> 4.dp
            isNextOrderedTarget -> 5.dp
            isSpawnPoint || isTarget -> 3.dp
            (attacker != null || defender != null) && AppSettings.showUnitTowerBackground.value -> 3.dp
            effectiveFieldEffect != null -> 3.dp // Thick border for field effects
            trap != null -> 3.dp // Thick border for trap
            barricade != null -> 3.dp // Thick border for barricade
            else -> 0.dp // No border for empty cells
        }

    // Flag to indicate dashed border (for preview and buildable tiles)
    val useDashedBorder =
        showPlacementPreview ||
            isInPreviewRange ||
            isBuildableAndEmpty ||
            canBeUsedAsTowerBase ||
            cellIsInBarricadeRange ||
            cellIsValidForMineTrapPlacement ||
            cellIsValidForMagicalTrapPlacement ||
            cellIsSupportBarricadePlacement ||
            cellIsSupportDwarvenTrapPlacement ||
            cellIsSupportMagicalTrapPlacement

    val showDiagonalStripes =
        isBuildableAndEmpty ||
            canBeUsedAsTowerBase ||
            cellIsInBarricadeRange ||
            cellIsValidForMineTrapPlacement ||
            cellIsValidForMagicalTrapPlacement ||
            cellIsSupportBarricadePlacement ||
            cellIsSupportDwarvenTrapPlacement ||
            cellIsSupportMagicalTrapPlacement

    // Determine if we should use gradient blending
    val useTileImages = de.egril.defender.ui.settings.AppSettings.useTileImages.value
    val useTileSmoothTransitions = de.egril.defender.ui.settings.AppSettings.useTileSmoothTransitions.value
    val shouldUseGradientBlending = useTileImages && useTileSmoothTransitions && tilePainter != null

    // Helper function to get tile type for a position
    val getNeighborTileType: (Position) -> de.egril.defender.editor.TileType? = { pos ->
        if (pos.x < 0 ||
            pos.x >= gameState.level.gridWidth ||
            pos.y < 0 ||
            pos.y >= gameState.level.gridHeight
        ) {
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
    val neighborTileTypes =
        remember(position, gameState.defenders.size, gameState.level) {
            if (!shouldUseGradientBlending) {
                emptyMap()
            } else {
                val neighbors = position.getHexNeighbors()
                neighbors
                    .mapNotNull { neighborPos ->
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
    val neighborPainters =
        neighborTileTypes.mapValues { (pos, type) ->
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

    GridCellFrame(
        hexSize = hexSize,
        position = position,
        tileType = tileType,
        backgroundColor = finalBackgroundColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
        useDashedBorder = useDashedBorder,
        backgroundPainter = tilePainter,
        onClick = onClick,
        onHover = onHoverChange,
        shouldUseGradientBlending = shouldUseGradientBlending,
        getNeighborTileType = getNeighborTileType,
        neighborPainters = neighborPainters,
    ) {
        GridCellContent(
            position = position,
            gameState = gameState,
            attacker = attacker,
            healingEffect = healingEffect,
            damageEffect = damageEffect,
            defender = defender,
            riverTile = riverTile,
            fieldEffect = effectiveFieldEffect,
            trap = trap,
            barricade = barricade,
            fief = fief,
            mushroom = mushroom,
            gargoyle = gargoyle,
            isSpawnPoint = isSpawnPoint,
            isTarget = isTarget,
            isRiverTile = isRiverTile,
            showPlacementPreview = showPlacementPreview,
            showBarricadePreview = showBarricadePreview,
            supportObjectPreviewType = supportObjectPreviewType,
            previewDefenderType = previewDefenderType,
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
            coolingAreaTurnsRemaining = coolingAreaTurnsRemaining,
            isCoolingSpellPreview = isCoolingSpellPreview,
            bombEffect = bombEffect,
            bombExplosion = bombExplosion,
            deathEffect = deathEffect,
            coinGainEffect = coinGainEffect,
            towerAttackEffect = towerAttackEffect,
            constructionCompleteEffect = constructionCompleteEffect,
            enemySpawnEffect = enemySpawnEffect,
            trapTriggerEffect = trapTriggerEffect,
            enemyMoveEffect = enemyMoveEffect,
            dragonLevelChangeEffect = dragonLevelChangeEffect,
            mineDigEffect = mineDigEffect,
            arrowAttackEffect = arrowAttackEffect,
            isArrowTargetTile = isArrowTargetTile,
            isBallistaTargetTile = isBallistaTargetTile,
            isBowTargetTile = isBowTargetTile,
            isSpearTargetTile = isSpearTargetTile,
            isPikeTargetTile = isPikeTargetTile,
            isWizardTargetTile = isWizardTargetTile,
            isAlchemyTargetTile = isAlchemyTargetTile,
            isInWizardAttackArea = isInWizardAttackArea,
            isInAlchemyAttackArea = isInAlchemyAttackArea,
            dragonIsTargetingMine = dragonIsTargetingMine,
            suppressEnemyBackground = suppressEnemyBackground,
            attackPreview = attackPreview,
            isDangerous = isDangerous,
            isInSelectedEnemyPath = isInSelectedEnemyPath,
            mapZoom = mapZoom,
        )
    }
}
