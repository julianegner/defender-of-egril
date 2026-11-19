package de.egril.defender.ui.editor.level

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorMap
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import de.egril.defender.model.isRealVillain
import de.egril.defender.model.isSpecialEnemy
import de.egril.defender.ui.*
import de.egril.defender.ui.hexagon.EnemyIconOnHexagon
import defender_of_egril.composeapp.generated.resources.*

private enum class EnemyCategory {
    REGULAR,
    SPECIAL,
    VILLAIN,
}

/**
 * Dialog for adding an enemy to a specific turn
 */
@Composable
fun AddEnemyDialog(
    presentVillainTypes: Set<AttackerType> = emptySet(),
    turn: Int,
    map: EditorMap?,
    onDismiss: () -> Unit,
    onAdd: (AttackerType, Int, Int, Position?) -> Unit, // Added spawnPoint parameter
    initialType: AttackerType = AttackerType.GOBLIN,
    initialLevel: String = "1",
    initialAmount: String = "1",
    initialSpawnPoint: Position? = null,
) {
    var selectedType by remember(map, initialType) {
        mutableStateOf(
            initialType.takeIf { map?.getCompatibleSpawnPoints(it)?.isNotEmpty() == true }
                ?: AttackerType.entries.firstOrNull {
                    !it.isMirrorImage &&
                        !it.isRealVillain &&
                        !it.isSpecialEnemy() &&
                        map?.getCompatibleSpawnPoints(it)?.isNotEmpty() == true
                } ?: initialType,
        )
    }
    var level by remember { mutableStateOf(initialLevel) }
    var amount by remember { mutableStateOf(initialAmount) }

    // Villains are unique enemy "heroes": they are listed separately, may only be added once per
    // level, and never use the amount field (see issue #538).
    val spawnableTypes = remember { AttackerType.entries.filterNot { it.isMirrorImage } }
    val specialTypes = remember { spawnableTypes.filter { it.isSpecialEnemy() }.toSet() }
    val regularTypes = remember { spawnableTypes.filter { !it.isRealVillain && !it.isSpecialEnemy() } }
    val shownSpecialTypes = remember { spawnableTypes.filter { it.isSpecialEnemy() } }
    val villainTypes = remember { spawnableTypes.filter { it.isRealVillain } }
    val isVillainSelected = selectedType.isRealVillain
    var selectedCategory by remember { mutableStateOf(EnemyCategory.REGULAR) }

    LaunchedEffect(selectedType) {
        selectedCategory =
            when {
                selectedType.isRealVillain -> EnemyCategory.VILLAIN
                selectedType in specialTypes -> EnemyCategory.SPECIAL
                else -> EnemyCategory.REGULAR
            }
    }

    // Get spawn points from the map that are compatible with the selected enemy type.
    val compatibleSpawnPoints = remember(map, selectedType) { map?.getCompatibleSpawnPoints(selectedType) ?: emptyList() }
    var selectedSpawnPoint by remember { mutableStateOf<Position?>(initialSpawnPoint) }

    LaunchedEffect(compatibleSpawnPoints) {
        selectedSpawnPoint =
            selectedSpawnPoint?.takeIf { it in compatibleSpawnPoints }
                ?: compatibleSpawnPoints.firstOrNull()
    }

    // State for spawn point selection dialog
    var showSpawnPointDialog by remember { mutableStateOf(false) }

    // A villain can only be added if one of the same type is not already in the level.
    val villainAlreadyInLevel = isVillainSelected && selectedType in presentVillainTypes
    val canAdd = !villainAlreadyInLevel && selectedSpawnPoint in compatibleSpawnPoints

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_enemy_to_turn, turn)) },
        text = {
            Column {
                Text(stringResource(Res.string.enemy_type), modifier = Modifier.padding(bottom = 4.dp))
                PrimaryTabRow(selectedTabIndex = selectedCategory.ordinal, modifier = Modifier.padding(bottom = 8.dp)) {
                    Tab(
                        selected = selectedCategory == EnemyCategory.REGULAR,
                        onClick = { selectedCategory = EnemyCategory.REGULAR },
                        text = { Text(stringResource(Res.string.enemies)) },
                    )
                    Tab(
                        selected = selectedCategory == EnemyCategory.SPECIAL,
                        onClick = { selectedCategory = EnemyCategory.SPECIAL },
                        text = { Text(stringResource(Res.string.special_label)) },
                    )
                    Tab(
                        selected = selectedCategory == EnemyCategory.VILLAIN,
                        onClick = { selectedCategory = EnemyCategory.VILLAIN },
                        text = { Text(stringResource(Res.string.villains)) },
                    )
                }
                LazyColumn(
                    modifier = Modifier.height(150.dp).padding(bottom = 8.dp),
                ) {
                    when (selectedCategory) {
                        EnemyCategory.REGULAR -> {
                            items(regularTypes) { type ->
                                EnemyTypeSelectableRow(
                                    type = type,
                                    selected = selectedType == type,
                                    enabled = map?.getCompatibleSpawnPoints(type)?.isNotEmpty() == true,
                                    onSelect = { selectedType = type },
                                )
                            }
                        }

                        EnemyCategory.SPECIAL -> {
                            items(shownSpecialTypes) { type ->
                                EnemyTypeSelectableRow(
                                    type = type,
                                    selected = selectedType == type,
                                    enabled = map?.getCompatibleSpawnPoints(type)?.isNotEmpty() == true,
                                    onSelect = { selectedType = type },
                                )
                            }
                        }

                        EnemyCategory.VILLAIN -> {
                            items(villainTypes) { type ->
                                val alreadyPresent = type in presentVillainTypes
                                EnemyTypeSelectableRow(
                                    type = type,
                                    selected = selectedType == type,
                                    enabled = !alreadyPresent && map?.getCompatibleSpawnPoints(type)?.isNotEmpty() == true,
                                    onSelect = { selectedType = type },
                                )
                            }
                        }
                    }
                }
                // Villains are unique per level, so they never use the amount field.
                if (isVillainSelected) {
                    OutlinedTextField(
                        value = level,
                        onValueChange = { if (it.all { c -> c.isDigit() }) level = it },
                        label = { Text(stringResource(Res.string.enemy_level)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = level,
                            onValueChange = { if (it.all { c -> c.isDigit() }) level = it },
                            label = { Text(stringResource(Res.string.enemy_level)) },
                            modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.isNotEmpty()) amount = it },
                            label = { Text(stringResource(Res.string.amount)) },
                            modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                        )
                    }
                }

                // Spawn point selection button (opens dialog with minimap)
                if (compatibleSpawnPoints.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.spawn_point),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )

                    OutlinedButton(
                        onClick = { showSpawnPointDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text =
                                selectedSpawnPoint?.let { "Position (${it.x}, ${it.y})" }
                                    ?: stringResource(Res.string.select_spawn_point),
                        )
                    }
                }

                Text(stringResource(Res.string.hp_with_level, selectedType.health * (level.toIntOrNull() ?: 1)), fontSize = 12.sp)
                if (villainAlreadyInLevel) {
                    Text(
                        text = stringResource(Res.string.villain_once_warning),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Villains are unique per level, so their amount is always 1.
                    val amountValue = if (isVillainSelected) 1 else (amount.toIntOrNull() ?: 1)
                    onAdd(selectedType, level.toIntOrNull() ?: 1, amountValue, selectedSpawnPoint)
                },
                enabled = canAdd,
            ) {
                Text(stringResource(Res.string.add))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )

    // Spawn point selection dialog with minimap
    if (showSpawnPointDialog) {
        SelectSpawnPointDialog(
            selectedType = selectedType,
            level = level.toIntOrNull() ?: 1,
            map = map,
            currentSelection = selectedSpawnPoint,
            onDismiss = { showSpawnPointDialog = false },
            onSelect = { point ->
                selectedSpawnPoint = point
                showSpawnPointDialog = false
            },
        )
    }
}

/**
 * A single selectable enemy/villain row (radio button, icon, and name) used in [AddEnemyDialog].
 * When [enabled] is false the row is shown greyed out and cannot be selected (e.g. a villain that is
 * already present in the level).
 */
@Composable
private fun EnemyTypeSelectableRow(
    type: AttackerType,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (enabled) Modifier.clickable { onSelect() } else Modifier)
                .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = if (enabled) onSelect else null,
            enabled = enabled,
        )
        // Add enemy icon in hexagon
        Box(modifier = Modifier.size(24.dp)) {
            EnemyIconOnHexagon(
                attackerType = type,
                size = 24.dp,
            )
        }
        val label = "${type.getLocalizedName()} (${stringResource(Res.string.hp_label)}: ${type.health})"
        if (enabled) {
            Text(label)
        } else {
            Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
        }
    }
}
