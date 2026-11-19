package de.egril.defender.editor

import de.egril.defender.config.LogConfig
import de.egril.defender.editor.EditorEventJsonCodec.parseEvents
import de.egril.defender.editor.EditorJsonScanner.extractDataSection
import de.egril.defender.editor.EditorJsonScanner.extractJsonArray
import de.egril.defender.editor.EditorJsonScanner.splitJsonArrayObjects
import de.egril.defender.editor.EditorSpawnGroupJsonCodec.parseSpawnGroups
import de.egril.defender.editor.EditorSupportsJsonCodec.parseSupports
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DEFAULT_AUTO_TOWER_BASE_EXTRA_COST
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import de.egril.defender.utils.JsonUtils

/**
 * JSON deserializer for editor level files. Counterpart of [EditorLevelJsonCodec].
 */
internal object EditorLevelJsonDeserializer {
    fun deserializeLevel(json: String): EditorLevel? {
        val dataJson = extractDataSection(json)
        try {
            val id = JsonUtils.extractValue(dataJson, "id")
            val mapId = JsonUtils.extractValue(dataJson, "mapId")
            val title = JsonUtils.extractValue(dataJson, "title")
            val titleKey =
                try {
                    JsonUtils.extractValue(dataJson, "titleKey").takeIf { it.isNotEmpty() }
                } catch (e: Exception) {
                    null // Optional field - null if not present
                }
            val subtitle = JsonUtils.extractValue(dataJson, "subtitle")
            val subtitleKey =
                try {
                    JsonUtils.extractValue(dataJson, "subtitleKey").takeIf { it.isNotEmpty() }
                } catch (e: Exception) {
                    null // Optional field - null if not present
                }
            val startCoins = JsonUtils.extractValue(dataJson, "startCoins").toInt()
            val startHealthPoints = JsonUtils.extractValue(dataJson, "startHealthPoints").toInt()

            // Parse enemy spawns
            val spawns = mutableListOf<EditorEnemySpawn>()
            val spawnsSection = dataJson.substringAfter("\"enemySpawns\": [").substringBefore("],")
            val spawnEntries = spawnsSection.split("},").map { it.trim() + "}" }

            for (entry in spawnEntries) {
                if (!entry.contains("attackerType")) continue
                val attackerTypeStr = JsonUtils.extractValue(entry, "attackerType")
                val attackerType =
                    try {
                        AttackerType.valueOf(attackerTypeStr)
                    } catch (e: IllegalArgumentException) {
                        println(
                            "Warning: skipping enemy spawn with unknown attackerType '$attackerTypeStr' — this type may have been removed from the game.",
                        )
                        continue
                    }
                val level = JsonUtils.extractValue(entry, "level").toInt()
                val spawnTurn = JsonUtils.extractValue(entry, "spawnTurn").toInt()

                // Parse spawn point (optional for backward compatibility)
                val spawnPoint =
                    if (entry.contains("\"spawnPoint\"")) {
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
                    try {
                        towers.add(DefenderType.valueOf(entry))
                    } catch (e: IllegalArgumentException) {
                        println(
                            "Warning: skipping unknown tower type '$entry' in availableTowers — this type may have been removed from the game.",
                        )
                    }
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
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("Error parsing waypoints (continuing without them): ${e.message}")
                    }
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
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("Error parsing prerequisites (continuing without them): ${e.message}")
                    }
                    // Continue without prerequisites for backward compatibility
                }
            }

