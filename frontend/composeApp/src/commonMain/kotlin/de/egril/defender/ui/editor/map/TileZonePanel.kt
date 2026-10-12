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
import androidx.compose.material3.Checkbox
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
import de.egril.defender.model.MIN_ROTATABLE_REGION_TILES
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
import defender_of_egril.composeapp.generated.resources.tile_zone_brush_add
import defender_of_egril.composeapp.generated.resources.tile_zone_brush_add_help
import defender_of_egril.composeapp.generated.resources.tile_zone_brush_paint
import defender_of_egril.composeapp.generated.resources.tile_zone_copy
import defender_of_egril.composeapp.generated.resources.tile_zone_custom_background_warning
import defender_of_egril.composeapp.generated.resources.tile_zone_delete
import defender_of_egril.composeapp.generated.resources.tile_zone_finish_drawing
import defender_of_egril.composeapp.generated.resources.tile_zone_help
import defender_of_egril.composeapp.generated.resources.tile_zone_name_label
import defender_of_egril.composeapp.generated.resources.tile_zone_paint_erase
import defender_of_egril.composeapp.generated.resources.tile_zone_movable
import defender_of_egril.composeapp.generated.resources.tile_zone_movable_help
import defender_of_egril.composeapp.generated.resources.tile_zone_overlap_warning
import defender_of_egril.composeapp.generated.resources.tile_zone_rotatable
import defender_of_egril.composeapp.generated.resources.tile_zone_rotatable_help
import defender_of_egril.composeapp.generated.resources.tile_zone_rotatable_size_warning
import defender_of_egril.composeapp.generated.resources.tile_zone_movable_spawn_warning
import defender_of_egril.composeapp.generated.resources.tile_zone_river_flow_hint
import defender_of_egril.composeapp.generated.resources.tile_zone_start_drawing
import defender_of_egril.composeapp.generated.resources.tile_zone_tile_count
import defender_of_egril.composeapp.generated.resources.tile_zone_show_other
import defender_of_egril.composeapp.generated.resources.tile_zones

/** Border color that marks tiles belonging to the selected tile zone in the map editor. */
internal val TILE_ZONE_HIGHLIGHT_COLOR = Color(0xFFE040FB)

/** Distinct colors used to outline non-selected zones when their overlay is enabled. */
internal val TILE_ZONE_COLORS =
    listOf(
        Color(0xFF2196F3),
        Color(0xFF4CAF50),
        Color(0xFFFF9800),
        Color(0xFF00BCD4),
        Color(0xFFFF5722),
        Color(0xFF8BC34A),
    )

internal fun tileZoneColor(index: Int): Color = TILE_ZONE_COLORS[index % TILE_ZONE_COLORS.size]

internal fun mapBackgroundPaths(map: EditorMap): Pair<String, String> {
    val directory = "gamedata/${if (map.isOfficial) "official" else "user"}/maps/${map.id}"
    return "$directory.png" to "$directory.json"
}

/** Create a new zone with an id that is unique among [existing]. */
internal fun createTileZone(existing: List<TileZone>): TileZone {
    val ids = existing.map { it.id }.toSet()
    var counter = existing.size + 1
    while ("zone_$counter" in ids) counter++
    return TileZone(id = "zone_$counter")
}

/** Copy a zone's terrain data into a new, independently identified zone. */
internal fun copyTileZone(
    existing: List<TileZone>,
    source: TileZone,
): TileZone {
    val newZone = createTileZone(existing)
    return newZone.copy(tiles = source.tiles.toMap(), riverTiles = source.riverTiles.toMap(), movable = source.movable, rotatable = source.rotatable)
}

/**
 * Paint [paintType] into [zone] at [position]. A null [paintType] removes the tile from the zone.
 * Any tile type can be part of a zone; an unset base tile is treated as [TileType.NO_PLAY].
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
    val effectiveBaseType = baseType ?: TileType.NO_PLAY
    val tiles = zone.tiles.toMutableMap()
    val rivers = zone.riverTiles.toMutableMap()
    if (paintType == null || (paintType == effectiveBaseType && paintType != TileType.RIVER)) {
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

/** What a click on the map does while a zone is being drawn. */
internal enum class ZoneBrush {
    /** Paints the selected tile type into the zone. */
    PAINT_TYPE,

    /** Removes the tile from the zone. */
    ERASE,

    /** Adds the tile to the zone with its current base type, so the terrain does not change. */
    ADD_KEEP_TYPE,
}

/**
 * Add [position] to [zone] while keeping the tile's current terrain: it takes [baseType] (and [baseRiver]
 * for river tiles). Used for zones that only mark tiles for movement or rotation.
 * Tiles already in the zone are left unchanged, so their painted type is kept.
 */
internal fun addTileToZone(
    zone: TileZone,
    position: Position,
    baseType: TileType?,
    baseRiver: RiverTile?,
): TileZone {
    if (position in zone.tiles) return zone
    val type = baseType ?: TileType.NO_PLAY
    val rivers =
        if (type == TileType.RIVER && baseRiver != null) {
            zone.riverTiles + (position to baseRiver.copy(position = position))
        } else {
            zone.riverTiles
        }
    return zone.copy(tiles = zone.tiles + (position to type), riverTiles = rivers)
}

