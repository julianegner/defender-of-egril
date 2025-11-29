package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import de.egril.defender.utils.JsonUtils

/**
 * Simple JSON serialization for editor data
 * Since kotlinx.serialization has issues with enums in multiplatform,
 * we'll use manual JSON serialization
 */
object EditorJsonSerializer {
    
    fun serializeMap(map: EditorMap): String {
        val tilesJson = map.tiles.entries.joinToString(",\n    ") { (pos, type) ->
            "\"$pos\": \"${type.name}\""
        }
        
        val worldMapPositionJson = if (map.worldMapPosition != null) {
            ",\n  \"worldMapPosition\": {\"x\": ${map.worldMapPosition.x}, \"y\": ${map.worldMapPosition.y}}"
        } else ""
        
        return """{
  "id": "${map.id}",
  "name": "${map.name}",
  "width": ${map.width},
  "height": ${map.height},
  "readyToUse": ${map.readyToUse}$worldMapPositionJson,
  "tiles": {
    $tilesJson
  }
}"""
    }
    
    fun deserializeMap(json: String): EditorMap? {
        try {
            val id = JsonUtils.extractValue(json, "id")
            val name = JsonUtils.extractValue(json, "name")
            val width = JsonUtils.extractValue(json, "width").toInt()
            val height = JsonUtils.extractValue(json, "height").toInt()
            val readyToUse = try {
                JsonUtils.extractBooleanValue(json, "readyToUse")
            } catch (e: Exception) {
                false  // Default to false for backward compatibility
            }
            
            // Parse optional world map position
            val worldMapPosition = try {
                val positionSection = json.substringAfter("\"worldMapPosition\":", "")
                if (positionSection.isNotEmpty()) {
                    val posObj = positionSection.substringAfter("{").substringBefore("}")
                    val x = JsonUtils.extractNumericValue("{$posObj}", "x").toIntOrNull()
                    val y = JsonUtils.extractNumericValue("{$posObj}", "y").toIntOrNull()
                    if (x != null && y != null) Position(x, y) else null
                } else null
            } catch (e: Exception) {
                null  // Optional field - null if not present
            }
            
            val tiles = mutableMapOf<String, TileType>()
            val tilesSection = json.substringAfter("\"tiles\": {")
                .substringBefore("}")
                .replace("\",","\";")
            val tileEntries = tilesSection.split(";").map { it.trim() }

            for (entry in tileEntries) {
                if (entry.isBlank()) continue
                val parts = entry.split(":")
                if (parts.size != 2) continue

                val pos = parts[0].trim().removeSurrounding("\"")
                val typeStr = parts[1].trim().removeSurrounding("\"")
                tiles[pos] = TileType.valueOf(typeStr)
            }
            
            return EditorMap(id, name, width, height, tiles, readyToUse, worldMapPosition)
        } catch (e: Exception) {
            println("Error deserializing map: ${e.message}")
            return null
        }
    }
    
    fun serializeLevel(level: EditorLevel): String {
        val spawnsJson = level.enemySpawns.joinToString(",\n    ") { spawn ->
            val spawnPointJson = if (spawn.spawnPoint != null) {
                """, "spawnPoint": {"x": ${spawn.spawnPoint.x}, "y": ${spawn.spawnPoint.y}}"""
            } else {
                ""
            }
            """{"attackerType": "${spawn.attackerType.name}", "level": ${spawn.level}, "spawnTurn": ${spawn.spawnTurn}$spawnPointJson}"""
        }
        
        val towersJson = level.availableTowers.joinToString(", ") { "\"${it.name}\"" }
        
        val waypointsJson = level.waypoints.joinToString(",\n    ") { waypoint ->
            """{"position": {"x": ${waypoint.position.x}, "y": ${waypoint.position.y}}, "nextTargetPosition": {"x": ${waypoint.nextTargetPosition.x}, "y": ${waypoint.nextTargetPosition.y}}}"""
        }
        
        val prerequisitesJson = level.prerequisites.joinToString(", ") { "\"$it\"" }
        
        val requiredCountJson = if (level.requiredPrerequisiteCount != null) {
            ",\n  \"requiredPrerequisiteCount\": ${level.requiredPrerequisiteCount}"
        } else {
            ""
        }
        
        return """{
  "id": "${level.id}",
  "mapId": "${level.mapId}",
  "title": "${level.title}",
  "subtitle": "${level.subtitle}",
  "startCoins": ${level.startCoins},
  "startHealthPoints": ${level.startHealthPoints},
  "enemySpawns": [
    $spawnsJson
  ],
  "availableTowers": [$towersJson],
  "waypoints": [
    $waypointsJson
  ],
  "prerequisites": [$prerequisitesJson]$requiredCountJson
}"""
    }
    
