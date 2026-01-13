@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyperether.resources.stringResource
import de.egril.defender.editor.RepositoryManager
import de.egril.defender.ui.editor.ConfirmationDialog
import de.egril.defender.ui.icon.SpeakerHighIcon
import de.egril.defender.ui.icon.SpeakerLowIcon
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.launch

/**
 * Settings dialog that provides access to app settings like language selection and dark mode
 */
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var showRestoreSuccess by remember { mutableStateOf(false) }
    var backupPath by remember { mutableStateOf("") }
    var isRestoring by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(min = 300.dp, max = 500.dp)
                .wrapContentHeight()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = stringResource(Res.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                HorizontalDivider()
                
                // Language section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.language),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    LanguageChooser(
                        modifier = Modifier.fillMaxWidth(),
                        onLanguageChanged = { locale ->
                            AppSettings.saveLanguage(locale)
                        }
                    )
                }
                
                HorizontalDivider()
                
                // Difficulty section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.difficulty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    DifficultyChooser(
                        modifier = Modifier.fillMaxWidth(),
                        onDifficultyChanged = { level ->
                            AppSettings.saveDifficulty(level)
                        }
                    )
                    
                    // Info text about difficulty not affecting current level
                    Text(
                        text = stringResource(Res.string.difficulty_info_current_level),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                HorizontalDivider()
                
                // Appearance section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.appearance),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Dark mode switch
                    GenericSwitch(
                        state = AppSettings.isDarkMode,
                        checkedText = stringResource(Res.string.dark_mode),
                        uncheckedText = stringResource(Res.string.dark_mode),
                        onCheckedChange = { enabled ->
                            AppSettings.saveDarkMode(enabled)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // World map style switch (inverted logic: false = Image Map View, true = Level Cards View)
                    val invertedUseLevelCards = remember { mutableStateOf(!AppSettings.useLevelCards.value) }
                    LaunchedEffect(AppSettings.useLevelCards.value) {
                        invertedUseLevelCards.value = !AppSettings.useLevelCards.value
                    }
                    DualLabelSwitch(
                        state = invertedUseLevelCards,
                        leftText = stringResource(Res.string.world_map_level_cards),
                        rightText = stringResource(Res.string.world_map_image_map),
                        onCheckedChange = { enabled ->
                            AppSettings.saveUseLevelCards(!enabled)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Tile background images switch
                    DualLabelSwitch(
                        state = AppSettings.useTileImages,
                        leftText = stringResource(Res.string.tile_background_images_off),
                        rightText = stringResource(Res.string.tile_background_images_on),
                        onCheckedChange = { enabled ->
                            AppSettings.saveUseTileImages(enabled)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Show testing levels switch
                    GenericSwitch(
                        state = AppSettings.showTestingLevels,
                        checkedText = stringResource(Res.string.show_testing_levels),
                        uncheckedText = stringResource(Res.string.show_testing_levels),
                        onCheckedChange = { enabled ->
                            AppSettings.saveShowTestingLevels(enabled)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Level header text size slider
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.header_text_size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.header_text_size_small),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Slider(
                                value = AppSettings.headerTextSize.value.ordinal.toFloat(),
                                onValueChange = { value ->
                                    val size = when (value.toInt()) {
                                        0 -> HeaderTextSize.SMALL
                                        1 -> HeaderTextSize.MEDIUM
                                        else -> HeaderTextSize.LARGE
                                    }
                                    AppSettings.saveHeaderTextSize(size)
                                },
                                modifier = Modifier.weight(1f),
                                valueRange = 0f..2f,
                                steps = 1
                            )
                            Text(
                                text = stringResource(Res.string.header_text_size_large),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = when (AppSettings.headerTextSize.value) {
                                HeaderTextSize.SMALL -> stringResource(Res.string.header_text_size_small)
                                HeaderTextSize.MEDIUM -> stringResource(Res.string.header_text_size_medium)
                                HeaderTextSize.LARGE -> stringResource(Res.string.header_text_size_large)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
                
                HorizontalDivider()
                
                // Controls section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.controls),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Control pad switch
                    GenericSwitch(
                        state = AppSettings.showControlPad,
                        checkedText = stringResource(Res.string.controls),
                        uncheckedText = stringResource(Res.string.controls),
                        onCheckedChange = { enabled ->
                            AppSettings.saveShowControlPad(enabled)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                HorizontalDivider()
                
                // Sound section
                var showDetailedSoundSettings by remember { mutableStateOf(false) }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.sound),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Overall sound enabled/disabled switch
                    GenericSwitch(
                        state = AppSettings.isSoundEnabled,
                        checkedText = stringResource(Res.string.sound),
                        uncheckedText = stringResource(Res.string.sound),
                        onCheckedChange = { enabled ->
                            AppSettings.saveSoundEnabled(enabled)
                            // Only control playback, don't change child toggle states
                            // Sound effects are controlled by both master AND effects toggle
                            de.egril.defender.audio.GlobalSoundManager.getInstance()?.setEnabled(enabled && AppSettings.isEffectsEnabled.value)
                            // Background music controlled by master AND music toggle
                            // Category-specific logic handled in BackgroundMusicManager.playMusic()
                            if (enabled && AppSettings.isMusicEnabled.value) {
                                // Restart current music if it was playing before
                                val currentMusic = de.egril.defender.audio.GlobalBackgroundMusicManager.getCurrentMusic()
                                if (currentMusic != null) {
                                    de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(currentMusic, loop = true)
                                }
                            } else {
                                de.egril.defender.audio.GlobalBackgroundMusicManager.stopMusic()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Master volume slider (only shown when sound is enabled)
                    if (AppSettings.isSoundEnabled.value) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.sound_volume),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SpeakerLowIcon(size = 20.dp)
                                Slider(
                                    value = AppSettings.soundVolume.value,
                                    onValueChange = { volume ->
                                        AppSettings.saveSoundVolume(volume)
                                    },
                                    modifier = Modifier.weight(1f),
                                    valueRange = 0f..1f
                                )
                                SpeakerHighIcon(size = 20.dp)
                            }
                        }
                        
                        // Button to show/hide detailed sound settings
                        Button(
                            onClick = { showDetailedSoundSettings = !showDetailedSoundSettings },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (showDetailedSoundSettings) {
                                    stringResource(Res.string.hide_detailed_sound_settings)
                                } else {
                                    stringResource(Res.string.show_detailed_sound_settings)
                                }
                            )
                        }
                        
                        // Detailed sound settings (collapsible)
                        if (showDetailedSoundSettings) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                // Effect sounds sub-section
                                Text(
                                    text = stringResource(Res.string.effect_sounds),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                GenericSwitch(
                                    state = AppSettings.isEffectsEnabled,
                                    checkedText = stringResource(Res.string.effects_enabled),
                                    uncheckedText = stringResource(Res.string.effects_disabled),
                                    onCheckedChange = { enabled ->
                                        AppSettings.saveEffectsEnabled(enabled)
                                        de.egril.defender.audio.GlobalSoundManager.getInstance()?.setEnabled(enabled && AppSettings.isSoundEnabled.value)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                if (AppSettings.isEffectsEnabled.value) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.effects_volume),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            SpeakerLowIcon(size = 20.dp)
                                            Slider(
                                                value = AppSettings.effectsVolume.value,
                                                onValueChange = { volume ->
                                                    AppSettings.saveEffectsVolume(volume)
                                                    de.egril.defender.audio.GlobalSoundManager.getInstance()?.setVolume(volume)
                                                },
                                                modifier = Modifier.weight(1f),
                                                valueRange = 0f..1f
                                            )
                                            SpeakerHighIcon(size = 20.dp)
                                        }
                                    }
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                
                                // Background music sub-section
                                Text(
                                    text = stringResource(Res.string.background_music),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                GenericSwitch(
                                    state = AppSettings.isMusicEnabled,
                                    checkedText = stringResource(Res.string.music_enabled),
                                    uncheckedText = stringResource(Res.string.music_disabled),
                                    onCheckedChange = { enabled ->
                                        AppSettings.saveMusicEnabled(enabled)
                                        // Only control playback, don't change child toggle states
                                        if (enabled && AppSettings.isSoundEnabled.value) {
                                            // Restart current music if it was playing before
                                            val currentMusic = de.egril.defender.audio.GlobalBackgroundMusicManager.getCurrentMusic()
                                            if (currentMusic != null) {
                                                de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(currentMusic, loop = true)
                                            }
                                        } else {
                                            de.egril.defender.audio.GlobalBackgroundMusicManager.stopMusic()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                if (AppSettings.isMusicEnabled.value) {
                                    // World Map Music
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.worldmap_music),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        GenericSwitch(
                                            state = AppSettings.isWorldMapMusicEnabled,
                                            checkedText = stringResource(Res.string.worldmap_music_enabled),
                                            uncheckedText = stringResource(Res.string.worldmap_music_disabled),
                                            onCheckedChange = { enabled ->
                                                AppSettings.saveWorldMapMusicEnabled(enabled)
                                                // Always restart world map music if we're on the world map screen
                                                val currentMusic = de.egril.defender.audio.GlobalBackgroundMusicManager.getCurrentMusic()
                                                if (currentMusic == de.egril.defender.audio.BackgroundMusic.WORLD_MAP) {
                                                    if (enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                                                        de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                                                            de.egril.defender.audio.BackgroundMusic.WORLD_MAP,
                                                            loop = true
                                                        )
                                                    } else {
                                                        de.egril.defender.audio.GlobalBackgroundMusicManager.stopMusic()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        if (AppSettings.isWorldMapMusicEnabled.value) {
                                            Text(
                                                text = stringResource(Res.string.worldmap_music_volume),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                SpeakerLowIcon(size = 20.dp)
                                                Slider(
                                                    value = AppSettings.worldMapMusicVolume.value,
                                                    onValueChange = { volume ->
                                                        AppSettings.saveWorldMapMusicVolume(volume)
                                                        // Update volume if world map music is playing
                                                        val currentMusic = de.egril.defender.audio.GlobalBackgroundMusicManager.getCurrentMusic()
                                                        if (currentMusic == de.egril.defender.audio.BackgroundMusic.WORLD_MAP) {
                                                            de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                                                                de.egril.defender.audio.BackgroundMusic.WORLD_MAP,
                                                                loop = true
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    valueRange = 0f..1f
                                                )
                                                SpeakerHighIcon(size = 20.dp)
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Gameplay Music
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.gameplay_music),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        GenericSwitch(
                                            state = AppSettings.isGameplayMusicEnabled,
                                            checkedText = stringResource(Res.string.gameplay_music_enabled),
                                            uncheckedText = stringResource(Res.string.gameplay_music_disabled),
                                            onCheckedChange = { enabled ->
                                                AppSettings.saveGameplayMusicEnabled(enabled)
                                                // Always restart gameplay music if we're in a level
                                                val currentMusic = de.egril.defender.audio.GlobalBackgroundMusicManager.getCurrentMusic()
                                                if (currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL || 
                                                    currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH) {
                                                    if (enabled && AppSettings.isSoundEnabled.value && AppSettings.isMusicEnabled.value) {
                                                        de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                                                            currentMusic,
                                                            loop = true
                                                        )
                                                    } else {
                                                        de.egril.defender.audio.GlobalBackgroundMusicManager.stopMusic()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        if (AppSettings.isGameplayMusicEnabled.value) {
                                            Text(
                                                text = stringResource(Res.string.gameplay_music_volume),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                SpeakerLowIcon(size = 20.dp)
                                                Slider(
                                                    value = AppSettings.gameplayMusicVolume.value,
                                                    onValueChange = { volume ->
                                                        AppSettings.saveGameplayMusicVolume(volume)
                                                        // Update volume if gameplay music is playing
                                                        val currentMusic = de.egril.defender.audio.GlobalBackgroundMusicManager.getCurrentMusic()
                                                        if (currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL || 
                                                            currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH) {
                                                            de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                                                                currentMusic,
                                                                loop = true
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    valueRange = 0f..1f
                                                )
                                                SpeakerHighIcon(size = 20.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider()
                
                // Game Data section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.game_data),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Restore game data button
                    OutlinedButton(
                        onClick = {
                            showRestoreConfirmation = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRestoring
                    ) {
                        Text(stringResource(Res.string.restore_game_data))
                    }
                }
                
                HorizontalDivider()
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset button
                    OutlinedButton(
                        onClick = {
                            AppSettings.resetToDefaults()
                        }
                    ) {
                        Text(stringResource(Res.string.reset_settings))
                    }
                    
                    // Close button
                    Button(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(Res.string.close))
                    }
                }
            }
        }
    }
    
    // Restore confirmation dialog
    if (showRestoreConfirmation) {
        ConfirmationDialog(
            title = stringResource(Res.string.restore_game_data_confirm_title),
            message = stringResource(Res.string.restore_game_data_confirm_message),
            onDismiss = { 
                if (!isRestoring) {
                    showRestoreConfirmation = false
                }
            },
            onConfirm = {
                showRestoreConfirmation = false
                isRestoring = true
                // Perform restore in a coroutine
                scope.launch {
                    val result = RepositoryManager.restoreFromRepository()
                    isRestoring = false
                    if (result != null) {
                        backupPath = result
                        showRestoreSuccess = true
                    }
                }
            }
        )
    }
    
    // Restore success dialog
    if (showRestoreSuccess) {
        AlertDialog(
            onDismissRequest = { showRestoreSuccess = false },
            title = { Text(stringResource(Res.string.restore_game_data_success_title)) },
            text = { 
                Text(
                    stringResource(Res.string.restore_game_data_success_message, backupPath)
                )
            },
            confirmButton = {
                Button(onClick = { 
                    showRestoreSuccess = false
                    // Close the settings dialog after restoration
                    onDismiss()
                }) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }
}
