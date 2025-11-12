package com.defenderofegril.ui.gameplay

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
import com.defenderofegril.model.*
import com.defenderofegril.ui.*
import com.defenderofegril.ui.icon.HoleIcon
import com.defenderofegril.ui.icon.InfoIcon
import com.defenderofegril.ui.icon.LightningIcon
import com.defenderofegril.ui.icon.PickIcon
import com.defenderofegril.ui.icon.SwordIcon
import com.defenderofegril.ui.icon.TimerIcon
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
                            if (dragonAlive) stringResource(Res.string.dragons_lair) else stringResource(Res.string.empty_dragons_lair)
                        } else {
                            defender.type.displayName
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
                        Text(
                            stringResource(Res.string.dragons_lair_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {

                        // Normal tower stats and buttons
                        val baseDamage =
                            if (defender.type == DefenderType.DWARVEN_MINE) defender.trapDamage else defender.damage
                        val nextLevelDamage = baseDamage + 5
                        val nextActualDamage = when (defender.type.attackType) {
                            AttackType.LASTING -> nextLevelDamage / 2
                            else -> nextLevelDamage
                        }
                        val nextLevel = defender.level.value + 1
                        val nextRangeCalculated = defender.type.baseRange + (nextLevel - 1) / 2
                        val nextRange = if (defender.type == DefenderType.SPIKE_TOWER && nextLevel >= 5) {
                            minOf(nextRangeCalculated, 2)
                        } else {
                            nextRangeCalculated
                        }
                        val nextActions =
                            if (defender.type == DefenderType.SPIKE_TOWER || defender.type == DefenderType.DWARVEN_MINE) {
                                val bonusActions = nextLevel / 5
                                minOf(1 + bonusActions, 3)
                            } else {
                                defender.type.actionsPerTurn
                            }
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

                            dwarvenMineActionButtonArea(
                                defender.type,
                                gameState,
                                defender,
                                onMineAction,
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
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = 2.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HoleIcon(size = 24.dp)
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
            Text("Name", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Chance (%)", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Reward", Modifier.weight(1f), fontWeight = FontWeight.Bold)
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
