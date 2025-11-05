package com.defenderofegril.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.defenderofegril.model.*
import com.defenderofegril.ui.*

@Composable
fun GameHeader(
    gameState: GameState,
    headerExpanded: Boolean,
    showOverlay: Boolean,
    onHeaderExpandedChange: (Boolean) -> Unit,
    onShowOverlayChange: (Boolean) -> Unit,
    onBackToMap: () -> Unit,
    onSaveGame: (() -> Unit)?,
    onCheatCode: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .zIndex(2f)
    ) {
        if (headerExpanded) {
            ExpandedGameHeader(
                gameState = gameState,
                showOverlay = showOverlay,
                onHeaderCollapse = { onHeaderExpandedChange(false) },
                onShowOverlayChange = onShowOverlayChange,
                onBackToMap = onBackToMap,
                onSaveGame = onSaveGame,
                onCheatCode = onCheatCode
            )
        } else {
            CompactGameHeader(
                gameState = gameState,
                showOverlay = showOverlay,
                onHeaderExpand = { onHeaderExpandedChange(true) },
                onShowOverlayChange = onShowOverlayChange,
                onBackToMap = onBackToMap,
                onSaveGame = onSaveGame,
                onCheatCode = onCheatCode
            )
        }
    }
}

@Composable
private fun ExpandedGameHeader(
    gameState: GameState,
    showOverlay: Boolean,
    onHeaderCollapse: () -> Unit,
    onShowOverlayChange: (Boolean) -> Unit,
    onBackToMap: () -> Unit,
    onSaveGame: (() -> Unit)?,
    onCheatCode: (() -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // First row: Level name centered with fold button on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty spacer for symmetry
            Spacer(modifier = Modifier.width(100.dp))

            // Level name centered (without "Level:" prefix)
            Text(
                text = gameState.level.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // Fold button at right end (same size as other buttons)
            Button(
                onClick = onHeaderCollapse
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TriangleUpIcon(size = 14.dp, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fold Header")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main header content
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameStats(
                gameState = gameState,
                onCheatCode = onCheatCode
            )

            PhaseIndicator(phase = gameState.phase.value)

            HeaderActions(
                showOverlay = showOverlay,
                onShowOverlayChange = onShowOverlayChange,
                onBackToMap = onBackToMap,
                onSaveGame = onSaveGame
            )
        }
    }
}

@Composable
private fun CompactGameHeader(
    gameState: GameState,
    showOverlay: Boolean,
    onHeaderExpand: () -> Unit,
    onShowOverlayChange: (Boolean) -> Unit,
    onBackToMap: () -> Unit,
    onSaveGame: (() -> Unit)?,
    onCheatCode: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Statistics at far left
        CompactStatsRow(
            coins = gameState.coins.value,
            health = gameState.healthPoints.value,
            turn = gameState.turnNumber.value,
            onCoinsClick = onCheatCode
        )

        // Level name in center (without prefix, bold when collapsed)
        Text(
            text = gameState.level.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        // Three buttons at far right (four on mobile if save is available)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onSaveGame != null) {
                Button(
                    onClick = onSaveGame,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    SaveIcon(size = 16.dp, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }

            Button(
                onClick = onBackToMap,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Map", fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterVertically))
            }

            Button(
                onClick = { onShowOverlayChange(!showOverlay) },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showOverlay) GamePlayColors.Success else GamePlayColors.Info
                )
            ) {
                if (showOverlay) {
                    TriangleRightIcon(size = 12.dp)
                } else {
                    TriangleLeftIcon(size = 12.dp)
                }
            }

            // Fold button
            Button(
                onClick = onHeaderExpand,
                modifier = Modifier.size(32.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                TriangleDownIcon(size = 12.dp, tint = Color.White)
            }
        }
    }
}

@Composable
private fun GameStats(
    gameState: GameState,
    onCheatCode: (() -> Unit)?
) {
    GameStatsDisplay(
        coins = gameState.coins.value,
        health = gameState.healthPoints.value,
        turn = gameState.turnNumber.value,
        iconSize = GamePlayConstants.IconSizes.Large,
        textStyle = MaterialTheme.typography.bodyLarge,
        onCoinsClick = onCheatCode
    )
    
    // Enemy count display
    val activeEnemies = gameState.attackers.count { !it.isDefeated.value }
    val totalSpawned = gameState.nextAttackerId.value - 1
    val plannedSpawns = gameState.spawnPlan.drop(totalSpawned)
    val remainingEnemies = plannedSpawns.size

    Text(
        "Enemies: $activeEnemies active, $remainingEnemies to come",
        style = MaterialTheme.typography.bodyMedium,
        color = GamePlayColors.Error
    )
}

@Composable
private fun PhaseIndicator(phase: GamePhase) {
    val phaseText = when (phase) {
        GamePhase.INITIAL_BUILDING -> "Initial Building Phase"
        GamePhase.PLAYER_TURN -> "YOUR TURN"
        GamePhase.ENEMY_TURN -> "ENEMY TURN"
    }
    val phaseColor = when (phase) {
        GamePhase.INITIAL_BUILDING -> GamePlayColors.Info
        GamePhase.PLAYER_TURN -> GamePlayColors.Success
        GamePhase.ENEMY_TURN -> GamePlayColors.Error
    }
    Text(
        text = phaseText,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = phaseColor,
        modifier = Modifier.background(phaseColor.copy(alpha = 0.1f)).padding(12.dp)
    )
}

@Composable
private fun HeaderActions(
    showOverlay: Boolean,
    onShowOverlayChange: (Boolean) -> Unit,
    onBackToMap: () -> Unit,
    onSaveGame: (() -> Unit)?
) {
    Column {
        Button(onClick = onBackToMap) {
            Text("Back to Map")
        }
        
        if (onSaveGame != null) {
            Button(
                onClick = onSaveGame
            ) {
                Text("Save Game")
            }
        }

        // Toggle button positioned above the map and far to the right
        Button(
            onClick = { onShowOverlayChange(!showOverlay) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showOverlay) GamePlayColors.Success else GamePlayColors.Info
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (showOverlay) "Hide Info" else "Show Info")
                Spacer(modifier = Modifier.width(4.dp))
                if (showOverlay) {
                    TriangleRightIcon(size = 18.dp, tint = Color.White)
                } else {
                    TriangleLeftIcon(size = 18.dp, tint = Color.White)
                }
            }
        }
    }
}
