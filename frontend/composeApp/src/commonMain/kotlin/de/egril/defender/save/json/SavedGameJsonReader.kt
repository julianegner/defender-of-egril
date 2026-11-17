package de.egril.defender.save

import de.egril.defender.config.LogConfig
import de.egril.defender.model.*
import de.egril.defender.save.SavedEntityJsonParser.parsePosition
import de.egril.defender.save.SavedEntityJsonParser.parseMovedTileRegion
import de.egril.defender.save.SavedEntityJsonParser.parseSavedAttacker
import de.egril.defender.save.SavedEntityJsonParser.parseSavedBarricade
import de.egril.defender.save.SavedEntityJsonParser.parseSavedBridge
import de.egril.defender.save.SavedEntityJsonParser.parseSavedDefender
import de.egril.defender.save.SavedEntityJsonParser.parseSavedFieldEffect
import de.egril.defender.save.SavedEntityJsonParser.parseSavedGargoyle
import de.egril.defender.save.SavedEntityJsonParser.parseSavedPortal
import de.egril.defender.save.SavedEntityJsonParser.parseSavedRaft
import de.egril.defender.save.SavedEntityJsonParser.parseSavedSpellEffect
import de.egril.defender.save.SavedEntityJsonParser.parseSavedTrap
import de.egril.defender.save.SaveMetadataJsonCodec.extractCommentValue
import de.egril.defender.save.WorldMapJsonCodec.deserializeWorldMapSave
import de.egril.defender.utils.EventMapImageJson
import de.egril.defender.utils.JsonUtils
import de.egril.defender.utils.currentTimeMillis

