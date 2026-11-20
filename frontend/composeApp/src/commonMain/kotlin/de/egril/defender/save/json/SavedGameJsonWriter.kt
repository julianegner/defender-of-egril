package de.egril.defender.save

import de.egril.defender.model.*
import de.egril.defender.save.SaveJsonFormat.PROGRAM_NAME
import de.egril.defender.utils.EventMapImageJson

internal object SavedGameJsonWriter {
    fun serializeSavedGame(savedGame: SavedGame): String {
        val defendersJson =
            savedGame.defenders.joinToString(",\n    ") { defender ->
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
      "towerBaseBarricadeId": $towerBaseBarricadeIdStr,
      "isChanneling": ${defender.isChanneling},
      "trapCooldownRemaining": ${defender.trapCooldownRemaining},
      "hasBeenUsed": ${defender.hasBeenUsed},
      "isDisabled": ${defender.isDisabled},
      "disabledTurnsRemaining": ${defender.disabledTurnsRemaining}
    }"""
            }

        val attackersJson =
            savedGame.attackers.joinToString(",\n    ") { attacker ->
                val dragonNameStr = if (attacker.dragonName != null) "\"${attacker.dragonName}\"" else "null"
                """{
      "id": ${attacker.id},
      "type": "${attacker.type.name}",
      "position": {"x": ${attacker.position.x}, "y": ${attacker.position.y}},
      "level": ${attacker.level},
      "currentHealth": ${attacker.currentHealth},
      "isDefeated": ${attacker.isDefeated},
      "dragonName": $dragonNameStr,
      "movementPenalty": ${attacker.movementPenalty},
      "bloodlustRoundsLeft": ${attacker.bloodlustRoundsLeft},
      "mushroomTurnsRemaining": ${attacker.mushroomTurnsRemaining},
      "mushroomLevelBonus": ${attacker.mushroomLevelBonus},
      "goblinRunnerUndamagedRounds": ${attacker.goblinRunnerUndamagedRounds},
      "goblinRunnerTookDamageSinceLastTurn": ${attacker.goblinRunnerTookDamageSinceLastTurn},
      "goblinRunnerSpawnTurnNumber": ${attacker.goblinRunnerSpawnTurnNumber},
      "goblinRunnerMomentumReady": ${attacker.goblinRunnerMomentumReady},
      "isSubmerged": ${attacker.isSubmerged}
    }"""
            }

        val attackersToSpawnJson = savedGame.attackersToSpawn.joinToString(", ") { "\"${it.name}\"" }

        val fieldEffectsJson =
            savedGame.fieldEffects.joinToString(",\n    ") { effect ->
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

        val trapsJson =
            savedGame.traps.joinToString(",\n    ") { trap ->
                """{
      "position": {"x": ${trap.position.x}, "y": ${trap.position.y}},
      "damage": ${trap.damage},
      "defenderId": ${trap.defenderId},
      "type": "${trap.type}"
    }"""
            }

        val raftsJson =
            savedGame.rafts.joinToString(",\n    ") { raft ->
                """{
      "id": ${raft.id},
      "defenderId": ${raft.defenderId},
      "position": {"x": ${raft.position.x}, "y": ${raft.position.y}},
      "healthPoints": ${raft.healthPoints}
    }"""
            }

        val barricadesJson =
            savedGame.barricades.joinToString(",\n    ") { barricade ->
                val supportedTowerIdStr = barricade.supportedTowerId?.toString() ?: "null"
                """{
      "position": {"x": ${barricade.position.x}, "y": ${barricade.position.y}},
      "healthPoints": ${barricade.healthPoints},
      "defenderId": ${barricade.defenderId},
      "id": ${barricade.id},
      "supportedTowerId": $supportedTowerIdStr
    }"""
            }

        val fiefsJson =
            savedGame.fiefs.joinToString(",\n    ") { fief ->
                """{"position": {"x": ${fief.position.x}, "y": ${fief.position.y}}, "type": "${fief.type}"}"""
            }

        val mushroomsJson =
            savedGame.mushrooms.joinToString(",\n    ") { mushroom ->
                """{"position": {"x": ${mushroom.position.x}, "y": ${mushroom.position.y}}}"""
            }

        val gargoylesJson =
            savedGame.gargoyles.joinToString(",\n    ") { gargoyle ->
                val activeAttackerIdStr = gargoyle.activeAttackerId?.toString() ?: "null"
                """{"id": ${gargoyle.id}, "position": {"x": ${gargoyle.position.x}, "y": ${gargoyle.position.y}}, "level": ${gargoyle.level}, "currentHealth": ${gargoyle.currentHealth}, "isActive": ${gargoyle.isActive}, "activeTurnsRemaining": ${gargoyle.activeTurnsRemaining}, "activeAttackerId": $activeAttackerIdStr}"""
            }

        val spellEffectsJson =
            savedGame.spellEffects.joinToString(",\n    ") { effect ->
                val posStr = effect.position?.let { """{"x": ${it.x}, "y": ${it.y}}""" } ?: "null"
                val defStr = effect.defenderId?.toString() ?: "null"
                val atkStr = effect.attackerId?.toString() ?: "null"
                """{
      "spell": "${effect.spell}",
      "position": $posStr,
      "defenderId": $defStr,
      "attackerId": $atkStr,
      "turnsRemaining": ${effect.turnsRemaining},
      "castTurn": ${effect.castTurn}
    }"""
            }

        // Escape comment for JSON (handle quotes and newlines)
        val commentJson =
            savedGame.comment?.let { comment ->
                val escaped =
                    comment
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
        val worldMapSaveJson =
            savedGame.worldMapSave?.let { worldMap ->
                val statusesJson =
                    worldMap.levelStatuses.entries.joinToString(", ") { (editorLevelId, status) ->
                        "\"$editorLevelId\": \"${status.name}\""
                    }
                """{"levelStatuses": {$statusesJson}}"""
            } ?: "null"

        // Support state (placeable objects, spell tokens, cooldown powers) as name->int maps
        val supportObjectsJson =
            savedGame.supportObjectsRemaining.entries.joinToString(", ") { (type, count) ->
                "\"${type.name}\": $count"
            }
        val supportSpellsJson =
            savedGame.supportSpellsRemaining.entries.joinToString(", ") { (spell, count) ->
                "\"${spell.name}\": $count"
            }
        val supportFiefsJson =
            savedGame.supportFiefRemaining.entries.joinToString(", ") { (type, count) ->
                "\"${type.name}\": $count"
            }
        val cooldownPowersJson =
            savedGame.cooldownPowerReadyIn.entries.joinToString(", ") { (type, readyIn) ->
                "\"${type.name}\": $readyIn"
            }
        val playedTileAnimationKeysJson =
            savedGame.playedTileAnimationKeys.joinToString(", ") { "\"$it\"" }

        // Scripted-event tracking
        val triggeredEventIdsJson =
            savedGame.triggeredEventIds.joinToString(", ") { "\"$it\"" }
        val storyLogJson =
            savedGame.storyLog.joinToString(",\n    ") { entry ->
                val nameStr = entry.name?.let { "\"$it\"" } ?: "null"
                val amountStr = entry.amount?.toString() ?: "null"
                """{"turn": ${entry.turn}, "type": "${entry.type}", "name": $nameStr, "amount": $amountStr}"""
            }
        val enemiesKilledByTypeJson =
            savedGame.enemiesKilledByType.entries.joinToString(", ") { (type, count) ->
                "\"${type.name}\": $count"
            }

        // Sandbox map tiles: array of {x, y, type}. Serialized as null when not a sandbox save.
        val sandboxMapTilesJson =
            savedGame.sandboxMapTiles?.let { tiles ->
                val entries =
                    tiles.entries.joinToString(", ") { (position, type) ->
                        "{\"x\": ${position.x}, \"y\": ${position.y}, \"type\": \"${type.name}\"}"
                    }
                "[$entries]"
            } ?: "null"

        // Sandbox river tiles: array of {x, y, flowDirection, flowSpeed}. Null when no painted rivers.
        val sandboxRiverTilesJson =
            savedGame.sandboxRiverTiles?.let { rivers ->
                val entries =
                    rivers.entries.joinToString(", ") { (position, river) ->
                        "{\"x\": ${position.x}, \"y\": ${position.y}, " +
                            "\"flowDirection\": \"${river.flowDirection.name}\", \"flowSpeed\": ${river.flowSpeed}}"
                    }
                "[$entries]"
            } ?: "null"

        val bridgesJson =
            savedGame.bridges.joinToString(",\n    ") { bridge ->
                val positionsJson =
                    bridge.positions.joinToString(", ") { position ->
                        """{"x": ${position.x}, "y": ${position.y}}"""
                    }
                """{
      "id": ${bridge.id},
      "type": "${bridge.type.name}",
      "positions": [$positionsJson],
      "currentHealth": ${bridge.currentHealth},
      "turnsRemaining": ${bridge.turnsRemaining},
      "createdByAttackerId": ${bridge.createdByAttackerId},
      "createdOnTurn": ${bridge.createdOnTurn},
      "isIndestructible": ${bridge.isIndestructible}
    }"""
            }

        val activePortalsJson =
            savedGame.activePortals.joinToString(",\n    ") { portal ->
                """{
      "id": ${portal.id},
      "entryPosition": {"x": ${portal.entryPosition.x}, "y": ${portal.entryPosition.y}},
      "exitPosition": {"x": ${portal.exitPosition.x}, "y": ${portal.exitPosition.y}},
      "villainId": ${portal.villainId},
      "runeIndex": ${portal.runeIndex},
      "usedThisTurn": ${portal.usedThisTurn}
    }"""
            }

        val takenTargetsJson =
            savedGame.takenTargets.joinToString(", ") { position ->
                """{"x": ${position.x}, "y": ${position.y}}"""
            }

        val activeTileZoneIdsJson = savedGame.activeTileZoneIds.joinToString(", ") { "\"$it\"" }
        val movedTileRegionPivotsJson =
            savedGame.movedTileRegionPivots.entries.joinToString(", ") { (zoneId, pivot) ->
                "{\"zoneId\": \"$zoneId\", \"position\": {\"x\": ${pivot.x}, \"y\": ${pivot.y}}}"
            }
        val movedTileRegionsJson =
            savedGame.movedTileRegions.entries.joinToString(",\n    ") { (zoneId, tiles) ->
                val tilesJson =
                    tiles.entries
                        .sortedWith(compareBy({ it.key.y }, { it.key.x }))
                        .joinToString(", ") { (position, tile) ->
                            val river = tile.riverTile
                            val riverPart =
                                if (river != null) {
                                    ", \"flowDirection\": \"${river.flowDirection.name}\", \"flowSpeed\": ${river.flowSpeed}"
                                } else {
                                    ""
                                }
                            "{\"position\": {\"x\": ${position.x}, \"y\": ${position.y}}, " +
                                "\"home\": {\"x\": ${tile.homePosition.x}, \"y\": ${tile.homePosition.y}}, " +
                                "\"type\": \"${tile.type.name}\"$riverPart}"
                        }
                "{\"zoneId\": \"$zoneId\", \"tiles\": [$tilesJson]}"
            }
        val activeEventMapImagesJson = savedGame.activeEventMapImages.joinToString(", ") { EventMapImageJson.serialize(it) }
        val activeAltarLinksJson = savedGame.activeAltarLinks.joinToString(", ") { EventMapImageJson.serializeAltarLink(it) }

        // Spawn loops (issue #694): serialize the runtime cursor (null for non-group levels) and the
        // logical-unit-id → attacker-id bindings.
        val spawnGroupCursorJson =
            savedGame.spawnGroupCursor?.let { cursor ->
                val framesJson =
                    cursor.frames.orEmpty().joinToString(", ") { frame ->
                        """{"entryIndex": ${frame.entryIndex}, "repetition": ${frame.repetition}, "iterationStartTurn": ${frame.iterationStartTurn}}"""
                    }
                """{"frames": [$framesJson], "segmentStartTurn": ${cursor.segmentStartTurn}, "finished": ${cursor.finished}, "lastProcessedTurn": ${cursor.lastProcessedTurn}}"""
            } ?: "null"
        val spawnGroupBindingsJson =
            savedGame.spawnGroupBindings.entries.joinToString(", ") { (unitId, attackerId) ->
                "\"$unitId\": $attackerId"
            }

        // Each frame is written as [stepIndex, turnsRemaining, iterationsDone, stepExecuted (0/1)].
        val activeEventLoopsJson =
            savedGame.activeEventLoops.joinToString(", ") { loop ->
                val framesJson =
                    loop.frames.joinToString(", ") { frame ->
                        "[${frame.stepIndex}, ${frame.turnsRemaining}, ${frame.iterationsDone}, ${if (frame.stepExecuted) 1 else 0}]"
                    }
                """{"eventId": "${loop.eventId}", "frames": [$framesJson]}"""
            }

        val data = """{
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
  "fiefs": [
    $fiefsJson
  ],
  "mushrooms": [
    $mushroomsJson
  ],
  "gargoyles": [
    $gargoylesJson
  ],
  "nextGargoyleId": ${savedGame.nextGargoyleId},
  "spellEffects": [
    $spellEffectsJson
  ],
  "comment": $commentJson,
  "mapId": $mapIdJson,
  "worldMapSave": $worldMapSaveJson,
  "currentMana": ${savedGame.currentMana},
  "maxMana": ${savedGame.maxMana},
  "runes": ${savedGame.runes},
  "scriptedVictory": ${savedGame.scriptedVictory},
  "supportObjectsRemaining": {$supportObjectsJson},
  "supportSpellsRemaining": {$supportSpellsJson},
  "supportFiefRemaining": {$supportFiefsJson},
  "cooldownPowerReadyIn": {$cooldownPowersJson},
  "coinSurgeActive": ${savedGame.coinSurgeActive},
  "playedTileAnimationKeys": [$playedTileAnimationKeysJson],
  "triggeredEventIds": [$triggeredEventIdsJson],
  "storyLog": [
    $storyLogJson
  ],
  "enemiesKilledTotal": ${savedGame.enemiesKilledTotal},
  "enemiesKilledByType": {$enemiesKilledByTypeJson},
  "waaghPoints": ${savedGame.waaghPoints},
  "waaghFrenzyActive": ${savedGame.waaghFrenzyActive},
  "waaghFrenzyRoundsLeft": ${savedGame.waaghFrenzyRoundsLeft},
  "hasShownWaaghFrenzyMessage": ${savedGame.hasShownWaaghFrenzyMessage},
  "sandboxMapTiles": $sandboxMapTilesJson,
  "sandboxRiverTiles": $sandboxRiverTilesJson,
  "bridges": [
   $bridgesJson
  ],
  "nextBridgeId": ${savedGame.nextBridgeId},
  "activePortals": [
    $activePortalsJson
  ],
  "nextPortalId": ${savedGame.nextPortalId},
  "takenTargets": [$takenTargetsJson],
  "activeTileZoneIds": [$activeTileZoneIdsJson],
  "movedTileRegions": [
    $movedTileRegionsJson
  ],
  "movedTileRegionPivots": [$movedTileRegionPivotsJson],
  "activeEventLoops": [$activeEventLoopsJson],
  "activeEventMapImages": [$activeEventMapImagesJson],
  "activeAltarLinks": [$activeAltarLinksJson],
  "spawnGroupCursor": $spawnGroupCursorJson,
  "spawnGroupBindings": {$spawnGroupBindingsJson},
  "stoppedSpawnLoops": [${savedGame.stoppedSpawnLoops.joinToString(", ") { EventMapImageJson.quote(it) }}],
  "bridges": [
   $bridgesJson
  ],
  "nextBridgeId": ${savedGame.nextBridgeId}
}"""
        return """{
  "metadata": {
    "program": "$PROGRAM_NAME",
    "type": "savegame"
  },
  "data": $data
}"""
    }
}
