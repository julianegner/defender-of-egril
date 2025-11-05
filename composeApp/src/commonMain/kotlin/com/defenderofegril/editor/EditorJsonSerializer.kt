package com.defenderofegril.editor

import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.DefenderType

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
        
        return """{
  "id": "${map.id}",
  "name": "${map.name}",
  "width": ${map.width},
  "height": ${map.height},
  "readyToUse": ${map.readyToUse},
  "tiles": {
    $tilesJson
  }
}"""
    }
    
    fun deserializeMap(json: String): EditorMap? {
        try {
            val id = extractValue(json, "id")
            val name = extractValue(json, "name")
            val width = extractValue(json, "width").toInt()
            val height = extractValue(json, "height").toInt()
            val readyToUse = try {
                extractValue(json, "readyToUse").toBoolean()
            } catch (e: Exception) {
                false  // Default to false for backward compatibility
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
            
            return EditorMap(id, name, width, height, tiles, readyToUse)
        } catch (e: Exception) {
            println("Error deserializing map: ${e.message}")
            return null
        }
    }
    
    fun serializeLevel(level: EditorLevel): String {
        val spawnsJson = level.enemySpawns.joinToString(",\n    ") { spawn ->
            """{"attackerType": "${spawn.attackerType.name}", "level": ${spawn.level}, "spawnTurn": ${spawn.spawnTurn}}"""
        }
        
        val towersJson = level.availableTowers.joinToString(", ") { "\"${it.name}\"" }
        
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
  "availableTowers": [$towersJson]
}"""
    }
    
    fun deserializeLevel(json: String): EditorLevel? {
        try {
            val id = extractValue(json, "id")
            val mapId = extractValue(json, "mapId")
            val title = extractValue(json, "title")
            val subtitle = extractValue(json, "subtitle")
            val startCoins = extractValue(json, "startCoins").toInt()
            val startHealthPoints = extractValue(json, "startHealthPoints").toInt()
            
            // Parse enemy spawns
            val spawns = mutableListOf<EditorEnemySpawn>()
            val spawnsSection = json.substringAfter("\"enemySpawns\": [").substringBefore("],")
            val spawnEntries = spawnsSection.split("},").map { it.trim() + "}" }
            
            for (entry in spawnEntries) {
                if (!entry.contains("attackerType")) continue
                val attackerType = AttackerType.valueOf(extractValue(entry, "attackerType"))
                val level = extractValue(entry, "level").toInt()
                val spawnTurn = extractValue(entry, "spawnTurn").toInt()
                spawns.add(EditorEnemySpawn(attackerType, level, spawnTurn))
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
            
            return EditorLevel(id, mapId, title, subtitle, startCoins, startHealthPoints, spawns, towers)
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
    
    private fun extractValue(json: String, key: String): String {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"|\"$key\"\\s*:\\s*([0-9]+)"
        val regex = Regex(pattern)
        val match = regex.find(json)
        return match?.let {
            it.groupValues[1].ifEmpty { it.groupValues[2] }
        } ?: ""
    }
}