internal object SavedGameJsonReader {
    fun deserializeSavedGame(json: String): SavedGame? {
        val dataJson =
            de.egril.defender.utils.JsonUtils
                .extractDataSection(json)
        try {
            fun parseRequiredInt(field: String): Int? =
                try {
                    JsonUtils.extractValue(dataJson, field).toIntOrNull()
                } catch (e: Exception) {
                    null
                }

            val id = JsonUtils.extractValue(dataJson, "id")
            val rawTimestamp = JsonUtils.extractValue(dataJson, "timestamp")
            val timestamp =
                rawTimestamp.toLongOrNull() ?: run {
                    if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                        println("Invalid or missing timestamp in save '$id': '$rawTimestamp'. Using current time.")
                    }
                    currentTimeMillis()
                }
            val levelId = parseRequiredInt("levelId") ?: return null
            val levelName = JsonUtils.extractValue(dataJson, "levelName")
            val turnNumber = parseRequiredInt("turnNumber") ?: return null
            val coins = parseRequiredInt("coins") ?: return null
            val healthPoints = parseRequiredInt("healthPoints") ?: return null
            val phase = GamePhase.valueOf(JsonUtils.extractValue(dataJson, "phase"))
            val nextDefenderId = parseRequiredInt("nextDefenderId") ?: return null
            val nextAttackerId = parseRequiredInt("nextAttackerId") ?: return null
            val currentWaveIndex = parseRequiredInt("currentWaveIndex") ?: return null
            val spawnCounter = parseRequiredInt("spawnCounter") ?: return null

            // Parse defenders
            val defenders = mutableListOf<SavedDefender>()
            val defendersSection = dataJson.substringAfter("\"defenders\": [").substringBefore("],")
            if (defendersSection.isNotBlank()) {
                val defenderEntries = JsonUtils.splitJsonArray(defendersSection)
                for (entry in defenderEntries) {
                    defenders.add(parseSavedDefender(entry))
                }
            }

            // Parse attackers
            val attackers = mutableListOf<SavedAttacker>()
            val attackersSection = dataJson.substringAfter("\"attackers\": [").substringBefore("],")
            if (attackersSection.isNotBlank()) {
                val attackerEntries = JsonUtils.splitJsonArray(attackersSection)
                for (entry in attackerEntries) {
                    attackers.add(parseSavedAttacker(entry))
                }
            }

            // Parse attackersToSpawn
            val attackersToSpawn = mutableListOf<AttackerType>()
            val attackersToSpawnSection = dataJson.substringAfter("\"attackersToSpawn\": [").substringBefore("],")
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
            val fieldEffectsSection = dataJson.substringAfter("\"fieldEffects\": [").substringBefore("],")
            if (fieldEffectsSection.isNotBlank()) {
                val effectEntries = JsonUtils.splitJsonArray(fieldEffectsSection)
                for (entry in effectEntries) {
                    fieldEffects.add(parseSavedFieldEffect(entry))
                }
            }

            // Parse traps
            val traps = mutableListOf<SavedTrap>()
            val trapsSection =
                try {
                    dataJson.substringAfter("\"traps\": [").substringBefore("],")
                } catch (e: Exception) {
                    "" // Handle old saves without rafts field
                }
            if (trapsSection.isNotBlank()) {
                val trapEntries = JsonUtils.splitJsonArray(trapsSection)
                for (entry in trapEntries) {
                    traps.add(parseSavedTrap(entry))
                }
            }

            // Parse rafts (optional field for backward compatibility with old saves)
            val rafts = mutableListOf<SavedRaft>()
            if (dataJson.contains("\"rafts\":")) {
                val raftsSection =
                    try {
                        dataJson.substringAfter("\"rafts\": [").substringBefore("],")
                    } catch (e: Exception) {
                        "" // Old saves don't have rafts
                    }
                if (raftsSection.isNotBlank()) {
                    val raftEntries = JsonUtils.splitJsonArray(raftsSection)
                    for (entry in raftEntries) {
                        rafts.add(parseSavedRaft(entry))
                    }
                }
            }

            // Parse nextRaftId (optional field for backward compatibility)
            val nextRaftId =
                try {
                    JsonUtils.extractValue(dataJson, "nextRaftId").toInt()
                } catch (e: Exception) {
                    1 // Default to 1 for old saves
                }

            // Parse barricades (optional field for backward compatibility with old saves)
            val barricades = mutableListOf<SavedBarricade>()
            if (dataJson.contains("\"barricades\":")) {
                val barricadesSection =
                    try {
                        dataJson.substringAfter("\"barricades\": [").substringBefore("],")
                    } catch (e: Exception) {
                        "" // Old saves don't have barricades
                    }
                if (barricadesSection.isNotBlank()) {
                    val barricadeEntries = JsonUtils.splitJsonArray(barricadesSection)
                    for (entry in barricadeEntries) {
                        barricades.add(parseSavedBarricade(entry))
                    }
                }
            }

            // Parse fiefs (optional field for backward compatibility with old saves)
            val fiefs = mutableListOf<SavedFief>()
            if (dataJson.contains("\"fiefs\":")) {
                val fiefsSection =
                    try {
                        dataJson.substringAfter("\"fiefs\": [").substringBefore("],")
                    } catch (e: Exception) {
                        ""
                    }
                if (fiefsSection.isNotBlank()) {
                    val fiefEntries = JsonUtils.splitJsonArray(fiefsSection)
                    for (entry in fiefEntries) {
                        val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                        val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                        val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                        val type = JsonUtils.extractValue(entry, "type")
                        fiefs.add(SavedFief(Position(x, y), type))
                    }
                }
            }

            // Parse mushrooms (optional field for backward compatibility with old saves)
            val mushrooms = mutableListOf<SavedMushroom>()
            if (dataJson.contains("\"mushrooms\":")) {
                val mushroomsSection =
                    try {
                        dataJson.substringAfter("\"mushrooms\": [").substringBefore("],")
                    } catch (e: Exception) {
                        ""
                    }
                if (mushroomsSection.isNotBlank()) {
                    val mushroomEntries = JsonUtils.splitJsonArray(mushroomsSection)
                    for (entry in mushroomEntries) {
                        val posSection = entry.substringAfter("\"position\": {").substringBefore("}")
                        val x = JsonUtils.extractValue("{$posSection}", "x").toInt()
                        val y = JsonUtils.extractValue("{$posSection}", "y").toInt()
                        mushrooms.add(SavedMushroom(Position(x, y)))
                    }
                }
            }

            // Parse gargoyles (optional field for backward compatibility with old saves)
            val gargoyles = mutableListOf<SavedGargoyle>()
            if (dataJson.contains("\"gargoyles\":")) {
                val gargoylesSection = dataJson.substringAfter("\"gargoyles\": [").substringBefore("],")
                if (gargoylesSection.isNotBlank()) {
                    for (entry in JsonUtils.splitJsonArray(gargoylesSection)) {
                        gargoyles.add(parseSavedGargoyle(entry))
                    }
                }
            }
            val nextGargoyleId =
                try {
                    JsonUtils.extractValue(dataJson, "nextGargoyleId").toInt()
                } catch (e: Exception) {
                    1
                }

            // Parse spell effects (optional field for backward compatibility with old saves)
            val spellEffects = mutableListOf<SavedSpellEffect>()
            if (dataJson.contains("\"spellEffects\":")) {
                val spellEffectsSection =
                    try {
                        dataJson.substringAfter("\"spellEffects\": [").substringBefore("],")
                    } catch (e: Exception) {
                        "" // Old saves don't have spellEffects
                    }
                if (spellEffectsSection.isNotBlank()) {
                    val effectEntries = JsonUtils.splitJsonArray(spellEffectsSection)
                    for (entry in effectEntries) {
                        spellEffects.add(parseSavedSpellEffect(entry))
                    }
                }
            }

            // Parse comment (optional field, may not exist in older saves)
            val comment =
                try {
                    extractCommentValue(dataJson)
                } catch (e: Exception) {
                    null // If comment field doesn't exist (old save), default to null
                }

            // Parse mapId (optional field, may not exist in older saves)
            val mapId =
                try {
                    val value = JsonUtils.extractValue(dataJson, "mapId")
                    if (value.isBlank() || value == "null") null else value
                } catch (e: Exception) {
                    null // If mapId field doesn't exist (old save), default to null
                }

            // Parse worldMapSave (optional field, may not exist in older saves)
            val worldMapSave =
                try {
                    if (dataJson.contains("\"worldMapSave\":")) {
                        val worldMapSection =
                            dataJson
                                .substringAfter("\"worldMapSave\":")
                                .trim()

                        // Check if it's null
                        if (worldMapSection.startsWith("null")) {
                            null
                        } else {
                            // Extract the world map object (embedded without its own metadata wrapper;
                            // deserializeWorldMapSave handles both wrapped and plain formats)
                            val worldMapJson =
                                worldMapSection
                                    .substringAfter("{")
                                    .substringBefore("}}")
                                    .let { "{$it}" }
                            deserializeWorldMapSave(worldMapJson)
                        }
                    } else {
                        null // Field doesn't exist in old saves
                    }
                } catch (e: Exception) {
                    if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                        println("Warning: Failed to parse worldMapSave: ${e.message}")
                    }
                    null // If worldMapSave field doesn't exist or is malformed, default to null
                }

