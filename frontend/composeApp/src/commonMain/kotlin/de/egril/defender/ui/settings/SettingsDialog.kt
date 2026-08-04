@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import de.egril.defender.editor.RepositoryManager
import de.egril.defender.ui.a11y.ColorBlindPalette
import de.egril.defender.ui.a11y.a11ySemantics
import de.egril.defender.ui.common.ScrollableTabRowWithHints
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.icon.SpeakerHighIcon
import de.egril.defender.ui.icon.SpeakerLowIcon
import de.egril.defender.ui.infopage.KeybindFocusManager
import de.egril.defender.ui.infopage.KeyboardShortcutsInfo
import de.egril.defender.ui.infopage.LocalKeybindFocusManager
import de.egril.defender.utils.isPlatformWasm
import de.egril.defender.utils.reloadApp
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import dev.vicart.compose.material.symbols.FilledSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import kotlinx.coroutines.launch

/**
 * Settings dialog that provides access to app settings like language selection and dark mode.
 * Settings are organized into tabs: General, Worldmap, Level, Sound, Accessibility, Shortcuts.
 */
enum class SettingsTab {
    GENERAL,
    WORLD_MAP,
    LEVEL,
    SOUND,
    ACCESSIBILITY,
    SHORTCUTS,
}

private val COLOR_BLIND_OPTION_LABEL_TOP_PADDING = 10.dp

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    initialTab: SettingsTab = SettingsTab.GENERAL,
    pageBackgroundMusic: de.egril.defender.audio.BackgroundMusic? = null,
) {
    val tabCount = 6 // GENERAL, WORLD_MAP, LEVEL, SOUND, ACCESSIBILITY, SHORTCUTS
    var selectedTabIndex by remember(initialTab) {
        mutableStateOf(
            SettingsTab.entries.indexOf(initialTab).coerceAtLeast(0),
        )
    }
    var shouldRestoreBackgroundMusicOnDismiss by remember { mutableStateOf(false) }

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
                            FilledSymbol(
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

@Composable
private fun ScrollableSettingsTabContent(
    scrollState: ScrollState,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .verticalScroll(scrollState),
        ) {
            content()
        }
    }
}

/**
 * Helper composable that wraps a setting with a number shortcut chip when hints are enabled.
 */
