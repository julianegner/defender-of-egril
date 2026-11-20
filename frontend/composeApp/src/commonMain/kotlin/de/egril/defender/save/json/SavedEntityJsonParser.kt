package de.egril.defender.save

import de.egril.defender.model.*
import de.egril.defender.utils.JsonUtils

internal object SavedEntityJsonParser {
    internal fun parseSavedDefender(json: String): SavedDefender {
        val id = JsonUtils.extractValue(json, "id").toInt()
        val type = DefenderType.valueOf(JsonUtils.extractValue(json, "type"))
        val position = parsePosition(json)
        val level = JsonUtils.extractValue(json, "level").toInt()
        val buildTimeRemaining = JsonUtils.extractValue(json, "buildTimeRemaining").toInt()
        val placedOnTurn = JsonUtils.extractValue(json, "placedOnTurn").toInt()
        // Backward compatibility: default to 0 if field doesn't exist in old saves
        val actionsRemaining =
            try {
                JsonUtils.extractValue(json, "actionsRemaining").toIntOrNull() ?: 0
            } catch (e: Exception) {
                0
            }
        // Backward compatibility: default to null if field doesn't exist in old saves
        val dragonName =
            try {
                val value = JsonUtils.extractValue(json, "dragonName")
                if (value == "null") null else value
            } catch (e: Exception) {
                null
            }

        // Backward compatibility: default to null if field doesn't exist in old saves
        val raftId =
            try {
                val value = JsonUtils.extractValue(json, "raftId")
                if (value == "null") null else value.toIntOrNull()
            } catch (e: Exception) {
                null
            }

        // Backward compatibility: default to null if field doesn't exist in old saves
        val towerBaseBarricadeId =
            try {
                val value = JsonUtils.extractValue(json, "towerBaseBarricadeId")
                if (value == "null") null else value.toIntOrNull()
            } catch (e: Exception) {
                null
            }

        return SavedDefender(
            id,
            type,
            position,
            level,
            buildTimeRemaining,
            placedOnTurn,
            actionsRemaining,
            dragonName,
            raftId,
            towerBaseBarricadeId,
            isChanneling = JsonUtils.extractValue(json, "isChanneling") == "true",
            trapCooldownRemaining = JsonUtils.extractValue(json, "trapCooldownRemaining").toIntOrNull() ?: 0,
            hasBeenUsed = JsonUtils.extractValue(json, "hasBeenUsed") == "true",
            isDisabled = JsonUtils.extractValue(json, "isDisabled") == "true",
            disabledTurnsRemaining = JsonUtils.extractValue(json, "disabledTurnsRemaining").toIntOrNull() ?: 0,
        )
    }

    internal fun parseSavedRaft(json: String): SavedRaft {
        val id = JsonUtils.extractValue(json, "id").toInt()
        val defenderId = JsonUtils.extractValue(json, "defenderId").toInt()
        val position = parsePosition(json)
        val healthPoints = JsonUtils.extractNumericValue(json, "healthPoints").toIntOrNull() ?: Raft.RAFT_MAX_HEALTH
        return SavedRaft(id, defenderId, position, healthPoints)
    }

    internal fun parseSavedBarricade(json: String): SavedBarricade {
        val position = parsePosition(json)
        val healthPoints = JsonUtils.extractValue(json, "healthPoints").toInt()
        val defenderId = JsonUtils.extractValue(json, "defenderId").toInt()

        // Backward compatibility: default to 0 if field doesn't exist in old saves
        val id =
            try {
                JsonUtils.extractValue(json, "id").toIntOrNull() ?: 0
            } catch (e: Exception) {
                0
            }

        // Backward compatibility: default to null if field doesn't exist in old saves
        val supportedTowerId =
            try {
                val value = JsonUtils.extractValue(json, "supportedTowerId")
                if (value == "null") null else value.toIntOrNull()
            } catch (e: Exception) {
                null
            }

        return SavedBarricade(position, healthPoints, defenderId, id, supportedTowerId)
    }

