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
import de.egril.defender.ui.icon.RightArrowIcon
import de.egril.defender.ui.icon.ShieldIcon
import de.egril.defender.ui.icon.SnowflakeIcon
import de.egril.defender.ui.icon.WarningIcon
import de.egril.defender.ui.icon.enemy.EnemyIcon
import defender_of_egril.composeapp.generated.resources.*

private const val VILLAIN_INFO_FIRST_COLUMN_MAX_ENTRIES = 4

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
 * Display details about a selected enemy attacker
 * Similar to DefenderInfo but for enemies
 */
@Composable
fun AttackerInfo(
    attacker: Attacker,
    activeSpellEffects: SnapshotStateList<ActiveSpellEffect> = androidx.compose.runtime.mutableStateListOf(),
    isMobile: Boolean = false,
    onShowDragonInfo: () -> Unit = {},
) {
    val locale = com.hyperether.resources.currentLanguage.value
    val attackerDisplayName =
        if (attacker.type.isDragon && attacker.dragonName != null) {
            "${stringResource(Res.string.the_dragon)} ${attacker.dragonName}"
        } else {
            attacker.getLocalizedName(locale)
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
            append(attacker.type.speed)
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
                    EnemyIcon(attacker = attacker, modifier = Modifier.size(iconInnerSize))
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
                    val cooledSpeed = if (coolingEffect != null) maxOf(0, barbsSpeed - 1) else null

                    // Pre-compute freeze effect for reuse throughout the Column
                    val freezeEffect =
                        activeSpellEffects.find {
                            it.spell == SpellType.FREEZE_SPELL && it.attackerId == attacker.id
                        }

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
                        if (attacker.level.value > 1) {
                            Text(
                                "Lvl ${attacker.level.value}",
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
                    if (attacker.type.isVillain) {
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
                    if (attacker.type.immuneToAcid) {
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
                    if (attacker.type.shieldWallRangeBehind > 0) {
                        infoEntries.add(
                            AttackerInfoEntry(
                                icon = AttackerInfoEntryIcon.SHIELD,
                                text = stringResource(Res.string.villain_freya_shield_wall_short),
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
                // Villain description column (desktop only – right of the stats column)
                if (attacker.type.isVillain && !isMobile) {
                    val villainDescription =
                        when (attacker.type) {
                            AttackerType.EWHAD -> stringResource(Res.string.villain_ewhad_description)
                            AttackerType.GAROKK -> stringResource(Res.string.villain_garokk_description)
                            AttackerType.SNOTLING_BOSS -> stringResource(Res.string.villain_gribnak_description)
                            AttackerType.MORGUK_BONEWHISPER -> stringResource(Res.string.villain_morguk_description)
                            AttackerType.ARAXXA -> stringResource(Res.string.villain_araxxa_description)
                            AttackerType.BARON_RATTERZAHN -> stringResource(Res.string.villain_ratterzahn_description)
                            AttackerType.FALLEN_SHIELDMAIDEN_FREYA -> stringResource(Res.string.villain_freya_description)
                            AttackerType.SILAS_THE_MASKMASTER,
                            AttackerType.SILAS_MIRROR_IMAGE,
                            -> stringResource(Res.string.villain_silas_description)
                            else -> ""
                        }
                    if (villainDescription.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
                        ) {
                            Text(
                                text = villainDescription,
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