@Composable
private fun NumberedSetting(
    number: Int,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        de.egril.defender.ui.gameplay
            .ShortcutKeyChip(text = number.toString())
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

/**
 * General tab: Language, Difficulty, Dark mode, Check for updates, Debug options.
 */
@Composable
private fun GeneralTabContent(
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

@Composable
private fun AccessibilityTabContent(
    selectedSliderIndex: Int = 0,
    onSliderIndexChanged: (Int) -> Unit = {},
) {
    val accessibilityPreferences = AppSettings.getAccessibilityPreferences()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectableText(
            text = stringResource(Res.string.accessibility),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        NumberedSetting(1) {
            GenericSwitch(
                state = AppSettings.highContrastEnabled,
                checkedText = stringResource(Res.string.accessibility_high_contrast),
                uncheckedText = stringResource(Res.string.accessibility_high_contrast),
                onCheckedChange = { enabled ->
                    AppSettings.saveHighContrastEnabled(enabled)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AccessibilityInfoText(stringResource(Res.string.accessibility_high_contrast_info))

        NumberedSetting(2) { CaptionsSetting() }

        NumberedSetting(3) {
            GenericSwitch(
                state = AppSettings.holdToConfirmEnabled,
                checkedText = stringResource(Res.string.accessibility_hold_to_confirm),
                uncheckedText = stringResource(Res.string.accessibility_hold_to_confirm),
                onCheckedChange = { enabled ->
                    AppSettings.saveHoldToConfirmEnabled(enabled)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AccessibilityInfoText(stringResource(Res.string.accessibility_hold_to_confirm_info))

        NumberedSetting(4) {
            GenericSwitch(
                state = AppSettings.enableAnimations,
                checkedText = stringResource(Res.string.level_animations),
                uncheckedText = stringResource(Res.string.level_animations),
                onCheckedChange = { enabled ->
                    AppSettings.saveEnableAnimations(enabled)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AccessibilityInfoText(stringResource(Res.string.accessibility_reduce_motion_level_setting_info))

        NumberedSetting(5) {
            GenericSwitch(
                state = AppSettings.enableWorldMapAnimations,
                checkedText = stringResource(Res.string.world_map_animations),
                uncheckedText = stringResource(Res.string.world_map_animations),
                onCheckedChange = { enabled ->
                    AppSettings.saveEnableWorldMapAnimations(enabled)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AccessibilityInfoText(stringResource(Res.string.accessibility_reduce_motion_worldmap_setting_info))

        SelectableText(
            text =
                if (accessibilityPreferences.reduceMotionEnabled) {
                    stringResource(Res.string.accessibility_reduce_motion_on)
                } else {
                    stringResource(Res.string.accessibility_reduce_motion_off)
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SelectableText(
            text = stringResource(Res.string.accessibility_reduce_motion_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FontSizeSetting(
            isSelected = selectedSliderIndex == 0,
            onSelect = { onSliderIndexChanged(0) },
        )

        HeaderTextSizeSetting(
            isSelected = selectedSliderIndex == 1,
            onSelect = { onSliderIndexChanged(1) },
        )

        ColorBlindPaletteChooser(
            selected = AppSettings.colorBlindPalette.value,
            onSelected = { AppSettings.saveColorBlindPalette(it) },
        )
        AccessibilityInfoText(stringResource(Res.string.accessibility_color_blind_palette_info))
    }
}

@Composable
private fun AccessibilityInfoText(text: String) {
    SelectableText(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ShortcutBindingsTabContent(
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

@Composable
private fun ColorBlindPaletteChooser(
    selected: ColorBlindPalette,
    onSelected: (ColorBlindPalette) -> Unit,
) {
    val options =
        listOf(
            Triple(
                ColorBlindPalette.OFF,
                stringResource(Res.string.accessibility_color_blind_off),
                stringResource(Res.string.accessibility_color_blind_off_description),
            ),
            Triple(
                ColorBlindPalette.DEUTERANOPIA,
                stringResource(Res.string.accessibility_color_blind_deuteranopia),
                stringResource(Res.string.accessibility_color_blind_deuteranopia_description),
            ),
            Triple(
                ColorBlindPalette.PROTANOPIA,
                stringResource(Res.string.accessibility_color_blind_protanopia),
                stringResource(Res.string.accessibility_color_blind_protanopia_description),
            ),
            Triple(
                ColorBlindPalette.TRITANOPIA,
                stringResource(Res.string.accessibility_color_blind_tritanopia),
                stringResource(Res.string.accessibility_color_blind_tritanopia_description),
            ),
        )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectableText(
            text = stringResource(Res.string.accessibility_color_blind_palette),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        options.forEachIndexed { index, (palette, label, description) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                de.egril.defender.ui.gameplay
                    .ShortcutKeyChip(text = "${index + 6}")
                RadioButton(
                    selected = selected == palette,
                    onClick = { onSelected(palette) },
                )
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(top = COLOR_BLIND_OPTION_LABEL_TOP_PADDING),
                ) {
                    SelectableText(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    SelectableText(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Section with a "Restore Game Data" button that clears cached game data and reloads
 * from the bundled repository. On web it reloads the browser; on other platforms it
 * performs a backup-and-restore via RepositoryManager and reloads the world map data.
 */
@Composable
private fun RestoreGameDataSection(
    onDismissSettings: () -> Unit,
    triggerRestore: Boolean = false,
    onRestoreHandled: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf<Pair<Boolean, String?>?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    // Handle keyboard trigger
    LaunchedEffect(triggerRestore) {
        if (triggerRestore) {
            showConfirmDialog = true
            onRestoreHandled()
        }
    }

    val confirmMessage =
        if (isPlatformWasm) {
            stringResource(Res.string.restore_game_data_confirm_message_web)
        } else {
            stringResource(Res.string.restore_game_data_confirm_message)
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SelectableText(
            text = stringResource(Res.string.game_data),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedButton(
            onClick = { showConfirmDialog = true },
            enabled = !isRestoring,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.restore_game_data))
            if (AppSettings.showButtonShortcutHints.value) {
                Spacer(modifier = Modifier.width(8.dp))
                de.egril.defender.ui.gameplay
                    .ShortcutKeyChip(text = "R")
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(Res.string.restore_game_data_confirm_title)) },
            text = { SelectionContainer { Text(confirmMessage) } },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isRestoring = true
                        coroutineScope.launch {
                            val backupPath = RepositoryManager.restoreFromRepository()
                            isRestoring = false
                            if (isPlatformWasm) {
                                reloadApp()
                            } else {
                                showResultDialog = (backupPath != null) to backupPath
                            }
                        }
                    },
                ) {
                    Text(stringResource(Res.string.yes))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    val resultDialogState = showResultDialog
    if (resultDialogState != null) {
        val (resultSuccess, resultPath) = resultDialogState
        val title =
            if (resultSuccess) {
                stringResource(Res.string.restore_game_data_success_title)
            } else {
                stringResource(Res.string.restore_game_data_failure_title)
            }
        val message =
            if (resultSuccess && resultPath != null) {
                stringResource(Res.string.restore_game_data_success_message, resultPath)
            } else {
                stringResource(Res.string.restore_game_data_failure_message)
            }
        AlertDialog(
            onDismissRequest = { showResultDialog = null },
            title = { Text(title) },
            text = { SelectionContainer { Text(message) } },
            confirmButton = {
                Button(onClick = {
                    showResultDialog = null
                    if (resultSuccess) {
                        RepositoryManager.onDataRestored?.invoke()
                        onDismissSettings()
                    }
                }) {
                    Text(stringResource(Res.string.close))
                }
            },
        )
    }
}

/**
 * Worldmap UI tab: World map style, world map animations, show testing levels.
 */
@Composable
private fun WorldmapTabContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // World map style switch (inverted logic: false = Image Map View, true = Level Cards View)
        val invertedUseLevelCards = remember { mutableStateOf(!AppSettings.useLevelCards.value) }
        LaunchedEffect(AppSettings.useLevelCards.value) {
            invertedUseLevelCards.value = !AppSettings.useLevelCards.value
        }
        NumberedSetting(1) {
            DualLabelSwitch(
                state = invertedUseLevelCards,
                leftText = stringResource(Res.string.world_map_level_cards),
                rightText = stringResource(Res.string.world_map_image_map),
                onCheckedChange = { enabled ->
                    AppSettings.saveUseLevelCards(!enabled)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // World map animation switch
        NumberedSetting(2) {
            GenericSwitch(
                state = AppSettings.enableWorldMapAnimations,
                checkedText = stringResource(Res.string.world_map_animations),
                uncheckedText = stringResource(Res.string.world_map_animations),
                onCheckedChange = { enabled ->
                    AppSettings.saveEnableWorldMapAnimations(enabled)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Show testing levels switch
        NumberedSetting(3) {
            GenericSwitch(
                state = AppSettings.showTestingLevels,
                checkedText = stringResource(Res.string.show_testing_levels),
                uncheckedText = stringResource(Res.string.show_testing_levels),
                onCheckedChange = { enabled ->
                    AppSettings.saveShowTestingLevels(enabled)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Level UI tab: Tile images, animations, map image, header text size, controls.
 */
@Composable
private fun LevelTabContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Tile images section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableText(
                text = stringResource(Res.string.appearance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Tile background images switch
            NumberedSetting(1) {
                DualLabelSwitch(
                    state = AppSettings.useTileImages,
                    leftText = stringResource(Res.string.tile_background_images_off),
                    rightText = stringResource(Res.string.tile_background_images_on),
                    onCheckedChange = { enabled ->
                        AppSettings.saveUseTileImages(enabled)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Tile smooth transitions switch (only visible when tile images are on)
            if (AppSettings.useTileImages.value) {
                NumberedSetting(2) {
                    DualLabelSwitch(
                        state = AppSettings.useTileSmoothTransitions,
                        leftText = stringResource(Res.string.tile_smooth_transitions_off),
                        rightText = stringResource(Res.string.tile_smooth_transitions_on),
                        onCheckedChange = { enabled ->
                            AppSettings.saveUseTileSmoothTransitions(enabled)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Animations switch
            NumberedSetting(3) {
                GenericSwitch(
                    state = AppSettings.enableAnimations,
                    checkedText = stringResource(Res.string.level_animations),
                    uncheckedText = stringResource(Res.string.level_animations),
                    onCheckedChange = { enabled ->
                        AppSettings.saveEnableAnimations(enabled)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Level map image switch
            NumberedSetting(4) {
                GenericSwitch(
                    state = AppSettings.useLevelMapImage,
                    checkedText = stringResource(Res.string.level_map_image),
                    uncheckedText = stringResource(Res.string.level_map_image),
                    onCheckedChange = { enabled ->
                        AppSettings.saveUseLevelMapImage(enabled)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Unit/tower background color switch
            NumberedSetting(5) {
                DualLabelSwitch(
                    state = AppSettings.showUnitTowerBackground,
                    leftText = stringResource(Res.string.unit_tower_background_off),
                    rightText = stringResource(Res.string.unit_tower_background_on),
                    onCheckedChange = { enabled ->
                        AppSettings.saveShowUnitTowerBackground(enabled)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HeaderTextSizeSetting()
        }

        HorizontalDivider()

        // Controls section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableText(
                text = stringResource(Res.string.controls),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Control pad switch
            NumberedSetting(6) {
                GenericSwitch(
                    state = AppSettings.showControlPad,
                    checkedText = stringResource(Res.string.control_pad_enabled),
                    uncheckedText = stringResource(Res.string.control_pad_enabled),
                    onCheckedChange = { enabled ->
                        AppSettings.saveShowControlPad(enabled)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Auto-jump to next actionable tower switch
            NumberedSetting(7) {
                GenericSwitch(
                    state = AppSettings.autoJumpToNextTower,
                    checkedText = stringResource(Res.string.auto_jump_to_next_tower),
                    uncheckedText = stringResource(Res.string.auto_jump_to_next_tower),
                    onCheckedChange = { enabled ->
                        AppSettings.saveAutoJumpToNextTower(enabled)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun FontSizeSetting(
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (isSelected) {
                        Modifier
                            .border(
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                RoundedCornerShape(8.dp),
                            ).padding(4.dp)
                    } else {
                        Modifier
                    },
                ).clickable { onSelect() },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SelectableText(
                text = stringResource(Res.string.accessibility_font_size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (AppSettings.showButtonShortcutHints.value) {
                de.egril.defender.ui.gameplay
                    .ShortcutKeyChip(text = "F")
                if (isSelected) {
                    Text(
                        text = "+/-",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectableText(
                text = stringResource(Res.string.accessibility_font_size_small),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(48.dp),
            )
            var sliderValue by remember(AppSettings.fontSize.value) {
                mutableStateOf(
                    AppSettings.fontSize.value.ordinal
                        .toFloat(),
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    sliderValue = value
                },
                onValueChangeFinished = {
                    val size = FontSize.entries[sliderValue.toInt().coerceIn(0, FontSize.entries.lastIndex)]
                    AppSettings.saveFontSize(size)
                },
                modifier = Modifier.weight(1f),
                valueRange = 0f..(FontSize.entries.size - 1).toFloat(),
                steps = FontSize.entries.size - 2,
            )
            SelectableText(
                text = stringResource(Res.string.accessibility_font_size_large),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(48.dp),
            )
        }
        AccessibilityInfoText(stringResource(Res.string.accessibility_font_size_info))
    }
}

@Composable
private fun HeaderTextSizeSetting(
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (isSelected) {
                        Modifier
                            .border(
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                RoundedCornerShape(8.dp),
                            ).padding(4.dp)
                    } else {
                        Modifier
                    },
                ).clickable { onSelect() },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectableText(
            text = stringResource(Res.string.header_text_size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (AppSettings.showButtonShortcutHints.value) {
            de.egril.defender.ui.gameplay
                .ShortcutKeyChip(text = "T")
            if (isSelected) {
                Text(
                    text = "+/-",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectableText(
                    text = stringResource(Res.string.header_text_size_small),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(40.dp),
                )
                Slider(
                    value =
                        AppSettings.headerTextSize.value.ordinal
                            .toFloat(),
                    onValueChange = { value ->
                        val size =
                            when (value.toInt()) {
                                0 -> HeaderTextSize.SMALL
                                1 -> HeaderTextSize.MEDIUM
                                else -> HeaderTextSize.LARGE
                            }
                        AppSettings.saveHeaderTextSize(size)
                    },
                    modifier = Modifier.weight(1f),
                    valueRange = 0f..2f,
                    steps = 1,
                )
                SelectableText(
                    text = stringResource(Res.string.header_text_size_large),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(40.dp),
                )
            }
            SelectableText(
                text = stringResource(Res.string.header_text_size_medium),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun CaptionsSetting() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        GenericSwitch(
            state = AppSettings.captionsEnabled,
            checkedText = stringResource(Res.string.accessibility_captions),
            uncheckedText = stringResource(Res.string.accessibility_captions),
            onCheckedChange = { enabled ->
                AppSettings.saveCaptionsEnabled(enabled)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        AccessibilityInfoText(stringResource(Res.string.accessibility_captions_info))
    }
}

/**
 * Sound tab: All sound settings.
 */
@Composable
private fun SoundTabContent(
    triggerShowDetails: Boolean = false,
    onShowDetailsHandled: () -> Unit = {},
    selectedVolumeIndex: Int = 0,
    onVolumeIndexChanged: (Int) -> Unit = {},
    onManualBackgroundMusicStart: () -> Unit = {},
) {
    var showDetailedSoundSettings by remember { mutableStateOf(false) }

    // Handle keyboard trigger for showing details
    LaunchedEffect(triggerShowDetails) {
        if (triggerShowDetails) {
            showDetailedSoundSettings = !showDetailedSoundSettings
            onShowDetailsHandled()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Overall sound enabled/disabled switch
        NumberedSetting(1) {
            GenericSwitch(
                state = AppSettings.isSoundEnabled,
                checkedText = stringResource(Res.string.sound),
                uncheckedText = stringResource(Res.string.sound),
                onCheckedChange = { enabled ->
                    AppSettings.saveSoundEnabled(enabled)
                    de.egril.defender.audio.GlobalSoundManager
                        .getInstance()
                        ?.setEnabled(enabled && AppSettings.isEffectsEnabled.value)
                    if (enabled && AppSettings.isMusicEnabled.value) {
                        val currentMusic =
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .getCurrentMusic()
                        if (currentMusic != null) {
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .playMusic(currentMusic, loop = true)
                        }
                    } else {
                        de.egril.defender.audio.GlobalBackgroundMusicManager
                            .stopMusic()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        NumberedSetting(2) { CaptionsSetting() }

        // Master volume slider (only shown when sound is enabled)
        if (AppSettings.isSoundEnabled.value) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (selectedVolumeIndex == 0) {
                                Modifier
                                    .border(
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                        RoundedCornerShape(8.dp),
                                    ).padding(4.dp)
                            } else {
                                Modifier
                            },
                        ).clickable { onVolumeIndexChanged(0) },
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SelectableText(
                        text = stringResource(Res.string.sound_volume),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (AppSettings.showButtonShortcutHints.value) {
                        de.egril.defender.ui.gameplay
                            .ShortcutKeyChip(text = "0")
                        if (selectedVolumeIndex == 0) {
                            Text(
                                text = "+/-",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            dev.vicart.compose.material.symbols.FilledSymbol(
                                icon = dev.vicart.compose.material.symbols.MaterialSymbols.ARROW_BACK,
                                size = 12.dp,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpeakerLowIcon(size = 20.dp)
                    Slider(
                        value = AppSettings.soundVolume.value,
                        onValueChange = { volume ->
                            AppSettings.saveSoundVolume(volume)
                        },
                        modifier = Modifier.weight(1f),
                        valueRange = 0f..1f,
                    )
                    SpeakerHighIcon(size = 20.dp)
                }
            }

            // Button to show/hide detailed sound settings
            Button(
                onClick = { showDetailedSoundSettings = !showDetailedSoundSettings },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text =
                        if (showDetailedSoundSettings) {
                            stringResource(Res.string.hide_detailed_sound_settings)
                        } else {
                            stringResource(Res.string.show_detailed_sound_settings)
                        },
                )
                if (AppSettings.showButtonShortcutHints.value) {
                    Spacer(modifier = Modifier.width(8.dp))
                    de.egril.defender.ui.gameplay
                        .ShortcutKeyChip(text = "D")
                }
            }

            // Detailed sound settings (collapsible)
            if (showDetailedSoundSettings) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Effect sounds sub-section
                    Text(
                        text = stringResource(Res.string.effect_sounds),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    NumberedSetting(3) {
                        GenericSwitch(
                            state = AppSettings.isEffectsEnabled,
                            checkedText = stringResource(Res.string.effects_enabled),
                            uncheckedText = stringResource(Res.string.effects_disabled),
                            onCheckedChange = { enabled ->
                                AppSettings.saveEffectsEnabled(enabled)
                                de.egril.defender.audio.GlobalSoundManager.getInstance()?.setEnabled(
                                    enabled && AppSettings.isSoundEnabled.value,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (AppSettings.isEffectsEnabled.value) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (selectedVolumeIndex == 1) {
                                            Modifier
                                                .border(
                                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                                    RoundedCornerShape(8.dp),
                                                ).padding(4.dp)
                                        } else {
                                            Modifier
                                        },
                                    ).clickable { onVolumeIndexChanged(1) },
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.effects_volume),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (AppSettings.showButtonShortcutHints.value) {
                                    de.egril.defender.ui.gameplay
                                        .ShortcutKeyChip(text = "7")
                                    if (selectedVolumeIndex == 1) {
                                        Text(
                                            text = "+/-",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SpeakerLowIcon(size = 20.dp)
                                Slider(
                                    value = AppSettings.effectsVolume.value,
                                    onValueChange = { volume ->
                                        AppSettings.saveEffectsVolume(volume)
                                        de.egril.defender.audio.GlobalSoundManager
                                            .getInstance()
                                            ?.setVolume(volume)
                                    },
                                    modifier = Modifier.weight(1f),
                                    valueRange = 0f..1f,
                                )
                                SpeakerHighIcon(size = 20.dp)
                            }
                        }

                        // Play effect preview button (bow tower attack sound)
                        OutlinedButton(
                            onClick = {
                                de.egril.defender.audio.GlobalSoundManager
                                    .playSound(de.egril.defender.audio.SoundEvent.ATTACK_RANGED)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(Res.string.sound_preview_play_effect))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Background music sub-section
                    Text(
                        text = stringResource(Res.string.background_music),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    NumberedSetting(4) {
                        GenericSwitch(
                            state = AppSettings.isMusicEnabled,
                            checkedText = stringResource(Res.string.music_enabled),
                            uncheckedText = stringResource(Res.string.music_disabled),
                            onCheckedChange = { enabled ->
                                AppSettings.saveMusicEnabled(enabled)
                                if (enabled && AppSettings.isSoundEnabled.value) {
                                    val currentMusic =
                                        de.egril.defender.audio.GlobalBackgroundMusicManager
                                            .getCurrentMusic()
                                    if (currentMusic != null) {
                                        de.egril.defender.audio.GlobalBackgroundMusicManager
                                            .playMusic(currentMusic, loop = true)
                                    }
                                } else {
                                    de.egril.defender.audio.GlobalBackgroundMusicManager
                                        .stopMusic()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Play/Stop background music preview button
                    if (AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                        // Background music volume slider
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.music_volume),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SpeakerLowIcon(size = 20.dp)
                                Slider(
                                    value = AppSettings.musicVolume.value,
                                    onValueChange = { volume ->
                                        AppSettings.saveMusicVolume(volume)
                                        val currentMusic =
                                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                                .getCurrentMusic()
                                        if (currentMusic != null) {
                                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                                .playMusic(currentMusic, loop = true)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    valueRange = 0f..1f,
                                )
                                SpeakerHighIcon(size = 20.dp)
                            }
                        }
                    }

                    if (AppSettings.isMusicEnabled.value) {
                        // World Map Music
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.worldmap_music),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            NumberedSetting(5) {
                                GenericSwitch(
                                    state = AppSettings.isWorldMapMusicEnabled,
                                    checkedText = stringResource(Res.string.worldmap_music_enabled),
                                    uncheckedText = stringResource(Res.string.worldmap_music_disabled),
                                    onCheckedChange = { enabled ->
                                        AppSettings.saveWorldMapMusicEnabled(enabled)
                                        val currentMusic =
                                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                                .getCurrentMusic()
                                        if (currentMusic == de.egril.defender.audio.BackgroundMusic.WORLD_MAP) {
                                            if (enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                                                de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                                                    de.egril.defender.audio.BackgroundMusic.WORLD_MAP,
                                                    loop = true,
                                                )
                                            } else {
                                                de.egril.defender.audio.GlobalBackgroundMusicManager
                                                    .stopMusic()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            if (AppSettings.isWorldMapMusicEnabled.value) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (selectedVolumeIndex == 2) {
                                                    Modifier
                                                        .border(
                                                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                                            RoundedCornerShape(8.dp),
                                                        ).padding(4.dp)
                                                } else {
                                                    Modifier
                                                },
                                            ).clickable { onVolumeIndexChanged(2) },
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.worldmap_music_volume),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (AppSettings.showButtonShortcutHints.value) {
                                            de.egril.defender.ui.gameplay
                                                .ShortcutKeyChip(text = "8")
                                            if (selectedVolumeIndex == 2) {
                                                Text(
                                                    text = "+/-",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        SpeakerLowIcon(size = 20.dp)
                                        Slider(
                                            value = AppSettings.worldMapMusicVolume.value,
                                            onValueChange = { volume ->
                                                AppSettings.saveWorldMapMusicVolume(volume)
                                                val currentMusic =
                                                    de.egril.defender.audio.GlobalBackgroundMusicManager
                                                        .getCurrentMusic()
                                                if (currentMusic == de.egril.defender.audio.BackgroundMusic.WORLD_MAP) {
                                                    de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                                                        de.egril.defender.audio.BackgroundMusic.WORLD_MAP,
                                                        loop = true,
                                                    )
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            valueRange = 0f..1f,
                                        )
                                        SpeakerHighIcon(size = 20.dp)
                                    }
                                }
                            }

                            if (AppSettings.isSoundEnabled.value) {
                                // Start worldmap background music persistently (not stopped when leaving settings)
                                OutlinedButton(
                                    onClick = {
                                        onManualBackgroundMusicStart()
                                        de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                                            de.egril.defender.audio.BackgroundMusic.WORLD_MAP,
                                            loop = true,
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(text = stringResource(Res.string.worldmap_music))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Gameplay Music
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.gameplay_music),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            NumberedSetting(6) {
                                GenericSwitch(
                                    state = AppSettings.isGameplayMusicEnabled,
                                    checkedText = stringResource(Res.string.gameplay_music_enabled),
                                    uncheckedText = stringResource(Res.string.gameplay_music_disabled),
                                    onCheckedChange = { enabled ->
                                        AppSettings.saveGameplayMusicEnabled(enabled)
                                        val currentMusic =
                                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                                .getCurrentMusic()
                                        if (currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL ||
                                            currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH
                                        ) {
                                            if (enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                                                de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                                                    currentMusic,
                                                    loop = true,
                                                )
                                            } else {
                                                de.egril.defender.audio.GlobalBackgroundMusicManager
                                                    .stopMusic()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            if (AppSettings.isGameplayMusicEnabled.value) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (selectedVolumeIndex == 3) {
                                                    Modifier
                                                        .border(
                                                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                                            RoundedCornerShape(8.dp),
                                                        ).padding(4.dp)
                                                } else {
                                                    Modifier
                                                },
                                            ).clickable { onVolumeIndexChanged(3) },
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.gameplay_music_volume),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (AppSettings.showButtonShortcutHints.value) {
                                            de.egril.defender.ui.gameplay
                                                .ShortcutKeyChip(text = "9")
                                            if (selectedVolumeIndex == 3) {
                                                Text(
                                                    text = "+/-",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        SpeakerLowIcon(size = 20.dp)
                                        Slider(
                                            value = AppSettings.gameplayMusicVolume.value,
                                            onValueChange = { volume ->
                                                AppSettings.saveGameplayMusicVolume(volume)
                                                val currentMusic =
                                                    de.egril.defender.audio.GlobalBackgroundMusicManager
                                                        .getCurrentMusic()
                                                if (currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL ||
                                                    currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH
                                                ) {
                                                    de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                                                        currentMusic,
                                                        loop = true,
                                                    )
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            valueRange = 0f..1f,
                                        )
                                        SpeakerHighIcon(size = 20.dp)
                                    }
                                }

                                if (AppSettings.isSoundEnabled.value) {
                                    // Start gameplay background music persistently (not stopped when leaving settings)
                                    OutlinedButton(
                                        onClick = {
                                            onManualBackgroundMusicStart()
                                            de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                                                de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL,
                                                loop = true,
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(text = stringResource(Res.string.start_gameplay_background_music))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Handle number key shortcuts to toggle settings in the current settings tab.
 * Returns true if the key was handled.
 */
private fun handleSettingsNumberKey(
    tab: SettingsTab,
    number: Int,
): Boolean =
    when (tab) {
        SettingsTab.GENERAL ->
            when (number) {
                1 -> {
                    AppSettings.saveDarkMode(!AppSettings.isDarkMode.value)
                    true
                }
                2 -> {
                    AppSettings.saveShowDebugOptions(!AppSettings.showDebugOptions.value)
                    true
                }
                3 -> {
                    AppSettings.saveCheckForUpdates(!AppSettings.checkForUpdates.value)
                    true
                }
                else -> false
            }
        SettingsTab.WORLD_MAP ->
            when (number) {
                1 -> {
                    AppSettings.saveUseLevelCards(!AppSettings.useLevelCards.value)
                    true
                }
                2 -> {
                    AppSettings.saveEnableWorldMapAnimations(!AppSettings.enableWorldMapAnimations.value)
                    true
                }
                3 -> {
                    AppSettings.saveShowTestingLevels(!AppSettings.showTestingLevels.value)
                    true
                }
                else -> false
            }
        SettingsTab.LEVEL ->
            when (number) {
                1 -> {
                    AppSettings.saveUseTileImages(!AppSettings.useTileImages.value)
                    true
                }
                2 -> {
                    if (AppSettings.useTileImages.value) {
                        AppSettings.saveUseTileSmoothTransitions(!AppSettings.useTileSmoothTransitions.value)
                    }
                    true
                }
                3 -> {
                    AppSettings.saveEnableAnimations(!AppSettings.enableAnimations.value)
                    true
                }
                4 -> {
                    AppSettings.saveUseLevelMapImage(!AppSettings.useLevelMapImage.value)
                    true
                }
                5 -> {
                    AppSettings.saveShowUnitTowerBackground(!AppSettings.showUnitTowerBackground.value)
                    true
                }
                6 -> {
                    AppSettings.saveShowControlPad(!AppSettings.showControlPad.value)
                    true
                }
                7 -> {
                    AppSettings.saveAutoJumpToNextTower(!AppSettings.autoJumpToNextTower.value)
                    true
                }
                else -> false
            }
        SettingsTab.SOUND ->
            when (number) {
                1 -> {
                    val newVal = !AppSettings.isSoundEnabled.value
                    AppSettings.saveSoundEnabled(newVal)
                    de.egril.defender.audio.GlobalSoundManager
                        .getInstance()
                        ?.setEnabled(newVal && AppSettings.isEffectsEnabled.value)
                    if (newVal && AppSettings.isMusicEnabled.value) {
                        val currentMusic =
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .getCurrentMusic()
                        if (currentMusic != null) {
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .playMusic(currentMusic, loop = true)
                        }
                    } else {
                        de.egril.defender.audio.GlobalBackgroundMusicManager
                            .stopMusic()
                    }
                    true
                }
                2 -> {
                    val newVal = !AppSettings.captionsEnabled.value
                    AppSettings.saveCaptionsEnabled(newVal)
                    true
                }
                3 -> {
                    val newVal = !AppSettings.isEffectsEnabled.value
                    AppSettings.saveEffectsEnabled(newVal)
                    de.egril.defender.audio.GlobalSoundManager
                        .getInstance()
                        ?.setEnabled(newVal && AppSettings.isSoundEnabled.value)
                    true
                }
                4 -> {
                    val newVal = !AppSettings.isMusicEnabled.value
                    AppSettings.saveMusicEnabled(newVal)
                    if (newVal && AppSettings.isSoundEnabled.value) {
                        val currentMusic =
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .getCurrentMusic()
                        if (currentMusic != null) {
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .playMusic(currentMusic, loop = true)
                        }
                    } else {
                        de.egril.defender.audio.GlobalBackgroundMusicManager
                            .stopMusic()
                    }
                    true
                }
                5 -> {
                    val newVal = !AppSettings.isWorldMapMusicEnabled.value
                    AppSettings.saveWorldMapMusicEnabled(newVal)
                    val currentMusic =
                        de.egril.defender.audio.GlobalBackgroundMusicManager
                            .getCurrentMusic()
                    if (currentMusic == de.egril.defender.audio.BackgroundMusic.WORLD_MAP) {
                        if (newVal && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                            de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                                de.egril.defender.audio.BackgroundMusic.WORLD_MAP,
                                loop = true,
                            )
                        } else {
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .stopMusic()
                        }
                    }
                    true
                }
                6 -> {
                    val newVal = !AppSettings.isGameplayMusicEnabled.value
                    AppSettings.saveGameplayMusicEnabled(newVal)
                    val currentMusic =
                        de.egril.defender.audio.GlobalBackgroundMusicManager
                            .getCurrentMusic()
                    if (currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL ||
                        currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH
                    ) {
                        if (newVal && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .playMusic(currentMusic, loop = true)
                        } else {
                            de.egril.defender.audio.GlobalBackgroundMusicManager
                                .stopMusic()
                        }
                    }
                    true
                }
                else -> false
            }
        SettingsTab.ACCESSIBILITY ->
            when (number) {
                1 -> {
                    AppSettings.saveHighContrastEnabled(!AppSettings.highContrastEnabled.value)
                    true
                }
                2 -> {
                    AppSettings.saveCaptionsEnabled(!AppSettings.captionsEnabled.value)
                    true
                }
                3 -> {
                    AppSettings.saveHoldToConfirmEnabled(!AppSettings.holdToConfirmEnabled.value)
                    true
                }
                4 -> {
                    AppSettings.saveEnableAnimations(!AppSettings.enableAnimations.value)
                    true
                }
                5 -> {
                    AppSettings.saveEnableWorldMapAnimations(!AppSettings.enableWorldMapAnimations.value)
                    true
                }
                6 -> {
                    AppSettings.saveColorBlindPalette(ColorBlindPalette.OFF)
                    true
                }
                7 -> {
                    AppSettings.saveColorBlindPalette(ColorBlindPalette.DEUTERANOPIA)
                    true
                }
                8 -> {
                    AppSettings.saveColorBlindPalette(ColorBlindPalette.PROTANOPIA)
                    true
                }
                9 -> {
                    AppSettings.saveColorBlindPalette(ColorBlindPalette.TRITANOPIA)
                    true
                }
                else -> false
            }
        SettingsTab.SHORTCUTS ->
            when (number) {
                1 -> {
                    AppSettings.saveShowButtonShortcutHints(!AppSettings.showButtonShortcutHints.value)
                    true
                }
                else -> false
            }
    }

/**
 * Handle letter/special key shortcuts for settings that aren't simple toggles.
 * Returns true if the key was handled.
 */
private fun handleSettingsLetterKey(
    tab: SettingsTab,
    key: Key,
): Boolean =
    when (tab) {
        SettingsTab.GENERAL -> false // L, D, R handled inline in composable
        SettingsTab.SOUND ->
            when (key) {
                Key.D -> {
                    // Toggle show details - handled via composable state, signal via dummy toggle
                    // This will be handled separately in the composable
                    false
                }
                // +/- now handled inline with selectedVolumeIndex
                else -> false
            }
        SettingsTab.LEVEL ->
            when (key) {
                Key.Plus, Key.Equals -> {
                    adjustHeaderTextSize(increase = true)
                    true
                }
                Key.Minus -> {
                    adjustHeaderTextSize(increase = false)
                    true
                }
                else -> false
            }
        SettingsTab.ACCESSIBILITY -> false
        SettingsTab.SHORTCUTS ->
            when (key) {
                Key.R -> {
                    AppSettings.resetShortcutBindings()
                    true
                }
                else -> false
            }
        else -> false
    }

/**
 * Adjusts a sound volume slider based on the selected volume index.
 * 0=master, 1=effects, 2=worldmap music, 3=gameplay music.
 */
private fun adjustSoundVolume(
    selectedIndex: Int,
    increase: Boolean,
) {
    val step = 0.1f
    when (selectedIndex) {
        0 -> {
            val newVolume =
                if (increase) {
                    (AppSettings.soundVolume.value + step).coerceAtMost(1f)
                } else {
                    (AppSettings.soundVolume.value - step).coerceAtLeast(0f)
                }
            AppSettings.saveSoundVolume(newVolume)
        }
        1 -> {
            val newVolume =
                if (increase) {
                    (AppSettings.effectsVolume.value + step).coerceAtMost(1f)
                } else {
                    (AppSettings.effectsVolume.value - step).coerceAtLeast(0f)
                }
            AppSettings.saveEffectsVolume(newVolume)
            de.egril.defender.audio.GlobalSoundManager
                .getInstance()
                ?.setVolume(newVolume)
        }
        2 -> {
            val newVolume =
                if (increase) {
                    (AppSettings.worldMapMusicVolume.value + step).coerceAtMost(1f)
                } else {
                    (AppSettings.worldMapMusicVolume.value - step).coerceAtLeast(0f)
                }
            AppSettings.saveWorldMapMusicVolume(newVolume)
            val currentMusic =
                de.egril.defender.audio.GlobalBackgroundMusicManager
                    .getCurrentMusic()
            if (currentMusic == de.egril.defender.audio.BackgroundMusic.WORLD_MAP) {
                de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                    de.egril.defender.audio.BackgroundMusic.WORLD_MAP,
                    loop = true,
                )
            }
        }
        3 -> {
            val newVolume =
                if (increase) {
                    (AppSettings.gameplayMusicVolume.value + step).coerceAtMost(1f)
                } else {
                    (AppSettings.gameplayMusicVolume.value - step).coerceAtLeast(0f)
                }
            AppSettings.saveGameplayMusicVolume(newVolume)
            val currentMusic =
                de.egril.defender.audio.GlobalBackgroundMusicManager
                    .getCurrentMusic()
            if (currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL ||
                currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH
            ) {
                de.egril.defender.audio.GlobalBackgroundMusicManager
                    .playMusic(currentMusic, loop = true)
            }
        }
    }
}

/**
 * Adjusts the header text size up or down by one step.
 */
private fun adjustHeaderTextSize(increase: Boolean) {
    val current = AppSettings.headerTextSize.value
    val next =
        if (increase) {
            when (current) {
                HeaderTextSize.SMALL -> HeaderTextSize.MEDIUM
                HeaderTextSize.MEDIUM -> HeaderTextSize.LARGE
                HeaderTextSize.LARGE -> HeaderTextSize.LARGE
            }
        } else {
            when (current) {
                HeaderTextSize.LARGE -> HeaderTextSize.MEDIUM
                HeaderTextSize.MEDIUM -> HeaderTextSize.SMALL
                HeaderTextSize.SMALL -> HeaderTextSize.SMALL
            }
        }
    AppSettings.saveHeaderTextSize(next)
}

private fun adjustFontSize(increase: Boolean) {
    val entries = FontSize.entries
    val currentIndex = entries.indexOf(AppSettings.fontSize.value)
    val nextIndex =
        if (increase) {
            (currentIndex + 1).coerceAtMost(entries.lastIndex)
        } else {
            (currentIndex - 1).coerceAtLeast(0)
        }
    AppSettings.saveFontSize(entries[nextIndex])
}

private fun adjustA11ySlider(
    selectedIndex: Int,
    increase: Boolean,
) {
    when (selectedIndex) {
        0 -> adjustFontSize(increase)
        1 -> adjustHeaderTextSize(increase)
    }
}
