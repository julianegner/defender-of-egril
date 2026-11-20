@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyperether.resources.stringResource
import de.egril.defender.ui.a11y.a11ySemantics
import de.egril.defender.ui.common.ScrollableTabRowWithHints
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.infopage.KeybindFocusManager
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import dev.vicart.compose.material.symbols.MaterialSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import kotlinx.coroutines.launch

/**
 * Settings dialog that provides access to app settings like language selection and dark mode.
 * Settings are organized into tabs: General, Worldmap, Level, Sound, Accessibility, Mouse Pointers, Shortcuts.
 */
enum class SettingsTab {
    GENERAL,
    WORLD_MAP,
    LEVEL,
    SOUND,
    ACCESSIBILITY,
    MOUSE_POINTERS,
    SHORTCUTS,
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    initialTab: SettingsTab = SettingsTab.GENERAL,
    pageBackgroundMusic: de.egril.defender.audio.BackgroundMusic? = null,
) {
    val tabCount = SettingsTab.entries.size
    var selectedTabIndex by remember(initialTab) {
        mutableStateOf(
            SettingsTab.entries.indexOf(initialTab).coerceAtLeast(0),
        )
    }
    var shouldRestoreBackgroundMusicOnDismiss by remember { mutableStateOf(false) }
    var selectedMousePointerSliderIndex by remember { mutableStateOf(0) }

    fun dismissSettingsDialog() {
        if (shouldRestoreBackgroundMusicOnDismiss) {
            if (!AppSettings.isSoundEnabled.value || !AppSettings.isMusicEnabled.value) {
                de.egril.defender.audio.GlobalBackgroundMusicManager
                    .stopMusic()
            } else {
                val currentMusic =
                    de.egril.defender.audio.GlobalBackgroundMusicManager
                        .getCurrentMusic()
                if (pageBackgroundMusic == null) {
                    if (currentMusic != null) {
                        de.egril.defender.audio.GlobalBackgroundMusicManager
                            .stopMusic()
                    }
                } else if (currentMusic != pageBackgroundMusic) {
                    de.egril.defender.audio.GlobalBackgroundMusicManager
                        .stopMusic()
                    de.egril.defender.audio.GlobalBackgroundMusicManager
                        .playMusic(pageBackgroundMusic, loop = true)
                }
            }
        }
        onDismiss()
    }

    Dialog(onDismissRequest = ::dismissSettingsDialog) {
        val focusRequester = remember { FocusRequester() }
        val scope = rememberCoroutineScope()
        val settingsScrollState = rememberScrollState()
        // State triggers for keyboard shortcuts that need composable-level state
        var triggerRestoreData by remember { mutableStateOf(false) }
        var triggerShowSoundDetails by remember { mutableStateOf(false) }
        var triggerOpenLanguage by remember { mutableStateOf(false) }
        var triggerOpenDifficulty by remember { mutableStateOf(false) }
        // Track which volume slider is selected for +/- adjustment in Sound tab
        var selectedVolumeIndex by remember { mutableStateOf(0) } // 0=master, 1=effects, 2=worldmap, 3=gameplay
        // Track which slider is selected for +/- adjustment in Accessibility tab
        var selectedA11ySliderIndex by remember { mutableStateOf(0) } // 0=font size, 1=header text size
        // Focus manager for keybind entries on Shortcuts tab
        val keybindFocusManager = remember { KeybindFocusManager() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        // Reset scroll when tab changes, and re-request focus to ensure arrow keys work
        LaunchedEffect(selectedTabIndex) {
            settingsScrollState.scrollTo(0)
            try {
                focusRequester.requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
        Surface(
            modifier =
                Modifier
                    .widthIn(min = 300.dp, max = 500.dp)
                    .fillMaxHeight(fraction = 0.9f)
                    .heightIn(max = 680.dp)
                    .focusRequester(focusRequester)
                    .focusTarget()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.Back, Key.Escape -> {
                                    dismissSettingsDialog()
                                    true
                                }
                                Key.DirectionRight -> {
                                    selectedTabIndex = (selectedTabIndex + 1).coerceAtMost(tabCount - 1)
                                    true
                                }
                                Key.DirectionLeft -> {
                                    selectedTabIndex = (selectedTabIndex - 1).coerceAtLeast(0)
                                    true
                                }
                                Key.DirectionUp -> {
                                    scope.launch { settingsScrollState.animateScrollTo((settingsScrollState.value - 100).coerceAtLeast(0)) }
                                    true
                                }
                                Key.DirectionDown -> {
                                    scope.launch {
                                        settingsScrollState.animateScrollTo(
                                            (settingsScrollState.value + 100).coerceAtMost(settingsScrollState.maxValue),
                                        )
                                    }
                                    true
                                }
                                Key.Tab -> {
                                    val currentTab = SettingsTab.entries.getOrNull(selectedTabIndex) ?: SettingsTab.GENERAL
                                    if (currentTab == SettingsTab.SHORTCUTS) {
                                        // On Shortcuts tab, cycle focus through keybind buttons only
                                        if (event.isShiftPressed) {
                                            keybindFocusManager.focusPrevious()
                                        } else {
                                            keybindFocusManager.focusNext()
                                        }
                                        true
                                    } else {
                                        // Allow Tab to navigate between settings items within the current tab
                                        false
                                    }
                                }
                                else -> {
                                    // Number key shortcuts for toggling settings in current tab
                                    val number: Int? =
                                        when (event.key) {
                                            Key.Zero -> 0
                                            Key.One -> 1
                                            Key.Two -> 2
                                            Key.Three -> 3
                                            Key.Four -> 4
                                            Key.Five -> 5
                                            Key.Six -> 6
                                            Key.Seven -> 7
                                            Key.Eight -> 8
                                            Key.Nine -> 9
                                            else -> null
                                        }
                                    val currentTab = SettingsTab.entries.getOrNull(selectedTabIndex) ?: SettingsTab.GENERAL
                                    if (number != null && !event.isCtrlPressed && !event.isAltPressed) {
                                        // In Sound tab, number 0 selects master volume, 7-9 select other volume bars
                                        if (currentTab == SettingsTab.SOUND && number == 0) {
                                            selectedVolumeIndex = 0 // master volume
                                            true
                                        } else if (currentTab == SettingsTab.SOUND && number in 7..9) {
                                            selectedVolumeIndex = number - 6 // 7->1(effects), 8->2(worldmap), 9->3(gameplay)
                                            true
                                        } else if (currentTab == SettingsTab.MOUSE_POINTERS && number == 3) {
                                            selectedMousePointerSliderIndex = 0
                                            true
                                        } else if (currentTab == SettingsTab.MOUSE_POINTERS && number == 4) {
                                            if (AppSettings.mousePointerSource.value == MousePointerSource.GAME) {
                                                selectedMousePointerSliderIndex = 1
                                            }
                                            true
                                        } else {
                                            handleSettingsNumberKey(currentTab, number)
                                        }
                                    } else if (!event.isCtrlPressed && !event.isAltPressed) {
                                        when {
                                            event.key == Key.X -> {
                                                AppSettings.resetToDefaults()
                                                true
                                            }
                                            currentTab == SettingsTab.GENERAL && event.key == Key.L -> {
                                                triggerOpenLanguage = true
                                                true
                                            }
                                            currentTab == SettingsTab.GENERAL && event.key == Key.D -> {
                                                triggerOpenDifficulty = true
                                                true
                                            }
                                            currentTab == SettingsTab.GENERAL && event.key == Key.R -> {
                                                triggerRestoreData = true
                                                true
                                            }
                                            currentTab == SettingsTab.SOUND && event.key == Key.D -> {
                                                triggerShowSoundDetails = true
                                                true
                                            }
                                            // +/- for Sound tab uses selectedVolumeIndex
                                            currentTab == SettingsTab.SOUND && (event.key == Key.Plus || event.key == Key.Equals) -> {
                                                adjustSoundVolume(selectedVolumeIndex, increase = true)
                                                true
                                            }
                                            currentTab == SettingsTab.SOUND && event.key == Key.Minus -> {
                                                adjustSoundVolume(selectedVolumeIndex, increase = false)
                                                true
                                            }
                                            // F/T select slider, +/- adjust selected slider in Accessibility tab
                                            currentTab == SettingsTab.ACCESSIBILITY && event.key == Key.F -> {
                                                selectedA11ySliderIndex = 0
                                                true
                                            }
                                            currentTab == SettingsTab.ACCESSIBILITY && event.key == Key.T -> {
                                                selectedA11ySliderIndex = 1
                                                true
                                            }
                                            currentTab == SettingsTab.ACCESSIBILITY && (event.key == Key.Plus || event.key == Key.Equals) -> {
                                                adjustA11ySlider(selectedA11ySliderIndex, increase = true)
                                                true
                                            }
                                            currentTab == SettingsTab.ACCESSIBILITY && event.key == Key.Minus -> {
                                                adjustA11ySlider(selectedA11ySliderIndex, increase = false)
                                                true
                                            }
                                            currentTab == SettingsTab.MOUSE_POINTERS && AppSettings.showButtonShortcutHints.value && (event.key == Key.Plus || event.key == Key.Equals) -> {
                                                adjustMousePointerSlider(selectedMousePointerSliderIndex, increase = true)
                                                true
                                            }
                                            currentTab == SettingsTab.MOUSE_POINTERS && AppSettings.showButtonShortcutHints.value && event.key == Key.Minus -> {
                                                adjustMousePointerSlider(selectedMousePointerSliderIndex, increase = false)
                                                true
                                            }
                                            else -> handleSettingsLetterKey(currentTab, event.key)
                                        }
                                    } else {
                                        false
                                    }
                                }
                            }
                        } else {
                            false
                        }
                    },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(),
            ) {
                // Title row with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelectableText(
                        text = stringResource(Res.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        de.egril.defender.ui.gameplay
                            .ShortcutKeyChip(text = "Esc")
                        val closeLabel = stringResource(Res.string.close)
                        IconButton(
                            onClick = onDismiss,
                            modifier =
                                Modifier.a11ySemantics(
                                    role = Role.Button,
                                    label = closeLabel,
                                ),
                        ) {
                            MaterialSymbol.Filled(
                                icon = MaterialSymbols.CLOSE,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                // Tab row
                val tabEntriesWithLabels =
                    listOf(
                        SettingsTab.GENERAL to stringResource(Res.string.general),
                        SettingsTab.WORLD_MAP to stringResource(Res.string.world_map),
                        SettingsTab.LEVEL to stringResource(Res.string.settings_tab_level),
                        SettingsTab.SOUND to stringResource(Res.string.sound),
                        SettingsTab.ACCESSIBILITY to stringResource(Res.string.accessibility),
                        SettingsTab.MOUSE_POINTERS to stringResource(Res.string.settings_tab_mouse_pointers),
                        SettingsTab.SHORTCUTS to stringResource(Res.string.settings_tab_shortcuts),
                    )

                // Tab navigation hint
                if (AppSettings.showButtonShortcutHints.value) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        de.egril.defender.ui.gameplay
                            .ShortcutKeyChip(text = "\u2190\u2192")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tabs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        de.egril.defender.ui.gameplay
                            .ShortcutKeyChip(text = "1-9")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(Res.string.toggle_setting),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        de.egril.defender.ui.gameplay
                            .ShortcutKeyChip(text = "\u2191\u2193")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(Res.string.scroll),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                ScrollableTabRowWithHints(
                    selectedTabIndex = selectedTabIndex,
                ) {
                    tabEntriesWithLabels.forEachIndexed { index, (_, title) ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val selectedTabType = tabEntriesWithLabels[selectedTabIndex].first

                // Tab content
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                ) {
                    when (selectedTabType) {
                        SettingsTab.GENERAL ->
                            ScrollableSettingsTabContent(settingsScrollState) {
                                GeneralTabContent(onDismissSettings = ::dismissSettingsDialog, triggerRestore = triggerRestoreData, onRestoreHandled = {
                                    triggerRestoreData =
                                        false
                                }, triggerOpenLanguage = triggerOpenLanguage, onOpenLanguageHandled = {
                                    triggerOpenLanguage = false
                                }, triggerOpenDifficulty = triggerOpenDifficulty, onOpenDifficultyHandled = {
                                    triggerOpenDifficulty =
                                        false
                                })
                            }
                        SettingsTab.WORLD_MAP -> ScrollableSettingsTabContent(settingsScrollState) { WorldmapTabContent() }
                        SettingsTab.LEVEL -> ScrollableSettingsTabContent(settingsScrollState) { LevelTabContent() }
                        SettingsTab.SOUND ->
                            ScrollableSettingsTabContent(settingsScrollState) {
                                SoundTabContent(triggerShowDetails = triggerShowSoundDetails, onShowDetailsHandled = {
                                    triggerShowSoundDetails =
                                        false
                                }, selectedVolumeIndex = selectedVolumeIndex, onVolumeIndexChanged = { selectedVolumeIndex = it }, onManualBackgroundMusicStart = {
                                    shouldRestoreBackgroundMusicOnDismiss = true
                                })
                            }
                        SettingsTab.ACCESSIBILITY ->
                            ScrollableSettingsTabContent(settingsScrollState) {
                                AccessibilityTabContent(selectedSliderIndex = selectedA11ySliderIndex, onSliderIndexChanged = {
                                    selectedA11ySliderIndex =
                                        it
                                })
                            }
                        SettingsTab.MOUSE_POINTERS ->
                            ScrollableSettingsTabContent(settingsScrollState) {
                                MousePointerTabContent(
                                    selectedSliderIndex = selectedMousePointerSliderIndex,
                                    onSliderIndexChanged = { selectedMousePointerSliderIndex = it },
                                )
                            }
                        SettingsTab.SHORTCUTS -> ShortcutBindingsTabContent(settingsScrollState, keybindFocusManager)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

                // Reset button
                OutlinedButton(
                    onClick = { AppSettings.resetToDefaults() },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    Text(stringResource(Res.string.reset_settings))
                    if (AppSettings.showButtonShortcutHints.value) {
                        Spacer(modifier = Modifier.width(8.dp))
                        de.egril.defender.ui.gameplay
                            .ShortcutKeyChip(text = "X")
                    }
                }
            }
        }
    }
}
