package de.egril.defender.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import de.egril.defender.model.*
import de.egril.defender.ui.animations.ArrowAttackAnimation
import de.egril.defender.ui.animations.BombExplosionAnimation
import de.egril.defender.ui.animations.DragonLevelChangeAnimation
import de.egril.defender.ui.animations.DragonTargetAnimation
import de.egril.defender.ui.animations.EnemyMoveAnimation
import de.egril.defender.ui.animations.EnemySpawnAnimation
import de.egril.defender.ui.animations.TowerAttackImpactAnimation
import de.egril.defender.ui.animations.TrapTriggerAnimation
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.atan2

/**
 * Tile-level animation and overlay layers drawn on top of a grid cell's content.
 */
@Composable
internal fun BoxScope.GridCellAnimations(
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
    bombExplosion: BombExplosionEffect?,
    enemySpawnEffect: EnemySpawnEffect?,
    trapTriggerEffect: TrapTriggerEffect?,
    enemyMoveEffect: EnemyMoveEffect?,
    dragonLevelChangeEffect: DragonLevelChangeEffect?,
    arrowAttackEffect: ArrowAttackEffect?,
    dragonIsTargetingMine: Boolean,
    bombEffect: ActiveSpellEffect?,
    isSpawnPoint: Boolean,
    isTarget: Boolean,
    isRiverTile: Boolean,
    targetCircleInfo: TargetCircleInfo?,
    useDashedBorder: Boolean,
    borderColor: Color,
    borderWidth: Dp,
    hexSize: Dp,
    showDiagonalStripes: Boolean,
    isInCoolingArea: Boolean,
    coolingAreaTurnsRemaining: Int?,
    isCoolingSpellPreview: Boolean,
    previewDefenderType: DefenderType?,
    mapZoom: () -> Float,
    showTrapPreview: Boolean,
    supportObjectPreviewType: SupportObjectType?,
    selectedMineAction: MineAction?,
    selectedWizardAction: WizardAction?,
) {
    TileStatusOverlays(
        gameState = gameState,
        position = position,
        isInCoolingArea = isInCoolingArea,
        coolingAreaTurnsRemaining = coolingAreaTurnsRemaining,
        isCoolingSpellPreview = isCoolingSpellPreview,
        previewDefenderType = previewDefenderType,
        mapZoom = mapZoom,
        showTrapPreview = showTrapPreview,
        supportObjectPreviewType = supportObjectPreviewType,
        selectedMineAction = selectedMineAction,
        selectedWizardAction = selectedWizardAction,
    )

    // Show bomb explosion animation overlay on affected tiles (highest priority, above everything)
    val shouldPlayBombExplosionAnimation =
        rememberShouldPlayOneShotTileAnimation(
            gameState,
            bombExplosion?.let {
                oneShotTileAnimationKey("bomb_explosion", position, it.turnNumber, "${it.center.x},${it.center.y}")
            },
        )
    if (bombExplosion != null && shouldPlayBombExplosionAnimation) {
        BombExplosionAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(20f),
        )
    }

    val towerImpactAnimationKey =
        towerAttackEffect?.let {
            oneShotTileAnimationKey("tower_attack_impact", it.targetPosition, it.turnNumber)
        }
    TileDeathAndCoinAnimations(
        position = position,
        gameState = gameState,
        attacker = attacker,
        deathEffect = deathEffect,
        coinGainEffect = coinGainEffect,
        towerAttackEffect = towerAttackEffect,
        isArrowTargetTile = isArrowTargetTile,
        isBallistaTargetTile = isBallistaTargetTile,
        isBowTargetTile = isBowTargetTile,
        isSpearTargetTile = isSpearTargetTile,
        isPikeTargetTile = isPikeTargetTile,
        isWizardTargetTile = isWizardTargetTile,
        isAlchemyTargetTile = isAlchemyTargetTile,
        isInWizardAttackArea = isInWizardAttackArea,
        isInAlchemyAttackArea = isInAlchemyAttackArea,
        displayedHealth = displayedHealth,
        isDeathEffectActive = isDeathEffectActive,
        towerImpactAnimationKey = towerImpactAnimationKey,
    )

    // Show tower attack impact overlay when this tile was attacked.
    // When a projectile is targeting this tile the hit animation is delayed
    // so the projectile visibly arrives before the impact flash.
    val shouldPlayTowerImpactAnimation =
        rememberShouldPlayOneShotTileAnimation(gameState, towerImpactAnimationKey)
    var showHitAnimation by remember(towerImpactAnimationKey) {
        mutableStateOf(
            shouldPlayTowerImpactAnimation &&
                towerAttackEffect != null &&
                !isArrowTargetTile &&
                !isBallistaTargetTile &&
                !isBowTargetTile &&
                !isSpearTargetTile &&
                !isPikeTargetTile &&
                !isWizardTargetTile &&
                !isAlchemyTargetTile,
        )
    }
    if (towerAttackEffect != null) {
        LaunchedEffect(
            towerImpactAnimationKey,
            isArrowTargetTile,
            isBallistaTargetTile,
            isBowTargetTile,
            isSpearTargetTile,
            isPikeTargetTile,
            isWizardTargetTile,
            isAlchemyTargetTile,
            shouldPlayTowerImpactAnimation,
        ) {
            if (!shouldPlayTowerImpactAnimation) {
                showHitAnimation = false
                return@LaunchedEffect
            }
            when {
                isArrowTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS)
                    showHitAnimation = true
                }
                isBallistaTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS)
                    showHitAnimation = true
                }
                isBowTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS)
                    showHitAnimation = true
                }
                isSpearTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS)
                    showHitAnimation = true
                }
                isPikeTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.PIKE_EXTEND_DELAY_MS)
                    showHitAnimation = true
                }
                isWizardTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS)
                    showHitAnimation = true
                }
                isAlchemyTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS)
                    showHitAnimation = true
                }
                else -> {
                    showHitAnimation = true
                }
            }
        }
    } else {
        SideEffect { showHitAnimation = false }
    }
    if (showHitAnimation && towerAttackEffect != null) {
        TowerAttackImpactAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(17f),
        )
    }

    // Show enemy spawn portal overlay when an enemy just appeared at this position
    val shouldPlayEnemySpawnAnimation =
        rememberShouldPlayOneShotTileAnimation(
            gameState,
            enemySpawnEffect?.let {
                oneShotTileAnimationKey("enemy_spawn", it.position, it.turnNumber)
            },
        )
    if (enemySpawnEffect != null &&
        gameState.level.isSpawnPoint(enemySpawnEffect.position) &&
        shouldPlayEnemySpawnAnimation &&
        enemySpawnEffect.suppressPortalAnimation != true
    ) {
        EnemySpawnAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(16f),
        )
    }

    // Show trap trigger overlay when a trap was triggered at this position.
    // Uses a high z-index (21) to be visible even when the death animation is also showing
    // (which happens when the trap kills the enemy in the same turn).
    val shouldPlayTrapTriggerAnimation =
        rememberShouldPlayOneShotTileAnimation(
            gameState,
            trapTriggerEffect?.let {
                oneShotTileAnimationKey("trap_trigger", it.position, it.turnNumber)
            },
        )
    if (trapTriggerEffect != null && shouldPlayTrapTriggerAnimation) {
        TrapTriggerAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(21f),
        )
    }

    // Show enemy movement trail when an enemy just left this tile during the enemy turn
    val shouldPlayEnemyMoveAnimation =
        rememberShouldPlayOneShotTileAnimation(
            gameState,
            enemyMoveEffect?.let {
                oneShotTileAnimationKey("enemy_move", it.position, it.turnNumber)
            },
        )
    if (enemyMoveEffect != null && attacker == null && shouldPlayEnemyMoveAnimation) {
        EnemyMoveAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(13f),
        )
    }

    // Show dragon level change flash on the dragon's tile when its level changed
    val shouldPlayDragonLevelChangeAnimation =
        rememberShouldPlayOneShotTileAnimation(
            gameState,
            dragonLevelChangeEffect?.let {
                oneShotTileAnimationKey("dragon_level_change", it.position, it.turnNumber, if (it.isLevelUp) "up" else "down")
            },
        )
    if (dragonLevelChangeEffect != null && shouldPlayDragonLevelChangeAnimation) {
        DragonLevelChangeAnimation(
            animate = AppSettings.enableAnimations.value,
            isLevelUp = dragonLevelChangeEffect.isLevelUp,
            modifier = Modifier.fillMaxSize().zIndex(14f),
        )
    }

    // Show arrow/bolt projectile on the source tower tile for ranged attacks
    val shouldPlayArrowAttackAnimation =
        rememberShouldPlayOneShotTileAnimation(
            gameState,
            arrowAttackEffect?.let {
                oneShotTileAnimationKey(
                    animationName = "arrow_attack",
                    position = it.sourcePosition,
                    turnNumber = it.turnNumber,
                    suffix = "${it.targetPosition.x},${it.targetPosition.y}",
                )
            },
        )
    if (arrowAttackEffect != null && shouldPlayArrowAttackAnimation) {
        val dx = (arrowAttackEffect.targetPosition.x - arrowAttackEffect.sourcePosition.x).toFloat()
        val dy = (arrowAttackEffect.targetPosition.y - arrowAttackEffect.sourcePosition.y).toFloat()
        val angle =
            if (dx == 0f && dy == 0f) {
                0f
            } else {
                (atan2(dy.toDouble(), dx.toDouble()) * GamePlayConstants.AnimationTimings.RADIANS_TO_DEGREES).toFloat()
            }
        ArrowAttackAnimation(
            animate = AppSettings.enableAnimations.value,
            directionAngle = angle,
            isTargetTile = isArrowTargetTile,
            modifier = Modifier.fillMaxSize().zIndex(18f),
        )
    }

    // Show per-tile Lottie arrow animation at the bow attack target tile when the volley arrives.
    // Uses showHitAnimation as the delay trigger (already delayed by ARROW_FLIGHT_DELAY_MS) so
    // the arrow lands visually at the same time the impact flash fires.
    if (showHitAnimation && isBowTargetTile) {
        val bowEffect = gameState.bowAttackEffects.find { it.targetPosition == position }
        if (bowEffect != null) {
            val dx = (bowEffect.targetPosition.x - bowEffect.sourcePosition.x).toFloat()
            val dy = (bowEffect.targetPosition.y - bowEffect.sourcePosition.y).toFloat()
            val angle =
                if (dx == 0f && dy == 0f) {
                    0f
                } else {
                    (atan2(dy.toDouble(), dx.toDouble()) * GamePlayConstants.AnimationTimings.RADIANS_TO_DEGREES).toFloat()
                }
            ArrowAttackAnimation(
                animate = AppSettings.enableAnimations.value,
                directionAngle = angle,
                isTargetTile = true,
                modifier = Modifier.fillMaxSize().zIndex(18f),
            )
        }
    }

    // Show dragon-targeting warning animation on mines that a dragon is approaching
    if (dragonIsTargetingMine) {
        DragonTargetAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(12f),
        )
    }

    // Show small bomb countdown overlay when an enemy is on the same bomb tile
    if (bombEffect != null && attacker != null) {
        Box(
            modifier = Modifier.fillMaxSize().zIndex(15f),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(2.dp)
                        .background(
                            color = Color(0xFFCC0000),
                            shape = androidx.compose.foundation.shape.CircleShape,
                        ).padding(horizontal = 3.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${bombEffect.turnsRemaining}",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }

    TileFrameOverlays(
        position = position,
        gameState = gameState,
        isSpawnPoint = isSpawnPoint,
        isTarget = isTarget,
        isRiverTile = isRiverTile,
        targetCircleInfo = targetCircleInfo,
        useDashedBorder = useDashedBorder,
        borderColor = borderColor,
        borderWidth = borderWidth,
        hexSize = hexSize,
        showDiagonalStripes = showDiagonalStripes,
    )
}
