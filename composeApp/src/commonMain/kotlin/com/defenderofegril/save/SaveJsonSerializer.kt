package com.defenderofegril.save

import com.defenderofegril.model.*

/**
 * JSON serialization for save data
 * Uses simple manual serialization like the editor
 */
object SaveJsonSerializer {
    
    fun serializeWorldMapSave(worldMap: WorldMapSave): String {
        val statusesJson = worldMap.levelStatuses.entries.joinToString(",\n    ") { (levelId, status) ->
            "\"$levelId\": \"${status.name}\""
        }
        
        return """{
  "levelStatuses": {
    $statusesJson
  }
}"""
    }
    
    fun deserializeWorldMapSave(json: String): WorldMapSave? {
        try {
            val statuses = mutableMapOf<Int, LevelStatus>()
            val statusesSection = json.substringAfter("\"levelStatuses\": {")
                .substringBefore("}")
                .replace("\",", "\";")
            val statusEntries = statusesSection.split(";").map { it.trim() }
            
            for (entry in statusEntries) {
                if (entry.isBlank()) continue
                val parts = entry.split(":")
                if (parts.size != 2) continue
                
                val levelId = parts[0].trim().removeSurrounding("\"").toInt()
                val statusStr = parts[1].trim().removeSurrounding("\"")
                statuses[levelId] = LevelStatus.valueOf(statusStr)
            }
            
            return WorldMapSave(statuses)
        } catch (e: Exception) {
            println("Error deserializing world map save: ${e.message}")
            return null
        }
    }
    
    fun serializeSavedGame(savedGame: SavedGame): String {
        val defendersJson = savedGame.defenders.joinToString(",\n    ") { defender ->
            """{
      "id": ${defender.id},
      "type": "${defender.type.name}",
      "position": {"x": ${defender.position.x}, "y": ${defender.position.y}},
      "level": ${defender.level},
      "buildTimeRemaining": ${defender.buildTimeRemaining},
      "placedOnTurn": ${defender.placedOnTurn},
      "actionsRemaining": ${defender.actionsRemaining}
    }"""
        }
        
        val attackersJson = savedGame.attackers.joinToString(",\n    ") { attacker ->
            """{
      "id": ${attacker.id},
      "type": "${attacker.type.name}",
      "position": {"x": ${attacker.position.x}, "y": ${attacker.position.y}},
      "level": ${attacker.level},
      "currentHealth": ${attacker.currentHealth},
      "isDefeated": ${attacker.isDefeated}
    }"""
        }
        
        val attackersToSpawnJson = savedGame.attackersToSpawn.joinToString(", ") { "\"${it.name}\"" }
        
        val fieldEffectsJson = savedGame.fieldEffects.joinToString(",\n    ") { effect ->
            val attackerIdStr = effect.attackerId?.toString() ?: "null"
            """{
      "position": {"x": ${effect.position.x}, "y": ${effect.position.y}},
      "type": "${effect.type.name}",
      "damage": ${effect.damage},
      "turnsRemaining": ${effect.turnsRemaining},
      "defenderId": ${effect.defenderId},
      "attackerId": $attackerIdStr
    }"""
        }
        
        val trapsJson = savedGame.traps.joinToString(",\n    ") { trap ->
            """{
      "position": {"x": ${trap.position.x}, "y": ${trap.position.y}},
      "damage": ${trap.damage},
      "mineId": ${trap.mineId}
    }"""
        }
        
        // Escape comment for JSON (handle quotes and newlines)
        val commentJson = savedGame.comment?.let { comment ->
            val escaped = comment
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
            "\"$escaped\""
        } ?: "null"
        
        return """{
  "id": "${savedGame.id}",
  "timestamp": ${savedGame.timestamp},
  "levelId": ${savedGame.levelId},
  "levelName": "${savedGame.levelName}",
  "turnNumber": ${savedGame.turnNumber},
  "coins": ${savedGame.coins},
  "healthPoints": ${savedGame.healthPoints},
  "phase": "${savedGame.phase.name}",
  "defenders": [
    $defendersJson
  ],
  "attackers": [
    $attackersJson
  ],
  "nextDefenderId": ${savedGame.nextDefenderId},
  "nextAttackerId": ${savedGame.nextAttackerId},
  "currentWaveIndex": ${savedGame.currentWaveIndex},
  "spawnCounter": ${savedGame.spawnCounter},
  "attackersToSpawn": [$attackersToSpawnJson],
  "fieldEffects": [
    $fieldEffectsJson
  ],
  "traps": [
    $trapsJson
  ],
  "comment": $commentJson
}"""
    }
    
