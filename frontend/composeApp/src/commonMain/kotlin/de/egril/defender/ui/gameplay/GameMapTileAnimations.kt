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
import de.egril.defender.ui.hexagon.BaseGridCell
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
import de.egril.defender.ui.icon.enemy.enemyAttackPreview
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
internal fun BoxScope.TileDeathAndCoinAnimations(
    position: Position,
    gameState: GameState,
    attacker: Attacker?,
    deathEffect: EnemyDeathEffect?,
    coinGainEffect: CoinGainEffect?,
    towerAttackEffect: TowerAttackEffect?,
    isArrowTargetTile: Boolean,
    isBallistaTargetTile: Boolean,
    isBowTargetTile: Boolean,
    isSpearTargetTile: Boolean,
    isPikeTargetTile: Boolean,
    isWizardTargetTile: Boolean,
    isAlchemyTargetTile: Boolean,
    isInWizardAttackArea: Boolean,
    isInAlchemyAttackArea: Boolean,
    displayedHealth: Int,
    isDeathEffectActive: Boolean,
    towerImpactAnimationKey: String?,
) {
    val deathAnimationKey =
        deathEffect?.let {
            oneShotTileAnimationKey("enemy_death", it.position, it.turnNumber)
        }
    val shouldPlayDeathAnimation = rememberShouldPlayOneShotTileAnimation(gameState, deathAnimationKey)
    var showGhost by remember(deathAnimationKey, towerImpactAnimationKey, shouldPlayDeathAnimation) {
        mutableStateOf(isDeathEffectActive && shouldPlayDeathAnimation)
    }
    if (deathEffect != null) {
        LaunchedEffect(deathAnimationKey, towerImpactAnimationKey, shouldPlayDeathAnimation) {
            if (isDeathEffectActive && shouldPlayDeathAnimation) {
                showGhost = true
                val arrowDelay =
                    when {
                        towerAttackEffect != null && isArrowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isBallistaTargetTile -> GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isBowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isSpearTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isPikeTargetTile -> GamePlayConstants.AnimationTimings.PIKE_EXTEND_DELAY_MS
                        towerAttackEffect != null && isWizardTargetTile -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isAlchemyTargetTile -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                        // AoE secondary targets: delay matches the fireball/acid flight time
                        isInWizardAttackArea -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                        isInAlchemyAttackArea -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                        else -> 0L
                    }
                val impactDelay =
                    if (towerAttackEffect != null ||
                        isInWizardAttackArea ||
                        isInAlchemyAttackArea
                    ) {
                        GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS
                    } else {
                        0L
                    }
                kotlinx.coroutines.delay(arrowDelay + impactDelay + GamePlayConstants.AnimationTimings.ENEMY_DEATH_ANIMATION_DURATION_MS)
                showGhost = false
            } else {
                showGhost = false
            }
        }
    } else {
        SideEffect { showGhost = false }
    }
    if (showGhost && isDeathEffectActive && deathEffect != null) {
        EnemyTypeIcon(
            attackerType = deathEffect.attackerType,
            swarmHealthOverride = ghostSwarmCount(deathEffect.attackerType, displayedHealth),
            modifier = Modifier.fillMaxSize().zIndex(15f),
        )
        // Show level badge on top of the ghost icon when level > 1
        if (deathEffect.attackerLevel > 1) {
            Box(
                modifier = Modifier.fillMaxSize().zIndex(15f),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = "${deathEffect.attackerLevel}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    var showDeathAnimation by remember(deathAnimationKey, towerImpactAnimationKey) {
        mutableStateOf(false)
    }
    if (deathEffect != null) {
        LaunchedEffect(deathAnimationKey, towerImpactAnimationKey, shouldPlayDeathAnimation) {
            if (isDeathEffectActive && shouldPlayDeathAnimation) {
                // Wait for both the projectile flight and the impact flash to finish before
                // starting the death animation.  Also handles AoE secondary targets where
                // towerAttackEffect is null but the tile is in the fireball/acid blast area.
                val flightDelay =
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
                val impactDelay =
                    if (towerAttackEffect != null ||
                        isInWizardAttackArea ||
                        isInAlchemyAttackArea
                    ) {
                        GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS
                    } else {
                        0L
                    }
                if (flightDelay > 0L || impactDelay > 0L) {
                    kotlinx.coroutines.delay(flightDelay + impactDelay)
                }
                showDeathAnimation = true
                GlobalSoundManager.playSound(SoundEvent.ENEMY_DESTROYED)
            } else {
                showDeathAnimation = false
            }
        }
    } else {
        SideEffect { showDeathAnimation = false }
    }
    if (showDeathAnimation && isDeathEffectActive) {
        EnemyDeathAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(18f),
        )
    }

    // Show coin gain animation overlay after the full death-animation sequence has finished:
    // arrowDelay + impactDelay + deathDuration + post-death pause.
    val coinAnimationKey =
        coinGainEffect?.let {
            oneShotTileAnimationKey("coin_gain", it.position, it.turnNumber)
        }
    val shouldPlayCoinAnimation = rememberShouldPlayOneShotTileAnimation(gameState, coinAnimationKey)
    var showCoinAnimation by remember(coinAnimationKey, towerImpactAnimationKey) {
        mutableStateOf(false)
    }
    // Track where this tile's coin-gain "bubbling" animation ends, in root coordinates, so a
    // coin-flight animation can start from there. The rising coins finish near the top of the tile
    // (see COIN_BUBBLE_END_HEIGHT_FRACTION), horizontally centered.
    // Keyed on the (stable) tile grid position so it is captured once and not reset to null when
    // a new coin-gain effect appears on the same tile.
    var coinFlightStartPosition by remember(position) {
        mutableStateOf<Offset?>(null)
    }
    // Diameter (px) of the coin-gain "bubbling" coins on this tile, so the fly-to-counter coins can
    // be launched at the same visible size. The Lottie (coin_gain.json) is a 100x100 viewport with
    // 14-unit coins, fitted (ContentScale.Fit) to the tile box, so the coin diameter on screen is
    // COIN_BUBBLE_COIN_SIZE_FRACTION of the tile's smaller dimension.
    var flyingCoinSizePx by remember(position) {
        mutableStateOf(CoinFlightController.DEFAULT_COIN_SIZE_PX)
    }
    if (coinGainEffect != null) {
        Box(
            modifier =
                Modifier.matchParentSize().onGloballyPositioned { coords ->
                    // Captured fresh whenever a coin-gain effect is present on this tile (this Box
                    // only exists then) and re-fired while the map pans/zooms, so the value used at
                    // launch time reflects the tile's current on-screen position.
                    val topLeft = coords.positionInRoot()
                    coinFlightStartPosition =
                        Offset(
                            x = topLeft.x + coords.size.width / 2f,
                            y = topLeft.y + coords.size.height * COIN_BUBBLE_END_HEIGHT_FRACTION,
                        )
                    flyingCoinSizePx =
                        minOf(coords.size.width, coords.size.height) * COIN_BUBBLE_COIN_SIZE_FRACTION
                },
        )
    }
    if (coinGainEffect != null) {
        LaunchedEffect(coinAnimationKey, towerImpactAnimationKey, shouldPlayCoinAnimation) {
            if (!shouldPlayCoinAnimation) {
                showCoinAnimation = false
                return@LaunchedEffect
            }
            val arrowDelay =
                when {
                    towerAttackEffect != null && isArrowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isBallistaTargetTile -> GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isBowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isSpearTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isPikeTargetTile -> GamePlayConstants.AnimationTimings.PIKE_EXTEND_DELAY_MS
                    towerAttackEffect != null && isWizardTargetTile -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isAlchemyTargetTile -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                    // AoE secondary targets have no direct towerAttackEffect but are in the blast area
                    isInWizardAttackArea -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                    isInAlchemyAttackArea -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                    else -> 0L
                }
            val impactDelay =
                if (towerAttackEffect != null ||
                    isInWizardAttackArea ||
                    isInAlchemyAttackArea
                ) {
                    GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS
                } else {
                    0L
                }
            kotlinx.coroutines.delay(
                arrowDelay + impactDelay +
                    GamePlayConstants.AnimationTimings.ENEMY_DEATH_ANIMATION_DURATION_MS +
                    GamePlayConstants.AnimationTimings.COIN_GAIN_DELAY_AFTER_DEATH_MS,
            )
            // Add coins to the player's total in sync with the animation so the counter
            // visually increases when the coin animation plays, not before the attack runs.
            showCoinAnimation = true
            // Launch the "coin fly-to-counter" animation only after the coin-gain (coins bubbling
            // up) animation has played, so the flying coins appear to peel off the end of that
            // animation. The counter is then updated as those coins reach it (via the launch
            // callback), so the number and the arriving coins stay in step.
            if (AppSettings.enableAnimations.value) {
                kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.COIN_GAIN_ANIMATION_DURATION_MS)
            }
            // Credit the coins that are still pending for this reward (guard against the safety
            // flush in completeEnemyTurn() having already credited them). Reserve them out of
            // pending immediately before launching so the flush can't also credit them; the flying
            // coins then add them to the visible total as they land.
            val toAdd = minOf(coinGainEffect.amount, gameState.pendingCoinGains.value)
            if (toAdd > 0) {
                gameState.pendingCoinGains.value -= toAdd
                val source = coinFlightStartPosition
                val launched =
                    if (AppSettings.enableAnimations.value && source != null) {
                        CoinFlightController.launch(
                            source = source,
                            amount = coinGainEffect.amount,
                            coinSizePx = flyingCoinSizePx,
                            creditAmount = toAdd,
                        ) { arrived ->
                            gameState.coins.value += arrived
                        }
                    } else {
                        0
                    }
                // No flying coins launched (animations off, no source position, or the queue is
                // full): credit the reserved coins immediately so the reward is never lost.
                if (launched == 0) {
                    gameState.coins.value += toAdd
                }
            }
        }
    } else {
        SideEffect { showCoinAnimation = false }
    }
    if (showCoinAnimation && coinGainEffect != null) {
        CoinGainAnimation(
            amount = coinGainEffect.amount,
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(19f),
        )
    }
}
