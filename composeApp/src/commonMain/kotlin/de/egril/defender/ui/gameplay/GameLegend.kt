package de.egril.defender.ui.gameplay

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
import de.egril.defender.model.*
import de.egril.defender.ui.*
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleLeftIcon
import de.egril.defender.ui.icon.enemy.EnemyIcon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun GameLegend(modifier: Modifier = Modifier) {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value
    val borderColor = if (isDarkMode) Color(0xFF303030) else Color.Gray  // Even darker border in dark mode
    
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
                    "${stringResource(Res.string.areas_label)}:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                LegendItemHex(
                            color = GamePlayColors.BuildIsland,
                            label = "",
                            description = stringResource(Res.string.build_island),
                            border = borderColor,
                            tileType = de.egril.defender.editor.TileType.ISLAND
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.BuildStrip,
                            label = "",
                            description = stringResource(Res.string.build_strip),
                            border = borderColor,
                            tileType = de.egril.defender.editor.TileType.BUILD_AREA
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.Path,
                            label = "", // ⬡
                            description = stringResource(Res.string.enemy_path),
                            border = borderColor,
                            tileType = de.egril.defender.editor.TileType.PATH
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.River,
                            label = "",
                            description = stringResource(Res.string.river),
                            border = borderColor,
                            tileType = de.egril.defender.editor.TileType.RIVER
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.NonPlayable,
                            label = "",
                            description = stringResource(Res.string.non_playable),
                            border = borderColor,
                            tileType = de.egril.defender.editor.TileType.NO_PLAY
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${stringResource(Res.string.special_label)}:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        LegendItemHex(
                            color = GamePlayColors.Path,
                            label = stringResource(Res.string.spawn),
                            description = stringResource(Res.string.spawn_desc),
                            border = GamePlayColors.Warning,
                            borderWidth = 3.dp
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.Path,
                            label = stringResource(Res.string.target),
                            description = stringResource(Res.string.target_desc),
                            border = GamePlayColors.Success,
                            borderWidth = 3.dp
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${stringResource(Res.string.units_label)}:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        LegendItemHex(
                            color = GamePlayColors.Info,
                            label = "",
                            description = stringResource(Res.string.tower_ready),
                            border = GamePlayColors.Info,
                            borderWidth = 3.dp
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.Building,
                            label = "",
                            description = stringResource(Res.string.tower_building),
                            border = GamePlayColors.Building,
                            borderWidth = 3.dp
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.InfoLight,
                            label = "",
                            description = stringResource(Res.string.tower_no_actions),
                            border = GamePlayColors.Info,
                            borderWidth = 3.dp
                        )
                    }
                    item {
                        LegendItemHex(
                            color = GamePlayColors.Error,
                            label = "",
                            description = stringResource(Res.string.enemy_unit),
                            border = GamePlayColors.Error,
                            borderWidth = 3.dp
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${stringResource(Res.string.info_label)}:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Text(
                            "• ${stringResource(Res.string.legend_info_ballista)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GamePlayColors.WarningDark
                        )
                    }
                    item {
                        Text("• ${stringResource(Res.string.legend_info_tower_icons)}", style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        Text(
                            "• ${stringResource(Res.string.legend_info_tower_actions)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    item {
                        Text("• ${stringResource(Res.string.legend_info_enemy_health)}", style = MaterialTheme.typography.bodySmall)
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
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    tileType: de.egril.defender.editor.TileType? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Get tile painter if tile type is provided
        val tilePainter = tileType?.let { TileImageProvider.getTilePainter(it) }
        
        Box(
            modifier = Modifier
                .size(32.dp, 28.dp)
                .clip(HexagonShape())
                .background(color)
                .border(borderWidth, border, HexagonShape()),
            contentAlignment = Alignment.Center
        ) {
            // Show tile image if available, otherwise show label
            tilePainter?.let { painter ->
                androidx.compose.foundation.Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } ?: run {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 5.sp,
                    color = border
                )
            }
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

    ExpandableCard(
        title = stringResource(Res.string.enemies),
        modifier = modifier,
        defaultExpanded = true
    ) {
        Text(
            "${stringResource(Res.string.active_label)}: ${activeEnemies.size} | ${stringResource(Res.string.planned_label)}: ${plannedSpawns.size}",
            style = MaterialTheme.typography.bodySmall
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Active enemies on the map
            if (activeEnemies.isNotEmpty()) {
                item(key = "header-active") {
                    Text(
                        "${stringResource(Res.string.on_map)}:",
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
                        "${stringResource(Res.string.planned_spawns)}:",
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

@Composable
fun EnemyItemDetailed(attacker: Attacker, showPosition: Boolean) {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        attacker.type.getLocalizedName(locale),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (attacker.level.value > 1) {
                        Text(
                            "Lvl ${attacker.level.value}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GamePlayColors.ErrorDark
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${stringResource(Res.string.hp_short)}: ${attacker.currentHealth.value}/${attacker.maxHealth}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp
                    )
                    if (showPosition) {
                        Text(
                            "Pos: (${attacker.position.value.x},${attacker.position.value.y})",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = if (isDarkMode) GamePlayColors.InfoLight else GamePlayColors.InfoDark
                        )
                    }
                }
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
                            "Lvl ${plannedSpawn.level}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GamePlayColors.ErrorDark
                        )
                    }
                }
                Text(
                    "${stringResource(Res.string.hp_short)}: ${plannedSpawn.healthPoints}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp
                )
            }

            // Spawn turn
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${stringResource(Res.string.turn)} ${plannedSpawn.spawnTurn}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (plannedSpawn.spawnTurn == currentTurn + 1) GamePlayColors.WarningDeep else GamePlayColors.Warning
                )
                if (plannedSpawn.spawnTurn > currentTurn) {
                    Text(
                        stringResource(Res.string.in_x_turns, (plannedSpawn.spawnTurn - currentTurn).toString()),
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
                "${stringResource(Res.string.hp_short)}: ${attacker.currentHealth.value}/${attacker.maxHealth}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "${stringResource(Res.string.reward)}: ${attacker.type.reward} coins",
                style = MaterialTheme.typography.bodySmall,
                color = GamePlayColors.Warning
            )

            Text(
                "${stringResource(Res.string.position_label)}: (${attacker.position.value.x}, ${attacker.position.value.y})",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
