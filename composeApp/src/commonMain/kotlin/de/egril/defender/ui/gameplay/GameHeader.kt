package de.egril.defender.ui.gameplay

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
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.icon.SaveIcon
import de.egril.defender.ui.icon.TriangleLeftIcon
import de.egril.defender.ui.icon.TriangleRightIcon
import de.egril.defender.ui.settings.SettingsButton
import de.egril.defender.ui.settings.DifficultyDisplay
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

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
            val locale = com.hyperether.resources.currentLanguage.value
            Text(
                text = gameState.level.getLocalizedTitle(locale),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // Buttons and difficulty at far right
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Difficulty display (non-clickable on gameplay screen)
                DifficultyDisplay(
                    isClickable = false
                )
                
                // Settings button (icon only to save space)
                SettingsButton(
                    modifier = Modifier.size(GamePlayConstants.ButtonSizes.CompactHeight)
                )
                
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
                        stringResource(Res.string.map_label),
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