    internal fun parseSavedBridge(json: String): SavedBridge {
        val id = JsonUtils.extractValue(json, "id").toInt()
        val type = BridgeType.valueOf(JsonUtils.extractValue(json, "type"))
        val currentHealth = JsonUtils.extractValue(json, "currentHealth").toInt()
        val turnsRemaining = JsonUtils.extractValue(json, "turnsRemaining").toInt()
        val createdByAttackerId = JsonUtils.extractValue(json, "createdByAttackerId").toInt()
        val createdOnTurn = JsonUtils.extractValue(json, "createdOnTurn").toInt()
        val isIndestructible =
            try {
                JsonUtils.extractBooleanValue(json, "isIndestructible")
            } catch (e: Exception) {
                false
            }

        val positions = mutableListOf<Position>()
        val positionsSection = JsonUtils.extractJsonArrayForKey(json, "positions")
        if (positionsSection.isNotBlank()) {
            val positionEntries = JsonUtils.splitJsonArray(positionsSection)
            for (entry in positionEntries) {
                val x = JsonUtils.extractValue(entry, "x").toInt()
                val y = JsonUtils.extractValue(entry, "y").toInt()
                val position = Position(x, y)
                positions.add(position)
            }
        }

        return SavedBridge(
            id = id,
            type = type,
            positions = positions,
            currentHealth = currentHealth,
            turnsRemaining = turnsRemaining,
            createdByAttackerId = createdByAttackerId,
            createdOnTurn = createdOnTurn,
            isIndestructible = isIndestructible,
        )
    }

    internal fun parseSavedPortal(json: String): SavedPortal {
        val id = JsonUtils.extractValue(json, "id").toInt()
        val villainId = JsonUtils.extractValue(json, "villainId").toInt()
        val runeIndex = JsonUtils.extractValue(json, "runeIndex").toInt()
        val usedThisTurn =
            try {
                JsonUtils.extractBooleanValue(json, "usedThisTurn")
            } catch (e: Exception) {
                false
            }

        val entryJson = JsonUtils.extractJsonObjectForKey(json, "entryPosition")
        val entryX = JsonUtils.extractValue(entryJson, "x").toInt()
        val entryY = JsonUtils.extractValue(entryJson, "y").toInt()
        val exitJson = JsonUtils.extractJsonObjectForKey(json, "exitPosition")
        val exitX = JsonUtils.extractValue(exitJson, "x").toInt()
        val exitY = JsonUtils.extractValue(exitJson, "y").toInt()

        return SavedPortal(
            id = id,
            entryPosition = Position(entryX, entryY),
            exitPosition = Position(exitX, exitY),
            villainId = villainId,
            runeIndex = runeIndex,
            usedThisTurn = usedThisTurn,
        )
    }

    internal fun parseSavedGargoyle(json: String): SavedGargoyle {
        val activeAttackerId =
            JsonUtils.extractValue(json, "activeAttackerId").let { it.toIntOrNull() }
        return SavedGargoyle(
            id = JsonUtils.extractValue(json, "id").toInt(),
            position = parsePosition(json),
            level = JsonUtils.extractValue(json, "level").toInt(),
            currentHealth = JsonUtils.extractValue(json, "currentHealth").toInt(),
            isActive = JsonUtils.extractBooleanValue(json, "isActive"),
            activeTurnsRemaining = JsonUtils.extractValue(json, "activeTurnsRemaining").toIntOrNull() ?: 0,
            activeAttackerId = activeAttackerId,
        )
    }

