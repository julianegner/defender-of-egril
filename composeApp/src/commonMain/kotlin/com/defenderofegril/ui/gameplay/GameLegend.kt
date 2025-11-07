package com.defenderofegril.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.model.*
import com.defenderofegril.ui.*
import com.defenderofegril.ui.icon.TriangleDownIcon
import com.defenderofegril.ui.icon.TriangleLeftIcon
import com.defenderofegril.ui.icon.enemy.EnemyIcon
import com.defenderofegril.ui.icon.enemy.EnemyTypeIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun GameLegend(modifier: Modifier = Modifier) {
    ExpandableCard(
        title = stringResource(Res.string.legend),
        modifier = modifier,
        defaultExpanded = false
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(Res.string.areas_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                LegendItemHex(
                            color = GamePlayColors.BuildIsland,
                            label = "⬡",
                            description = stringResource(Res.string.build_island),
                            border = Color.Gray
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.BuildStrip,
                            label = "⬡",
                            description = stringResource(Res.string.build_strip),
                            border = Color.Gray
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.Path,
                            label = "⬡",
                            description = stringResource(Res.string.enemy_path),
                            border = Color.Gray
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.NonPlayable,
                            label = "⬡",
                            description = stringResource(Res.string.non_playable),
                            border = Color.Gray
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Special:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        LegendItemHex(
                            color = GamePlayColors.Path,
                            label = "Spawn",
                            description = "Spawn (3 points)",
                            border = GamePlayColors.Warning,
                            borderWidth = 3.dp
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.Path,
                            label = "Target",
                            description = "Target (Defend!)",
                            border = GamePlayColors.Success,
                            borderWidth = 3.dp
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Units:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        LegendItemHex(
                            color = GamePlayColors.Info,
                            label = "⬡",
                            description = "Tower (Ready)",
                            border = GamePlayColors.Info,
                            borderWidth = 3.dp
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.Building,
                            label = "⬡",
                            description = "Tower (Building)",
                            border = GamePlayColors.Building,
                            borderWidth = 3.dp
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.InfoLight,
                            label = "⬡",
                            description = "Tower (No Actions)",
                            border = GamePlayColors.Info,
                            borderWidth = 3.dp
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.Error,
                            label = "⬡",
                            description = stringResource(Res.string.enemy_unit),
                            border = GamePlayColors.Error,
                            borderWidth = 3.dp
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Info:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Text(
                            "• Ballista: min range 3",
                            style = MaterialTheme.typography.bodySmall,
                            color = GamePlayColors.WarningDark
                        )
                    }
                    item {
                        Text("• Icons show tower/enemy type", style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        Text(
                            "• Level & actions shown on towers",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    item {
                        Text("• Health shown on enemies", style = MaterialTheme.typography.bodySmall)
                    }
                }
    }
}

@Composable
fun LegendItemHex(
    color: Color,
    label: String,
    description: String,
    border: Color = Color.Gray,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp, 28.dp)
                .clip(HexagonShape())
                .background(color)
                .border(borderWidth, border, HexagonShape()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 14.sp,
                color = if (color.luminance() > 0.5f) Color.Black else Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(description, style = MaterialTheme.typography.bodySmall)
    }
}

// Extension function to calculate color luminance
private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}

@Composable
fun EnemyListPanel(gameState: GameState, modifier: Modifier = Modifier) {
    // Expand by default during initial building phase, collapsed otherwise
    // This state is remembered per GameState instance (each level has its own GameState)
    // so it will auto-expand when a new level is loaded
    var isExpanded by remember { mutableStateOf(gameState.phase.value == GamePhase.INITIAL_BUILDING) }

    // LazyListState to control scrolling
    val listState = rememberLazyListState()

    // Scroll to top when turn changes
    val currentTurn = gameState.turnNumber.value
    LaunchedEffect(currentTurn) {
        if (isExpanded && currentTurn > 0) {
            listState.animateScrollToItem(0)
        }
    }

    // Compute values directly - parent GamePlayScreen's key() will trigger recomposition
    val activeEnemies = gameState.attackers.filter { !it.isDefeated.value }.sortedBy { it.id }

    // Calculate how many enemies have spawned from the spawn plan
    // nextAttackerId starts at 1, so (nextAttackerId - 1) gives us the count of spawned enemies
    val totalSpawned = gameState.nextAttackerId.value - 1

    // Get the remaining planned spawns (those that haven't spawned yet)
    val plannedSpawns = gameState.spawnPlan.drop(totalSpawned)//.take(15)

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enemies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isExpanded) {
                    TriangleDownIcon(size = 20.dp)
                } else {
                    TriangleLeftIcon(size = 20.dp)
                }
            }
            Text(
                "Active: ${activeEnemies.size} | Planned: ${plannedSpawns.size}",
                style = MaterialTheme.typography.bodySmall
            )

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
                ) {
                    // Active enemies on the map
                    if (activeEnemies.isNotEmpty()) {
                        item(key = "header-active") {
                            Text(
                                "On Map:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = GamePlayColors.ErrorDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    items(
                        items = activeEnemies,
                        key = { attacker -> "active-${attacker.id}" }
                    ) { attacker ->
                        // Key by id, position and health to force recomposition when enemy moves or takes damage
                        key(
                            attacker.id,
                            attacker.position.value.x,
                            attacker.position.value.y,
                            attacker.currentHealth.value
                        ) {
                            EnemyItemDetailed(attacker, showPosition = true)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Planned enemy spawns (show what's left to spawn with turn information)
                    if (plannedSpawns.isNotEmpty()) {
                        item(key = "header-planned") {
                            if (activeEnemies.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                "Planned Spawns:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = GamePlayColors.Warning
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        itemsIndexed(
                            items = plannedSpawns,
                            key = { index, _ -> "planned-$index" }
                        ) { index, plannedSpawn ->
                            PlannedEnemyItem(plannedSpawn, gameState.turnNumber.value)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnemyItemDetailed(attacker: Attacker, showPosition: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = GamePlayColors.EnemyCardBackground
        )
    ) {
        Row(
            modifier = Modifier.padding(6.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enemy icon (small version)
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                EnemyIcon(attacker = attacker, modifier = Modifier.size(28.dp), healthTextColor = Color.Black)
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Enemy details
            Column(modifier = Modifier.weight(1f)) {
                val locale = com.hyperether.resources.currentLanguage.value
                Text(
                    attacker.type.getLocalizedName(locale),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "HP: ${attacker.currentHealth.value}/${attacker.maxHealth}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp
                    )
                    if (showPosition) {
                        Text(
                            "Pos: (${attacker.position.value.x},${attacker.position.value.y})",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = GamePlayColors.InfoDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingEnemyItem(attackerType: AttackerType) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = GamePlayColors.UpcomingCardBackground
        )
    ) {
        Row(
            modifier = Modifier.padding(6.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enemy type icon using graphical representation
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                EnemyTypeIcon(attackerType = attackerType, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Enemy details
            Column(modifier = Modifier.weight(1f)) {
                val locale = com.hyperether.resources.currentLanguage.value
                Text(
                    attackerType.getLocalizedName(locale),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${stringResource(Res.string.hp_label)}: ${attackerType.health}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun PlannedEnemyItem(plannedSpawn: PlannedEnemySpawn, currentTurn: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = GamePlayColors.UpcomingCardBackground
        )
    ) {
        Row(
            modifier = Modifier.padding(6.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enemy type icon using graphical representation
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                EnemyTypeIcon(attackerType = plannedSpawn.attackerType, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Enemy details
            Column(modifier = Modifier.weight(1f)) {
                val locale = com.hyperether.resources.currentLanguage.value
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        plannedSpawn.attackerType.getLocalizedName(locale),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (plannedSpawn.level > 1) {
                        Text(
                            "Lv${plannedSpawn.level}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GamePlayColors.ErrorDark
                        )
                    }
                }
                Text(
                    "HP: ${plannedSpawn.healthPoints}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp
                )
            }

            // Spawn turn
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Turn ${plannedSpawn.spawnTurn}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (plannedSpawn.spawnTurn == currentTurn + 1) GamePlayColors.WarningDeep else GamePlayColors.Warning
                )
                if (plannedSpawn.spawnTurn > currentTurn) {
                    Text(
                        "in ${plannedSpawn.spawnTurn - currentTurn} turns",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun EnemyItem(attacker: Attacker) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GamePlayColors.EnemyCardBackground)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            val locale = com.hyperether.resources.currentLanguage.value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    attacker.type.getLocalizedName(locale),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    "ID: ${attacker.id}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                "HP: ${attacker.currentHealth.value}/${attacker.maxHealth}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "Reward: ${attacker.type.reward} coins",
                style = MaterialTheme.typography.bodySmall,
                color = GamePlayColors.Warning
            )

            Text(
                "Position: (${attacker.position.value.x}, ${attacker.position.value.y})",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
