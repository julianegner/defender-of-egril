package de.egril.defender.editor

import de.egril.defender.editor.EditorJsonScanner.extractObjectSection
import de.egril.defender.model.AttackerType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.EventLoop
import de.egril.defender.model.EventLoopStep
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.Position
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObjectType
import de.egril.defender.utils.EventMapImageJson
import de.egril.defender.utils.JsonUtils

/**
 * JSON codec for scripted level events, their conditions, actions and loops.
 */
internal object EditorEventJsonCodec {
    /**
     * Serialize a single [LevelEvent] to a compact single-line JSON object.
     */
    internal fun serializeEvent(event: LevelEvent): String {
        val c = event.condition
        val conditionParts = mutableListOf<String>()
        conditionParts.add("\"type\": \"${c.type.name}\"")
        conditionParts.add("\"fromTurn\": ${c.fromTurn}")
        conditionParts.add("\"threshold\": ${c.threshold}")
        if (c.attackerType != null) conditionParts.add("\"attackerType\": \"${c.attackerType.name}\"")
        if (c.position != null) conditionParts.add("\"position\": {\"x\": ${c.position.x}, \"y\": ${c.position.y}}")
        val conditionJson = "{${conditionParts.joinToString(", ")}}"

        val actionsJson = serializeActions(event.actions)

        val messageJson =
            if (event.messageKey != null) ", \"messageKey\": \"${event.messageKey}\"" else ""
        // Only non-standard frames are written so existing level files stay unchanged.
        val messageFrameJson =
            if (event.messageFrame != null) ", \"messageFrame\": \"${event.messageFrame}\"" else ""
        // The loop is written last so the event's own fields can be parsed from the text before it.
        val loopJson = event.loop?.let { ", \"loop\": ${serializeLoop(it)}" } ?: ""
        // Upcoming-list fields are only written when enabled so existing level files stay unchanged.
        val upcomingJson =
            if (event.showInUpcomingList) {
                val symbolJson = event.upcomingSymbol?.let { ", \"upcomingSymbol\": \"$it\"" } ?: ""
                val nameJson = event.upcomingName?.let { ", \"upcomingName\": \"$it\"" } ?: ""
                ", \"showInUpcomingList\": true$symbolJson$nameJson"
            } else {
                ""
            }
        return "{\"id\": \"${event.id}\", \"condition\": $conditionJson, " +
            "\"actions\": [$actionsJson]$messageJson$messageFrameJson, \"repeatable\": ${event.repeatable}$upcomingJson$loopJson}"
    }

    internal fun serializeActions(actions: List<EventAction>): String =
        actions.joinToString(", ") { action ->
            val parts = mutableListOf<String>()
            parts.add("\"type\": \"${action.type.name}\"")
            parts.add("\"amount\": ${action.amount}")
            if (action.supportObjectType != null) parts.add("\"supportObjectType\": \"${action.supportObjectType.name}\"")
            if (action.spellType != null) parts.add("\"spellType\": \"${action.spellType.name}\"")
            if (action.position != null) parts.add("\"position\": {\"x\": ${action.position.x}, \"y\": ${action.position.y}}")
            if (action.zoneId != null) parts.add("\"zoneId\": \"${action.zoneId}\"")
            if (action.targetEventId != null) parts.add("\"targetEventId\": \"${action.targetEventId}\"")
            if (action.mapImage != null) parts.add("\"mapImage\": ${EventMapImageJson.serialize(action.mapImage)}")
            if (action.imageId != null) parts.add("\"imageId\": ${EventMapImageJson.quote(action.imageId)}")
            if (action.spawnLoopId != null) parts.add("\"spawnLoopId\": ${EventMapImageJson.quote(action.spawnLoopId)}")
            if (action.linkId != null) parts.add("\"linkId\": ${EventMapImageJson.quote(action.linkId)}")
            if (action.altarFrom != null) parts.add("\"altarFrom\": {\"x\": ${action.altarFrom.x}, \"y\": ${action.altarFrom.y}}")
            if (action.altarTo != null) parts.add("\"altarTo\": {\"x\": ${action.altarTo.x}, \"y\": ${action.altarTo.y}}")
            if (action.moveDx != 0) parts.add("\"moveDx\": ${action.moveDx}")
            if (action.moveDy != 0) parts.add("\"moveDy\": ${action.moveDy}")
            if (action.rotationSteps != 0) parts.add("\"rotationSteps\": ${action.rotationSteps}")
            "{${parts.joinToString(", ")}}"
        }

