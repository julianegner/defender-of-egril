@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.infopage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Composable displaying keyboard shortcuts documentation
 */
@Composable
fun KeyboardShortcutsInfo() {
    val ctrl = stringResource(Res.string.keyboard_modifier_ctrl)
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
                ShortcutRow(key = "$ctrl+A", description = stringResource(Res.string.keyboard_shortcut_auto_attack))
                ShortcutRow(key = "C", description = stringResource(Res.string.keyboard_shortcut_cheat))
                ShortcutRow(key = "E", description = stringResource(Res.string.keyboard_shortcut_enemy_list))
                ShortcutRow(key = "Enter", description = stringResource(Res.string.keyboard_shortcut_end_turn))
                ShortcutRow(key = "$ctrl+S", description = stringResource(Res.string.keyboard_shortcut_save))
                ShortcutRow(key = "W / ↑", description = stringResource(Res.string.keyboard_shortcut_pan_up))
                ShortcutRow(key = "S / ↓", description = stringResource(Res.string.keyboard_shortcut_pan_down))
                ShortcutRow(key = "A / ←", description = stringResource(Res.string.keyboard_shortcut_pan_left))
                ShortcutRow(key = "D / →", description = stringResource(Res.string.keyboard_shortcut_pan_right))
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

            Spacer(modifier = Modifier.height(16.dp))
        }
        }
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
