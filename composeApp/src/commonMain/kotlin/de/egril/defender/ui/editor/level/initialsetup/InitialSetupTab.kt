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
    initialDefenders: List<InitialDefender>,
    onInitialDefendersChange: (List<InitialDefender>) -> Unit,
    initialAttackers: List<InitialAttacker>,
    onInitialAttackersChange: (List<InitialAttacker>) -> Unit,
    initialTraps: List<InitialTrap>,
    onInitialTrapsChange: (List<InitialTrap>) -> Unit,
    initialBarricades: List<InitialBarricade>,
    onInitialBarricadesChange: (List<InitialBarricade>) -> Unit,
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
                        existingDefenders = initialDefenders,
                        existingAttackers = initialAttackers,
                        existingTraps = initialTraps,
                        existingBarricades = initialBarricades,
                        selectedElement = selectedElement,
                        onTileClick = { position ->
                            when (placementMode) {
                                PlacementMode.DEFENDER -> {
                                    if (canPlaceDefender(position, initialDefenders)) {
                                        val newDefender = InitialDefender(
                                            type = selectedDefenderType,
                                            position = position,
                                            level = selectedDefenderLevel,
                                            dragonName = if (selectedDefenderType == DefenderType.DRAGONS_LAIR && dragonName.isNotBlank()) dragonName else null
                                        )
                                        onInitialDefendersChange(initialDefenders + newDefender)
                                    }
                                }
                                PlacementMode.ATTACKER -> {
                                    val newAttacker = InitialAttacker(
                                        type = selectedAttackerType,
                                        position = position,
                                        level = selectedAttackerLevel,
                                        currentHealth = customHealth,
                                        dragonName = if (selectedAttackerType == AttackerType.DRAGON && attackerDragonName.isNotBlank()) attackerDragonName else null
                                    )
                                    onInitialAttackersChange(initialAttackers + newAttacker)
                                }
                                PlacementMode.TRAP -> {
                                    if (canPlaceTrap(position, initialTraps, initialBarricades)) {
                                        val newTrap = InitialTrap(
                                            position = position,
                                            damage = trapDamage,
                                            type = selectedTrapType
                                        )
                                        onInitialTrapsChange(initialTraps + newTrap)
                                    }
                                }
                                PlacementMode.BARRICADE -> {
                                    if (canPlaceBarricade(position, initialTraps, initialBarricades)) {
                                        val newBarricade = InitialBarricade(
                                            position = position,
                                            healthPoints = barricadeHealthPoints
                                        )
                                        onInitialBarricadesChange(initialBarricades + newBarricade)
                                    }
                                }
                                null -> {
                                    // Selection mode - find clicked element
                                    val clickedElement = findElementAtPosition(
                                        position,
                                        initialDefenders,
                                        initialAttackers,
                                        initialTraps,
                                        initialBarricades
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
            availableTowers = availableTowers,
            initialDefenders = initialDefenders,
            onRemoveDefender = { index ->
                val newList = initialDefenders.toMutableList()
                newList.removeAt(index)
                onInitialDefendersChange(newList)
                selectedElement = null
            },
            initialAttackers = initialAttackers,
            onRemoveAttacker = { index ->
                val newList = initialAttackers.toMutableList()
                newList.removeAt(index)
                onInitialAttackersChange(newList)
                selectedElement = null
            },
            initialTraps = initialTraps,
            onRemoveTrap = { index ->
                val newList = initialTraps.toMutableList()
                newList.removeAt(index)
                onInitialTrapsChange(newList)
                selectedElement = null
            },
            initialBarricades = initialBarricades,
            onRemoveBarricade = { index ->
                val newList = initialBarricades.toMutableList()
                newList.removeAt(index)
                onInitialBarricadesChange(newList)
                selectedElement = null
            },
            selectedElement = selectedElement,
            onSelectedElementChange = { selectedElement = it }
        )
    }
}

/**
 * Validation functions
 */

private fun canPlaceDefender(position: Position, existingDefenders: List<InitialDefender>): Boolean {
    return existingDefenders.none { it.position == position }
}

private fun canPlaceTrap(position: Position, existingTraps: List<InitialTrap>, existingBarricades: List<InitialBarricade>): Boolean {
    return existingTraps.none { it.position == position } && existingBarricades.none { it.position == position }
}

private fun canPlaceBarricade(position: Position, existingTraps: List<InitialTrap>, existingBarricades: List<InitialBarricade>): Boolean {
    return existingTraps.none { it.position == position } && existingBarricades.none { it.position == position }
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
    defenders: List<InitialDefender>,
    attackers: List<InitialAttacker>,
    traps: List<InitialTrap>,
    barricades: List<InitialBarricade>
): SelectedElement? {
    defenders.forEachIndexed { index, defender ->
        if (defender.position == position) {
            return SelectedElement.Defender(index, defender)
        }
    }
    attackers.forEachIndexed { index, attacker ->
        if (attacker.position == position) {
            return SelectedElement.Attacker(index, attacker)
        }
    }
    traps.forEachIndexed { index, trap ->
        if (trap.position == position) {
            return SelectedElement.Trap(index, trap)
        }
    }
    barricades.forEachIndexed { index, barricade ->
        if (barricade.position == position) {
            return SelectedElement.Barricade(index, barricade)
        }
    }
    return null
}

/**
 * Types of initial elements that can be placed (kept for potential future use)
 */
enum class InitialElementType {
    DEFENDER,
    ATTACKER,
    TRAP,
    BARRICADE
}
