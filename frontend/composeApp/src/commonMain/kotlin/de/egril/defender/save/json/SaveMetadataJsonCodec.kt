package de.egril.defender.save

import de.egril.defender.model.*

internal object SaveMetadataJsonCodec {
    internal fun serializeSaveGameMetadata(metadata: SaveGameMetadata): String =
        """{
  "id": "${metadata.id}",
  "timestamp": ${metadata.timestamp},
  "levelId": ${metadata.levelId},
  "levelName": "${metadata.levelName}",
  "turnNumber": ${metadata.turnNumber},
  "towerCount": ${metadata.towerCount},
  "enemyCount": ${metadata.enemyCount}
}"""

    /**
     * Extract comment value from JSON, handling escaped characters properly
     */
    internal fun extractCommentValue(json: String): String? {
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
