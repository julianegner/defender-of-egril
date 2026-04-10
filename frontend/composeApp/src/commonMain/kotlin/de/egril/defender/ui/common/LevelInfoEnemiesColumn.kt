package de.egril.defender.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import de.egril.defender.ui.getLocalizedTitle
import de.egril.defender.ui.getLocalizedSubtitle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.AttackerType
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.icon.HeartIcon
import de.egril.defender.ui.icon.MoneyIcon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import kotlin.collections.component1
import kotlin.collections.component2

data class LevelInfoEnemiesLevelData(
    val id: String,
    val name: String,
    val subtitle: String = "",
    val titleKey: String? = null,  // Optional translation key for the title
    val subtitleKey: String? = null,  // Optional translation key for the subtitle
    val initialCoins: Int,
    val healthPoints: Int,
    val enemyTypeCounts: Map<AttackerType, Int>
)

@Composable
fun RowScope.LevelInfoEnemiesColumn(
    level: LevelInfoEnemiesLevelData,
    textColor: Color
) {

    val enemyList = level.enemyTypeCounts.entries.toList()

    // Left column: Level info, coins, health, and enemies
    Column(
        modifier = Modifier.weight(2f).fillMaxHeight(),
        verticalArrangement = Arrangement.Top
    ) {
        Row {
            // Header: level number and name
            Column {
                Text(
                    text = "Level ${level.id}",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontSize = 18.sp
                )

                val locale = com.hyperether.resources.currentLanguage.value
                Text(
                    text = level.getLocalizedTitle(locale),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14.sp
                )

                // Show subtitle if it's not empty
                val localizedSubtitle = level.getLocalizedSubtitle(locale)
                if (localizedSubtitle.isNotEmpty()) {
                    Text(
                        text = localizedSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.85f),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Coins and Health display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MoneyIcon(size = 12.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${level.initialCoins}",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor,
                            fontSize = 12.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeartIcon(size = 12.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${level.healthPoints}",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row {
            // Enemy units display
            if (enemyList.isNotEmpty()) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    enemyList.forEachIndexed { index, (attackerType, count) ->
                        if (index % 2 == 0) {
                            EnemyUnitEntry(attackerType, count, textColor)
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    enemyList.forEachIndexed { index, (attackerType, count) ->
                        if (index % 2 == 1) {
                            EnemyUnitEntry(attackerType, count, textColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnemyUnitEntry(attackerType: AttackerType, count: Int, textColor: Color) {
    val locale = com.hyperether.resources.currentLanguage.value
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier.size(24.dp)
        ) {
            EnemyTypeIcon(attackerType = attackerType)
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "${attackerType.getLocalizedName(locale)}: ${count}",
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontSize = 11.sp
        )
    }
}
