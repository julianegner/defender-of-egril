@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.icon.SpeakerHighIcon
import de.egril.defender.ui.icon.SpeakerLowIcon
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

/**
 * Background music part of the sound tab: music switch, music volume and the
 * world map / gameplay music sub-sections.
 */
@Composable
internal fun SoundBackgroundMusicSection(
    selectedVolumeIndex: Int,
    onVolumeIndexChanged: (Int) -> Unit,
    onManualBackgroundMusicStart: () -> Unit,
) {
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
