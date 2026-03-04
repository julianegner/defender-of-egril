package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.egril.defender.model.Barricade
import de.egril.defender.model.Position
import de.egril.defender.ui.localizeEntityName
import de.egril.defender.ui.icon.GateIcon
import de.egril.defender.ui.icon.WoodIcon
import com.hyperether.resources.stringResource
import com.hyperether.resources.currentLanguage
import defender_of_egril.composeapp.generated.resources.*

/**
 * Info panel shown when the player clicks on a barricade or gate.
 * Displays the icon, name (if set), HP, description, and a Remove button.
 *
 * Replaces the old "Remove Barricade?" confirmation dialog.
 */
@Composable
fun BarricadeInfoPanel(
    position: Position,
    barricade: Barricade,
    isMobile: Boolean = false,
    onRemove: () -> Unit
) {
    val locale = currentLanguage.value
    val localizedName = barricade.name?.takeIf { it.isNotBlank() }
        ?.let { localizeEntityName(it, locale) }
    val isGate = barricade.isGate

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(if (isMobile) 4.dp else 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Icon: gate or wood barricade
            val iconSize = if (isMobile) 64.dp else 96.dp
            val iconInnerSize = if (isMobile) 48.dp else 72.dp
            Box(
                modifier = Modifier.size(iconSize),
                contentAlignment = Alignment.Center
            ) {
                if (isGate) {
                    GateIcon(size = iconInnerSize)
                } else {
                    WoodIcon(size = iconInnerSize)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Name, HP, description column
            Column(modifier = Modifier.weight(1f)) {
                // Title: named or generic (localizedName already has \n for multi-line; no need to replace)
                val title = if (!localizedName.isNullOrBlank()) {
                    localizedName
                } else if (isGate) {
                    stringResource(Res.string.gate_info_panel_title)
                } else {
                    stringResource(Res.string.barricade_info_panel_title)
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // HP row — use string template instead of format()
                Text(
                    text = "${stringResource(Res.string.health_points)}: ${barricade.healthPoints.value}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF795548)
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Description
                val description = if (isGate) {
                    stringResource(Res.string.gate_info_description)
                } else {
                    stringResource(Res.string.barricade_info_description)
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Remove button
            Button(
                onClick = onRemove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(stringResource(Res.string.remove_barricade))
            }
        }
    }
}
