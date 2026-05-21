package de.egril.defender.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.TooltipWrapper
import de.egril.defender.ui.gameplay.ShortcutKeyChip
import dev.vicart.compose.material.symbols.FilledSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.settings

/**
 * Settings button with gear icon that opens the settings dialog
 * Can be placed in any screen to provide access to settings
 */
@Composable
fun SettingsButton(
    modifier: Modifier = Modifier,
    initiallyOpen: Boolean = false,
    initialTab: SettingsTab = SettingsTab.GENERAL,
    onInitialOpenHandled: (() -> Unit)? = null,
    shortcutKey: String? = null
) {
    var showSettings by remember { mutableStateOf(false) }
    var autoOpenConsumed by remember { mutableStateOf(false) }
    val settingsLabel = stringResource(Res.string.settings)

    LaunchedEffect(initiallyOpen, autoOpenConsumed) {
        if (initiallyOpen && !autoOpenConsumed) {
            showSettings = true
            autoOpenConsumed = true
            onInitialOpenHandled?.invoke()
        }
    }

    TooltipWrapper(text = settingsLabel) {
        Box {
            IconButton(
                onClick = { showSettings = true },
                modifier = modifier.semantics { contentDescription = settingsLabel }
            ) {
                FilledSymbol(
                    icon = MaterialSymbols.SETTINGS,
                    size = 32.dp,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            if (shortcutKey != null && AppSettings.showButtonShortcutHints.value) {
                Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                    ShortcutKeyChip(text = shortcutKey)
                }
            }
        }
    }
    
    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            initialTab = initialTab
        )
    }
}
