package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import de.egril.defender.model.SpawnCondition
import de.egril.defender.model.SpawnGroup
import de.egril.defender.model.SpawnGroupSpawn
import de.egril.defender.model.SpawnGroupTurn
import de.egril.defender.model.SpawnRepeatMode
import de.egril.defender.model.SpawnSequenceEntry
import de.egril.defender.utils.EventMapImageJson
import de.egril.defender.utils.JsonUtils

/**
 * JSON codec for the spawn sequence: spawn groups (repeating/conditional) and plain spawn turns.
 */
internal object EditorSpawnGroupJsonCodec {
    internal fun serializeSpawnSequenceEntry(entry: SpawnSequenceEntry): String =
        when (entry) {
            is SpawnGroupTurn -> serializeSpawnGroupTurn(entry)
            is SpawnGroup -> serializeSpawnGroup(entry)
        }

    internal fun serializeSpawnGroupTurn(turn: SpawnGroupTurn): String {
        val spawnsJson =
            turn.spawns.joinToString(", ") { spawn ->
                val spawnParts = mutableListOf<String>()
                spawnParts.add("\"attackerType\": \"${spawn.attackerType.name}\"")
                spawnParts.add("\"count\": ${spawn.count}")
                spawnParts.add("\"level\": ${spawn.level}")
                if (spawn.spawnPoint != null) {
                    spawnParts.add("\"spawnPoint\": {\"x\": ${spawn.spawnPoint.x}, \"y\": ${spawn.spawnPoint.y}}")
                }
                if (spawn.unitId != null) spawnParts.add("\"unitId\": ${EventMapImageJson.quote(spawn.unitId)}")
                if (spawn.firstIterationOnly) spawnParts.add("\"firstIterationOnly\": true")
                "{${spawnParts.joinToString(", ")}}"
            }
        return "{\"turnOffset\": ${turn.turnOffset}, \"spawns\": [$spawnsJson]}"
    }

    /** Groups write their own fields first and the (possibly nested) entries last. */
    internal fun serializeSpawnGroup(group: SpawnGroup): String {
        val parts = mutableListOf<String>()
        parts.add("\"groupId\": ${EventMapImageJson.quote(group.groupId)}")
        parts.add("\"repeatMode\": \"${group.repeatMode.name}\"")
        if (group.repeatMode == SpawnRepeatMode.COUNT) {
            parts.add("\"repeatCount\": ${group.repeatCount}")
        }
        if (group.condition != null) parts.add("\"condition\": \"${group.condition.name}\"")
        if (group.targetUnitId != null) parts.add("\"targetUnitId\": ${EventMapImageJson.quote(group.targetUnitId)}")
        parts.add("\"entries\": [${group.entries.joinToString(", ") { serializeSpawnSequenceEntry(it) }}]")
        return "{${parts.joinToString(", ")}}"
    }

    /**
     * Parses the top-level spawn sequence. Elements are groups (with `repeatMode`) or plain turns
     * (with `turnOffset`). Groups store their content in `entries`; files written before nested
     * loops use `turns` (turns only), which is still accepted.
     */
    internal fun parseSpawnGroups(dataJson: String): List<SpawnSequenceEntry>? {
        val arrayKey = Regex("\"spawnGroups\"\\s*:\\s*")
        val arrayStart = arrayKey.find(dataJson) ?: return null
        require(dataJson.getOrNull(arrayStart.range.last + 1) == '[') { "spawnGroups must be a JSON array" }
        val groupsSection = extractSpawnGroupSection(dataJson, arrayStart.range.last + 2, '[', ']')
        val groupIds = mutableSetOf<String>()
        return parseSpawnSequence(groupsSection, groupIds)
    }

