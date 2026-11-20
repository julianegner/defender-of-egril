package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.AutoAttackAvailability
import de.egril.defender.model.DefenderType
import de.egril.defender.model.DigOutcome
import de.egril.defender.model.GameMessage
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.SpellType
import de.egril.defender.ui.CheatCodeDialog
import de.egril.defender.ui.ReminderMessage
import de.egril.defender.ui.editor.ConfirmationDialog
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.formatShortcutBindingForDisplay
import defender_of_egril.composeapp.generated.resources.*

@Composable
internal fun GamePlayDialogs(
    gameState: GameState,
    windowSize: String,
    hasUnsavedChanges: (() -> Boolean)?,
    onSaveGame: ((String?) -> String?)?,
    onCheatCode: ((String) -> Boolean)?,
    onSandboxSpawnEnemy: ((AttackerType, Int, Position?) -> Unit)?,
    onSandboxAddCoins: (() -> Unit)?,
    onUndoTower: (Int) -> Boolean,
    onSellTower: (Int) -> Boolean,
    onRemoveBarricade: ((Position) -> Int)?,
    onBackToMap: () -> Unit,
    onWinLevelNow: (() -> Unit)?,
    onOpenMagicPanel: (() -> Unit)?,
    onCancelInstantTowerSpell: (() -> Unit)?,
    onClearPlatformInfo: (() -> Unit)?,
    onClearCheatHelp: (() -> Unit)?,
    onClearSpecialActionsWarning: (() -> Unit)?,
    onClearReminderMessage: (() -> Unit)?,
    onConfirmTargetSpell: (() -> Unit)?,
    onDismissTargetConfirmation: (() -> Unit)?,
    onDismissFreezeWarning: (() -> Unit)?,
    onDismissGameMessage: (() -> Unit)?,
    onStopDemoMode: (() -> Unit)?,
    endPlayerTurnAction: () -> Unit,
    autoAttackAndEndTurnAction: () -> Unit,
    showPlatformInfo: Boolean,
    showCheatHelp: Boolean,
    specialActionsRemaining: List<DefenderType>,
    reminderMessage: ReminderMessage?,
    showSpellTargetConfirmation: Pair<SpellType, Any>?,
    showFreezeImmuneWarning: Attacker?,
    pendingGameMessage: GameMessage?,
    activeSpellToken: SpellType?,
    currentDigOutcome: DigOutcome?,
    currentDragonName: String?,
    showDigOutcomeDialog: Boolean,
    onShowDigOutcomeDialogChange: (Boolean) -> Unit,
    onSelectedDefenderTypeChange: (DefenderType?) -> Unit,
    onSelectedDefenderIdChange: (Int?) -> Unit,
    showAbortInstantTowerDialog: Boolean,
    onShowAbortInstantTowerDialogChange: (Boolean) -> Unit,
    showSaveDialog: Boolean,
    onShowSaveDialogChange: (Boolean) -> Unit,
    saveCommentInput: String,
    onSaveCommentInputChange: (String) -> Unit,
    showSaveConfirmation: Boolean,
    onShowSaveConfirmationChange: (Boolean) -> Unit,
    showCheatDialog: Boolean,
    onShowCheatDialogChange: (Boolean) -> Unit,
    cheatCodeInput: String,
    onCheatCodeInputChange: (String) -> Unit,
    showSandboxTools: Boolean,
    onShowSandboxToolsChange: (Boolean) -> Unit,
    showRemoveBarricadeDialog: Boolean,
    onShowRemoveBarricadeDialogChange: (Boolean) -> Unit,
    barricadeToRemove: Position?,
    onBarricadeToRemoveChange: (Position?) -> Unit,
    showRemoveTrapDialog: Boolean,
    onShowRemoveTrapDialogChange: (Boolean) -> Unit,
    trapToRemove: Position?,
    onTrapToRemoveChange: (Position?) -> Unit,
    keyboardUndoOrSellConfirmation: Pair<Int, Boolean>?,
    onKeyboardUndoOrSellConfirmationChange: (Pair<Int, Boolean>?) -> Unit,
    showUnsavedChangesDialog: Boolean,
    onShowUnsavedChangesDialogChange: (Boolean) -> Unit,
    showEndTurnConfirmation: Boolean,
    onShowEndTurnConfirmationChange: (Boolean) -> Unit,
    showStopDemoDialog: Boolean,
    onShowStopDemoDialogChange: (Boolean) -> Unit,
) {
    if (showDigOutcomeDialog && currentDigOutcome != null) {
        DigOutcomeDialog(
            outcome = currentDigOutcome!!,
            onDismiss = { onShowDigOutcomeDialogChange(false) },
            onResetSelections = {
                onSelectedDefenderTypeChange(null)
                onSelectedDefenderIdChange(null)
            },
            dragonName = currentDragonName,
        )
    }

    // Abort Instant Tower spell dialog
    if (showAbortInstantTowerDialog) {
        AbortInstantTowerSpellDialog(
            onAbort = {
                onShowAbortInstantTowerDialogChange(false)
                onCancelInstantTowerSpell?.invoke()
                onOpenMagicPanel?.invoke()
            },
            onContinue = { onShowAbortInstantTowerDialogChange(false) },
        )
    }

    // Save game dialog (with optional comment input)
    if (showSaveDialog && onSaveGame != null) {
        SaveGameDialog(
            saveCommentInput = saveCommentInput,
            onSaveCommentChange = { onSaveCommentInputChange(it) },
            onSave = { comment ->
                onSaveGame(comment)
                onShowSaveDialogChange(false)
                onSaveCommentInputChange("")
                onShowSaveConfirmationChange(true)
            },
            onDismiss = {
                onShowSaveDialogChange(false)
                onSaveCommentInputChange("")
            },
        )
    }

    // Save confirmation dialog
    if (showSaveConfirmation) {
        SaveConfirmationDialog(
            onDismiss = { onShowSaveConfirmationChange(false) },
        )
    }

    // Cheat code dialog
    if (showCheatDialog && onCheatCode != null) {
        CheatCodeDialog(
            onDismiss = {
                onShowCheatDialogChange(false)
                onCheatCodeInputChange("")
            },
            onApplyCheatCode = onCheatCode,
            showHints = true,
            initialInput = cheatCodeInput,
            onInputChange = { onCheatCodeInputChange(it) },
        )
    }

    if (showSandboxTools && onSandboxSpawnEnemy != null) {
        SandboxToolsDialog(
            spawnPoints = gameState.level.startPositions,
            onSpawnEnemy = { type, level, spawnPoint -> onSandboxSpawnEnemy(type, level, spawnPoint) },
            onAddCoins = { onSandboxAddCoins?.invoke() },
            onDismiss = { onShowSandboxToolsChange(false) },
        )
    }

    // Platform info dialog (from platform cheat code)
    if (showPlatformInfo && onClearPlatformInfo != null) {
        de.egril.defender.ui.PlatformInfoDialog(
            platformInfo =
                de.egril.defender.utils
                    .getPlatform()
                    .name,
            windowSize = windowSize,
            onDismiss = onClearPlatformInfo,
        )
    }

    // Cheat code help screen (from cheat/cheats/help cheat code)
    if (showCheatHelp && onClearCheatHelp != null) {
        de.egril.defender.ui.CheatCodeHelpScreen(
            onDismiss = onClearCheatHelp,
            isInGameplay = true,
        )
    }

    // Remove barricade confirmation dialog
    // Note: This dialog only shows for barricades without towers
    if (showRemoveBarricadeDialog && barricadeToRemove != null) {
        ConfirmationDialog(
            title = stringResource(Res.string.remove_barricade_title),
            message = stringResource(Res.string.remove_barricade_message),
            onConfirm = {
                val actualRefund = onRemoveBarricade?.invoke(barricadeToRemove!!) ?: 0
                if (actualRefund > 0) {
                    // Add coins back to player (should be 0 for barricades without towers)
                    gameState.coins.value += actualRefund
                }
                onShowRemoveBarricadeDialogChange(false)
                onBarricadeToRemoveChange(null)
            },
            onDismiss = {
                onShowRemoveBarricadeDialogChange(false)
                onBarricadeToRemoveChange(null)
            },
        )
    }

    // Remove trap confirmation dialog
    if (showRemoveTrapDialog && trapToRemove != null) {
        ConfirmationDialog(
            title = stringResource(Res.string.remove_trap_title),
            message = stringResource(Res.string.remove_trap_message),
            onConfirm = {
                // Remove trap from game state
                gameState.traps.removeAll { it.position == trapToRemove }
                onShowRemoveTrapDialogChange(false)
                onTrapToRemoveChange(null)
            },
            onDismiss = {
                onShowRemoveTrapDialogChange(false)
                onTrapToRemoveChange(null)
            },
        )
    }

    val keyboardConfirmation = keyboardUndoOrSellConfirmation
    if (keyboardConfirmation != null) {
        val (confirmDefenderId, isUndo) = keyboardConfirmation
        val defender = gameState.defenders.find { it.id == confirmDefenderId }
        if (defender != null) {
            val locale = com.hyperether.resources.currentLanguage.value
            val towerName = defender.type.getLocalizedName(locale)
            val coinsLabel = stringResource(Res.string.coins_label)
            val refundAmount = if (isUndo) defender.totalCost else (defender.totalCost * 0.75).toInt()
            val titleStr =
                if (isUndo) {
                    stringResource(
                        Res.string.undo_tower_title,
                    )
                } else {
                    stringResource(Res.string.sell_tower_title)
                }
            val messageStr =
                if (isUndo) {
                    stringResource(Res.string.undo_tower_message, towerName, refundAmount.toString(), coinsLabel)
                } else {
                    stringResource(Res.string.sell_tower_message, towerName, refundAmount.toString(), coinsLabel)
                }
            val confirmLabel = if (isUndo) stringResource(Res.string.undo) else stringResource(Res.string.sell)
            val confirmColor = if (isUndo) GamePlayColors.Success else GamePlayColors.Warning
            AlertDialog(
                onDismissRequest = { onKeyboardUndoOrSellConfirmationChange(null) },
                title = { Text(titleStr) },
                text = { Text(messageStr) },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isUndo) {
                                onUndoTower(defender.id)
                            } else if (onSellTower(defender.id)) {
                                onSelectedDefenderTypeChange(null)
                                onSelectedDefenderIdChange(null)
                            }
                            onKeyboardUndoOrSellConfirmationChange(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(confirmLabel)
                            ShortcutKeyChip(
                                text = formatShortcutBindingForDisplay(AppSettings.shortcutUndoOrSellSelectedTower.value),
                            )
                        }
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { onKeyboardUndoOrSellConfirmationChange(null) },
                    ) {
                        Text(stringResource(Res.string.cancel))
                    }
                },
            )
        } else {
            onKeyboardUndoOrSellConfirmationChange(null)
        }
    }

    val unsavedChangesEnabled = hasUnsavedChanges != null && onSaveGame != null
    // Unsaved changes dialog
    if (showUnsavedChangesDialog && unsavedChangesEnabled) {
        UnsavedChangesDialog(
            onSaveAndExit = {
                // Save the game first
                onSaveGame(null)
                onShowUnsavedChangesDialogChange(false)
                // Then navigate back to map
                onBackToMap()
            },
            onDiscardChanges = {
                onShowUnsavedChangesDialogChange(false)
                // Navigate back without saving
                onBackToMap()
            },
            onCancel = {
                // Just close the dialog and stay in the game
                onShowUnsavedChangesDialogChange(false)
            },
        )
    }

    // End turn confirmation dialog
    if (showEndTurnConfirmation) {
        val canWinLevelNow = gameState.canWinLevelNow()
        val autoAttackAvailability =
            if (gameState.level.allowAutoAttack) {
                gameState.getAutoAttackAvailability()
            } else {
                AutoAttackAvailability.NONE
            }
        val autoAttackButtonText =
            if (autoAttackAvailability == AutoAttackAvailability.MANA_ONLY) {
                stringResource(Res.string.generate_mana_and_end_turn)
            } else {
                stringResource(Res.string.auto_attack_and_end_turn)
            }
        EndTurnConfirmationDialog(
            onConfirm = {
                onShowEndTurnConfirmationChange(false)
                endPlayerTurnAction()
                // Track tutorial progress
                if (gameState.tutorialState.value.isActive &&
                    !gameState.tutorialState.value.hasStartedFirstTurn
                ) {
                    gameState.tutorialState.value = gameState.tutorialState.value.markTurnStarted()
                }
            },
            onAutoAttackAndConfirm = {
                onShowEndTurnConfirmationChange(false)
                autoAttackAndEndTurnAction()
                // Track tutorial progress
                if (gameState.tutorialState.value.isActive &&
                    !gameState.tutorialState.value.hasStartedFirstTurn
                ) {
                    gameState.tutorialState.value = gameState.tutorialState.value.markTurnStarted()
                }
            },
            onCancel = {
                onShowEndTurnConfirmationChange(false)
            },
            showAutoAttackButton = autoAttackAvailability != AutoAttackAvailability.NONE,
            autoAttackButtonText = autoAttackButtonText,
            showEndTurnWarning = gameState.hasDefendersWithUnusedActions(),
            showWinLevelNow = canWinLevelNow && onWinLevelNow != null,
            onWinLevelNow = {
                onShowEndTurnConfirmationChange(false)
                onWinLevelNow?.invoke()
            },
        )
    }

    // Special actions remaining dialog
    if (specialActionsRemaining.isNotEmpty()) {
        SpecialActionsRemainingDialog(
            remainingTypes = specialActionsRemaining,
            onContinueTurn = {
                onClearSpecialActionsWarning?.invoke()
            },
        )
    }

    // Time reminder dialog
    reminderMessage?.let { reminder ->
        val elapsedTime =
            reminder.elapsedMs?.let { elapsedMs ->
                val hours = elapsedMs / (60 * 60 * 1000)
                val minutes = (elapsedMs % (60 * 60 * 1000)) / (60 * 1000)
                buildString {
                    if (hours > 0) {
                        val hourStr =
                            if (hours == 1L) {
                                stringResource(Res.string.hour, hours.toInt())
                            } else {
                                stringResource(Res.string.hours, hours.toInt())
                            }
                        append(hourStr)
                    }
                    if (minutes > 0) {
                        if (hours > 0) append(" ")
                        val minuteStr =
                            if (minutes == 1L) {
                                stringResource(Res.string.minute, minutes.toInt())
                            } else {
                                stringResource(Res.string.minutes, minutes.toInt())
                            }
                        append(minuteStr)
                    }
                    if (hours == 0L && minutes == 0L) {
                        append(stringResource(Res.string.minutes, 0))
                    }
                }
            }
        ReminderDialog(
            type = reminder.type,
            elapsedTime = elapsedTime,
            timeDescription =
                when (reminder.timeDescription) {
                    "close_to_midnight" -> stringResource(Res.string.time_for_sleep_close_to_midnight)
                    "midnight" -> stringResource(Res.string.time_for_sleep_midnight)
                    "after_midnight" -> stringResource(Res.string.time_for_sleep_after_midnight)
                    else -> null
                },
            onDismiss = {
                onClearReminderMessage?.invoke()
            },
        )
    }

    // Post-target spell confirmation dialog (shows after target is selected)
    if (showSpellTargetConfirmation != null && onConfirmTargetSpell != null && onDismissTargetConfirmation != null) {
        val (spell, target) = showSpellTargetConfirmation
        SpellTargetConfirmationDialog(
            spell = spell,
            target = target,
            currentMana = gameState.currentMana.value,
            onConfirm = { onConfirmTargetSpell.invoke() },
            onDismiss = { onDismissTargetConfirmation.invoke() },
            isTokenCast = activeSpellToken == spell,
        )
    }

    // Freeze immune warning dialog
    if (showFreezeImmuneWarning != null && onDismissFreezeWarning != null) {
        FreezeImmuneWarningDialog(
            enemy = showFreezeImmuneWarning,
            onDismiss = { onDismissFreezeWarning.invoke() },
        )
    }

    // In-game event message dialog (target captured, gate destroyed, or Ewhad narrative events)
    GameMessageDialogs(pendingGameMessage = pendingGameMessage, onDismissGameMessage = onDismissGameMessage, supports = gameState.level.supports)

    // Stop demo mode confirmation dialog
    if (showStopDemoDialog && onStopDemoMode != null) {
        AlertDialog(
            onDismissRequest = { onShowStopDemoDialogChange(false) },
            title = { Text(stringResource(Res.string.stop_demo_title)) },
            text = { Text(stringResource(Res.string.stop_demo_message)) },
            confirmButton = {
                Button(onClick = {
                    onShowStopDemoDialogChange(false)
                    onStopDemoMode.invoke()
                }) {
                    Text(stringResource(Res.string.yes))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onShowStopDemoDialogChange(false) }) {
                    Text(stringResource(Res.string.no))
                }
            },
        )
    }
}
