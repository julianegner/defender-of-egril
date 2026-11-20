package de.egril.defender.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.common.SelectableText
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

@Composable
internal fun WorldmapTabContent() {
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
internal fun LevelTabContent() {
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

            // Split build tower button switch
            NumberedSetting(8) {
                GenericSwitch(
                    state = AppSettings.splitBuildTowerButton,
                    checkedText = stringResource(Res.string.split_build_tower_button),
                    uncheckedText = stringResource(Res.string.split_build_tower_button),
                    onCheckedChange = { enabled ->
                        AppSettings.saveSplitBuildTowerButton(enabled)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