    /** Serialize an [EventLoop]; each step's nested loop is written last within the step. */
    internal fun serializeLoop(loop: EventLoop): String {
        val stepsJson =
            loop.steps.joinToString(", ") { step ->
                val messageJson = if (step.messageKey != null) ", \"messageKey\": \"${step.messageKey}\"" else ""
                val messageFrameJson =
                    if (step.messageFrame != null) ", \"messageFrame\": \"${step.messageFrame}\"" else ""
                val nestedJson = step.nestedLoop?.let { ", \"nestedLoop\": ${serializeLoop(it)}" } ?: ""
                "{\"waitTurns\": ${step.waitTurns}, \"actions\": [${serializeActions(step.actions)}]" +
                    "$messageJson$messageFrameJson$nestedJson}"
            }
        return "{\"repeatCount\": ${loop.repeatCount}, \"steps\": [$stepsJson]}"
    }

    internal fun parseActions(actionsSection: String): List<EventAction> {
        val actions = mutableListOf<EventAction>()
        if (actionsSection.isBlank()) return actions
        for (actionEntry in EventMapImageJson.splitArray(actionsSection)) {
            val actionTypeName = runCatching { JsonUtils.extractValue(actionEntry, "type") }.getOrNull() ?: continue
            val actionType = runCatching { EventActionType.valueOf(actionTypeName) }.getOrNull() ?: continue
            val amount = runCatching { JsonUtils.extractValue(actionEntry, "amount").toInt() }.getOrDefault(0)
            val supportObjectType =
                if (actionEntry.contains("\"supportObjectType\"")) {
                    runCatching { SupportObjectType.valueOf(JsonUtils.extractValue(actionEntry, "supportObjectType")) }.getOrNull()
                } else {
                    null
                }
            val spellType =
                if (actionEntry.contains("\"spellType\"")) {
                    runCatching { SpellType.valueOf(JsonUtils.extractValue(actionEntry, "spellType")) }.getOrNull()
                } else {
                    null
                }
            val zoneId = JsonUtils.extractStringValue(actionEntry, "zoneId").takeIf { it.isNotBlank() }
            val targetEventId = JsonUtils.extractStringValue(actionEntry, "targetEventId").takeIf { it.isNotBlank() }
            actions.add(
                EventAction(
                    type = actionType,
                    amount = amount,
                    supportObjectType = supportObjectType,
                    spellType = spellType,
                    position = parsePositionField(actionEntry, "position"),
                    zoneId = zoneId,
                    targetEventId = targetEventId,
                    mapImage = EventMapImageJson.deserialize(JsonUtils.extractJsonObjectForKey(actionEntry, "mapImage")),
                    imageId = EventMapImageJson.stringValue(actionEntry, "imageId")?.takeIf { it.isNotBlank() },
                    spawnLoopId = EventMapImageJson.stringValue(actionEntry, "spawnLoopId")?.takeIf { it.isNotBlank() },
                    linkId = EventMapImageJson.stringValue(actionEntry, "linkId")?.takeIf { it.isNotBlank() },
                    altarFrom = parsePositionField(actionEntry, "altarFrom"),
                    altarTo = parsePositionField(actionEntry, "altarTo"),
                    moveDx = JsonUtils.extractNumericValue(actionEntry, "moveDx").toIntOrNull() ?: 0,
                    moveDy = JsonUtils.extractNumericValue(actionEntry, "moveDy").toIntOrNull() ?: 0,
                    rotationSteps = JsonUtils.extractNumericValue(actionEntry, "rotationSteps").toIntOrNull() ?: 0,
                ),
            )
        }
        return actions
    }

    /** Parse the body (without outer braces) of a serialized [EventLoop]. */
    internal fun parseLoop(loopBody: String): EventLoop {
        val head = loopBody.substringBefore("\"steps\"")
        val repeatCount = JsonUtils.extractNumericValue(head, "repeatCount").toIntOrNull() ?: 0
        val stepsSection = JsonUtils.extractJsonArrayForKey(loopBody, "steps")
        val steps =
            EventMapImageJson.splitArray(stepsSection).map { stepJson ->
                // Only look at the step's own fields, not those of its nested loop (written last).
                val stepHead = stepJson.substringBefore("\"nestedLoop\"")
                val waitTurns = JsonUtils.extractNumericValue(stepHead, "waitTurns").toIntOrNull()?.coerceAtLeast(0) ?: 1
                val messageKey = JsonUtils.extractStringValue(stepHead, "messageKey").takeIf { it.isNotBlank() }
                val nestedLoop =
                    if (stepJson.contains("\"nestedLoop\"")) {
                        parseLoop(JsonUtils.extractJsonObjectForKey(stepJson, "nestedLoop"))
                    } else {
                        null
                    }
                EventLoopStep(
                    waitTurns = waitTurns,
                    actions = parseActions(JsonUtils.extractJsonArrayForKey(stepHead, "actions")),
                    messageKey = messageKey,
                    messageFrame = JsonUtils.extractStringValue(stepHead, "messageFrame").takeIf { it.isNotBlank() },
                    nestedLoop = nestedLoop,
                )
            }
        return EventLoop(steps = steps, repeatCount = repeatCount.coerceAtLeast(0))
    }

