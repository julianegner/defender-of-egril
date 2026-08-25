package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.a11y.a11ySemantics
import de.egril.defender.ui.icon.HeartIcon
import de.egril.defender.ui.icon.InfoIcon
import de.egril.defender.ui.icon.LightningIcon
import de.egril.defender.ui.icon.LockIcon
import de.egril.defender.ui.icon.MushroomIcon
import de.egril.defender.ui.icon.RightArrowIcon
import de.egril.defender.ui.icon.ShieldIcon
import de.egril.defender.ui.icon.SnowflakeIcon
import de.egril.defender.ui.icon.WarningIcon
import de.egril.defender.ui.icon.enemy.EnemyIcon
import defender_of_egril.composeapp.generated.resources.*

private const val VILLAIN_INFO_FIRST_COLUMN_MAX_ENTRIES = 4

internal enum class MushroomEnhancementKind {
    SPEED_AND_LEVEL,
    SPEED_LEVEL_AND_ABILITIES,
}

internal fun Attacker.mushroomEnhancementKind(): MushroomEnhancementKind? =
    if (mushroomTurnsRemaining.value <= 0) {
        null
    } else if (type.hasMushroomAbilityBoost) {
        MushroomEnhancementKind.SPEED_LEVEL_AND_ABILITIES
    } else {
        MushroomEnhancementKind.SPEED_AND_LEVEL
    }

private val AttackerType.hasMushroomAbilityBoost: Boolean
    get() =
        canHeal ||
            canDisableTowers ||
            this == AttackerType.GRAND_COVEN_MOTHER_SYBILLA ||
            this == AttackerType.HAGA ||
            this == AttackerType.ZUSSA

private enum class AttackerInfoEntryIcon {
    WARNING,
    LIGHTNING,
    HEART,
    LOCK,
    SHIELD,
}

private data class AttackerInfoEntry(
    val icon: AttackerInfoEntryIcon,
    val text: String,
    val color: Color,
    val isBold: Boolean = false,
)

/**
 * Returns the localized description for an attacker type, or an empty string if none exists.
 */
@Composable
private fun getAttackerDescription(attackerType: AttackerType): String = attackerType.getLocalizedDescription()

/**
 * Display details about a selected enemy attacker
 * Similar to DefenderInfo but for enemies
 */
