package de.egril.defender.ui.editor.level.waypoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.WaypointValidationResult
import de.egril.defender.model.Position
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.no_waypoints_configured
import defender_of_egril.composeapp.generated.resources.waypoint_chains_title

/**
 * Tree view display for waypoint chains showing the hierarchical structure
 */
@Composable
fun WaypointTreeView(
    validationResult: WaypointValidationResult,
    map: EditorMap?,
    onDeleteConnection: (Position) -> Unit,
    onConnectWaypoint: (Position) -> Unit
) {
    val spawnPoints = remember(map) { map?.getSpawnPoints() ?: emptyList() }
    val targets = remember(map) { map?.getTargets() ?: emptyList() }

    if (validationResult.waypointChains.isEmpty()) {
        Text(
            text = stringResource(Res.string.no_waypoints_configured),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.Companion.padding(vertical = 16.dp)
        )
        return
    }

    Column(
        modifier = Modifier.Companion.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.waypoint_chains_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Companion.Bold
        )

        validationResult.waypointChains.forEach { chain ->
            WaypointChainCard(
                chain = chain,
                spawnPoints = spawnPoints,
                targets = targets,
                circularDeps = validationResult.circularDependencies,
                unconnectedWaypoints = validationResult.unconnectedWaypoints,
                onDeleteConnection = onDeleteConnection,
                onConnectWaypoint = onConnectWaypoint
            )
        }
    }
}
