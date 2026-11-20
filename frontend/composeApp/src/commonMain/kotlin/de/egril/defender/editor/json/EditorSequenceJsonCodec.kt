package de.egril.defender.editor

import de.egril.defender.config.LogConfig
import de.egril.defender.editor.EditorJsonScanner.PROGRAM_NAME
import de.egril.defender.editor.EditorJsonScanner.extractDataSection
import de.egril.defender.editor.EditorJsonScanner.extractJsonArray
import de.egril.defender.editor.EditorJsonScanner.splitJsonArrayObjects
import de.egril.defender.utils.JsonUtils

/**
 * JSON codec for level sequences and world map data.
 */
internal object EditorSequenceJsonCodec {
    fun serializeSequence(sequence: LevelSequence): String {
        val idsJson = sequence.sequence.joinToString(", ") { "\"$it\"" }
        return """{
  "metadata": {
    "program": "$PROGRAM_NAME",
    "type": "sequence"
  },
  "data": {
    "sequence": [$idsJson]
  }
}"""
    }

    fun deserializeSequence(json: String): LevelSequence? {
        val dataJson = extractDataSection(json)
        try {
            val sequenceSection = dataJson.substringAfter("\"sequence\": [").substringBefore("]")
            if (sequenceSection.isBlank()) {
                return LevelSequence(emptyList())
            }
            val ids = sequenceSection.split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotBlank() }
            return LevelSequence(ids)
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("Error deserializing sequence: ${e.message}")
            }
            return null
        }
    }

    // ==================== World Map Data Serialization ====================

    fun serializeWorldMapData(data: WorldMapData): String {
        val locationsJson =
            data.locations.joinToString(",\n    ") { location ->
                val levelIdsJson = location.levelIds.joinToString(", ") { "\"$it\"" }
                val nameKeyJson =
                    if (location.nameKey != null) {
                        """,
      "nameKey": "${location.nameKey}""""
                    } else {
                        ""
                    }
                val iconResourceNameJson =
                    if (location.iconResourceName != null) {
                        """,
      "iconResourceName": "${location.iconResourceName}""""
                    } else {
                        ""
                    }
                """{
      "id": "${location.id}",
      "name": "${location.name}"$nameKeyJson,
      "position": {"x": ${location.position.x}, "y": ${location.position.y}},
      "levelIds": [$levelIdsJson]$iconResourceNameJson
    }"""
            }

        val pathsJson =
            data.paths.joinToString(",\n    ") { path ->
                val controlPointsJson =
                    if (path.controlPoints.isEmpty()) {
                        "[]"
                    } else {
                        "[" +
                            path.controlPoints.joinToString(", ") { pt ->
                                """{"x": ${pt.x}, "y": ${pt.y}}"""
                            } + "]"
                    }
                val segmentTypesJson =
                    if (path.segmentTypes.isEmpty()) {
                        ""
                    } else {
                        val typesArray = "[" + path.segmentTypes.joinToString(", ") { "\"${it.name}\"" } + "]"
                        """,
      "segmentTypes": $typesArray"""
                    }
                """{
      "fromLocationId": "${path.fromLocationId}",
      "toLocationId": "${path.toLocationId}",
      "controlPoints": $controlPointsJson,
      "type": "${path.type.name}"$segmentTypesJson
    }"""
            }

        val data = """{
  "locations": [
    $locationsJson
  ],
  "paths": [
    $pathsJson
  ]
}"""
        return """{
  "metadata": {
    "program": "$PROGRAM_NAME",
    "type": "worldmap"
  },
  "data": $data
}"""
    }

    fun deserializeWorldMapData(json: String): WorldMapData? {
        val dataJson = extractDataSection(json)
        try {
            val locations = mutableListOf<WorldMapLocationData>()
            val paths = mutableListOf<WorldMapPathData>()

            // Parse locations
            if (dataJson.contains("\"locations\"")) {
                val locationsSection = extractJsonArray(dataJson, "locations")
                val locationEntries = splitJsonArrayObjects(locationsSection)

                for (entry in locationEntries) {
                    if (!entry.contains("\"id\"")) continue

                    val id = JsonUtils.extractValue(entry, "id")
                    val name = JsonUtils.extractValue(entry, "name")
                    val nameKey =
                        try {
                            JsonUtils.extractValue(entry, "nameKey").takeIf { it.isNotEmpty() }
                        } catch (e: Exception) {
                            null // Optional field - null if not present
                        }

                    // Parse position
                    val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                    val posX = JsonUtils.extractNumericValue("{$posSection}", "x").toIntOrNull() ?: 0
                    val posY = JsonUtils.extractNumericValue("{$posSection}", "y").toIntOrNull() ?: 0
                    val position = WorldMapPoint(posX, posY)

                    // Parse level IDs
                    val levelIds = mutableListOf<String>()
                    val levelIdsSection = entry.substringAfter("\"levelIds\": [").substringBefore("]")
                    if (levelIdsSection.isNotBlank()) {
                        val idEntries = levelIdsSection.split(",").map { it.trim().removeSurrounding("\"") }
                        for (idEntry in idEntries) {
                            if (idEntry.isNotBlank()) {
                                levelIds.add(idEntry)
                            }
                        }
                    }

                    // Parse optional icon resource name
                    val iconResourceName =
                        try {
                            JsonUtils.extractValue(entry, "iconResourceName").takeIf { it.isNotEmpty() }
                        } catch (e: Exception) {
                            null // Optional field - null if not present
                        }

                    locations.add(WorldMapLocationData(id, name, nameKey, position, levelIds, iconResourceName))
                }
            }

            // Parse paths
            if (dataJson.contains("\"paths\"")) {
                val pathsSection = extractJsonArray(dataJson, "paths")
                val pathEntries = splitJsonArrayObjects(pathsSection)

                for (entry in pathEntries) {
                    if (!entry.contains("\"fromLocationId\"")) continue

                    val fromLocationId = JsonUtils.extractValue(entry, "fromLocationId")
                    val toLocationId = JsonUtils.extractValue(entry, "toLocationId")

                    // Parse control points
                    val controlPoints = mutableListOf<WorldMapPoint>()
                    if (entry.contains("\"controlPoints\"")) {
                        val cpSection = entry.substringAfter("\"controlPoints\": [").substringBefore("]")
                        if (cpSection.isNotBlank() && cpSection.contains("{")) {
                            val cpEntries = splitJsonArrayObjects(cpSection)
                            for (cp in cpEntries) {
                                val cpX = JsonUtils.extractNumericValue(cp, "x").toIntOrNull() ?: continue
                                val cpY = JsonUtils.extractNumericValue(cp, "y").toIntOrNull() ?: continue
                                controlPoints.add(WorldMapPoint(cpX, cpY))
                            }
                        }
                    }

                    // Parse connection type (default to ROAD for backward compatibility)
                    val type =
                        try {
                            if (entry.contains("\"type\"")) {
                                val typeStr = JsonUtils.extractValue(entry, "type")
                                ConnectionType.valueOf(typeStr)
                            } else {
                                ConnectionType.ROAD
                            }
                        } catch (e: Exception) {
                            ConnectionType.ROAD
                        }

                    // Parse segment types (optional for backward compatibility)
                    val segmentTypes = mutableListOf<ConnectionType>()
                    if (entry.contains("\"segmentTypes\"")) {
                        try {
                            val typesSection = entry.substringAfter("\"segmentTypes\": [").substringBefore("]")
                            if (typesSection.isNotBlank()) {
                                val typeEntries = typesSection.split(",").map { it.trim().removeSurrounding("\"") }
                                for (typeEntry in typeEntries) {
                                    if (typeEntry.isNotBlank()) {
                                        segmentTypes.add(ConnectionType.valueOf(typeEntry))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // If parsing fails, use empty list (will fall back to default type)
                        }
                    }

                    paths.add(WorldMapPathData(fromLocationId, toLocationId, controlPoints, type, segmentTypes))
                }
            }

            return WorldMapData(locations, paths)
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("Error deserializing world map data: ${e.message}")
            }
            return null
        }
    }
}
