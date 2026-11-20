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
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.icon.SpeakerHighIcon
import de.egril.defender.ui.icon.SpeakerLowIcon
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

/**
 * Sound tab: All sound settings.
 */
@Composable
internal fun SoundTabContent(
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
                            dev.vicart.compose.material.symbols.MaterialSymbol.Filled(
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

                    SoundBackgroundMusicSection(
                        selectedVolumeIndex = selectedVolumeIndex,
                        onVolumeIndexChanged = onVolumeIndexChanged,
                        onManualBackgroundMusicStart = onManualBackgroundMusicStart,
                    )
                }
            }
        }
    }
}