            // Parse support state (optional fields for backward compatibility with old saves)
            val supportObjectsRemaining =
                parseEnumIntMap(dataJson, "supportObjectsRemaining") { SupportObjectType.valueOf(it) }
            val supportSpellsRemaining =
                parseEnumIntMap(dataJson, "supportSpellsRemaining") { SpellType.valueOf(it) }
            val supportFiefRemaining =
                parseEnumIntMap(dataJson, "supportFiefRemaining") {
                    de.egril.defender.model.FiefType
                        .valueOf(it)
                }
            val cooldownPowerReadyIn =
                parseEnumIntMap(dataJson, "cooldownPowerReadyIn") { CooldownPowerType.valueOf(it) }
            val coinSurgeActive =
                try {
                    JsonUtils.extractBooleanValue(dataJson, "coinSurgeActive")
                } catch (e: Exception) {
                    false
                }
            val playedTileAnimationKeys = parseStringArray(dataJson, "playedTileAnimationKeys")

            // Parse scripted-event tracking (optional for backward compatibility)
            val triggeredEventIds = parseStringArray(dataJson, "triggeredEventIds")
            val storyLog =
                JsonUtils
                    .splitJsonArray(JsonUtils.extractJsonArrayForKey(dataJson, "storyLog"))
                    .filter { it.isNotBlank() }
                    .map { entry ->
                        val nameRaw = JsonUtils.extractValue(entry, "name")
                        SavedStoryLogEntry(
                            turn = JsonUtils.extractValue(entry, "turn").toInt(),
                            type = JsonUtils.extractValue(entry, "type"),
                            name = nameRaw.takeUnless { it == "null" || it.isEmpty() },
                            amount = JsonUtils.extractValue(entry, "amount").toIntOrNull(),
                        )
                    }
            val enemiesKilledTotal =
                try {
                    JsonUtils.extractValue(dataJson, "enemiesKilledTotal").toInt()
                } catch (e: Exception) {
                    0
                }
            val enemiesKilledByType =
                parseEnumIntMap(dataJson, "enemiesKilledByType") { AttackerType.valueOf(it) }
            val waaghPoints =
                try {
                    JsonUtils.extractValue(dataJson, "waaghPoints").toInt()
                } catch (e: Exception) {
                    0
                }
            val waaghFrenzyActive =
                try {
                    JsonUtils.extractBooleanValue(dataJson, "waaghFrenzyActive")
                } catch (e: Exception) {
                    false
                }
            val waaghFrenzyRoundsLeft =
                try {
                    JsonUtils.extractValue(dataJson, "waaghFrenzyRoundsLeft").toInt()
                } catch (e: Exception) {
                    0
                }
            val hasShownWaaghFrenzyMessage =
                try {
                    JsonUtils.extractBooleanValue(dataJson, "hasShownWaaghFrenzyMessage")
                } catch (e: Exception) {
                    false
                }

