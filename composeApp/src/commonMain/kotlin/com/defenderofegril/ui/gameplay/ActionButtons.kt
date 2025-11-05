package com.defenderofegril.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.model.*
import com.defenderofegril.ui.*

// UI Constants
private val ATTACK_BUTTON_COLOR = Color(0xFFD32F2F)

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
        // For AOE/DOT towers with position selected
        if ((defender.type.attackType == AttackType.AREA || defender.type.attackType == AttackType.LASTING) && selectedTargetPosition != null) {
            // If there's an enemy at the position, show enemy info
            if (selectedTargetId != null) {
                val target = gameState.attackers.find { it.id == selectedTargetId }
                if (target != null && defender.canAttack(target)) {
                    Button(
                        onClick = { onDefenderAttackPosition(defender.id, selectedTargetPosition) },
                        modifier = modifier,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ATTACK_BUTTON_COLOR
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
                                    "ATTACK",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                                Text(
                                    "${target.type.displayName} (${target.currentHealth.value}/${target.maxHealth} HP) + Area",
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ATTACK_BUTTON_COLOR
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
                                "ATTACK AREA",
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ATTACK_BUTTON_COLOR
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
                                "ATTACK",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${target.type.displayName} (${target.currentHealth.value}/${target.maxHealth} HP)",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradeButton(
    defender: Defender,
    gameState: GameState,
    modifier: Modifier = Modifier
        .width(240.dp)
        .height(60.dp),
    onUpgradeDefender: (Int) -> Unit,
) {
    Button(
        onClick = { onUpgradeDefender(defender.id) },
        enabled = gameState.canUpgradeDefender(defender),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Upgrade", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoneyIcon(size = 14.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${defender.upgradeCost}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
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
        .width(240.dp)
        .height(60.dp),
) {
    var showSellConfirmation by remember { mutableStateOf(false) }

    // Determine if undo or sell is available
    val canUndo = defender.placedOnTurn == gameState.turnNumber.value && !defender.hasBeenUsed.value
    val canSell = defender.isReady && defender.actionsRemaining.value > 0

    if (canUndo) {
        // Show Undo button (100% refund)
        Button(
            onClick = { onUndoTower(defender.id) },
            enabled = true,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)  // Green for undo
            ),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Undo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MoneyIcon(size = 14.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${defender.totalCost}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else if (canSell) {
        // Show Sell button (75% refund)
        val sellAmount = (defender.totalCost * 0.75).toInt()
        Button(
            onClick = { showSellConfirmation = true },
            enabled = true,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800)  // Orange for sell
            ),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Sell", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MoneyIcon(size = 14.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "$sellAmount",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Confirmation dialog
        if (showSellConfirmation) {
            AlertDialog(
                onDismissRequest = { showSellConfirmation = false },
                title = { Text("Sell Tower?") },
                text = {
                    Text("Do you really want to sell the ${defender.type.displayName} for $sellAmount coins?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onSellTower(defender.id)
                            showSellConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Text("Sell")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showSellConfirmation = false }
                    ) {
                        Text("Cancel")
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
                Text("Sell", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "not enough Actions",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
