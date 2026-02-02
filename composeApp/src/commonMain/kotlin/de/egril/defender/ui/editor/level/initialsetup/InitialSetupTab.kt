package de.egril.defender.ui.editor.level.initialsetup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.*
import defender_of_egril.composeapp.generated.resources.*

/**
 * Tab 5: Initial Setup - Place towers, enemies, traps, and barricades before level starts
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
    availableTowers: Set<de.egril.defender.model.DefenderType>
) {
    var selectedElementType by remember { mutableStateOf(InitialElementType.DEFENDER) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    de.egril.defender.ui.icon.InfoIcon(size = 20.dp)
                    Text(
                        text = stringResource(Res.string.initial_setup_info),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Element type selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InitialElementType.entries.forEach { type ->
                    Button(
                        onClick = { selectedElementType = type },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedElementType == type)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(type.displayName)
                    }
                }
            }
        }
        
        // Content based on selected type
        item {
            when (selectedElementType) {
                InitialElementType.DEFENDER -> {
                    InitialDefendersSection(
                        initialDefenders = initialDefenders,
                        onInitialDefendersChange = onInitialDefendersChange,
                        map = map,
                        availableTowers = availableTowers
                    )
                }
                InitialElementType.ATTACKER -> {
                    InitialAttackersSection(
                        initialAttackers = initialAttackers,
                        onInitialAttackersChange = onInitialAttackersChange,
                        map = map
                    )
                }
                InitialElementType.TRAP -> {
                    InitialTrapsSection(
                        initialTraps = initialTraps,
                        onInitialTrapsChange = onInitialTrapsChange,
                        map = map
                    )
                }
                InitialElementType.BARRICADE -> {
                    InitialBarricadesSection(
                        initialBarricades = initialBarricades,
                        onInitialBarricadesChange = onInitialBarricadesChange,
                        map = map
                    )
                }
            }
        }
    }
}

/**
 * Types of initial elements that can be placed
 */
enum class InitialElementType(val displayName: String) {
    DEFENDER("Towers"),
    ATTACKER("Enemies"),
    TRAP("Traps"),
    BARRICADE("Barricades")
}
