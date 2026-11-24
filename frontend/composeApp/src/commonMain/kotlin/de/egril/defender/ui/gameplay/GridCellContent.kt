package de.egril.defender.ui.gameplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.game.BridgeSystem
import de.egril.defender.game.isProtectedByObsidianProtector
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.animations.AlchemyIdleAnimation
import de.egril.defender.ui.animations.BarricadeDamageAnimation
import de.egril.defender.ui.animations.FearSpellAnimation
import de.egril.defender.ui.animations.FreezeSpellAnimation
import de.egril.defender.ui.animations.GreenWitchHealingAnimation
import de.egril.defender.ui.animations.InstantTowerSpellAnimation
import de.egril.defender.ui.animations.MineDigAnimation
import de.egril.defender.ui.animations.MushroomBuffAnimation
import de.egril.defender.ui.animations.TowerConstructionCompleteAnimation
import de.egril.defender.ui.animations.TowerReadyPulseAnimation
import de.egril.defender.ui.animations.WaterFlowAnimation
import de.egril.defender.ui.animations.WizardIdleAnimation
import de.egril.defender.ui.editor.RiverFlowIndicator
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
import de.egril.defender.ui.icon.enemy.GargoyleIcon
import de.egril.defender.ui.icon.enemy.shouldShowSeafaringPirateBarge
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Content displayed inside a grid cell (separated for reuse between BaseGridCell and GradientBlendedTileCell)
 */
