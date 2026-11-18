package de.egril.defender.ui.editor.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.editor.level.DensityBand
import de.egril.defender.ui.editor.level.MapFlowSummary
import de.egril.defender.ui.editor.level.MapLaneShape
import de.egril.defender.ui.editor.level.TravelBand
import defender_of_egril.composeapp.generated.resources.*

@Composable
internal fun MapFlowValidatorCard(summary: MapFlowSummary) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(Res.string.map_flow_validator),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = if (summary.isReady) stringResource(Res.string.ready_to_use) else stringResource(Res.string.not_ready),
                color = if (summary.isReady) Color(0xFF2E7D32) else Color.Red,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MapFlowMetric(stringResource(Res.string.lane_shape), summary.laneShape.localizedLabel())
                MapFlowMetric(stringResource(Res.string.build_coverage), summary.buildCoverage.localizedLabel())
                MapFlowMetric(stringResource(Res.string.travel_length), summary.travelLength.localizedLabel())
            }
            Text(
                text = "${stringResource(Res.string.dead_corridor)}: ${summary.longestDeadCorridor}",
                style = MaterialTheme.typography.bodySmall,
                color = if (summary.longestDeadCorridor >= 8) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${stringResource(Res.string.connectivity)}: ${summary.spawnCount}/${summary.targetCount}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun MapPathPreviewCard(previews: List<MapPathPreview>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(Res.string.map_path_preview),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text =
                    stringResource(
                        Res.string.path_preview_summary,
                        previews.count { it.isReachable },
                        previews.count { !it.isReachable },
                        previews.count { it.isAmbiguous },
                    ),
                style = MaterialTheme.typography.bodySmall,
            )
            previews.forEach { preview ->
                Text(
                    text =
                        buildString {
                            append("(${preview.spawn.x}, ${preview.spawn.y})")
                            append(" -> ")
                            append(
                                preview.target?.let { "(${it.x}, ${it.y})" }
                                    ?: stringResource(Res.string.unreachable),
                            )
                            append(" (${preview.path.size.coerceAtLeast(0)})")
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        when {
                            !preview.isReachable -> Color.Red
                            preview.isAmbiguous -> Color(0xFFFFC107)
                            else -> Color(0xFF00838F)
                        },
                )
            }
        }
    }
}

@Composable
internal fun MapUsageConsistencyCard(issues: List<Pair<de.egril.defender.editor.EditorLevel, de.egril.defender.ui.editor.level.LevelConsistencySummary>>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(Res.string.used_level_checks),
                style = MaterialTheme.typography.titleSmall,
            )
            issues.forEach { (level, summary) ->
                Text(
                    text = "${level.title.ifBlank { level.id }}: ${summary.issueCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
internal fun MapFlowMetric(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Draws a red hairline crosshair centered on the map's tile content (not the screen/canvas),
 * so it stays anchored to the exact center of the map regardless of zoom or pan.
 *
 * This is rendered via the [HexagonalMapView] `overlayContent` slot, which applies the same
 * scale/translation transform as the tile grid itself.
 */
@Composable
internal fun MapCrosshairOverlay(contentSize: IntSize) {
    if (contentSize.width == 0 || contentSize.height == 0) return
    val density = LocalDensity.current
    val contentWidthDp = with(density) { contentSize.width.toDp() }
    val contentHeightDp = with(density) { contentSize.height.toDp() }
    androidx.compose.foundation.Canvas(
        modifier = Modifier.requiredSize(contentWidthDp, contentHeightDp),
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        drawLine(
            color = Color.Red,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.Red,
            start = Offset(centerX, 0f),
            end = Offset(centerX, size.height),
            strokeWidth = 2f,
        )
    }
}

@Composable
internal fun MapLaneShape.localizedLabel(): String =
    when (this) {
        MapLaneShape.STRAIGHT -> stringResource(Res.string.lane_shape_straight)
        MapLaneShape.BRANCHING -> stringResource(Res.string.lane_shape_branching)
    }

@Composable
internal fun DensityBand.localizedLabel(): String =
    when (this) {
        DensityBand.SPARSE -> stringResource(Res.string.density_sparse)
        DensityBand.GOOD -> stringResource(Res.string.rating_good)
        DensityBand.DENSE -> stringResource(Res.string.density_dense)
    }

@Composable
internal fun TravelBand.localizedLabel(): String =
    when (this) {
        TravelBand.SHORT -> stringResource(Res.string.travel_short)
        TravelBand.GOOD -> stringResource(Res.string.rating_good)
        TravelBand.LONG -> stringResource(Res.string.travel_long)
    }