    internal fun parseSpawnSequence(
        arrayContent: String,
        groupIds: MutableSet<String>,
    ): List<SpawnSequenceEntry> {
        val entries = mutableListOf<SpawnSequenceEntry>()
        val segmentOffsets = mutableSetOf<Int>()
        for (entry in EventMapImageJson.splitArray(arrayContent)) {
            require(entry.startsWith("{") && entry.endsWith("}")) { "Invalid spawn sequence entry" }
            val head = spawnObjectHead(entry)
            if (hasSpawnGroupKey(head, "repeatMode")) {
                segmentOffsets.clear()
                entries.add(parseSpawnGroup(entry, head, groupIds))
            } else {
                val turn = parseSpawnGroupTurn(entry, head)
                require(segmentOffsets.add(turn.turnOffset)) { "Duplicate turnOffset '${turn.turnOffset}'" }
                entries.add(turn)
            }
        }
        return entries
    }

    internal fun parseSpawnGroup(
        entry: String,
        head: String,
        groupIds: MutableSet<String>,
    ): SpawnGroup {
        val groupId = requiredSpawnGroupString(head, "groupId")
        require(groupIds.add(groupId)) { "Duplicate spawn group id '$groupId'" }
        val repeatMode =
            requiredSpawnGroupString(head, "repeatMode").let { value ->
                runCatching { SpawnRepeatMode.valueOf(value) }
                    .getOrElse { throw IllegalArgumentException("Unknown spawn repeat mode '$value'") }
            }
        val repeatCount =
            optionalSpawnGroupInt(head, "repeatCount", 1)
                .also { require(it > 0) { "repeatCount must be positive" } }
        val condition =
            optionalSpawnGroupString(head, "condition")?.let { value ->
                runCatching { SpawnCondition.valueOf(value) }
                    .getOrElse { throw IllegalArgumentException("Unknown spawn condition '$value'") }
            }
        val targetUnitId = optionalSpawnGroupString(head, "targetUnitId")
        if (repeatMode == SpawnRepeatMode.CONDITION) {
            require(condition != null && !targetUnitId.isNullOrBlank()) {
                "CONDITION spawn groups require a condition and targetUnitId"
            }
        }
        val contentKey = if (hasSpawnGroupKey(head, "entries")) "entries" else "turns"
        val entries = parseSpawnSequence(requiredSpawnGroupArray(entry, contentKey), groupIds)
        require(entries.isNotEmpty()) { "Spawn groups must contain at least one entry" }
        return SpawnGroup(groupId, repeatMode, repeatCount, condition, targetUnitId, entries)
    }

    internal fun parseSpawnGroupTurn(
        turnEntry: String,
        head: String,
    ): SpawnGroupTurn {
        val turnOffset =
            requiredSpawnGroupInt(head, "turnOffset")
                .also { require(it > 0) { "turnOffset must be positive" } }
        val spawnsSection = requiredSpawnGroupArray(turnEntry, "spawns")
        val spawns = mutableListOf<SpawnGroupSpawn>()
        for (spawnEntry in EventMapImageJson.splitArray(spawnsSection)) {
            require(spawnEntry.startsWith("{") && spawnEntry.endsWith("}")) { "Invalid spawn entry" }
            val attackerTypeName = requiredSpawnGroupString(spawnEntry, "attackerType")
            val attackerType =
                runCatching { AttackerType.valueOf(attackerTypeName) }
                    .getOrElse { throw IllegalArgumentException("Unknown attacker type '$attackerTypeName'") }
            val count = optionalSpawnGroupInt(spawnEntry, "count", 1)
            val level = optionalSpawnGroupInt(spawnEntry, "level", 1)
            require(count > 0) { "Spawn count must be positive" }
            require(level > 0) { "Spawn level must be positive" }
            val spawnPoint =
                if (hasSpawnGroupKey(spawnEntry, "spawnPoint")) {
                    val section = requiredSpawnGroupObject(spawnEntry, "spawnPoint")
                    val x = requiredSpawnGroupInt(section, "x")
                    val y = requiredSpawnGroupInt(section, "y")
                    require(x >= 0 && y >= 0) { "Spawn point coordinates must be non-negative" }
                    Position(x, y)
                } else {
                    null
                }
            val unitId = optionalSpawnGroupString(spawnEntry, "unitId")
            val firstIterationOnly =
                if (hasSpawnGroupKey(spawnEntry, "firstIterationOnly")) {
                    Regex("\"firstIterationOnly\"\\s*:\\s*(true|false)(?=\\s*[,}])")
                        .find(spawnEntry)
                        ?.groupValues
                        ?.get(1)
                        ?.toBoolean()
                        ?: throw IllegalArgumentException("firstIterationOnly must be a boolean")
                } else {
                    false
                }
            spawns.add(SpawnGroupSpawn(attackerType, count, level, spawnPoint, unitId, firstIterationOnly))
        }
        return SpawnGroupTurn(turnOffset, spawns)
    }

