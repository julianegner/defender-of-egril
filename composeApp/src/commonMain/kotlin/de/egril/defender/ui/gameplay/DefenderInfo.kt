package de.egril.defender.ui.gameplay

import androidx.compose.foundation.clickable
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
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.icon.HoleIcon
import de.egril.defender.ui.icon.InfoIcon
import de.egril.defender.ui.icon.LightningIcon
import de.egril.defender.ui.icon.PickIcon
import de.egril.defender.ui.icon.SwordIcon
import de.egril.defender.ui.icon.TimerIcon
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.icon.WoodIcon
import com.hyperether.resources.AppLocale
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun DefenderInfo(
    defender: Defender,
    gameState: GameState,
    onUpgradeDefender: (Int) -> Unit,
    onUndoTower: (Int) -> Unit,
    onSellTower: (Int) -> Unit,
    onMineAction: ((Int, MineAction) -> Unit)? = null,
    onWizardAction: ((Int, WizardAction) -> Unit)? = null,  // For wizard tower magical traps - click to select action, then click map
    selectedMineAction: MineAction? = null,  // Current trap placement mode
    selectedWizardAction: WizardAction? = null,  // Current wizard trap placement mode
    onBarricadeAction: ((Int, BarricadeAction) -> Unit)? = null,  // For spike/spear tower barricades - click to select action, then click map
    selectedBarricadeAction: BarricadeAction? = null,  // Current barricade placement mode
    compactBuyPanel: Boolean = false,
    isMobile: Boolean = false,  // Add platform parameter
    selectedTargetId: Int? = null,
    selectedTargetPosition: Position? = null,
    onDefenderAttack: ((Int, Int) -> Boolean)? = null,
    onDefenderAttackPosition: ((Int, Position) -> Boolean)? = null,
    isPlayerTurn: Boolean = false
) {
    val locale = com.hyperether.resources.currentLanguage.value
    val buttonHeight = if (isMobile) 100.dp else 60.dp
    // Use key to force recomposition when defender stats change
    key(
        defender.id,
        defender.level,
        defender.damage,
        defender.range,
        defender.actionsRemaining.value,
        defender.buildTimeRemaining.value,
        defender.isReady
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isMobile) 4.dp else 8.dp)
        ) {
                // Tower icon, name, and actions in one row
                Row(
                    // modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    // Tower icon - double the original size
                    val iconSize = if (isMobile) 64.dp else 96.dp
                    val iconInnerSize = if (isMobile) 56.dp else 88.dp
                    Box(
                        modifier = Modifier.size(iconSize),
                        contentAlignment = Alignment.Center
                    ) {
                        TowerIcon(defender = defender, modifier = Modifier.size(iconInnerSize), gameState = gameState)
                    }

                    val horizontalSpacing = if (isMobile) 4.dp else 8.dp
                    Spacer(modifier = Modifier.width(horizontalSpacing))

                    // Tower name and level
                    Column(modifier = Modifier.weight(1f)
                    ) {
                        val displayName = if (defender.type == DefenderType.DRAGONS_LAIR) {
                            // Check if the specific dragon from this lair is still alive
                            val dragonAlive = defender.dragonId.value?.let { dragonId ->
                                gameState.attackers.any {
                                    it.id == dragonId && !it.isDefeated.value
                                }
                            } ?: false
                            if (dragonAlive) {
                                // Show "Lair of the Dragon [name]" in normal text above the italic desc
                                if (defender.dragonName != null) {
                                    "${stringResource(Res.string.lair_of_the_dragon)} ${defender.dragonName}"
                                } else {
                                    stringResource(Res.string.dragons_lair)
                                }
                            } else {
                                stringResource(Res.string.empty_dragons_lair)
                            }
                        } else if (defender.raftId.value != null) {
                            // If defender is on a raft, show localized "Type Raft" with space separator
                            // Use getLocalizedShortName() to get just the type (e.g., "Bow" instead of "Bow Tower")
                            "${defender.type.getLocalizedShortName(locale)} ${stringResource(Res.string.raft)}"
                        } else {
                            // Regular tower - use localized name
                            defender.type.getLocalizedName(locale)
                        }
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Level ${defender.level.value}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GamePlayColors.Success
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SwordIcon(size = 12.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    defender.type.attackType.getLocalizedName(locale),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                        // Show tower base info if on tower base
                        val towerBase = gameState.barricades.find { it.id == defender.towerBaseBarricadeId.value }
                        if (towerBase != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                WoodIcon(size = 12.dp)
                                Text(
                                    stringResource(Res.string.tower_base_hp_label, towerBase.healthPoints.value),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (towerBase.healthPoints.value < 100) GamePlayColors.Warning else GamePlayColors.Success
                                )
                            }
                        }
                        Row {
                            DefenderActionsInfo(defender)
                            dwarvenMineInfoButtonArea(defender)
                        }
                    }
                
                // Show undo button for towers that are still building (in same turn as placed)
                if (!defender.isReady && defender.placedOnTurn == gameState.turnNumber.value && !defender.hasBeenUsed.value) {
                    Spacer(modifier = Modifier.width(horizontalSpacing))
                    Column(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        UndoOrSellButton(
                            defender = defender,
                            gameState = gameState,
                            onUndoTower = onUndoTower,
                            onSellTower = onSellTower,
                            modifier = Modifier
                                .width(240.dp)
                                .height(buttonHeight)
                        )
                    }
                }

                if (defender.isReady) {
                    if (defender.type == DefenderType.DRAGONS_LAIR) {
                        // Dragon's lair - no actions, can't be sold
                        // Show dragon name in normal text if available
                        val dragonAlive = defender.dragonId.value?.let { dragonId ->
                            gameState.attackers.any {
                                it.id == dragonId && !it.isDefeated.value
                            }
                        } ?: false
                        
                        if (dragonAlive && defender.dragonName != null) {
                            Text(
                                "${stringResource(Res.string.lair_of_the_dragon)} ${defender.dragonName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GamePlayColors.ErrorDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            stringResource(Res.string.dragons_lair_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {

                        // Normal tower stats and buttons
                        // Calculate next level stats using helper functions
                        val currentLevel = defender.level.value
                        val nextLevel = currentLevel + 1
                        val nextActualDamage = if (defender.type == DefenderType.DWARVEN_MINE) {
                            calculateTrapDamage(defender, nextLevel)
                        } else {
                            calculateActualDamage(defender, nextLevel)
                        }
                        val nextRange = calculateRange(defender, nextLevel)
                        val nextActions = calculateActionsPerTurn(defender, nextLevel)

                        // Current stats column
                            Column(modifier = Modifier.weight(0.5f)) {
                                Text(
                                    "Lvl ${defender.level.value}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                TowerStats(
                                    defender.type.minRange,
                                    if (defender.type == DefenderType.DWARVEN_MINE) defender.trapDamage else defender.actualDamage,
                                    defender.range,
                                    defender.actionsPerTurnCalculated
                                )
                            }

                            // After upgrade stats column
                            Column(modifier = Modifier.weight(0.5f)) {
                                Text(
                                    "Lvl $nextLevel",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (gameState.canUpgradeDefender(defender)) GamePlayColors.Success else Color.Gray
                                )
                                TowerStats(
                                    defender.type.minRange,
                                    nextActualDamage,
                                    nextRange,
                                    nextActions
                                )
                            }

                            Column(modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                UpgradeButton(defender, gameState,
                                    onUpgradeDefender = onUpgradeDefender,
                                    modifier = Modifier
                                        .width(240.dp)
                                        .height(buttonHeight),
                                )
                            }
                            Spacer(modifier = Modifier.width(horizontalSpacing))
                            Column(modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center,
                                ) {
                                UndoOrSellButton(
                                    defender = defender,
                                    gameState = gameState,
                                    onUndoTower = onUndoTower,
                                    onSellTower = onSellTower,
                                    modifier = Modifier
                                        .width(240.dp)
                                        .height(buttonHeight)
                                )
                            }

                            if (isPlayerTurn &&
                                defender.type != DefenderType.DWARVEN_MINE &&
                                defender.type != DefenderType.DRAGONS_LAIR &&
                                onDefenderAttack != null &&
                                onDefenderAttackPosition != null) {

                                Spacer(modifier = Modifier.width(horizontalSpacing))
                                Column(modifier = Modifier.weight(1.3f)) {
                                    AttackButton(
                                        defender = defender,
                                        gameState = gameState,
                                        selectedTargetId = selectedTargetId,
                                        selectedTargetPosition = selectedTargetPosition,
                                        onDefenderAttack = { defenderId, targetId ->
                                            onDefenderAttack(defenderId, targetId)
                                        },
                                        onDefenderAttackPosition = { defenderId, targetPos ->
                                            onDefenderAttackPosition(defenderId, targetPos)
                                        },
                                        modifier = Modifier
                                            .width(240.dp)
                                            .height(buttonHeight)
                                    )
                                }
                            }

                            // Magical trap button for wizard tower level 10+
                            if (isPlayerTurn &&
                                defender.type == DefenderType.WIZARD_TOWER &&
                                defender.level.value >= 10 &&
                                onWizardAction != null) {

                                Spacer(modifier = Modifier.width(horizontalSpacing))
                                Column(modifier = Modifier.weight(1.3f)) {
                                    MagicalTrapButton(
                                        defender = defender,
                                        onWizardAction = onWizardAction,
                                        selectedWizardAction = selectedWizardAction,
                                        modifier = Modifier
                                            .width(240.dp)
                                            .height(buttonHeight)
                                    )
                                }
                            }

                            // Barricade button for spike tower (level 20+) or spear tower (level 10+)
                            if (isPlayerTurn && onBarricadeAction != null) {
                                val canBuildBarricade = when (defender.type) {
                                    DefenderType.SPIKE_TOWER -> defender.level.value >= 20
                                    DefenderType.SPEAR_TOWER -> defender.level.value >= 10
                                    else -> false
                                }
                                
                                if (canBuildBarricade) {
                                    Spacer(modifier = Modifier.width(horizontalSpacing))
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        BarricadeButton(
                                            defender = defender,
                                            onBarricadeAction = onBarricadeAction,
                                            selectedBarricadeAction = selectedBarricadeAction,
                                            modifier = Modifier
                                                .width(240.dp)
                                                .height(buttonHeight)
                                        )
                                    }
                                }
                            }

                            dwarvenMineActionButtonArea(
                                defender.type,
                                gameState,
                                defender,
                                onMineAction,
                                selectedMineAction,
                                compactBuyPanel,
                                horizontalSpacing,
                                buttonHeight
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun dwarvenMineInfoButtonArea(defender: Defender) {
    if (defender.type == DefenderType.DWARVEN_MINE) {
        // Mine-specific UI with info dialog
        var showMiningInfoDialog by remember { mutableStateOf(false) }

        // Mining info dialog
        if (showMiningInfoDialog) {
            AlertDialog(
                onDismissRequest = { showMiningInfoDialog = false },
                title = { Text(stringResource(Res.string.mining_probabilities)) },
                text = { MiningOutcomeGrid() },
                confirmButton = {
                    TextButton(onClick = { showMiningInfoDialog = false }) {
                        Text(stringResource(Res.string.close))
                    }
                }
            )
        }

        InfoIcon(
            size = 16.dp,
            modifier = Modifier
                .clickable { showMiningInfoDialog = true }
                .padding(4.dp)
        )
    }
}

@Composable
private fun RowScope.dwarvenMineActionButtonArea(
    type: DefenderType,
    gameState: GameState,
    defender: Defender,
    onMineAction: ((Int, MineAction) -> Unit)?,
    selectedMineAction: MineAction? = null,  // Current trap placement mode
    compactBuyPanel: Boolean = false,
    horizontalSpacing: Dp = 8.dp,
    buttonHeight: Dp = 60.dp

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
                    modifier = Modifier
                        .width(240.dp)
                        .height(buttonHeight)
                        .padding(start = horizontalSpacing),
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = 2.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PickIcon(size = 24.dp)
                        Text(stringResource(Res.string.dig), fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                            MineAction.BUILD_TRAP
                        )
                    },
                    enabled = mineActionsEnabled,
                    modifier = Modifier
                        .width(240.dp)
                        .height(buttonHeight)
                        .padding(start = horizontalSpacing),
                    border = if (isTrapModeActive) {
                        androidx.compose.foundation.BorderStroke(
                            width = 3.dp,
                            color = GamePlayColors.Yellow
                        )
                    } else null,
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = 2.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TrapIcon(size = 24.dp)
                        Text(stringResource(Res.string.trap), fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
        Spacer(modifier = Modifier.weight(if (compactBuyPanel) 1f else 4f))
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimerIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Building: ${defender.buildTimeRemaining.value}T",
                style = MaterialTheme.typography.titleMedium,
                color = GamePlayColors.Warning
            )
        }
    } else if (defender.isDisabled.value) {
        // Show disabled status by Red Witch
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            de.egril.defender.ui.icon.LockIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${stringResource(Res.string.disabled)}: ${defender.disabledTurnsRemaining.value}T",
                style = MaterialTheme.typography.titleMedium,
                color = GamePlayColors.ErrorDark,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically
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
 * Button for wizard tower to place magical traps (level 10+)
 * Works like dwarven mine trap button - click to enter placement mode, then click on map
 * Always shows when wizard is ready, with cooldown info when on cooldown
 */
@Composable
fun MagicalTrapButton(
    defender: Defender,
    onWizardAction: (Int, WizardAction) -> Unit,
    selectedWizardAction: WizardAction? = null,  // Current wizard trap placement mode
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp)
) {
    if (defender.isReady) {
        val isOnCooldown = defender.trapCooldownRemaining.value > 0
        val isTrapModeActive = selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP

        // Button to enter magical trap placement mode - enabled when trap is ready and has actions
        Button(
            onClick = { onWizardAction(defender.id, WizardAction.PLACE_MAGICAL_TRAP) },
            enabled = !isOnCooldown && defender.actionsRemaining.value > 0,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = GamePlayColors.InfoDark
            ),
            border = if (isTrapModeActive) {
                androidx.compose.foundation.BorderStroke(
                    width = 3.dp,
                    color = GamePlayColors.Yellow
                )
            } else null
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                de.egril.defender.ui.icon.PentagramIcon(size = 24.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(3f)) {
                    Text(
                        stringResource(Res.string.magical_trap),
                        fontSize = if (isOnCooldown) 14.sp else 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isOnCooldown) {
                    Column(modifier = Modifier.weight(1f)) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            defender.trapCooldownRemaining.value.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
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
    selectedBarricadeAction: BarricadeAction? = null,  // Current barricade placement mode
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp)
) {
    if (defender.isReady) {
        // Calculate HP that will be added based on tower type
        val hpAmount = if (defender.type == DefenderType.SPIKE_TOWER) {
            maxOf(1, (defender.level.value - 20) / 2)
        } else {
            maxOf(1, defender.level.value - 10)
        }
        val isBarricadeModeActive = selectedBarricadeAction == BarricadeAction.BUILD_BARRICADE
        
        // Button to enter barricade placement mode - enabled when tower has actions
        Button(
            onClick = { onBarricadeAction(defender.id, BarricadeAction.BUILD_BARRICADE) },
            enabled = defender.actionsRemaining.value > 0,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF795548)  // Brown color for wood
            ),
            border = if (isBarricadeModeActive) {
                androidx.compose.foundation.BorderStroke(
                    width = 3.dp,
                    color = GamePlayColors.Yellow
                )
            } else null
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 0.dp, end = 4.dp)
            ) {
                // Icon on the left
                WoodIcon(size = 40.dp)
                Spacer(modifier = Modifier.width(4.dp))
                // Two rows on the right
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Upper row: "Barricade"
                    Text(
                        stringResource(Res.string.barricade),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    // Lower row: "X HP"
                    Text(
                        "$hpAmount ${stringResource(Res.string.hp_label)}",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }
    }
}

/**
 * Calculate the damage for a defender at a specific level
 */
private fun calculateDamage(defender: Defender, level: Int): Int {
    return defender.type.baseDamage + (level - 1) * 5
}

/**
 * Calculate the actual damage (accounting for LASTING attack type) for a defender at a specific level
 */
private fun calculateActualDamage(defender: Defender, level: Int): Int {
    val baseDamage = calculateDamage(defender, level)
    return when (defender.type.attackType) {
        AttackType.LASTING -> baseDamage / 2
        else -> baseDamage
    }
}

/**
 * Calculate trap damage for dwarven mine at a specific level
 */
private fun calculateTrapDamage(defender: Defender, level: Int): Int {
    return if (defender.type == DefenderType.DWARVEN_MINE) {
        10 + ((level / 2) * 5)
    } else {
        0
    }
}

/**
 * Calculate the range for a defender at a specific level
 */
private fun calculateRange(defender: Defender, level: Int): Int {
    val baseCalculatedRange = if (defender.type == DefenderType.DWARVEN_MINE) {
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
private fun calculateActionsPerTurn(defender: Defender, level: Int): Int {
    return if (defender.type == DefenderType.SPIKE_TOWER) {
        val bonusActions = level / 5
        minOf(defender.type.actionsPerTurn + bonusActions, 3)
    } else if (defender.type == DefenderType.DWARVEN_MINE) {
        1 + (level / 5)
    } else {
        defender.type.actionsPerTurn
    }
}
