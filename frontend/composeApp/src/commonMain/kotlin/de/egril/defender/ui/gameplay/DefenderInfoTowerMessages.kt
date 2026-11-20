package de.egril.defender.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.*
import de.egril.defender.ui.icon.HammerIcon
import de.egril.defender.ui.icon.InfoIcon
import de.egril.defender.ui.icon.SwordIcon
import de.egril.defender.ui.icon.WoodIcon
import defender_of_egril.composeapp.generated.resources.*

/**
 * Calculate the damage for a defender at a specific level
 */
private fun calculateDamage(
    defender: Defender,
    level: Int,
): Int = defender.type.baseDamage + (level - 1) * 5

/**
 * Calculate the actual damage (accounting for LASTING attack type) for a defender at a specific level
 */
internal fun calculateActualDamage(
    defender: Defender,
    level: Int,
): Int {
    val baseDamage = calculateDamage(defender, level)
    return when (defender.type.attackType) {
        AttackType.LASTING -> baseDamage / 2
        else -> baseDamage
    }
}

/**
 * Calculate trap damage for dwarven mine at a specific level
 */
internal fun calculateTrapDamage(
    defender: Defender,
    level: Int,
): Int =
    if (defender.type == DefenderType.DWARVEN_MINE) {
        10 + ((level / 2) * 5)
    } else {
        0
    }

/**
 * Calculate the range for a defender at a specific level
 */
internal fun calculateRange(
    defender: Defender,
    level: Int,
): Int {
    val baseCalculatedRange =
        if (defender.type == DefenderType.DWARVEN_MINE) {
            // Dwarven mine has special growth: 3 base + 1 every 5 levels
            3 + (level / 5)
        } else {
            // Standard growth: baseRange + (level - 1) / 2
            defender.type.baseRange + (level - 1) / 2
        }
    // Apply maxRange cap if defined for this tower type
    return if (defender.type.maxRange != null) {
        minOf(baseCalculatedRange, defender.type.maxRange)
    } else {
        baseCalculatedRange
    }
}

/**
 * Calculate actions per turn for a defender at a specific level
 */
internal fun calculateActionsPerTurn(
    defender: Defender,
    level: Int,
): Int =
    if (defender.type == DefenderType.SPIKE_TOWER) {
        val bonusActions = level / 5
        minOf(defender.type.actionsPerTurn + bonusActions, 3)
    } else if (defender.type == DefenderType.DWARVEN_MINE) {
        1 + (level / 5)
    } else {
        defender.type.actionsPerTurn
    }

/**
 * Info message data for displaying in combined tower info dialog
 */
private data class TowerInfoMessage(
    val title: String,
    val message: String,
    val icon: @Composable () -> Unit,
    val color: Color,
    val extraContent: (@Composable () -> Unit)? = null, // Optional additional content after message
)

/**
 * Get all relevant info messages for a specific tower
 */
