package de.egril.defender.editor

import de.egril.defender.config.LogConfig
import de.egril.defender.editor.EditorJsonScanner.splitJsonArrayObjects
import de.egril.defender.model.AttackerType
import de.egril.defender.model.BridgeType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import de.egril.defender.utils.JsonUtils

/**
 * Parses the `initialData` section of an editor level JSON. Prefers the nested format and falls back to the
 * legacy flat format. Returns null when no initial data was found.
 */
internal fun parseInitialData(dataJson: String, id: String): InitialData? {
    var initialDefenders = mutableListOf<InitialDefender>()
    var initialAttackers = mutableListOf<InitialAttacker>()
    var initialTraps = mutableListOf<InitialTrap>()
    var initialBarricades = mutableListOf<InitialBarricade>()
    var initialBridges = mutableListOf<InitialBridge>()
    var initialFiefs = mutableListOf<InitialFief>()
    var initialMushrooms = mutableListOf<InitialMushroom>()
    var initialGargoyles = mutableListOf<InitialGargoyle>()
    var initialPortals = mutableListOf<InitialPortal>()
    val initialPlacementAreas = mutableListOf<Position>()

    if (LogConfig.ENABLE_INITIAL_DATA_PARSING_LOGGING && id == "t3") {
        println("")
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("---------------------------------- DEBUG DESERIALIZE LEVEL ----------------------------------")
        }
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("")
        }
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Comeplete JSON Data")
            println(dataJson)
            println("")
        }
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("------------------------------------------------------------------------------------------------------")
        }
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("")
        }

        if (dataJson.contains("\"initialData\"")) {
            println("EditorJsonSerializer: initialData found in JSON")
        } else {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("EditorJsonSerializer: initialData NOT found in JSON")
            }
        }

        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("---------------------------------- END DEBUG DESERIALIZE LEVEL ----------------------------------")
        }
    }

    // Try new nested format first
    if (dataJson.contains("\"initialData\"")) {
        try {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("EditorJsonSerializer: Found initialData in JSON")
            }
            // Extract initialData section - find opening brace after "initialData":
            val afterKey = dataJson.substringAfter("\"initialData\"")

            // Find the first { after the key (skipping the colon and any whitespace)
            val openBraceIndex = afterKey.indexOf('{')
            val initialDataSection =
                if (openBraceIndex == -1) {
                    if (LogConfig.ENABLE_INITIAL_DATA_PARSING_LOGGING) {
                        println("EditorJsonSerializer: ERROR - No opening brace found after initialData")
                    }
                    ""
                } else {
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("EditorJsonSerializer: Found opening brace at index $openBraceIndex")
                    }
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
                        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                            println("EditorJsonSerializer: ERROR - Could not find matching closing brace, endIndex=$endIndex")
                        }
                        afterInitialData.substringBefore("\n  }") // Fallback to old method
                    }
                }
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("EditorJsonSerializer: initialDataSection length = ${initialDataSection.length}")
            }
            // println("EditorJsonSerializer: initialDataSection first 100 chars = ${initialDataSection.take(100)}")

            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("EditorJsonSerializer: initialDataSection = $initialDataSection")
            }

            // Parse defenders from new format
            if (initialDataSection.contains("\"defenders\"")) {
                if (LogConfig.ENABLE_INITIAL_DATA_PARSING_LOGGING) {
                    println("EditorJsonSerializer: Found defenders in initialDataSection")
                }
                val afterKey = initialDataSection.substringAfter("\"defenders\"")

                // Find the opening [ bracket (skip colon and whitespace)
                val openBracketIndex = afterKey.indexOf('[')
                if (openBracketIndex == -1) {
                    if (LogConfig.ENABLE_INITIAL_DATA_PARSING_LOGGING) {
                        println("EditorJsonSerializer: ERROR - No opening bracket found after defenders")
                    }
                }
                val afterBracket = afterKey.substring(openBracketIndex + 1)
                val defendersSection =
                    if (afterBracket.contains("],")) {
                        afterBracket.substringBefore("],")
                    } else {
                        afterBracket.substringBefore("]")
                    }

                if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                    println("EditorJsonSerializer: defendersSection = $defendersSection")
                }

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
                        val dragonName =
                            try {
                                JsonUtils.extractValue(entry, "dragonName").takeIf { it.isNotEmpty() }
                            } catch (e: Exception) {
                                null
                            }
                        val onTowerBase =
                            try {
                                JsonUtils.extractValue(entry, "onTowerBase").toBoolean()
                            } catch (e: Exception) {
                                false
                            }
                        initialDefenders.add(InitialDefender(type, position, level, dragonName, onTowerBase))
                        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                            println(
                                "EditorJsonSerializer: Added InitialDefender(type=$type, position=$position, level=$level, dragonName=$dragonName, onTowerBase=$onTowerBase)",
                            )
                        }
                    }
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("EditorJsonSerializer: Parsed ${initialDefenders.size} initial defenders")
                    }
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("EditorJsonSerializer: initialDefenders = $initialDefenders")
                    }
                }
            }

            // Parse attackers from new format
            if (initialDataSection.contains("\"attackers\"")) {
                if (LogConfig.ENABLE_INITIAL_DATA_PARSING_LOGGING) {
                    println("EditorJsonSerializer: Found attackers in initialDataSection")
                }
                val afterKey = initialDataSection.substringAfter("\"attackers\"")
                // Find the opening [ bracket (skip colon and whitespace)
                val openBracketIndex = afterKey.indexOf('[')
                if (openBracketIndex == -1) {
                    if (LogConfig.ENABLE_INITIAL_DATA_PARSING_LOGGING) {
                        println("EditorJsonSerializer: ERROR - No opening bracket found after attackers")
                    }
                }
                val afterBracket = afterKey.substring(openBracketIndex + 1)
                val attackersSection =
                    if (afterBracket.contains("],")) {
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
                        val currentHealth =
                            try {
                                JsonUtils.extractValue(entry, "currentHealth").toIntOrNull()
                            } catch (e: Exception) {
                                null
                            }
                        val dragonName =
                            try {
                                JsonUtils.extractValue(entry, "dragonName").takeIf { it.isNotEmpty() }
                            } catch (e: Exception) {
                                null
                            }
                        initialAttackers.add(InitialAttacker(type, position, level, currentHealth, dragonName))
                    }
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("EditorJsonSerializer: Parsed ${initialAttackers.size} initial attackers")
                    }
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("EditorJsonSerializer: initialAttackers = $initialAttackers")
                    }
                }
            }

            // Parse traps from new format
            if (initialDataSection.contains("\"traps\"")) {
                if (LogConfig.ENABLE_INITIAL_DATA_PARSING_LOGGING) {
                    println("EditorJsonSerializer: Found traps in initialDataSection")
                }
                val afterKey = initialDataSection.substringAfter("\"traps\"")
                // Find the opening [ bracket (skip colon and whitespace)
                val openBracketIndex = afterKey.indexOf('[')
                if (openBracketIndex == -1) {
                    if (LogConfig.ENABLE_INITIAL_DATA_PARSING_LOGGING) {
                        println("EditorJsonSerializer: ERROR - No opening bracket found after traps")
                    }
                }
                val afterBracket = afterKey.substring(openBracketIndex + 1)
                val trapsSection =
                    if (afterBracket.contains("],")) {
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
                        val type =
                            try {
                                JsonUtils.extractValue(entry, "type")
                            } catch (e: Exception) {
                                "DWARVEN"
                            }
                        initialTraps.add(InitialTrap(position, damage, type))
                    }
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("EditorJsonSerializer: Parsed ${initialTraps.size} initial traps")
                    }
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("EditorJsonSerializer: initialTraps = $initialTraps")
                    }
                }
            }

            // Parse barricades from new format
            if (initialDataSection.contains("\"barricades\"")) {
                if (LogConfig.ENABLE_INITIAL_DATA_PARSING_LOGGING) {
                    println("EditorJsonSerializer: Found barricades in initialDataSection")
                }
                val afterKey = initialDataSection.substringAfter("\"barricades\"")
                // Find the opening [ bracket (skip colon and whitespace)
                val openBracketIndex = afterKey.indexOf('[')
                if (openBracketIndex == -1) {
                    if (LogConfig.ENABLE_INITIAL_DATA_PARSING_LOGGING) {
                        println("EditorJsonSerializer: ERROR - No opening bracket found after barricades")
                    }
                }
                val afterBracket = afterKey.substring(openBracketIndex + 1)
                val barricadesSection =
                    if (afterBracket.contains("],")) {
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
                        val barricadeName =
                            try {
                                JsonUtils.extractValue(entry, "name").takeIf { it.isNotBlank() }
                            } catch (e: Exception) {
                                null
                            }
                        val supportsTower =
                            try {
                                JsonUtils.extractBooleanValue(entry, "supportsTower")
                            } catch (e: Exception) {
                                false
                            }
                        val isGate =
                            try {
                                JsonUtils.extractBooleanValue(entry, "isGate")
                            } catch (e: Exception) {
                                false
                            }
                        initialBarricades.add(
                            InitialBarricade(
                                position,
                                healthPoints,
                                supportsTower = supportsTower,
                                name = barricadeName,
                                isGate = isGate,
                            ),
                        )
                    }
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("EditorJsonSerializer: Parsed ${initialBarricades.size} initial barricades")
                    }
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("EditorJsonSerializer: initialBarricades = $initialBarricades")
                    }
                }
            }

            // Parse bridges from new format
            if (initialDataSection.contains("\"bridges\"")) {
                val afterKey = initialDataSection.substringAfter("\"bridges\"")
                val openBracketIndex = afterKey.indexOf('[')
                if (openBracketIndex != -1) {
                    val afterBracket = afterKey.substring(openBracketIndex + 1)
                    val bridgesSection =
                        if (afterBracket.contains("],")) {
                            afterBracket.substringBefore("],")
                        } else {
                            afterBracket.substringBefore("]")
                        }
                    if (bridgesSection.isNotBlank()) {
                        val bridgeEntries = splitJsonArrayObjects(bridgesSection)
                        for (entry in bridgeEntries) {
                            if (!entry.contains("position")) continue
                            val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                            val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                            val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                            val position = Position(x, y)
                            val type =
                                try {
                                    BridgeType.valueOf(JsonUtils.extractValue(entry, "type"))
                                } catch (e: Exception) {
                                    BridgeType.WOODEN
                                }
                            val healthPoints =
                                JsonUtils
                                    .extractValue(entry, "healthPoints")
                                    .toIntOrNull()
                                    ?: if (type == BridgeType.STONE) InitialBridge.DEFAULT_STONE_HEALTH else InitialBridge.DEFAULT_WOODEN_HEALTH
                            val isIndestructible =
                                try {
                                    JsonUtils.extractBooleanValue(entry, "isIndestructible")
                                } catch (e: Exception) {
                                    false
                                }
                            initialBridges.add(
                                InitialBridge(
                                    position = position,
                                    type = type,
                                    healthPoints = healthPoints,
                                    isIndestructible = isIndestructible,
                                ),
                            )
                        }
                    }
                }
            }

            // Parse fiefs from new format
            if (initialDataSection.contains("\"fiefs\"")) {
                val afterKey = initialDataSection.substringAfter("\"fiefs\"")
                val openBracketIndex = afterKey.indexOf('[')
                if (openBracketIndex != -1) {
                    val afterBracket = afterKey.substring(openBracketIndex + 1)
                    val fiefsSection =
                        if (afterBracket.contains("],")) {
                            afterBracket.substringBefore("],")
                        } else {
                            afterBracket.substringBefore("]")
                        }
                    if (fiefsSection.isNotBlank()) {
                        val fiefEntries = splitJsonArrayObjects(fiefsSection)
                        for (entry in fiefEntries) {
                            if (!entry.contains("position")) continue
                            val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                            val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                            val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                            val position = Position(x, y)
                            val type =
                                try {
                                    val typeStr = JsonUtils.extractValue(entry, "type")
                                    de.egril.defender.model.FiefType
                                        .valueOf(typeStr)
                                } catch (e: Exception) {
                                    de.egril.defender.model.FiefType.FISHER
                                }
                            initialFiefs.add(InitialFief(position, type))
                        }
                    }
                }
            }
            // Parse gargoyles
            if (initialDataSection.contains("\"gargoyles\"")) {
                val afterKey = initialDataSection.substringAfter("\"gargoyles\"")
                val openBracketIndex = afterKey.indexOf('[')
                if (openBracketIndex != -1) {
                    val afterBracket = afterKey.substring(openBracketIndex + 1)
                    val gargoylesSection =
                        if (afterBracket.contains("],")) {
                            afterBracket.substringBefore("],")
                        } else {
                            afterBracket.substringBefore("]")
                        }
                    if (gargoylesSection.isNotBlank()) {
                        for (entry in splitJsonArrayObjects(gargoylesSection)) {
                            if (!entry.contains("position")) continue
                            val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                            val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                            val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                            val level =
                                entry
                                    .substringAfter("\"level\":", "")
                                    .trim()
                                    .takeWhile { it.isDigit() }
                                    .toIntOrNull()
                                    ?: 1
                            initialGargoyles.add(InitialGargoyle(Position(x, y), level))
                        }
                    }
                }
            }
            // Parse mushrooms from new format
            if (initialDataSection.contains("\"mushrooms\"")) {
                val afterKey = initialDataSection.substringAfter("\"mushrooms\"")
                val openBracketIndex = afterKey.indexOf('[')
                if (openBracketIndex != -1) {
                    val afterBracket = afterKey.substring(openBracketIndex + 1)
                    val mushroomsSection =
                        if (afterBracket.contains("],")) {
                            afterBracket.substringBefore("],")
                        } else {
                            afterBracket.substringBefore("]")
                        }
                    if (mushroomsSection.isNotBlank()) {
                        val mushroomEntries = splitJsonArrayObjects(mushroomsSection)
                        for (entry in mushroomEntries) {
                            if (!entry.contains("position")) continue
                            val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                            val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                            val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                            initialMushrooms.add(InitialMushroom(Position(x, y)))
                        }
                    }
                }
            }
            // Parse portals from new format
            if (initialDataSection.contains("\"portals\"")) {
                val afterKey = initialDataSection.substringAfter("\"portals\"")
                val openBracketIndex = afterKey.indexOf('[')
                if (openBracketIndex != -1) {
                    val afterBracket = afterKey.substring(openBracketIndex + 1)
                    val portalsSection =
                        if (afterBracket.contains("],")) {
                            afterBracket.substringBefore("],")
                        } else {
                            afterBracket.substringBefore("]")
                        }
                    if (portalsSection.isNotBlank()) {
                        val portalEntries = splitJsonArrayObjects(portalsSection)
                        for (entry in portalEntries) {
                            if (!entry.contains("entryPosition") || !entry.contains("exitPosition")) continue
                            val entrySection = entry.substringAfter("\"entryPosition\": {").substringBefore("}")
                            val exitSection = entry.substringAfter("\"exitPosition\": {").substringBefore("}")
                            val entryX = JsonUtils.extractValue("{$entrySection}", "x").toInt()
                            val entryY = JsonUtils.extractValue("{$entrySection}", "y").toInt()
                            val exitX = JsonUtils.extractValue("{$exitSection}", "x").toInt()
                            val exitY = JsonUtils.extractValue("{$exitSection}", "y").toInt()
                            initialPortals.add(InitialPortal(Position(entryX, entryY), Position(exitX, exitY)))
                        }
                    }
                }
            }
            // Parse placement areas (optional; absent means unrestricted)
            if (initialDataSection.contains("\"placementAreas\"")) {
                val afterKey = initialDataSection.substringAfter("\"placementAreas\"")
                val openBracketIndex = afterKey.indexOf('[')
                if (openBracketIndex != -1) {
                    val areaSection = afterKey.substring(openBracketIndex + 1).substringBefore("]")
                    for (entry in splitJsonArrayObjects(areaSection)) {
                        val x = JsonUtils.extractValue(entry, "x").toInt()
                        val y = JsonUtils.extractValue(entry, "y").toInt()
                        initialPlacementAreas.add(Position(x, y))
                    }
                }
            }
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("Error parsing initial data (new format): ${e.message}")
            }
        }
    }

    // Fall back to old flat format if new format wasn't found or was empty
    if (initialDefenders.isEmpty() &&
        initialAttackers.isEmpty() &&
        initialTraps.isEmpty() &&
        initialBarricades.isEmpty() &&
        initialBridges.isEmpty() &&
        initialFiefs.isEmpty()
    ) {
        // Parse initial defenders (legacy flat format)
        if (dataJson.contains("\"initialDefenders\"")) {
            try {
                // Extract array - handle both `],` and `]` at end
                val afterKey = dataJson.substringAfter("\"initialDefenders\": [")
                val defendersSection =
                    if (afterKey.contains("],")) {
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
                        val dragonName =
                            try {
                                JsonUtils.extractValue(entry, "dragonName").takeIf { it.isNotEmpty() }
                            } catch (e: Exception) {
                                null
                            }
                        val onTowerBase =
                            try {
                                JsonUtils.extractValue(entry, "onTowerBase").toBoolean()
                            } catch (e: Exception) {
                                false
                            }
                        initialDefenders.add(InitialDefender(type, position, level, dragonName, onTowerBase))
                    }
                }
            } catch (e: Exception) {
                if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                    println("Error parsing initial defenders (continuing without them): ${e.message}")
                }
            }
        }

        // Parse initial attackers (legacy flat format)
        if (dataJson.contains("\"initialAttackers\"")) {
            try {
                // Extract array - handle both `],` and `]` at end
                val afterKey = dataJson.substringAfter("\"initialAttackers\": [")
                val attackersSection =
                    if (afterKey.contains("],")) {
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
                        val currentHealth =
                            try {
                                JsonUtils.extractValue(entry, "currentHealth").toIntOrNull()
                            } catch (e: Exception) {
                                null
                            }
                        val dragonName =
                            try {
                                JsonUtils.extractValue(entry, "dragonName").takeIf { it.isNotEmpty() }
                            } catch (e: Exception) {
                                null
                            }
                        initialAttackers.add(InitialAttacker(type, position, level, currentHealth, dragonName))
                    }
                }
            } catch (e: Exception) {
                if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                    println("Error parsing initial attackers (continuing without them): ${e.message}")
                }
            }
        }

        // Parse initial traps (legacy flat format)
        if (dataJson.contains("\"initialTraps\"")) {
            try {
                // Extract array - handle both `],` and `]` at end
                val afterKey = dataJson.substringAfter("\"initialTraps\": [")
                val trapsSection =
                    if (afterKey.contains("],")) {
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
                        val type =
                            try {
                                JsonUtils.extractValue(entry, "type")
                            } catch (e: Exception) {
                                "DWARVEN"
                            }
                        initialTraps.add(InitialTrap(position, damage, type))
                    }
                }
            } catch (e: Exception) {
                if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                    println("Error parsing initial traps (continuing without them): ${e.message}")
                }
            }
        }

        // Parse initial barricades (legacy flat format)
        if (dataJson.contains("\"initialBarricades\"")) {
            try {
                // Extract array - handle both `],` and `]` at end
                val afterKey = dataJson.substringAfter("\"initialBarricades\": [")
                val barricadesSection =
                    if (afterKey.contains("],")) {
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
                        val supportsTower =
                            try {
                                JsonUtils.extractBooleanValue(entry, "supportsTower")
                            } catch (e: Exception) {
                                false
                            }
                        initialBarricades.add(InitialBarricade(position, healthPoints, supportsTower = supportsTower))
                    }
                }
            } catch (e: Exception) {
                if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                    println("Error parsing initial barricades (continuing without them): ${e.message}")
                }
            }
        }
    } // End of legacy format fallback

    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        val defenderCount = initialDefenders.size
        val attackerCount = initialAttackers.size
        val trapCount = initialTraps.size
        val barricadeCount = initialBarricades.size
        val bridgeCount = initialBridges.size
        val fiefCount = initialFiefs.size
        println(
            "EditorJsonSerializer.deserializeLevel: Parsed level $id " +
                "with $defenderCount defenders, $attackerCount attackers, " +
                "$trapCount traps, $barricadeCount barricades, $bridgeCount bridges, $fiefCount fiefs",
        )
    }

    // Create InitialData object from parsed data
    val initialData =
        if (initialDefenders.isNotEmpty() ||
            initialAttackers.isNotEmpty() ||
            initialTraps.isNotEmpty() ||
            initialBarricades.isNotEmpty() ||
            initialBridges.isNotEmpty() ||
            initialFiefs.isNotEmpty() ||
            initialMushrooms.isNotEmpty() ||
            initialGargoyles.isNotEmpty() ||
            initialPortals.isNotEmpty() ||
            initialPlacementAreas.isNotEmpty()
        ) {
            InitialData(
                defenders = initialDefenders,
                attackers = initialAttackers,
                traps = initialTraps,
                barricades = initialBarricades,
                bridges = initialBridges,
                fiefs = initialFiefs,
                mushrooms = initialMushrooms,
                gargoyles = initialGargoyles,
                portals = initialPortals,
                placementAreas = initialPlacementAreas,
            )
        } else {
            null
        }
    return initialData
}
