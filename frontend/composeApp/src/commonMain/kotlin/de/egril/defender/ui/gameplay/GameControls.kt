package de.egril.defender.ui.gameplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.config.LogConfig
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.gameplay.defenderButtons.CompactDefenderButton
import de.egril.defender.ui.gameplay.defenderButtons.DefenderButton
import de.egril.defender.ui.isMobileWebBrowser
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.HeaderTextSize
import de.egril.defender.ui.settings.formatShortcutBindingForDisplay
import de.egril.defender.utils.isPlatformMobile
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.end_turn_button
import defender_of_egril.composeapp.generated.resources.enemy_turn_title
import defender_of_egril.composeapp.generated.resources.initial_building_phase
import defender_of_egril.composeapp.generated.resources.start_battle
import defender_of_egril.composeapp.generated.resources.your_turn_message

@Composable
private fun turnButtonShortcutHintColor(
    isPlayerTurn: Boolean,
    primaryButtonColor: Color,
): Color =
    if (isPlayerTurn) {
        GamePlayColors.readableContentColor(primaryButtonColor).copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
    }

/** Font size for the primary turn button label. Uses larger sizes than other controls for readability. */
private fun turnButtonLabelFontSize(headerTextSize: HeaderTextSize) =
    when (headerTextSize) {
        HeaderTextSize.SMALL -> GamePlayConstants.TextSizes.TurnButtonSmall
        HeaderTextSize.MEDIUM -> GamePlayConstants.TextSizes.TurnButtonMedium
        HeaderTextSize.LARGE -> GamePlayConstants.TextSizes.TurnButtonLarge
    }

