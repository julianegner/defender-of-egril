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
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

@Composable
internal fun FontSizeSetting(
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
internal fun HeaderTextSizeSetting(
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
internal fun CaptionsSetting() {
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
