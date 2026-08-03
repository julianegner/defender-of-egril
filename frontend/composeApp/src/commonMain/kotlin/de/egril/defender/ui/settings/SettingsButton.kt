package de.egril.defender.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import de.egril.defender.audio.BackgroundMusic
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.settings
import dev.vicart.compose.material.symbols.FilledSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols

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
    shortcutKey: String? = null,
    triggerOpen: Boolean = false,
    onTriggerHandled: (() -> Unit)? = null,
    triggerOpenWithTab: SettingsTab? = null,
    onTriggerWithTabHandled: (() -> Unit)? = null,
    pageBackgroundMusic: BackgroundMusic? = null,
) {
    var showSettings by remember { mutableStateOf(false) }
    var autoOpenConsumed by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(initialTab) }
    val settingsLabel = stringResource(Res.string.settings)

    LaunchedEffect(initiallyOpen, autoOpenConsumed) {
        if (initiallyOpen && !autoOpenConsumed) {
            activeTab = initialTab
            showSettings = true
            autoOpenConsumed = true
            onInitialOpenHandled?.invoke()
        }
    }

    LaunchedEffect(triggerOpen) {
        if (triggerOpen) {
            activeTab = initialTab
            showSettings = true
            onTriggerHandled?.invoke()
        }
    }

    LaunchedEffect(triggerOpenWithTab) {
        if (triggerOpenWithTab != null) {
            activeTab = triggerOpenWithTab
            showSettings = true
            onTriggerWithTabHandled?.invoke()
        }
    }

    TooltipWrapper(text = settingsLabel) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    activeTab = initialTab
                    showSettings = true
                },
                modifier = modifier.semantics { contentDescription = settingsLabel },
            ) {
                FilledSymbol(
                    icon = MaterialSymbols.SETTINGS,
                    size = 32.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (shortcutKey != null && AppSettings.showButtonShortcutHints.value) {
                Spacer(modifier = Modifier.width(2.dp))
                ShortcutKeyChip(text = shortcutKey)
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            initialTab = activeTab,
            pageBackgroundMusic = pageBackgroundMusic,
        )
    }
}
