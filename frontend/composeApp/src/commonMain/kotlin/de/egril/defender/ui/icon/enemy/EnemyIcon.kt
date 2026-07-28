package de.egril.defender.ui.icon.enemy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.hidesHealthBar
import de.egril.defender.model.isSwarmUnit
import de.egril.defender.ui.getLocalizedShortName
import de.egril.defender.utils.BigHeadMode

/**
 * Composable that draws an enemy unit icon
 *
 * @param healthOverride When provided, this value is shown instead of [attacker.currentHealth]
 *   in the health bar. Used to delay the health display during attack animations so the number
 *   only changes after the projectile impact flash has completed.
 */

/** Scale factor for snotling icons relative to the goblin icon (20% of goblin size). */
private const val SNOTLING_ICON_SCALE = 0.2f

/**
 * Relative offsets (xFactor, yFactor) for each snotling in the diamond layout, in units of the
 * grid spacing. Filled from the center outward so small stacks look natural. Maximum 15 icons:
 *
 * Row 1 (top):   1 slot  (y = -2)
 * Row 2:         4 slots (y = -1)
 * Row 3 (center):5 slots (y =  0)
 * Row 4:         4 slots (y = +1)
 * Row 5 (bottom):1 slot  (y = +2)
 */
private val SNOTLING_DIAMOND_OFFSETS =
    listOf(
        // 1 – center/center
        Pair(0f, 0f),
        // 2–3 – row 4 inner pair
        Pair(-0.5f, 1f),
        Pair(0.5f, 1f),
        // 4–5 – row 2 inner pair
        Pair(-0.5f, -1f),
        Pair(0.5f, -1f),
        // 6–9 – fill rest of row 3
        Pair(-2f, 0f),
        Pair(-1f, 0f),
        Pair(1f, 0f),
        Pair(2f, 0f),
        // 10–11 – row 2 outer pair
        Pair(-1.5f, -1f),
        Pair(1.5f, -1f),
        // 12–13 – row 4 outer pair
        Pair(-1.5f, 1f),
        Pair(1.5f, 1f),
        // 14–15 – row 1 and row 5 singles
        Pair(0f, -2f),
        Pair(0f, 2f),
    )

