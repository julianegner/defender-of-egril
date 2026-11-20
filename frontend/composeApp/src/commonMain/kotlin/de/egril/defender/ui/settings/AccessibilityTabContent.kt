@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

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
internal fun AccessibilityTabContent(
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
internal fun AccessibilityInfoText(text: String) {
    SelectableText(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
