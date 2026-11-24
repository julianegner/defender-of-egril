package de.egril.defender.ui.gameplay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.egril.defender.model.*
import de.egril.defender.ui.MAP_KEYBOARD_PAN_STEP
import de.egril.defender.ui.MapPanController
import de.egril.defender.ui.ReminderMessage
import de.egril.defender.ui.a11y.accessibilityVisualFilter
import de.egril.defender.ui.animations.CoinFlightOverlay
import de.egril.defender.ui.getGameplayUIScale
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.isShortcutBindingPressed

@Composable
fun GamePlayScreen(
    gameState: GameState,
    onPlaceDefender: (DefenderType, Position) -> Boolean,
    onUpgradeDefender: (Int) -> Boolean,
    onUndoTower: (Int) -> Boolean,
    onSellTower: (Int) -> Boolean,
    onStartFirstPlayerTurn: () -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onDefenderAttackPosition: (Int, Position) -> Boolean,
    onEndPlayerTurn: () -> Unit,
    onAutoAttackAndEndTurn: () -> Unit, // Add auto-attack callback
    onBackToMap: () -> Unit,
    onSaveGame: ((String?) -> String?)? = null, // Add save game callback with optional comment
    onCheatCode: ((String) -> Boolean)? = null, // Add cheat code callback
    onMineDig: ((Int) -> DigOutcome?)? = null, // Add mine dig callback
    onMineBuildTrap: ((Int, Position) -> Boolean)? = null, // Add mine build trap callback
    onWizardPlaceMagicalTrap: ((Int, Position) -> Boolean)? = null, // Add wizard magical trap callback
    onWizardGenerateMana: ((Int) -> Boolean)? = null, // Add wizard mana generation callback
    onSanctifyDefender: ((Int) -> Boolean)? = null,
    onActivateAltar: ((Int) -> Boolean)? = null,
    onBuildBarricade: ((Int, Position) -> Boolean)? = null, // Add barricade building callback
    onRemoveBarricade: ((Position) -> Int)? = null, // Add barricade removal callback - returns coin refund
    cheatDigOutcome: DigOutcome? = null, // Dig outcome from cheat code
    onClearCheatDigOutcome: (() -> Unit)? = null, // Callback to clear cheat dig outcome
    showPlatformInfo: Boolean = false, // Show platform info from cheat code
    onClearPlatformInfo: (() -> Unit)? = null, // Callback to clear platform info
    showCheatHelp: Boolean = false, // Show cheat code help screen
    onClearCheatHelp: (() -> Unit)? = null, // Callback to clear cheat help
    hasUnsavedChanges: (() -> Boolean)? = null, // Callback to check for unsaved changes
    specialActionsRemaining: List<DefenderType> = emptyList(), // List of defender types with remaining special actions
    onClearSpecialActionsWarning: (() -> Unit)? = null, // Callback to clear special actions warning
    reminderMessage: ReminderMessage? = null, // Time reminder message
    onClearReminderMessage: (() -> Unit)? = null, // Callback to clear reminder message
    // Magic panel parameters
    showMagicPanel: Boolean = false, // Show magic panel overlay
    playerStats: PlayerAbilities? = null, // Player stats for spell list
    selectedSpell: SpellType? = null, // Currently selected spell (highlighted with border)
    onOpenMagicPanel: (() -> Unit)? = null, // Callback to open magic panel
    onCloseMagicPanel: (() -> Unit)? = null, // Callback to close magic panel
    onCastSpell: ((SpellType) -> Unit)? = null, // Callback to cast/select spell
    onCancelInstantTowerSpell: (() -> Unit)? = null, // Callback to cancel instant tower spell (abort dialog)
    pendingSpellCast: SpellType? = null, // Spell awaiting confirmation
    onConfirmSpellCast: (() -> Unit)? = null, // Callback to confirm spell cast
    onCancelSpellCast: (() -> Unit)? = null, // Callback to cancel spell cast
    onSelectSpellTarget: ((Any) -> Unit)? = null, // Callback to select spell target
    onExitSpellTargeting: (() -> Unit)? = null, // Callback to exit targeting mode
    // Post-target confirmation dialogs
    showSpellTargetConfirmation: Pair<SpellType, Any>? = null, // Show confirmation after target selected
    onConfirmTargetSpell: (() -> Unit)? = null, // Callback to confirm spell on target
    onDismissTargetConfirmation: (() -> Unit)? = null, // Callback to dismiss target confirmation
    showFreezeImmuneWarning: de.egril.defender.model.Attacker? = null, // Show warning for immune enemy
    onDismissFreezeWarning: (() -> Unit)? = null, // Callback to dismiss freeze warning
    scrollToPosition: de.egril.defender.model.Position? = null, // Scroll map to position (e.g. bomb explosion)
    onScrollToPositionConsumed: (() -> Unit)? = null, // Callback after scroll consumed
    pendingGameMessage: de.egril.defender.model.GameMessage? = null, // In-game event message (target taken, gate destroyed)
    onDismissGameMessage: (() -> Unit)? = null, // Callback to dismiss current message and show next
    isDemoMode: Boolean = false, // True when demo mode is active
    onStopDemoMode: (() -> Unit)? = null, // Callback to stop demo mode
    demoSelectedDefenderType: DefenderType? = null,
    demoHoveredPosition: Position? = null,
    demoSelectedDefenderId: Int? = null,
    demoSelectedTargetPosition: Position? = null,
    onGetAutoAttackTarget: ((Int) -> Position?)? = null, // Get best auto-attack target for a tower
    onWinLevelNow: (() -> Unit)? = null, // Instantly win the level when guaranteed (finish level fast)
    onPlaceSupportObject: ((SupportObjectType, Position) -> Boolean)? = null, // Place a level support object at a position
    onPlaceSupportFief: ((de.egril.defender.model.FiefType, Position) -> Boolean)? = null, // Place a level support fief token at a position
    onCastSupportSpellToken: ((SpellType) -> Unit)? = null, // Start casting a level support spell token (no mana cost)
    activeSpellToken: SpellType? = null, // Currently active support spell token (highlighted in support bar)
    onActivateCooldownPower: ((de.egril.defender.model.CooldownPowerType) -> Unit)? = null, // Activate a cooldown-based support power
    onSandboxSpawnEnemy: ((de.egril.defender.model.AttackerType, Int, de.egril.defender.model.Position?) -> Unit)? = null, // Sandbox: spawn an adjustable test enemy at an optional spawn point
    onSandboxAddCoins: (() -> Unit)? = null, // Sandbox: add coins
    onSandboxPaintTile: ((de.egril.defender.model.Position, de.egril.defender.editor.TileType, de.egril.defender.model.RiverFlow, Int) -> Unit)? = null, // Sandbox: repaint a map tile (with river flow when RIVER)
) {
    GamePlayScreenContent(
        gameState = gameState,
        onPlaceDefender = onPlaceDefender,
        onUpgradeDefender = onUpgradeDefender,
        onUndoTower = onUndoTower,
        onSellTower = onSellTower,
        onStartFirstPlayerTurn = onStartFirstPlayerTurn,
        onDefenderAttack = onDefenderAttack,
        onDefenderAttackPosition = onDefenderAttackPosition,
        onEndPlayerTurn = onEndPlayerTurn,
        onAutoAttackAndEndTurn = onAutoAttackAndEndTurn,
        onBackToMap = onBackToMap,
        onSaveGame = onSaveGame,
        onCheatCode = onCheatCode,
        onMineDig = onMineDig,
        onMineBuildTrap = onMineBuildTrap,
        onWizardPlaceMagicalTrap = onWizardPlaceMagicalTrap,
        onWizardGenerateMana = onWizardGenerateMana,
        onSanctifyDefender = onSanctifyDefender,
        onActivateAltar = onActivateAltar,
        onBuildBarricade = onBuildBarricade,
        onRemoveBarricade = onRemoveBarricade,
        cheatDigOutcome = cheatDigOutcome,
        onClearCheatDigOutcome = onClearCheatDigOutcome,
        showPlatformInfo = showPlatformInfo,
        onClearPlatformInfo = onClearPlatformInfo,
        showCheatHelp = showCheatHelp,
        onClearCheatHelp = onClearCheatHelp,
        hasUnsavedChanges = hasUnsavedChanges,
        specialActionsRemaining = specialActionsRemaining,
        onClearSpecialActionsWarning = onClearSpecialActionsWarning,
        reminderMessage = reminderMessage,
        onClearReminderMessage = onClearReminderMessage,
        showMagicPanel = showMagicPanel,
        playerStats = playerStats,
        selectedSpell = selectedSpell,
        onOpenMagicPanel = onOpenMagicPanel,
        onCloseMagicPanel = onCloseMagicPanel,
        onCastSpell = onCastSpell,
        onCancelInstantTowerSpell = onCancelInstantTowerSpell,
        pendingSpellCast = pendingSpellCast,
        onConfirmSpellCast = onConfirmSpellCast,
        onCancelSpellCast = onCancelSpellCast,
        onSelectSpellTarget = onSelectSpellTarget,
        onExitSpellTargeting = onExitSpellTargeting,
        showSpellTargetConfirmation = showSpellTargetConfirmation,
        onConfirmTargetSpell = onConfirmTargetSpell,
        onDismissTargetConfirmation = onDismissTargetConfirmation,
        showFreezeImmuneWarning = showFreezeImmuneWarning,
        onDismissFreezeWarning = onDismissFreezeWarning,
        scrollToPosition = scrollToPosition,
        onScrollToPositionConsumed = onScrollToPositionConsumed,
        pendingGameMessage = pendingGameMessage,
        onDismissGameMessage = onDismissGameMessage,
        isDemoMode = isDemoMode,
        onStopDemoMode = onStopDemoMode,
        demoSelectedDefenderType = demoSelectedDefenderType,
        demoHoveredPosition = demoHoveredPosition,
        demoSelectedDefenderId = demoSelectedDefenderId,
        demoSelectedTargetPosition = demoSelectedTargetPosition,
        onGetAutoAttackTarget = onGetAutoAttackTarget,
        onWinLevelNow = onWinLevelNow,
        onPlaceSupportObject = onPlaceSupportObject,
        onPlaceSupportFief = onPlaceSupportFief,
        onCastSupportSpellToken = onCastSupportSpellToken,
        activeSpellToken = activeSpellToken,
        onActivateCooldownPower = onActivateCooldownPower,
        onSandboxSpawnEnemy = onSandboxSpawnEnemy,
        onSandboxAddCoins = onSandboxAddCoins,
        onSandboxPaintTile = onSandboxPaintTile,
    )
}

