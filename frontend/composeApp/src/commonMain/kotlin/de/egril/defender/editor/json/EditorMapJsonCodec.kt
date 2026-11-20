package de.egril.defender.editor

import de.egril.defender.config.LogConfig
import de.egril.defender.editor.EditorJsonScanner.PROGRAM_NAME
import de.egril.defender.editor.EditorJsonScanner.extractDataSection
import de.egril.defender.model.Position
import de.egril.defender.model.SpawnPointType
import de.egril.defender.utils.JsonUtils

/**
 * JSON codec for editor map files (tiles, tile zones, river tiles, target and spawn point info).
 */
internal object EditorMapJsonCodec {
    fun serializeMap(map: EditorMap): String {
        val tilesJson =
            map.tiles.entries.joinToString(",\n    ") { (pos, type) ->
                "\"$pos\": \"${type.name}\""
            }

        val nameKeyJson =
            if (map.nameKey != null) {
                ",\n  \"nameKey\": \"${map.nameKey}\""
            } else {
                ""
            }

        val worldMapPositionJson =
            if (map.worldMapPosition != null) {
                ",\n  \"worldMapPosition\": {\"x\": ${map.worldMapPosition.x}, \"y\": ${map.worldMapPosition.y}}"
            } else {
                ""
            }

        val riverTilesJson =
            if (map.riverTiles.isNotEmpty()) {
                val riverData =
                    map.riverTiles.entries.joinToString(",\n    ") { (pos, river) ->
                        "\"$pos\": {\"flowDirection\": \"${river.flowDirection.name}\", \"flowSpeed\": ${river.flowSpeed}}"
                    }
                ",\n  \"riverTiles\": {\n    $riverData\n  }"
            } else {
                ""
            }

        val authorJson =
            if (map.author.isNotEmpty()) {
                ",\n  \"author\": \"${map.author}\""
            } else {
                ""
            }

        val mapToolingInfoJson = ",\n  \"mapToolingInfo\": \"${map.mapToolingInfo}\""

        val allowNoBuildableTilesJson =
            if (map.allowNoBuildableTiles) {
                ",\n  \"allowNoBuildableTiles\": true"
            } else {
                ""
            }

        val allowNoDirectPathJson =
            if (map.allowNoDirectPath) {
                ",\n  \"allowNoDirectPath\": true"
            } else {
                ""
            }

        val targetInfoJson =
            if (map.targetInfoMap.isNotEmpty()) {
                val targetData =
                    map.targetInfoMap.entries.joinToString(",\n    ") { (pos, info) ->
                        "\"$pos\": {\"name\": \"${info.name}\", \"type\": \"${info.type.name}\"}"
                    }
                ",\n  \"targetInfo\": {\n    $targetData\n  }"
            } else {
                ""
            }

        val spawnPointInfoJson =
            if (map.spawnPointInfoMap.isNotEmpty()) {
                val spawnData =
                    map.spawnPointInfoMap.entries.joinToString(",\n    ") { (pos, type) ->
                        "\"$pos\": \"${type.name}\""
                    }
                ",\n  \"spawnPointInfo\": {\n    $spawnData\n  }"
            } else {
                ""
            }

        val tileZonesJson =
            if (map.tileZones.isNotEmpty()) {
                val zonesData =
                    map.tileZones.joinToString(",\n    ") { zone ->
                        val zoneTilesData =
                            zone.tiles.entries
                                .sortedWith(compareBy({ it.key.y }, { it.key.x }))
                                .joinToString(",\n        ") { (pos, type) ->
                                    val river = zone.riverTiles[pos]
                                    val riverPart =
                                        if (type == TileType.RIVER && river != null) {
                                            ", \"flowDirection\": \"${river.flowDirection.name}\", \"flowSpeed\": ${river.flowSpeed}"
                                        } else {
                                            ""
                                        }
                                    "\"${pos.x},${pos.y}\": {\"type\": \"${type.name}\"$riverPart}"
                                }
                        val movableJson = if (zone.movable) ",\n      \"movable\": true" else ""
                        val rotatableJson = if (zone.movable && zone.rotatable) ",\n      \"rotatable\": true" else ""
                        "{\n      \"id\": \"${zone.id}\",\n      \"name\": \"${zone.name}\"$movableJson$rotatableJson,\n      \"zoneTiles\": {\n        $zoneTilesData\n      }\n    }"
                    }
                ",\n  \"tileZones\": [\n    $zonesData\n  ]"
            } else {
                ""
            }

        val data = """{
  "id": "${map.id}",
  "name": "${map.name}"$nameKeyJson,
  "width": ${map.width},
  "height": ${map.height},
  "readyToUse": ${map.readyToUse},
  "isOfficial": ${map.isOfficial}$worldMapPositionJson$authorJson$mapToolingInfoJson$allowNoBuildableTilesJson$allowNoDirectPathJson,
  "tiles": {
    $tilesJson
  }$riverTilesJson$targetInfoJson$spawnPointInfoJson$tileZonesJson
}"""
        return """{
  "metadata": {
    "program": "$PROGRAM_NAME",
    "type": "map",
    "license": "GNU Affero General Public License v3.0 (AGPL-3.0)"
  },
  "data": $data
}"""
    }

