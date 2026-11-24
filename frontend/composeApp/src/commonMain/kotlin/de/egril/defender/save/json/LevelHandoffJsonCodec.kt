package de.egril.defender.save

import de.egril.defender.config.LogConfig
import de.egril.defender.model.*
import de.egril.defender.save.SaveJsonFormat.PROGRAM_NAME
import de.egril.defender.save.SavedEntityJsonParser.parseSavedBarricade
import de.egril.defender.save.SavedEntityJsonParser.parseSavedDefender
import de.egril.defender.save.SavedEntityJsonParser.parseSavedRaft
import de.egril.defender.save.SavedEntityJsonParser.parseSavedTrap
import de.egril.defender.utils.JsonUtils

internal object LevelHandoffJsonCodec {
    internal fun serializeLevelHandoffSave(handoff: LevelHandoffSave): String {
        val defendersJson =
            handoff.defenders.joinToString(",\n    ") { defender ->
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

        val barricadesJson =
            handoff.barricades.joinToString(",\n    ") { barricade ->
                val supportedTowerIdStr = barricade.supportedTowerId?.toString() ?: "null"
                """{
      "position": {"x": ${barricade.position.x}, "y": ${barricade.position.y}},
      "healthPoints": ${barricade.healthPoints},
      "defenderId": ${barricade.defenderId},
      "id": ${barricade.id},
      "supportedTowerId": $supportedTowerIdStr
    }"""
            }

        val trapsJson =
            handoff.traps.joinToString(",\n    ") { trap ->
                """{
      "position": {"x": ${trap.position.x}, "y": ${trap.position.y}},
      "damage": ${trap.damage},
      "defenderId": ${trap.defenderId},
      "type": "${trap.type}"
    }"""
            }

        val raftsJson =
            handoff.rafts.joinToString(",\n    ") { raft ->
                """{
      "id": ${raft.id},
      "defenderId": ${raft.defenderId},
      "position": {"x": ${raft.position.x}, "y": ${raft.position.y}},
      "healthPoints": ${raft.healthPoints}
    }"""
            }

        val data = """{
  "fromLevelEditorId": "${handoff.fromLevelEditorId}",
  "toLevelEditorId": "${handoff.toLevelEditorId}",
  "coins": ${handoff.coins},
  "currentMana": ${handoff.currentMana},
  "maxMana": ${handoff.maxMana},
  "nextDefenderId": ${handoff.nextDefenderId},
  "nextRaftId": ${handoff.nextRaftId},
  "mapId": "${handoff.mapId}",
  "defenders": [
    $defendersJson
  ],
  "barricades": [
    $barricadesJson
  ],
  "traps": [
    $trapsJson
  ],
  "rafts": [
    $raftsJson
  ],
  "version": 1
}"""
        return """{
  "metadata": {
    "program": "$PROGRAM_NAME",
    "type": "level_handoff"
  },
  "data": $data
}"""
    }

    internal fun deserializeLevelHandoffSave(json: String): LevelHandoffSave? {
        val dataJson =
            de.egril.defender.utils.JsonUtils
                .extractDataSection(json)
        try {
            val fromLevelEditorId = JsonUtils.extractValue(dataJson, "fromLevelEditorId")
            val toLevelEditorId = JsonUtils.extractValue(dataJson, "toLevelEditorId")
            val coins = JsonUtils.extractValue(dataJson, "coins").toInt()
            val currentMana =
                try {
                    JsonUtils.extractValue(dataJson, "currentMana").toInt()
                } catch (e: Exception) {
                    0
                }
            val maxMana =
                try {
                    JsonUtils.extractValue(dataJson, "maxMana").toInt()
                } catch (e: Exception) {
                    0
                }
            val nextDefenderId = JsonUtils.extractValue(dataJson, "nextDefenderId").toInt()
            val nextRaftId =
                try {
                    JsonUtils.extractValue(dataJson, "nextRaftId").toInt()
                } catch (e: Exception) {
                    1
                }
            val mapId =
                try {
                    JsonUtils.extractValue(dataJson, "mapId")
                } catch (e: Exception) {
                    ""
                }

            // Parse array fields using substringBefore("],") pattern.
            // This requires each array to be followed by a comma (not be the last JSON field).
            // The serializer ensures this by placing "version": 1 after all arrays.

            // Parse defenders
            val defenders = mutableListOf<SavedDefender>()
            val defendersSection = dataJson.substringAfter("\"defenders\": [").substringBefore("],")
            if (defendersSection.isNotBlank()) {
                val defenderEntries = JsonUtils.splitJsonArray(defendersSection)
                for (entry in defenderEntries) {
                    defenders.add(parseSavedDefender(entry))
                }
            }

            // Parse barricades
            val barricades = mutableListOf<SavedBarricade>()
            val barricadesSection = dataJson.substringAfter("\"barricades\": [").substringBefore("],")
            if (barricadesSection.isNotBlank()) {
                val barricadeEntries = JsonUtils.splitJsonArray(barricadesSection)
                for (entry in barricadeEntries) {
                    barricades.add(parseSavedBarricade(entry))
                }
            }

            // Parse traps
            val traps = mutableListOf<SavedTrap>()
            val trapsSection = dataJson.substringAfter("\"traps\": [").substringBefore("],")
            if (trapsSection.isNotBlank()) {
                val trapEntries = JsonUtils.splitJsonArray(trapsSection)
                for (entry in trapEntries) {
                    traps.add(parseSavedTrap(entry))
                }
            }

            // Parse rafts
            val rafts = mutableListOf<SavedRaft>()
            val raftsSection = dataJson.substringAfter("\"rafts\": [").substringBefore("],")
            if (raftsSection.isNotBlank()) {
                val raftEntries = JsonUtils.splitJsonArray(raftsSection)
                for (entry in raftEntries) {
                    rafts.add(parseSavedRaft(entry))
                }
            }

            return LevelHandoffSave(
                fromLevelEditorId = fromLevelEditorId,
                toLevelEditorId = toLevelEditorId,
                coins = coins,
                currentMana = currentMana,
                maxMana = maxMana,
                defenders = defenders,
                barricades = barricades,
                traps = traps,
                rafts = rafts,
                nextDefenderId = nextDefenderId,
                nextRaftId = nextRaftId,
                mapId = mapId,
            )
        } catch (e: Exception) {
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Error deserializing level handoff save: ${e.message}")
            }
            return null
        }
    }
}
