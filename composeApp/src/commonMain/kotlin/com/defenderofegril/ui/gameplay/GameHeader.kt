package com.defenderofegril.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.defenderofegril.model.*
import com.defenderofegril.ui.*
import com.defenderofegril.ui.icon.SaveIcon
import com.defenderofegril.ui.icon.TriangleLeftIcon
import com.defenderofegril.ui.icon.TriangleRightIcon

@Composable
fun GameHeader(
    gameState: GameState,
    showOverlay: Boolean,
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Statistics at far left
            Row(
                horizontalArrangement = Arrangement.spacedBy(GamePlayConstants.Spacing.Items),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameStats(
                    gameState = gameState,
                    onCheatCode = onCheatCode
                )
            }

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
                        modifier = Modifier.height(GamePlayConstants.ButtonSizes.CompactHeight),
                        contentPadding = PaddingValues(horizontal = GamePlayConstants.Spacing.Items, vertical = 0.dp)
                    ) {
                        SaveIcon(
                            size = GamePlayConstants.IconSizes.Medium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }

                Button(
                    onClick = onBackToMap,
                    modifier = Modifier.height(GamePlayConstants.ButtonSizes.CompactHeight),
                    contentPadding = PaddingValues(horizontal = GamePlayConstants.Spacing.Items, vertical = 0.dp)
                ) {
                    Text(
                        "Map",
                        fontSize = GamePlayConstants.TextSizes.Body,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Button(
                    onClick = { onShowOverlayChange(!showOverlay) },
                    modifier = Modifier.height(GamePlayConstants.ButtonSizes.CompactHeight),
                    contentPadding = PaddingValues(horizontal = GamePlayConstants.Spacing.Items, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showOverlay) GamePlayColors.Success else GamePlayColors.Info
                    )
                ) {
                    if (showOverlay) {
                        TriangleRightIcon(size = GamePlayConstants.IconSizes.Medium)
                    } else {
                        TriangleLeftIcon(size = GamePlayConstants.IconSizes.Medium)
                    }
                }
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
        activeEnemyCount = gameState.getActiveEnemyCount(),
        remainingEnemyCount = gameState.getRemainingEnemyCount(),
        iconSize = GamePlayConstants.IconSizes.Large,
        textStyle = MaterialTheme.typography.bodyLarge,
        onCoinsClick = onCheatCode
    )
}
