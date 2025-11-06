package com.defenderofegril.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.model.*
import com.defenderofegril.ui.*

@Composable
fun ColumnScope.TurnButton(
    isPlayerTurn: Boolean,
    modifier: Modifier,
    onPrimaryAction: () -> Unit = {},
    primaryButtonColor: Color = GamePlayColors.WarningDeep
    ){
    Button(
        onClick = onPrimaryAction,
        // modifier = Modifier.fillMaxWidth(),
        colors = if (isPlayerTurn) {
            ButtonDefaults.buttonColors(containerColor = primaryButtonColor)
        } else {
            ButtonDefaults.buttonColors()
        },
        modifier = modifier
    ) {
        Text(if (isPlayerTurn) "End Turn" else "Start Battle",
            style = MaterialTheme.typography.labelMedium,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            fontSize = 14.sp,
            maxLines = 1,
            )
    }
}

@Composable
fun GameControlsPanel(
    phase: GamePhase,
    gameState: GameState,
    coinsState: State<Int>,
    selectedDefenderType: DefenderType?,
    selectedDefenderId: Int?,
    selectedTargetId: Int?,
    selectedTargetPosition: Position?,
    onSelectDefenderType: (DefenderType?) -> Unit,
    onUpgradeDefender: (Int) -> Unit,
    onUndoTower: (Int) -> Unit,
    onSellTower: (Int) -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onDefenderAttackPosition: (Int, Position) -> Boolean,
    onPrimaryAction: () -> Unit,
    onMineAction: ((Int, MineAction) -> Unit)? = null,
    uiScale: Float = 1f  // Add platform scale parameter
) {
    // Automatically fold buy panel when a defender is selected
    val compactBuyPanel = selectedDefenderId != null

    // Determine phase-specific properties
    val isPlayerTurn = phase == GamePhase.PLAYER_TURN
    val title = if (isPlayerTurn) {
        "Your Turn - Place towers and attack enemies"
    } else {
        "Initial Building Phase - Place towers (no build time)"
    }
    val primaryButtonText = if (isPlayerTurn) "End Turn" else "Start Battle"
    val primaryButtonColor = if (isPlayerTurn) {
        GamePlayColors.WarningDeep
    } else {
        ButtonDefaults.buttonColors().containerColor
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Title - hide when tower is selected
        if (!compactBuyPanel) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (compactBuyPanel) {
            // Folded view: Compact layout with defender info on left, buy buttons and End Turn on right
            Row(modifier = Modifier.fillMaxWidth()) {
                // Selected defender info on left (smaller)
                selectedDefenderId?.let { defenderId ->
                    val defender = gameState.defenders.find { it.id == defenderId }
                    if (defender != null) {
                        Box(modifier = Modifier.weight(1f)) {
                            DefenderInfo(
                                defender,
                                gameState,
                                onUpgradeDefender,
                                onUndoTower,
                                onSellTower,
                                onMineAction = onMineAction,
                                compactBuyPanel,
                                isMobile = uiScale < 1f,
                                selectedTargetId = selectedTargetId,
                                selectedTargetPosition = selectedTargetPosition,
                                onDefenderAttack = onDefenderAttack,
                                onDefenderAttackPosition = onDefenderAttackPosition,
                                isPlayerTurn = isPlayerTurn
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Right side: buy buttons and End Turn button
                Column(modifier = Modifier
                    .width(600.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)

                ) {

                    val isMobile = uiScale < 1f
                    val compactDefenderButtonModifier = Modifier
                        .fillMaxWidth()
                        .height(if (isMobile) 45.dp else 45.dp)

                    // Compact buy buttons
                    LazyVerticalGrid(
                        modifier = Modifier.padding(top = 8.dp),
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        val types = DefenderType.entries
                            // hack: we need dragons lair as the last one, as the last one is overridden by the Next turn Button
                            // .filter { it != DefenderType.DRAGONS_LAIR }
                            .toTypedArray()

                        itemsIndexed(types, key = { index: Int, type: DefenderType -> "${type.name}_folded_${coinsState.value}" }) { index: Int, type: DefenderType ->
                            val isLast = index == types.lastIndex
                            CompactDefenderButton(
                                type = type,
                                isSelected = selectedDefenderType == type,
                                canAfford = coinsState.value >= type.baseCost,
                                modifier = compactDefenderButtonModifier,
                                onClick = {
                                    onSelectDefenderType(if (selectedDefenderType == type) null else type)
                                }
                            )
                            if (isLast) {
                                TurnButton(
                                    isPlayerTurn,
                                    modifier = compactDefenderButtonModifier,
                                    onPrimaryAction
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Expanded view: Original layout
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth().height(75.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    DefenderType.entries
                        .filter { it != DefenderType.DRAGONS_LAIR }
                        .toTypedArray(),
                    key = { type -> "${type.name}_${coinsState.value}_${gameState.defenders.count { it.type == type }}" }) { type ->
                    val canAfford = coinsState.value >= type.baseCost
                    println("DEBUG: ${phase.name} Button for ${type.displayName} - coins: ${coinsState.value}, cost: ${type.baseCost}, canAfford: $canAfford")
                    DefenderButton(
                        type = type,
                        isSelected = selectedDefenderType == type,
                        canAfford = canAfford,
                        coinsState = coinsState,
                        onClick = {
                            onSelectDefenderType(if (selectedDefenderType == type) null else type)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Selected defender info and attack button
            selectedDefenderId?.let { defenderId ->
                val defender = gameState.defenders.find { it.id == defenderId }
                if (defender != null) {
                    DefenderInfo(
                        defender,
                        gameState,
                        onUpgradeDefender,
                        onUndoTower,
                        onSellTower,
                        onMineAction = onMineAction,
                        compactBuyPanel,
                        isMobile = uiScale < 1f,
                        selectedTargetId = selectedTargetId,
                        selectedTargetPosition = selectedTargetPosition,
                        onDefenderAttack = onDefenderAttack,
                        onDefenderAttackPosition = onDefenderAttackPosition,
                        isPlayerTurn = isPlayerTurn
                    )
                }
            }
        }

        // End Turn button - only show in expanded view (not compact)
        if (!compactBuyPanel) {
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onPrimaryAction,
                modifier = Modifier.fillMaxWidth(),
                colors = if (isPlayerTurn) {
                    ButtonDefaults.buttonColors(containerColor = primaryButtonColor)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(primaryButtonText)
            }
        }
    }
}

@Composable
fun EnemyTurnInfo() {
    // The ViewModel automatically handles the delays and phase progression
    // This composable displays the enemy turn indicator with animation
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GamePlayColors.DangerCardBackground)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Enemy Turn",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(color = Color.Red)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Enemies are spawning and moving...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Watch the grid for changes!",
                    style = MaterialTheme.typography.bodySmall,
                    color = GamePlayColors.ErrorDark,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}
