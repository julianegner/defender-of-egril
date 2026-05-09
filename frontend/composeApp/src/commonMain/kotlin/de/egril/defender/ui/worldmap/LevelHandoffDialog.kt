package de.egril.defender.ui.worldmap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyperether.resources.stringResource
import de.egril.defender.model.Level
import de.egril.defender.model.WorldLevel
import de.egril.defender.save.LevelHandoffSave
import de.egril.defender.ui.hexagon.HexagonMinimap
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.TowerIcon
import de.egril.defender.ui.icon.LightningIcon
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.*
/**
 * Dialog shown when starting a level that is connected to the previous level.
 * Offers two cards: "Fresh Start" and "Continue with previous build-up".
 * The "Continue" option is selected by default.
 */
@Composable
fun LevelHandoffDialog(
    worldLevel: WorldLevel,
    handoff: LevelHandoffSave,
    onStartFresh: () -> Unit,
    onStartWithHandoff: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDarkMode = AppSettings.isDarkMode.value
    var selectedOption by remember { mutableStateOf(HandoffOption.CONTINUE) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .widthIn(min = 500.dp, max = 900.dp)
                .heightIn(max = 700.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = stringResource(Res.string.level_handoff_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Text(
                    text = worldLevel.level.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDarkMode) Color.LightGray else Color.DarkGray
                )

                // Two cards side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Continue card (left, default)
                    HandoffOptionCard(
                        title = stringResource(Res.string.level_handoff_continue),
                        isSelected = selectedOption == HandoffOption.CONTINUE,
                        modifier = Modifier.weight(1f),
                        isDarkMode = isDarkMode,
                        onClick = { selectedOption = HandoffOption.CONTINUE }
                    ) {
                        HandoffContinueCardContent(
                            handoff = handoff,
                            level = worldLevel.level,
                            isDarkMode = isDarkMode
                        )
                    }

                    // Fresh start card (right)
                    HandoffOptionCard(
                        title = stringResource(Res.string.level_handoff_fresh_start),
                        isSelected = selectedOption == HandoffOption.FRESH_START,
                        modifier = Modifier.weight(1f),
                        isDarkMode = isDarkMode,
                        onClick = { selectedOption = HandoffOption.FRESH_START }
                    ) {
                        HandoffFreshStartCardContent(
                            level = worldLevel.level,
                            isDarkMode = isDarkMode
                        )
                    }
                }

                // Start button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.cancel))
                    }
                    Button(
                        onClick = {
                            if (selectedOption == HandoffOption.CONTINUE) {
                                onStartWithHandoff()
                            } else {
                                onStartFresh()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) Color(0xFF2E7D32) else Color(0xFF4CAF50),
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(Res.string.start_battle))
                    }
                }
            }
        }
    }
}

private enum class HandoffOption {
    CONTINUE, FRESH_START
}

@Composable
private fun HandoffOptionCard(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val selectedBorderColor = if (isDarkMode) Color(0xFF4CAF50) else Color(0xFF2E7D32)
    val selectedBgColor = if (isDarkMode) Color(0xFF1B3A1F) else Color(0xFFE8F5E9)
    val normalBgColor = if (isDarkMode) Color(0xFF3C3C3C) else Color(0xFFFFFFFF)

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(BorderStroke(2.dp, selectedBorderColor), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) selectedBgColor else normalBgColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = selectedBorderColor
                    )
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDarkMode) Color.White else Color.Black
                )
            }
            content()
        }
    }
}

@Composable
private fun HandoffContinueCardContent(
    handoff: LevelHandoffSave,
    level: Level,
    isDarkMode: Boolean
) {
    val textColor = if (isDarkMode) Color.LightGray else Color.DarkGray
    val towerCount = handoff.defenders.count { it.buildTimeRemaining == 0 }
    val barricadeCount = handoff.barricades.size
    val trapCount = handoff.traps.size

    // Stats row
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MoneyIcon(size = 14.dp)
            Text(
                text = handoff.coins.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
        if (handoff.maxMana > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LightningIcon(size = 14.dp)
                Text(
                    text = "${handoff.currentMana} / ${handoff.maxMana}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TowerIcon(size = 14.dp)
            Text(
                text = stringResource(Res.string.level_handoff_towers, towerCount.toString()),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
        if (barricadeCount > 0) {
            Text(
                text = stringResource(Res.string.level_handoff_barricades, barricadeCount.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
        if (trapCount > 0) {
            Text(
                text = stringResource(Res.string.level_handoff_traps, trapCount.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
    }

    // Minimap showing towers
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        HexagonMinimap(
            level = level,
            config = MinimapConfig(
                showSpawnPoints = true,
                showTarget = true,
                showTowers = false,
                showEnemies = false,
                showViewport = false,
                backgroundColor = Color.Transparent,
                borderColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun HandoffFreshStartCardContent(
    level: Level,
    isDarkMode: Boolean
) {
    val textColor = if (isDarkMode) Color.LightGray else Color.DarkGray

    // Stats row with level defaults
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MoneyIcon(size = 14.dp)
            Text(
                text = level.initialCoins.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TowerIcon(size = 14.dp)
            Text(
                text = stringResource(Res.string.level_handoff_no_towers),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }

    // Minimap showing only map (no towers)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        HexagonMinimap(
            level = level,
            config = MinimapConfig(
                showSpawnPoints = true,
                showTarget = true,
                showTowers = false,
                showEnemies = false,
                showViewport = false,
                backgroundColor = Color.Transparent,
                borderColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}
