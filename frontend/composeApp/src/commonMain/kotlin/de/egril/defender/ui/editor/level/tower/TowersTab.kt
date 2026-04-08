package de.egril.defender.ui.editor.level.tower

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.hexagon.TowerIconOnHexagon
import de.egril.defender.ui.getLocalizedName
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.add_all_towers
import defender_of_egril.composeapp.generated.resources.available_towers
import defender_of_egril.composeapp.generated.resources.cost_label
import defender_of_egril.composeapp.generated.resources.damage_label
import defender_of_egril.composeapp.generated.resources.remove_all_towers

/**
 * Tab 3: Available Towers
 */
@Composable
fun TowersTab(
    availableTowers: Set<DefenderType>,
    onAvailableTowersChange: (Set<DefenderType>) -> Unit
) {
    val allTowers = DefenderType.entries.filter { it != DefenderType.DRAGONS_LAIR }
    val hasUnselectedTowers = allTowers.any { !availableTowers.contains(it) }
    val hasSelectedTowers = availableTowers.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Add All / Remove All buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onAvailableTowersChange(allTowers.toSet())
                    },
                    enabled = hasUnselectedTowers,
                    modifier = Modifier.weight(1f)
                ) {
                    SelectableText(stringResource(Res.string.add_all_towers))
                }
                Button(
                    onClick = {
                        onAvailableTowersChange(emptySet())
                    },
                    enabled = hasSelectedTowers,
                    modifier = Modifier.weight(1f)
                ) {
                    SelectableText(stringResource(Res.string.remove_all_towers))
                }
            }
        }

        item {
            SelectableText(
                text = stringResource(Res.string.available_towers),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        items(allTowers) { tower ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = availableTowers.contains(tower),
                    onCheckedChange = { checked ->
                        val newTowers = if (checked) {
                            availableTowers + tower
                        } else {
                            availableTowers - tower
                        }
                        onAvailableTowersChange(newTowers)
                    }
                )
                TowerIconOnHexagon(defenderType = tower)
                SelectableText("${tower.getLocalizedName()} (${stringResource(Res.string.cost_label)}: ${tower.baseCost}, ${stringResource(Res.string.damage_label)}: ${tower.baseDamage})")
            }
        }
    }
}
