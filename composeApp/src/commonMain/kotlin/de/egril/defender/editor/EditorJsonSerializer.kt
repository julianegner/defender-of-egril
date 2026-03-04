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
    
    private const val PROGRAM_NAME = "Defender of Egril"

    /**
     * Extracts the "data" section from a file with metadata wrapper.
     * If the JSON has no metadata wrapper (old format), returns the JSON as-is for backward compatibility.
     */
    internal fun extractDataSection(json: String): String = de.egril.defender.utils.JsonUtils.extractDataSection(json)

    fun serializeMap(map: EditorMap): String {
        val tilesJson = map.tiles.entries.joinToString(",\n    ") { (pos, type) ->
            "\"$pos\": \"${type.name}\""
        }
        
        val nameKeyJson = if (map.nameKey != null) {
            ",\n  \"nameKey\": \"${map.nameKey}\""
        } else ""
        
        val worldMapPositionJson = if (map.worldMapPosition != null) {
            ",\n  \"worldMapPosition\": {\"x\": ${map.worldMapPosition.x}, \"y\": ${map.worldMapPosition.y}}"
        } else ""
        
        val riverTilesJson = if (map.riverTiles.isNotEmpty()) {
            val riverData = map.riverTiles.entries.joinToString(",\n    ") { (pos, river) ->
                "\"$pos\": {\"flowDirection\": \"${river.flowDirection.name}\", \"flowSpeed\": ${river.flowSpeed}}"
            }
            ",\n  \"riverTiles\": {\n    $riverData\n  }"
        } else ""
        
        val authorJson = if (map.author.isNotEmpty()) {
            ",\n  \"author\": \"${map.author}\""
        } else ""
        
        val targetInfoJson = if (map.targetInfoMap.isNotEmpty()) {
            val targetData = map.targetInfoMap.entries.joinToString(",\n    ") { (pos, info) ->
                "\"$pos\": {\"name\": \"${info.name}\", \"type\": \"${info.type.name}\"}"
            }
            ",\n  \"targetInfo\": {\n    $targetData\n  }"
        } else ""
        
        val data = """{
  "id": "${map.id}",
  "name": "${map.name}"$nameKeyJson,
  "width": ${map.width},
  "height": ${map.height},
  "readyToUse": ${map.readyToUse},
  "isOfficial": ${map.isOfficial}$worldMapPositionJson$authorJson,
  "tiles": {
    $tilesJson
  }$riverTilesJson$targetInfoJson
}"""
        return """{
  "metadata": {
    "program": "$PROGRAM_NAME",
    "type": "map"
  },
  "data": $data
}"""
    }
    
    fun deserializeMap(json: String): EditorMap? {
        val dataJson = extractDataSection(json)
        try {
            val id = JsonUtils.extractValue(dataJson, "id")
            val name = JsonUtils.extractValue(dataJson, "name")
            val nameKey = try {
                JsonUtils.extractValue(dataJson, "nameKey").takeIf { it.isNotEmpty() }
            } catch (e: Exception) {
                null  // Optional field - null if not present
            }
            val width = JsonUtils.extractValue(dataJson, "width").toInt()
            val height = JsonUtils.extractValue(dataJson, "height").toInt()
            val readyToUse = try {
                JsonUtils.extractBooleanValue(dataJson, "readyToUse")
            } catch (e: Exception) {
                false  // Default to false for backward compatibility
            }
            val isOfficial = try {
                JsonUtils.extractBooleanValue(dataJson, "isOfficial")
            } catch (e: Exception) {
                false  // Default to false for backward compatibility
            }
            val author = try {
                JsonUtils.extractValue(dataJson, "author")
            } catch (e: Exception) {
                ""  // Optional field
            }
            
            // Parse optional world map position
            val worldMapPosition = try {
                val positionSection = dataJson.substringAfter("\"worldMapPosition\":", "")
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
            val tilesSection = dataJson.substringAfter("\"tiles\": {")
                .substringBefore("}")
                .replace("\",","\";")
            val tileEntries = tilesSection.split(";").map { it.trim() }

            for (entry in tileEntries) {
                if (entry.isBlank()) continue
                val parts = entry.split(":")
                if (parts.size != 2) continue

                val pos = parts[0].trim().removeSurrounding("\"")
                val typeStr = parts[1].trim().removeSurrounding("\"")
                // Backward compatibility: ISLAND tiles are treated as BUILD_AREA
                tiles[pos] = if (typeStr == "ISLAND") TileType.BUILD_AREA else TileType.valueOf(typeStr)
            }
            
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
                                    val flowDirection = de.egril.defender.model.RiverFlow.valueOf(flowDirectionMatch.groupValues[1])
                                    val flowSpeed = flowSpeedMatch.groupValues[1].toInt()
                                    
                                    val parts = pos.split(",")
                                    val position = Position(parts[0].toInt(), parts[1].toInt())
                                    riverTiles[pos] = de.egril.defender.model.RiverTile(position, flowDirection, flowSpeed)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error deserializing river tiles: ${e.message}")
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
                                val tName = try { de.egril.defender.utils.JsonUtils.extractValue(body, "name") } catch (e: Exception) { "" }
                                val tType = try {
                                    de.egril.defender.model.TargetType.valueOf(
                                        de.egril.defender.utils.JsonUtils.extractValue(body, "type")
                                    )
                                } catch (e: Exception) {
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
            
            return EditorMap(id, name, nameKey, width, height, tiles, readyToUse, worldMapPosition, riverTiles, isOfficial, author, targetInfoMap)
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
        
        val titleKeyJson = if (level.titleKey != null) {
            ",\n  \"titleKey\": \"${level.titleKey}\""
        } else ""
        
        val subtitleKeyJson = if (level.subtitleKey != null) {
            ",\n  \"subtitleKey\": \"${level.subtitleKey}\""
        } else ""
        
        val requiredCountJson = if (level.requiredPrerequisiteCount != null) {
            ",\n  \"requiredPrerequisiteCount\": ${level.requiredPrerequisiteCount}"
        } else {
            ""
        }
        
        val testingOnlyJson = if (level.testingOnly) {
            ",\n  \"testingOnly\": true"
        } else {
            ""
        }
        
        val allowAutoAttackJson = if (level.allowAutoAttack) {
            ",\n  \"allowAutoAttack\": true"
        } else {
            ""
        }
        
        val isOfficialJson = if (level.isOfficial) {
            ",\n  \"isOfficial\": true"
        } else {
            ""
        }
        
        val authorJson = if (level.author.isNotEmpty()) {
            ",\n  \"author\": \"${level.author}\""
        } else {
            ""
        }
        
        // Serialize initial data in new nested format (optional)
        val initialData = level.getEffectiveInitialData()
        val initialDataJson = if (initialData.defenders.isNotEmpty() || 
                                   initialData.attackers.isNotEmpty() || 
                                   initialData.traps.isNotEmpty() || 
                                   initialData.barricades.isNotEmpty()) {
            val parts = mutableListOf<String>()
            
            // Defenders
            if (initialData.defenders.isNotEmpty()) {
                val defendersData = initialData.defenders.joinToString(",\n      ") { defender ->
                    val dragonNameJson = if (defender.dragonName != null) {
                        """, "dragonName": "${defender.dragonName}""""
                    } else ""
                    val onTowerBaseJson = if (defender.onTowerBase) {
                        """, "onTowerBase": true"""
                    } else ""
                    """{"type": "${defender.type.name}", "position": {"x": ${defender.position.x}, "y": ${defender.position.y}}, "level": ${defender.level}$dragonNameJson$onTowerBaseJson}"""
                }
                parts.add(""""defenders": [
      $defendersData
    ]""")
            }
            
            // Attackers
            if (initialData.attackers.isNotEmpty()) {
                val attackersData = initialData.attackers.joinToString(",\n      ") { attacker ->
                    val healthJson = if (attacker.currentHealth != null) {
                        """, "currentHealth": ${attacker.currentHealth}"""
                    } else ""
                    val dragonNameJson = if (attacker.dragonName != null) {
                        """, "dragonName": "${attacker.dragonName}""""
                    } else ""
                    """{"type": "${attacker.type.name}", "position": {"x": ${attacker.position.x}, "y": ${attacker.position.y}}, "level": ${attacker.level}$healthJson$dragonNameJson}"""
                }
                parts.add(""""attackers": [
      $attackersData
    ]""")
            }
            
            // Traps
            if (initialData.traps.isNotEmpty()) {
                val trapsData = initialData.traps.joinToString(",\n      ") { trap ->
                    """{"position": {"x": ${trap.position.x}, "y": ${trap.position.y}}, "damage": ${trap.damage}, "type": "${trap.type}"}"""
                }
                parts.add(""""traps": [
      $trapsData
    ]""")
            }
            
            // Barricades
            if (initialData.barricades.isNotEmpty()) {
                val barricadesData = initialData.barricades.joinToString(",\n      ") { barricade ->
                    val nameJson = if (!barricade.name.isNullOrBlank()) {
                        """, "name": "${barricade.name}""""
                    } else ""
                    val isGateJson = if (barricade.isGate) {
                        """, "isGate": true"""
                    } else ""
                    """{"position": {"x": ${barricade.position.x}, "y": ${barricade.position.y}}, "healthPoints": ${barricade.healthPoints}$nameJson$isGateJson}"""
                }
                parts.add(""""barricades": [
      $barricadesData
    ]""")
            }
            
            val allParts = parts.joinToString(",\n    ")
            ",\n  \"initialData\": {\n    $allParts\n  }"
        } else ""
        
        val data = """{
  "id": "${level.id}",
  "mapId": "${level.mapId}",
  "title": "${level.title}"$titleKeyJson,
  "subtitle": "${level.subtitle}"$subtitleKeyJson,
  "startCoins": ${level.startCoins},
  "startHealthPoints": ${level.startHealthPoints},
  "enemySpawns": [
    $spawnsJson
  ],
  "availableTowers": [$towersJson],
  "waypoints": [
    $waypointsJson
  ],
  "prerequisites": [$prerequisitesJson]$requiredCountJson$testingOnlyJson$allowAutoAttackJson$isOfficialJson$authorJson$initialDataJson
}"""
        return """{
  "metadata": {
    "program": "$PROGRAM_NAME",
    "type": "level"
  },
  "data": $data
}"""
    }
    
    fun deserializeLevel(json: String): EditorLevel? {
        val dataJson = extractDataSection(json)
        try {
            val id = JsonUtils.extractValue(dataJson, "id")
            val mapId = JsonUtils.extractValue(dataJson, "mapId")
            val title = JsonUtils.extractValue(dataJson, "title")
            val titleKey = try {
                JsonUtils.extractValue(dataJson, "titleKey").takeIf { it.isNotEmpty() }
            } catch (e: Exception) {
                null  // Optional field - null if not present
            }
            val subtitle = JsonUtils.extractValue(dataJson, "subtitle")
            val subtitleKey = try {
                JsonUtils.extractValue(dataJson, "subtitleKey").takeIf { it.isNotEmpty() }
            } catch (e: Exception) {
                null  // Optional field - null if not present
            }
            val startCoins = JsonUtils.extractValue(dataJson, "startCoins").toInt()
            val startHealthPoints = JsonUtils.extractValue(dataJson, "startHealthPoints").toInt()
            
            // Parse enemy spawns
            val spawns = mutableListOf<EditorEnemySpawn>()
            val spawnsSection = dataJson.substringAfter("\"enemySpawns\": [").substringBefore("],")
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
            val towersSection = dataJson.substringAfter("\"availableTowers\": [").substringBefore("]")
            val towerEntries = towersSection.split(",").map { it.trim().removeSurrounding("\"") }
            
            for (entry in towerEntries) {
                if (entry.isNotBlank()) {
                    towers.add(DefenderType.valueOf(entry))
                }
            }
            
            // Parse waypoints (optional for backward compatibility)
            val waypoints = mutableListOf<EditorWaypoint>()
            if (dataJson.contains("\"waypoints\"")) {
                try {
                    // Find the waypoints array section
                    val waypointsStart = dataJson.indexOf("\"waypoints\": [")
                    if (waypointsStart >= 0) {
                        val arrayStart = dataJson.indexOf("[", waypointsStart)
                        var arrayEnd = arrayStart + 1
                        var depth = 1
                        
                        // Find matching closing bracket
                        while (depth > 0 && arrayEnd < dataJson.length) {
                            when (dataJson[arrayEnd]) {
                                '[' -> depth++
                                ']' -> depth--
                            }
                            arrayEnd++
                        }
                        
                        val waypointsSection = dataJson.substring(arrayStart + 1, arrayEnd - 1).trim()
                        
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
            if (dataJson.contains("\"prerequisites\"")) {
                try {
                    val prerequisitesSection = dataJson.substringAfter("\"prerequisites\": [").substringBefore("]")
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
            val requiredPrerequisiteCount: Int? = if (dataJson.contains("\"requiredPrerequisiteCount\"")) {
                try {
                    JsonUtils.extractValue(dataJson, "requiredPrerequisiteCount").toInt()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            
            // Parse testingOnly (optional, defaults to false)
            val testingOnly = if (dataJson.contains("\"testingOnly\"")) {
                try {
                    JsonUtils.extractValue(dataJson, "testingOnly").toBoolean()
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
            
            // Parse allowAutoAttack (optional, defaults to false)
            val allowAutoAttack = if (dataJson.contains("\"allowAutoAttack\"")) {
                try {
                    JsonUtils.extractValue(dataJson, "allowAutoAttack").toBoolean()
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
            
            // Parse isOfficial (optional, defaults to false)
            val isOfficial = try {
                JsonUtils.extractBooleanValue(dataJson, "isOfficial")
            } catch (e: Exception) {
                false  // Default to false for backward compatibility
            }
            
            // Parse author (optional, defaults to empty)
            val author = try {
                JsonUtils.extractValue(dataJson, "author")
            } catch (e: Exception) {
                ""  // Optional field
            }
            
            // Parse initial data (new nested format preferred, with backward compatibility for old flat format)
            var initialDefenders = mutableListOf<InitialDefender>()
            var initialAttackers = mutableListOf<InitialAttacker>()
            var initialTraps = mutableListOf<InitialTrap>()
            var initialBarricades = mutableListOf<InitialBarricade>()


            if (id == "t3") {
                println("")
                println("---------------------------------- DEBUG DESERIALIZE LEVEL ----------------------------------")
                println("")
                println("Comeplete JSON Data")
                println(dataJson)
                println("")
                println("------------------------------------------------------------------------------------------------------")
                println("")


                if (dataJson.contains("\"initialData\"")) {
                    println("EditorJsonSerializer: initialData found in JSON")
                } else {
                    println("EditorJsonSerializer: initialData NOT found in JSON")
                }

                println("---------------------------------- END DEBUG DESERIALIZE LEVEL ----------------------------------")
            }

            // Try new nested format first
            if (dataJson.contains("\"initialData\"")) {
                try {
                    println("EditorJsonSerializer: Found initialData in JSON")
                    // Extract initialData section - find opening brace after "initialData":
                    val afterKey = dataJson.substringAfter("\"initialData\"")
                    
                    // Find the first { after the key (skipping the colon and any whitespace)
                    val openBraceIndex = afterKey.indexOf('{')
                    val initialDataSection = if (openBraceIndex == -1) {
                        println("EditorJsonSerializer: ERROR - No opening brace found after initialData")
                        ""
                    } else {
                        println("EditorJsonSerializer: Found opening brace at index $openBraceIndex")
                        val afterInitialData = afterKey.substring(openBraceIndex + 1)
                        
                        // Find the closing } that matches the opening {
                        var braceCount = 1
                        var endIndex = 0
                        for (i in afterInitialData.indices) {
                            when (afterInitialData[i]) {
                                '{' -> braceCount++
                                '}' -> {
                                    braceCount--
                                    if (braceCount == 0) {
                                        endIndex = i
                                        break
                                    }
                                }
                            }
                        }
                        if (endIndex > 0) {
                            afterInitialData.substring(0, endIndex)
                        } else {
                            println("EditorJsonSerializer: ERROR - Could not find matching closing brace, endIndex=$endIndex")
                            afterInitialData.substringBefore("\n  }")  // Fallback to old method
                        }
                    }
                    println("EditorJsonSerializer: initialDataSection length = ${initialDataSection.length}")
                    // println("EditorJsonSerializer: initialDataSection first 100 chars = ${initialDataSection.take(100)}")

                    println("EditorJsonSerializer: initialDataSection = ${initialDataSection}")
                    
                    // Parse defenders from new format
                    if (initialDataSection.contains("\"defenders\"")) {
                        println("EditorJsonSerializer: Found defenders in initialDataSection")
                        val afterKey = initialDataSection.substringAfter("\"defenders\"")

                        // Find the opening [ bracket (skip colon and whitespace)
                        val openBracketIndex = afterKey.indexOf('[')
                        if (openBracketIndex == -1) {
                            println("EditorJsonSerializer: ERROR - No opening bracket found after defenders")
                        }
                        val afterBracket = afterKey.substring(openBracketIndex + 1)
                        val defendersSection = if (afterBracket.contains("],")) {
                            afterBracket.substringBefore("],")
                        } else {
                            afterBracket.substringBefore("]")
                        }

                        println("EditorJsonSerializer: defendersSection = ${defendersSection}")

                        if (defendersSection.isNotBlank()) {
                            val defenderEntries = splitJsonArrayObjects(defendersSection)
                            for (entry in defenderEntries) {
                                if (!entry.contains("type")) continue
                                val type = DefenderType.valueOf(JsonUtils.extractValue(entry, "type"))
                                val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                                val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                                val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                                val position = Position(x, y)
                                val level = JsonUtils.extractValue(entry, "level").toInt()
                                val dragonName = try {
                                    JsonUtils.extractValue(entry, "dragonName").takeIf { it.isNotEmpty() }
                                } catch (e: Exception) {
                                    null
                                }
                                val onTowerBase = try {
                                    JsonUtils.extractValue(entry, "onTowerBase").toBoolean()
                                } catch (e: Exception) {
                                    false
                                }
                                initialDefenders.add(InitialDefender(type, position, level, dragonName, onTowerBase))
                                println("EditorJsonSerializer: Added InitialDefender(type=$type, position=$position, level=$level, dragonName=$dragonName, onTowerBase=$onTowerBase)")
                            }
                            println("EditorJsonSerializer: Parsed ${initialDefenders.size} initial defenders")
                            println("EditorJsonSerializer: initialDefenders = $initialDefenders")
                        }
                    }
                    
                    // Parse attackers from new format
                    if (initialDataSection.contains("\"attackers\"")) {
                        println("EditorJsonSerializer: Found attackers in initialDataSection")
                        val afterKey = initialDataSection.substringAfter("\"attackers\"")
                        // Find the opening [ bracket (skip colon and whitespace)
                        val openBracketIndex = afterKey.indexOf('[')
                        if (openBracketIndex == -1) {
                            println("EditorJsonSerializer: ERROR - No opening bracket found after attackers")
                        }
                        val afterBracket = afterKey.substring(openBracketIndex + 1)
                        val attackersSection = if (afterBracket.contains("],")) {
                            afterBracket.substringBefore("],")
                        } else {
                            afterBracket.substringBefore("]")
                        }
                        if (attackersSection.isNotBlank()) {
                            val attackerEntries = splitJsonArrayObjects(attackersSection)
                            for (entry in attackerEntries) {
                                if (!entry.contains("type")) continue
                                val type = AttackerType.valueOf(JsonUtils.extractValue(entry, "type"))
                                val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                                val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                                val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                                val position = Position(x, y)
                                val level = JsonUtils.extractValue(entry, "level").toInt()
                                val currentHealth = try {
                                    JsonUtils.extractValue(entry, "currentHealth").toIntOrNull()
                                } catch (e: Exception) {
                                    null
                                }
                                val dragonName = try {
                                    JsonUtils.extractValue(entry, "dragonName").takeIf { it.isNotEmpty() }
                                } catch (e: Exception) {
                                    null
                                }
                                initialAttackers.add(InitialAttacker(type, position, level, currentHealth, dragonName))
                            }
                            println("EditorJsonSerializer: Parsed ${initialAttackers.size} initial attackers")
                            println("EditorJsonSerializer: initialAttackers = $initialAttackers")
                        }
                    }
                    
                    // Parse traps from new format
                    if (initialDataSection.contains("\"traps\"")) {
                        println("EditorJsonSerializer: Found traps in initialDataSection")
                        val afterKey = initialDataSection.substringAfter("\"traps\"")
                        // Find the opening [ bracket (skip colon and whitespace)
                        val openBracketIndex = afterKey.indexOf('[')
                        if (openBracketIndex == -1) {
                            println("EditorJsonSerializer: ERROR - No opening bracket found after traps")
                        }
                        val afterBracket = afterKey.substring(openBracketIndex + 1)
                        val trapsSection = if (afterBracket.contains("],")) {
                            afterBracket.substringBefore("],")
                        } else {
                            afterBracket.substringBefore("]")
                        }
                        if (trapsSection.isNotBlank()) {
                            val trapEntries = splitJsonArrayObjects(trapsSection)
                            for (entry in trapEntries) {
                                if (!entry.contains("position")) continue
                                val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                                val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                                val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                                val position = Position(x, y)
                                val damage = JsonUtils.extractValue(entry, "damage").toInt()
                                val type = try {
                                    JsonUtils.extractValue(entry, "type")
                                } catch (e: Exception) {
                                    "DWARVEN"
                                }
                                initialTraps.add(InitialTrap(position, damage, type))
                            }
                            println("EditorJsonSerializer: Parsed ${initialTraps.size} initial traps")
                            println("EditorJsonSerializer: initialTraps = $initialTraps")
                        }
                    }
                    
                    // Parse barricades from new format
                    if (initialDataSection.contains("\"barricades\"")) {
                        println("EditorJsonSerializer: Found barricades in initialDataSection")
                        val afterKey = initialDataSection.substringAfter("\"barricades\"")
                        // Find the opening [ bracket (skip colon and whitespace)
                        val openBracketIndex = afterKey.indexOf('[')
                        if (openBracketIndex == -1) {
                            println("EditorJsonSerializer: ERROR - No opening bracket found after barricades")
                        }
                        val afterBracket = afterKey.substring(openBracketIndex + 1)
                        val barricadesSection = if (afterBracket.contains("],")) {
                            afterBracket.substringBefore("],")
                        } else {
                            afterBracket.substringBefore("]")
                        }
                        if (barricadesSection.isNotBlank()) {
                            val barricadeEntries = splitJsonArrayObjects(barricadesSection)
                            for (entry in barricadeEntries) {
                                if (!entry.contains("position")) continue
                                val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                                val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                                val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                                val position = Position(x, y)
                                val healthPoints = JsonUtils.extractValue(entry, "healthPoints").toInt()
                                val barricadeName = try {
                                    JsonUtils.extractValue(entry, "name").takeIf { it.isNotBlank() }
                                } catch (e: Exception) { null }
                                val isGate = try {
                                    JsonUtils.extractBooleanValue(entry, "isGate")
                                } catch (e: Exception) { false }
                                initialBarricades.add(InitialBarricade(position, healthPoints, name = barricadeName, isGate = isGate))
                            }
                            println("EditorJsonSerializer: Parsed ${initialBarricades.size} initial barricades")
                            println("EditorJsonSerializer: initialBarricades = $initialBarricades")
                        }
                    }
                } catch (e: Exception) {
                    println("Error parsing initial data (new format): ${e.message}")
                }
            }
            
            // Fall back to old flat format if new format wasn't found or was empty
            if (initialDefenders.isEmpty() && initialAttackers.isEmpty() && 
                initialTraps.isEmpty() && initialBarricades.isEmpty()) {
                
                // Parse initial defenders (legacy flat format)
                if (dataJson.contains("\"initialDefenders\"")) {
                try {
                    // Extract array - handle both `],` and `]` at end
                    val afterKey = dataJson.substringAfter("\"initialDefenders\": [")
                    val defendersSection = if (afterKey.contains("],")) {
                        afterKey.substringBefore("],")
                    } else {
                        afterKey.substringBefore("]")
                    }
                    if (defendersSection.isNotBlank()) {
                        val defenderEntries = defendersSection.split("},").map { it.trim() + "}" }
                        for (entry in defenderEntries) {
                            if (!entry.contains("type")) continue
                            val type = DefenderType.valueOf(JsonUtils.extractValue(entry, "type"))
                            val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                            val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                            val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                            val position = Position(x, y)
                            val level = JsonUtils.extractValue(entry, "level").toInt()
                            val dragonName = try {
                                JsonUtils.extractValue(entry, "dragonName").takeIf { it.isNotEmpty() }
                            } catch (e: Exception) {
                                null
                            }
                            val onTowerBase = try {
                                JsonUtils.extractValue(entry, "onTowerBase").toBoolean()
                            } catch (e: Exception) {
                                false
                            }
                            initialDefenders.add(InitialDefender(type, position, level, dragonName, onTowerBase))
                        }
                    }
                } catch (e: Exception) {
                    println("Error parsing initial defenders (continuing without them): ${e.message}")
                }
            }
            
                // Parse initial attackers (legacy flat format)
                if (dataJson.contains("\"initialAttackers\"")) {
                try {
                    // Extract array - handle both `],` and `]` at end
                    val afterKey = dataJson.substringAfter("\"initialAttackers\": [")
                    val attackersSection = if (afterKey.contains("],")) {
                        afterKey.substringBefore("],")
                    } else {
                        afterKey.substringBefore("]")
                    }
                    if (attackersSection.isNotBlank()) {
                        val attackerEntries = attackersSection.split("},").map { it.trim() + "}" }
                        for (entry in attackerEntries) {
                            if (!entry.contains("type")) continue
                            val type = AttackerType.valueOf(JsonUtils.extractValue(entry, "type"))
                            val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                            val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                            val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                            val position = Position(x, y)
                            val level = JsonUtils.extractValue(entry, "level").toInt()
                            val currentHealth = try {
                                JsonUtils.extractValue(entry, "currentHealth").toIntOrNull()
                            } catch (e: Exception) {
                                null
                            }
                            val dragonName = try {
                                JsonUtils.extractValue(entry, "dragonName").takeIf { it.isNotEmpty() }
                            } catch (e: Exception) {
                                null
                            }
                            initialAttackers.add(InitialAttacker(type, position, level, currentHealth, dragonName))
                        }
                    }
                } catch (e: Exception) {
                    println("Error parsing initial attackers (continuing without them): ${e.message}")
                }
            }
            
                // Parse initial traps (legacy flat format)
                if (dataJson.contains("\"initialTraps\"")) {
                try {
                    // Extract array - handle both `],` and `]` at end
                    val afterKey = dataJson.substringAfter("\"initialTraps\": [")
                    val trapsSection = if (afterKey.contains("],")) {
                        afterKey.substringBefore("],")
                    } else {
                        afterKey.substringBefore("]")
                    }
                    if (trapsSection.isNotBlank()) {
                        val trapEntries = trapsSection.split("},").map { it.trim() + "}" }
                        for (entry in trapEntries) {
                            if (!entry.contains("position")) continue
                            val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                            val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                            val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                            val position = Position(x, y)
                            val damage = JsonUtils.extractValue(entry, "damage").toInt()
                            val type = try {
                                JsonUtils.extractValue(entry, "type")
                            } catch (e: Exception) {
                                "DWARVEN"
                            }
                            initialTraps.add(InitialTrap(position, damage, type))
                        }
                    }
                } catch (e: Exception) {
                    println("Error parsing initial traps (continuing without them): ${e.message}")
                }
            }
            
                // Parse initial barricades (legacy flat format)
                if (dataJson.contains("\"initialBarricades\"")) {
                try {
                    // Extract array - handle both `],` and `]` at end
                    val afterKey = dataJson.substringAfter("\"initialBarricades\": [")
                    val barricadesSection = if (afterKey.contains("],")) {
                        afterKey.substringBefore("],")
                    } else {
                        afterKey.substringBefore("]")
                    }
                    if (barricadesSection.isNotBlank()) {
                        val barricadeEntries = barricadesSection.split("},").map { it.trim() + "}" }
                        for (entry in barricadeEntries) {
                            if (!entry.contains("position")) continue
                            val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                            val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                            val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                            val position = Position(x, y)
                            val healthPoints = JsonUtils.extractValue(entry, "healthPoints").toInt()
                            initialBarricades.add(InitialBarricade(position, healthPoints))
                        }
                    }
                } catch (e: Exception) {
                    println("Error parsing initial barricades (continuing without them): ${e.message}")
                }
            }
            }  // End of legacy format fallback
            
            println("EditorJsonSerializer.deserializeLevel: Parsed level $id with ${initialDefenders.size} defenders, ${initialAttackers.size} attackers, ${initialTraps.size} traps, ${initialBarricades.size} barricades")
            
            // Create InitialData object from parsed data
            val initialData = if (initialDefenders.isNotEmpty() || initialAttackers.isNotEmpty() || 
                                   initialTraps.isNotEmpty() || initialBarricades.isNotEmpty()) {
                InitialData(initialDefenders, initialAttackers, initialTraps, initialBarricades)
            } else {
                null
            }
            
            return EditorLevel(
                id, mapId, title, titleKey, subtitle, subtitleKey, 
                startCoins, startHealthPoints, spawns, towers, waypoints, 
                prerequisites, requiredPrerequisiteCount, testingOnly, 
                allowAutoAttack, isOfficial,
                author = author,
                initialData = initialData
            )
        } catch (e: Exception) {
            println("Error deserializing level: ${e.message}")
            return null
        }
    }
    
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
            println("Error deserializing sequence: ${e.message}")
            return null
        }
    }
    
    // ==================== World Map Data Serialization ====================
    
    fun serializeWorldMapData(data: WorldMapData): String {
        val locationsJson = data.locations.joinToString(",\n    ") { location ->
            val levelIdsJson = location.levelIds.joinToString(", ") { "\"$it\"" }
            val nameKeyJson = if (location.nameKey != null) {
                """,
      "nameKey": "${location.nameKey}""""
            } else ""
            val iconResourceNameJson = if (location.iconResourceName != null) {
                """,
      "iconResourceName": "${location.iconResourceName}""""
            } else ""
            """{
      "id": "${location.id}",
      "name": "${location.name}"$nameKeyJson,
      "position": {"x": ${location.position.x}, "y": ${location.position.y}},
      "levelIds": [$levelIdsJson]$iconResourceNameJson
    }"""
        }
        
        val pathsJson = data.paths.joinToString(",\n    ") { path ->
            val controlPointsJson = if (path.controlPoints.isEmpty()) {
                "[]"
            } else {
                "[" + path.controlPoints.joinToString(", ") { pt -> 
                    """{"x": ${pt.x}, "y": ${pt.y}}"""
                } + "]"
            }
            val segmentTypesJson = if (path.segmentTypes.isEmpty()) {
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
                    val nameKey = try {
                        JsonUtils.extractValue(entry, "nameKey").takeIf { it.isNotEmpty() }
                    } catch (e: Exception) {
                        null  // Optional field - null if not present
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
                    val iconResourceName = try {
                        JsonUtils.extractValue(entry, "iconResourceName").takeIf { it.isNotEmpty() }
                    } catch (e: Exception) {
                        null  // Optional field - null if not present
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
                    val type = try {
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
            println("Error deserializing world map data: ${e.message}")
            return null
        }
    }
    
    /**
     * Extract the content of a JSON array by its key name.
     */
    private fun extractJsonArray(json: String, key: String): String {
        val startIndex = json.indexOf("\"$key\": [")
        if (startIndex == -1) return ""
        
        val arrayStart = json.indexOf("[", startIndex)
        if (arrayStart == -1) return ""
        
        var depth = 1
        var arrayEnd = arrayStart + 1
        while (depth > 0 && arrayEnd < json.length) {
            when (json[arrayEnd]) {
                '[' -> depth++
                ']' -> depth--
            }
            arrayEnd++
        }
        
        return json.substring(arrayStart + 1, arrayEnd - 1).trim()
    }
    
    /**
     * Split a JSON array content into individual object strings.
     */
    private fun splitJsonArrayObjects(arrayContent: String): List<String> {
        val objects = mutableListOf<String>()
        var depth = 0
        var currentObject = ""
        
        for (char in arrayContent) {
            when (char) {
                '{' -> {
                    depth++
                    currentObject += char
                }
                '}' -> {
                    depth--
                    currentObject += char
                    if (depth == 0) {
                        objects.add(currentObject.trim())
                        currentObject = ""
                    }
                }
                else -> {
                    if (depth > 0) {
                        currentObject += char
                    }
                }
            }
        }
        
        return objects
    }
}
