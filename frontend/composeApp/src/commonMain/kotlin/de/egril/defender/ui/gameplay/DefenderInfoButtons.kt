package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.model.*
import de.egril.defender.ui.icon.LightningIcon
import de.egril.defender.ui.icon.PickIcon
import de.egril.defender.ui.icon.TimerIcon
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.icon.WoodIcon
import defender_of_egril.composeapp.generated.resources.*

@Composable
internal fun dwarvenMineInfoButtonArea(defender: Defender) {
    // Mine info is now handled by the combined TowerInfoButtonArea
    // This function is kept for compatibility but does nothing
}

@Composable
internal fun RowScope.dwarvenMineActionButtonArea(
    type: DefenderType,
    gameState: GameState,
    defender: Defender,
    onMineAction: ((Int, MineAction) -> Unit)?,
    selectedMineAction: MineAction? = null, // Current trap placement mode
    compactBuyPanel: Boolean = false,
    horizontalSpacing: Dp = 8.dp,
    buttonHeight: Dp = 60.dp,
    hasExtraButtonColumns: Boolean = false,
) {
    if (type == DefenderType.DWARVEN_MINE) {
        val mineActionsEnabled =
            gameState.phase.value != GamePhase.INITIAL_BUILDING && defender.actionsRemaining.value > 0

        if (mineActionsEnabled || gameState.phase.value == GamePhase.INITIAL_BUILDING) {
            // Dig button
            Column(modifier = Modifier.weight(0.5f)) {
                Button(
                    onClick = { onMineAction?.invoke(defender.id, MineAction.DIG) },
                    enabled = mineActionsEnabled,
                    modifier =
                        Modifier
                            .width(240.dp)
                            .height(buttonHeight)
                            .padding(start = horizontalSpacing),
                    contentPadding =
                        PaddingValues(
                            horizontal = 4.dp,
                            vertical = 2.dp,
                        ),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PickIcon(size = 24.dp)
                        Text(stringResource(Res.string.dig), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        ShortcutKeyChip(text = "1")
                    }
                }
            }

            Column(modifier = Modifier.weight(0.5f)) {
                // Trap button
                val isTrapModeActive = selectedMineAction == MineAction.BUILD_TRAP
                Button(
                    onClick = {
                        onMineAction?.invoke(
                            defender.id,
                            MineAction.BUILD_TRAP,
                        )
                    },
                    enabled = mineActionsEnabled,
                    modifier =
                        Modifier
                            .width(240.dp)
                            .height(buttonHeight)
                            .padding(start = horizontalSpacing),
                    border =
                        if (isTrapModeActive) {
                            androidx.compose.foundation.BorderStroke(
                                width = 3.dp,
                                color = GamePlayColors.Yellow,
                            )
                        } else {
                            null
                        },
                    contentPadding =
                        PaddingValues(
                            horizontal = 4.dp,
                            vertical = 2.dp,
                        ),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TrapIcon(size = 24.dp)
                        Text(stringResource(Res.string.trap), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        ShortcutKeyChip(text = "2")
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (!compactBuyPanel) {
            Spacer(modifier = Modifier.weight(3f))
        }
    } else {
        // Only reserve placeholder space when there are additional button columns whose
        // presence would otherwise make the upgrade/sell buttons disproportionately narrow.
        // When no extra columns exist the upgrade/sell buttons can fill the available space.
        if (hasExtraButtonColumns) {
            Spacer(modifier = Modifier.weight(if (compactBuyPanel) 1f else 4f))
        }
    }
}

@Composable
fun MiningOutcomeGrid() {
    Column {
        // Header row
        Row {
            Text(stringResource(Res.string.dig_outcome_name), Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.dig_outcome_chance), Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.dig_outcome_reward), Modifier.weight(1f), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        // Data rows
        DigOutcome.values().forEach { outcome ->
            Row {
                Text(outcome.displayName, Modifier.weight(1f))
                Text("${outcome.probability}", Modifier.weight(1f))
                Text("${outcome.coins}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun DefenderActionsInfo(defender: Defender) {
    if (!defender.isReady) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimerIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Building: ${defender.buildTimeRemaining.value}T",
                style = MaterialTheme.typography.titleMedium,
                color = GamePlayColors.Warning,
            )
        }
    } else if (defender.isDisabled.value) {
        // Show disabled status by Red Witch
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            de.egril.defender.ui.icon
                .LockIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${stringResource(Res.string.disabled)}: ${defender.disabledTurnsRemaining.value}T",
                style = MaterialTheme.typography.titleMedium,
                color = GamePlayColors.ErrorDark,
                fontWeight = FontWeight.Bold,
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LightningIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${defender.actionsRemaining.value}/${defender.actionsPerTurnCalculated}",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/**
 * Button for wizard tower to generate mana
 * Generates base 5 mana + (level / 5) bonus mana
 * Always available when wizard has actions
 */
@Composable
fun GenerateManaButton(
    defender: Defender,
    gameState: GameState,
    onWizardAction: (Int, WizardAction) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp),
) {
    if (defender.isReady) {
        // Calculate mana generation amount
        val manaAmount = 5 + (defender.level.value / 5)
        val isAtMaxMana = gameState.currentMana.value >= gameState.maxMana.value

        // Button to generate mana - enabled when wizard has actions and not at max mana
        Button(
            onClick = { onWizardAction(defender.id, WizardAction.GENERATE_MANA) },
            enabled = defender.actionsRemaining.value > 0 && !isAtMaxMana,
            modifier = modifier,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0), // Purple color for mana
                ),
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 0.dp, end = 4.dp),
            ) {
                // Mana icon on the left
                de.egril.defender.ui.icon
                    .PentagramIcon(size = 40.dp)
                Spacer(modifier = Modifier.width(4.dp))
                // Two rows on the right
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                ) {
                    // Upper row: "Generate Mana"
                    Text(
                        stringResource(Res.string.generate_mana),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                    // Lower row: "+X Mana"
                    Text(
                        "+$manaAmount ${stringResource(Res.string.mana_label)}",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                    )
                    ShortcutKeyChip(text = "1")
                }
            }
        }
    }
}

/**
 * Button for wizard tower to place magical traps (level 10+)
 * Works like dwarven mine trap button - click to enter placement mode, then click on map
 * Always shows when wizard is ready, with cooldown info when on cooldown
 */
@Composable
fun MagicalTrapButton(
    defender: Defender,
    onWizardAction: (Int, WizardAction) -> Unit,
    selectedWizardAction: WizardAction? = null, // Current wizard trap placement mode
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp),
) {
    if (defender.isReady) {
        val isOnCooldown = defender.trapCooldownRemaining.value > 0
        val isTrapModeActive = selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP

        // Button to enter magical trap placement mode - enabled when trap is ready and has actions
        Button(
            onClick = { onWizardAction(defender.id, WizardAction.PLACE_MAGICAL_TRAP) },
            enabled = !isOnCooldown && defender.isReady && !defender.isDisabled.value && defender.actionsRemaining.value > 0,
            modifier = modifier,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = GamePlayColors.InfoDark,
                ),
            border =
                if (isTrapModeActive) {
                    androidx.compose.foundation.BorderStroke(
                        width = 3.dp,
                        color = GamePlayColors.Yellow,
                    )
                } else {
                    null
                },
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                de.egril.defender.ui.icon
                    .PentagramIcon(size = 24.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.magical_trap),
                        fontSize = if (isOnCooldown) 14.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ShortcutKeyChip(text = "2")
                }
                if (isOnCooldown) {
                    Column {
                        Text(
                            defender.trapCooldownRemaining.value.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Button for spike/spear tower to place barricades
 * Spike Tower: level 20+ with HP = (level - 20) / 2 (minimum 1)
 * Spear Tower: level 10+ with HP = level - 10 (minimum 1)
 * Works like wizard magical trap button - click to enter placement mode, then click on map
 */
@Composable
fun BarricadeButton(
    defender: Defender,
    onBarricadeAction: (Int, BarricadeAction) -> Unit,
    selectedBarricadeAction: BarricadeAction? = null, // Current barricade placement mode
    doubleLevelEffect: ActiveSpellEffect? = null, // Active DOUBLE_TOWER_LEVEL spell effect
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp),
) {
    if (defender.isReady) {
        // Calculate HP that will be added based on tower type, using effective level
        val effectiveLevel = if (doubleLevelEffect != null) defender.level.value * 2 else defender.level.value
        val hpAmount =
            if (defender.type == DefenderType.SPIKE_TOWER) {
                maxOf(1, (effectiveLevel - 20) / 2)
            } else {
                maxOf(1, effectiveLevel - 10)
            }
        val isBarricadeModeActive = selectedBarricadeAction == BarricadeAction.BUILD_BARRICADE

        // Button to enter barricade placement mode - enabled when tower has actions
        Button(
            onClick = { onBarricadeAction(defender.id, BarricadeAction.BUILD_BARRICADE) },
            enabled = defender.actionsRemaining.value > 0,
            modifier = modifier,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF795548), // Brown color for wood
                ),
            border =
                if (isBarricadeModeActive) {
                    androidx.compose.foundation.BorderStroke(
                        width = 3.dp,
                        color = GamePlayColors.Yellow,
                    )
                } else {
                    null
                },
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 0.dp, end = 4.dp),
            ) {
                // Icon on the left
                WoodIcon(size = 40.dp)
                Spacer(modifier = Modifier.width(4.dp))
                // Two rows on the right
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                ) {
                    // Upper row: "Barricade"
                    Text(
                        stringResource(Res.string.barricade),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                    // Lower row: "X HP"
                    Text(
                        "$hpAmount ${stringResource(Res.string.hp_label)}",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                    )
                    ShortcutKeyChip(text = "1")
                }
            }
        }
    }
}
