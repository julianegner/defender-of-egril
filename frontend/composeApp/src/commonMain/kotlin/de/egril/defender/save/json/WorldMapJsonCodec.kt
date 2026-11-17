package de.egril.defender.save

import de.egril.defender.config.LogConfig
import de.egril.defender.model.*
import de.egril.defender.save.SaveJsonFormat.PROGRAM_NAME

internal object WorldMapJsonCodec {
    fun serializeWorldMapSave(worldMap: WorldMapSave): String {
        val statusesJson =
            worldMap.levelStatuses.entries.joinToString(",\n    ") { (editorLevelId, status) ->
                "\"$editorLevelId\": \"${status.name}\""
            }

        return """{
  "metadata": {
    "program": "$PROGRAM_NAME",
    "type": "level_progress"
  },
  "data": {
    "levelStatuses": {
      $statusesJson
    }
  }
}"""
    }

    fun deserializeWorldMapSave(json: String): WorldMapSave? {
        val dataJson =
            de.egril.defender.utils.JsonUtils
                .extractDataSection(json)
        try {
            val statuses = mutableMapOf<String, LevelStatus>()
            val statusesSection =
                dataJson
                    .substringAfter("\"levelStatuses\": {")
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
}
