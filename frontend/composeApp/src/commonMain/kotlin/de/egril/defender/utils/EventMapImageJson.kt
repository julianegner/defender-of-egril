package de.egril.defender.utils

import de.egril.defender.config.GameLogBuffer
import de.egril.defender.model.EventMapImage

/** Shared manual JSON representation for scripted images in levels and saves. */
internal object EventMapImageJson {
    // Unlike the legacy splitter, punctuation and escaped backslashes inside names are ignored.
    fun splitArray(json: String): List<String> {
        val entries = mutableListOf<String>()
        var start = 0
        var depth = 0
        var inString = false
        var escaped = false
        for (index in json.indices) {
            val char = json[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = false
                }
            } else {
                when (char) {
                    '"' -> inString = true
                    '{', '[' -> depth++
                    '}', ']' -> depth--
                    ',' -> {
                        if (depth == 0) {
                            entries.add(json.substring(start, index).trim())
                            start = index + 1
                        }
                    }
                }
            }
        }
        json
            .substring(start)
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.let(entries::add)
        return entries
    }

    fun serialize(image: EventMapImage): String =
        "{\"id\": ${quote(image.id)}, \"fileName\": ${quote(image.fileName)}, " +
            "\"x\": ${image.x}, \"y\": ${image.y}, \"width\": ${image.width}, \"height\": ${image.height}}"

    fun deserialize(json: String): EventMapImage? {
        if (json.isBlank()) return null
        val id = stringValue(json, "id")
        val fileName = stringValue(json, "fileName")
        val x = floatValue(json, "x", 0f)
        val y = floatValue(json, "y", 0f)
        val width = floatValue(json, "width", 1f)
        val height = floatValue(json, "height", 1f)
        if (id == null || fileName == null || x == null || y == null || width == null || height == null) {
            GameLogBuffer.log("EVENT", "Invalid map image JSON fields")
            return null
        }
        val image = EventMapImage(id, fileName, x, y, width, height)
        if (!image.isValid()) {
            GameLogBuffer.log("EVENT", "Invalid map image geometry or file name for '$id'")
            return null
        }
        return image
    }

    fun quote(value: String): String =
        buildString {
            append('"')
            for (char in value) {
                when (char) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (char.code < 32) {
                            append("\\u${char.code.toString(16).padStart(4, '0')}")
                        } else {
                            append(char)
                        }
                    }
                }
            }
            append('"')
        }

    fun stringValue(
        json: String,
        key: String,
    ): String? {
        val match = Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").find(json) ?: return null
        val raw = match.groupValues[1]
        return buildString {
            var index = 0
            while (index < raw.length) {
                val char = raw[index++]
                if (char != '\\') {
                    append(char)
                    continue
                }
                if (index >= raw.length) return null
                when (val escaped = raw[index++]) {
                    '"', '\\', '/' -> append(escaped)
                    'b' -> append('\b')
                    'f' -> append('\u000C')
                    'n' -> append('\n')
                    'r' -> append('\r')
                    't' -> append('\t')
                    'u' -> {
                        if (index + 4 > raw.length) return null
                        val code = raw.substring(index, index + 4).toIntOrNull(16) ?: return null
                        append(code.toChar())
                        index += 4
                    }
                    else -> return null
                }
            }
        }
    }

    private fun floatValue(
        json: String,
        key: String,
        default: Float,
    ): Float? {
        val raw = Regex("\"$key\"\\s*:\\s*([^,}\\s]+)").find(json)?.groupValues?.get(1) ?: return default
        return raw.toFloatOrNull()
    }
}