            // Parse sandbox map tiles (optional; only present for sandbox saves). Null when absent.
            val sandboxMapTiles: Map<Position, de.egril.defender.editor.TileType>? =
                if (dataJson.contains("\"sandboxMapTiles\": [")) {
                    val tilesSection =
                        try {
                            dataJson.substringAfter("\"sandboxMapTiles\": [").substringBefore("]")
                        } catch (e: Exception) {
                            ""
                        }
                    val result = mutableMapOf<Position, de.egril.defender.editor.TileType>()
                    if (tilesSection.isNotBlank()) {
                        for (entry in JsonUtils.splitJsonArray(tilesSection)) {
                            try {
                                val x = JsonUtils.extractValue(entry, "x").toInt()
                                val y = JsonUtils.extractValue(entry, "y").toInt()
                                val type =
                                    de.egril.defender.editor.TileType
                                        .valueOf(JsonUtils.extractValue(entry, "type"))
                                result[Position(x, y)] = type
                            } catch (e: Exception) {
                                // Skip malformed tile entries.
                            }
                        }
                    }
                    result
                } else {
                    null
                }

            // Parse sandbox river tiles (optional; only present for sandbox saves with painted rivers).
            val sandboxRiverTiles: Map<Position, de.egril.defender.model.RiverTile>? =
                if (dataJson.contains("\"sandboxRiverTiles\": [")) {
                    val riverSection =
                        try {
                            dataJson.substringAfter("\"sandboxRiverTiles\": [").substringBefore("]")
                        } catch (e: Exception) {
                            ""
                        }
                    val result = mutableMapOf<Position, de.egril.defender.model.RiverTile>()
                    if (riverSection.isNotBlank()) {
                        for (entry in JsonUtils.splitJsonArray(riverSection)) {
                            try {
                                val x = JsonUtils.extractValue(entry, "x").toInt()
                                val y = JsonUtils.extractValue(entry, "y").toInt()
                                val flow =
                                    de.egril.defender.model.RiverFlow
                                        .valueOf(JsonUtils.extractValue(entry, "flowDirection"))
                                val speed = JsonUtils.extractValue(entry, "flowSpeed").toInt()
                                result[Position(x, y)] =
                                    de.egril.defender.model.RiverTile(
                                        position = Position(x, y),
                                        flowDirection = flow,
                                        flowSpeed = speed,
                                    )
                            } catch (e: Exception) {
                                // Skip malformed river entries.
                            }
                        }
                    }
                    result
                } else {
                    null
                }

