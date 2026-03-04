package de.egril.defender.ui.editor.level.initialsetup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.*
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import defender_of_egril.composeapp.generated.resources.*

/**
 * Tab 5: Initial Setup - Place towers, enemies, traps, and barricades before level starts
 * New unified interface with map on left and configuration sidebar on right
 */
@Composable
fun InitialSetupTab(
    initialData: InitialData,
    onInitialDataChange: (InitialData) -> Unit,
    map: EditorMap?,
    availableTowers: Set<DefenderType>
) {
    if (map == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(Res.string.no_map_selected),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }
    
    var placementMode by remember { mutableStateOf<PlacementMode?>(null) }
    var selectedDefenderType by remember { mutableStateOf(DefenderType.SPIKE_TOWER) }
    var selectedDefenderLevel by remember { mutableStateOf(1) }
    var showAllTowers by remember { mutableStateOf(false) }
    var dragonName by remember { mutableStateOf("") }
    
    var selectedAttackerType by remember { mutableStateOf(AttackerType.GOBLIN) }
    var selectedAttackerLevel by remember { mutableStateOf(1) }
    var customHealth by remember { mutableStateOf<Int?>(null) }
    var attackerDragonName by remember { mutableStateOf("") }
    
    var selectedTrapType by remember { mutableStateOf("DWARVEN") }
    var trapDamage by remember { mutableStateOf(10) }
    
    var barricadeHealthPoints by remember { mutableStateOf(10) }
    var barricadeName by remember { mutableStateOf("") }
    var barricadeIsGate by remember { mutableStateOf(false) }
    
    var editBarricadeIndex by remember { mutableStateOf<Int?>(null) }  // Index of barricade being edited
    
    var selectedElement by remember { mutableStateOf<SelectedElement?>(null) }
    
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left side: Map with placed elements
        Card(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.initial_setup_map),
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (placementMode != null) {
                    Text(
                        text = stringResource(Res.string.initial_setup_click_to_place),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    InitialSetupMinimap(
                        map = map,
                        placementMode = placementMode,
                        initialData = initialData,
                        selectedElement = selectedElement,
                        onTileClick = { position ->
                            when (placementMode) {
                                PlacementMode.DEFENDER -> {
                                    if (canPlaceDefender(position, initialData, map)) {
                                        val newDefender = InitialDefender(
                                            type = selectedDefenderType,
                                            position = position,
                                            level = selectedDefenderLevel,
                                            dragonName = if (selectedDefenderType == DefenderType.DRAGONS_LAIR && dragonName.isNotBlank()) dragonName else null
                                        )
                                        onInitialDataChange(initialData.copy(defenders = initialData.defenders + newDefender))
                                    }
                                }
                                PlacementMode.ATTACKER -> {
                                    if (canPlaceAttacker(position, initialData, map)) {
                                        val newAttacker = InitialAttacker(
                                            type = selectedAttackerType,
                                            position = position,
                                            level = selectedAttackerLevel,
                                            currentHealth = customHealth,
                                            dragonName = if (selectedAttackerType == AttackerType.DRAGON && attackerDragonName.isNotBlank()) attackerDragonName else null
                                        )
                                        onInitialDataChange(initialData.copy(attackers = initialData.attackers + newAttacker))
                                    }
                                }
                                PlacementMode.TRAP -> {
                                    if (canPlaceTrap(position, initialData, map)) {
                                        val newTrap = InitialTrap(
                                            position = position,
                                            damage = trapDamage,
                                            type = selectedTrapType
                                        )
                                        onInitialDataChange(initialData.copy(traps = initialData.traps + newTrap))
                                    }
                                }
                                PlacementMode.BARRICADE -> {
                                    // Check if there's already a barricade at this position
                                    val existingIndex = initialData.barricades.indexOfFirst { it.position == position }
                                    if (existingIndex >= 0) {
                                        // Edit existing barricade
                                        editBarricadeIndex = existingIndex
                                    } else if (canPlaceBarricade(position, initialData, map)) {
                                        val newBarricade = InitialBarricade(
                                            position = position,
                                            healthPoints = barricadeHealthPoints,
                                            name = barricadeName.takeIf { it.isNotBlank() },
                                            isGate = barricadeIsGate
                                        )
                                        onInitialDataChange(initialData.copy(barricades = initialData.barricades + newBarricade))
                                    }
                                }
                                null -> {
                                    // Selection mode - find clicked element
                                    val clickedElement = findElementAtPosition(
                                        position,
                                        initialData
                                    )
                                    selectedElement = clickedElement
                                }
                            }
                        }
                    )
                }
            }
        }
        
        // Right side: Element configuration sidebar
        InitialSetupSidebar(
            placementMode = placementMode,
            onPlacementModeChange = { 
                placementMode = it
                selectedElement = null
            },
            selectedDefenderType = selectedDefenderType,
            onSelectedDefenderTypeChange = { selectedDefenderType = it },
            selectedDefenderLevel = selectedDefenderLevel,
            onSelectedDefenderLevelChange = { selectedDefenderLevel = it },
            showAllTowers = showAllTowers,
            onShowAllTowersChange = { showAllTowers = it },
            dragonName = dragonName,
            onDragonNameChange = { dragonName = it },
            selectedAttackerType = selectedAttackerType,
            onSelectedAttackerTypeChange = { selectedAttackerType = it },
            selectedAttackerLevel = selectedAttackerLevel,
            onSelectedAttackerLevelChange = { selectedAttackerLevel = it },
            customHealth = customHealth,
            onCustomHealthChange = { customHealth = it },
            attackerDragonName = attackerDragonName,
            onAttackerDragonNameChange = { attackerDragonName = it },
            selectedTrapType = selectedTrapType,
            onSelectedTrapTypeChange = { selectedTrapType = it },
            trapDamage = trapDamage,
            onTrapDamageChange = { trapDamage = it },
            barricadeHealthPoints = barricadeHealthPoints,
            onBarricadeHealthPointsChange = { barricadeHealthPoints = it },
            barricadeName = barricadeName,
            onBarricadeNameChange = { barricadeName = it },
            barricadeIsGate = barricadeIsGate,
            onBarricadeIsGateChange = { barricadeIsGate = it },
            availableTowers = availableTowers,
            initialData = initialData,
            onRemoveDefender = { index ->
                val newList = initialData.defenders.toMutableList()
                newList.removeAt(index)
                onInitialDataChange(initialData.copy(defenders = newList))
                selectedElement = null
            },
            onRemoveAttacker = { index ->
                val newList = initialData.attackers.toMutableList()
                newList.removeAt(index)
                onInitialDataChange(initialData.copy(attackers = newList))
                selectedElement = null
            },
            onRemoveTrap = { index ->
                val newList = initialData.traps.toMutableList()
                newList.removeAt(index)
                onInitialDataChange(initialData.copy(traps = newList))
                selectedElement = null
            },
            onRemoveBarricade = { index ->
                val newList = initialData.barricades.toMutableList()
                newList.removeAt(index)
                onInitialDataChange(initialData.copy(barricades = newList))
                selectedElement = null
            },
            selectedElement = selectedElement,
            onSelectedElementChange = { selectedElement = it }
        )
    }

    // Edit barricade dialog – opens when clicking an existing barricade in BARRICADE placement mode
    editBarricadeIndex?.let { editIdx ->
        val barricade = initialData.barricades.getOrNull(editIdx)
        if (barricade != null) {
            var editHP by remember(editIdx) { mutableStateOf(barricade.healthPoints.toString()) }
            var editName by remember(editIdx) { mutableStateOf(barricade.name ?: "") }
            var editIsGate by remember(editIdx) { mutableStateOf(barricade.isGate) }
            AlertDialog(
                onDismissRequest = { editBarricadeIndex = null },
                title = { Text(stringResource(Res.string.barricade_configuration)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editHP,
                            onValueChange = { editHP = it },
                            label = { Text(stringResource(Res.string.health_points)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text(stringResource(Res.string.barricade_name_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(Res.string.is_gate_label),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(checked = editIsGate, onCheckedChange = { editIsGate = it })
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val hp = editHP.toIntOrNull()?.coerceIn(1, 9999) ?: barricade.healthPoints
                        val updated = barricade.copy(
                            healthPoints = hp,
                            name = editName.takeIf { it.isNotBlank() },
                            isGate = editIsGate
                        )
                        val newList = initialData.barricades.toMutableList()
                        newList[editIdx] = updated
                        onInitialDataChange(initialData.copy(barricades = newList))
                        editBarricadeIndex = null
                    }) {
                        Text(stringResource(Res.string.ok))
                    }
                },
                dismissButton = {
                    Button(onClick = { editBarricadeIndex = null }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        } else {
            editBarricadeIndex = null
        }
    }
}

/**
 * Validation functions
 */

/**
 * Check if a position is occupied by ANY element type.
 * Rule: Only one element (tower, trap, barricade, OR unit) is possible on a tile.
 */
private fun isPositionOccupied(
    position: Position,
    initialData: InitialData
): Boolean {
    return initialData.defenders.any { it.position == position } ||
           initialData.attackers.any { it.position == position } ||
           initialData.traps.any { it.position == position } ||
           initialData.barricades.any { it.position == position }
}

private fun canPlaceDefender(
    position: Position,
    initialData: InitialData,
    map: EditorMap
): Boolean {
    // Must be valid tile type for defenders
    if (!isValidPlacement(position, PlacementMode.DEFENDER, map)) {
        return false
    }
    // Must not be occupied by any element
    return !isPositionOccupied(position, initialData)
}

private fun canPlaceAttacker(
    position: Position,
    initialData: InitialData,
    map: EditorMap
): Boolean {
    // Must be valid tile type for attackers
    if (!isValidPlacement(position, PlacementMode.ATTACKER, map)) {
        return false
    }
    // Must not be occupied by any element
    return !isPositionOccupied(position, initialData)
}

private fun canPlaceTrap(
    position: Position,
    initialData: InitialData,
    map: EditorMap
): Boolean {
    // Must be valid tile type for traps (PATH only)
    if (!isValidPlacement(position, PlacementMode.TRAP, map)) {
        return false
    }
    // Must not be occupied by any element
    return !isPositionOccupied(position, initialData)
}

private fun canPlaceBarricade(
    position: Position,
    initialData: InitialData,
    map: EditorMap
): Boolean {
    // Must be valid tile type for barricades (PATH only)
    if (!isValidPlacement(position, PlacementMode.BARRICADE, map)) {
        return false
    }
    // Must not be occupied by any element
    return !isPositionOccupied(position, initialData)
}

/**
 * Represents a selected element on the map
 */
sealed class SelectedElement {
    data class Defender(val index: Int, val defender: InitialDefender) : SelectedElement()
    data class Attacker(val index: Int, val attacker: InitialAttacker) : SelectedElement()
    data class Trap(val index: Int, val trap: InitialTrap) : SelectedElement()
    data class Barricade(val index: Int, val barricade: InitialBarricade) : SelectedElement()
}

private fun findElementAtPosition(
    position: Position,
    initialData: InitialData
): SelectedElement? {
    initialData.defenders.forEachIndexed { index, defender ->
        if (defender.position == position) {
            return SelectedElement.Defender(index, defender)
        }
    }
    initialData.attackers.forEachIndexed { index, attacker ->
        if (attacker.position == position) {
            return SelectedElement.Attacker(index, attacker)
        }
    }
    initialData.traps.forEachIndexed { index, trap ->
        if (trap.position == position) {
            return SelectedElement.Trap(index, trap)
        }
    }
    initialData.barricades.forEachIndexed { index, barricade ->
        if (barricade.position == position) {
            return SelectedElement.Barricade(index, barricade)
        }
    }
    return null
}
