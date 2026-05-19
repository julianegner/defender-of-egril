package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.SwordIcon
import de.egril.defender.ui.icon.SellTowerIcon
import de.egril.defender.ui.icon.UpgradeTowerIcon
import de.egril.defender.ui.settings.AppSettings
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.delay

@Composable
fun AttackButton(
    defender: Defender,
    gameState: GameState,
    selectedTargetId: Int?,
    selectedTargetPosition: Position?,
    onDefenderAttack: (Int, Int) -> Unit,
    onDefenderAttackPosition: (Int, Position) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp)
) {
    if (defender.isReady && defender.actionsRemaining.value > 0) {
        // Determine if attack mode is active (target or position selected)
        val isAttackModeActive = selectedTargetId != null || selectedTargetPosition != null
        
        // For AOE/DOT towers with position selected
        if ((defender.type.attackType == AttackType.AREA || defender.type.attackType == AttackType.LASTING) && selectedTargetPosition != null) {
            // If there's an enemy at the position, show enemy info
            if (selectedTargetId != null) {
                val target = gameState.attackers.find { it.id == selectedTargetId }
                if (target != null && defender.canAttack(target)) {
                    Button(
                        onClick = { onDefenderAttackPosition(defender.id, selectedTargetPosition) },
                        modifier = modifier,
                        border = if (isAttackModeActive) {
                            androidx.compose.foundation.BorderStroke(
                                width = 3.dp,
                                color = GamePlayColors.Yellow
                            )
                        } else null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GamePlayColors.ErrorDark
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SwordIcon(size = 24.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                val locale = com.hyperether.resources.currentLanguage.value
                                Text(
                                    stringResource(Res.string.attack_button),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                                Text(
                                    "${target.type.getLocalizedName(locale)} (${target.currentHealth.value}/${target.maxHealth} ${stringResource(Res.string.hp_label)}) + Area",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // No enemy at position, show position coordinates
                Button(
                    onClick = { onDefenderAttackPosition(defender.id, selectedTargetPosition) },
                    modifier = modifier,
                    border = if (isAttackModeActive) {
                        androidx.compose.foundation.BorderStroke(
                            width = 3.dp,
                            color = GamePlayColors.Yellow
                        )
                    } else null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GamePlayColors.ErrorDark
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SwordIcon(size = 24.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                stringResource(Res.string.attack_area_button),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "at (${selectedTargetPosition.x}, ${selectedTargetPosition.y})",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        } else if (selectedTargetId != null) {
            // For all towers, allow attacking enemies
            val target = gameState.attackers.find { it.id == selectedTargetId }
            if (target != null && defender.canAttack(target)) {
                Button(
                    onClick = { onDefenderAttack(defender.id, selectedTargetId) },
                    modifier = modifier,
                    border = if (isAttackModeActive) {
                        androidx.compose.foundation.BorderStroke(
                            width = 3.dp,
                            color = GamePlayColors.Yellow
                        )
                    } else null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GamePlayColors.ErrorDark
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SwordIcon(size = 24.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            val locale = com.hyperether.resources.currentLanguage.value
                            Text(
                                stringResource(Res.string.attack_button),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${target.type.getLocalizedName(locale)} (${target.currentHealth.value}/${target.maxHealth} ${stringResource(Res.string.hp_label)})",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        } else if (selectedTargetPosition != null && 
                   (defender.type.attackType == AttackType.MELEE || defender.type.attackType == AttackType.RANGED)) {
            // For single-target towers, allow attacking bridges when no enemy is present
            val bridge = gameState.getBridgeAt(selectedTargetPosition)
            if (bridge != null && bridge.isActive) {
                val distance = defender.position.value.distanceTo(selectedTargetPosition)
                if (distance >= defender.type.minRange && distance <= defender.range) {
                    Button(
                        onClick = { onDefenderAttackPosition(defender.id, selectedTargetPosition) },
                        modifier = modifier,
                        border = if (isAttackModeActive) {
                            androidx.compose.foundation.BorderStroke(
                                width = 3.dp,
                                color = GamePlayColors.Yellow
                            )
                        } else null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GamePlayColors.ErrorDark
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SwordIcon(size = 24.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    stringResource(Res.string.attack_button),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Bridge (${bridge.currentHealth.value}/${bridge.maxHealth} ${stringResource(Res.string.hp_label)})",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable button content for tower action buttons (upgrade / sell / undo).
 * Shows [icon] + [label] text + coin [amount] when [compact] is false, or [icon] only when [compact] is true.
 */
@Composable
private fun TowerActionButtonContent(
    compact: Boolean,
    icon: @Composable () -> Unit,
    label: String,
    amount: Int,
) {
    if (compact) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            icon()
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(GamePlayConstants.Spacing.IconText))
                Text(
                    label,
                    fontSize = GamePlayConstants.TextSizes.Medium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(GamePlayConstants.Spacing.Items))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoneyIcon(size = 14.dp)
                Spacer(modifier = Modifier.width(GamePlayConstants.Spacing.IconText))
                Text(
                    "$amount",
                    fontSize = GamePlayConstants.TextSizes.Large,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun UpgradeButton(
    defender: Defender,
    gameState: GameState,
    modifier: Modifier = Modifier
        .width(GamePlayConstants.ButtonSizes.ActionWidth)
        .height(GamePlayConstants.ButtonSizes.ActionHeight),
    onUpgradeDefender: (Int) -> Unit,
) {
    val upgradeLabel = stringResource(Res.string.upgrade)
    val coinsLabel = stringResource(Res.string.coins_label)
    val tooltipText = "$upgradeLabel: ${defender.upgradeCost} $coinsLabel"

    BoxWithConstraints(modifier = modifier) {
        val compact = maxHeight < GamePlayConstants.ButtonSizes.ActionCompactThreshold ||
                maxWidth < GamePlayConstants.ButtonSizes.ActionCompactWidthThreshold
        TooltipWrapper(text = if (compact) tooltipText else null) {
            Button(
                onClick = { onUpgradeDefender(defender.id) },
                enabled = gameState.canUpgradeDefender(defender),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = GamePlayConstants.Padding.Medium, vertical = GamePlayConstants.Padding.Small)
            ) {
                TowerActionButtonContent(
                    compact = compact,
                    icon = { UpgradeTowerIcon(size = 20.dp) },
                    label = upgradeLabel,
                    amount = defender.upgradeCost
                )
            }
        }
    }
}

@Composable
fun UndoOrSellButton(
    defender: Defender,
    gameState: GameState,
    onUndoTower: (Int) -> Unit,
    onSellTower: (Int) -> Unit,
    modifier: Modifier = Modifier
        .width(GamePlayConstants.ButtonSizes.ActionWidth)
        .height(GamePlayConstants.ButtonSizes.ActionHeight),
) {
    var showSellConfirmation by remember { mutableStateOf(false) }

    // Determine if undo or sell is available
    val canUndo = defender.placedOnTurn == gameState.turnNumber.value && !defender.hasBeenUsed.value
    val canSell = defender.isReady && defender.actionsRemaining.value > 0

    val coinsLabel = stringResource(Res.string.coins_label)

    if (canUndo) {
        // Show Undo button (100% refund)
        val undoLabel = stringResource(Res.string.undo)
        val tooltipText = "$undoLabel: ${defender.totalCost} $coinsLabel"
        BoxWithConstraints(modifier = modifier) {
            val compact = maxHeight < GamePlayConstants.ButtonSizes.ActionCompactThreshold ||
                    maxWidth < GamePlayConstants.ButtonSizes.ActionCompactWidthThreshold
            TooltipWrapper(text = if (compact) tooltipText else null) {
                Button(
                    onClick = { onUndoTower(defender.id) },
                    enabled = true,
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GamePlayColors.Success  // Green for undo
                    ),
                    contentPadding = PaddingValues(horizontal = GamePlayConstants.Padding.Medium, vertical = GamePlayConstants.Padding.Small)
                ) {
                    TowerActionButtonContent(
                        compact = compact,
                        icon = { SellTowerIcon(size = 20.dp) },
                        label = undoLabel,
                        amount = defender.totalCost
                    )
                }
            }
        }
    } else if (canSell) {
        // Show Sell button (75% refund)
        val sellAmount = (defender.totalCost * 0.75).toInt()
        val sellLabel = stringResource(Res.string.sell)
        val tooltipText = "$sellLabel: $sellAmount $coinsLabel"
        BoxWithConstraints(modifier = modifier) {
            val compact = maxHeight < GamePlayConstants.ButtonSizes.ActionCompactThreshold ||
                    maxWidth < GamePlayConstants.ButtonSizes.ActionCompactWidthThreshold
            TooltipWrapper(text = if (compact) tooltipText else null) {
                Button(
                    onClick = { showSellConfirmation = true },
                    enabled = true,
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GamePlayColors.Warning  // Orange for sell
                    ),
                    contentPadding = PaddingValues(horizontal = GamePlayConstants.Padding.Medium, vertical = GamePlayConstants.Padding.Small)
                ) {
                    TowerActionButtonContent(
                        compact = compact,
                        icon = { SellTowerIcon(size = 20.dp) },
                        label = sellLabel,
                        amount = sellAmount
                    )
                }
            }
        }

        // Confirmation dialog
        if (showSellConfirmation) {
            val locale = com.hyperether.resources.currentLanguage.value
            AlertDialog(
                onDismissRequest = { showSellConfirmation = false },
                title = { Text(stringResource(Res.string.sell_tower_title)) },
                text = {
                    val towerName = defender.type.getLocalizedName(locale)
                    Text(stringResource(Res.string.sell_tower_message, towerName, sellAmount.toString(), coinsLabel))
                },
                confirmButton = {
                    val holdToConfirmEnabled = AppSettings.holdToConfirmEnabled.value
                    if (holdToConfirmEnabled) {
                        val holdToSellLabel = stringResource(Res.string.sell_hold_to_confirm)
                        val holdToConfirmLabel = stringResource(Res.string.accessibility_hold_to_confirm)
                        val holdInteractionSource = remember { MutableInteractionSource() }
                        val isPressed by holdInteractionSource.collectIsPressedAsState()
                        var holdProgress by remember { mutableFloatStateOf(0f) }
                        val holdDurationMs = GamePlayConstants.ButtonSizes.HoldToConfirmDurationMs
                        val progressSteps = GamePlayConstants.ButtonSizes.HoldToConfirmProgressSteps

                        LaunchedEffect(isPressed) {
                            if (isPressed) {
                                holdProgress = 0f
                                repeat(progressSteps) { step ->
                                    delay(holdDurationMs / progressSteps)
                                    if (!isPressed) {
                                        holdProgress = 0f
                                        return@LaunchedEffect
                                    }
                                    holdProgress = (step + 1).toFloat() / progressSteps.toFloat()
                                }
                                holdProgress = 0f
                                onSellTower(defender.id)
                                showSellConfirmation = false
                            } else {
                                holdProgress = 0f
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (holdProgress > 0f) {
                                LinearProgressIndicator(
                                    progress = { holdProgress },
                                    modifier = Modifier
                                        .width(GamePlayConstants.ButtonSizes.HoldToConfirmProgressWidth)
                                        .height(GamePlayConstants.ButtonSizes.HoldToConfirmProgressHeight),
                                    color = GamePlayColors.Warning
                                )
                                Spacer(modifier = Modifier.height(GamePlayConstants.ButtonSizes.HoldToConfirmProgressSpacing))
                            }
                            Button(
                                onClick = { },
                                interactionSource = holdInteractionSource,
                                modifier = Modifier.semantics { stateDescription = holdToConfirmLabel },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GamePlayColors.Warning
                                )
                            ) {
                                Text(holdToSellLabel)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                onSellTower(defender.id)
                                showSellConfirmation = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GamePlayColors.Warning
                            )
                        ) {
                            Text(stringResource(Res.string.sell))
                        }
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showSellConfirmation = false }
                    ) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        }
    } else {
        // Show disabled button when neither undo nor sell is available
        Button(
            onClick = { },
            enabled = false,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SellTowerIcon(size = 14.dp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(Res.string.not_enough_actions),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
