package de.egril.defender.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

@Composable
internal fun MousePointerTabContent(
    selectedSliderIndex: Int = 0,
    onSliderIndexChanged: (Int) -> Unit = {},
) {
    val useCustomPointers = AppSettings.mousePointerSource.value != MousePointerSource.SYSTEM
    val useHandPointer = AppSettings.mousePointerSource.value == MousePointerSource.GAME
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.settings_tab_mouse_pointers),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AccessibilityInfoText(stringResource(Res.string.mouse_pointer_accessibility_info))

        NumberedSetting(1) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.mouse_pointer_source),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = AppSettings.mousePointerSource.value == MousePointerSource.GAME,
                                role = Role.RadioButton,
                                onClick = { AppSettings.saveMousePointerSource(MousePointerSource.GAME) },
                            ).testTag("mousePointerSourceGame"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    RadioButton(
                        selected = AppSettings.mousePointerSource.value == MousePointerSource.GAME,
                        onClick = null,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.mouse_pointer_source_game),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(Res.string.mouse_pointer_source_game_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = AppSettings.mousePointerSource.value == MousePointerSource.GAUNTLET,
                                role = Role.RadioButton,
                                onClick = { AppSettings.saveMousePointerSource(MousePointerSource.GAUNTLET) },
                            ).testTag("mousePointerSourceGauntlet"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    RadioButton(
                        selected = AppSettings.mousePointerSource.value == MousePointerSource.GAUNTLET,
                        onClick = null,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.mouse_pointer_source_gauntlet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(Res.string.mouse_pointer_source_gauntlet_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = AppSettings.mousePointerSource.value == MousePointerSource.SYSTEM,
                                role = Role.RadioButton,
                                onClick = { AppSettings.saveMousePointerSource(MousePointerSource.SYSTEM) },
                            ).testTag("mousePointerSourceSystem"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    RadioButton(
                        selected = AppSettings.mousePointerSource.value == MousePointerSource.SYSTEM,
                        onClick = null,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.mouse_pointer_source_system),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(Res.string.mouse_pointer_source_system_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        NumberedSetting(2) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.mouse_pointer_direction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                DualLabelSwitch(
                    checked = AppSettings.mousePointerDirection.value == MousePointerDirection.RIGHT,
                    leftText = stringResource(Res.string.mouse_pointer_direction_left),
                    rightText = stringResource(Res.string.mouse_pointer_direction_right),
                    onCheckedChange = { checked ->
                        AppSettings.saveMousePointerDirection(
                            if (checked) {
                                MousePointerDirection.RIGHT
                            } else {
                                MousePointerDirection.LEFT
                            },
                        )
                    },
                    enabled = useCustomPointers,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (selectedSliderIndex == 0) {
                            Modifier
                                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        } else {
                            Modifier
                        },
                    ).clickable { onSliderIndexChanged(0) }
                    .testTag("mousePointerSizeControl"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.mouse_pointer_size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (AppSettings.showButtonShortcutHints.value && selectedSliderIndex == 0) {
                    Text(
                        text = stringResource(Res.string.mouse_pointer_size_shortcut_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            val sizeEntries = MousePointerSize.entries
            Slider(
                value =
                    AppSettings.mousePointerSize.value.ordinal
                        .toFloat(),
                onValueChange = { value ->
                    val next = sizeEntries[value.toInt().coerceIn(0, sizeEntries.lastIndex)]
                    AppSettings.saveMousePointerSize(next)
                },
                modifier = Modifier.fillMaxWidth().testTag("mousePointerSizeSlider"),
                valueRange = 0f..(sizeEntries.lastIndex).toFloat(),
                steps = sizeEntries.size - 2,
                enabled = useCustomPointers,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(Res.string.mouse_pointer_size_small), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(Res.string.mouse_pointer_size_large), style = MaterialTheme.typography.bodySmall)
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (useHandPointer && selectedSliderIndex == 1) {
                            Modifier
                                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        } else {
                            Modifier
                        },
                    ).then(
                        if (useHandPointer) {
                            Modifier.clickable { onSliderIndexChanged(1) }
                        } else {
                            Modifier
                        },
                    ).testTag("mousePointerBrightnessControl"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.mouse_pointer_skin_brightness),
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (useHandPointer) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                )
                if (useHandPointer && AppSettings.showButtonShortcutHints.value && selectedSliderIndex == 1) {
                    Text(
                        text = stringResource(Res.string.mouse_pointer_skin_brightness_shortcut_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Slider(
                value = AppSettings.mousePointerSkinBrightness.value,
                onValueChange = { AppSettings.saveMousePointerSkinBrightness(it) },
                modifier = Modifier.fillMaxWidth().testTag("mousePointerBrightnessSlider"),
                valueRange = AppSettings.MOUSE_POINTER_SKIN_BRIGHTNESS_MIN..AppSettings.MOUSE_POINTER_SKIN_BRIGHTNESS_MAX,
                steps = ((AppSettings.MOUSE_POINTER_SKIN_BRIGHTNESS_MAX - AppSettings.MOUSE_POINTER_SKIN_BRIGHTNESS_MIN) / 0.05f).toInt() - 1,
                enabled = useHandPointer,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(Res.string.mouse_pointer_skin_darker),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (useHandPointer) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                )
                Text(
                    text = stringResource(Res.string.mouse_pointer_skin_brighter),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (useHandPointer) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                )
            }
        }

        AccessibilityInfoText(stringResource(Res.string.mouse_pointer_info))
    }
}
