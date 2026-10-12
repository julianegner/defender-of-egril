package de.egril.defender

import de.egril.defender.mapgen.core.MapImageInput
import de.egril.defender.mapgen.core.MapImageRenderer
import de.egril.defender.mapgen.core.MapImageTileKind
import de.egril.defender.mapgen.core.TileZoneOverlayRenderer
import de.egril.defender.png.OptimalPngEncoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.sql.Connection

/**
 * The parts of a community map JSON that are needed to render its background images.
 *
 * @property base Input for the map background image
 * @property zoneTiles Tile overrides per tile zone, in the order the zones appear in the map
 */
class CommunityMapImageSource(
    val base: MapImageInput,
    val zoneTiles: List<Map<Pair<Int, Int>, MapImageTileKind>>,
)

/** PNG files generated for a community map. [zones] is indexed like the map's tile zones. */
class CommunityMapImages(
    val map: ByteArray,
    val zones: List<ByteArray>,
)

/**
 * Extracts the rendering inputs from the map JSON stored in community_files.
 * Returns null if the JSON is not a map with a valid width and height.
 */
fun parseCommunityMapImageSource(json: String): CommunityMapImageSource? =
    try {
        val root = Json.parseToJsonElement(json).jsonObject
        val id = root["id"]?.jsonPrimitive?.contentOrNull ?: ""
        val width = root.getValue("width").jsonPrimitive.int
        val height = root.getValue("height").jsonPrimitive.int
        val waterSpawnPoints =
            root["spawnPointInfo"]
                ?.jsonObject
                ?.filterValues { it.jsonPrimitive.contentOrNull == "WATER" }
                ?.keys
                ?.mapNotNull(::parseGridKey)
                ?.toSet()
                .orEmpty()
        val zoneTiles =
            root["tileZones"]?.jsonArray?.map { zone ->
                parseTileKinds(zone.jsonObject["zoneTiles"], typeField = "type")
            }.orEmpty()
        CommunityMapImageSource(
            base =
                MapImageInput(
                    seed = id,
                    width = width,
                    height = height,
                    tiles = parseTileKinds(root["tiles"], typeField = null),
                    waterSpawnPoints = waterSpawnPoints,
                ),
            zoneTiles = zoneTiles,
        )
    } catch (_: Exception) {
        null
    }

/**
 * Renders the base map image and one overlay image per tile zone as optimally compressed PNG files.
 * Rendering is deterministic, so the output matches what the map editor generates on the client.
 */
fun renderCommunityMapImages(source: CommunityMapImageSource): CommunityMapImages {
    val map = encodePng(MapImageRenderer.generatePixels(source.base))
    val zones =
        source.zoneTiles.map { zone ->
            encodePng(TileZoneOverlayRenderer.generateOverlayPixels(source.base, zone))
        }
    return CommunityMapImages(map = map, zones = zones)
}

/**
 * Replaces the stored images of the MAP community file [fileId] with [images] in one transaction.
 * Zone images from a previous upload are removed first, so stale zones never remain.
 */
fun storeCommunityMapImages(
    conn: Connection,
    fileId: String,
    images: CommunityMapImages,
) {
    val previousAutoCommit = conn.autoCommit
    conn.autoCommit = false
    try {
        conn
            .prepareStatement("UPDATE community_files SET map_image = ? WHERE file_type = 'MAP' AND file_id = ?")
            .use { stmt ->
                stmt.setBytes(1, images.map)
                stmt.setString(2, fileId)
                stmt.executeUpdate()
            }
        conn
            .prepareStatement(
                "DELETE FROM community_map_zone_images WHERE community_file_id = " +
                    "(SELECT id FROM community_files WHERE file_type = 'MAP' AND file_id = ?)",
            ).use { stmt ->
                stmt.setString(1, fileId)
                stmt.executeUpdate()
            }
        conn
            .prepareStatement(
                "INSERT INTO community_map_zone_images (community_file_id, zone_index, image) " +
                    "SELECT id, ?, ? FROM community_files WHERE file_type = 'MAP' AND file_id = ?",
            ).use { stmt ->
                images.zones.forEachIndexed { index, png ->
                    stmt.setInt(1, index)
                    stmt.setBytes(2, png)
                    stmt.setString(3, fileId)
                    stmt.executeUpdate()
                }
            }
        conn.commit()
    } catch (e: Exception) {
        conn.rollback()
        throw e
    } finally {
        conn.autoCommit = previousAutoCommit
    }
}

/**
 * Parses a tile map of the form `"x,y": "TYPE"` (base tiles) or `"x,y": {"type": "TYPE"}` (zone tiles).
 * Unknown tile types are rendered as NO_PLAY, matching the editor's default for missing tiles.
 */
private fun parseTileKinds(
    element: JsonElement?,
    typeField: String?,
): Map<Pair<Int, Int>, MapImageTileKind> {
    val tiles = element as? JsonObject ?: return emptyMap()
    return tiles.entries
        .mapNotNull { (key, value) ->
            val position = parseGridKey(key) ?: return@mapNotNull null
            val typeName =
                if (typeField == null) {
                    (value as? JsonPrimitive)?.contentOrNull
                } else {
                    (value as? JsonObject)?.get(typeField)?.jsonPrimitive?.contentOrNull
                }
            val kind = MapImageTileKind.entries.firstOrNull { it.name == typeName } ?: MapImageTileKind.NO_PLAY
            position to kind
        }.toMap()
}

private fun parseGridKey(key: String): Pair<Int, Int>? {
    val parts = key.split(",")
    if (parts.size != 2) return null
    val x = parts[0].toIntOrNull() ?: return null
    val y = parts[1].toIntOrNull() ?: return null
    return x to y
}

private fun encodePng(rendered: Triple<IntArray, Int, Int>): ByteArray =
    OptimalPngEncoder.encode(rendered.first, rendered.second, rendered.third)
