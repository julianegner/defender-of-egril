package de.egril.defender.editor

import de.egril.defender.utils.JsonUtils

/**
 * Shared JSON scanning helpers used by the editor codecs.
 */
internal object EditorJsonScanner {
    internal const val PROGRAM_NAME = "Defender of Egril"

    /**
     * Extracts the "data" section from a file with metadata wrapper.
     * If the JSON has no metadata wrapper (old format), returns the JSON as-is for backward compatibility.
     */
    internal fun extractDataSection(json: String): String =
        de.egril.defender.utils.JsonUtils
            .extractDataSection(json)

    /**
     * Extract the content of a JSON array by its key name.
     */
    internal fun extractJsonArray(
        json: String,
        key: String,
    ): String {
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
    internal fun splitJsonArrayObjects(arrayContent: String): List<String> {
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

    /**
     * Extract the JSON object body that follows a given object key (e.g. "supports").
     * Returns the text between the matching braces, or "" if the key is not present.
     */
    internal fun extractObjectSection(
        json: String,
        key: String,
    ): String {
        if (!json.contains("\"$key\"")) return ""
        val afterKey = json.substringAfter("\"$key\"")
        val openBraceIndex = afterKey.indexOf('{')
        if (openBraceIndex == -1) return ""
        val afterBrace = afterKey.substring(openBraceIndex + 1)
        var braceCount = 1
        var endIndex = -1
        for (i in afterBrace.indices) {
            when (afterBrace[i]) {
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
        return if (endIndex >= 0) afterBrace.substring(0, endIndex) else ""
    }

    /**
     * Extract the contents of a JSON array that follows a given array key.
     * Returns the text between the outermost brackets, or "" if the key is not present.
     */
    internal fun extractArraySection(
        json: String,
        key: String,
    ): String {
        if (!json.contains("\"$key\"")) return ""
        val afterKey = json.substringAfter("\"$key\"")
        val openBracketIndex = afterKey.indexOf('[')
        if (openBracketIndex == -1) return ""
        val afterBracket = afterKey.substring(openBracketIndex + 1)
        var depth = 1
        var endIndex = -1
        for (i in afterBracket.indices) {
            when (afterBracket[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        endIndex = i
                        break
                    }
                }
            }
        }
        return if (endIndex >= 0) afterBracket.substring(0, endIndex) else ""
    }
}