    internal fun parseSavedAttacker(json: String): SavedAttacker {
        val id = JsonUtils.extractValue(json, "id").toInt()
        val type = AttackerType.valueOf(JsonUtils.extractValue(json, "type"))
        val position = parsePosition(json)
        val level = JsonUtils.extractValue(json, "level").toInt()
        val currentHealth = JsonUtils.extractValue(json, "currentHealth").toInt()
        val isDefeated = JsonUtils.extractValue(json, "isDefeated").toBoolean()
        // Backward compatibility: default to null if field doesn't exist in old saves
        val dragonName =
            try {
                val value = JsonUtils.extractValue(json, "dragonName")
                if (value == "null") null else value
            } catch (e: Exception) {
                null
            }
        // Backward compatibility: default to 0 if field doesn't exist in old saves
        val movementPenalty =
            try {
                JsonUtils.extractValue(json, "movementPenalty").toInt()
            } catch (e: Exception) {
                0
            }
        val bloodlustRoundsLeft =
            try {
                JsonUtils.extractValue(json, "bloodlustRoundsLeft").toInt()
            } catch (e: Exception) {
                0
            }
        val mushroomTurnsRemaining =
            try {
                JsonUtils.extractValue(json, "mushroomTurnsRemaining").toInt()
            } catch (e: Exception) {
                0
            }
        val mushroomLevelBonus =
            try {
                JsonUtils.extractValue(json, "mushroomLevelBonus").toInt()
            } catch (e: Exception) {
                0
            }
        val goblinRunnerUndamagedRounds =
            try {
                JsonUtils.extractValue(json, "goblinRunnerUndamagedRounds").toInt()
            } catch (e: Exception) {
                0
            }
        val goblinRunnerTookDamageSinceLastTurn =
            try {
                JsonUtils.extractValue(json, "goblinRunnerTookDamageSinceLastTurn").toBoolean()
            } catch (e: Exception) {
                false
            }
        val goblinRunnerSpawnTurnNumber =
            try {
                JsonUtils.extractValue(json, "goblinRunnerSpawnTurnNumber").toInt()
            } catch (e: Exception) {
                -1
            }
        val goblinRunnerMomentumReady =
            try {
                JsonUtils.extractValue(json, "goblinRunnerMomentumReady").toBoolean()
            } catch (e: Exception) {
                false
            }
        val isSubmerged = JsonUtils.extractBooleanValue(json, "isSubmerged")

        return SavedAttacker(
            id = id,
            type = type,
            position = position,
            level = level,
            currentHealth = currentHealth,
            isDefeated = isDefeated,
            dragonName = dragonName,
            movementPenalty = movementPenalty,
            bloodlustRoundsLeft = bloodlustRoundsLeft,
            mushroomTurnsRemaining = mushroomTurnsRemaining,
            mushroomLevelBonus = mushroomLevelBonus,
            goblinRunnerUndamagedRounds = goblinRunnerUndamagedRounds,
            goblinRunnerTookDamageSinceLastTurn = goblinRunnerTookDamageSinceLastTurn,
            goblinRunnerSpawnTurnNumber = goblinRunnerSpawnTurnNumber,
            goblinRunnerMomentumReady = goblinRunnerMomentumReady,
            isSubmerged = isSubmerged,
        )
    }

    internal fun parseSavedFieldEffect(json: String): SavedFieldEffect {
        val position = parsePosition(json)
        val type = FieldEffectType.valueOf(JsonUtils.extractValue(json, "type"))
        val damage = JsonUtils.extractValue(json, "damage").toInt()
        val turnsRemaining = JsonUtils.extractValue(json, "turnsRemaining").toInt()
        val defenderId = JsonUtils.extractValue(json, "defenderId").toInt()
        val attackerIdStr = JsonUtils.extractValue(json, "attackerId")
        val attackerId = if (attackerIdStr == "null") null else attackerIdStr.toInt()

        return SavedFieldEffect(position, type, damage, turnsRemaining, defenderId, attackerId)
    }