            val singleHitTargetOrder =
                try {
                    splitJsonArrayObjects(extractJsonArray(dataJson, "singleHitTargetOrder"))
                        .mapNotNull { entry ->
                            if (!entry.contains("\"x\"") || !entry.contains("\"y\"")) return@mapNotNull null
                            Position(
                                JsonUtils.extractValue(entry, "x").toInt(),
                                JsonUtils.extractValue(entry, "y").toInt(),
                            )
                        }
                } catch (e: Exception) {
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("Error parsing single-hit target order (continuing without it): ${e.message}")
                    }
                    emptyList()
                }

            // Parse requiredPrerequisiteCount (optional)
            val requiredPrerequisiteCount: Int? =
                if (dataJson.contains("\"requiredPrerequisiteCount\"")) {
                    try {
                        JsonUtils.extractValue(dataJson, "requiredPrerequisiteCount").toInt()
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }

            // Parse testingOnly (optional, defaults to false)
            val testingOnly =
                if (dataJson.contains("\"testingOnly\"")) {
                    try {
                        JsonUtils.extractValue(dataJson, "testingOnly").toBoolean()
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }

            // Parse allowAutoAttack (optional, defaults to true)
            val allowAutoAttack =
                if (dataJson.contains("\"allowAutoAttack\"")) {
                    try {
                        JsonUtils.extractValue(dataJson, "allowAutoAttack").toBoolean()
                    } catch (e: Exception) {
                        true
                    }
                } else {
                    true
                }

            // Parse connectedToPreviousLevel (optional, defaults to false)
            val connectedToPreviousLevel =
                if (dataJson.contains("\"connectedToPreviousLevel\"")) {
                    try {
                        JsonUtils.extractValue(dataJson, "connectedToPreviousLevel").toBoolean()
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }

            // Parse autoTowerBase options (optional, default off with extra cost 20)
            val autoTowerBase =
                if (dataJson.contains("\"autoTowerBase\"")) {
                    try {
                        JsonUtils.extractValue(dataJson, "autoTowerBase").toBoolean()
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }
            val autoTowerBaseExtraCost =
                if (dataJson.contains("\"autoTowerBaseExtraCost\"")) {
                    try {
                        JsonUtils.extractValue(dataJson, "autoTowerBaseExtraCost").toInt()
                    } catch (e: Exception) {
                        DEFAULT_AUTO_TOWER_BASE_EXTRA_COST
                    }
                } else {
                    DEFAULT_AUTO_TOWER_BASE_EXTRA_COST
                }

            // Parse isSandbox (optional, defaults to false)
            val isSandbox =
                if (dataJson.contains("\"isSandbox\"")) {
                    try {
                        JsonUtils.extractValue(dataJson, "isSandbox").toBoolean()
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }

            // Parse waaghEnabled (optional, defaults to false)
            val waaghEnabled =
                if (dataJson.contains("\"waaghEnabled\"")) {
                    try {
                        JsonUtils.extractValue(dataJson, "waaghEnabled").toBoolean()
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }

            // Parse isOfficial (optional, defaults to false)
            val isOfficial =
                try {
                    JsonUtils.extractBooleanValue(dataJson, "isOfficial")
                } catch (e: Exception) {
                    false // Default to false for backward compatibility
                }

            // Parse author (optional, defaults to empty)
            val author =
                try {
                    JsonUtils.extractValue(dataJson, "author")
                } catch (e: Exception) {
                    "" // Optional field
                }

            // Parse communityDescription (optional, defaults to empty)
            val communityDescription =
                try {
                    JsonUtils.extractValue(dataJson, "communityDescription")
                } catch (e: Exception) {
                    "" // Optional field
                }

            // Parse initial data (new nested format preferred, with backward compatibility for old flat format)
            val initialData = parseInitialData(dataJson, id)

            // Parse optional player-usable supports (objects + spell tokens)
            val supports = parseSupports(dataJson)

            // Parse optional scripted level events
            val events = parseEvents(dataJson)

            // Parse optional spawn groups (issue #694)
            val spawnGroups = parseSpawnGroups(dataJson)

            return EditorLevel(
                id = id,
                mapId = mapId,
                title = title,
                titleKey = titleKey,
                subtitle = subtitle,
                subtitleKey = subtitleKey,
                startCoins = startCoins,
                startHealthPoints = startHealthPoints,
                enemySpawns = spawns,
                availableTowers = towers,
                waypoints = waypoints,
                singleHitTargetOrder = singleHitTargetOrder,
                prerequisites = prerequisites,
                requiredPrerequisiteCount = requiredPrerequisiteCount,
                testingOnly = testingOnly,
                allowAutoAttack = allowAutoAttack,
                connectedToPreviousLevel = connectedToPreviousLevel,
                autoTowerBase = autoTowerBase,
                autoTowerBaseExtraCost = autoTowerBaseExtraCost,
                isSandbox = isSandbox,
                waaghEnabled = waaghEnabled,
                isOfficial = isOfficial,
                author = author,
                communityDescription = communityDescription,
                supports = supports,
                events = events,
                initialData = initialData,
                spawnGroups = spawnGroups,
            )
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("Error deserializing level: ${e.message}")
            }
            return null
        }
    }
}
