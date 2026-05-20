@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyperether.resources.AppLocale
import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.currentLanguage
import com.hyperether.resources.stringResource
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.buildShortcutBindingFromEvent
import de.egril.defender.ui.settings.formatShortcutBindingForDisplay
import de.egril.defender.ui.settings.isShortcutBindingChanged
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer

private enum class BindingTarget {
    ATTACK_SELECTED_TARGET,
    SELECT_NEXT_TOWER,
    SELECT_PREVIOUS_TOWER,
    AUTO_ATTACK_END_TURN,
    CHEAT,
    TOGGLE_ENEMY_LIST,
    END_TURN_START_BATTLE,
    SAVE_GAME,
    PAN_UP,
    PAN_DOWN,
    PAN_LEFT,
    PAN_RIGHT,
    CENTER_SELECTED_TOWER,
    CENTER_NEXT_SPAWN,
    UPGRADE_SELECTED_TOWER,
    UNDO_OR_SELL_SELECTED_TOWER,
    TOGGLE_SPELL_MENU,
    SWITCH_TO_TOWER_MODE
}

/**
 * Composable displaying keyboard shortcuts documentation
 */
@Composable
fun KeyboardShortcutsInfo(
    enableBindingEdit: Boolean = false,
    showResetButton: Boolean = false
) {
    val centerSelectedTowerShortcutDescription = remember(currentLanguage.value) {
        val localizedValue = LocalizedStrings.get(
            "keyboard_shortcut_center_selected_tower",
            currentLanguage.value
        )
        if (localizedValue == "???") {
            LocalizedStrings.get("keyboard_shortcut_center_selected_tower", AppLocale.DEFAULT)
        } else {
            localizedValue
        }
    }
    var bindingCaptureTarget by remember { mutableStateOf<BindingTarget?>(null) }

    SelectionContainer {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
        Text(
            text = stringResource(Res.string.keyboard_shortcuts_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(Res.string.keyboard_shortcuts_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Gameplay shortcuts
            ShortcutSection(title = stringResource(Res.string.keyboard_shortcuts_gameplay_section)) {
                ShortcutBindingRow(
                    key = AppSettings.shortcutAttackSelectedTarget.value,
                    defaultKey = "F",
                    description = stringResource(Res.string.keyboard_shortcut_attack),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.ATTACK_SELECTED_TARGET },
                    buttonTestTag = "shortcut-binding-attack-selected"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutSelectNextTower.value,
                    defaultKey = "Tab",
                    description = stringResource(Res.string.keyboard_shortcut_tab_next_tower),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.SELECT_NEXT_TOWER },
                    buttonTestTag = "shortcut-binding-next-tower"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutSelectPreviousTower.value,
                    defaultKey = "Shift+Tab",
                    description = stringResource(Res.string.keyboard_shortcut_shift_tab_prev_tower),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.SELECT_PREVIOUS_TOWER },
                    buttonTestTag = "shortcut-binding-previous-tower"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutCenterSelectedTower.value,
                    defaultKey = "R",
                    description = centerSelectedTowerShortcutDescription,
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.CENTER_SELECTED_TOWER },
                    buttonTestTag = "shortcut-binding-center-selected"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutCenterNextSpawnPoint.value,
                    defaultKey = "G",
                    description = stringResource(Res.string.keyboard_shortcut_center_next_spawn_point),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.CENTER_NEXT_SPAWN },
                    buttonTestTag = "shortcut-binding-center-spawn"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutUpgradeSelectedTower.value,
                    defaultKey = "U",
                    description = stringResource(Res.string.keyboard_shortcut_upgrade_selected_tower),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.UPGRADE_SELECTED_TOWER },
                    buttonTestTag = "shortcut-binding-upgrade-selected-tower"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutUndoOrSellSelectedTower.value,
                    defaultKey = "X",
                    description = stringResource(Res.string.keyboard_shortcut_undo_or_sell_selected_tower),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.UNDO_OR_SELL_SELECTED_TOWER },
                    buttonTestTag = "shortcut-binding-undo-or-sell-selected-tower"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutToggleSpellMenu.value,
                    defaultKey = "M",
                    description = stringResource(Res.string.keyboard_shortcut_toggle_spell_menu),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.TOGGLE_SPELL_MENU },
                    buttonTestTag = "shortcut-binding-toggle-spell-menu"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutSwitchToTowerMode.value,
                    defaultKey = "T",
                    description = stringResource(Res.string.keyboard_shortcut_switch_to_tower_mode),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.SWITCH_TO_TOWER_MODE },
                    buttonTestTag = "shortcut-binding-switch-to-tower-mode"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutAutoAttackEndTurn.value,
                    defaultKey = "Ctrl+A",
                    description = stringResource(Res.string.keyboard_shortcut_auto_attack),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.AUTO_ATTACK_END_TURN },
                    buttonTestTag = "shortcut-binding-auto-attack"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutCheat.value,
                    defaultKey = "C",
                    description = stringResource(Res.string.keyboard_shortcut_cheat),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.CHEAT },
                    buttonTestTag = "shortcut-binding-cheat"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutToggleEnemyList.value,
                    defaultKey = "E",
                    description = stringResource(Res.string.keyboard_shortcut_enemy_list),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.TOGGLE_ENEMY_LIST },
                    buttonTestTag = "shortcut-binding-toggle-enemy-list"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutEndTurnStartBattle.value,
                    defaultKey = "Enter",
                    description = stringResource(Res.string.keyboard_shortcut_end_turn),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.END_TURN_START_BATTLE },
                    buttonTestTag = "shortcut-binding-end-turn"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutSaveGame.value,
                    defaultKey = "Ctrl+S",
                    description = stringResource(Res.string.keyboard_shortcut_save),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.SAVE_GAME },
                    buttonTestTag = "shortcut-binding-save-game"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutPanUp.value,
                    defaultKey = "W",
                    description = stringResource(Res.string.keyboard_shortcut_pan_up),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.PAN_UP },
                    buttonTestTag = "shortcut-binding-pan-up"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutPanDown.value,
                    defaultKey = "S",
                    description = stringResource(Res.string.keyboard_shortcut_pan_down),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.PAN_DOWN },
                    buttonTestTag = "shortcut-binding-pan-down"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutPanLeft.value,
                    defaultKey = "A",
                    description = stringResource(Res.string.keyboard_shortcut_pan_left),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.PAN_LEFT },
                    buttonTestTag = "shortcut-binding-pan-left"
                )
                ShortcutBindingRow(
                    key = AppSettings.shortcutPanRight.value,
                    defaultKey = "D",
                    description = stringResource(Res.string.keyboard_shortcut_pan_right),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.PAN_RIGHT },
                    buttonTestTag = "shortcut-binding-pan-right"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // World map shortcuts
            ShortcutSection(title = stringResource(Res.string.keyboard_shortcuts_worldmap_section)) {
                ShortcutBindingRow(
                    key = AppSettings.shortcutCheat.value,
                    defaultKey = "C",
                    description = stringResource(Res.string.keyboard_shortcut_cheat),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.CHEAT },
                    buttonTestTag = "shortcut-binding-cheat-worldmap"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Abilities screen shortcuts
            ShortcutSection(title = stringResource(Res.string.keyboard_shortcuts_abilities_section)) {
                ShortcutBindingRow(
                    key = AppSettings.shortcutCheat.value,
                    defaultKey = "C",
                    description = stringResource(Res.string.keyboard_shortcut_cheat),
                    enableEdit = enableBindingEdit,
                    onEdit = { bindingCaptureTarget = BindingTarget.CHEAT },
                    buttonTestTag = "shortcut-binding-cheat-abilities"
                )
            }

            if (enableBindingEdit && showResetButton) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { AppSettings.resetShortcutBindings() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.shortcut_bindings_reset_all))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
        }
    }

    if (bindingCaptureTarget != null) {
        val captureFocusRequester = remember { FocusRequester() }
        val handleCaptureEvent: (KeyEvent) -> Boolean = capture@{ event ->
            if (event.type != KeyEventType.KeyDown) {
                return@capture false
            }
            if (event.key == Key.Escape) {
                bindingCaptureTarget = null
                return@capture true
            }
            val binding = buildShortcutBindingFromEvent(event) ?: return@capture true
            when (bindingCaptureTarget) {
                BindingTarget.ATTACK_SELECTED_TARGET -> AppSettings.saveShortcutAttackSelectedTarget(binding)
                BindingTarget.SELECT_NEXT_TOWER -> AppSettings.saveShortcutSelectNextTower(binding)
                BindingTarget.SELECT_PREVIOUS_TOWER -> AppSettings.saveShortcutSelectPreviousTower(binding)
                BindingTarget.AUTO_ATTACK_END_TURN -> AppSettings.saveShortcutAutoAttackEndTurn(binding)
                BindingTarget.CHEAT -> AppSettings.saveShortcutCheat(binding)
                BindingTarget.TOGGLE_ENEMY_LIST -> AppSettings.saveShortcutToggleEnemyList(binding)
                BindingTarget.END_TURN_START_BATTLE -> AppSettings.saveShortcutEndTurnStartBattle(binding)
                BindingTarget.SAVE_GAME -> AppSettings.saveShortcutSaveGame(binding)
                BindingTarget.PAN_UP -> AppSettings.saveShortcutPanUp(binding)
                BindingTarget.PAN_DOWN -> AppSettings.saveShortcutPanDown(binding)
                BindingTarget.PAN_LEFT -> AppSettings.saveShortcutPanLeft(binding)
                BindingTarget.PAN_RIGHT -> AppSettings.saveShortcutPanRight(binding)
                BindingTarget.CENTER_SELECTED_TOWER -> AppSettings.saveShortcutCenterSelectedTower(binding)
                BindingTarget.CENTER_NEXT_SPAWN -> AppSettings.saveShortcutCenterNextSpawnPoint(binding)
                BindingTarget.UPGRADE_SELECTED_TOWER -> AppSettings.saveShortcutUpgradeSelectedTower(binding)
                BindingTarget.UNDO_OR_SELL_SELECTED_TOWER -> AppSettings.saveShortcutUndoOrSellSelectedTower(binding)
                BindingTarget.TOGGLE_SPELL_MENU -> AppSettings.saveShortcutToggleSpellMenu(binding)
                BindingTarget.SWITCH_TO_TOWER_MODE -> AppSettings.saveShortcutSwitchToTowerMode(binding)
                null -> {}
            }
            bindingCaptureTarget = null
            true
        }
        LaunchedEffect(bindingCaptureTarget) {
            captureFocusRequester.requestFocus()
        }
        AlertDialog(
            onDismissRequest = { bindingCaptureTarget = null },
            title = { Text(stringResource(Res.string.shortcut_bindings_capture_title)) },
            text = {
                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .testTag("shortcut-capture-target")
                            .focusRequester(captureFocusRequester)
                            .focusable()
                            .onPreviewKeyEvent(handleCaptureEvent)
                    ) {
                        Text(stringResource(Res.string.shortcut_bindings_capture_message))
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { bindingCaptureTarget = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ShortcutSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Header row
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.keyboard_shortcut_key_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(100.dp)
            )
            Text(
                text = stringResource(Res.string.keyboard_shortcut_description_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        content()
    }
}

@Composable
private fun ShortcutRow(key: String, description: String) {
    ShortcutRow(
        keyContent = {
            Text(
                text = key,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(100.dp)
            )
        },
        description = description
    )
}

@Composable
private fun ShortcutBindingRow(
    key: String,
    defaultKey: String,
    description: String,
    enableEdit: Boolean,
    onEdit: () -> Unit,
    buttonTestTag: String
) {
    val isChanged = remember(key, defaultKey) {
        isShortcutBindingChanged(key, defaultKey)
    }
    ShortcutRow(
        keyContent = {
            if (enableEdit) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.width(100.dp).testTag(buttonTestTag),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = formatShortcutBindingForDisplay(key),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = formatShortcutBindingForDisplay(key),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(100.dp)
                )
            }
        },
        description = description,
        marker = if (isChanged) stringResource(Res.string.shortcut_binding_changed_marker) else null
    )
}

@Composable
private fun ShortcutRow(
    keyContent: @Composable RowScope.() -> Unit,
    description: String,
    marker: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.width(100.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = keyContent
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (marker != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = marker,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
