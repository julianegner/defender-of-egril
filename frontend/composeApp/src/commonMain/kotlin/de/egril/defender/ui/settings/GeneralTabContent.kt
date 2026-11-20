package de.egril.defender.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.common.SelectableText
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

/**
 * General tab: Language, Difficulty, Dark mode, Check for updates, Debug options.
 */
@Composable
internal fun GeneralTabContent(
    onDismissSettings: () -> Unit,
    triggerRestore: Boolean = false,
    onRestoreHandled: () -> Unit = {},
    triggerOpenLanguage: Boolean = false,
    onOpenLanguageHandled: () -> Unit = {},
    triggerOpenDifficulty: Boolean = false,
    onOpenDifficultyHandled: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Language section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SelectableText(
                    text = stringResource(Res.string.language),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                de.egril.defender.ui.gameplay
                    .ShortcutKeyChip(text = "L")
            }
            LanguageChooser(
                modifier = Modifier.fillMaxWidth(),
                onLanguageChanged = { locale ->
                    AppSettings.saveLanguage(locale)
                },
                triggerOpen = triggerOpenLanguage,
                onTriggerOpenHandled = onOpenLanguageHandled,
            )
        }

        HorizontalDivider()

        // Difficulty section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SelectableText(
                    text = stringResource(Res.string.difficulty),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                de.egril.defender.ui.gameplay
                    .ShortcutKeyChip(text = "D")
            }
            DifficultyChooser(
                modifier = Modifier.fillMaxWidth(),
                onDifficultyChanged = { level ->
                    AppSettings.saveDifficulty(level)
                },
                triggerOpen = triggerOpenDifficulty,
                onTriggerOpenHandled = onOpenDifficultyHandled,
            )
            SelectableText(
                text = stringResource(Res.string.difficulty_info_current_level),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        HorizontalDivider()

        // Appearance section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableText(
                text = stringResource(Res.string.appearance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Dark mode switch
            NumberedSetting(1) {
                GenericSwitch(
                    state = AppSettings.isDarkMode,
                    checkedText = stringResource(Res.string.dark_mode),
                    uncheckedText = stringResource(Res.string.dark_mode),
                    onCheckedChange = { enabled ->
                        AppSettings.saveDarkMode(enabled)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Debug options switch
            NumberedSetting(2) {
                GenericSwitch(
                    state = AppSettings.showDebugOptions,
                    checkedText = stringResource(Res.string.debug_options),
                    uncheckedText = stringResource(Res.string.debug_options),
                    onCheckedChange = { enabled ->
                        AppSettings.saveShowDebugOptions(enabled)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Check for updates switch
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                NumberedSetting(3) {
                    GenericSwitch(
                        state = AppSettings.checkForUpdates,
                        checkedText = stringResource(Res.string.check_for_updates),
                        uncheckedText = stringResource(Res.string.check_for_updates),
                        onCheckedChange = { enabled ->
                            AppSettings.saveCheckForUpdates(enabled)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SelectableText(
                    text = stringResource(Res.string.check_for_updates_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        HorizontalDivider()

        // Restore game data section
        RestoreGameDataSection(onDismissSettings = onDismissSettings, triggerRestore = triggerRestore, onRestoreHandled = onRestoreHandled)
    }
}