            // Parse bridges (optional field for backward compatibility with old saves)
            val bridges = mutableListOf<SavedBridge>()
            if (dataJson.contains("\"bridges\":")) {
                val bridgesSection = JsonUtils.extractJsonArrayForKey(dataJson, "bridges")
                if (bridgesSection.isNotBlank()) {
                    val bridgeEntries = JsonUtils.splitJsonArray(bridgesSection)
                    for (entry in bridgeEntries) {
                        try {
                            bridges.add(parseSavedBridge(entry))
                        } catch (e: Exception) {
                            // Skip malformed bridge entries.
                        }
                    }
                }
            }

            val nextBridgeId =
                try {
                    JsonUtils.extractValue(dataJson, "nextBridgeId").toInt()
                } catch (e: Exception) {
                    1
                }

            // Parse active portals (optional for backward compatibility with old saves)
            val activePortals = mutableListOf<SavedPortal>()
            if (dataJson.contains("\"activePortals\":")) {
                val portalsSection = JsonUtils.extractJsonArrayForKey(dataJson, "activePortals")
                if (portalsSection.isNotBlank()) {
                    val portalEntries = JsonUtils.splitJsonArray(portalsSection)
                    for (entry in portalEntries) {
                        try {
                            activePortals.add(parseSavedPortal(entry))
                        } catch (e: Exception) {
                            // Skip malformed portal entries.
                        }
                    }
                }
            }

            val nextPortalId =
                try {
                    JsonUtils.extractValue(dataJson, "nextPortalId").toInt()
                } catch (e: Exception) {
                    1
                }