@Composable
private fun GamePlayScreenContent(
    gameState: GameState,
    onPlaceDefender: (DefenderType, Position) -> Boolean,
    onUpgradeDefender: (Int) -> Boolean,
    onUndoTower: (Int) -> Boolean,
    onSellTower: (Int) -> Boolean,
    onStartFirstPlayerTurn: () -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onDefenderAttackPosition: (Int, Position) -> Boolean,
    onEndPlayerTurn: () -> Unit,
    onAutoAttackAndEndTurn: () -> Unit, // Add auto-attack callback
    onBackToMap: () -> Unit,
    onSaveGame: ((String?) -> String?)? = null, // Add save game callback with optional comment
    onCheatCode: ((String) -> Boolean)? = null,
    onMineDig: ((Int) -> DigOutcome?)? = null,
    onMineBuildTrap: ((Int, Position) -> Boolean)? = null,
    onWizardPlaceMagicalTrap: ((Int, Position) -> Boolean)? = null, // Add wizard magical trap callback
    onWizardGenerateMana: ((Int) -> Boolean)? = null, // Add wizard mana generation callback
    onSanctifyDefender: ((Int) -> Boolean)? = null,
    onActivateAltar: ((Int) -> Boolean)? = null,
    onBuildBarricade: ((Int, Position) -> Boolean)? = null, // Add barricade building callback
    onRemoveBarricade: ((Position) -> Int)? = null, // Add barricade removal callback - returns coin refund
    cheatDigOutcome: DigOutcome? = null, // Dig outcome from cheat code
    onClearCheatDigOutcome: (() -> Unit)? = null, // Callback to clear cheat dig outcome
    showPlatformInfo: Boolean = false, // Show platform info from cheat code
    onClearPlatformInfo: (() -> Unit)? = null, // Callback to clear platform info
    showCheatHelp: Boolean = false, // Show cheat code help screen
    onClearCheatHelp: (() -> Unit)? = null, // Callback to clear cheat help
    hasUnsavedChanges: (() -> Boolean)? = null, // Callback to check for unsaved changes
    specialActionsRemaining: List<DefenderType> = emptyList(), // List of defender types with remaining special actions
    onClearSpecialActionsWarning: (() -> Unit)? = null, // Callback to clear special actions warning
    reminderMessage: ReminderMessage? = null, // Time reminder message
    onClearReminderMessage: (() -> Unit)? = null, // Callback to clear reminder message
    // Magic panel parameters
    showMagicPanel: Boolean = false,
    playerStats: PlayerAbilities? = null,
    selectedSpell: SpellType? = null,
    onOpenMagicPanel: (() -> Unit)? = null,
    onCloseMagicPanel: (() -> Unit)? = null,
    onCastSpell: ((SpellType) -> Unit)? = null,
    onCancelInstantTowerSpell: (() -> Unit)? = null,
    pendingSpellCast: SpellType? = null,
    onConfirmSpellCast: (() -> Unit)? = null,
    onCancelSpellCast: (() -> Unit)? = null,
    onSelectSpellTarget: ((Any) -> Unit)? = null,
    onExitSpellTargeting: (() -> Unit)? = null,
    showSpellTargetConfirmation: Pair<SpellType, Any>? = null,
    onConfirmTargetSpell: (() -> Unit)? = null,
    onDismissTargetConfirmation: (() -> Unit)? = null,
    showFreezeImmuneWarning: de.egril.defender.model.Attacker? = null,
    onDismissFreezeWarning: (() -> Unit)? = null,
    scrollToPosition: de.egril.defender.model.Position? = null,
    onScrollToPositionConsumed: (() -> Unit)? = null,
    pendingGameMessage: de.egril.defender.model.GameMessage? = null, // In-game event message (target taken, gate destroyed)
    onDismissGameMessage: (() -> Unit)? = null, // Callback to dismiss current message and show next
    isDemoMode: Boolean = false, // True when demo mode is active
    onStopDemoMode: (() -> Unit)? = null, // Callback to stop demo mode
    // Demo visual state – drives placement preview and attack aiming in the UI
    demoSelectedDefenderType: DefenderType? = null,
    demoHoveredPosition: Position? = null,
    demoSelectedDefenderId: Int? = null,
    demoSelectedTargetPosition: Position? = null,
    onGetAutoAttackTarget: ((Int) -> Position?)? = null, // Get best auto-attack target for a tower
    onWinLevelNow: (() -> Unit)? = null, // Instantly win the level when guaranteed (finish level fast)
    onPlaceSupportObject: ((SupportObjectType, Position) -> Boolean)? = null, // Place a level support object at a position
    onPlaceSupportFief: ((de.egril.defender.model.FiefType, Position) -> Boolean)? = null, // Place a level support fief token at a position
    onCastSupportSpellToken: ((SpellType) -> Unit)? = null, // Start casting a level support spell token (no mana cost)
    activeSpellToken: SpellType? = null, // Currently active support spell token (highlighted in support bar)
    onActivateCooldownPower: ((de.egril.defender.model.CooldownPowerType) -> Unit)? = null, // Activate a cooldown-based support power
    onSandboxSpawnEnemy: ((de.egril.defender.model.AttackerType, Int, de.egril.defender.model.Position?) -> Unit)? = null, // Sandbox: spawn an adjustable test enemy at an optional spawn point
    onSandboxAddCoins: (() -> Unit)? = null, // Sandbox: add coins
    onSandboxPaintTile: ((de.egril.defender.model.Position, de.egril.defender.editor.TileType, de.egril.defender.model.RiverFlow, Int) -> Unit)? = null, // Sandbox: repaint a map tile (with river flow when RIVER)
) {
    val state = remember { GamePlayScreenState() }
    with(state) {
        // Wrap end-turn callbacks to always clear the selected tower type when the turn ends
        val endPlayerTurnAction: () -> Unit = {
            selectedDefenderType = null
            onEndPlayerTurn()
        }
        val autoAttackAndEndTurnAction: () -> Unit = {
            selectedDefenderType = null
            onAutoAttackAndEndTurn()
        }

        // Focus requester for the screen container - ensures keyboard events (especially ESC) always work
        val screenFocusRequester = remember { FocusRequester() }

        // Check if unsaved changes feature is enabled (both hasUnsavedChanges and onSaveGame must be available)
        val unsavedChangesEnabled = hasUnsavedChanges != null && onSaveGame != null

        // Get platform-specific UI scale for mobile (affects layout, not just rendering)
        val uiScale = getGameplayUIScale()

        // Create a scaled density with separate scaling for layout (dp) and text (sp)
        // Layout elements (padding, spacing) scaled to 0.5x to save space
        // Text/icons scaled to 1.5x (doubled from 0.75x) for better readability on mobile
        val density = LocalDensity.current
        val scaledDensity =
            remember(density, uiScale) {
                // Scale layout (dp) by uiScale, but scale text (sp) to be larger
                val textScale =
                    if (uiScale < 1f) {
                        // For mobile, use 1.5x for text (doubled from previous 0.75x)
                        // This means text will be at ~original size despite layout being 0.5x
                        1.5f
                    } else {
                        1f // Desktop unchanged
                    }
                Density(density.density * uiScale, density.fontScale * textScale)
            }

        val captionsEnabled = AppSettings.captionsEnabled.value

        GamePlayScreenEffects(
            state = state,
            gameState = gameState,
            isDemoMode = isDemoMode,
            demoSelectedDefenderType = demoSelectedDefenderType,
            demoSelectedDefenderId = demoSelectedDefenderId,
            demoSelectedTargetPosition = demoSelectedTargetPosition,
            screenFocusRequester = screenFocusRequester,
            captionsEnabled = captionsEnabled,
            cheatDigOutcome = cheatDigOutcome,
            onClearCheatDigOutcome = onClearCheatDigOutcome,
        )

        // Helper: select the next (or previous if reversed) actionable tower, scroll to it, and
        // pre-select its best auto-attack target. If no actionable tower exists, visually highlight
        // the End Turn button (do NOT actually end the turn).
        val jumpToNextActionableTower: (Int?, Boolean) -> Unit = { currentId, reversed ->
            val actionable = gameState.getActionableTowersForTab()
            if (actionable.isEmpty()) {
                // No tower with actions left → highlight the End Turn button (keyboard focus)
                highlightEndTurnButton = true
            } else {
                highlightEndTurnButton = false
                val currentIdx = actionable.indexOfFirst { it.id == currentId }
                val nextIdx =
                    if (reversed) {
                        if (currentIdx <= 0) actionable.size - 1 else currentIdx - 1
                    } else {
                        if (currentIdx < 0 || currentIdx >= actionable.size - 1) 0 else currentIdx + 1
                    }
                val nextTower = actionable[nextIdx]
                selectedDefenderId = nextTower.id
                selectedAttackerId = null
                selectedMineAction = null
                selectedWizardAction = null
                selectedBarricadeAction = null
                tabScrollPosition = nextTower.position.value

                // Auto-select the best attack target using the same logic as automatic attacks
                val autoTarget = onGetAutoAttackTarget?.invoke(nextTower.id)
                if (autoTarget != null) {
                    selectedTargetPosition = autoTarget
                    selectedTargetId =
                        gameState.attackers
                            .find {
                                !it.isDefeated.value && it.position.value == autoTarget
                            }?.id
                } else {
                    selectedTargetId = null
                    selectedTargetPosition = null
                }
            }
        }

        val keyboardSelectableTowers =
            remember(gameState.level.availableTowers) {
                gameState.level.availableTowers.filter { it != DefenderType.DRAGONS_LAIR && it != DefenderType.ALTAR }
            }

        val cycleTowerBuySelection: (Boolean) -> Unit = cycleTowerBuySelection@{ reversed ->
            // Filter to only affordable towers when cycling
            val affordableTowers = keyboardSelectableTowers.filter { gameState.canAffordTowerButton(it) }
            if (affordableTowers.isEmpty()) {
                return@cycleTowerBuySelection
            }
            val currentIndex =
                selectedDefenderType?.let { current ->
                    affordableTowers.indexOf(current).takeIf { it != -1 }
                } ?: -1
            val nextIndex =
                if (reversed) {
                    if (currentIndex <= 0) affordableTowers.lastIndex else currentIndex - 1
                } else {
                    if (currentIndex < 0 || currentIndex >= affordableTowers.lastIndex) 0 else currentIndex + 1
                }
            selectedDefenderType = affordableTowers[nextIndex]
            selectedDefenderId = null
            selectedAttackerId = null
            highlightEndTurnButton = false
        }

        val handleTowerSelectionShortcut: (Boolean) -> Unit = { reversed ->
            if (gameState.phase.value == GamePhase.INITIAL_BUILDING) {
                cycleTowerBuySelection(reversed)
            } else if (gameState.phase.value == GamePhase.PLAYER_TURN) {
                if (selectedDefenderType != null && selectedDefenderId == null && !showMagicPanel) {
                    cycleTowerBuySelection(reversed)
                } else {
                    jumpToNextActionableTower(selectedDefenderId, reversed)
                }
            }
        }

        val keyboardSelectableSpells =
            remember(playerStats) {
                SpellType.entries.filter { spell ->
                    playerStats?.unlockedSpells?.contains(spell) == true
                }
            }
        LaunchedEffect(showMagicPanel, selectedSpell, keyboardSelectableSpells) {
            if (!showMagicPanel || keyboardSelectableSpells.isEmpty()) {
                keyboardSpellFocusIndex = 0
                return@LaunchedEffect
            }
            val currentIndex = keyboardSelectableSpells.indexOf(selectedSpell)
            keyboardSpellFocusIndex = if (currentIndex >= 0) currentIndex else 0
        }

        // Auto-jump: select first actionable tower when player turn starts (if setting is ON)
        LaunchedEffect(gameState.phase.value) {
            if (AppSettings.autoJumpToNextTower.value &&
                gameState.phase.value == GamePhase.PLAYER_TURN
            ) {
                jumpToNextActionableTower(null, false)
            }
        }

        // Auto-jump: when the current selected tower runs out of actions, jump to the next (if setting is ON).
        // Use SideEffect to track the actions-remaining value from the previous composition so that we
        // only jump on the transition (> 0) → 0 for the *same* tower, and ignore manually selected
        // towers that already have 0 actions when selected.
        val selectedDefenderActionsRemaining =
            selectedDefenderId?.let { id ->
                gameState.defenders
                    .find { it.id == id }
                    ?.actionsRemaining
                    ?.value
            }
        val prevAutoJumpDefenderId = remember { mutableStateOf<Int?>(null) }
        val prevAutoJumpActions = remember { mutableStateOf<Int?>(null) }
        val actionsJustExhausted =
            selectedDefenderId != null &&
                selectedDefenderId == prevAutoJumpDefenderId.value &&
                selectedDefenderActionsRemaining == 0 &&
                (prevAutoJumpActions.value ?: 0) > 0
        SideEffect {
            prevAutoJumpDefenderId.value = selectedDefenderId
            prevAutoJumpActions.value = selectedDefenderActionsRemaining
        }
        LaunchedEffect(actionsJustExhausted) {
            if (actionsJustExhausted &&
                AppSettings.autoJumpToNextTower.value &&
                gameState.phase.value == GamePhase.PLAYER_TURN
            ) {
                jumpToNextActionableTower(selectedDefenderId, false)
            }
        }

        // Clear End Turn button highlight when a defender is selected (manual map click or other means)
        LaunchedEffect(selectedDefenderId) {
            if (selectedDefenderId != null) {
                highlightEndTurnButton = false
            }
        }

        // Reset keyboard build tile selection when tower-place mode exits
        LaunchedEffect(selectedDefenderType) {
            if (selectedDefenderType == null) {
                keyboardSelectedBuildTile = null
            }
        }

        // Reset the keyboard placement/targeting cursor when neither a support object is selected nor a
        // spell is being targeted, so a stale cursor does not linger after placement finishes or is cancelled.
        LaunchedEffect(selectedSupportObject, selectedSupportFief, gameState.spellTargeting.value) {
            if (selectedSupportObject == null && selectedSupportFief == null && gameState.spellTargeting.value == null) {
                keyboardPlacementTile = null
            }
        }

        // Mine action handler
        val handleMineAction: (Int, MineAction) -> Unit = { mineId, action ->
            when (action) {
                MineAction.DIG -> {
                    val outcome = onMineDig?.invoke(mineId)
                    if (outcome != null) {
                        // If a dragon was spawned, find the newly created lair and get the dragon's name
                        if (outcome == DigOutcome.DRAGON) {
                            val newLair = gameState.defenders.lastOrNull { it.type == DefenderType.DRAGONS_LAIR }
                            currentDragonName = newLair?.dragonName
                        } else {
                            currentDragonName = null
                        }
                        // Show dig result after the mine-dig animation plays (1.5 s when animations ON)
                        pendingDigOutcome = outcome
                        pendingDigKey++
                    }
                }

                MineAction.BUILD_TRAP -> {
                    // Toggle trap placement mode - if already selected, deselect it
                    selectedMineAction = if (selectedMineAction == action) null else action
                    // Clear target selection when entering trap placement mode
                    if (selectedMineAction != null) {
                        selectedTargetId = null
                        selectedTargetPosition = null
                    }
                    showMineActionDialog = true
                }
            }
        }

        // Wizard action handler - similar to mine action, click button first then select on map
        val handleWizardAction: (Int, WizardAction) -> Unit = { wizardId, action ->
            when (action) {
                WizardAction.GENERATE_MANA -> {
                    // Generate mana - immediate action
                    onWizardGenerateMana?.invoke(wizardId)
                }
                WizardAction.PLACE_MAGICAL_TRAP -> {
                    // Toggle trap placement mode - if already selected, deselect it
                    selectedWizardAction = if (selectedWizardAction == action) null else action
                    // Clear target selection when entering trap placement mode
                    if (selectedWizardAction != null) {
                        selectedTargetId = null
                        selectedTargetPosition = null
                    }
                    // The user will now click on the map to place the trap
                }
            }
        }

        // Barricade action handler - similar to wizard action, click button first then select on map
        val handleBarricadeAction: (Int, BarricadeAction) -> Unit = { towerId, action ->
            when (action) {
                BarricadeAction.BUILD_BARRICADE -> {
                    // Toggle placement mode - if already selected, deselect it
                    selectedBarricadeAction = if (selectedBarricadeAction == action) null else action
                    // Clear target selection when entering barricade placement mode
                    if (selectedBarricadeAction != null) {
                        selectedTargetId = null
                        selectedTargetPosition = null
                    }
                    // The user will now click on the map to place the barricade
                }
            }
        }

        // Map panning via keys works from any focus. Bubble phase: only keys that no focused element consumed reach this
        // (e.g. text fields keep their letters and arrows; the map itself consumes pan keys when focused).
        val mapPanController = remember { MapPanController() }
        val mapPanKeyHandler: (KeyEvent) -> Boolean = { event ->
            val noModifiers = !event.isCtrlPressed && !event.isAltPressed && !event.isShiftPressed && !event.isMetaPressed
            val up = isShortcutBindingPressed(event, AppSettings.shortcutPanUp.value) || (noModifiers && event.key == Key.DirectionUp)
            val down = isShortcutBindingPressed(event, AppSettings.shortcutPanDown.value) || (noModifiers && event.key == Key.DirectionDown)
            val left = isShortcutBindingPressed(event, AppSettings.shortcutPanLeft.value) || (noModifiers && event.key == Key.DirectionLeft)
            val right = isShortcutBindingPressed(event, AppSettings.shortcutPanRight.value) || (noModifiers && event.key == Key.DirectionRight)
            val isPanKey = up || down || left || right
            if (isDemoMode || event.type != KeyEventType.KeyDown || !isPanKey) {
                false
            } else {
                val dx =
                    if (left) {
                        MAP_KEYBOARD_PAN_STEP
                    } else if (right) {
                        -MAP_KEYBOARD_PAN_STEP
                    } else {
                        0f
                    }
                val dy =
                    if (up) {
                        MAP_KEYBOARD_PAN_STEP
                    } else if (down) {
                        -MAP_KEYBOARD_PAN_STEP
                    } else {
                        0f
                    }
                mapPanController.pan(dx, dy)
                true
            }
        }

        val keyboardHandler: (KeyEvent) -> Boolean =
            rememberGamePlayKeyboardHandler(
                state = state,
                gameState = gameState,
                isDemoMode = isDemoMode,
                showMagicPanel = showMagicPanel,
                hasUnsavedChanges = hasUnsavedChanges,
                onSaveGame = onSaveGame,
                onCheatCode = onCheatCode,
                onBackToMap = onBackToMap,
                onOpenMagicPanel = onOpenMagicPanel,
                onCloseMagicPanel = onCloseMagicPanel,
                onCastSpell = onCastSpell,
                onExitSpellTargeting = onExitSpellTargeting,
                onStartFirstPlayerTurn = onStartFirstPlayerTurn,
                onDefenderAttack = onDefenderAttack,
                onDefenderAttackPosition = onDefenderAttackPosition,
                onPlaceDefender = onPlaceDefender,
                onUpgradeDefender = onUpgradeDefender,
                onActivateAltar = onActivateAltar,
                onPlaceSupportObject = onPlaceSupportObject,
                onPlaceSupportFief = onPlaceSupportFief,
                onCastSupportSpellToken = onCastSupportSpellToken,
                onActivateCooldownPower = onActivateCooldownPower,
                onSelectSpellTarget = onSelectSpellTarget,
                keyboardSelectableTowers = keyboardSelectableTowers,
                keyboardSelectableSpells = keyboardSelectableSpells,
                handleTowerSelectionShortcut = handleTowerSelectionShortcut,
                endPlayerTurnAction = endPlayerTurnAction,
                autoAttackAndEndTurnAction = autoAttackAndEndTurnAction,
                handleMineAction = handleMineAction,
                handleWizardAction = handleWizardAction,
                handleBarricadeAction = handleBarricadeAction,
            )

        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .focusRequester(screenFocusRequester)
                        .focusTarget()
                        .onPreviewKeyEvent(keyboardHandler)
                        .onKeyEvent(mapPanKeyHandler)
                        // In demo mode, intercept any click/tap to show "stop demo?" dialog
                        .then(
                            if (isDemoMode) {
                                Modifier.clickable(
                                    indication = null,
                                    interactionSource =
                                        remember {
                                            androidx.compose.foundation.interaction
                                                .MutableInteractionSource()
                                        },
                                ) { showStopDemoDialog = true }
                            } else {
                                Modifier
                            },
                        ),
            ) {
                val availableWindowWidth = maxWidth
                val availableWindowHeight = maxHeight
                val windowSize =
                    remember(maxWidth, maxHeight) {
                        "Window: ${maxWidth.value.toInt()} x ${maxHeight.value.toInt()} dp"
                    }

                Surface(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .accessibilityVisualFilter(
                                highContrastEnabled = AppSettings.highContrastEnabled.value,
                                colorBlindPalette = AppSettings.colorBlindPalette.value,
                            ),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Header with prominent phase indicator (collapsible)
                        GameHeader(
                            gameState = gameState,
                            showOverlay = showOverlay,
                            onShowOverlayChange = { showOverlay = it },
                            onBackToMap = {
                                if (isDemoMode) {
                                    showStopDemoDialog = true
                                } else if (unsavedChangesEnabled && hasUnsavedChanges.invoke()) {
                                    showUnsavedChangesDialog = true
                                } else {
                                    onBackToMap()
                                }
                            },
                            onSaveGame =
                                if (onSaveGame != null && !isDemoMode) {
                                    { showSaveDialog = true }
                                } else {
                                    null
                                },
                            onCheatCode =
                                if (onCheatCode != null && !isDemoMode) {
                                    { showCheatDialog = true }
                                } else {
                                    null
                                },
                            onEnemyCountClick = { showOverlay = !showOverlay },
                            onWinLevelInfoClick = { showEndTurnConfirmation = true },
                            onSandboxTools =
                                if (onSandboxSpawnEnemy != null && !isDemoMode) {
                                    { showSandboxTools = true }
                                } else {
                                    null
                                },
                            onManaClick =
                                if (onOpenMagicPanel != null && gameState.maxMana.value > 0 && !isDemoMode) {
                                    {
                                        if (gameState.instantTowerSpellActive.value) {
                                            showAbortInstantTowerDialog = true
                                        } else if (showMagicPanel) {
                                            onCloseMagicPanel?.invoke()
                                        } else {
                                            onOpenMagicPanel.invoke()
                                        }
                                    }
                                } else {
                                    null
                                },
                            isDemoMode = isDemoMode,
                            onDemoTitleClick =
                                if (isDemoMode) {
                                    { showStopDemoDialog = true }
                                } else {
                                    null
                                },
                            externalShowShortcuts = triggerShowShortcuts,
                            onExternalShowShortcutsHandled = { triggerShowShortcuts = false },
                            externalShowHelp = triggerShowHelp,
                            onExternalShowHelpHandled = { triggerShowHelp = false },
                            externalShowFeedback = triggerShowFeedback,
                            onExternalShowFeedbackHandled = { triggerShowFeedback = false },
                            externalShowSettings = triggerShowSettings,
                            onExternalShowSettingsHandled = { triggerShowSettings = false },
                        )

                        gameState.attackers
                            .firstOrNull { it.type.showBossHealthBar && !it.isDefeated.value }
                            ?.let { BossHealthBar(it) }

                        Spacer(modifier = Modifier.height(8.dp))

                        GamePlayMapArea(
                            state = state,
                            gameState = gameState,
                            isDemoMode = isDemoMode,
                            showMagicPanel = showMagicPanel,
                            captionsEnabled = captionsEnabled,
                            availableWindowWidth = availableWindowWidth,
                            availableWindowHeight = availableWindowHeight,
                            demoHoveredPosition = demoHoveredPosition,
                            scrollToPosition = scrollToPosition,
                            onScrollToPositionConsumed = onScrollToPositionConsumed,
                            onPlaceDefender = onPlaceDefender,
                            onPlaceSupportObject = onPlaceSupportObject,
                            onPlaceSupportFief = onPlaceSupportFief,
                            onSelectSpellTarget = onSelectSpellTarget,
                            onOpenMagicPanel = onOpenMagicPanel,
                            onCloseMagicPanel = onCloseMagicPanel,
                            onMineBuildTrap = onMineBuildTrap,
                            onWizardPlaceMagicalTrap = onWizardPlaceMagicalTrap,
                            onBuildBarricade = onBuildBarricade,
                            onSandboxPaintTile = onSandboxPaintTile,
                            mapPanController = mapPanController,
                        )

                        GamePlayControlsArea(
                            state = state,
                            gameState = gameState,
                            showMagicPanel = showMagicPanel,
                            playerStats = playerStats,
                            selectedSpell = selectedSpell,
                            activeSpellToken = activeSpellToken,
                            uiScale = uiScale,
                            endPlayerTurnAction = endPlayerTurnAction,
                            handleMineAction = handleMineAction,
                            handleWizardAction = handleWizardAction,
                            handleBarricadeAction = handleBarricadeAction,
                            onCastSpell = onCastSpell,
                            onCloseMagicPanel = onCloseMagicPanel,
                            onExitSpellTargeting = onExitSpellTargeting,
                            onCastSupportSpellToken = onCastSupportSpellToken,
                            onActivateCooldownPower = onActivateCooldownPower,
                            onUpgradeDefender = onUpgradeDefender,
                            onSanctifyDefender = onSanctifyDefender,
                            onActivateAltar = onActivateAltar,
                            onUndoTower = onUndoTower,
                            onSellTower = onSellTower,
                            onDefenderAttack = onDefenderAttack,
                            onDefenderAttackPosition = onDefenderAttackPosition,
                            onStartFirstPlayerTurn = onStartFirstPlayerTurn,
                            onRemoveBarricade = onRemoveBarricade,
                        )

                        GamePlayDialogs(
                            gameState = gameState,
                            windowSize = windowSize,
                            hasUnsavedChanges = hasUnsavedChanges,
                            onSaveGame = onSaveGame,
                            onCheatCode = onCheatCode,
                            onSandboxSpawnEnemy = onSandboxSpawnEnemy,
                            onSandboxAddCoins = onSandboxAddCoins,
                            onUndoTower = onUndoTower,
                            onSellTower = onSellTower,
                            onRemoveBarricade = onRemoveBarricade,
                            onBackToMap = onBackToMap,
                            onWinLevelNow = onWinLevelNow,
                            onOpenMagicPanel = onOpenMagicPanel,
                            onCancelInstantTowerSpell = onCancelInstantTowerSpell,
                            onClearPlatformInfo = onClearPlatformInfo,
                            onClearCheatHelp = onClearCheatHelp,
                            onClearSpecialActionsWarning = onClearSpecialActionsWarning,
                            onClearReminderMessage = onClearReminderMessage,
                            onConfirmTargetSpell = onConfirmTargetSpell,
                            onDismissTargetConfirmation = onDismissTargetConfirmation,
                            onDismissFreezeWarning = onDismissFreezeWarning,
                            onDismissGameMessage = onDismissGameMessage,
                            onStopDemoMode = onStopDemoMode,
                            endPlayerTurnAction = endPlayerTurnAction,
                            autoAttackAndEndTurnAction = autoAttackAndEndTurnAction,
                            showPlatformInfo = showPlatformInfo,
                            showCheatHelp = showCheatHelp,
                            specialActionsRemaining = specialActionsRemaining,
                            reminderMessage = reminderMessage,
                            showSpellTargetConfirmation = showSpellTargetConfirmation,
                            showFreezeImmuneWarning = showFreezeImmuneWarning,
                            pendingGameMessage = pendingGameMessage,
                            activeSpellToken = activeSpellToken,
                            currentDigOutcome = currentDigOutcome,
                            currentDragonName = currentDragonName,
                            showDigOutcomeDialog = showDigOutcomeDialog,
                            onShowDigOutcomeDialogChange = { showDigOutcomeDialog = it },
                            onSelectedDefenderTypeChange = { selectedDefenderType = it },
                            onSelectedDefenderIdChange = { selectedDefenderId = it },
                            showAbortInstantTowerDialog = showAbortInstantTowerDialog,
                            onShowAbortInstantTowerDialogChange = { showAbortInstantTowerDialog = it },
                            showSaveDialog = showSaveDialog,
                            onShowSaveDialogChange = { showSaveDialog = it },
                            saveCommentInput = saveCommentInput,
                            onSaveCommentInputChange = { saveCommentInput = it },
                            showSaveConfirmation = showSaveConfirmation,
                            onShowSaveConfirmationChange = { showSaveConfirmation = it },
                            showCheatDialog = showCheatDialog,
                            onShowCheatDialogChange = { showCheatDialog = it },
                            cheatCodeInput = cheatCodeInput,
                            onCheatCodeInputChange = { cheatCodeInput = it },
                            showSandboxTools = showSandboxTools,
                            onShowSandboxToolsChange = { showSandboxTools = it },
                            showRemoveBarricadeDialog = showRemoveBarricadeDialog,
                            onShowRemoveBarricadeDialogChange = { showRemoveBarricadeDialog = it },
                            barricadeToRemove = barricadeToRemove,
                            onBarricadeToRemoveChange = { barricadeToRemove = it },
                            showRemoveTrapDialog = showRemoveTrapDialog,
                            onShowRemoveTrapDialogChange = { showRemoveTrapDialog = it },
                            trapToRemove = trapToRemove,
                            onTrapToRemoveChange = { trapToRemove = it },
                            keyboardUndoOrSellConfirmation = keyboardUndoOrSellConfirmation,
                            onKeyboardUndoOrSellConfirmationChange = { keyboardUndoOrSellConfirmation = it },
                            showUnsavedChangesDialog = showUnsavedChangesDialog,
                            onShowUnsavedChangesDialogChange = { showUnsavedChangesDialog = it },
                            showEndTurnConfirmation = showEndTurnConfirmation,
                            onShowEndTurnConfirmationChange = { showEndTurnConfirmation = it },
                            showStopDemoDialog = showStopDemoDialog,
                            onShowStopDemoDialogChange = { showStopDemoDialog = it },
                        )
                    }
                }

                // Coin fly-to-counter animation overlay: sits above header + map so coins can travel
                // between them. No-op when animations are disabled.
                CoinFlightOverlay()
            }
        }
    }
}
