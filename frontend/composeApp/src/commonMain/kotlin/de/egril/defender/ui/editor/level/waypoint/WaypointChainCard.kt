package de.egril.defender.ui.editor.level.waypoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.WaypointChain
import de.egril.defender.model.Position

/**
 * Card displaying a single waypoint chain
 */
@Composable
fun WaypointChainCard(
    chain: WaypointChain,
    spawnPoints: List<Position>,
    targets: List<Position>,
    circularDeps: Set<Position>,
    unconnectedWaypoints: Set<Position>,
    onDeleteConnection: (Position) -> Unit,
    onConnectWaypoint: (Position) -> Unit
) {
    Card(
        modifier = Modifier.Companion.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (chain.hasCircularDependency) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.Companion.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // waypoints
            chain.positions.forEachIndexed { index, pos ->
                WaypointChainNode(
                    position = pos,
                    isSpawn = spawnPoints.contains(pos),
                    isTarget = false,
                    isInCircular = circularDeps.contains(pos),
                    isUnconnected = unconnectedWaypoints.contains(pos),
                    indentLevel = index,
                    onDelete = { onDeleteConnection(pos) },
                    onConnect = { onConnectWaypoint(pos) }
                )
            }

            // End position (target or incomplete)
            if (chain.endPosition != null) {
                WaypointChainNode(
                    position = chain.endPosition,
                    isSpawn = false,
                    isTarget = targets.contains(chain.endPosition),
                    isInCircular = circularDeps.contains(chain.endPosition),
                    isUnconnected = unconnectedWaypoints.contains(chain.endPosition),
                    indentLevel = chain.positions.size,
                    onDelete = null,  // Can't delete target
                    onConnect = { onConnectWaypoint(chain.endPosition) }
                )
            }
        }
    }
}