@Composable
private fun getTowerInfoMessages(
    defender: Defender,
    gameState: GameState,
): List<TowerInfoMessage> {
    val messages = mutableListOf<TowerInfoMessage>()

    // Add first-use info message for the tower type
    when (defender.type) {
        DefenderType.ALTAR -> {
            messages.add(
                TowerInfoMessage(
                    title = stringResource(Res.string.tower_altar),
                    message = stringResource(Res.string.altar_conversion_info),
                    icon = {
                        de.egril.defender.ui.TowerTypeIcon(
                            defenderType = DefenderType.ALTAR,
                            modifier = Modifier.size(56.dp),
                        )
                    },
                    color = Color(0xFFFFD700),
                ),
            )
        }
        DefenderType.WIZARD_TOWER -> {
            messages.add(
                TowerInfoMessage(
                    title = stringResource(Res.string.wizard_first_use_title),
                    message = stringResource(Res.string.wizard_first_use_message),
                    icon = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray, CircleShape),
                            // Gray background for visibility
                            contentAlignment = Alignment.Center,
                        ) {
                            de.egril.defender.ui.TowerTypeIcon(
                                defenderType = DefenderType.WIZARD_TOWER,
                                modifier = Modifier.size(56.dp), // Slightly smaller than container
                            )
                        }
                    },
                    color = Color(0xFF9C27B0), // Purple
                ),
            )
        }
        DefenderType.ALCHEMY_TOWER -> {
            messages.add(
                TowerInfoMessage(
                    title = stringResource(Res.string.alchemy_first_use_title),
                    message = stringResource(Res.string.alchemy_first_use_message),
                    icon = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray, CircleShape),
                            // Gray background for visibility
                            contentAlignment = Alignment.Center,
                        ) {
                            de.egril.defender.ui.TowerTypeIcon(
                                defenderType = DefenderType.ALCHEMY_TOWER,
                                modifier = Modifier.size(56.dp), // Slightly smaller than container
                            )
                        }
                    },
                    color = Color(0xFF4CAF50), // Green
                ),
            )
        }
        DefenderType.BALLISTA_TOWER -> {
            messages.add(
                TowerInfoMessage(
                    title = stringResource(Res.string.ballista_first_use_title),
                    message = stringResource(Res.string.ballista_first_use_message),
                    icon = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray, CircleShape),
                            // Gray background for visibility
                            contentAlignment = Alignment.Center,
                        ) {
                            de.egril.defender.ui.TowerTypeIcon(
                                defenderType = DefenderType.BALLISTA_TOWER,
                                modifier = Modifier.size(56.dp), // Slightly smaller than container
                            )
                        }
                    },
                    color = Color(0xFF795548), // Brown
                ),
            )
        }
        DefenderType.DWARVEN_MINE -> {
            messages.add(
                TowerInfoMessage(
                    title = stringResource(Res.string.mine_first_use_title),
                    message = stringResource(Res.string.mine_first_use_message),
                    icon = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray, CircleShape),
                            // Gray background for visibility
                            contentAlignment = Alignment.Center,
                        ) {
                            de.egril.defender.ui.TowerTypeIcon(
                                defenderType = DefenderType.DWARVEN_MINE,
                                modifier = Modifier.size(56.dp), // Slightly smaller than container
                            )
                        }
                    },
                    color = Color(0xFFFFD700), // Gold
                    extraContent = {
                        // Add mining probabilities section
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(Res.string.mining_probabilities),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MiningOutcomeGrid()
                    },
                ),
            )
        }
        else -> {}
    }

    // Add ability info messages based on tower level

    // Spike barbs info (spike tower level 10+ AND Construction level 1+)
    if (defender.type == DefenderType.SPIKE_TOWER &&
        defender.level.value >= 10 &&
        gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_1
    ) {
        messages.add(
            TowerInfoMessage(
                title = stringResource(Res.string.spike_barbs_info_title),
                message = stringResource(Res.string.spike_barbs_info_message),
                icon = { SwordIcon(size = 32.dp) },
                color = Color(0xFF8B4513), // SaddleBrown
            ),
        )
    }

    // Barricade info (spike tower level 20+ with Construction 2+ or spear tower level 10+ with Construction 1+)
    val hasBarricadeAbility =
        when (defender.type) {
            DefenderType.SPIKE_TOWER ->
                defender.level.value >= 20 &&
                    gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_2
            DefenderType.SPEAR_TOWER ->
                defender.level.value >= 10 &&
                    gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_1
            else -> false
        }
    if (hasBarricadeAbility) {
        messages.add(
            TowerInfoMessage(
                title = stringResource(Res.string.barricade_info_title),
                message = stringResource(Res.string.barricade_info_message),
                icon = { WoodIcon(size = 32.dp) },
                color = Color(0xFF795548), // Brown
            ),
        )
    }

    // Barricade hint: tower has level but construction ability is insufficient
    val needsConstructionUpgrade =
        when (defender.type) {
            DefenderType.SPIKE_TOWER ->
                defender.level.value >= 20 &&
                    gameState.constructionLevel < PlayerAbilities.CONSTRUCTION_LEVEL_2
            DefenderType.SPEAR_TOWER ->
                defender.level.value >= 10 &&
                    gameState.constructionLevel < PlayerAbilities.CONSTRUCTION_LEVEL_1
            else -> false
        }
    if (needsConstructionUpgrade) {
        val requiredLevel =
            when (defender.type) {
                DefenderType.SPIKE_TOWER -> PlayerAbilities.CONSTRUCTION_LEVEL_2
                else -> PlayerAbilities.CONSTRUCTION_LEVEL_1
            }
        messages.add(
            0,
            TowerInfoMessage(
                title = stringResource(Res.string.barricade_construction_hint_title),
                message = stringResource(Res.string.barricade_construction_hint_message, requiredLevel),
                icon = { HammerIcon(size = 32.dp) },
                color = Color(0xFFFF9800), // Orange (warning)
            ),
        )
    }

    // Magical trap info (wizard tower level 10+)
    if (defender.hasMagicalTraps && defender.level.value >= 10) {
        messages.add(
            TowerInfoMessage(
                title = stringResource(Res.string.magical_trap_tutorial_title),
                message = stringResource(Res.string.magical_trap_tutorial_message),
                icon = {
                    de.egril.defender.ui.icon
                        .PentagramIcon(size = 32.dp)
                },
                color = Color(0xFF9C27B0), // Purple
            ),
        )
    }

    // Extended area info (wizard or alchemy tower level 20+)
    if ((defender.type == DefenderType.WIZARD_TOWER || defender.type == DefenderType.ALCHEMY_TOWER) &&
        defender.level.value >= 20
    ) {
        messages.add(
            TowerInfoMessage(
                title = stringResource(Res.string.extended_area_tutorial_title),
                message = stringResource(Res.string.extended_area_tutorial_message),
                icon = {
                    de.egril.defender.ui.icon
                        .ExplosionIcon(size = 32.dp)
                },
                color = Color(0xFFFF5722), // Deep orange
            ),
        )
    }

    return messages
}

