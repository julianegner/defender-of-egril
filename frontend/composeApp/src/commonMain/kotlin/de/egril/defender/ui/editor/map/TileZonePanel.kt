package de.egril.defender.ui.editor.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import de.egril.defender.model.TileZone
import de.egril.defender.ui.common.SelectableText
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.map_background_storage_info
import defender_of_egril.composeapp.generated.resources.tile_zone_add
import defender_of_egril.composeapp.generated.resources.tile_zone_delete
import defender_of_egril.composeapp.generated.resources.tile_zone_help
import defender_of_egril.composeapp.generated.resources.tile_zone_name_label
import defender_of_egril.composeapp.generated.resources.tile_zone_custom_background_warning
import defender_of_egril.composeapp.generated.resources.tile_zone_paint_erase
import defender_of_egril.composeapp.generated.resources.tile_zone_paint_path
import defender_of_egril.composeapp.generated.resources.tile_zone_paint_river
import defender_of_egril.composeapp.generated.resources.tile_zone_river_flow_hint
import defender_of_egril.composeapp.generated.resources.tile_zone_tile_count
import defender_of_egril.composeapp.generated.resources.tile_zones

/** Border color that marks tiles belonging to the selected tile zone in the map editor. */
internal val TILE_ZONE_HIGHLIGHT_COLOR = Color(0xFFE040FB)

internal fun mapBackgroundPaths(map: EditorMap): Pair<String, String> {
    val directory = "gamedata/${if (map.isOfficial) "official" else "user"}/maps/${map.id}"
    return "$directory.png" to "$directory.json"
}

/** Base tile types that a tile zone may change (only land paths and rivers can swap). */
private val ZONE_EDITABLE_BASE_TYPES = setOf(TileType.PATH, TileType.RIVER)

/** Create a new zone with an id that is unique among [existing]. */
internal fun createTileZone(existing: List<TileZone>): TileZone {
    val ids = existing.map { it.id }.toSet()
    var counter = existing.size + 1
    while ("zone_$counter" in ids) counter++
    return TileZone(id = "zone_$counter")
}

/**
 * Paint [paintType] into [zone] at [position]. A null [paintType] removes the tile from the zone.
 * Only tiles whose base type ([baseType]) is PATH or RIVER can be part of a zone; others are ignored.
 * Painting the base type itself removes the tile from the zone (it would not change anything).
 */
internal fun paintTileZone(
    zone: TileZone,
    position: Position,
    baseType: TileType?,
    paintType: TileType?,
    riverFlow: RiverFlow,
    riverSpeed: Int,
): TileZone {
    if (baseType !in ZONE_EDITABLE_BASE_TYPES) return zone
    val tiles = zone.tiles.toMutableMap()
    val rivers = zone.riverTiles.toMutableMap()
    if (paintType == null || (paintType == baseType && paintType != TileType.RIVER)) {
        tiles.remove(position)
        rivers.remove(position)
    } else {
        tiles[position] = paintType
        if (paintType == TileType.RIVER) {
            rivers[position] = RiverTile(position = position, flowDirection = riverFlow, flowSpeed = riverSpeed)
        } else {
            rivers.remove(position)
        }
    }
    return zone.copy(tiles = tiles, riverTiles = rivers)
}

/** Shift all zone tiles after a map resize; tiles that fall outside the new bounds are dropped. */
internal fun shiftTileZones(
    zones: List<TileZone>,
    leftDelta: Int,
    topDelta: Int,
    newWidth: Int,
    newHeight: Int,
): List<TileZone> {
    fun shifted(position: Position): Position? =
        Position(position.x + leftDelta, position.y + topDelta).takeIf { it.x in 0 until newWidth && it.y in 0 until newHeight }
    return zones.map { zone ->
        zone.copy(
            tiles = zone.tiles.mapNotNull { (position, type) -> shifted(position)?.let { it to type } }.toMap(),
            riverTiles =
                zone.riverTiles
                    .mapNotNull { (position, river) -> shifted(position)?.let { it to river.copy(position = it) } }
                    .toMap(),
        )
    }
}

/**
 * Floating panel for managing the map's tile zones: create, rename, select and delete zones and
 * choose what the brush paints into the selected zone.
 *
 * @param paintType PATH or RIVER to paint, null to erase tiles from the zone.
 */
@Composable
internal fun TileZonePanel(
    map: EditorMap,
    zones: List<TileZone>,
    selectedZoneId: String?,
    paintType: TileType?,
    onZonesChange: (List<TileZone>) -> Unit,
    onSelectZone: (String?) -> Unit,
    onPaintTypeChange: (TileType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.width(300.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.tile_zones),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.tile_zone_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (zones.any { it.id == selectedZoneId }) {
                SelectableText(
                    text = stringResource(Res.string.tile_zone_custom_background_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val (imagePath, dataPath) = mapBackgroundPaths(map)
                SelectableText(
                    text = stringResource(Res.string.map_background_storage_info, imagePath, dataPath),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            zones.forEach { zone ->
                val isSelected = zone.id == selectedZoneId
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) TILE_ZONE_HIGHLIGHT_COLOR else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(6.dp),
                            ).background(
                                if (isSelected) TILE_ZONE_HIGHLIGHT_COLOR.copy(alpha = 0.12f) else Color.Transparent,
                                RoundedCornerShape(6.dp),
                            ).clickable { onSelectZone(if (isSelected) null else zone.id) }
                            .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(zone.displayName, fontWeight = FontWeight.Bold)
                        Text(
                            text = stringResource(Res.string.tile_zone_tile_count, zone.tiles.size),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            onZonesChange(zones.filterNot { it.id == zone.id })
                            if (isSelected) onSelectZone(null)
                        },
                    ) {
                        Text(stringResource(Res.string.tile_zone_delete))
                    }
                }
            }

            Button(
                onClick = {
                    val zone = createTileZone(zones)
                    onZonesChange(zones + zone)
                    onSelectZone(zone.id)
                },
            ) {
                Text(stringResource(Res.string.tile_zone_add))
            }

            val selectedZone = zones.firstOrNull { it.id == selectedZoneId }
            if (selectedZone != null) {
                OutlinedTextField(
                    value = selectedZone.name,
                    onValueChange = { input ->
                        // The manual JSON serializer does not escape strings, so keep names JSON-safe.
                        val newName = input.filter { it != '"' && it != '\\' }
                        onZonesChange(zones.map { if (it.id == selectedZone.id) it.copy(name = newName) else it })
                    },
                    label = { Text(stringResource(Res.string.tile_zone_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PaintTypeOption(stringResource(Res.string.tile_zone_paint_path), paintType == TileType.PATH) {
                    onPaintTypeChange(TileType.PATH)
                }
                PaintTypeOption(stringResource(Res.string.tile_zone_paint_river), paintType == TileType.RIVER) {
                    onPaintTypeChange(TileType.RIVER)
                }
                PaintTypeOption(stringResource(Res.string.tile_zone_paint_erase), paintType == null) {
                    onPaintTypeChange(null)
                }
                Text(
                    text = stringResource(Res.string.tile_zone_river_flow_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PaintTypeOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label)
    }
}
