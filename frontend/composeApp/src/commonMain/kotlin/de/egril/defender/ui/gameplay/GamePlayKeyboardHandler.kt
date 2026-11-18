package de.egril.defender.ui.gameplay

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import de.egril.defender.model.*
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.isShortcutBindingPressed

// Keyboard event handler for shortcuts
// Using onPreviewKeyEvent to intercept before HexagonalMapView handles it
// This works in the "capture" phase and doesn't require focus on this element
@Composable
internal fun rememberGamePlayKeyboardHandler(
    state: GamePlayScreenState,
    gameState: GameState,
    isDemoMode: Boolean,
    showMagicPanel: Boolean,
    hasUnsavedChanges: (() -> Boolean)?,
    onSaveGame: ((String?) -> String?)?,
    onCheatCode: ((String) -> Boolean)?,
    onBackToMap: () -> Unit,
    onOpenMagicPanel: (() -> Unit)?,
    onCloseMagicPanel: (() -> Unit)?,
    onCastSpell: ((SpellType) -> Unit)?,
    onExitSpellTargeting: (() -> Unit)?,
    onStartFirstPlayerTurn: () -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onDefenderAttackPosition: (Int, Position) -> Boolean,
    onPlaceDefender: (DefenderType, Position) -> Boolean,
    onUpgradeDefender: (Int) -> Boolean,
    onActivateAltar: ((Int) -> Boolean)?,
    onPlaceSupportObject: ((SupportObjectType, Position) -> Boolean)?,
    onPlaceSupportFief: ((de.egril.defender.model.FiefType, Position) -> Boolean)?,
    onCastSupportSpellToken: ((SpellType) -> Unit)?,
    onActivateCooldownPower: ((de.egril.defender.model.CooldownPowerType) -> Unit)?,
    onSelectSpellTarget: ((Any) -> Unit)?,
    keyboardSelectableTowers: List<DefenderType>,
    keyboardSelectableSpells: List<SpellType>,
    handleTowerSelectionShortcut: (Boolean) -> Unit,
    endPlayerTurnAction: () -> Unit,
    autoAttackAndEndTurnAction: () -> Unit,
    handleMineAction: (Int, MineAction) -> Unit,
    handleWizardAction: (Int, WizardAction) -> Unit,
    handleBarricadeAction: (Int, BarricadeAction) -> Unit,
): (KeyEvent) -> Boolean {
    // Local val so the smart cast of hasUnsavedChanges below still works inside the key handler
    val unsavedChangesEnabled = hasUnsavedChanges != null && onSaveGame != null
    return with(state) {
        remember(
            onSaveGame,
            onCheatCode,
            onOpenMagicPanel,
            onCloseMagicPanel,
            onCastSpell,
            onExitSpellTargeting,
            onStartFirstPlayerTurn,
            onDefenderAttack,
            onDefenderAttackPosition,
            showMagicPanel,
            showUnsavedChangesDialog,
            keyboardSelectableSpells,
            endPlayerTurnAction,
            autoAttackAndEndTurnAction,
            isDemoMode,
        ) {
            { event ->
                when {
                    // In demo mode any key press shows "stop demo?" dialog
                    isDemoMode && event.type == KeyEventType.KeyDown -> {
                        showStopDemoDialog = true
                        true
                    }
                    // Unsaved-changes dialog key handling: intercept before generic Esc/Enter handlers
                    event.type == KeyEventType.KeyDown && showUnsavedChangesDialog -> {
                        when {
                            event.key == Key.Enter && !event.isCtrlPressed && !event.isAltPressed -> {
                                // Enter: save and exit
                                onSaveGame?.invoke(null)
                                showUnsavedChangesDialog = false
                                onBackToMap()
                                true
                            }
                            event.key == Key.Escape ||
                                isShortcutBindingPressed(event, AppSettings.shortcutBackToWorldMap.value) -> {
                                // Esc: cancel the dialog
                                showUnsavedChangesDialog = false
                                true
                            }
                            event.key == Key.D && !event.isCtrlPressed && !event.isAltPressed -> {
                                // D: discard changes and exit
                                showUnsavedChangesDialog = false
                                onBackToMap()
                                true
                            }
                            else -> false
                        }
                    }
                    // Ctrl+S: Save game
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutSaveGame.value) &&
                        onSaveGame != null -> {
                        showSaveDialog = true
                        true
                    }
                    // M (remappable): Open/close spell menu
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutToggleSpellMenu.value) &&
                        onOpenMagicPanel != null &&
                        gameState.maxMana.value > 0 &&
                        !isDemoMode -> {
                        if (showMagicPanel) {
                            onCloseMagicPanel?.invoke()
                        } else {
                            onOpenMagicPanel.invoke()
                        }
                        true
                    }
                    // T (remappable): Toggle tower-place mode (close spell UI/targeting); if already
                    // in tower-place mode, switch back to attack/select mode instead
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutSwitchToTowerMode.value) -> {
                        val isInTowerPlaceMode = selectedDefenderType != null && !showMagicPanel && gameState.spellTargeting.value == null
                        if (isInTowerPlaceMode) {
                            // Already in tower-place mode → switch back to attack/select mode
                            selectedDefenderType = null
                        } else {
                            // Enter tower-place mode (close spell UI/targeting first)
                            onCloseMagicPanel?.invoke()
                            if (gameState.spellTargeting.value != null) {
                                onExitSpellTargeting?.invoke()
                            }
                            selectedDefenderType = keyboardSelectableTowers.firstOrNull()
                            selectedDefenderId = null
                            selectedAttackerId = null
                            selectedTargetId = null
                            selectedTargetPosition = null
                            // Leaving the support bar for tower-place mode: drop the support focus cursor.
                            supportFocusIndex = null
                        }
                        true
                    }
                    // B: Toggle split tower selector dropdown (only when split build button is active)
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.B &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        AppSettings.splitBuildTowerButton.value &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) -> {
                        splitSelectorToggle++
                        true
                    }
                    // Spell menu keyboard mode: Tab/Shift+Tab navigates spells
                    event.type == KeyEventType.KeyDown &&
                        showMagicPanel &&
                        event.key == Key.Tab &&
                        keyboardSelectableSpells.isNotEmpty() -> {
                        val delta = if (event.isShiftPressed) -1 else 1
                        val size = keyboardSelectableSpells.size
                        keyboardSpellFocusIndex = (keyboardSpellFocusIndex + delta + size) % size
                        true
                    }
                    // Spell menu keyboard mode: Enter selects/casts focused spell
                    event.type == KeyEventType.KeyDown &&
                        showMagicPanel &&
                        event.key == Key.Enter &&
                        keyboardSelectableSpells.isNotEmpty() &&
                        onCastSpell != null -> {
                        val focusedSpell = keyboardSelectableSpells[keyboardSpellFocusIndex]
                        onCastSpell.invoke(focusedSpell)
                        true
                    }
                    // Ctrl+A: Auto-attack all towers and end turn (player turn only)
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutAutoAttackEndTurn.value) &&
                        gameState.phase.value == GamePhase.PLAYER_TURN -> {
                        autoAttackAndEndTurnAction()
                        true
                    }
                    // F: Attack with selected tower's current target (player turn only)
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutAttackSelectedTarget.value) &&
                        gameState.phase.value == GamePhase.PLAYER_TURN -> {
                        val defenderId = selectedDefenderId
                        val targetId = selectedTargetId
                        val targetPosition = selectedTargetPosition
                        val defender = defenderId?.let { gameState.defenders.find { it.id == defenderId } }
                        if (defender != null && defender.isReady && defender.actionsRemaining.value > 0) {
                            when {
                                targetId != null -> {
                                    onDefenderAttack(defenderId, targetId)
                                    true
                                }
                                targetPosition != null -> {
                                    onDefenderAttackPosition(defenderId, targetPosition)
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    // U (remappable): Upgrade currently selected tower
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutUpgradeSelectedTower.value) &&
                        selectedDefenderId != null -> {
                        selectedDefenderId?.let { defenderId ->
                            onUpgradeDefender(defenderId)
                            true
                        } ?: false
                    }
                    // X (remappable): Undo (if eligible) or sell selected tower – always shows confirmation
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutUndoOrSellSelectedTower.value) &&
                        selectedDefenderId != null -> {
                        val defender = gameState.defenders.find { it.id == selectedDefenderId }
                        if (defender != null) {
                            val canUndo = defender.placedOnTurn == gameState.turnNumber.value && !defender.hasBeenUsed.value
                            // Cannot sell while the Kraken has gripped this barge
                            val canSell = defender.isReady && defender.actionsRemaining.value > 0 && !defender.isGrippedByKraken.value
                            when {
                                canUndo -> {
                                    // Show confirmation dialog: isUndo = true (full refund)
                                    keyboardUndoOrSellConfirmation = Pair(defender.id, true)
                                    true
                                }
                                canSell -> {
                                    // Show confirmation dialog: isUndo = false (75% refund)
                                    keyboardUndoOrSellConfirmation = Pair(defender.id, false)
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    // Tab / Shift+Tab: Select next/previous actionable tower (player turn only)
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutSelectNextTower.value) &&
                        (
                            gameState.phase.value == GamePhase.INITIAL_BUILDING ||
                                gameState.phase.value == GamePhase.PLAYER_TURN
                        ) -> {
                        handleTowerSelectionShortcut(false)
                        true
                    }
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutSelectPreviousTower.value) &&
                        (
                            gameState.phase.value == GamePhase.INITIAL_BUILDING ||
                                gameState.phase.value == GamePhase.PLAYER_TURN
                        ) -> {
                        handleTowerSelectionShortcut(true)
                        true
                    }
                    // Remappable: Center map on selected tower
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutCenterSelectedTower.value) -> {
                        val defender =
                            selectedDefenderId?.let { defenderId ->
                                gameState.defenders.find { it.id == defenderId }
                            }
                        if (defender != null) {
                            tabScrollPosition = defender.position.value
                            true
                        } else {
                            false
                        }
                    }
                    // Remappable: Center map on next spawn point
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutCenterNextSpawnPoint.value) -> {
                        val spawnPoints = gameState.level.startPositions
                        if (spawnPoints.isEmpty()) {
                            false
                        } else {
                            val nextPosition = spawnPoints[nextSpawnPointIndex % spawnPoints.size]
                            tabScrollPosition = nextPosition
                            nextSpawnPointIndex = (nextSpawnPointIndex + 1) % spawnPoints.size
                            true
                        }
                    }
                    // C: Open cheat code dialog
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutCheat.value) &&
                        onCheatCode != null -> {
                        showCheatDialog = true
                        true
                    }
                    // E: Toggle enemy list overlay (cycle: off → both → legend only → enemy list only → off)
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutToggleEnemyList.value) -> {
                        if (!showOverlay) {
                            showOverlay = true
                            overlayMode = 0 // both
                        } else {
                            when (overlayMode) {
                                0 -> overlayMode = 1 // legend only
                                1 -> overlayMode = 2 // enemy list only
                                else -> {
                                    showOverlay = false
                                    overlayMode = 0
                                }
                            }
                        }
                        true
                    }
                    // /: Open keyboard shortcuts dialog
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.Slash &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isShiftPressed -> {
                        triggerShowShortcuts = true
                        true
                    }
                    // H: Open tutorials/help dialog
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.H &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isShiftPressed &&
                        selectedDefenderId == null &&
                        selectedDefenderType == null -> {
                        triggerShowHelp = true
                        true
                    }
                    // Period (.): Open feedback dialog
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.Period &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isShiftPressed -> {
                        triggerShowFeedback = true
                        true
                    }
                    // Comma (,): Open settings dialog
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.Comma &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isShiftPressed -> {
                        triggerShowSettings = true
                        true
                    }
                    // P (remappable): Toggle audio on/off
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutToggleAudio.value) -> {
                        AppSettings.saveSoundEnabled(!AppSettings.isSoundEnabled.value)
                        true
                    }
                    // Reuses the "next enemy target" binding (default N) for cycling build tiles in tower-place mode
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutNextEnemyTarget.value) &&
                        selectedDefenderType != null &&
                        !showMagicPanel &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) -> {
                        val buildTiles = selectedDefenderType?.let { gameState.keyboardPlacementCandidates(it) }.orEmpty()
                        if (buildTiles.isNotEmpty()) {
                            val currentIdx = keyboardSelectedBuildTile?.let { buildTiles.indexOf(it).takeIf { i -> i != -1 } }
                            val nextIdx = if (currentIdx == null || currentIdx >= buildTiles.lastIndex) 0 else currentIdx + 1
                            keyboardSelectedBuildTile = buildTiles[nextIdx]
                            tabScrollPosition = buildTiles[nextIdx]
                            true
                        } else {
                            false
                        }
                    }
                    // Reuses the "prev enemy target" binding (default Shift+N) for cycling build tiles in tower-place mode
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutPrevEnemyTarget.value) &&
                        selectedDefenderType != null &&
                        !showMagicPanel &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) -> {
                        val buildTiles = selectedDefenderType?.let { gameState.keyboardPlacementCandidates(it) }.orEmpty()
                        if (buildTiles.isNotEmpty()) {
                            val currentIdx = keyboardSelectedBuildTile?.let { buildTiles.indexOf(it).takeIf { i -> i != -1 } }
                            val prevIdx = if (currentIdx == null || currentIdx <= 0) buildTiles.lastIndex else currentIdx - 1
                            keyboardSelectedBuildTile = buildTiles[prevIdx]
                            tabScrollPosition = buildTiles[prevIdx]
                            true
                        } else {
                            false
                        }
                    }
                    // Move the keyboard placement/targeting cursor a whole grid row up/down while placing
                    // a support object or targeting a spell. Reuses the pan up/down bindings (W/S by
                    // default) — during placement the map already auto-follows the cursor, so these keys
                    // move between rows instead of panning. Complements the next/prev (row-major) stepping
                    // below. Placed before pan handling (onPreviewKeyEvent) so it takes precedence.
                    event.type == KeyEventType.KeyDown &&
                        (
                            isShortcutBindingPressed(event, AppSettings.shortcutPanUp.value) ||
                                isShortcutBindingPressed(event, AppSettings.shortcutPanDown.value)
                        ) &&
                        !showMagicPanel &&
                        (selectedSupportObject != null || selectedSupportFief != null || gameState.spellTargeting.value != null) &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) -> {
                        val candidateTiles =
                            selectedSupportObject?.let { supportObjectPlacementTiles(gameState, it) }
                                ?: selectedSupportFief?.let { supportFiefPlacementTiles(gameState, it) }
                                ?: spellTargetPositions(gameState)
                        if (candidateTiles.isEmpty()) {
                            false
                        } else {
                            val up = isShortcutBindingPressed(event, AppSettings.shortcutPanUp.value)
                            val target = rowStepPlacementTile(candidateTiles, keyboardPlacementTile, up)
                            if (target != null) {
                                keyboardPlacementTile = target
                                tabScrollPosition = target
                            }
                            // Consume the key even at the top/bottom edge so it never falls through to
                            // panning while a placement/targeting mode is active.
                            true
                        }
                    }
                    // Cycle the keyboard placement/targeting cursor over the valid tiles while placing a
                    // support object or targeting a spell (reuses the next/prev enemy-target bindings, as
                    // tower placement does for build tiles). Placed before the enemy-target handlers so it
                    // takes precedence whenever a placement/targeting mode is active.
                    event.type == KeyEventType.KeyDown &&
                        (
                            isShortcutBindingPressed(event, AppSettings.shortcutNextEnemyTarget.value) ||
                                isShortcutBindingPressed(event, AppSettings.shortcutPrevEnemyTarget.value)
                        ) &&
                        !showMagicPanel &&
                        (selectedSupportObject != null || selectedSupportFief != null || gameState.spellTargeting.value != null) &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) -> {
                        val candidateTiles =
                            selectedSupportObject?.let { supportObjectPlacementTiles(gameState, it) }
                                ?: selectedSupportFief?.let { supportFiefPlacementTiles(gameState, it) }
                                ?: spellTargetPositions(gameState)
                        if (candidateTiles.isEmpty()) {
                            false
                        } else {
                            val forward = isShortcutBindingPressed(event, AppSettings.shortcutNextEnemyTarget.value)
                            val currentIdx =
                                keyboardPlacementTile?.let { candidateTiles.indexOf(it).takeIf { i -> i != -1 } }
                            val nextIdx =
                                when {
                                    currentIdx == null -> if (forward) 0 else candidateTiles.lastIndex
                                    forward -> (currentIdx + 1) % candidateTiles.size
                                    else -> (currentIdx - 1 + candidateTiles.size) % candidateTiles.size
                                }
                            keyboardPlacementTile = candidateTiles[nextIdx]
                            tabScrollPosition = candidateTiles[nextIdx]
                            true
                        }
                    }
                    // N (remappable): Cycle to next reachable enemy target
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutNextEnemyTarget.value) &&
                        selectedDefenderId != null &&
                        gameState.phase.value == GamePhase.PLAYER_TURN -> {
                        val defender = gameState.defenders.find { it.id == selectedDefenderId }
                        if (defender != null) {
                            val reachableEnemies =
                                gameState.attackers.filter { attacker ->
                                    !attacker.isDefeated.value &&
                                        defender.position.value.distanceTo(attacker.position.value) <= defender.range
                                }
                            if (reachableEnemies.isNotEmpty()) {
                                val currentIdx =
                                    selectedTargetId?.let { tid ->
                                        reachableEnemies.indexOfFirst { it.id == tid }
                                    } ?: -1
                                val nextIdx = (currentIdx + 1) % reachableEnemies.size
                                val nextEnemy = reachableEnemies[nextIdx]
                                selectedTargetId = nextEnemy.id
                                selectedTargetPosition = nextEnemy.position.value
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }
                    // Shift+N (remappable): Cycle to previous reachable enemy target
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutPrevEnemyTarget.value) &&
                        selectedDefenderId != null &&
                        gameState.phase.value == GamePhase.PLAYER_TURN -> {
                        val defender = gameState.defenders.find { it.id == selectedDefenderId }
                        if (defender != null) {
                            val reachableEnemies =
                                gameState.attackers.filter { attacker ->
                                    !attacker.isDefeated.value &&
                                        defender.position.value.distanceTo(attacker.position.value) <= defender.range
                                }
                            if (reachableEnemies.isNotEmpty()) {
                                val currentIdx =
                                    selectedTargetId?.let { tid ->
                                        reachableEnemies.indexOfFirst { it.id == tid }
                                    } ?: 0
                                val prevIdx = (currentIdx - 1 + reachableEnemies.size) % reachableEnemies.size
                                val prevEnemy = reachableEnemies[prevIdx]
                                selectedTargetId = prevEnemy.id
                                selectedTargetPosition = prevEnemy.position.value
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }
                    // Left / Right: Move the keyboard-focus cursor between support elements
                    // (placeable objects, spell tokens, cooldown powers) in the support bar.
                    event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionLeft || event.key == Key.DirectionRight) &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isShiftPressed &&
                        !showMagicPanel &&
                        (gameState.phase.value == GamePhase.PLAYER_TURN || gameState.phase.value == GamePhase.INITIAL_BUILDING) -> {
                        val slots = visibleSupportSlots(gameState)
                        if (slots.isEmpty()) {
                            false
                        } else {
                            supportFocusIndex =
                                nextSupportFocusIndex(
                                    current = supportFocusIndex,
                                    slotCount = slots.size,
                                    forward = event.key == Key.DirectionRight,
                                )
                            true
                        }
                    }
                    // Enter / Space: Activate (select / cast / trigger) the support element currently
                    // under the keyboard-focus cursor. Placed before the plain-Enter placement handler
                    // so it only intercepts Enter while a support box is focused.
                    event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Enter || event.key == Key.Spacebar) &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isShiftPressed &&
                        !showMagicPanel &&
                        supportFocusIndex != null &&
                        (gameState.phase.value == GamePhase.PLAYER_TURN || gameState.phase.value == GamePhase.INITIAL_BUILDING) -> {
                        val slots = visibleSupportSlots(gameState)
                        val slot = slots.getOrNull(supportFocusIndex ?: -1)
                        if (slot != null && isSupportSlotEnabled(gameState, slot, barEnabled = true)) {
                            when (slot) {
                                is SupportSlot.ObjectSlot -> {
                                    selectedSupportObject = if (selectedSupportObject == slot.type) null else slot.type
                                    selectedSupportFief = null
                                    selectedDefenderType = null
                                }
                                is SupportSlot.FiefSlot -> {
                                    selectedSupportFief = if (selectedSupportFief == slot.type) null else slot.type
                                    selectedSupportObject = null
                                    selectedDefenderType = null
                                }
                                is SupportSlot.SpellSlot -> {
                                    selectedSupportObject = null
                                    selectedSupportFief = null
                                    onCastSupportSpellToken?.invoke(slot.spell)
                                }
                                is SupportSlot.PowerSlot -> {
                                    selectedSupportObject = null
                                    selectedSupportFief = null
                                    onActivateCooldownPower?.invoke(slot.type)
                                }
                            }
                            // Drop the focus cursor after activating so a plain Enter afterwards
                            // confirms tower placement again instead of re-triggering this support.
                            supportFocusIndex = null
                            true
                        } else {
                            // Consume Enter/Space even when the focused box can't be used, so pressing
                            // select on a focused-but-disabled support doesn't fall through to the
                            // tower-placement / attack handlers bound to the same keys.
                            true
                        }
                    }
                    // Enter / Space: Confirm placing the selected support object, or casting the spell
                    // being targeted, on the keyboard placement cursor tile (falling back to the first
                    // valid tile). Placed before the tower-placement Enter handler so it wins whenever a
                    // support object is selected or a spell is being targeted.
                    event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Enter || event.key == Key.Spacebar) &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isMetaPressed &&
                        !showMagicPanel &&
                        (selectedSupportObject != null || selectedSupportFief != null || gameState.spellTargeting.value != null) &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) -> {
                        val supportType = selectedSupportObject
                        val fiefType = selectedSupportFief
                        if (supportType != null) {
                            val candidateTiles = supportObjectPlacementTiles(gameState, supportType)
                            val tile =
                                keyboardPlacementTile?.takeIf { candidateTiles.contains(it) }
                                    ?: candidateTiles.firstOrNull()
                            if (tile != null && onPlaceSupportObject?.invoke(supportType, tile) == true) {
                                val remaining = gameState.supportObjectsRemaining[supportType] ?: 0
                                if (remaining <= 0) {
                                    selectedSupportObject = null
                                    keyboardPlacementTile = null
                                } else {
                                    // Keep the placement cursor near the tile just used instead of
                                    // resetting it to the first tile, so consecutive placements stay
                                    // in the same area of the map. The placed tile is now occupied and
                                    // dropped from the list, so the same (clamped) index lands on the
                                    // next remaining tile.
                                    val placedIndex = candidateTiles.indexOf(tile)
                                    val remainingTiles = supportObjectPlacementTiles(gameState, supportType)
                                    keyboardPlacementTile =
                                        if (remainingTiles.isEmpty()) {
                                            null
                                        } else {
                                            remainingTiles[placedIndex.coerceIn(0, remainingTiles.lastIndex)]
                                        }
                                }
                            }
                        } else if (fiefType != null) {
                            val candidateTiles = supportFiefPlacementTiles(gameState, fiefType)
                            val tile =
                                keyboardPlacementTile?.takeIf { candidateTiles.contains(it) }
                                    ?: candidateTiles.firstOrNull()
                            if (tile != null && onPlaceSupportFief?.invoke(fiefType, tile) == true) {
                                val remaining = gameState.supportFiefRemaining[fiefType] ?: 0
                                if (remaining <= 0) {
                                    selectedSupportFief = null
                                    keyboardPlacementTile = null
                                } else {
                                    val placedIndex = candidateTiles.indexOf(tile)
                                    val remainingTiles = supportFiefPlacementTiles(gameState, fiefType)
                                    keyboardPlacementTile =
                                        if (remainingTiles.isEmpty()) {
                                            null
                                        } else {
                                            remainingTiles[placedIndex.coerceIn(0, remainingTiles.lastIndex)]
                                        }
                                }
                            }
                        } else {
                            val targeting = gameState.spellTargeting.value
                            val candidateTiles = spellTargetPositions(gameState)
                            val tile =
                                keyboardPlacementTile?.takeIf { candidateTiles.contains(it) }
                                    ?: candidateTiles.firstOrNull()
                            if (targeting != null && tile != null) {
                                when (targeting.activeSpell.targetType) {
                                    de.egril.defender.model.SpellTargetType.POSITION ->
                                        onSelectSpellTarget?.invoke(tile)
                                    de.egril.defender.model.SpellTargetType.ENEMY -> {
                                        val enemy =
                                            gameState.attackers.find { it.position.value == tile && !it.isDefeated.value }
                                        if (enemy != null && targeting.validTargets.contains(enemy)) {
                                            onSelectSpellTarget?.invoke(enemy)
                                        }
                                    }
                                    de.egril.defender.model.SpellTargetType.TOWER -> {
                                        val tower = gameState.defenders.find { it.position.value == tile }
                                        if (tower != null && targeting.validTargets.contains(tower)) {
                                            onSelectSpellTarget?.invoke(tower)
                                        }
                                    }
                                    else -> {}
                                }
                                keyboardPlacementTile = null
                            }
                        }
                        // Always consume the key so it never falls through to tower placement / attack.
                        true
                    }
                    // 1-8: Select defender type by index.
                    // Keys 1-2 are intentionally always enabled for tower-type selection as well.
                    event.type == KeyEventType.KeyDown &&
                        event.key in setOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight) &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        (gameState.phase.value == GamePhase.INITIAL_BUILDING || gameState.phase.value == GamePhase.PLAYER_TURN) &&
                        !showMagicPanel -> {
                        val digit =
                            when (event.key) {
                                Key.One -> 1
                                Key.Two -> 2
                                Key.Three -> 3
                                Key.Four -> 4
                                Key.Five -> 5
                                Key.Six -> 6
                                Key.Seven -> 7
                                Key.Eight -> 8
                                else -> 0
                            }
                        if (digit > 0 && digit <= keyboardSelectableTowers.size) {
                            selectedDefenderType =
                                if (selectedDefenderType ==
                                    keyboardSelectableTowers[digit - 1]
                                ) {
                                    null
                                } else {
                                    keyboardSelectableTowers[digit - 1]
                                }
                            selectedAttackerId = null
                            selectedTargetId = null
                            selectedTargetPosition = null
                            // Leaving the support bar for a tower: drop the support focus cursor so
                            // Enter confirms tower placement rather than a focused support element.
                            supportFocusIndex = null
                            true
                        } else {
                            false
                        }
                    }
                    // 1: First special action for selected tower
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.One &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        selectedDefenderId != null &&
                        !splitSelectorExpanded &&
                        gameState.phase.value == GamePhase.PLAYER_TURN -> {
                        val defender = gameState.defenders.find { it.id == selectedDefenderId }
                        if (defender != null && defender.isReady && defender.actionsRemaining.value > 0) {
                            when (defender.type) {
                                DefenderType.DWARVEN_MINE -> {
                                    handleMineAction(defender.id, MineAction.DIG)
                                    true
                                }
                                DefenderType.WIZARD_TOWER -> {
                                    val isAtMaxMana = gameState.currentMana.value >= gameState.maxMana.value
                                    if (!isAtMaxMana) {
                                        handleWizardAction(defender.id, WizardAction.GENERATE_MANA)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                DefenderType.ALTAR -> onActivateAltar?.invoke(defender.id) == true
                                DefenderType.SPIKE_TOWER -> {
                                    val canBuild =
                                        defender.level.value >= 20 &&
                                            gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_2
                                    if (canBuild) {
                                        handleBarricadeAction(defender.id, BarricadeAction.BUILD_BARRICADE)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                DefenderType.SPEAR_TOWER -> {
                                    val canBuild =
                                        defender.level.value >= 10 &&
                                            gameState.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_1
                                    if (canBuild) {
                                        handleBarricadeAction(defender.id, BarricadeAction.BUILD_BARRICADE)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    // 2: Second special action for selected tower
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.Two &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        selectedDefenderId != null &&
                        !splitSelectorExpanded &&
                        gameState.phase.value == GamePhase.PLAYER_TURN -> {
                        val defender = gameState.defenders.find { it.id == selectedDefenderId }
                        if (defender != null && defender.isReady && defender.actionsRemaining.value > 0) {
                            when (defender.type) {
                                DefenderType.DWARVEN_MINE -> {
                                    handleMineAction(defender.id, MineAction.BUILD_TRAP)
                                    true
                                }
                                DefenderType.WIZARD_TOWER, DefenderType.ALTAR -> {
                                    if (defender.level.value >= 10) {
                                        handleWizardAction(defender.id, WizardAction.PLACE_MAGICAL_TRAP)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    // Escape (remappable): Cancel an active support-object placement or spell targeting
                    // first, so keyboard users can stop placing (e.g. the Bomb) without leaving the level.
                    // A second press then falls through to the back-to-world-map handler below.
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutBackToWorldMap.value) &&
                        !isDemoMode &&
                        (selectedSupportObject != null || selectedSupportFief != null || gameState.spellTargeting.value != null) -> {
                        when {
                            selectedSupportObject != null -> selectedSupportObject = null
                            selectedSupportFief != null -> selectedSupportFief = null
                            else -> onExitSpellTargeting?.invoke()
                        }
                        keyboardPlacementTile = null
                        true
                    }
                    // Escape (remappable): Back to world map with confirmation
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutBackToWorldMap.value) &&
                        !isDemoMode -> {
                        // Skip unsaved changes check if in initial building phase with no defenders placed
                        val hasSandboxMapEdits =
                            gameState.level.isSandbox &&
                                (gameState.sandboxPaintedTiles.isNotEmpty() || gameState.sandboxPaintedRiverTiles.isNotEmpty())
                        val isInitialWithNothingDone =
                            gameState.phase.value == GamePhase.INITIAL_BUILDING &&
                                gameState.defenders.isEmpty() &&
                                !hasSandboxMapEdits
                        if (!isInitialWithNothingDone && unsavedChangesEnabled && hasUnsavedChanges.invoke()) {
                            showUnsavedChangesDialog = true
                        } else {
                            onBackToMap()
                        }
                        true
                    }
                    // End turn / start battle (Ctrl+Enter by default)
                    event.type == KeyEventType.KeyDown &&
                        isShortcutBindingPressed(event, AppSettings.shortcutEndTurnStartBattle.value) -> {
                        when (gameState.phase.value) {
                            GamePhase.PLAYER_TURN -> {
                                when {
                                    highlightEndTurnButton -> {
                                        highlightEndTurnButton = false
                                        endPlayerTurnAction()
                                        true
                                    }
                                    gameState.hasDefendersWithUnusedActions() -> {
                                        showEndTurnConfirmation = true
                                        true
                                    }
                                    else -> {
                                        endPlayerTurnAction()
                                        true
                                    }
                                }
                            }
                            GamePhase.INITIAL_BUILDING -> {
                                onStartFirstPlayerTurn()
                                true
                            }
                            else -> false
                        }
                    }
                    // Enter (plain): Confirm tower type selection / attack with selected tower
                    // Always consume Enter in gameplay phases to prevent focused buttons from handling it
                    // (Start Battle / End Turn buttons should only be triggered via shortcutEndTurnStartBattle)
                    event.type == KeyEventType.KeyDown &&
                        event.key == Key.Enter &&
                        !event.isCtrlPressed &&
                        !event.isAltPressed &&
                        !event.isMetaPressed -> {
                        when (gameState.phase.value) {
                            GamePhase.INITIAL_BUILDING -> {
                                // In build phase, Enter confirms placement on keyboard-selected build tile (or first available)
                                val defType = selectedDefenderType
                                if (defType != null) {
                                    // Same order as the mouse: buildable tiles first, then PATH tiles with the surcharge.
                                    // The selected tile is tried first; each candidate goes through the normal placement check.
                                    val candidates =
                                        gameState
                                            .keyboardPlacementCandidates(defType)
                                            .sortedByDescending { it == keyboardSelectedBuildTile }
                                    if (candidates.any { pos -> onPlaceDefender(defType, pos) }) {
                                        if (!gameState.canPlaceDefender(defType)) {
                                            selectedDefenderType = null
                                        }
                                        keyboardSelectedBuildTile = null
                                    }
                                }
                                // Always consume Enter in build phase (prevent Start Battle button activation)
                                true
                            }
                            GamePhase.PLAYER_TURN -> {
                                when {
                                    // If a tower type is selected (buy mode), place on first available tile
                                    selectedDefenderType != null && selectedDefenderId == null && !showMagicPanel -> {
                                        val defType = selectedDefenderType
                                        if (defType != null) {
                                            // Buildable tiles first, then PATH tiles with the surcharge (see INITIAL_BUILDING above)
                                            val candidates =
                                                gameState
                                                    .keyboardPlacementCandidates(defType)
                                                    .sortedByDescending { it == keyboardSelectedBuildTile }
                                            if (candidates.any { pos -> onPlaceDefender(defType, pos) }) {
                                                if (!gameState.canPlaceDefender(defType)) {
                                                    selectedDefenderType = null
                                                }
                                                keyboardSelectedBuildTile = null
                                            }
                                        }
                                        true
                                    }
                                    // If a tower is selected with a valid target, attack
                                    selectedDefenderId != null && !showMagicPanel -> {
                                        val defenderId = selectedDefenderId
                                        val targetId = selectedTargetId
                                        val targetPos = selectedTargetPosition
                                        val defender = defenderId?.let { id -> gameState.defenders.find { it.id == id } }
                                        if (defender != null &&
                                            defender.isReady &&
                                            defender.actionsRemaining.value > 0 &&
                                            (targetId != null || targetPos != null)
                                        ) {
                                            when {
                                                targetId != null -> {
                                                    onDefenderAttack(defenderId, targetId)
                                                    true
                                                }
                                                targetPos != null -> {
                                                    onDefenderAttackPosition(defenderId, targetPos)
                                                    true
                                                }
                                                else -> true
                                            }
                                        } else {
                                            // Consume Enter anyway to prevent End Turn button activation
                                            true
                                        }
                                    }
                                    // Always consume Enter in player turn to prevent End Turn button activation
                                    else -> true
                                }
                            }
                            else -> false
                        }
                    }
                    else -> false
                }
            }
        }
    }
}
