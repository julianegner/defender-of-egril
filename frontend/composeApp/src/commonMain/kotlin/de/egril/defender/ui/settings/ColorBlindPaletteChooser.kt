package de.egril.defender.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.a11y.ColorBlindPalette
import de.egril.defender.ui.common.SelectableText
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

private val COLOR_BLIND_OPTION_LABEL_TOP_PADDING = 10.dp

@Composable
internal fun ColorBlindPaletteChooser(
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