@Composable
fun EnemyIcon(
    attacker: Attacker,
    modifier: Modifier = Modifier,
    healthTextColor: Color = Color.White,
    backgroundColor: Color? = null,
    healthOverride: Int? = null,
    moveVillainNameUp: Boolean = false,
) {
    val bgLuminance = (backgroundColor ?: MaterialTheme.colorScheme.background).luminance()
    val contrastOutlineColor = if (bgLuminance < 0.5f) Color.White else Color.Black

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Draw enemy graphics first (will be behind text)
        val headScale = if (BigHeadMode.isEnabled.value) 2f else 1f
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val iconSize = minOf(size.width, size.height)

            // Villains get a shared aura ring behind their symbol so they stand out on the map.
            if (attacker.type.isVillain) {
                drawVillainMarker(centerX, centerY, iconSize)
            }

            when (attacker.type) {
                AttackerType.GOBLIN -> drawGoblinSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.ORK -> drawOrkSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.OGRE -> drawOgreSymbol(centerX, centerY, iconSize * 0.75f, headScale = headScale)
                AttackerType.SKELETON -> drawSkeletonSymbol(centerX, centerY, iconSize * 0.7f, contrastOutlineColor, headScale)
                AttackerType.ZOMBIE -> drawZombieSymbol(centerX, centerY, iconSize * 0.72f, contrastOutlineColor, headScale)
                AttackerType.EVIL_WIZARD -> drawEvilWizardSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.BLUE_DEMON -> drawBlueDemonSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.RED_DEMON -> drawRedDemonSymbol(centerX, centerY, iconSize * 0.75f, contrastOutlineColor, headScale)
                AttackerType.RED_WITCH -> drawRedWitchSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.GREEN_WITCH -> drawGreenWitchSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.SNOTLING,
                AttackerType.SPIDERLING,
                -> {
                    // Swarm units: diamond layout, up to 15 icons, filled from the center outward.
                    // Each icon is 20 % of goblin size; the grid is shifted up slightly to leave
                    // room for the HP counter that is always shown at the bottom.
                    val hp = healthOverride ?: attacker.currentHealth.value
                    val count = minOf(hp, SNOTLING_DIAMOND_OFFSETS.size)
                    val snotlingSize = iconSize * 0.7f * SNOTLING_ICON_SCALE
                    val gridUnit = iconSize * 0.18f
                    val gridCenterY = centerY - iconSize * 0.06f
                    for (i in 0 until count) {
                        val (xFactor, yFactor) = SNOTLING_DIAMOND_OFFSETS[i]
                        if (attacker.type == AttackerType.SPIDERLING) {
                            drawSpiderlingSymbol(
                                centerX + xFactor * gridUnit,
                                gridCenterY + yFactor * gridUnit,
                                snotlingSize,
                                headScale = headScale,
                            )
                        } else {
                            drawGoblinSymbol(
                                centerX + xFactor * gridUnit,
                                gridCenterY + yFactor * gridUnit,
                                snotlingSize,
                                headScale = headScale,
                            )
                        }
                    }
                }
                AttackerType.ROBOTIC_GOBLIN -> drawRoboticGoblinSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.SNOTLING_BOSS -> drawSnotlingBossSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.EWHAD -> drawEwhadSymbol(centerX, centerY, iconSize * 0.8f, headScale = headScale)
                AttackerType.DRAGON -> drawDragonSymbol(centerX, centerY, iconSize * 0.9f, headScale = headScale)
                AttackerType.UNDEAD_DRAGON -> drawUndeadDragonSymbol(centerX, centerY, iconSize * 0.9f, contrastOutlineColor, headScale)
                AttackerType.GAROKK -> drawGarokkSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.MORGUK_BONEWHISPER -> drawMorgukBonewhisperSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.ARAXXA -> drawAraxxaSymbol(centerX, centerY, iconSize * 0.75f, headScale = headScale)
                AttackerType.BARON_RATTERZAHN -> drawBaronRatterzahnSymbol(centerX, centerY, iconSize * 0.78f, headScale = headScale)
                AttackerType.FALLEN_SHIELDMAIDEN_FREYA -> drawFallenShieldmaidenFreyaSymbol(centerX, centerY, iconSize * 0.82f, headScale = headScale)
                AttackerType.PRINCE_VALERIUS_THE_SOULREAPER -> drawPrinceValeriusSymbol(centerX, centerY, iconSize * 0.8f, contrastOutlineColor, headScale)
                AttackerType.SILAS_THE_MASKMASTER,
                AttackerType.SILAS_MIRROR_IMAGE,
                -> drawSilasSymbol(centerX, centerY, iconSize * 0.74f, headScale = headScale)
                AttackerType.GRAND_COVEN_MOTHER_SYBILLA -> drawSybillaSymbol(centerX, centerY, iconSize * 0.78f, headScale = headScale)
                AttackerType.HAGA -> drawHagaSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.ZUSSA -> drawZussaSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.SYLVANAS_THE_MOLDING -> drawSylvanasTheMoldingSymbol(centerX, centerY, iconSize * 0.76f, headScale = headScale)
                AttackerType.ARCHMAGE_MALAKOR_THE_RENEGADE -> drawArchmageMalakorSymbol(centerX, centerY, iconSize * 0.78f, headScale = headScale)
                AttackerType.IGNIS_VA_THE_DRAGONVOICE -> drawIgnisVaSymbol(centerX, centerY, iconSize * 0.78f, headScale = headScale)
                AttackerType.DRAGON_TERROR -> drawDragonTerrorSymbol(centerX, centerY, iconSize * 0.85f, headScale = headScale)
                AttackerType.XARITHON_THE_SHADOW_DRAGON -> drawXarithonTheShadowDragonSymbol(centerX, centerY, iconSize * 0.90f, headScale = headScale)
                AttackerType.CAPTAIN_RODERICH -> drawCaptainRoderichSymbol(centerX, centerY, iconSize * 0.80f, headScale = headScale)
                AttackerType.THE_KRAKEN -> drawKrakenSymbol(centerX, centerY, iconSize * 0.85f, headScale = headScale)
            }
        }

        // Level number at top center - only if level > 1 (not shown for snotlings)
        if (attacker.level.value > 1 && !attacker.type.isSwarmUnit()) {
            Text(
                text = "${attacker.level.value}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                color = healthTextColor,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
            )
        }

        // Health number at bottom center - 10dp from bottom edge.
        // Villains show their short name in place of health points.
        // All other enemies (including snotlings) always show their health points.
        val displayedHealth = healthOverride ?: attacker.currentHealth.value
        if (attacker.type.isVillain) {
            Text(
                text = attacker.type.getLocalizedShortName(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = healthTextColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (moveVillainNameUp) 22.dp else 10.dp),
            )
        } else if (!attacker.type.hidesHealthBar) {
            Text(
                text = "$displayedHealth",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 13.sp,
                color = healthTextColor,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
            )
        }
    }
}

/**
 * Composable that draws an enemy type icon (for planned spawns)
 */