    /**
     * Returns [json] (a single object) with the contents of all nested arrays/objects removed, so
     * key lookups only see the object's own fields and never those of nested entries.
     */
    internal fun spawnObjectHead(json: String): String {
        val result = StringBuilder()
        var depth = 0
        var inString = false
        var escaped = false
        for (char in json) {
            if (inString) {
                if (depth <= 1) result.append(char)
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = false
                }
                continue
            }
            when (char) {
                '"' -> {
                    inString = true
                    if (depth <= 1) result.append(char)
                }
                '{', '[' -> {
                    depth++
                    if (depth <= 2) result.append(char)
                }
                '}', ']' -> {
                    if (depth <= 2) result.append(char)
                    depth--
                }
                else -> if (depth <= 1) result.append(char)
            }
        }
        return result.toString()
    }

    internal fun extractSpawnGroupSection(
        json: String,
        contentStart: Int,
        open: Char,
        close: Char,
    ): String {
        var depth = 1
        var inString = false
        var escaped = false
        var index = contentStart
        while (index < json.length) {
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
                    open -> depth++
                    close -> {
                        depth--
                        if (depth == 0) return json.substring(contentStart, index)
                    }
                }
            }
            index++
        }
        throw IllegalArgumentException("Unterminated JSON section")
    }

    internal fun requiredSpawnGroupArray(
        json: String,
        key: String,
    ): String {
        val keyMatch =
            Regex("\"${Regex.escape(key)}\"\\s*:\\s*\\[")
                .find(json)
                ?: throw IllegalArgumentException("$key must be a JSON array")
        return extractSpawnGroupSection(json, keyMatch.range.last + 1, '[', ']')
    }

    internal fun requiredSpawnGroupObject(
        json: String,
        key: String,
    ): String {
        val keyMatch =
            Regex("\"${Regex.escape(key)}\"\\s*:\\s*\\{")
                .find(json)
                ?: throw IllegalArgumentException("$key must be a JSON object")
        val contentStart = keyMatch.range.last + 1
        return extractSpawnGroupSection(json, contentStart, '{', '}')
    }

    internal fun requiredSpawnGroupString(
        json: String,
        key: String,
    ): String =
        optionalSpawnGroupString(json, key)
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("$key must be a non-blank string")

    internal fun optionalSpawnGroupString(
        json: String,
        key: String,
    ): String? {
        if (!hasSpawnGroupKey(json, key)) return null
        return EventMapImageJson
            .stringValue(json, key)
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("$key must be a non-blank string")
    }

    internal fun requiredSpawnGroupInt(
        json: String,
        key: String,
    ): Int =
        JsonUtils
            .extractNumericValue(json, key)
            .toIntOrNull()
            ?: throw IllegalArgumentException("$key must be an integer")

    internal fun optionalSpawnGroupInt(
        json: String,
        key: String,
        default: Int,
    ): Int =
        if (hasSpawnGroupKey(json, key)) {
            requiredSpawnGroupInt(json, key)
        } else {
            default
        }

    internal fun hasSpawnGroupKey(
        json: String,
        key: String,
    ): Boolean = Regex("\"${Regex.escape(key)}\"\\s*:").containsMatchIn(json)
}