    fun deserializeSavedGame(json: String): SavedGame? {
        try {
            val id = extractValue(json, "id")
            val timestamp = extractValue(json, "timestamp").toLong()
            val levelId = extractValue(json, "levelId").toInt()
            val levelName = extractValue(json, "levelName")
            val turnNumber = extractValue(json, "turnNumber").toInt()
            val coins = extractValue(json, "coins").toInt()
            val healthPoints = extractValue(json, "healthPoints").toInt()
            val phase = GamePhase.valueOf(extractValue(json, "phase"))
            val nextDefenderId = extractValue(json, "nextDefenderId").toInt()
            val nextAttackerId = extractValue(json, "nextAttackerId").toInt()
            val currentWaveIndex = extractValue(json, "currentWaveIndex").toInt()
            val spawnCounter = extractValue(json, "spawnCounter").toInt()
            
            // Parse defenders
            val defenders = mutableListOf<SavedDefender>()
            val defendersSection = json.substringAfter("\"defenders\": [").substringBefore("],")
            if (defendersSection.isNotBlank()) {
                val defenderEntries = splitJsonArray(defendersSection)
                for (entry in defenderEntries) {
                    defenders.add(parseSavedDefender(entry))
                }
            }
            
            // Parse attackers
            val attackers = mutableListOf<SavedAttacker>()
            val attackersSection = json.substringAfter("\"attackers\": [").substringBefore("],")
            if (attackersSection.isNotBlank()) {
                val attackerEntries = splitJsonArray(attackersSection)
                for (entry in attackerEntries) {
                    attackers.add(parseSavedAttacker(entry))
                }
            }
            
            // Parse attackersToSpawn
            val attackersToSpawn = mutableListOf<AttackerType>()
            val attackersToSpawnSection = json.substringAfter("\"attackersToSpawn\": [").substringBefore("],")
            if (attackersToSpawnSection.isNotBlank()) {
                val types = attackersToSpawnSection.split(",").map { it.trim().removeSurrounding("\"") }
                for (typeStr in types) {
                    if (typeStr.isNotBlank()) {
                        attackersToSpawn.add(AttackerType.valueOf(typeStr))
                    }
                }
            }
            
            // Parse field effects
            val fieldEffects = mutableListOf<SavedFieldEffect>()
            val fieldEffectsSection = json.substringAfter("\"fieldEffects\": [").substringBefore("],")
            if (fieldEffectsSection.isNotBlank()) {
                val effectEntries = splitJsonArray(fieldEffectsSection)
                for (entry in effectEntries) {
                    fieldEffects.add(parseSavedFieldEffect(entry))
                }
            }
            
            // Parse traps
            val traps = mutableListOf<SavedTrap>()
            val trapsSection = json.substringAfter("\"traps\": [").substringBeforeLast("]")
            if (trapsSection.isNotBlank()) {
                val trapEntries = splitJsonArray(trapsSection)
                for (entry in trapEntries) {
                    traps.add(parseSavedTrap(entry))
                }
            }
            
            // Parse comment (optional field, may not exist in older saves)
            val comment = try {
                extractCommentValue(json)
            } catch (e: Exception) {
                null  // If comment field doesn't exist (old save), default to null
            }
            
            return SavedGame(
                id = id,
                timestamp = timestamp,
                levelId = levelId,
                levelName = levelName,
                turnNumber = turnNumber,
                coins = coins,
                healthPoints = healthPoints,
                phase = phase,
                defenders = defenders,
                attackers = attackers,
                nextDefenderId = nextDefenderId,
                nextAttackerId = nextAttackerId,
                currentWaveIndex = currentWaveIndex,
                spawnCounter = spawnCounter,
                attackersToSpawn = attackersToSpawn,
                fieldEffects = fieldEffects,
                traps = traps,
                comment = comment
            )
        } catch (e: Exception) {
            println("Error deserializing saved game: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    private fun parseSavedDefender(json: String): SavedDefender {
        val id = extractValue(json, "id").toInt()
        val type = DefenderType.valueOf(extractValue(json, "type"))
        val position = parsePosition(json)
        val level = extractValue(json, "level").toInt()
        val buildTimeRemaining = extractValue(json, "buildTimeRemaining").toInt()
        val placedOnTurn = extractValue(json, "placedOnTurn").toInt()
        // Backward compatibility: default to 0 if field doesn't exist in old saves
        val actionsRemaining = try {
            extractValue(json, "actionsRemaining").toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
        
        return SavedDefender(id, type, position, level, buildTimeRemaining, placedOnTurn, actionsRemaining)
    }
    
    private fun parseSavedAttacker(json: String): SavedAttacker {
        val id = extractValue(json, "id").toInt()
        val type = AttackerType.valueOf(extractValue(json, "type"))
        val position = parsePosition(json)
        val level = extractValue(json, "level").toInt()
        val currentHealth = extractValue(json, "currentHealth").toInt()
        val isDefeated = extractValue(json, "isDefeated").toBoolean()
        
        return SavedAttacker(id, type, position, level, currentHealth, isDefeated)
    }
    
    private fun parseSavedFieldEffect(json: String): SavedFieldEffect {
        val position = parsePosition(json)
        val type = FieldEffectType.valueOf(extractValue(json, "type"))
        val damage = extractValue(json, "damage").toInt()
        val turnsRemaining = extractValue(json, "turnsRemaining").toInt()
        val defenderId = extractValue(json, "defenderId").toInt()
        val attackerIdStr = extractValue(json, "attackerId")
        val attackerId = if (attackerIdStr == "null") null else attackerIdStr.toInt()
        
        return SavedFieldEffect(position, type, damage, turnsRemaining, defenderId, attackerId)
    }
    
    private fun parseSavedTrap(json: String): SavedTrap {
        val position = parsePosition(json)
        val damage = extractValue(json, "damage").toInt()
        val mineId = extractValue(json, "mineId").toInt()
        return SavedTrap(position, damage, mineId)
    }
    
    private fun parsePosition(json: String): Position {
        val posSection = json.substringAfter("\"position\": {").substringBefore("}")
        val x = extractValue(posSection, "x").toInt()
        val y = extractValue(posSection, "y").toInt()
        return Position(x, y)
    }
    
    private fun extractValue(json: String, key: String): String {
        val pattern = "\"$key\":\\s*\"?([^,\"\\}\\]]+)\"?"
        val regex = Regex(pattern)
        val match = regex.find(json)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }
    
    private fun splitJsonArray(arrayContent: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var currentItem = StringBuilder()
        
        for (char in arrayContent) {
            when (char) {
                '{' -> {
                    depth++
                    currentItem.append(char)
                }
                '}' -> {
                    depth--
                    currentItem.append(char)
                    if (depth == 0 && currentItem.isNotBlank()) {
                        result.add(currentItem.toString().trim())
                        currentItem = StringBuilder()
                    }
                }
                ',' -> {
                    if (depth == 0) {
                        // Skip comma between items
                    } else {
                        currentItem.append(char)
                    }
                }
                else -> {
                    if (depth > 0 || !char.isWhitespace()) {
                        currentItem.append(char)
                    }
                }
            }
        }
        
        if (currentItem.isNotBlank()) {
            result.add(currentItem.toString().trim())
        }
        
        return result
    }
    
    fun serializeSaveGameMetadata(metadata: SaveGameMetadata): String {
        return """{
  "id": "${metadata.id}",
  "timestamp": ${metadata.timestamp},
  "levelId": ${metadata.levelId},
  "levelName": "${metadata.levelName}",
  "turnNumber": ${metadata.turnNumber},
  "towerCount": ${metadata.towerCount},
  "enemyCount": ${metadata.enemyCount}
}"""
    }
    
    /**
     * Extract comment value from JSON, handling escaped characters properly
     */
    private fun extractCommentValue(json: String): String? {
        // Find "comment": in JSON
        val commentKey = "\"comment\":"
        val commentStart = json.indexOf(commentKey)
        if (commentStart == -1) {
            return null
        }
        
        // Skip whitespace after colon
        var pos = commentStart + commentKey.length
        while (pos < json.length && json[pos].isWhitespace()) {
            pos++
        }
        
        // Check if value is null
        if (json.substring(pos).startsWith("null")) {
            return null
        }
        
        // Value should start with a quote
        if (pos >= json.length || json[pos] != '"') {
            return null
        }
        
        // Move past opening quote
        pos++
        
        // Extract the string value, handling escaped characters
        val result = StringBuilder()
        var escaped = false
        
        while (pos < json.length) {
            val char = json[pos]
            
            if (escaped) {
                // Handle standard JSON escape sequences
                when (char) {
                    'n' -> result.append('\n')
                    'r' -> result.append('\r')
                    't' -> result.append('\t')
                    '"' -> result.append('"')
                    '\\' -> result.append('\\')
                    else -> {
                        // Unknown escape sequence - preserve as literal characters
                        // This is acceptable for game save files since we control the serialization
                        result.append('\\')
                        result.append(char)
                    }
                }
                escaped = false
            } else {
                when (char) {
                    '\\' -> escaped = true
                    '"' -> {
                        // End of string value
                        return result.toString()
                    }
                    else -> result.append(char)
                }
            }
            
            pos++
        }
        
        // If we reach here, the string wasn't properly terminated
        return null
    }
}
