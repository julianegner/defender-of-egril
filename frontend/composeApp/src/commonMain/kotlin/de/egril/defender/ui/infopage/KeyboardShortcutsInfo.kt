@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
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
import de.egril.defender.ui.icon.DownArrowIcon
import de.egril.defender.ui.icon.LeftArrowIcon
import de.egril.defender.ui.icon.RightArrowIcon
import de.egril.defender.ui.icon.UpArrowIcon
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer

private enum class BindingTarget {
    CENTER_SELECTED_TOWER,
    CENTER_NEXT_SPAWN
}

/**
 * Composable displaying keyboard shortcuts documentation
 */
@Composable
fun KeyboardShortcutsInfo(
    enableBindingEdit: Boolean = false,
    showResetButton: Boolean = false
) {
    val ctrl = stringResource(Res.string.keyboard_modifier_ctrl)
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
                ShortcutRow(key = "F", description = stringResource(Res.string.keyboard_shortcut_attack))
                ShortcutRow(key = "Tab", description = stringResource(Res.string.keyboard_shortcut_tab_next_tower))
                ShortcutRow(key = "Shift+Tab", description = stringResource(Res.string.keyboard_shortcut_shift_tab_prev_tower))
                if (enableBindingEdit) {
                    ShortcutBindingRow(
                        key = AppSettings.shortcutCenterSelectedTower.value,
                        description = centerSelectedTowerShortcutDescription,
                        onEdit = { bindingCaptureTarget = BindingTarget.CENTER_SELECTED_TOWER },
                        buttonTestTag = "shortcut-binding-center-selected"
                    )
                    ShortcutBindingRow(
                        key = AppSettings.shortcutCenterNextSpawnPoint.value,
                        description = stringResource(Res.string.keyboard_shortcut_center_next_spawn_point),
                        onEdit = { bindingCaptureTarget = BindingTarget.CENTER_NEXT_SPAWN },
                        buttonTestTag = "shortcut-binding-center-spawn"
                    )
                } else {
                    ShortcutRow(
                        key = AppSettings.shortcutCenterSelectedTower.value,
                        description = centerSelectedTowerShortcutDescription
                    )
                    ShortcutRow(
                        key = AppSettings.shortcutCenterNextSpawnPoint.value,
                        description = stringResource(Res.string.keyboard_shortcut_center_next_spawn_point)
                    )
                }
                ShortcutRow(key = "$ctrl+A", description = stringResource(Res.string.keyboard_shortcut_auto_attack))
                ShortcutRow(key = "C", description = stringResource(Res.string.keyboard_shortcut_cheat))
                ShortcutRow(key = "E", description = stringResource(Res.string.keyboard_shortcut_enemy_list))
                ShortcutRow(key = "Enter", description = stringResource(Res.string.keyboard_shortcut_end_turn))
                ShortcutRow(key = "$ctrl+S", description = stringResource(Res.string.keyboard_shortcut_save))
                ShortcutRow(
                    keyContent = {
                        Text(
                            text = "W / ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        UpArrowIcon(size = 14.dp)
                    },
                    description = stringResource(Res.string.keyboard_shortcut_pan_up)
                )
                ShortcutRow(
                    keyContent = {
                        Text(
                            text = "S / ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        DownArrowIcon(size = 14.dp)
                    },
                    description = stringResource(Res.string.keyboard_shortcut_pan_down)
                )
                ShortcutRow(
                    keyContent = {
                        Text(
                            text = "A / ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LeftArrowIcon(size = 14.dp)
                    },
                    description = stringResource(Res.string.keyboard_shortcut_pan_left)
                )
                ShortcutRow(
                    keyContent = {
                        Text(
                            text = "D / ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        RightArrowIcon(size = 14.dp)
                    },
                    description = stringResource(Res.string.keyboard_shortcut_pan_right)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // World map shortcuts
            ShortcutSection(title = stringResource(Res.string.keyboard_shortcuts_worldmap_section)) {
                ShortcutRow(key = "C", description = stringResource(Res.string.keyboard_shortcut_cheat))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Abilities screen shortcuts
            ShortcutSection(title = stringResource(Res.string.keyboard_shortcuts_abilities_section)) {
                ShortcutRow(key = "C", description = stringResource(Res.string.keyboard_shortcut_cheat))
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
        AlertDialog(
            onDismissRequest = { bindingCaptureTarget = null },
            title = { Text(stringResource(Res.string.shortcut_bindings_capture_title)) },
            text = {
                SelectionContainer {
                    Text(stringResource(Res.string.shortcut_bindings_capture_message))
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { bindingCaptureTarget = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            modifier = Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                if (event.key == Key.Escape) {
                    bindingCaptureTarget = null
                    return@onPreviewKeyEvent true
                }
                val binding = buildShortcutBindingFromEvent(event) ?: return@onPreviewKeyEvent true
                when (bindingCaptureTarget) {
                    BindingTarget.CENTER_SELECTED_TOWER -> AppSettings.saveShortcutCenterSelectedTower(binding)
                    BindingTarget.CENTER_NEXT_SPAWN -> AppSettings.saveShortcutCenterNextSpawnPoint(binding)
                    null -> {}
                }
                bindingCaptureTarget = null
                true
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
    description: String,
    onEdit: () -> Unit,
    buttonTestTag: String
) {
    ShortcutRow(
        keyContent = {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.width(100.dp).testTag(buttonTestTag)
            ) {
                Text(
                    text = key.replace('_', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        description = description
    )
}

@Composable
private fun ShortcutRow(keyContent: @Composable RowScope.() -> Unit, description: String) {
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
    }
}
