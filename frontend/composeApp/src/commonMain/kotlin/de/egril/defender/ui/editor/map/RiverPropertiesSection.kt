package de.egril.defender.ui.editor.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.model.RiverFlow
import de.egril.defender.ui.icon.WaterIcon
import defender_of_egril.composeapp.generated.resources.*

/**
 * River tile editing controls shown in the side panel: flow direction, flow speed,
 * a water spawn point shortcut and filling a river ring.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.RiverPropertiesSection(
    selectedRiverFlow: RiverFlow,
    onRiverFlowChange: (RiverFlow) -> Unit,
    selectedRiverSpeed: Int,
    onRiverSpeedChange: (Int) -> Unit,
    onWaterSpawnPointClick: () -> Unit,
    onFillRiverRing: () -> Unit,
) {
    Text(
        text = stringResource(Res.string.river_properties),
        style = MaterialTheme.typography.bodyMedium,
    )

    Text(
        text = stringResource(Res.string.flow_direction),
        style = MaterialTheme.typography.bodySmall,
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RiverFlow.entries.forEach { flow ->
            Button(
                onClick = { onRiverFlowChange(flow) },
                colors = selectionButtonColors(selectedRiverFlow == flow),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text(flow.name.replace("_", " "), fontSize = 10.sp)
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = stringResource(Res.string.flow_speed),
        style = MaterialTheme.typography.bodySmall,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(
            onClick = { onRiverSpeedChange(1) },
            colors = selectionButtonColors(selectedRiverSpeed == 1),
            modifier = Modifier.height(32.dp),
        ) {
            Text(stringResource(Res.string.speed_slow), fontSize = 10.sp)
        }
        Button(
            onClick = { onRiverSpeedChange(2) },
            colors = selectionButtonColors(selectedRiverSpeed == 2),
            modifier = Modifier.height(32.dp),
        ) {
            Text(stringResource(Res.string.speed_fast), fontSize = 10.sp)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onWaterSpawnPointClick,
        modifier = Modifier.fillMaxWidth().height(32.dp),
    ) {
        Text(
            "${stringResource(Res.string.spawn_point)} (${stringResource(Res.string.spawn_point_type_water)})",
            fontSize = 10.sp,
        )
    }

    Button(
        onClick = onFillRiverRing,
        modifier = Modifier.fillMaxWidth().height(32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WaterIcon(size = 16.dp)
            Text(stringResource(Res.string.fill_river_ring), fontSize = 10.sp)
        }
    }
}

@Composable
private fun selectionButtonColors(selected: Boolean) =
    ButtonDefaults.buttonColors(
        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
    )