/** Shift all zone tiles after a map resize; tiles that fall outside the new bounds are dropped. */
internal fun shiftTileZones(
    zones: List<TileZone>,
    leftDelta: Int,
    topDelta: Int,
    newWidth: Int,
    newHeight: Int,
): List<TileZone> {
    fun shifted(position: Position): Position? = Position(position.x + leftDelta, position.y + topDelta).takeIf { it.x in 0 until newWidth && it.y in 0 until newHeight }
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

/** True when [zone] is movable and shares a tile with another movable zone in [zones]. */
internal fun overlapsOtherMovableZone(
    zones: List<TileZone>,
    zone: TileZone,
): Boolean =
    zone.movable &&
        zones.any { other -> other.id != zone.id && other.movable && other.tiles.keys.any { it in zone.tiles } }

/**
 * True when [zone] cannot be movable because one of its tiles is, or would overwrite, a spawn point.
 * Spawn points are fixed on the map; a movable region carrying one would leave it behind or lose it.
 */
internal fun zoneCoversSpawnPoint(
    map: EditorMap,
    zone: TileZone,
): Boolean =
    zone.tiles.any { (position, type) ->
        type == TileType.SPAWN_POINT || map.tiles["${position.x},${position.y}"] == TileType.SPAWN_POINT
    }

/**
 * Floating panel for managing the map's tile zones: create, rename, select and delete zones and
 * switch between drawing on the map and drawing into the selected zone.
 */
@Composable
internal fun TileZonePanel(
    map: EditorMap,
    zones: List<TileZone>,
    selectedZoneId: String?,
    showOtherZones: Boolean,
    isZoneDrawingMode: Boolean,
    zoneBrush: ZoneBrush,
    onZonesChange: (List<TileZone>) -> Unit,
    onSelectZone: (String?) -> Unit,
    onCopyZone: (TileZone) -> Unit,
    onShowOtherZonesChange: (Boolean) -> Unit,
    onToggleZoneDrawingMode: () -> Unit,
    onZoneBrushChange: (ZoneBrush) -> Unit,
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

            if (zones.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onShowOtherZonesChange(!showOtherZones) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = showOtherZones, onCheckedChange = null)
                    Text(stringResource(Res.string.tile_zone_show_other))
                }
            }

            zones.forEachIndexed { index, zone ->
                val isSelected = zone.id == selectedZoneId
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) TILE_ZONE_HIGHLIGHT_COLOR else tileZoneColor(index),
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
                    Row {
                        OutlinedButton(onClick = { onCopyZone(zone) }) {
                            Text(stringResource(Res.string.tile_zone_copy))
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
                Button(onClick = onToggleZoneDrawingMode) {
                    Text(
                        stringResource(
                            if (isZoneDrawingMode) Res.string.tile_zone_finish_drawing else Res.string.tile_zone_start_drawing,
                        ),
                    )
                }
                if (isZoneDrawingMode) {
                    PaintTypeOption(stringResource(Res.string.tile_zone_brush_paint), zoneBrush == ZoneBrush.PAINT_TYPE) {
                        onZoneBrushChange(ZoneBrush.PAINT_TYPE)
                    }
                    PaintTypeOption(stringResource(Res.string.tile_zone_brush_add), zoneBrush == ZoneBrush.ADD_KEEP_TYPE) {
                        onZoneBrushChange(ZoneBrush.ADD_KEEP_TYPE)
                    }
                    Text(
                        text = stringResource(Res.string.tile_zone_brush_add_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PaintTypeOption(stringResource(Res.string.tile_zone_paint_erase), zoneBrush == ZoneBrush.ERASE) {
                        onZoneBrushChange(ZoneBrush.ERASE)
                    }
                }
                Text(
                    text = stringResource(Res.string.tile_zone_river_flow_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val spawnConflict = zoneCoversSpawnPoint(map, selectedZone)
                Row(
                    modifier =
                        Modifier.fillMaxWidth().clickable(enabled = selectedZone.movable || !spawnConflict) {
                            onZonesChange(zones.map { if (it.id == selectedZone.id) it.copy(movable = !it.movable) else it })
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = selectedZone.movable,
                        onCheckedChange = null,
                        enabled = selectedZone.movable || !spawnConflict,
                    )
                    Text(stringResource(Res.string.tile_zone_movable))
                }
                Text(
                    text = stringResource(Res.string.tile_zone_movable_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val rotatableEnabled = selectedZone.movable
                Row(
                    modifier =
                        Modifier.fillMaxWidth().clickable(enabled = rotatableEnabled) {
                            onZonesChange(zones.map { if (it.id == selectedZone.id) it.copy(rotatable = !it.rotatable) else it })
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = selectedZone.movable && selectedZone.rotatable,
                        onCheckedChange = null,
                        enabled = rotatableEnabled,
                    )
                    Text(stringResource(Res.string.tile_zone_rotatable, MIN_ROTATABLE_REGION_TILES))
                }
                Text(
                    text = stringResource(Res.string.tile_zone_rotatable_help, MIN_ROTATABLE_REGION_TILES),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selectedZone.movable && selectedZone.rotatable && selectedZone.tiles.size < MIN_ROTATABLE_REGION_TILES) {
                    Text(
                        text = stringResource(Res.string.tile_zone_rotatable_size_warning, MIN_ROTATABLE_REGION_TILES),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (overlapsOtherMovableZone(zones, selectedZone)) {
                    Text(
                        text = stringResource(Res.string.tile_zone_overlap_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (spawnConflict && !selectedZone.movable) {
                    Text(
                        text = stringResource(Res.string.tile_zone_movable_spawn_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
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
