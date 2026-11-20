package de.egril.defender.ui.editor.level

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.ui.common.SelectableText

internal fun areWaypointsValid(
    waypointsState: MutableList<EditorWaypoint>,
    currentMap: EditorMap?,
    level: EditorLevel,
): Boolean {
    val targets = currentMap?.getTargets() ?: emptyList()
    if (targets.isEmpty() || currentMap == null) {
        return false
    }

    val spawnPoints = currentMap.getSpawnPoints()
    val tempLevel = level.copy(waypoints = waypointsState.toList())
    val validationResult = tempLevel.validateWaypointsDetailed(targets, spawnPoints)

    // Valid if validation passes
    return validationResult.isValid
}

/**
 * Red dot badge to indicate incomplete data. When [messages] are given, clicking the dot shows them.
 */
@Composable
internal fun RedDotBadge(
    messages: List<String> = emptyList(),
    testTag: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier =
                Modifier
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                    .size(if (messages.isEmpty()) 8.dp else 12.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .then(if (messages.isNotEmpty()) Modifier.clickable { expanded = true } else Modifier),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Column(
                modifier = Modifier.widthIn(max = 360.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                messages.forEach { message ->
                    SelectableText(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Warning badge to indicate issues that don't prevent playability
 */
@Composable
internal fun WarningBadge() {
    Box(
        modifier =
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFA500)), // Orange color
    )
}
