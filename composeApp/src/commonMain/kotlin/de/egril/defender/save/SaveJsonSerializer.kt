package de.egril.defender.save

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import de.egril.defender.utils.JsonUtils
import de.egril.defender.config.LogConfig

/**
 * JSON serialization for save data
 * Uses simple manual serialization like the editor
 */
object SaveJsonSerializer {
    
    fun serializeWorldMapSave(worldMap: WorldMapSave): String {
        val statusesJson = worldMap.levelStatuses.entries.joinToString(",\n    ") { (editorLevelId, status) ->
            "\"$editorLevelId\": \"${status.name}\""
        }
        
        return """{
  "levelStatuses": {
    $statusesJson
  }
}"""
    }
    
    fun deserializeWorldMapSave(json: String): WorldMapSave? {
        try {
            val statuses = mutableMapOf<String, LevelStatus>()
            val statusesSection = json.substringAfter("\"levelStatuses\": {")
                .substringBefore("}")
                .replace("\",", "\";")
            val statusEntries = statusesSection.split(";").map { it.trim() }
            
            for (entry in statusEntries) {
                if (entry.isBlank()) continue
                val parts = entry.split(":")
                if (parts.size != 2) continue
                
                val editorLevelId = parts[0].trim().removeSurrounding("\"")
                val statusStr = parts[1].trim().removeSurrounding("\"")
                statuses[editorLevelId] = LevelStatus.valueOf(statusStr)
            }
            
            return WorldMapSave(statuses)
        } catch (e: Exception) {
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
            println("Error deserializing world map save: ${e.message}")
            }
            return null
        }
    }
    
    fun serializeSavedGame(savedGame: SavedGame): String {
        val defendersJson = savedGame.defenders.joinToString(",\n    ") { defender ->
            val dragonNameStr = if (defender.dragonName != null) "\"${defender.dragonName}\"" else "null"
            val raftIdStr = defender.raftId?.toString() ?: "null"
            val towerBaseBarricadeIdStr = defender.towerBaseBarricadeId?.toString() ?: "null"
            """{
      "id": ${defender.id},
      "type": "${defender.type.name}",
      "position": {"x": ${defender.position.x}, "y": ${defender.position.y}},
      "level": ${defender.level},
      "buildTimeRemaining": ${defender.buildTimeRemaining},
      "placedOnTurn": ${defender.placedOnTurn},
      "actionsRemaining": ${defender.actionsRemaining},
      "dragonName": $dragonNameStr,
      "raftId": $raftIdStr,
      "towerBaseBarricadeId": $towerBaseBarricadeIdStr
    }"""
        }
        
        val attackersJson = savedGame.attackers.joinToString(",\n    ") { attacker ->
            val dragonNameStr = if (attacker.dragonName != null) "\"${attacker.dragonName}\"" else "null"
            """{
      "id": ${attacker.id},
      "type": "${attacker.type.name}",
      "position": {"x": ${attacker.position.x}, "y": ${attacker.position.y}},
      "level": ${attacker.level},
      "currentHealth": ${attacker.currentHealth},
      "isDefeated": ${attacker.isDefeated},
      "dragonName": $dragonNameStr,
      "movementPenalty": ${attacker.movementPenalty}
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
      "defenderId": ${trap.defenderId},
      "type": "${trap.type}"
    }"""
        }
        
        val raftsJson = savedGame.rafts.joinToString(",\n    ") { raft ->
            """{
      "id": ${raft.id},
      "defenderId": ${raft.defenderId},
      "position": {"x": ${raft.position.x}, "y": ${raft.position.y}}
    }"""
        }
        
        val barricadesJson = savedGame.barricades.joinToString(",\n    ") { barricade ->
            val supportedTowerIdStr = barricade.supportedTowerId?.toString() ?: "null"
            """{
      "position": {"x": ${barricade.position.x}, "y": ${barricade.position.y}},
      "healthPoints": ${barricade.healthPoints},
      "defenderId": ${barricade.defenderId},
      "id": ${barricade.id},
      "supportedTowerId": $supportedTowerIdStr
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
        
        // Map ID (optional field for backward compatibility)
        val mapIdJson = savedGame.mapId?.let { "\"$it\"" } ?: "null"
        
        // World map save (optional field)
        val worldMapSaveJson = savedGame.worldMapSave?.let { worldMap ->
            val statusesJson = worldMap.levelStatuses.entries.joinToString(", ") { (editorLevelId, status) ->
                "\"$editorLevelId\": \"${status.name}\""
            }
            """{"levelStatuses": {$statusesJson}}"""
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
  "rafts": [
    $raftsJson
  ],
  "nextRaftId": ${savedGame.nextRaftId},
  "barricades": [
    $barricadesJson
  ],
  "comment": $commentJson,
  "mapId": $mapIdJson,
  "worldMapSave": $worldMapSaveJson
}"""
    }
    
    fun deserializeSavedGame(json: String): SavedGame? {
        try {
            val id = JsonUtils.extractValue(json, "id")
            val timestamp = JsonUtils.extractValue(json, "timestamp").toLong()
            val levelId = JsonUtils.extractValue(json, "levelId").toInt()
            val levelName = JsonUtils.extractValue(json, "levelName")
            val turnNumber = JsonUtils.extractValue(json, "turnNumber").toInt()
            val coins = JsonUtils.extractValue(json, "coins").toInt()
            val healthPoints = JsonUtils.extractValue(json, "healthPoints").toInt()
            val phase = GamePhase.valueOf(JsonUtils.extractValue(json, "phase"))
            val nextDefenderId = JsonUtils.extractValue(json, "nextDefenderId").toInt()
            val nextAttackerId = JsonUtils.extractValue(json, "nextAttackerId").toInt()
            val currentWaveIndex = JsonUtils.extractValue(json, "currentWaveIndex").toInt()
            val spawnCounter = JsonUtils.extractValue(json, "spawnCounter").toInt()
            
            // Parse defenders
            val defenders = mutableListOf<SavedDefender>()
            val defendersSection = json.substringAfter("\"defenders\": [").substringBefore("],")
            if (defendersSection.isNotBlank()) {
                val defenderEntries = JsonUtils.splitJsonArray(defendersSection)
                for (entry in defenderEntries) {
                    defenders.add(parseSavedDefender(entry))
                }
            }
            
            // Parse attackers
            val attackers = mutableListOf<SavedAttacker>()
            val attackersSection = json.substringAfter("\"attackers\": [").substringBefore("],")
            if (attackersSection.isNotBlank()) {
                val attackerEntries = JsonUtils.splitJsonArray(attackersSection)
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
                val effectEntries = JsonUtils.splitJsonArray(fieldEffectsSection)
                for (entry in effectEntries) {
                    fieldEffects.add(parseSavedFieldEffect(entry))
                }
            }
            
            // Parse traps
            val traps = mutableListOf<SavedTrap>()
            val trapsSection = try {
                json.substringAfter("\"traps\": [").substringBefore("],")
            } catch (e: Exception) {
                ""  // Handle old saves without rafts field
            }
            if (trapsSection.isNotBlank()) {
                val trapEntries = JsonUtils.splitJsonArray(trapsSection)
                for (entry in trapEntries) {
                    traps.add(parseSavedTrap(entry))
                }
            }
            
            // Parse rafts (optional field for backward compatibility with old saves)
            val rafts = mutableListOf<SavedRaft>()
            if (json.contains("\"rafts\":")) {
                val raftsSection = try {
                    json.substringAfter("\"rafts\": [").substringBefore("],")
                } catch (e: Exception) {
                    ""  // Old saves don't have rafts
                }
                if (raftsSection.isNotBlank()) {
                    val raftEntries = JsonUtils.splitJsonArray(raftsSection)
                    for (entry in raftEntries) {
                        rafts.add(parseSavedRaft(entry))
                    }
                }
            }
            
            // Parse nextRaftId (optional field for backward compatibility)
            val nextRaftId = try {
                JsonUtils.extractValue(json, "nextRaftId").toInt()
            } catch (e: Exception) {
                1  // Default to 1 for old saves
            }
            
            // Parse barricades (optional field for backward compatibility with old saves)
            val barricades = mutableListOf<SavedBarricade>()
            if (json.contains("\"barricades\":")) {
                val barricadesSection = try {
                    json.substringAfter("\"barricades\": [").substringBefore("],")
                } catch (e: Exception) {
                    ""  // Old saves don't have barricades
                }
                if (barricadesSection.isNotBlank()) {
                    val barricadeEntries = JsonUtils.splitJsonArray(barricadesSection)
                    for (entry in barricadeEntries) {
                        barricades.add(parseSavedBarricade(entry))
                    }
                }
            }
            
            // Parse comment (optional field, may not exist in older saves)
            val comment = try {
                extractCommentValue(json)
            } catch (e: Exception) {
                null  // If comment field doesn't exist (old save), default to null
            }
            
            // Parse mapId (optional field, may not exist in older saves)
            val mapId = try {
                val value = JsonUtils.extractValue(json, "mapId")
                if (value.isBlank() || value == "null") null else value
            } catch (e: Exception) {
                null  // If mapId field doesn't exist (old save), default to null
            }
            
            // Parse worldMapSave (optional field, may not exist in older saves)
            val worldMapSave = try {
                if (json.contains("\"worldMapSave\":")) {
                    val worldMapSection = json.substringAfter("\"worldMapSave\":")
                        .trim()
                    
                    // Check if it's null
                    if (worldMapSection.startsWith("null")) {
                        null
                    } else {
                        // Extract the world map object
                        val worldMapJson = worldMapSection.substringAfter("{").substringBefore("}}")
                            .let { "{$it}" }
                        deserializeWorldMapSave(worldMapJson)
                    }
                } else {
                    null  // Field doesn't exist in old saves
                }
            } catch (e: Exception) {
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Warning: Failed to parse worldMapSave: ${e.message}")
                }
                null  // If worldMapSave field doesn't exist or is malformed, default to null
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
                comment = comment,
                mapId = mapId,
                rafts = rafts,
                nextRaftId = nextRaftId,
                barricades = barricades,
                worldMapSave = worldMapSave
            )
        } catch (e: Exception) {
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
            println("Error deserializing saved game: ${e.message}")
            }
            e.printStackTrace()
            return null
        }
    }
    
    private fun parseSavedDefender(json: String): SavedDefender {
        val id = JsonUtils.extractValue(json, "id").toInt()
        val type = DefenderType.valueOf(JsonUtils.extractValue(json, "type"))
        val position = parsePosition(json)
        val level = JsonUtils.extractValue(json, "level").toInt()
        val buildTimeRemaining = JsonUtils.extractValue(json, "buildTimeRemaining").toInt()
        val placedOnTurn = JsonUtils.extractValue(json, "placedOnTurn").toInt()
        // Backward compatibility: default to 0 if field doesn't exist in old saves
        val actionsRemaining = try {
            JsonUtils.extractValue(json, "actionsRemaining").toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
        // Backward compatibility: default to null if field doesn't exist in old saves
        val dragonName = try {
            val value = JsonUtils.extractValue(json, "dragonName")
            if (value == "null") null else value
        } catch (e: Exception) {
            null
        }
        
        // Backward compatibility: default to null if field doesn't exist in old saves
        val raftId = try {
            val value = JsonUtils.extractValue(json, "raftId")
            if (value == "null") null else value.toIntOrNull()
        } catch (e: Exception) {
            null
        }
        
        // Backward compatibility: default to null if field doesn't exist in old saves
        val towerBaseBarricadeId = try {
            val value = JsonUtils.extractValue(json, "towerBaseBarricadeId")
            if (value == "null") null else value.toIntOrNull()
        } catch (e: Exception) {
            null
        }
        
        return SavedDefender(id, type, position, level, buildTimeRemaining, placedOnTurn, actionsRemaining, dragonName, raftId, towerBaseBarricadeId)
    }
    
    private fun parseSavedRaft(json: String): SavedRaft {
        val id = JsonUtils.extractValue(json, "id").toInt()
        val defenderId = JsonUtils.extractValue(json, "defenderId").toInt()
        val position = parsePosition(json)
        return SavedRaft(id, defenderId, position)
    }
    
    private fun parseSavedBarricade(json: String): SavedBarricade {
        val position = parsePosition(json)
        val healthPoints = JsonUtils.extractValue(json, "healthPoints").toInt()
        val defenderId = JsonUtils.extractValue(json, "defenderId").toInt()
        
        // Backward compatibility: default to 0 if field doesn't exist in old saves
        val id = try {
            JsonUtils.extractValue(json, "id").toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
        
        // Backward compatibility: default to null if field doesn't exist in old saves
        val supportedTowerId = try {
            val value = JsonUtils.extractValue(json, "supportedTowerId")
            if (value == "null") null else value.toIntOrNull()
        } catch (e: Exception) {
            null
        }
        
        return SavedBarricade(position, healthPoints, defenderId, id, supportedTowerId)
    }
    
    private fun parseSavedAttacker(json: String): SavedAttacker {
        val id = JsonUtils.extractValue(json, "id").toInt()
        val type = AttackerType.valueOf(JsonUtils.extractValue(json, "type"))
        val position = parsePosition(json)
        val level = JsonUtils.extractValue(json, "level").toInt()
        val currentHealth = JsonUtils.extractValue(json, "currentHealth").toInt()
        val isDefeated = JsonUtils.extractValue(json, "isDefeated").toBoolean()
        // Backward compatibility: default to null if field doesn't exist in old saves
        val dragonName = try {
            val value = JsonUtils.extractValue(json, "dragonName")
            if (value == "null") null else value
        } catch (e: Exception) {
            null
        }
        // Backward compatibility: default to 0 if field doesn't exist in old saves
        val movementPenalty = try {
            JsonUtils.extractValue(json, "movementPenalty").toInt()
        } catch (e: Exception) {
            0
        }
        
        return SavedAttacker(id, type, position, level, currentHealth, isDefeated, dragonName, movementPenalty)
    }
    
    private fun parseSavedFieldEffect(json: String): SavedFieldEffect {
        val position = parsePosition(json)
        val type = FieldEffectType.valueOf(JsonUtils.extractValue(json, "type"))
        val damage = JsonUtils.extractValue(json, "damage").toInt()
        val turnsRemaining = JsonUtils.extractValue(json, "turnsRemaining").toInt()
        val defenderId = JsonUtils.extractValue(json, "defenderId").toInt()
        val attackerIdStr = JsonUtils.extractValue(json, "attackerId")
        val attackerId = if (attackerIdStr == "null") null else attackerIdStr.toInt()
        
        return SavedFieldEffect(position, type, damage, turnsRemaining, defenderId, attackerId)
    }
    
    private fun parseSavedTrap(json: String): SavedTrap {
        val position = parsePosition(json)
        val damage = JsonUtils.extractValue(json, "damage").toInt()
        // Support both old mineId and new defenderId for backwards compatibility
        val defenderId = try {
            JsonUtils.extractValue(json, "defenderId").toInt()
        } catch (e: Exception) {
            JsonUtils.extractValue(json, "mineId").toInt()  // Fallback to old field name
        }
        val type = try {
            JsonUtils.extractValue(json, "type")
        } catch (e: Exception) {
            "DWARVEN"  // Default to dwarven trap for old saves
        }
        return SavedTrap(position, damage, defenderId, type)
    }
    
    private fun parsePosition(json: String): Position {
        val posSection = json.substringAfter("\"position\": {").substringBefore("}")
        val x = JsonUtils.extractValue(posSection, "x").toInt()
        val y = JsonUtils.extractValue(posSection, "y").toInt()
        return Position(x, y)
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
    
    // Player Profile Serialization
    
    fun serializePlayerProfiles(profiles: PlayerProfiles): String {
        val profilesJson = profiles.profiles.joinToString(",\n    ") { profile ->
            val achievementsJson = profile.achievements.joinToString(", ") { achievement ->
                """{"id": "${achievement.id.name}", "earnedAt": ${achievement.earnedAt}}"""
            }
            val statsJson = serializePlayerStats(profile.stats)
            """{
      "id": "${profile.id}",
      "name": "${profile.name}",
      "createdAt": ${profile.createdAt},
      "lastPlayedAt": ${profile.lastPlayedAt},
      "achievements": [$achievementsJson],
      "stats": $statsJson
    }"""
        }
        
        val lastUsedPlayerIdJson = profiles.lastUsedPlayerId?.let { "\"$it\"" } ?: "null"
        
        return """{
  "profiles": [
    $profilesJson
  ],
  "lastUsedPlayerId": $lastUsedPlayerIdJson
}"""
    }
    
    private fun serializePlayerStats(stats: PlayerStats): String {
        val unlockedSpellsJson = stats.unlockedSpells.joinToString(", ") { "\"${it.name}\"" }
        return """{
      "totalXP": ${stats.totalXP},
      "healthStat": ${stats.healthStat},
      "treasuryStat": ${stats.treasuryStat},
      "incomeStat": ${stats.incomeStat},
      "constructionStat": ${stats.constructionStat},
      "manaStat": ${stats.manaStat},
      "unlockedSpells": [$unlockedSpellsJson]
    }"""
    }
    
    fun deserializePlayerProfiles(json: String): PlayerProfiles? {
        try {
            val profiles = mutableListOf<PlayerProfile>()

            // Parse profiles
            val profilesSection = JsonUtils.extractJsonArraySection(json, "\"profiles\": [")

            if (profilesSection.isNotBlank()) {
                val profileEntries = JsonUtils.splitJsonArray(profilesSection)
                for (entry in profileEntries) {
                    val id = JsonUtils.extractValue(entry, "id")
                    val name = JsonUtils.extractValue(entry, "name")
                    val createdAt = JsonUtils.extractValue(entry, "createdAt").toLong()
                    val lastPlayedAt = JsonUtils.extractValue(entry, "lastPlayedAt").toLong()
                    
                    // Parse achievements (if present, for backward compatibility)
                    val achievements = mutableListOf<Achievement>()
                    try {
                        val achievementsSection = entry.substringAfter("\"achievements\": [").substringBefore("]")
                        if (achievementsSection.isNotBlank()) {
                            val achievementEntries = JsonUtils.splitJsonArray(achievementsSection)
                            for (achEntry in achievementEntries) {
                                val achievementId = JsonUtils.extractValue(achEntry, "id")
                                val earnedAt = JsonUtils.extractValue(achEntry, "earnedAt").toLong()
                                achievements.add(Achievement(
                                    id = AchievementId.valueOf(achievementId),
                                    earnedAt = earnedAt
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore achievement parsing errors for backward compatibility
                    }

                    // if (LogConfig.ENABLE_XP_LOGGING) {
                    //     println("=== XP DEBUG: Deserializing player entry JSON: $entry")
                    // }

                    // Parse stats (if present, for backward compatibility)
                    val stats = try {
                        // Extract the stats object - find the opening brace and match it
                        val statsStart = entry.indexOf("\"stats\": {")
                        if (statsStart >= 0) {
                            val jsonAfterStats = entry.substring(statsStart + "\"stats\": ".length)
                            // Find matching closing brace
                            var depth = 0
                            var endIndex = -1
                            for (i in jsonAfterStats.indices) {
                                when (jsonAfterStats[i]) {
                                    '{' -> depth++
                                    '}' -> {
                                        depth--
                                        if (depth == 0) {
                                            endIndex = i + 1
                                            break
                                        }
                                    }
                                }
                            }
                            val statsJson = if (endIndex > 0) jsonAfterStats.substring(0, endIndex) else jsonAfterStats
                            if (LogConfig.ENABLE_XP_LOGGING) {
                                println("=== XP DEBUG: Deserializing stats JSON: $statsJson")
                            }
                            val result = deserializePlayerStats(statsJson)
                            if (LogConfig.ENABLE_XP_LOGGING) {
                                println("=== XP DEBUG: Deserialized stats: totalXP=${result.totalXP}, healthStat=${result.healthStat}, level=${result.level}")
                            }
                            result
                        } else {
                            PlayerStats()
                        }
                    } catch (e: Exception) {
                        if (LogConfig.ENABLE_XP_LOGGING) {
                            println("=== XP DEBUG ERROR: Failed to parse stats: ${e.message}")
                            e.printStackTrace()
                            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                            println("=== XP DEBUG: Returning default PlayerStats() with totalXP=0")
                            }
                        }
                        PlayerStats() // Default stats for backward compatibility
                    }
                    
                    profiles.add(PlayerProfile(
                        id = id,
                        name = name,
                        createdAt = createdAt,
                        lastPlayedAt = lastPlayedAt,
                        achievements = achievements,
                        stats = stats
                    ))
                }
            }
            
            // Parse lastUsedPlayerId
            val lastUsedPlayerId = try {
                val value = JsonUtils.extractValue(json, "lastUsedPlayerId")
                if (value == "null") null else value
            } catch (e: Exception) {
                null
            }
            
            return PlayerProfiles(profiles, lastUsedPlayerId)
        } catch (e: Exception) {
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
            println("Error deserializing player profiles: ${e.message}")
            }
            return null
        }
    }
    
    private fun deserializePlayerStats(json: String): PlayerStats {
        try {
            if (LogConfig.ENABLE_XP_LOGGING) {
                println("=== XP DEBUG: deserializePlayerStats called with: $json")
            }
            val totalXP = JsonUtils.extractValue(json, "totalXP").toInt()
            if (LogConfig.ENABLE_XP_LOGGING) {
                println("=== XP DEBUG: Extracted totalXP = $totalXP")
            }
            val healthStat = JsonUtils.extractValue(json, "healthStat").toInt()
            val treasuryStat = JsonUtils.extractValue(json, "treasuryStat").toInt()
            val incomeStat = JsonUtils.extractValue(json, "incomeStat").toInt()
            val constructionStat = JsonUtils.extractValue(json, "constructionStat").toInt()
            val manaStat = JsonUtils.extractValue(json, "manaStat").toInt()
            
            // Parse unlocked spells
            val unlockedSpells = mutableListOf<SpellType>()
            try {
                val spellsSection = json.substringAfter("\"unlockedSpells\": [").substringBefore("]")
                if (spellsSection.isNotBlank()) {
                    val spellNames = spellsSection.split(",").map { it.trim().removeSurrounding("\"") }
                    for (spellName in spellNames) {
                        if (spellName.isNotBlank()) {
                            unlockedSpells.add(SpellType.valueOf(spellName))
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore spell parsing errors
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("DEBUG: Error parsing spells: ${e.message}")
                }
            }
            
            val result = PlayerStats(
                totalXP = totalXP,
                healthStat = healthStat,
                treasuryStat = treasuryStat,
                incomeStat = incomeStat,
                constructionStat = constructionStat,
                manaStat = manaStat,
                unlockedSpells = unlockedSpells.toSet()
            )
            if (LogConfig.ENABLE_XP_LOGGING) {
                println("=== XP DEBUG: Created PlayerStats with totalXP = ${result.totalXP}, level = ${result.level}")
            }
            return result
        } catch (e: Exception) {
            if (LogConfig.ENABLE_XP_LOGGING) {
                println("=== XP DEBUG ERROR: Failed to deserialize PlayerStats: ${e.message}")
                e.printStackTrace()
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("=== XP DEBUG: Returning default PlayerStats() with totalXP=0")
                }
            }
            return PlayerStats()
        }
    }
}
