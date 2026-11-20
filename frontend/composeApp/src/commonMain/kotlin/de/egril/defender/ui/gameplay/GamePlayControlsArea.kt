package de.egril.defender.ui.gameplay

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.*
import defender_of_egril.composeapp.generated.resources.*

@Composable
internal fun ColumnScope.GamePlayControlsArea(
    state: GamePlayScreenState,
    gameState: GameState,
    showMagicPanel: Boolean,
    playerStats: PlayerAbilities?,
    selectedSpell: SpellType?,
    activeSpellToken: SpellType?,
    uiScale: Float,
    endPlayerTurnAction: () -> Unit,
    handleMineAction: (Int, MineAction) -> Unit,
    handleWizardAction: (Int, WizardAction) -> Unit,
    handleBarricadeAction: (Int, BarricadeAction) -> Unit,
    onCastSpell: ((SpellType) -> Unit)?,
    onCloseMagicPanel: (() -> Unit)?,
    onExitSpellTargeting: (() -> Unit)?,
    onCastSupportSpellToken: ((SpellType) -> Unit)?,
    onActivateCooldownPower: ((de.egril.defender.model.CooldownPowerType) -> Unit)?,
    onUpgradeDefender: (Int) -> Boolean,
    onSanctifyDefender: ((Int) -> Boolean)?,
    onActivateAltar: ((Int) -> Boolean)?,
    onUndoTower: (Int) -> Boolean,
    onSellTower: (Int) -> Boolean,
    onDefenderAttack: (Int, Int) -> Boolean,
    onDefenderAttackPosition: (Int, Position) -> Boolean,
    onStartFirstPlayerTurn: () -> Unit,
    onRemoveBarricade: ((Position) -> Int)?,
) {
    // Track the maximum height the controls panel has reached (in pixels).
    // Using heightIn(min = ...) on the container prevents it from shrinking when switching
    // between the tall PLAYER_TURN panel and the short ENEMY_TURN indicator, which would
    // otherwise cause the map area (weight=1f) to expand and the map to visibly jump.
    var maxControlsHeightPx by remember { mutableStateOf(0) }

    with(state) {
        // Wrap in Box that tracks and maintains its maximum seen height to prevent map jumping.
        // contentAlignment = BottomStart ensures that when the current content is shorter than
        // the maximum recorded height, it stays pinned to the bottom of the reserved area so
        // the tower buttons and enemy-turn banner appear as low on the screen as possible.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .then(
                        if (maxControlsHeightPx > 0) {
                            with(LocalDensity.current) { Modifier.heightIn(min = maxControlsHeightPx.toDp()) }
                        } else {
                            Modifier
                        },
                    ).onSizeChanged { size ->
                        if (size.height > maxControlsHeightPx) {
                            maxControlsHeightPx = size.height
                        }
                    },
            contentAlignment = Alignment.BottomStart,
        ) {
            // Show magic panel inline (non-overlay) when open - map remains accessible
            if (showMagicPanel && playerStats != null && onCloseMagicPanel != null && onCastSpell != null) {
                MagicPanel(
                    playerStats = playerStats,
                    currentMana = gameState.currentMana.value,
                    maxMana = gameState.maxMana.value,
                    currentHealthPoints = gameState.healthPoints.value,
                    maxHealthPoints = gameState.level.healthPoints,
                    gamePhase = gameState.phase.value,
                    selectedSpell = selectedSpell,
                    onCastSpell = onCastSpell,
                    onClose = {
                        onCloseMagicPanel.invoke()
                        // Also exit targeting mode if active when closing the panel
                        if (gameState.spellTargeting.value != null) {
                            onExitSpellTargeting?.invoke()
                        }
                    },
                )
            } else if (gameState.spellTargeting.value != null) {
                // Spell targeting mode: show compact instruction in bottom panel so map stays accessible
                val targeting = gameState.spellTargeting.value!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Spell type icon
                        SpellTargetIcon(spell = targeting.activeSpell, size = 32.dp)

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = targeting.activeSpell.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text =
                                    when (targeting.activeSpell.targetType) {
                                        de.egril.defender.model.SpellTargetType.POSITION ->
                                            stringResource(Res.string.spell_targeting_position)
                                        de.egril.defender.model.SpellTargetType.ENEMY ->
                                            stringResource(Res.string.spell_targeting_enemy)
                                        de.egril.defender.model.SpellTargetType.TOWER ->
                                            stringResource(Res.string.spell_targeting_tower)
                                        else -> ""
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            PlacementKeyboardHints(modifier = Modifier.padding(top = 4.dp))
                        }
                        OutlinedButton(onClick = { onExitSpellTargeting?.invoke() }) {
                            Text(stringResource(Res.string.spell_targeting_cancel))
                            Spacer(modifier = Modifier.width(6.dp))
                            ShortcutKeyChip(
                                text = "Esc",
                                color = LocalContentColor.current.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            } else if (selectedSupportObject != null) {
                // Support object placement mode: show a compact instruction + cancel card
                // (mirrors the spell targeting card) so keyboard users can see how to cancel.
                val placingType = selectedSupportObject!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SupportObjectIcon(placingType, 32.dp)

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = placingType.localizedSupportName(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(Res.string.spell_targeting_position),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            PlacementKeyboardHints(modifier = Modifier.padding(top = 4.dp))
                        }
                        OutlinedButton(onClick = { selectedSupportObject = null }) {
                            Text(stringResource(Res.string.spell_targeting_cancel))
                            Spacer(modifier = Modifier.width(6.dp))
                            ShortcutKeyChip(
                                text = "Esc",
                                color = LocalContentColor.current.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            } else if (selectedSupportFief != null) {
                // Fief placement mode: show a compact instruction + cancel card
                // selectedSupportFief is a mutable var, so smart-cast is not possible;
                // capture a local non-null copy for use within this branch.
                val placingFief = selectedSupportFief!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = placingFief.localizedFiefName(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(Res.string.spell_targeting_position),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            PlacementKeyboardHints(modifier = Modifier.padding(top = 4.dp))
                        }
                        OutlinedButton(onClick = { selectedSupportFief = null }) {
                            Text(stringResource(Res.string.spell_targeting_cancel))
                            Spacer(modifier = Modifier.width(6.dp))
                            ShortcutKeyChip(
                                text = "Esc",
                                color = LocalContentColor.current.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Support bar: placable objects + spell tokens shown directly above the button row
                    SupportBar(
                        gameState = gameState,
                        selectedSupportObject = selectedSupportObject,
                        activeSpellToken = activeSpellToken,
                        enabled =
                            gameState.phase.value == GamePhase.PLAYER_TURN ||
                                gameState.phase.value == GamePhase.INITIAL_BUILDING,
                        onObjectClick = { type ->
                            // Toggle object placement selection; clear other selections
                            selectedSupportObject = if (selectedSupportObject == type) null else type
                            selectedSupportFief = null
                            selectedDefenderType = null
                        },
                        onSpellClick = { spell ->
                            selectedSupportObject = null
                            selectedSupportFief = null
                            onCastSupportSpellToken?.invoke(spell)
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        onCooldownPowerClick = { power ->
                            selectedSupportObject = null
                            selectedSupportFief = null
                            onActivateCooldownPower?.invoke(power)
                        },
                        selectedSupportFief = selectedSupportFief,
                        onFiefClick = { type ->
                            selectedSupportFief = if (selectedSupportFief == type) null else type
                            selectedSupportObject = null
                            selectedDefenderType = null
                        },
                        focusedSlotIndex =
                            supportFocusIndex?.let { idx ->
                                val count = visibleSupportSlots(gameState).size
                                if (count == 0) null else idx.coerceIn(0, count - 1)
                            },
                    )
                    // Keyboard-navigation hint for the support bar: shown only while there
                    // are support elements to navigate and hints are enabled.
                    if (visibleSupportSlots(gameState).isNotEmpty()) {
                        SupportBarKeyboardHints(
                            modifier =
                                Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 4.dp),
                        )
                    }
                    // Control Panel based on phase
                    when (gameState.phase.value) {
                        GamePhase.INITIAL_BUILDING -> {
                            GameControlsPanel(
                                phase = GamePhase.INITIAL_BUILDING,
                                gameState = gameState,
                                coinsState = gameState.coins,
                                selectedDefenderType = selectedDefenderType,
                                selectedDefenderId = selectedDefenderId,
                                selectedAttackerId = selectedAttackerId,
                                selectedTargetId = null,
                                selectedTargetPosition = null,
                                selectedBarricadePosition = selectedBarricadePosition,
                                selectedGargoyleId = selectedGargoyleId,
                                onSelectDefenderType = {
                                    selectedDefenderType = it
                                    selectedSupportObject = null
                                },
                                onUpgradeDefender = { onUpgradeDefender(it) },
                                onSanctifyDefender = onSanctifyDefender,
                                onActivateAltar = onActivateAltar,
                                onUndoTower = { defenderId ->
                                    if (onUndoTower(defenderId)) {
                                        selectedDefenderType = null
                                        selectedDefenderId = null
                                    }
                                },
                                onSellTower = { defenderId ->
                                    if (onSellTower(defenderId)) {
                                        selectedDefenderType = null
                                        selectedDefenderId = null
                                    }
                                },
                                onDefenderAttack = { _, _ -> false },
                                onDefenderAttackPosition = { _, _ -> false },
                                onPrimaryAction = {
                                    selectedDefenderType = null // Clear defender type selection when starting battle
                                    selectedDefenderId = null // Clear defender selection when starting battle
                                    selectedAttackerId = null // Clear attacker selection when starting battle
                                    onStartFirstPlayerTurn()

                                    // Show first-time auto-attack info at the start of the level if allowed and not seen
                                    if (gameState.level.allowAutoAttack &&
                                        !gameState.infoState.value.hasSeen(InfoType.AUTO_ATTACK_INFO)
                                    ) {
                                        gameState.infoState.value = gameState.infoState.value.showInfo(InfoType.AUTO_ATTACK_INFO)
                                    }

                                    // Track tutorial progress and auto-advance START_COMBAT step
                                    if (gameState.tutorialState.value.isActive) {
                                        if (!gameState.tutorialState.value.hasStartedFirstTurn) {
                                            gameState.tutorialState.value = gameState.tutorialState.value.markTurnStarted()
                                        }
                                        // Auto-advance if currently showing START_COMBAT step
                                        if (gameState.tutorialState.value.currentStep == TutorialStep.START_COMBAT) {
                                            gameState.tutorialState.value = gameState.tutorialState.value.advanceStep()
                                        }
                                    }
                                },
                                onMineAction = handleMineAction,
                                onWizardAction = handleWizardAction,
                                selectedMineAction = selectedMineAction,
                                selectedWizardAction = selectedWizardAction,
                                onBarricadeAction = handleBarricadeAction,
                                selectedBarricadeAction = selectedBarricadeAction,
                                onRemoveBarricade = { pos ->
                                    onRemoveBarricade?.invoke(pos)
                                    selectedBarricadePosition = null
                                },
                                uiScale = uiScale,
                                onShowDragonInfo = {
                                    gameState.infoState.value = gameState.infoState.value.showInfo(InfoType.DRAGON_INFO)
                                },
                                splitSelectorToggle = splitSelectorToggle,
                                onSplitSelectorExpandedChanged = { splitSelectorExpanded = it },
                            )
                        }

                        GamePhase.PLAYER_TURN -> {
                            GameControlsPanel(
                                phase = GamePhase.PLAYER_TURN,
                                gameState = gameState,
                                coinsState = gameState.coins,
                                selectedDefenderType = selectedDefenderType,
                                selectedDefenderId = selectedDefenderId,
                                selectedAttackerId = selectedAttackerId,
                                selectedTargetId = selectedTargetId,
                                selectedTargetPosition = selectedTargetPosition,
                                selectedBarricadePosition = selectedBarricadePosition,
                                selectedGargoyleId = selectedGargoyleId,
                                onSelectDefenderType = {
                                    selectedDefenderType = it
                                    selectedSupportObject = null
                                },
                                onUpgradeDefender = { onUpgradeDefender(it) },
                                onSanctifyDefender = onSanctifyDefender,
                                onActivateAltar = onActivateAltar,
                                onUndoTower = { defenderId ->
                                    if (onUndoTower(defenderId)) {
                                        selectedDefenderType = null
                                        selectedDefenderId = null
                                    }
                                },
                                onSellTower = { defenderId ->
                                    if (onSellTower(defenderId)) {
                                        selectedDefenderType = null
                                        selectedDefenderId = null
                                    }
                                },
                                onDefenderAttack = { defenderId, targetId ->
                                    if (onDefenderAttack(defenderId, targetId)) {
                                        // Check if we should keep the selection active:
                                        // - Tower still has actions remaining
                                        // - Enemy is still alive
                                        if (!shouldKeepTargetSelection(gameState, defenderId, targetId)) {
                                            selectedTargetId = null
                                            selectedTargetPosition = null
                                        }

                                        // Track tutorial progress
                                        if (gameState.tutorialState.value.isActive &&
                                            !gameState.tutorialState.value.hasAttackedEnemy
                                        ) {
                                            gameState.tutorialState.value = gameState.tutorialState.value.markAttackedEnemy()
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                },
                                onDefenderAttackPosition = { defenderId, targetPos ->
                                    if (onDefenderAttackPosition(defenderId, targetPos)) {
                                        // Check if we should keep the selection active:
                                        // - Tower still has actions remaining
                                        // - There's still a living enemy at the target position
                                        if (!shouldKeepTargetSelectionForPosition(gameState, defenderId, targetPos)) {
                                            selectedTargetId = null
                                            selectedTargetPosition = null
                                        }

                                        // Track tutorial progress
                                        if (gameState.tutorialState.value.isActive &&
                                            !gameState.tutorialState.value.hasAttackedEnemy
                                        ) {
                                            gameState.tutorialState.value = gameState.tutorialState.value.markAttackedEnemy()
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                },
                                onPrimaryAction = {
                                    // Check if there are unused action points before ending turn
                                    if (gameState.hasDefendersWithUnusedActions()) {
                                        // Show confirmation dialog
                                        showEndTurnConfirmation = true
                                    } else {
                                        // End turn directly
                                        endPlayerTurnAction()
                                        // Track tutorial progress
                                        if (gameState.tutorialState.value.isActive &&
                                            !gameState.tutorialState.value.hasStartedFirstTurn
                                        ) {
                                            gameState.tutorialState.value = gameState.tutorialState.value.markTurnStarted()
                                        }
                                    }
                                },
                                onMineAction = handleMineAction,
                                onWizardAction = handleWizardAction,
                                selectedMineAction = selectedMineAction,
                                selectedWizardAction = selectedWizardAction,
                                onBarricadeAction = handleBarricadeAction,
                                selectedBarricadeAction = selectedBarricadeAction,
                                onRemoveBarricade = { pos ->
                                    onRemoveBarricade?.invoke(pos)
                                    selectedBarricadePosition = null
                                },
                                uiScale = uiScale,
                                onShowDragonInfo = {
                                    gameState.infoState.value = gameState.infoState.value.showInfo(InfoType.DRAGON_INFO)
                                },
                                highlightEndTurnButton = highlightEndTurnButton,
                                splitSelectorToggle = splitSelectorToggle,
                                onSplitSelectorExpandedChanged = { splitSelectorExpanded = it },
                            )
                        }

                        GamePhase.ENEMY_TURN -> {
                            EnemyTurnInfo()
                        }
                    }
                }
            }
        } // end Box(height-locked controls panel)
    }
}
