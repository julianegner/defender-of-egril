package de.egril.defender.ui.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.infopage.KeybindFocusManager
import de.egril.defender.ui.infopage.KeyboardShortcutsInfo
import de.egril.defender.ui.infopage.LocalKeybindFocusManager
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

@Composable
internal fun ShortcutBindingsTabContent(
    settingsScrollState: ScrollState = rememberScrollState(),
    keybindFocusManager: KeybindFocusManager? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NumberedSetting(1) {
            GenericSwitch(
                state = AppSettings.showButtonShortcutHints,
                checkedText = stringResource(Res.string.shortcut_bindings_show_on_buttons),
                uncheckedText = stringResource(Res.string.shortcut_bindings_show_on_buttons),
                onCheckedChange = { AppSettings.saveShowButtonShortcutHints(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SelectableText(
            text = stringResource(Res.string.shortcut_bindings_show_on_buttons_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (AppSettings.showButtonShortcutHints.value) {
            // Descriptive info box for keybind navigation
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.shortcut_bindings_nav_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                de.egril.defender.ui.gameplay
                    .ShortcutKeyChip(text = "\u2191\u2193")
                Text(
                    text = stringResource(Res.string.scroll),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Keep shortcuts content in a weighted container so KeyboardShortcutsInfo's internal
        // vertical scroll receives bounded height and avoids infinite-constraints crashes.
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CompositionLocalProvider(LocalKeybindFocusManager provides keybindFocusManager) {
                KeyboardShortcutsInfo(
                    enableBindingEdit = true,
                    showResetButton = true,
                    scrollState = settingsScrollState,
                )
            }
        }
    }
}