/**
 * Tower info icon and dialog component (similar to mine info icon)
 * Triggers InfoState to show dialog as proper overlay
 */
@Composable
internal fun TowerInfoButtonArea(
    defender: Defender,
    gameState: GameState,
) {
    val messages = getTowerInfoMessages(defender, gameState)

    // Only show info icon if there are info messages for this tower
    if (messages.isEmpty()) {
        return
    }

    // Info icon button - triggers InfoState to show dialog as overlay
    InfoIcon(
        size = 16.dp,
        modifier =
            Modifier
                .clickable {
                    gameState.infoState.value =
                        gameState.infoState.value.showInfo(
                            type = InfoType.TOWER_INFO,
                            towerId = defender.id,
                        )
                }.padding(4.dp),
    )
}

/**
 * Tower info dialog displayed as overlay (accessed by GamePlayScreen)
 * Shows all relevant info for the specified tower
 */
@Composable
internal fun TowerInfoDialog(
    defender: Defender,
    gameState: GameState,
    onDismiss: () -> Unit,
) {
    val messages = getTowerInfoMessages(defender, gameState)

    ScrollableInfoCard(
        title = {
            Text(
                text = stringResource(Res.string.tower_info_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        width = 600.dp,
        maxHeight = 500.dp,
        onDismiss = onDismiss,
    ) {
        messages.forEachIndexed { index, info ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            // Subtitle with icon (doubled in size)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Box(modifier = Modifier.size(64.dp)) {
                    // Doubled from 32.dp
                    info.icon()
                }
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = info.color,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Message content
            Text(
                text = info.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Extra content (if any)
            info.extraContent?.invoke()
        }
    }
}