@Composable
fun ColumnScope.TurnButton(
    isPlayerTurn: Boolean,
    modifier: Modifier,
    onPrimaryAction: () -> Unit = {},
    primaryButtonColor: Color = GamePlayColors.WarningDeep,
    highlighted: Boolean = false,
    autoAttackAvailable: Boolean = false,
    autoAttackManaOnly: Boolean = false,
) {
    val buttonTextSize = turnButtonLabelFontSize(AppSettings.headerTextSize.value)
    val turnButtonContentColor = GamePlayColors.readableContentColor(primaryButtonColor)
    val shortcutHintColor = turnButtonShortcutHintColor(isPlayerTurn, primaryButtonColor)
    val buttonLabel =
        when {
            !isPlayerTurn -> stringResource(Res.string.start_battle)
            autoAttackAvailable && autoAttackManaOnly -> stringResource(Res.string.generate_mana_and_end_turn)
            autoAttackAvailable -> stringResource(Res.string.auto_attack_button)
            else -> stringResource(Res.string.end_turn_button)
        }
    val tooltipText =
        if (isPlayerTurn && autoAttackAvailable) {
            if (autoAttackManaOnly) {
                stringResource(Res.string.generate_mana_and_end_turn)
            } else {
                stringResource(Res.string.auto_attack_and_end_turn)
            }
        } else {
            null
        }
    TooltipWrapper(text = tooltipText, preferAbove = true) {
        Button(
            onClick = onPrimaryAction,
            // modifier = Modifier.fillMaxWidth(),
            colors =
                if (isPlayerTurn) {
                    ButtonDefaults.buttonColors(
                        containerColor = primaryButtonColor,
                        contentColor = turnButtonContentColor,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            border =
                if (highlighted && isPlayerTurn) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
            modifier = modifier,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    buttonLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    autoSize =
                        TextAutoSize.StepBased(
                            minFontSize = 9.sp,
                            maxFontSize = buttonTextSize,
                            stepSize = 1.sp,
                        ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
                ShortcutKeyChip(
                    text = formatShortcutBindingForDisplay(AppSettings.shortcutEndTurnStartBattle.value),
                    color = shortcutHintColor,
                )
            }
        }
    }
}

@Composable
fun GameControlsPanel(
    phase: GamePhase,
    gameState: GameState,
    coinsState: State<Int>,
    selectedDefenderType: DefenderType?,
    selectedDefenderId: Int?,
    selectedAttackerId: Int?, // Add attacker selection parameter
    selectedTargetId: Int?,
    selectedTargetPosition: Position?,
    selectedBarricadePosition: Position? = null, // Selected barricade for info panel
    onSelectDefenderType: (DefenderType?) -> Unit,
    onUpgradeDefender: (Int) -> Unit,
    onUndoTower: (Int) -> Unit,
    onSellTower: (Int) -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onDefenderAttackPosition: (Int, Position) -> Boolean,
    onPrimaryAction: () -> Unit,
    onMineAction: ((Int, MineAction) -> Unit)? = null,
    onWizardAction: ((Int, WizardAction) -> Unit)? = null, // Add wizard action callback for magical trap placement mode
    selectedMineAction: MineAction? = null, // Add trap placement mode state
    selectedWizardAction: WizardAction? = null, // Add wizard trap placement mode state
    onBarricadeAction: ((Int, BarricadeAction) -> Unit)? = null, // Add barricade action callback for barricade placement mode
    selectedBarricadeAction: BarricadeAction? = null, // Add barricade placement mode state
    onRemoveBarricade: ((Position) -> Unit)? = null, // Callback to remove a barricade
    uiScale: Float = 1f, // Add platform scale parameter
    onShowDragonInfo: () -> Unit = {}, // Add dragon info callback
    highlightEndTurnButton: Boolean = false, // Visually highlight the End Turn button (keyboard focus)
    splitSelectorToggle: Int = 0, // Counter incremented to toggle the split selector dropdown via keyboard
    onSplitSelectorExpandedChanged: (Boolean) -> Unit = {},
) {
    de.egril.defender.ui.a11y.FontSizeUnscaled {
        // Automatically fold buy panel when a defender, attacker, or barricade is selected
        val compactBuyPanel = selectedDefenderId != null || selectedAttackerId != null || selectedBarricadePosition != null

        // Determine phase-specific properties
        val isPlayerTurn = phase == GamePhase.PLAYER_TURN
        val autoAttackAvailability =
            if (isPlayerTurn && gameState.level.allowAutoAttack) {
                gameState.getAutoAttackAvailability()
            } else {
                AutoAttackAvailability.NONE
            }
        val autoAttackAvailable = autoAttackAvailability != AutoAttackAvailability.NONE
        val autoAttackManaOnly = autoAttackAvailability == AutoAttackAvailability.MANA_ONLY
        val title =
            if (isPlayerTurn) {
                stringResource(Res.string.your_turn_message)
            } else {
                stringResource(Res.string.initial_building_phase)
            }
        val primaryButtonText =
            when {
                !isPlayerTurn -> stringResource(Res.string.start_battle)
                autoAttackManaOnly -> stringResource(Res.string.generate_mana_and_end_turn)
                autoAttackAvailable -> stringResource(Res.string.auto_attack_button)
                else -> stringResource(Res.string.end_turn_button)
            }
        // Larger expanded button (shown when nothing is selected) uses the combined label
        val primaryButtonExpandedText =
            when {
                !isPlayerTurn -> stringResource(Res.string.start_battle)
                autoAttackManaOnly -> stringResource(Res.string.generate_mana_and_end_turn)
                autoAttackAvailable -> stringResource(Res.string.auto_attack_slash_end_turn)
                else -> stringResource(Res.string.end_turn_button)
            }
        val primaryButtonTooltip =
            if (isPlayerTurn && autoAttackAvailable) {
                if (autoAttackManaOnly) {
                    stringResource(Res.string.generate_mana_and_end_turn)
                } else {
                    stringResource(Res.string.auto_attack_and_end_turn)
                }
            } else {
                null
            }
        val primaryButtonColor =
            if (isPlayerTurn) {
                GamePlayColors.WarningDeep
            } else {
                ButtonDefaults.buttonColors().containerColor
            }

        Column(modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val panelWidth = maxWidth // Capture for use in nested composable scopes
                val isNarrowPanel = panelWidth < 500.dp
                val isMobileUI = isPlatformMobile || isMobileWebBrowser()
                val expandedGridHeight =
                    if (isMobileUI && isNarrowPanel) {
                        75.dp
                    } else if (isNarrowPanel) {
                        70.dp
                    } else if (isMobileUI) {
                        80.dp
                    } else {
                        75.dp
                    }

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Title - hide when tower is selected
                    if (!compactBuyPanel) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (compactBuyPanel) {
                        // Folded view: Compact layout with defender/attacker/barricade info on left, buy buttons and End Turn on right
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
                                            onWizardAction = onWizardAction,
                                            selectedMineAction = selectedMineAction,
                                            selectedWizardAction = selectedWizardAction,
                                            onBarricadeAction = onBarricadeAction,
                                            selectedBarricadeAction = selectedBarricadeAction,
                                            compactBuyPanel,
                                            isMobile = uiScale < 1f,
                                            selectedTargetId = selectedTargetId,
                                            selectedTargetPosition = selectedTargetPosition,
                                            onDefenderAttack = onDefenderAttack,
                                            onDefenderAttackPosition = onDefenderAttackPosition,
                                            isPlayerTurn = isPlayerTurn,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }

                            // Selected attacker info on left (when no defender is selected)
                            selectedAttackerId?.let { attackerId ->
                                val attacker = gameState.attackers.find { it.id == attackerId }
                                if (attacker != null && selectedDefenderId == null) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        AttackerInfo(
                                            attacker = attacker,
                                            activeSpellEffects = gameState.activeSpellEffects,
                                            isMobile = uiScale < 1f,
                                            onShowDragonInfo = onShowDragonInfo,
                                            waaghActive = gameState.waaghFrenzyActive.value,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }

                            // Selected barricade info panel (when no defender or attacker is selected)
                            if (selectedBarricadePosition != null && selectedDefenderId == null && selectedAttackerId == null) {
                                val barricade = gameState.barricades.find { it.position == selectedBarricadePosition }
                                if (barricade != null) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        BarricadeInfoPanel(
                                            position = selectedBarricadePosition,
                                            barricade = barricade,
                                            isMobile = uiScale < 1f,
                                            onRemove = { onRemoveBarricade?.invoke(selectedBarricadePosition) },
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }

                            // Right side: buy buttons and End Turn button
                            // When using the split button, use exactly the split control width so the
                            // info area on the left gets all remaining horizontal space.
                            val rightColumnModifier =
                                if (AppSettings.splitBuildTowerButton.value) {
                                    Modifier.width(SplitControlMaxWidth)
                                } else {
                                    Modifier.widthIn(max = 600.dp).fillMaxWidth()
                                }
                            Column(
                                modifier = rightColumnModifier,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                val isMobile = uiScale < 1f
                                val compactDefenderButtonModifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(if (isMobile) 45.dp else 45.dp)

                                if (AppSettings.splitBuildTowerButton.value) {
                                    val types =
                                        gameState.level.availableTowers
                                            .filter { it != DefenderType.DRAGONS_LAIR }
                                    SplitTowerBuildControls(
                                        availableTypes = types,
                                        selectedDefenderType = selectedDefenderType,
                                        coinsState = coinsState,
                                        instantTowerActive = gameState.instantTowerSpellActive.value,
                                        onSelectDefenderType = onSelectDefenderType,
                                        isPlayerTurn = isPlayerTurn,
                                        onPrimaryAction = onPrimaryAction,
                                        highlightEndTurnButton = highlightEndTurnButton,
                                        autoAttackAvailable = autoAttackAvailable,
                                        autoAttackManaOnly = autoAttackManaOnly,
                                        toggleSelectorKey = splitSelectorToggle,
                                        onSelectorExpandedChanged = onSplitSelectorExpandedChanged,
                                    )
                                } else {
                                    // Compact buy buttons
                                    LazyVerticalGrid(
                                        modifier = Modifier.padding(top = 8.dp),
                                        columns = GridCells.Fixed(4),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val types =
                                            gameState.level.availableTowers
                                                // hack: we need an additional entry
                                                // that is overridden by the start game/end turn button
                                                // in the compact view
                                                .plus(DefenderType.DRAGONS_LAIR)
                                                .toTypedArray()

                                        itemsIndexed(
                                            types,
                                            key = { index: Int, type: DefenderType -> "${type.name}_folded_${coinsState.value}" },
                                        ) { index: Int, type: DefenderType ->
                                            val isLast = index == types.lastIndex
                                            CompactDefenderButton(
                                                type = type,
                                                isSelected = selectedDefenderType == type,
                                                canAfford = coinsState.value >= type.baseCost,
                                                instantTowerActive = gameState.instantTowerSpellActive.value,
                                                shortcutIndex = if (type != DefenderType.DRAGONS_LAIR) index else null,
                                                modifier = compactDefenderButtonModifier,
                                                onClick = {
                                                    onSelectDefenderType(if (selectedDefenderType == type) null else type)
                                                },
                                            )
                                            if (isLast) {
                                                TurnButton(
                                                    isPlayerTurn,
                                                    modifier = compactDefenderButtonModifier,
                                                    onPrimaryAction,
                                                    highlighted = highlightEndTurnButton,
                                                    autoAttackAvailable = autoAttackAvailable,
                                                    autoAttackManaOnly = autoAttackManaOnly,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Expanded view: Flexible layout — all buttons in one row, same width, centered when max width is reached.
                        val types =
                            gameState.level.availableTowers
                                .filter { it != DefenderType.DRAGONS_LAIR }
                        val numButtons = types.size
                        if (numButtons > 0) {
                            val buttonSpacing = 4.dp
                            val totalSpacing = buttonSpacing * (numButtons - 1)
                            val availablePerButton = (panelWidth - totalSpacing) / numButtons
                            val buttonWidth = minOf(availablePerButton, GamePlayConstants.ButtonSizes.DefenderButtonMaxWidth)

                            Row(
                                modifier = Modifier.fillMaxWidth().height(expandedGridHeight),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                types.forEachIndexed { index, type ->
                                    if (index > 0) Spacer(modifier = Modifier.width(4.dp))
                                    val canAfford = coinsState.value >= type.baseCost
                                    if (LogConfig.ENABLE_UI_LOGGING) {
                                        println(
                                            "DEBUG: ${phase.name} Button for ${type.displayName} - coins: ${coinsState.value}, cost: ${type.baseCost}, canAfford: $canAfford",
                                        )
                                    }
                                    DefenderButton(
                                        type = type,
                                        isSelected = selectedDefenderType == type,
                                        canAfford = canAfford,
                                        coinsState = coinsState,
                                        instantTowerActive = gameState.instantTowerSpellActive.value,
                                        shortcutIndex = index,
                                        modifier = Modifier.width(buttonWidth),
                                        onClick = {
                                            onSelectDefenderType(if (selectedDefenderType == type) null else type)
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isPlatformMobile) 4.dp else 8.dp))

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
                                    onWizardAction = onWizardAction,
                                    selectedMineAction = selectedMineAction,
                                    selectedWizardAction = selectedWizardAction,
                                    onBarricadeAction = onBarricadeAction,
                                    selectedBarricadeAction = selectedBarricadeAction,
                                    compactBuyPanel,
                                    isMobile = uiScale < 1f,
                                    selectedTargetId = selectedTargetId,
                                    selectedTargetPosition = selectedTargetPosition,
                                    onDefenderAttack = onDefenderAttack,
                                    onDefenderAttackPosition = onDefenderAttackPosition,
                                    isPlayerTurn = isPlayerTurn,
                                )
                            }
                        }
                    }

                    // End Turn button - only show in expanded view (not compact)
                    if (!compactBuyPanel) {
                        Spacer(modifier = Modifier.height(8.dp))

                        TooltipWrapper(text = primaryButtonTooltip) {
                            Button(
                                onClick = onPrimaryAction,
                                modifier = Modifier.fillMaxWidth(),
                                border =
                                    if (highlightEndTurnButton && isPlayerTurn) {
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        null
                                    },
                                colors =
                                    if (isPlayerTurn) {
                                        ButtonDefaults.buttonColors(containerColor = primaryButtonColor)
                                    } else {
                                        ButtonDefaults.buttonColors()
                                    },
                            ) {
                                val shortcutHintColor = turnButtonShortcutHintColor(isPlayerTurn, primaryButtonColor)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        primaryButtonExpandedText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = turnButtonLabelFontSize(AppSettings.headerTextSize.value),
                                        maxLines = 1,
                                    )
                                    ShortcutKeyChip(
                                        text = formatShortcutBindingForDisplay(AppSettings.shortcutEndTurnStartBattle.value),
                                        color = shortcutHintColor,
                                    )
                                }
                            }
                        }
                    }
                } // end inner Column
            } // end BoxWithConstraints
        }
    } // FontSizeUnscaled
}

@Composable
fun EnemyTurnInfo() {
    // The ViewModel automatically handles the delays and phase progression
    // This composable displays the enemy turn indicator with animation
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GamePlayColors.DangerCardBackground),
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Res.string.enemy_turn_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(color = Color.Red)
            }
        }
    }
}
