package de.egril.defender.ui.editor.level

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.currentLanguage
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorLevel
import de.egril.defender.ui.getLocalizedDescription
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.getLocalizedTitle
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.back_to_levels
import defender_of_egril.composeapp.generated.resources.name_label
import defender_of_egril.composeapp.generated.resources.not_used_in_any_level
import defender_of_egril.composeapp.generated.resources.short_description
import defender_of_egril.composeapp.generated.resources.used_in_levels
import defender_of_egril.composeapp.generated.resources.villain
import defender_of_egril.composeapp.generated.resources.villain_usage

@Composable
internal fun VillainUsagePage(
    levels: List<EditorLevel>,
    onBack: () -> Unit,
) {
    val usageEntries = remember(levels) { villainUsageEntries(levels) }
    val locale = currentLanguage.value

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.villain_usage),
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = onBack) {
                Text(stringResource(Res.string.back_to_levels))
            }
        }

        VillainUsageHeaderRow()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(usageEntries) { entry ->
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            EnemyTypeIcon(
                                attackerType = entry.villainType,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Column(modifier = Modifier.weight(0.9f)) {
                            Text(
                                text = entry.villainType.getLocalizedName(locale),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Column(modifier = Modifier.weight(1.6f)) {
                            Text(
                                text = entry.villainType.getLocalizedDescription(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Column(modifier = Modifier.weight(1.5f)) {
                            if (entry.levels.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.not_used_in_any_level),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                entry.levels.forEach { level ->
                                    Text(
                                        text = "${level.getLocalizedTitle(locale)} (${level.id})",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VillainUsageHeaderRow() {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(Res.string.villain),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stringResource(Res.string.name_label),
                modifier = Modifier.weight(0.9f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.short_description),
                modifier = Modifier.weight(1.6f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.used_in_levels),
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
