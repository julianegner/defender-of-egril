package de.egril.defender.utils

/**
 * Simple JSON parsing utilities for manual JSON serialization
 * Shared between SaveJsonSerializer and EditorJsonSerializer
 */
object JsonUtils {
    /**
     * Extract a value for a given key from a JSON string
     * Supports both string and numeric values
     */
    fun extractValue(json: String, key: String): String {
        val pattern = "\"$key\":\\s*\"?([^,\"\\}\\]]+)\"?"
        val regex = Regex(pattern)
        val match = regex.find(json)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }
    
    /**
     * Extract a string value (quoted) for a given key from a JSON string
     */
    fun extractStringValue(json: String, key: String): String {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\""
        val regex = Regex(pattern)
        val match = regex.find(json)
        return match?.groupValues?.get(1) ?: ""
    }
    
    /**
     * Extract a numeric value for a given key from a JSON string
     */
    fun extractNumericValue(json: String, key: String): String {
        val pattern = "\"$key\"\\s*:\\s*([0-9.-]+)"
        val regex = Regex(pattern)
        val match = regex.find(json)
        return match?.groupValues?.get(1) ?: ""
    }
    
    /**
     * Extract a boolean value for a given key from a JSON string
     */
    fun extractBooleanValue(json: String, key: String): Boolean {
        val pattern = "\"$key\"\\s*:\\s*(true|false)"
        val regex = Regex(pattern)
        val match = regex.find(json)
        return match?.groupValues?.get(1) == "true"
    }
    
/**
     * Extract the "data" section from a JSON file with a metadata wrapper.
     * If the JSON has no metadata wrapper (old format), returns the original JSON for backward compatibility.
     */
    fun extractDataSection(json: String): String {
        if (!json.contains("\"metadata\"")) {
            return json  // Old format - no wrapper, return as-is
        }
        val dataKeyIndex = json.indexOf("\"data\"")
        if (dataKeyIndex == -1) return json
        val colonIndex = json.indexOf(":", dataKeyIndex + 6)
        if (colonIndex == -1) return json
        val openBrace = json.indexOf("{", colonIndex + 1)
        if (openBrace == -1) return json
        var depth = 1
        var endIndex = openBrace + 1
        var inString = false
        while (endIndex < json.length && depth > 0) {
            val c = json[endIndex]
            if (inString) {
                if (c == '\\') {
                    endIndex += 2  // skip escape sequence (e.g. \" or \\)
                    continue
                } else if (c == '"') {
                    inString = false
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> depth--
                }
            }
            endIndex++
        }
        return json.substring(openBrace, endIndex)
    }

    /**
     * Split a JSON array content into individual elements
     * Handles nested objects and arrays
     */
    fun splitJsonArray(arrayContent: String): List<String> {
        val elements = mutableListOf<String>()
        var depth = 0
        var currentElement = StringBuilder()
        var inString = false
        
        for (char in arrayContent) {
            when {
                char == '"' && (currentElement.isEmpty() || currentElement.last() != '\\') -> {
                    inString = !inString
                    currentElement.append(char)
                }
                inString -> {
                    currentElement.append(char)
                }
                char == '{' || char == '[' -> {
                    depth++
                    currentElement.append(char)
                }
                char == '}' || char == ']' -> {
                    depth--
                    currentElement.append(char)
                }
                char == ',' && depth == 0 -> {
                    if (currentElement.isNotBlank()) {
                        elements.add(currentElement.toString().trim())
                    }
                    currentElement = StringBuilder()
                }
                else -> {
                    currentElement.append(char)
                }
            }
        }
        
        if (currentElement.isNotBlank()) {
            elements.add(currentElement.toString().trim())
        }
        
        return elements
    }

    fun extractJsonArraySection(json: String, arrayKey: String): String {
        val startIdx = json.indexOf(arrayKey)
        if (startIdx == -1) return ""
        var pos = startIdx + arrayKey.length
        var openBrackets = 1
        val sb = StringBuilder()
        while (pos < json.length && openBrackets > 0) {
            val c = json[pos]
            if (c == '[') openBrackets++
            if (c == ']') openBrackets--
            if (openBrackets > 0) sb.append(c)
            pos++
        }
        return sb.toString()
    }
}
