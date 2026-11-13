package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.Gargoyle
import de.egril.defender.ui.icon.WarningIcon
import de.egril.defender.ui.icon.enemy.GargoyleIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.gargoyle_description
import defender_of_egril.composeapp.generated.resources.gargoyle_mighty_info
import defender_of_egril.composeapp.generated.resources.gargoyle_name
import defender_of_egril.composeapp.generated.resources.health_points
import defender_of_egril.composeapp.generated.resources.level_label

/**
 * Info panel for an inactive (stone) gargoyle. Active gargoyles are attackers and use [AttackerInfo].
 */
@Composable
fun GargoyleInfoPanel(
    gargoyle: Gargoyle,
    isMobile: Boolean = false,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(if (isMobile) 4.dp else 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            val iconSize = if (isMobile) 64.dp else 96.dp
            val iconInnerSize = if (isMobile) 56.dp else 88.dp
            Box(
                modifier = Modifier.size(iconSize),
                contentAlignment = Alignment.Center,
            ) {
                GargoyleIcon(active = false, modifier = Modifier.size(iconInnerSize))
            }

            Spacer(modifier = Modifier.width(if (isMobile) 4.dp else 8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        stringResource(Res.string.gargoyle_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${stringResource(Res.string.level_label)} ${gargoyle.level.value}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GamePlayColors.ErrorDark,
                    )
                }
                Text(
                    "${stringResource(Res.string.health_points)}: ${gargoyle.currentHealth.value}/${gargoyle.maxHealth}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(Res.string.gargoyle_description),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WarningIcon(size = 16.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(Res.string.gargoyle_mighty_info),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = GamePlayColors.ErrorDark,
                    )
                }
            }
        }
    }
}