@Composable
fun EnemyTypeIcon(
    attackerType: AttackerType,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
) {
    val bgLuminance = (backgroundColor ?: MaterialTheme.colorScheme.background).luminance()
    val contrastOutlineColor = if (bgLuminance < 0.5f) Color.White else Color.Black

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Draw enemy graphics
        val headScale = if (BigHeadMode.isEnabled.value) 2f else 1f
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val iconSize = minOf(size.width, size.height)

            // Villains get a shared aura ring behind their symbol so they stand out on the map.
            if (attackerType.isVillain) {
                drawVillainMarker(centerX, centerY, iconSize)
            }

            when (attackerType) {
                AttackerType.GOBLIN -> drawGoblinSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.ORK -> drawOrkSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.OGRE -> drawOgreSymbol(centerX, centerY, iconSize * 0.75f, headScale = headScale)
                AttackerType.SKELETON -> drawSkeletonSymbol(centerX, centerY, iconSize * 0.7f, contrastOutlineColor, headScale)
                AttackerType.ZOMBIE -> drawZombieSymbol(centerX, centerY, iconSize * 0.72f, contrastOutlineColor, headScale)
                AttackerType.EVIL_WIZARD -> drawEvilWizardSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.BLUE_DEMON -> drawBlueDemonSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.RED_DEMON -> drawRedDemonSymbol(centerX, centerY, iconSize * 0.75f, contrastOutlineColor, headScale)
                AttackerType.RED_WITCH -> drawRedWitchSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.GREEN_WITCH -> drawGreenWitchSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                // Snotlings: show a single small icon (20% of goblin icon size) in type previews
                AttackerType.SNOTLING -> drawGoblinSymbol(centerX, centerY, iconSize * 0.7f * SNOTLING_ICON_SCALE, headScale = headScale)
                AttackerType.SPIDERLING -> drawSpiderlingSymbol(centerX, centerY, iconSize * 0.7f * SNOTLING_ICON_SCALE, headScale = headScale)
                AttackerType.ROBOTIC_GOBLIN -> drawRoboticGoblinSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.SNOTLING_BOSS -> drawSnotlingBossSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.EWHAD -> drawEwhadSymbol(centerX, centerY, iconSize * 0.8f, headScale = headScale)
                AttackerType.DRAGON -> drawDragonSymbol(centerX, centerY, iconSize * 0.9f, headScale = headScale)
                AttackerType.UNDEAD_DRAGON -> drawUndeadDragonSymbol(centerX, centerY, iconSize * 0.9f, contrastOutlineColor, headScale)
                AttackerType.GAROKK -> drawGarokkSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.MORGUK_BONEWHISPER -> drawMorgukBonewhisperSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.ARAXXA -> drawAraxxaSymbol(centerX, centerY, iconSize * 0.75f, headScale = headScale)
                AttackerType.BARON_RATTERZAHN -> drawBaronRatterzahnSymbol(centerX, centerY, iconSize * 0.78f, headScale = headScale)
                AttackerType.FALLEN_SHIELDMAIDEN_FREYA -> drawFallenShieldmaidenFreyaSymbol(centerX, centerY, iconSize * 0.82f, headScale = headScale)
                AttackerType.PRINCE_VALERIUS_THE_SOULREAPER -> drawPrinceValeriusSymbol(centerX, centerY, iconSize * 0.8f, contrastOutlineColor, headScale)
                AttackerType.SILAS_THE_MASKMASTER,
                AttackerType.SILAS_MIRROR_IMAGE,
                -> drawSilasSymbol(centerX, centerY, iconSize * 0.74f, headScale = headScale)
                AttackerType.GRAND_COVEN_MOTHER_SYBILLA -> drawSybillaSymbol(centerX, centerY, iconSize * 0.78f, headScale = headScale)
                AttackerType.HAGA -> drawHagaSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.ZUSSA -> drawZussaSymbol(centerX, centerY, iconSize * 0.7f, headScale = headScale)
                AttackerType.SYLVANAS_THE_MOLDING -> drawSylvanasTheMoldingSymbol(centerX, centerY, iconSize * 0.76f, headScale = headScale)
                AttackerType.ARCHMAGE_MALAKOR_THE_RENEGADE -> drawArchmageMalakorSymbol(centerX, centerY, iconSize * 0.78f, headScale = headScale)
                AttackerType.IGNIS_VA_THE_DRAGONVOICE -> drawIgnisVaSymbol(centerX, centerY, iconSize * 0.78f, headScale = headScale)
                AttackerType.DRAGON_TERROR -> drawDragonTerrorSymbol(centerX, centerY, iconSize * 0.85f, headScale = headScale)
                AttackerType.XARITHON_THE_SHADOW_DRAGON -> drawXarithonTheShadowDragonSymbol(centerX, centerY, iconSize * 0.90f, headScale = headScale)
                AttackerType.CAPTAIN_RODERICH -> drawCaptainRoderichSymbol(centerX, centerY, iconSize * 0.80f, headScale = headScale)
                AttackerType.THE_KRAKEN -> drawKrakenSymbol(centerX, centerY, iconSize * 0.85f, headScale = headScale)
            }
        }
    }
}