    /**
     * Parse a position object embedded in [entry] under [key] (e.g. {"x": 1, "y": 2}).
     */
    internal fun parsePositionField(
        entry: String,
        key: String,
    ): Position? {
        if (!entry.contains("\"$key\"")) return null
        return runCatching {
            val section = entry.substringAfter("\"$key\"").substringAfter('{').substringBefore('}')
            val x = JsonUtils.extractValue("{$section}", "x").toInt()
            val y = JsonUtils.extractValue("{$section}", "y").toInt()
            Position(x, y)
        }.getOrNull()
    }

    /**
     * Parse the optional "events" section (scripted level events) from a level's data JSON.
     */
    internal fun parseEvents(dataJson: String): LevelEvents {
        val eventsSection = JsonUtils.extractJsonArrayForKey(dataJson, "events")
        if (eventsSection.isBlank()) {
            return LevelEvents()
        }

        val events = mutableListOf<LevelEvent>()
        for (fullEntry in EventMapImageJson.splitArray(eventsSection)) {
            // The loop is serialized last; parse the event's own fields only from the text before it.
            val entry = fullEntry.substringBefore("\"loop\"")
            val id = runCatching { JsonUtils.extractValue(entry, "id") }.getOrNull() ?: continue

            val conditionSection = extractObjectSection(entry, "condition")
            val condTypeName = runCatching { JsonUtils.extractValue(conditionSection, "type") }.getOrNull() ?: continue
            val condType =
                runCatching {
                    EventConditionType
                        .valueOf(condTypeName)
                }.getOrNull() ?: continue
            val fromTurn = runCatching { JsonUtils.extractValue(conditionSection, "fromTurn").toInt() }.getOrDefault(0)
            val threshold = runCatching { JsonUtils.extractValue(conditionSection, "threshold").toInt() }.getOrDefault(0)
            val condAttacker =
                if (conditionSection.contains("\"attackerType\"")) {
                    runCatching { AttackerType.valueOf(JsonUtils.extractValue(conditionSection, "attackerType")) }.getOrNull()
                } else {
                    null
                }
            val condPosition = parsePositionField(conditionSection, "position")
            val condition =
                EventCondition(
                    type = condType,
                    fromTurn = fromTurn,
                    threshold = threshold,
                    attackerType = condAttacker,
                    position = condPosition,
                )

            val actions = parseActions(JsonUtils.extractJsonArrayForKey(entry, "actions"))

            val messageKey =
                if (entry.contains("\"messageKey\"")) {
                    runCatching { JsonUtils.extractValue(entry, "messageKey") }.getOrNull()?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            // Absent frame (levels authored before frames were selectable) means the standard frame.
            val messageFrame = JsonUtils.extractStringValue(entry, "messageFrame").takeIf { it.isNotBlank() }
            val repeatable = runCatching { JsonUtils.extractValue(entry, "repeatable").toBoolean() }.getOrDefault(false)
            val showInUpcomingList =
                runCatching { JsonUtils.extractValue(entry, "showInUpcomingList").toBoolean() }.getOrDefault(false)
            val upcomingSymbol = JsonUtils.extractStringValue(entry, "upcomingSymbol").takeIf { it.isNotBlank() }
            val upcomingName = JsonUtils.extractStringValue(entry, "upcomingName").takeIf { it.isNotBlank() }
            val loop =
                if (fullEntry.contains("\"loop\"")) {
                    runCatching { parseLoop(JsonUtils.extractJsonObjectForKey(fullEntry, "loop")) }.getOrNull()
                } else {
                    null
                }

            events.add(
                LevelEvent(
                    id = id,
                    condition = condition,
                    actions = actions,
                    messageKey = messageKey,
                    messageFrame = messageFrame,
                    repeatable = repeatable,
                    loop = loop,
                    showInUpcomingList = showInUpcomingList,
                    upcomingSymbol = upcomingSymbol,
                    upcomingName = upcomingName,
                ),
            )
        }
        return LevelEvents(events = events)
    }
}