    fun deserializeLevel(json: String): EditorLevel? {
        try {
            val id = JsonUtils.extractValue(json, "id")
            val mapId = JsonUtils.extractValue(json, "mapId")
            val title = JsonUtils.extractValue(json, "title")
            val subtitle = JsonUtils.extractValue(json, "subtitle")
            val startCoins = JsonUtils.extractValue(json, "startCoins").toInt()
            val startHealthPoints = JsonUtils.extractValue(json, "startHealthPoints").toInt()
            
            // Parse enemy spawns
            val spawns = mutableListOf<EditorEnemySpawn>()
            val spawnsSection = json.substringAfter("\"enemySpawns\": [").substringBefore("],")
            val spawnEntries = spawnsSection.split("},").map { it.trim() + "}" }
            
            for (entry in spawnEntries) {
                if (!entry.contains("attackerType")) continue
                val attackerType = AttackerType.valueOf(JsonUtils.extractValue(entry, "attackerType"))
                val level = JsonUtils.extractValue(entry, "level").toInt()
                val spawnTurn = JsonUtils.extractValue(entry, "spawnTurn").toInt()
                
                // Parse spawn point (optional for backward compatibility)
                val spawnPoint = if (entry.contains("\"spawnPoint\"")) {
                    try {
                        val spawnPointSection = entry.substringAfter("\"spawnPoint\": {").substringBefore("}")
                        val x = JsonUtils.extractValue("{$spawnPointSection}", "x").toInt()
                        val y = JsonUtils.extractValue("{$spawnPointSection}", "y").toInt()
                        Position(x, y)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                
                spawns.add(EditorEnemySpawn(attackerType, level, spawnTurn, spawnPoint))
            }
            
            // Parse available towers
            val towers = mutableSetOf<DefenderType>()
            val towersSection = json.substringAfter("\"availableTowers\": [").substringBefore("]")
            val towerEntries = towersSection.split(",").map { it.trim().removeSurrounding("\"") }
            
            for (entry in towerEntries) {
                if (entry.isNotBlank()) {
                    towers.add(DefenderType.valueOf(entry))
                }
            }
            
            // Parse waypoints (optional for backward compatibility)
            val waypoints = mutableListOf<EditorWaypoint>()
            if (json.contains("\"waypoints\"")) {
                try {
                    // Find the waypoints array section
                    val waypointsStart = json.indexOf("\"waypoints\": [")
                    if (waypointsStart >= 0) {
                        val arrayStart = json.indexOf("[", waypointsStart)
                        var arrayEnd = arrayStart + 1
                        var depth = 1
                        
                        // Find matching closing bracket
                        while (depth > 0 && arrayEnd < json.length) {
                            when (json[arrayEnd]) {
                                '[' -> depth++
                                ']' -> depth--
                            }
                            arrayEnd++
                        }
                        
                        val waypointsSection = json.substring(arrayStart + 1, arrayEnd - 1).trim()
                        
                        if (waypointsSection.isNotEmpty()) {
                            // Split by "}," but need to be careful about nested braces
                            val waypointEntries = mutableListOf<String>()
                            var currentEntry = ""
                            var braceDepth = 0
                            
                            for (i in waypointsSection.indices) {
                                val char = waypointsSection[i]
                                currentEntry += char
                                
                                when (char) {
                                    '{' -> braceDepth++
                                    '}' -> {
                                        braceDepth--
                                        if (braceDepth == 0 && i + 1 < waypointsSection.length && waypointsSection[i + 1] == ',') {
                                            waypointEntries.add(currentEntry.trim())
                                            currentEntry = ""
                                            // Skip the comma
                                            continue
                                        }
                                    }
                                }
                            }
                            if (currentEntry.trim().isNotEmpty()) {
                                waypointEntries.add(currentEntry.trim())
                            }
                            
                            for (entry in waypointEntries) {
                                if (!entry.contains("position")) continue
                                
                                // Extract position
                                val posSection = entry.substringAfter("\"position\": {").substringBefore("},")
                                val posX = JsonUtils.extractValue(posSection, "x").toInt()
                                val posY = JsonUtils.extractValue(posSection, "y").toInt()
                                val position = Position(posX, posY)
                                
                                // Extract nextTargetPosition - it's the last object, so use } not },
                                val targetSection = entry.substringAfter("\"nextTargetPosition\": {").substringBefore("}")
                                val targetX = JsonUtils.extractValue(targetSection, "x").toInt()
                                val targetY = JsonUtils.extractValue(targetSection, "y").toInt()
                                val nextTargetPosition = Position(targetX, targetY)
                                
                                waypoints.add(EditorWaypoint(position, nextTargetPosition))
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Error parsing waypoints (continuing without them): ${e.message}")
                    e.printStackTrace()
                    // Continue without waypoints for backward compatibility
                }
            }
            
            // Parse prerequisites (optional for backward compatibility)
            val prerequisites = mutableSetOf<String>()
            if (json.contains("\"prerequisites\"")) {
                try {
                    val prerequisitesSection = json.substringAfter("\"prerequisites\": [").substringBefore("]")
                    if (prerequisitesSection.isNotBlank()) {
                        val prereqEntries = prerequisitesSection.split(",").map { it.trim().removeSurrounding("\"") }
                        for (entry in prereqEntries) {
                            if (entry.isNotBlank()) {
                                prerequisites.add(entry)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Error parsing prerequisites (continuing without them): ${e.message}")
                    // Continue without prerequisites for backward compatibility
                }
            }
            
            // Parse requiredPrerequisiteCount (optional)
            val requiredPrerequisiteCount: Int? = if (json.contains("\"requiredPrerequisiteCount\"")) {
                try {
                    JsonUtils.extractValue(json, "requiredPrerequisiteCount").toInt()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            
            return EditorLevel(id, mapId, title, subtitle, startCoins, startHealthPoints, spawns, towers, waypoints, prerequisites, requiredPrerequisiteCount)
        } catch (e: Exception) {
            println("Error deserializing level: ${e.message}")
            return null
        }
    }
    
    fun serializeSequence(sequence: LevelSequence): String {
        val idsJson = sequence.sequence.joinToString(", ") { "\"$it\"" }
        return """{
  "sequence": [$idsJson]
}"""
    }
    
    fun deserializeSequence(json: String): LevelSequence? {
        try {
            val sequenceSection = json.substringAfter("\"sequence\": [").substringBefore("]")
            if (sequenceSection.isBlank()) {
                return LevelSequence(emptyList())
            }
            val ids = sequenceSection.split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotBlank() }
            return LevelSequence(ids)
        } catch (e: Exception) {
            println("Error deserializing sequence: ${e.message}")
            return null
        }
    }
}