@Composable
fun AttackerInfo(
    attacker: Attacker,
    activeSpellEffects: SnapshotStateList<ActiveSpellEffect> = androidx.compose.runtime.mutableStateListOf(),
    isMobile: Boolean = false,
    onShowDragonInfo: () -> Unit = {},
    waaghActive: Boolean = false,
) {
    val locale = com.hyperether.resources.currentLanguage.value
    val attackerDisplayName =
        if (attacker.type.isDragon && attacker.dragonName != null) {
            val dragonLabel =
                if (attacker.type == AttackerType.UNDEAD_DRAGON) {
                    stringResource(Res.string.undead_dragon_label)
                } else {
                    stringResource(Res.string.the_dragon)
                }
            "$dragonLabel ${attacker.dragonName}"
        } else {
            attacker.getLocalizedName(locale)
        }
    val showWaaghGlow = waaghActive && attacker.type in setOf(AttackerType.GOBLIN, AttackerType.ORK, AttackerType.OGRE, AttackerType.SNOTLING)
    val waaghBoostText =
        if (waaghActive) {
            when (attacker.type) {
                AttackerType.GOBLIN -> stringResource(Res.string.waagh_goblin_boost)
                AttackerType.ORK -> stringResource(Res.string.waagh_ork_boost)
                AttackerType.OGRE -> stringResource(Res.string.waagh_ogre_boost)
                AttackerType.SNOTLING -> stringResource(Res.string.waagh_snotling_boost)
                else -> null
            }
        } else {
            null
        }
    val healthLabel = stringResource(Res.string.health)
    val healthPointsLabel = stringResource(Res.string.health_points)
    val speedLabel = stringResource(Res.string.speed_label)
    val attackerCardLabel =
        buildString {
            append(attackerDisplayName)
            if (!attacker.type.hidesHealthBar) {
                append(", ")
                append(healthLabel)
                append(": ")
                append(attacker.currentHealth.value)
                append(", ")
                append(healthPointsLabel)
                append(": ")
                append(attacker.maxHealth)
            }
            append(", ")
            append(speedLabel)
            append(": ")
            append(
                if (attacker.hasMushroomBuff) {
                    attacker.type.speed * 2
                } else {
                    attacker.type.speed
                },
            )
        }

    // Use key to force recomposition when attacker stats change
    key(
        attacker.id,
        attacker.level.value,
        attacker.currentHealth.value,
        attacker.position.value.x,
        attacker.position.value.y,
        attacker.greed,
        attacker.movementPenalty.value,
        attacker.mushroomTurnsRemaining.value,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(if (isMobile) 4.dp else 8.dp)
                    .a11ySemantics(label = attackerCardLabel),
        ) {
            // Enemy icon, name, and details in one row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                // Enemy icon - double the original size (matching tower icon size)
                val iconSize = if (isMobile) 64.dp else 96.dp
                val iconInnerSize = if (isMobile) 56.dp else 88.dp
                Box(
                    modifier = Modifier.size(iconSize),
                    contentAlignment = Alignment.Center,
                ) {
                    EnemyIcon(
                        attacker = attacker,
                        modifier = Modifier.size(iconInnerSize),
                        showWaaghGlow = showWaaghGlow,
                    )
                }

                val horizontalSpacing = if (isMobile) 4.dp else 8.dp
                Spacer(modifier = Modifier.width(horizontalSpacing))

                // Enemy name and level
                Column(modifier = Modifier.weight(1f)) {
                    // Pre-compute cooling effect for reuse throughout the Column
                    val coolingEffect =
                        activeSpellEffects.find { effect ->
                            effect.spell == SpellType.COOLING_SPELL &&
                                effect.position != null &&
                                attacker.position.value.hexDistanceTo(effect.position) <= 2
                        }
                    val barbsSpeed = maxOf(1, attacker.type.speed - attacker.movementPenalty.value)
                    val mushroomSpeed = if (attacker.hasMushroomBuff) barbsSpeed * 2 else barbsSpeed
                    val cooledSpeed = if (coolingEffect != null) maxOf(0, mushroomSpeed - 1) else null
                    val waaghSpeed = if (waaghActive && attacker.type == AttackerType.ORK) attacker.type.speed * 2 else null

                    // Pre-compute freeze effect for reuse throughout the Column
                    val freezeEffect =
                        activeSpellEffects.find {
                            it.spell == SpellType.FREEZE_SPELL && it.attackerId == attacker.id
                        }
                    val mushroomEnhancementKind = attacker.mushroomEnhancementKind()
                    val displayedLevel = attacker.displayLevel

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Show "The dragon [name]" for dragons, otherwise just the type name
                        Text(
                            attackerDisplayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (displayedLevel > 1) {
                            Text(
                                if (attacker.hasMushroomBuff) {
                                    "Lvl $displayedLevel (x2)"
                                } else {
                                    "Lvl $displayedLevel"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = GamePlayColors.ErrorDark,
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (!attacker.type.hidesHealthBar) {
                            Text(
                                "${stringResource(Res.string.hp_short)}: ${attacker.currentHealth.value}/${attacker.maxHealth}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        // Speed display - show base speed and current speed if affected by barbs or cooling
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${stringResource(Res.string.speed_label)}: ${attacker.type.speed}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )

                            // If affected by barbs, show current speed in red
                            if (attacker.movementPenalty.value > 0) {
                                RightArrowIcon(size = 12.dp, tint = Color.Red)
                                Text(
                                    "$barbsSpeed",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red,
                                )
                            }

                            if (attacker.hasMushroomBuff) {
                                RightArrowIcon(size = 12.dp, tint = Color(0xFFFF8C00))
                                Text(
                                    "$mushroomSpeed",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8C00),
                                )
                            }

                            if (waaghSpeed != null) {
                                RightArrowIcon(size = 12.dp, tint = Color(0xFFFFB000))
                                Text(
                                    "$waaghSpeed",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB000),
                                )
                            }

                            // If in cooling area, show cooled speed in turquoise
                            if (cooledSpeed != null) {
                                RightArrowIcon(size = 12.dp, tint = Color.Cyan)
                                Text(
                                    "$cooledSpeed",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Cyan,
                                )
                            }

                            // If frozen, show speed → 0 in turquoise
                            if (freezeEffect != null && cooledSpeed == null) {
                                RightArrowIcon(size = 12.dp, tint = Color.Cyan)
                                Text(
                                    "0",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Cyan,
                                )
                            }
                        }

                        Text(
                            "${stringResource(Res.string.position_label)}: (${attacker.position.value.x},${attacker.position.value.y})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }

                    if (mushroomEnhancementKind != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            MushroomIcon(size = 14.dp)
                            Text(
                                when (mushroomEnhancementKind) {
                                    MushroomEnhancementKind.SPEED_AND_LEVEL ->
                                        stringResource(Res.string.mushroom_enhanced_horde)
                                    MushroomEnhancementKind.SPEED_LEVEL_AND_ABILITIES ->
                                        stringResource(Res.string.mushroom_enhanced_witch)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF8C00),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (waaghBoostText != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            WarningIcon(size = 14.dp)
                            Text(
                                waaghBoostText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFB000),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Show barbs effect explanation if affected
                    if (attacker.movementPenalty.value > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            de.egril.defender.ui.icon
                                .DownArrowIcon(size = 14.dp)
                            Text(
                                stringResource(Res.string.slowed_by_barbs),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Show freeze status if enemy is frozen
                    if (freezeEffect != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            SnowflakeIcon(size = 14.dp, tint = Color.Cyan)
                            Text(
                                if (freezeEffect.turnsRemaining > 0) {
                                    stringResource(Res.string.frozen_turns_remaining, freezeEffect.turnsRemaining)
                                } else {
                                    stringResource(Res.string.frozen_label)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Cyan,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Show cooling status if enemy is in a cooling area (coolingEffect and cooledSpeed computed above)
                    if (coolingEffect != null && cooledSpeed != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            SnowflakeIcon(size = 14.dp, tint = Color.Cyan)
                            Text(
                                if (coolingEffect.turnsRemaining > 0) {
                                    stringResource(Res.string.cooled_turns_remaining, coolingEffect.turnsRemaining)
                                } else {
                                    stringResource(Res.string.cooled_label)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Cyan,
                                fontWeight = FontWeight.Bold,
                            )
                            RightArrowIcon(size = 12.dp, tint = Color.Cyan)
                            Text(
                                "$cooledSpeed",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Cyan,
                            )
                        }
                    }

                    // Show fear status if enemy is feared
                    val fearEffect =
                        activeSpellEffects.find { effect ->
                            (effect.spell == SpellType.FEAR_SPELL && effect.attackerId == attacker.id) ||
                                (
                                    effect.spell == SpellType.FEAR_SPELL_AREA &&
                                        effect.position != null &&
                                        attacker.position.value.hexDistanceTo(effect.position) <= 2
                                )
                        }
                    if (fearEffect != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            WarningIcon(size = 14.dp)
                            Text(
                                if (fearEffect.turnsRemaining > 0) {
                                    stringResource(Res.string.feared_turns_remaining, fearEffect.turnsRemaining)
                                } else {
                                    stringResource(Res.string.feared_label)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8B4513), // Dark brown / fear color
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Dragon-specific information
                    if (attacker.type.isDragon) {
                        // Greed level display
                        if (attacker.greed > 0) {
                            val greedLabel =
                                if (attacker.greed > 5) {
                                    stringResource(Res.string.very_greedy_label)
                                } else {
                                    stringResource(Res.string.greedy_label)
                                }
                            val greedDesc =
                                if (attacker.greed > 5) {
                                    stringResource(Res.string.very_greedy_desc)
                                } else {
                                    stringResource(Res.string.greedy_desc)
                                }

                            Column(
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        "${stringResource(Res.string.greed_level_label)}: ${attacker.greed} -",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GamePlayColors.ErrorDark,
                                    )
                                    Text(
                                        greedLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = GamePlayColors.ErrorDark,
                                    )
                                }
                                Text(
                                    greedDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GamePlayColors.Warning,
                                )
                            }
                        }

                        // Info button for dragons
                        TextButton(
                            onClick = onShowDragonInfo,
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            InfoIcon(size = 16.dp)
                            Text(
                                stringResource(Res.string.dragon_info_button),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    // Additional info about special abilities
                    val infoEntries = mutableListOf<AttackerInfoEntry>()
                    if (attacker.type.isVillain && attacker.type != AttackerType.THE_KRAKEN) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.WARNING,
                                text = stringResource(Res.string.ewhad_target_warning, attacker.type.getLocalizedShortName(locale)),
                                color = GamePlayColors.ErrorDark,
                                isBold = true,
                            ),
                        )
                    }
                    if (attacker.type.canSummon) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.LIGHTNING,
                                text = stringResource(Res.string.can_summon),
                                color = GamePlayColors.Warning,
                            ),
                        )
                    }
                    if (attacker.type.canHeal) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.HEART,
                                text = stringResource(Res.string.can_heal),
                                color = GamePlayColors.Success,
                            ),
                        )
                    }
                    if (attacker.type.canDisableTowers) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.LOCK,
                                text = stringResource(Res.string.can_disable_towers),
                                color = GamePlayColors.ErrorDark,
                            ),
                        )
                    }
                    if (attacker.type == AttackerType.GHOST) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.SHIELD,
                                text = stringResource(Res.string.can_only_be_harmed_by_magical_attacks),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.LIGHTNING,
                                text = stringResource(Res.string.ghost_movement_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                    } else if (attacker.type.immuneToAcid) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.SHIELD,
                                text = stringResource(Res.string.immune_to_acid),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                    }
                    if (attacker.type.immuneToFireball) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.SHIELD,
                                text = stringResource(Res.string.immune_to_fireball),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                    }
                    if (attacker.type.shieldWallFormationWidth > 0) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.SHIELD,
                                text = stringResource(Res.string.villain_freya_shield_wall_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                    }
                    if (attacker.type == AttackerType.PRINCE_VALERIUS_THE_SOULREAPER) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.LIGHTNING,
                                text = stringResource(Res.string.villain_valerius_soul_call_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                    }
                    if (attacker.type == AttackerType.GRAND_COVEN_MOTHER_SYBILLA ||
                        attacker.type == AttackerType.HAGA ||
                        attacker.type == AttackerType.ZUSSA
                    ) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.LIGHTNING,
                                text = stringResource(Res.string.villain_coven_synergy_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                    }
                    if (attacker.type == AttackerType.SYLVANAS_THE_MOLDING) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.LOCK,
                                text = stringResource(Res.string.villain_sylvanas_root_grip_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.HEART,
                                text = stringResource(Res.string.villain_sylvanas_self_heal_short),
                                color = GamePlayColors.Success,
                            ),
                        )
                    }
                    if (attacker.type == AttackerType.ARCHMAGE_MALAKOR_THE_RENEGADE) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.LOCK,
                                text = stringResource(Res.string.villain_malakor_time_loop_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.LIGHTNING,
                                text = stringResource(Res.string.villain_malakor_flies_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                    }

                    if (attacker.type == AttackerType.MORVATH_THE_SHADOWMASTER) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.LOCK,
                                text = stringResource(Res.string.villain_morvath_shadow_fog_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                    }

                    if (attacker.type == AttackerType.XARITHON_THE_SHADOW_DRAGON) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.LOCK,
                                text = stringResource(Res.string.villain_xarithon_shadow_spew_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.SHIELD,
                                text = stringResource(Res.string.villain_xarithon_immune_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                    }

                    if (attacker.type == AttackerType.CAPTAIN_RODERICH) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.SHIELD,
                                text = stringResource(Res.string.villain_roderich_seaworthy_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.LIGHTNING,
                                text = stringResource(Res.string.villain_roderich_broadside_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                    }

                    if (attacker.type == AttackerType.THE_KRAKEN) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.SHIELD,
                                text = stringResource(Res.string.villain_kraken_water_domain_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.SHIELD,
                                text = stringResource(Res.string.villain_kraken_dive_short),
                                color = GamePlayColors.InfoDark,
                            ),
                        )
                    }

                    // Mighty unit warning - for wizards, witches, demons, dragons
                    val isMightyUnit =
                        when (attacker.type) {
                            AttackerType.EVIL_WIZARD,
                            AttackerType.RED_WITCH,
                            AttackerType.GREEN_WITCH,
                            AttackerType.BLUE_DEMON,
                            AttackerType.RED_DEMON,
                            AttackerType.DRAGON,
                            -> true
                            else -> false
                        }
                    if (isMightyUnit) {
                        val damage = attacker.level.value
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.WARNING,
                                text = stringResource(Res.string.mighty_unit_warning, damage),
                                color = GamePlayColors.ErrorDark,
                                isBold = true,
                            ),
                        )
                    }

                    if (attacker.type.isVillain && !isMobile && infoEntries.size > VILLAIN_INFO_FIRST_COLUMN_MAX_ENTRIES) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                infoEntries
                                    .take(VILLAIN_INFO_FIRST_COLUMN_MAX_ENTRIES)
                                    .forEach { entry -> AttackerInfoEntryRow(entry) }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                infoEntries
                                    .drop(VILLAIN_INFO_FIRST_COLUMN_MAX_ENTRIES)
                                    .forEach { entry -> AttackerInfoEntryRow(entry) }
                            }
                        }
                    } else {
                        infoEntries.forEach { entry -> AttackerInfoEntryRow(entry) }
                    }
                }
                // Description column (desktop only – right of the stats column)
                // Displays descriptive text for all enemy types
                if (!isMobile) {
                    val description = getAttackerDescription(attacker.type)
                    if (description.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
                        ) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttackerInfoEntryRow(entry: AttackerInfoEntry) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (entry.icon) {
            AttackerInfoEntryIcon.WARNING -> WarningIcon(size = 14.dp)
            AttackerInfoEntryIcon.LIGHTNING -> LightningIcon(size = 14.dp)
            AttackerInfoEntryIcon.HEART -> HeartIcon(size = 14.dp)
            AttackerInfoEntryIcon.LOCK -> LockIcon(size = 14.dp)
            AttackerInfoEntryIcon.SHIELD -> ShieldIcon(size = 14.dp)
        }
        Text(
            text = entry.text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (entry.isBold) FontWeight.Bold else FontWeight.Normal,
            color = entry.color,
        )
    }
}