    internal fun parseSavedTrap(json: String): SavedTrap {
        val position = parsePosition(json)
        val damage = JsonUtils.extractValue(json, "damage").toInt()
        // Support both old mineId and new defenderId for backwards compatibility
        val defenderId =
            try {
                JsonUtils.extractValue(json, "defenderId").toInt()
            } catch (e: Exception) {
                JsonUtils.extractValue(json, "mineId").toInt() // Fallback to old field name
            }
        val type =
            try {
                JsonUtils.extractValue(json, "type")
            } catch (e: Exception) {
                "DWARVEN" // Default to dwarven trap for old saves
            }
        return SavedTrap(position, damage, defenderId, type)
    }

    internal fun parseSavedSpellEffect(json: String): SavedSpellEffect {
        val spell = JsonUtils.extractValue(json, "spell")
        val posSection =
            try {
                val raw = json.substringAfter("\"position\":").trim()
                if (raw.startsWith("null")) {
                    null
                } else {
                    val inner = raw.substringAfter("{").substringBefore("}")
                    val x = JsonUtils.extractValue(inner, "x").toInt()
                    val y = JsonUtils.extractValue(inner, "y").toInt()
                    Position(x, y)
                }
            } catch (e: Exception) {
                null
            }
        val defenderId =
            try {
                val defStr = JsonUtils.extractValue(json, "defenderId")
                if (defStr == "null") null else defStr.toInt()
            } catch (e: Exception) {
                null
            }
        val attackerId =
            try {
                val atkStr = JsonUtils.extractValue(json, "attackerId")
                if (atkStr == "null") null else atkStr.toInt()
            } catch (e: Exception) {
                null
            }
        val turnsRemaining =
            try {
                JsonUtils.extractValue(json, "turnsRemaining").toInt()
            } catch (e: Exception) {
                0
            }
        val castTurn =
            try {
                JsonUtils.extractValue(json, "castTurn").toInt()
            } catch (e: Exception) {
                0
            }
        return SavedSpellEffect(spell, posSection, defenderId, attackerId, turnsRemaining, castTurn)
    }

    /** Parse one entry of "movedTileRegions": the zone id and the tiles it covers with their terrain. */
    internal fun parseMovedTileRegion(json: String): Pair<String, Map<Position, de.egril.defender.model.RegionTile>> {
        val zoneId = JsonUtils.extractStringValue(json, "zoneId")
        val tiles =
            JsonUtils
                .splitJsonArray(JsonUtils.extractJsonArrayForKey(json, "tiles"))
                .associate { tileJson ->
                    fun pointIn(key: String): Position {
                        val section = JsonUtils.extractJsonObjectForKey(tileJson, key)
                        return Position(JsonUtils.extractValue(section, "x").toInt(), JsonUtils.extractValue(section, "y").toInt())
                    }
                    val position = pointIn("position")
                    val type =
                        de.egril.defender.editor.TileType
                            .valueOf(JsonUtils.extractStringValue(tileJson, "type"))
                    val river =
                        if (type == de.egril.defender.editor.TileType.RIVER) {
                            de.egril.defender.model.RiverTile(
                                position = position,
                                flowDirection =
                                    runCatching {
                                        de.egril.defender.model.RiverFlow
                                            .valueOf(JsonUtils.extractStringValue(tileJson, "flowDirection"))
                                    }.getOrDefault(de.egril.defender.model.RiverFlow.NONE),
                                flowSpeed = JsonUtils.extractNumericValue(tileJson, "flowSpeed").toIntOrNull()?.coerceIn(1, 2) ?: 1,
                            )
                        } else {
                            null
                        }
                    position to
                        de.egril.defender.model
                            .RegionTile(type, river, pointIn("home"))
                }
        return zoneId to tiles
    }

    internal fun parsePosition(json: String): Position {
        val posSection = json.substringAfter("\"position\": {").substringBefore("}")
        val x = JsonUtils.extractValue(posSection, "x").toInt()
        val y = JsonUtils.extractValue(posSection, "y").toInt()
        return Position(x, y)
    }
}