            val takenTargets =
                JsonUtils
                    .splitJsonArray(JsonUtils.extractJsonArrayForKey(dataJson, "takenTargets"))
                    .mapNotNull { entry ->
                        try {
                            Position(
                                JsonUtils.extractValue(entry, "x").toInt(),
                                JsonUtils.extractValue(entry, "y").toInt(),
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

            val activeTileZoneIds = parseStringArray(dataJson, "activeTileZoneIds")
            val movedTileRegions =
                JsonUtils
                    .splitJsonArray(JsonUtils.extractJsonArrayForKey(dataJson, "movedTileRegions"))
                    .mapNotNull { regionJson -> runCatching { parseMovedTileRegion(regionJson) }.getOrNull() }
                    .toMap()
            val movedTileRegionPivots =
                JsonUtils
                    .splitJsonArray(JsonUtils.extractJsonArrayForKey(dataJson, "movedTileRegionPivots"))
                    .mapNotNull { pivotJson ->
                        runCatching { JsonUtils.extractStringValue(pivotJson, "zoneId") to parsePosition(pivotJson) }.getOrNull()
                    }.toMap()
            val activeEventLoops = parseActiveEventLoops(dataJson)
            val activeEventMapImages =
                EventMapImageJson
                    .splitArray(JsonUtils.extractJsonArrayForKey(dataJson, "activeEventMapImages"))
                    .mapNotNull { EventMapImageJson.deserialize(it) }
            val activeAltarLinks =
                EventMapImageJson
                    .splitArray(JsonUtils.extractJsonArrayForKey(dataJson, "activeAltarLinks"))
                    .mapNotNull { EventMapImageJson.deserializeAltarLink(it) }

            // Spawn loops (issue #694): parse the runtime cursor (absent/null for non-group saves)
            // and the logical-unit-id → attacker-id bindings.
            val spawnGroupCursor = parseSpawnGroupCursor(dataJson)
            val spawnGroupBindings = parseEnumIntMap(dataJson, "spawnGroupBindings") { it }
            val stoppedSpawnLoops = parseStringArray(dataJson, "stoppedSpawnLoops")

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
                fiefs = fiefs,
                mushrooms = mushrooms,
                gargoyles = gargoyles,
                nextGargoyleId = nextGargoyleId,
                worldMapSave = worldMapSave,
                currentMana =
                    try {
                        JsonUtils.extractValue(dataJson, "currentMana").toInt()
                    } catch (e: Exception) {
                        0
                    },
                maxMana =
                    try {
                        JsonUtils.extractValue(dataJson, "maxMana").toInt()
                    } catch (e: Exception) {
                        0
                    },
                scriptedVictory = JsonUtils.extractValue(dataJson, "scriptedVictory") == "true",
                runes =
                    try {
                        JsonUtils.extractValue(dataJson, "runes").toInt()
                    } catch (e: Exception) {
                        0
                    },
                spellEffects = spellEffects,
                supportObjectsRemaining = supportObjectsRemaining,
                supportSpellsRemaining = supportSpellsRemaining,
                supportFiefRemaining = supportFiefRemaining,
                cooldownPowerReadyIn = cooldownPowerReadyIn,
                coinSurgeActive = coinSurgeActive,
                playedTileAnimationKeys = playedTileAnimationKeys,
                triggeredEventIds = triggeredEventIds,
                storyLog = storyLog,
                enemiesKilledTotal = enemiesKilledTotal,
                enemiesKilledByType = enemiesKilledByType,
                waaghPoints = waaghPoints,
                waaghFrenzyActive = waaghFrenzyActive,
                waaghFrenzyRoundsLeft = waaghFrenzyRoundsLeft,
                hasShownWaaghFrenzyMessage = hasShownWaaghFrenzyMessage,
                sandboxMapTiles = sandboxMapTiles,
                sandboxRiverTiles = sandboxRiverTiles,
                bridges = bridges,
                nextBridgeId = nextBridgeId,
                activePortals = activePortals,
                nextPortalId = nextPortalId,
                takenTargets = takenTargets,
                activeTileZoneIds = activeTileZoneIds,
                movedTileRegions = movedTileRegions,
                movedTileRegionPivots = movedTileRegionPivots,
                activeEventLoops = activeEventLoops,
                activeEventMapImages = activeEventMapImages,
                activeAltarLinks = activeAltarLinks,
                spawnGroupCursor = spawnGroupCursor,
                spawnGroupBindings = spawnGroupBindings,
                stoppedSpawnLoops = stoppedSpawnLoops,
            )
        } catch (e: Exception) {
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Error deserializing saved game: ${e.message}")
                e.printStackTrace()
            }
            return null
        }
    }

    /**
     * Parse a `"key": {"ENUM_NAME": intValue, ...}` object from [dataJson] into a map keyed by an
     * enum of type [K]. Missing keys, malformed sections, and unknown enum names are ignored so old
     * or partially-corrupt saves still load. Returns an empty map when the key is absent.
     */
    internal fun <E> parseEnumIntMap(
        dataJson: String,
        key: String,
        toEnum: (String) -> E,
    ): Map<E, Int> {
        if (!dataJson.contains("\"$key\":")) return emptyMap()
        val section =
            try {
                dataJson.substringAfter("\"$key\": {").substringBefore("}")
            } catch (e: Exception) {
                return emptyMap()
            }
        if (section.isBlank()) return emptyMap()
        val result = mutableMapOf<E, Int>()
        for (pair in section.split(",")) {
            val trimmed = pair.trim()
            if (trimmed.isBlank()) continue
            val name = trimmed.substringBefore(":").trim().removeSurrounding("\"")
            val value = trimmed.substringAfter(":").trim().toIntOrNull() ?: continue
            try {
                result[toEnum(name)] = value
            } catch (e: Exception) {
                // Unknown enum name (e.g. renamed/removed support) - skip it
            }
        }
        return result
    }

    /**
     * Parse the spawn-loop runtime cursor (issue #694) from [dataJson]. Returns null when absent or
     * explicitly serialized as `null` (non-group saves and old saves).
     */
    internal fun parseSpawnGroupCursor(dataJson: String): SavedSpawnGroupCursor? {
        if (!dataJson.contains("\"spawnGroupCursor\"")) return null
        val section = JsonUtils.extractJsonObjectForKey(dataJson, "spawnGroupCursor")
        if (section.isBlank()) return null
        return try {
            if (section.contains("\"frames\"")) {
                val frames =
                    JsonUtils.splitJsonArray(JsonUtils.extractJsonArrayForKey(section, "frames")).map { frame ->
                        SavedSpawnGroupFrame(
                            entryIndex = JsonUtils.extractNumericValue(frame, "entryIndex").toInt(),
                            repetition = JsonUtils.extractNumericValue(frame, "repetition").toInt(),
                            iterationStartTurn = JsonUtils.extractNumericValue(frame, "iterationStartTurn").toInt(),
                        )
                    }
                // Only read the cursor's own fields, which are written after the frames array.
                val tail = section.substringAfterLast("]")
                SavedSpawnGroupCursor(
                    frames = frames,
                    segmentStartTurn = JsonUtils.extractNumericValue(tail, "segmentStartTurn").toInt(),
                    finished = JsonUtils.extractBooleanValue(tail, "finished"),
                    lastProcessedTurn = JsonUtils.extractNumericValue(tail, "lastProcessedTurn").toInt(),
                )
            } else {
                // Saves from before nested spawn loops: one top-level group index plus its iteration.
                SavedSpawnGroupCursor(
                    frames = null,
                    segmentStartTurn = JsonUtils.extractNumericValue(section, "iterationStartTurn").toInt(),
                    finished = JsonUtils.extractBooleanValue(section, "finished"),
                    lastProcessedTurn = JsonUtils.extractNumericValue(section, "lastProcessedTurn").toInt(),
                    legacyGroupIndex = JsonUtils.extractNumericValue(section, "groupIndex").toInt(),
                    legacyRepetition = JsonUtils.extractNumericValue(section, "repetition").toInt(),
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Parse the running scripted-event loops (see [SavedGame.activeEventLoops]); malformed entries are skipped. */
    internal fun parseActiveEventLoops(dataJson: String): List<ActiveEventLoop> {
        val section = JsonUtils.extractJsonArrayForKey(dataJson, "activeEventLoops")
        if (section.isBlank()) return emptyList()
        return JsonUtils.splitJsonArray(section).mapNotNull { entry ->
            val eventId = JsonUtils.extractStringValue(entry, "eventId")
            if (eventId.isBlank()) return@mapNotNull null
            val frames =
                JsonUtils.splitJsonArray(JsonUtils.extractJsonArrayForKey(entry, "frames")).mapNotNull { frameJson ->
                    val values =
                        frameJson
                            .trim()
                            .removePrefix("[")
                            .removeSuffix("]")
                            .split(",")
                            .mapNotNull { it.trim().toIntOrNull() }
                    if (values.size < 4) {
                        null
                    } else {
                        EventLoopFrame(
                            stepIndex = values[0],
                            turnsRemaining = values[1],
                            iterationsDone = values[2],
                            stepExecuted = values[3] != 0,
                        )
                    }
                }
            if (frames.isEmpty()) null else ActiveEventLoop(eventId = eventId, frames = frames)
        }
    }

    internal fun parseStringArray(
        dataJson: String,
        key: String,
    ): MutableList<String> {
        val section = JsonUtils.extractJsonArrayForKey(dataJson, key)
        if (section.isBlank()) {
            return mutableListOf()
        }

        return JsonUtils
            .splitJsonArray(section)
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
            .toMutableList()
    }
}