    fun deserializeMap(json: String): EditorMap? {
        val dataJson = extractDataSection(json)
        try {
            val id = JsonUtils.extractValue(dataJson, "id")
            val name = JsonUtils.extractValue(dataJson, "name")
            val nameKey =
                try {
                    JsonUtils.extractValue(dataJson, "nameKey").takeIf { it.isNotEmpty() }
                } catch (e: Exception) {
                    null // Optional field - null if not present
                }
            val width = JsonUtils.extractValue(dataJson, "width").toInt()
            val height = JsonUtils.extractValue(dataJson, "height").toInt()
            val readyToUse =
                try {
                    JsonUtils.extractBooleanValue(dataJson, "readyToUse")
                } catch (e: Exception) {
                    false // Default to false for backward compatibility
                }
            val isOfficial =
                try {
                    JsonUtils.extractBooleanValue(dataJson, "isOfficial")
                } catch (e: Exception) {
                    false // Default to false for backward compatibility
                }
            val author =
                try {
                    JsonUtils.extractValue(dataJson, "author")
                } catch (e: Exception) {
                    "" // Optional field
                }
            val mapToolingInfo =
                try {
                    JsonUtils.extractStringValue(dataJson, "mapToolingInfo").ifBlank { DEFAULT_MAP_TOOLING_INFO }
                } catch (e: Exception) {
                    DEFAULT_MAP_TOOLING_INFO // Optional field with default for backward compatibility
                }
            val allowNoBuildableTiles =
                try {
                    JsonUtils.extractBooleanValue(dataJson, "allowNoBuildableTiles")
                } catch (e: Exception) {
                    false
                }
            val allowNoDirectPath =
                try {
                    JsonUtils.extractBooleanValue(dataJson, "allowNoDirectPath")
                } catch (e: Exception) {
                    false
                }

            // Parse optional world map position
            val worldMapPosition =
                try {
                    val positionSection = dataJson.substringAfter("\"worldMapPosition\":", "")
                    if (positionSection.isNotEmpty()) {
                        val posObj = positionSection.substringAfter("{").substringBefore("}")
                        val x = JsonUtils.extractNumericValue("{$posObj}", "x").toIntOrNull()
                        val y = JsonUtils.extractNumericValue("{$posObj}", "y").toIntOrNull()
                        if (x != null && y != null) Position(x, y) else null
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null // Optional field - null if not present
                }

            fun invalidMap(
                tiles: Map<String, TileType> = emptyMap(),
                riverTiles: Map<String, de.egril.defender.model.RiverTile> = emptyMap(),
                targetInfoMap: Map<String, EditorTargetInfo> = emptyMap(),
                spawnPointInfoMap: Map<String, SpawnPointType> = emptyMap(),
            ): EditorMap =
                EditorMap(
                    id = id,
                    name = name,
                    nameKey = nameKey,
                    width = width,
                    height = height,
                    tiles = tiles,
                    readyToUse = false,
                    worldMapPosition = worldMapPosition,
                    riverTiles = riverTiles,
                    isOfficial = isOfficial,
                    author = author,
                    targetInfoMap = targetInfoMap,
                    spawnPointInfoMap = spawnPointInfoMap,
                    mapToolingInfo = mapToolingInfo,
                    allowNoBuildableTiles = allowNoBuildableTiles,
                    allowNoDirectPath = allowNoDirectPath,
                    isValid = false,
                )

            if (!MapSizeLimits.isWithinLimits(width, height)) {
                return invalidMap()
            }

            // Individual tile/river entries that fall outside the map's declared width/height are
            // silently skipped rather than invalidating the whole map: legacy authoring artifacts
            // (e.g. a single stray tile left over from a resize) shouldn't make an otherwise valid,
            // hand-crafted map unusable.
            val tiles = parseTiles(dataJson, width, height)?.toMutableMap() ?: return null

            // Parse optional river tiles
            val riverTiles = mutableMapOf<String, de.egril.defender.model.RiverTile>()
            try {
                if (dataJson.contains("\"riverTiles\"")) {
                    // Find the riverTiles section by counting braces to find the matching closing brace
                    val startMarker = "\"riverTiles\": {"
                    val startIdx = dataJson.indexOf(startMarker)
                    if (startIdx != -1) {
                        val contentStart = startIdx + startMarker.length
                        var braceCount = 1
                        var endIdx = contentStart

                        // Find matching closing brace
                        while (endIdx < dataJson.length && braceCount > 0) {
                            when (dataJson[endIdx]) {
                                '{' -> braceCount++
                                '}' -> braceCount--
                            }
                            endIdx++
                        }

                        if (braceCount == 0) {
                            val riverSection = dataJson.substring(contentStart, endIdx - 1)
                            val riverEntries = riverSection.split("},").map { it.trim() }

                            for (entry in riverEntries) {
                                if (entry.isBlank()) continue
                                val posMatch = Regex("\"([0-9]+,[0-9]+)\"").find(entry) ?: continue
                                val pos = posMatch.groupValues[1]

                                val flowDirectionMatch = Regex("\"flowDirection\":\\s*\"([A-Z_]+)\"").find(entry)
                                val flowSpeedMatch = Regex("\"flowSpeed\":\\s*([12])").find(entry)

                                if (flowDirectionMatch != null && flowSpeedMatch != null) {
                                    val flowDirection =
                                        de.egril.defender.model.RiverFlow
                                            .valueOf(flowDirectionMatch.groupValues[1])
                                    val flowSpeed = flowSpeedMatch.groupValues[1].toInt()

                                    val parts = pos.split(",")
                                    val position = Position(parts[0].toInt(), parts[1].toInt())
                                    if (position.x !in 0 until width || position.y !in 0 until height) {
                                        continue // Skip this stray river tile entry, keep the rest of the map
                                    }
                                    riverTiles[pos] =
                                        de.egril.defender.model
                                            .RiverTile(position, flowDirection, flowSpeed)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                    println("Error deserializing river tiles: ${e.message}")
                }
                // River tiles are optional, continue without them
            }

            // Parse optional targetInfo section
            val targetInfoMap = mutableMapOf<String, EditorTargetInfo>()
            try {
                if (dataJson.contains("\"targetInfo\"")) {
                    val startMarker = "\"targetInfo\": {"
                    val startIdx = dataJson.indexOf(startMarker)
                    if (startIdx != -1) {
                        val contentStart = startIdx + startMarker.length
                        var braceCount = 1
                        var endIdx = contentStart
                        while (endIdx < dataJson.length && braceCount > 0) {
                            when (dataJson[endIdx]) {
                                '{' -> braceCount++
                                '}' -> braceCount--
                            }
                            endIdx++
                        }
                        if (braceCount == 0) {
                            val targetSection = dataJson.substring(contentStart, endIdx - 1)
                            // Each entry looks like: "x,y": {"name": "...", "type": "..."}
                            val entryRegex = Regex(""""(\d+,\d+)":\s*\{([^}]*)\}""")
                            for (match in entryRegex.findAll(targetSection)) {
                                val pos = match.groupValues[1]
                                val body = "{${match.groupValues[2]}}"
                                val tName =
                                    try {
                                        de.egril.defender.utils.JsonUtils
                                            .extractValue(body, "name")
                                    } catch (e: Exception) {
                                        println("Error parsing target name for pos '$pos': ${e.message}")
                                        ""
                                    }
                                val tType =
                                    try {
                                        de.egril.defender.model.TargetType.valueOf(
                                            de.egril.defender.utils.JsonUtils
                                                .extractValue(body, "type"),
                                        )
                                    } catch (e: Exception) {
                                        println("Error parsing target type for pos '$pos': ${e.message}, defaulting to STANDARD")
                                        de.egril.defender.model.TargetType.STANDARD
                                    }
                                targetInfoMap[pos] = EditorTargetInfo(name = tName, type = tType)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error deserializing targetInfo: ${e.message}")
                // targetInfo is optional, continue without it
            }

            // Parse optional spawnPointInfo section
            val spawnPointInfoMap = mutableMapOf<String, SpawnPointType>()
            try {
                if (dataJson.contains("\"spawnPointInfo\"")) {
                    val startMarker = "\"spawnPointInfo\": {"
                    val startIdx = dataJson.indexOf(startMarker)
                    if (startIdx != -1) {
                        val contentStart = startIdx + startMarker.length
                        var braceCount = 1
                        var endIdx = contentStart
                        while (endIdx < dataJson.length && braceCount > 0) {
                            when (dataJson[endIdx]) {
                                '{' -> braceCount++
                                '}' -> braceCount--
                            }
                            endIdx++
                        }
                        if (braceCount == 0) {
                            val spawnSection = dataJson.substring(contentStart, endIdx - 1)
                            // Each entry looks like: "x,y": "LAND" or "x,y": "WATER"
                            val entryRegex = Regex(""""(\d+,\d+)":\s*"([A-Z_]+)"""")
                            for (match in entryRegex.findAll(spawnSection)) {
                                val pos = match.groupValues[1]
                                val typeStr = match.groupValues[2]
                                try {
                                    spawnPointInfoMap[pos] = SpawnPointType.valueOf(typeStr)
                                } catch (e: Exception) {
                                    println("Unknown SpawnPointType '$typeStr' for pos '$pos', defaulting to LAND")
                                    spawnPointInfoMap[pos] = SpawnPointType.LAND
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error deserializing spawnPointInfo: ${e.message}")
                // spawnPointInfo is optional, continue without it
            }

            val tileZones = parseTileZones(dataJson, width, height)

            return EditorMap(
                id = id,
                name = name,
                nameKey = nameKey,
                width = width,
                height = height,
                tiles = tiles,
                readyToUse = readyToUse,
                worldMapPosition = worldMapPosition,
                riverTiles = riverTiles,
                isOfficial = isOfficial,
                author = author,
                targetInfoMap = targetInfoMap,
                spawnPointInfoMap = spawnPointInfoMap,
                mapToolingInfo = mapToolingInfo,
                allowNoBuildableTiles = allowNoBuildableTiles,
                allowNoDirectPath = allowNoDirectPath,
                tileZones = tileZones,
                isValid = true,
            )
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("Error deserializing map: ${e.message}")
            }
            return null
        }
    }

    /**
     * Parse the optional "tileZones" section of a map (alternative terrain states switched by
     * level events). Entries outside the map bounds or with unsupported tile types are skipped.
     */
    internal fun parseTileZones(
        dataJson: String,
        width: Int,
        height: Int,
    ): List<de.egril.defender.model.TileZone> {
        val zonesSection = JsonUtils.extractJsonArrayForKey(dataJson, "tileZones")
        if (zonesSection.isBlank()) return emptyList()
        val zones = mutableListOf<de.egril.defender.model.TileZone>()
        val entryRegex = Regex(""""(\d+),(\d+)":\s*\{([^}]*)\}""")
        for (zoneJson in JsonUtils.splitJsonArray(zonesSection)) {
            val id = JsonUtils.extractStringValue(zoneJson, "id")
            if (id.isBlank()) continue
            val name = JsonUtils.extractStringValue(zoneJson, "name")
            val movable = JsonUtils.extractBooleanValue(zoneJson, "movable")
            val rotatable = JsonUtils.extractBooleanValue(zoneJson, "rotatable")
            val tilesSection = JsonUtils.extractJsonObjectForKey(zoneJson, "zoneTiles")
            val tiles = mutableMapOf<Position, TileType>()
            val riverTiles = mutableMapOf<Position, de.egril.defender.model.RiverTile>()
            for (match in entryRegex.findAll(tilesSection)) {
                val position = Position(match.groupValues[1].toInt(), match.groupValues[2].toInt())
                if (position.x !in 0 until width || position.y !in 0 until height) continue
                val body = "{${match.groupValues[3]}}"
                val type = runCatching { TileType.valueOf(JsonUtils.extractStringValue(body, "type")) }.getOrNull() ?: continue
                tiles[position] = type
                if (type == TileType.RIVER) {
                    val flow =
                        runCatching {
                            de.egril.defender.model.RiverFlow
                                .valueOf(JsonUtils.extractStringValue(body, "flowDirection"))
                        }.getOrDefault(de.egril.defender.model.RiverFlow.NONE)
                    val speed = JsonUtils.extractNumericValue(body, "flowSpeed").toIntOrNull()?.coerceIn(1, 2) ?: 1
                    riverTiles[position] =
                        de.egril.defender.model
                            .RiverTile(position, flow, speed)
                }
            }
            zones.add(
                de.egril.defender.model
                    .TileZone(id = id, name = name, tiles = tiles, riverTiles = riverTiles, movable = movable, rotatable = rotatable),
            )
        }
        return zones
    }

    internal fun parseTiles(
        dataJson: String,
        width: Int,
        height: Int,
    ): Map<String, TileType>? {
        val tiles = mutableMapOf<String, TileType>()
        val tilesKeyIndex = dataJson.indexOf("\"tiles\"")
        if (tilesKeyIndex == -1) {
            return emptyMap()
        }

        val colonIndex = dataJson.indexOf(':', tilesKeyIndex)
        if (colonIndex == -1) return null

        var index = colonIndex + 1
        while (index < dataJson.length && dataJson[index].isWhitespace()) {
            index++
        }
        if (index >= dataJson.length || dataJson[index] != '{') return null
        index++

        while (index < dataJson.length) {
            while (index < dataJson.length && (dataJson[index].isWhitespace() || dataJson[index] == ',')) {
                index++
            }
            if (index >= dataJson.length) return null
            if (dataJson[index] == '}') {
                return tiles
            }
            if (dataJson[index] != '"') return null

            val posEnd = dataJson.indexOf('"', startIndex = index + 1)
            if (posEnd == -1) return null
            val pos = dataJson.substring(index + 1, posEnd)
            val parts = pos.split(",")
            val x = parts.getOrNull(0)?.toIntOrNull()
            val y = parts.getOrNull(1)?.toIntOrNull()
            // Skip - rather than reject the whole map for - a single malformed/out-of-bounds tile
            // entry. Legacy maps sometimes carry a handful of stray positions (e.g. left over from
            // a resize) that shouldn't make an otherwise valid, hand-crafted map unusable.
            val isWithinBounds = parts.size == 2 && x != null && y != null && x in 0 until width && y in 0 until height

            index = posEnd + 1
            while (index < dataJson.length && dataJson[index].isWhitespace()) {
                index++
            }
            if (index >= dataJson.length || dataJson[index] != ':') return null
            index++
            while (index < dataJson.length && dataJson[index].isWhitespace()) {
                index++
            }
            if (index >= dataJson.length || dataJson[index] != '"') return null

            val typeEnd = dataJson.indexOf('"', startIndex = index + 1)
            if (typeEnd == -1) return null
            val typeStr = dataJson.substring(index + 1, typeEnd)
            val tileType =
                when (typeStr) {
                    "ISLAND" -> TileType.BUILD_AREA
                    else ->
                        try {
                            TileType.valueOf(typeStr)
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                }
            if (tileType != null && isWithinBounds) {
                tiles[pos] = tileType
            }
            index = typeEnd + 1
        }

        return null
    }
}