@Composable
internal fun BoxScope.GridCellContent(
    position: Position,
    gameState: GameState,
    attacker: Attacker?,
    isDangerous: Boolean = false,
    healingEffect: HealingEffect?,
    damageEffect: DamageEffect?,
    defender: Defender?,
    riverTile: RiverTile?,
    fieldEffect: FieldEffect?,
    trap: Trap?,
    barricade: Barricade?,
    fief: de.egril.defender.model.Fief? = null,
    mushroom: de.egril.defender.model.Mushroom? = null,
    gargoyle: de.egril.defender.model.Gargoyle? = null,
    isSpawnPoint: Boolean,
    isTarget: Boolean,
    isRiverTile: Boolean,
    showPlacementPreview: Boolean,
    showBarricadePreview: Boolean,
    // Non-null only for the single hovered tile that is a valid placement target for the selected
    // support object; renders the same ghost preview used for tower-placed barricades/traps.
    supportObjectPreviewType: SupportObjectType? = null,
    // previewDefenderType replaces selectedDefenderType: non-null only for the 1 cell showing
    // the placement preview icon, so buy-button clicks don't cascade to all GridCellContent instances.
    previewDefenderType: DefenderType?,
    // Current map zoom; the hover cost label is counter-scaled by it so it keeps its button size on screen.
    mapZoom: () -> Float,
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
    coolingAreaTurnsRemaining: Int? = null,
    isCoolingSpellPreview: Boolean = false,
    bombEffect: ActiveSpellEffect? = null,
    bombExplosion: BombExplosionEffect? = null,
    deathEffect: EnemyDeathEffect? = null,
    coinGainEffect: CoinGainEffect? = null,
    towerAttackEffect: TowerAttackEffect? = null,
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
    dragonIsTargetingMine: Boolean = false,
    suppressEnemyBackground: Boolean = false,
    // Precomputed attack damage/lethality/immunity preview for the enemy on this tile (issue #591).
    // Non-null only when a defender is selected that could attack this enemy.
    attackPreview: EnemyAttackPreview? = null,
    isInSelectedEnemyPath: Boolean = false,
) {
    val bridgeSystem = remember(gameState) { BridgeSystem(gameState) }

    // When animations are enabled, delay updating the enemy's displayed health value until
    // the attack animation (projectile flight + impact flash) has completed.
    // This way the health number on the icon only changes after the impact flash, matching
    // the visual sequence of: projectile arrives → flash → damage visible.
    // When animations are off, or when the health changes without a tower attack (e.g. acid
    // DOT ticks during the enemy turn), the displayed value updates immediately.
    // For AoE attacks (wizard fireball, alchemy acid) we must also delay tiles that are in
    // the blast area but not the exact target position — those tiles have towerAttackEffect == null
    // but their enemy HP still changes as part of the AoE damage.
    val isDeathEffectActive = deathEffect != null && attacker == null
    // suppressEnemyBackground already covers all cases (targeted, AoE, and non-targeted tiles).
    val enemyBgSuppressed = suppressEnemyBackground
    val animationsEnabled = AppSettings.enableAnimations.value
    var displayedHealth by rememberDisplayedEnemyHealth(attacker)
    if (attacker != null) {
        LaunchedEffect(
            attacker.id,
            attacker.currentHealth.value,
            towerAttackEffect?.turnNumber,
            isInWizardAttackArea,
            isInAlchemyAttackArea,
            animationsEnabled,
        ) {
            val currentHealth = attacker.currentHealth.value
            val flightDelay: Long =
                when {
                    !animationsEnabled -> 0L
                    // Direct-target tiles: use the specific flight delay for that tower type
                    towerAttackEffect != null && isArrowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isBallistaTargetTile -> GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isBowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isSpearTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isPikeTargetTile -> GamePlayConstants.AnimationTimings.PIKE_EXTEND_DELAY_MS
                    towerAttackEffect != null && isWizardTargetTile -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isAlchemyTargetTile -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                    // AoE surrounding tiles: towerAttackEffect is null for them, but still delay
                    isInWizardAttackArea -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                    isInAlchemyAttackArea -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                    else -> 0L
                }
            if (flightDelay > 0L) {
                kotlinx.coroutines.delay(flightDelay + GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS)
            }
            displayedHealth = currentHealth
        }
    }

    when {
        attacker != null -> {
            // Use graphical icon for enemy units
            // Key by id, position, level, and movementPenalty to force recomposition when any changes.
            // currentHealth is intentionally omitted from the key so that the delayed displayedHealth
            // state (see above) is not discarded when health changes during an attack animation.
            key(
                attacker.id,
                attacker.position.value.x,
                attacker.position.value.y,
                attacker.level,
                attacker.movementPenalty.value,
                isInSelectedEnemyPath,
            ) {
                // Detect freeze effect before Box so it can be used in modifier for outline
                val freezeEffect =
                    gameState.activeSpellEffects.find {
                        it.spell == SpellType.FREEZE_SPELL && it.attackerId == attacker.id
                    }
                // Detect if cooling spell reduces this enemy's movement to 0
                val coolingReducesToZero =
                    isInCoolingArea &&
                        run {
                            val barbsSpeed = maxOf(1, attacker.currentBaseMovementSpeed - attacker.movementPenalty.value)
                            maxOf(0, barbsSpeed - 1) == 0
                        }
                // Compute the actual tile background color so the icon can derive the correct outline color.
                // When unit backgrounds are OFF (transparent), pass Color.White so the icon always uses
                // dark outlines – matching the light-mode appearance regardless of the current theme.
                // When a freeze/cooling effect is active, that color takes priority.
                val attackerTileBackground =
                    when {
                        freezeEffect != null || coolingReducesToZero -> TargetCircleConstants.COOLING_SPELL_COLOR.copy(alpha = 0.5f)
                        !AppSettings.showUnitTowerBackground.value -> Color.White
                        else -> null
                    }
                // Derive health/level text color from effective background:
                // - Freeze/cooling (turquoise) or unit backgrounds ON (red) → dark enough for white text
                // - Unit backgrounds OFF in dark mode → dark terrain bg → white text
                // - Unit backgrounds OFF in light mode → light terrain bg → dark text
                val healthTextColor =
                    when {
                        freezeEffect != null || coolingReducesToZero -> Color.White
                        AppSettings.showUnitTowerBackground.value -> Color.White
                        AppSettings.isDarkMode.value -> Color.White
                        else -> Color.Black
                    }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        if (freezeEffect != null || coolingReducesToZero) {
                            Modifier.border(2.dp, TargetCircleConstants.COOLING_SPELL_COLOR, RoundedCornerShape(4.dp))
                        } else {
                            Modifier
                        },
                ) {
                    EnemyIcon(
                        attacker = attacker,
                        backgroundColor = attackerTileBackground,
                        healthTextColor = healthTextColor,
                        healthOverride = displayedHealth,
                        showWaaghGlow = gameState.waaghFrenzyActive.value && attacker.type in setOf(AttackerType.GOBLIN, AttackerType.ORK, AttackerType.OGRE, AttackerType.SNOTLING),
                        showSeafaringPirateBarge = shouldShowSeafaringPirateBarge(attacker.type, isRiverTile),
                        protectedByObsidianAura = gameState.isProtectedByObsidianProtector(attacker),
                    )
                    if (isDangerous) {
                        Text(
                            text = "!",
                            color = GamePlayColors.Error,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp),
                        )
                    }
                    // Show healing effect overlay if present
                    val shouldPlayHealingAnimation =
                        rememberShouldPlayOneShotTileAnimation(
                            gameState,
                            healingEffect?.let {
                                oneShotTileAnimationKey("green_witch_healing", it.position, it.turnNumber)
                            },
                        )
                    if (healingEffect != null && shouldPlayHealingAnimation) {
                        GreenWitchHealingAnimation(
                            animate = AppSettings.enableAnimations.value,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    // Show freeze effect overlay
                    if (freezeEffect != null) {
                        FreezeSpellAnimation(
                            animate = AppSettings.enableAnimations.value,
                            modifier = Modifier.fillMaxSize(),
                            animationKey = freezeEffect.attackerId,
                        )
                    }
                    // Show fear effect overlay (black scribble cloud at top of icon)
                    val fearEffect =
                        gameState.activeSpellEffects.find { effect ->
                            (effect.spell == SpellType.FEAR_SPELL && effect.attackerId == attacker.id) ||
                                (
                                    effect.spell == SpellType.FEAR_SPELL_AREA &&
                                        effect.position != null &&
                                        attacker.position.value.hexDistanceTo(effect.position) <= 2
                                )
                        }
                    if (fearEffect != null) {
                        FearSpellAnimation(
                            animate = AppSettings.enableAnimations.value,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    // Show barb effect indicators if affected (show up to 5 arrows in center)
                    if (attacker.movementPenalty.value > 0) {
                        val barbCount = minOf(attacker.movementPenalty.value, 5)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                repeat(barbCount) {
                                    Box(
                                        modifier =
                                            Modifier.graphicsLayer {
                                                rotationZ = 10f // Tilt +10 degrees
                                            },
                                    ) {
                                        de.egril.defender.ui.icon.DownArrowIcon(
                                            size = 12.dp,
                                            tint = Color.Red,
                                        )
                                    }
                                }
                            }
                        }
                        // Show mushroom buff overlay when the enemy is under mushroom buff
                        if (attacker.mushroomTurnsRemaining.value > 0) {
                            MushroomBuffAnimation(
                                animate = AppSettings.enableAnimations.value,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    // Attack damage / lethality / immunity preview at the left border
                    if (attackPreview != null) {
                        EnemyAttackPreviewIcon(
                            damage = attackPreview.damage,
                            isLethal = attackPreview.isLethal,
                            isImmune = attackPreview.isImmune,
                            modifier =
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .offset(x = 10.dp),
                        )
                    }
                }
            }
        }

        isInSelectedEnemyPath -> {
            Text(
                text = "PATH",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        defender != null -> {
            // Use graphical icon for towers
            // Key by id, position, level and actionsRemaining to force recomposition when these change
            val doubleLevelActive =
                gameState.activeSpellEffects.any {
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
                doubleLevelActive,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    TowerIcon(defender = defender, gameState = gameState, altarScale = 2f)
                    // Show pulsing blue glow when tower is ready to act
                    if (defender.isReady && defender.actionsRemaining.value > 0) {
                        TowerReadyPulseAnimation(
                            animate = AppSettings.enableAnimations.value,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    // Show construction complete sparkle when tower just finished building
                    val shouldPlayConstructionCompleteAnimation =
                        rememberShouldPlayOneShotTileAnimation(
                            gameState,
                            constructionCompleteEffect?.let {
                                oneShotTileAnimationKey("tower_construction_complete", it.position, it.turnNumber)
                            },
                        )
                    if (constructionCompleteEffect != null && shouldPlayConstructionCompleteAnimation) {
                        TowerConstructionCompleteAnimation(
                            animate = AppSettings.enableAnimations.value,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    // Show idle ambient animation for wizard and alchemy towers (when built)
                    if (defender.buildTimeRemaining.value == 0) {
                        when (defender.type) {
                            // Wizard idle glows only while the tower still has actions this turn
                            DefenderType.WIZARD_TOWER ->
                                if (defender.actionsRemaining.value > 0 || defender.isChanneling.value) {
                                    WizardIdleAnimation(
                                        animate = AppSettings.enableAnimations.value,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            DefenderType.ALCHEMY_TOWER ->
                                AlchemyIdleAnimation(
                                    animate = AppSettings.enableAnimations.value,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            DefenderType.DWARVEN_MINE -> {
                                // Show dig animation on mine tile when it was just dug
                                val shouldPlayMineDigAnimation =
                                    rememberShouldPlayOneShotTileAnimation(
                                        gameState,
                                        mineDigEffect?.let {
                                            oneShotTileAnimationKey("mine_dig", it.position, it.turnNumber)
                                        },
                                    )
                                if (mineDigEffect != null && shouldPlayMineDigAnimation) {
                                    MineDigAnimation(
                                        animate = AppSettings.enableAnimations.value,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }
                    // Show Double Tower Level spell animation overlay (same animation as instant tower)
                    if (doubleLevelActive) {
                        InstantTowerSpellAnimation(
                            animate = AppSettings.enableAnimations.value,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    // Show red "XT" overlay if tower is disabled by Red Witch
                    if (defender.isDisabled.value && defender.disabledTurnsRemaining.value > 0) {
                        Text(
                            "${defender.disabledTurnsRemaining.value}T",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
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
                        verticalArrangement = Arrangement.Center,
                    ) {
                        TestTubeIcon(size = 20.dp)
                        Text(
                            "-${fieldEffect.damage}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${fieldEffect.turnsRemaining}T",
                            style = MaterialTheme.typography.labelSmall,
                            color = GamePlayColors.Yellow,
                        )
                    }
                }

                FieldEffectType.WEB -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        WebIcon(size = 20.dp)
                        Text(
                            "${fieldEffect.turnsRemaining}T",
                            style = MaterialTheme.typography.labelSmall,
                            color = GamePlayColors.Yellow,
                        )
                    }
                }

                FieldEffectType.BURNING_TILE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ExplosionIcon(size = 22.dp)
                        Text(
                            "${fieldEffect.turnsRemaining}T",
                            style = MaterialTheme.typography.labelSmall,
                            color = GamePlayColors.Yellow,
                        )
                    }
                }
                FieldEffectType.SHADOW_FOG -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(GamePlayConstants.TileIconSizes.ShadowFogOverlay)
                                    .background(Color(0xB020002A), RoundedCornerShape(50)),
                        )
                        Text(
                            "${fieldEffect.turnsRemaining}T",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE1BEE7),
                        )
                    }
                }
            }
        }

        fief != null -> {
            // Show fief image, type name, and coin income
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val fiefName =
                    when (fief.type) {
                        de.egril.defender.model.FiefType.FISHER ->
                            stringResource(Res.string.fief_type_fisher)
                        de.egril.defender.model.FiefType.FISHER_BOAT ->
                            stringResource(Res.string.fief_type_fisher_boat)
                        de.egril.defender.model.FiefType.WOODCUTTER ->
                            stringResource(Res.string.fief_type_woodcutter)
                        de.egril.defender.model.FiefType.QUARRY ->
                            stringResource(Res.string.fief_type_quarry)
                        de.egril.defender.model.FiefType.MARKETPLACE ->
                            stringResource(Res.string.fief_type_marketplace)
                    }

                when (fief.type) {
                    de.egril.defender.model.FiefType.FISHER_BOAT -> {
                        Box(
                            modifier = Modifier.size(GamePlayConstants.TileIconSizes.Fief),
                            contentAlignment = Alignment.Center,
                        ) {
                            FisherBoatIcon(modifier = Modifier.fillMaxSize())
                        }
                    }
                    de.egril.defender.model.FiefType.FISHER -> {
                        val rodRotation = getFisherRodRotationDegrees(position, gameState.level)
                        Box(
                            modifier = Modifier.size(GamePlayConstants.TileIconSizes.Fief),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.fief_fisher_hut),
                                contentDescription = fiefName,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Image(
                                painter = painterResource(Res.drawable.fief_fishing_rod),
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            rotationZ = rodRotation,
                                            transformOrigin = TransformOrigin(0.38f, 0.56f),
                                        ),
                            )
                        }
                    }
                    de.egril.defender.model.FiefType.WOODCUTTER -> {
                        Image(
                            painter = painterResource(Res.drawable.fief_woodcutter),
                            contentDescription = fiefName,
                            modifier = Modifier.size(GamePlayConstants.TileIconSizes.Fief),
                        )
                    }
                    de.egril.defender.model.FiefType.QUARRY -> {
                        Image(
                            painter = painterResource(Res.drawable.fief_quarry),
                            contentDescription = fiefName,
                            modifier = Modifier.size(GamePlayConstants.TileIconSizes.Fief),
                        )
                    }
                    de.egril.defender.model.FiefType.MARKETPLACE -> {
                        Image(
                            painter = painterResource(Res.drawable.fief_marketplace),
                            contentDescription = fiefName,
                            modifier = Modifier.size(GamePlayConstants.TileIconSizes.Fief),
                        )
                    }
                }
                Text(
                    fiefName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = (-3).dp),
                )
                Text(
                    "+${fief.type.incomePerTurn}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GamePlayColors.Yellow,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-3).dp),
                )
            }
        }

        gargoyle != null -> {
            // Inactive stone gargoyle guarding its tile
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    GargoyleIcon(active = false, size = GamePlayConstants.TileIconSizes.Gargoyle)
                }
                if (attackPreview != null) {
                    EnemyAttackPreviewIcon(
                        damage = attackPreview.damage,
                        isLethal = attackPreview.isLethal,
                        isImmune = attackPreview.isImmune,
                        modifier =
                            Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = 10.dp),
                    )
                }
            }
        }

        mushroom != null -> {
            // Show mushroom icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                MushroomIcon(size = GamePlayConstants.TileIconSizes.Mushroom)
            }
        }

        trap != null -> {
            // Show trap icon based on trap type
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
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
                            modifier = Modifier.offset(y = (-6).dp),
                        )
                    }
                }
            }
        }

        barricade != null -> {
            // Show barricade with HP
            val barricadeLocale = com.hyperether.resources.currentLanguage.value
            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Show wood/barricade symbol or gate icon with brown color.
                    // When the barricade also serves as a tower base (HP >= 100), overlay the
                    // wooden tower-base platform so it is distinguishable from a plain barricade.
                    Box(contentAlignment = Alignment.Center) {
                        if (barricade.isGate) {
                            GateIcon(
                                modifier = Modifier.offset(y = 10.dp),
                                size = GamePlayConstants.TileIconSizes.Barricade,
                            )
                        } else {
                            WoodIcon(size = GamePlayConstants.TileIconSizes.Barricade)
                        }
                        if (barricade.canSupportTower()) {
                            TowerBasePlatformIcon(size = GamePlayConstants.TileIconSizes.Barricade)
                        }
                    }

                    // Show gate/barricade name (2 lines) then HP (1 line, bold)
                    val barricadeDisplayName =
                        barricade.name
                            ?.takeIf { it.isNotBlank() }
                            ?.let { localizeEntityName(it, barricadeLocale) }
                    if (!barricadeDisplayName.isNullOrBlank()) {
                        Text(
                            text =
                                buildAnnotatedString {
                                    withStyle(SpanStyle(color = Color.White)) {
                                        appendLine(barricadeDisplayName)
                                    }
                                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                                        appendLine("${barricade.healthPoints.value} HP")
                                    }
                                },
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            minLines = 3,
                            maxLines = 3,
                            overflow = TextOverflow.Visible,
                            modifier =
                                Modifier
                                    .widthIn(max = 50.dp)
                                    .offset(y = (-32).dp),
                        )
                    } else {
                        Text(
                            "${barricade.healthPoints.value} HP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.offset(y = (-12).dp),
                        )
                    }
                }
                // Show damage effect overlay if present
                if (damageEffect != null) {
                    BarricadeDamageAnimation(
                        animate = AppSettings.enableAnimations.value,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        showBarricadePreview || supportObjectPreviewType == SupportObjectType.BARRICADE -> {
            // Show see-through barricade preview when hovering
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.graphicsLayer(alpha = 0.5f), // Semi-transparent
            ) {
                // Show wood/barricade symbol with brown color
                WoodIcon(size = GamePlayConstants.TileIconSizes.Barricade)
                // Show "NEW" text for new barricade preview
                Text(
                    stringResource(Res.string.barricade),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF795548), // Brown color
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        bombEffect != null -> {
            // Show bomb icon with countdown number overlaid prominently
            Box(contentAlignment = Alignment.Center) {
                BombIcon(size = 36.dp)
                // Countdown badge in bottom-right corner of icon
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .background(
                                color = Color(0xFFCC0000),
                                shape = androidx.compose.foundation.shape.CircleShape,
                            ).padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${bombEffect.turnsRemaining}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = MaterialTheme.typography.labelMedium.fontSize,
                    )
                }
            }
        }

        isSpawnPoint -> {
            // Show spawn indicator when cell is empty
            Text(
                stringResource(Res.string.spawn),
                style = MaterialTheme.typography.labelSmall,
                color = GamePlayColors.Warning,
            )
        }

        isTarget -> {
            // Show target name (if set) or fallback to generic "Target" label
            // Well-known names are translated; \n in the string gives multi-line tile display
            // Taken targets (SINGLE_HIT) show with a red cross overlay
            val locale = com.hyperether.resources.currentLanguage.value
            val isTaken = gameState.takenTargets.contains(position)
            val isNextOrderedTarget = gameState.getNextSingleHitTargetPosition() == position
            val rawName =
                gameState.level.targetInfoMap[position]
                    ?.name
                    ?.takeIf { it.isNotBlank() }
            val targetName =
                if (rawName != null) {
                    localizeEntityName(rawName, locale)
                } else {
                    stringResource(Res.string.target)
                }
            Box(
                modifier =
                    if (isNextOrderedTarget) {
                        Modifier
                            .border(BorderStroke(2.dp, Color(0xFFFFC107)), RoundedCornerShape(8.dp))
                            .background(Color(0x66400000), RoundedCornerShape(8.dp))
                            .padding(horizontal = 3.dp, vertical = 2.dp)
                    } else {
                        Modifier
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isTaken) {
                    // Show dimmed name with a red X cross on top
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = targetName,
                            style = MaterialTheme.typography.labelSmall,
                            color = GamePlayColors.Success.copy(alpha = 0.3f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 50.dp),
                        )
                        CrossIcon(size = 20.dp, tint = Color.Red)
                    }
                } else {
                    Text(
                        text = targetName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isNextOrderedTarget) Color(0xFFFFD54F) else GamePlayColors.Success,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 50.dp),
                        fontWeight = if (isNextOrderedTarget) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                if (isNextOrderedTarget) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .background(Color(0xFFFFC107), androidx.compose.foundation.shape.CircleShape)
                                .padding(horizontal = 3.dp),
                    ) {
                        Text(
                            text = "1",
                            color = Color(0xFF202020),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        isRiverTile -> {
            // Check if there's a bridge at this position
            val bridge = gameState.getBridgeAt(position)
            if (bridge != null) {
                // Show bridge over river
                val bridgeIncomePerTile = bridgeSystem.getBridgeIncomePerTile(bridge)
                BridgeVisualization(bridge = bridge, incomePerTile = bridgeIncomePerTile)
            } else {
                // Show river flow direction arrows
                if (riverTile != null) {
                    // Don't show trap icon on maelstrom when tile images are enabled
                    // (the tile_river_maelstrom.png image already shows the maelstrom visually)
                    val useTileImages = AppSettings.useTileImages.value
                    val isMaelstromWithTileImage = riverTile.flowDirection == RiverFlow.MAELSTROM && useTileImages
                    // Don't show dot symbol on NONE (still water) tiles when the level map image is enabled
                    val useLevelMapImage = AppSettings.useLevelMapImage.value
                    val isNoneWithMapImage = riverTile.flowDirection == RiverFlow.NONE && useLevelMapImage

                    // Show water flow animation when animations are enabled (not for NONE/MAELSTROM)
                    val enableAnimations = AppSettings.enableAnimations.value
                    val showWaterAnimation =
                        enableAnimations &&
                            riverTile.flowDirection != RiverFlow.NONE &&
                            riverTile.flowDirection != RiverFlow.MAELSTROM
                    if (showWaterAnimation) {
                        WaterFlowAnimation(
                            flowDirection = riverTile.flowDirection,
                            flowSpeed = riverTile.flowSpeed,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    if (!isMaelstromWithTileImage && !isNoneWithMapImage) {
                        RiverFlowIndicator(
                            flowDirection = riverTile.flowDirection,
                            flowSpeed = riverTile.flowSpeed,
                            size = 28.dp,
                        )
                    }
                }
            }
        }
    }

    GridCellAnimations(
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
        bombExplosion = bombExplosion,
        enemySpawnEffect = enemySpawnEffect,
        trapTriggerEffect = trapTriggerEffect,
        enemyMoveEffect = enemyMoveEffect,
        dragonLevelChangeEffect = dragonLevelChangeEffect,
        arrowAttackEffect = arrowAttackEffect,
        dragonIsTargetingMine = dragonIsTargetingMine,
        bombEffect = bombEffect,
        isSpawnPoint = isSpawnPoint,
        isTarget = isTarget,
        isRiverTile = isRiverTile,
        targetCircleInfo = targetCircleInfo,
        useDashedBorder = useDashedBorder,
        borderColor = borderColor,
        borderWidth = borderWidth,
        hexSize = hexSize,
        showDiagonalStripes = showDiagonalStripes,
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
}
