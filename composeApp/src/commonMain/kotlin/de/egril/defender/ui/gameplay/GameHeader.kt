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
    val headerTextSize = de.egril.defender.ui.settings.AppSettings.headerTextSize.value
    
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
                    onCheatCode = onCheatCode,
                    headerTextSize = headerTextSize
                )
            }

            // Level name in center (without prefix, bold when collapsed)
            val locale = com.hyperether.resources.currentLanguage.value
            val titleFontSize = when (headerTextSize) {
                de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.TextSizes.Body
                de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> GamePlayConstants.TextSizes.Medium
                de.egril.defender.ui.settings.HeaderTextSize.LARGE -> GamePlayConstants.TextSizes.Large
            }
            Text(
                text = gameState.level.getLocalizedTitle(locale),
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // Buttons and difficulty at far right
            val buttonHeight = when (headerTextSize) {
                de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.ButtonSizes.CompactHeight
                de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> 40.dp
                de.egril.defender.ui.settings.HeaderTextSize.LARGE -> 48.dp
            }
            val buttonIconSize = when (headerTextSize) {
                de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.IconSizes.Medium
                de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> GamePlayConstants.IconSizes.Large
                de.egril.defender.ui.settings.HeaderTextSize.LARGE -> GamePlayConstants.IconSizes.ExtraLarge
            }
            val buttonTextSize = when (headerTextSize) {
                de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.TextSizes.Body
                de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> GamePlayConstants.TextSizes.Medium
                de.egril.defender.ui.settings.HeaderTextSize.LARGE -> GamePlayConstants.TextSizes.Large
            }
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
                    modifier = Modifier.size(buttonHeight)
                )
                
                if (onSaveGame != null) {
                    Button(
                        onClick = onSaveGame,
                        modifier = Modifier.height(buttonHeight),
                        contentPadding = PaddingValues(horizontal = GamePlayConstants.Spacing.Items, vertical = 0.dp)
                    ) {
                        SaveIcon(
                            size = buttonIconSize,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }

                Button(
                    onClick = onBackToMap,
                    modifier = Modifier.height(buttonHeight),
                    contentPadding = PaddingValues(horizontal = GamePlayConstants.Spacing.Items, vertical = 0.dp)
                ) {
                    Text(
                        stringResource(Res.string.map_label),
                        fontSize = buttonTextSize,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Button(
                    onClick = { onShowOverlayChange(!showOverlay) },
                    modifier = Modifier.height(buttonHeight),
                    contentPadding = PaddingValues(horizontal = GamePlayConstants.Spacing.Items, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showOverlay) GamePlayColors.Success else GamePlayColors.Info
                    )
                ) {
                    if (showOverlay) {
                        TriangleRightIcon(size = buttonIconSize)
                    } else {
                        TriangleLeftIcon(size = buttonIconSize)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameStats(
    gameState: GameState,
    onCheatCode: (() -> Unit)?,
    headerTextSize: de.egril.defender.ui.settings.HeaderTextSize
) {
    val iconSize = when (headerTextSize) {
        de.egril.defender.ui.settings.HeaderTextSize.SMALL -> GamePlayConstants.IconSizes.Large
        de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> GamePlayConstants.IconSizes.ExtraLarge
        de.egril.defender.ui.settings.HeaderTextSize.LARGE -> 32.dp
    }
    val textStyle = when (headerTextSize) {
        de.egril.defender.ui.settings.HeaderTextSize.SMALL -> MaterialTheme.typography.bodyLarge
        de.egril.defender.ui.settings.HeaderTextSize.MEDIUM -> MaterialTheme.typography.titleMedium
        de.egril.defender.ui.settings.HeaderTextSize.LARGE -> MaterialTheme.typography.titleLarge
    }
    
    GameStatsDisplay(
        coins = gameState.coins.value,
        health = gameState.healthPoints.value,
        turn = gameState.turnNumber.value,
        activeEnemyCount = gameState.getActiveEnemyCount(),
        remainingEnemyCount = gameState.getRemainingEnemyCount(),
        iconSize = iconSize,
        textStyle = textStyle,
        onCoinsClick = onCheatCode
    )
}
