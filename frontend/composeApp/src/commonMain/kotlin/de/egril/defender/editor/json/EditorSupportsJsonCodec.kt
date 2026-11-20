package de.egril.defender.editor

import de.egril.defender.editor.EditorJsonScanner.extractArraySection
import de.egril.defender.editor.EditorJsonScanner.extractObjectSection
import de.egril.defender.editor.EditorJsonScanner.splitJsonArrayObjects
import de.egril.defender.model.CooldownPower
import de.egril.defender.model.CooldownPowerType
import de.egril.defender.model.INDEFINITE_SUPPORT_COUNT
import de.egril.defender.model.LevelSupports
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObject
import de.egril.defender.model.SupportObjectType
import de.egril.defender.model.SupportSpell
import de.egril.defender.model.isIndefiniteSupportCount
import de.egril.defender.utils.JsonUtils

/**
 * JSON codec for the level "supports" section (support objects, spells, cooldown powers and fiefs).
 */
internal object EditorSupportsJsonCodec {
    /**
     * Parse the optional "supports" section (player-usable objects + spell tokens) from a level's data JSON.
     */
    fun parseSupports(dataJson: String): LevelSupports {
        val supportsSection = extractObjectSection(dataJson, "supports")
        if (supportsSection.isBlank()) return LevelSupports()

        val objects = mutableListOf<SupportObject>()
        val objectsSection = extractArraySection(supportsSection, "objects")
        if (objectsSection.isNotBlank()) {
            for (entry in splitJsonArrayObjects(objectsSection)) {
                val typeName = runCatching { JsonUtils.extractValue(entry, "type") }.getOrNull() ?: continue
                val type = runCatching { SupportObjectType.valueOf(typeName) }.getOrNull() ?: continue
                val count = parseSupportCount(runCatching { JsonUtils.extractValue(entry, "count") }.getOrDefault(""))
                val damage = runCatching { JsonUtils.extractValue(entry, "damage").toInt() }.getOrDefault(10)
                val healthPoints = runCatching { JsonUtils.extractValue(entry, "healthPoints").toInt() }.getOrDefault(50)
                objects.add(SupportObject(type = type, count = count, damage = damage, healthPoints = healthPoints))
            }
        }

        val spells = mutableListOf<SupportSpell>()
        val spellsSection = extractArraySection(supportsSection, "spells")
        if (spellsSection.isNotBlank()) {
            for (entry in splitJsonArrayObjects(spellsSection)) {
                val spellName = runCatching { JsonUtils.extractValue(entry, "spell") }.getOrNull() ?: continue
                val spell = runCatching { SpellType.valueOf(spellName) }.getOrNull() ?: continue
                val count = parseSupportCount(runCatching { JsonUtils.extractValue(entry, "count") }.getOrDefault(""))
                spells.add(SupportSpell(spell = spell, count = count))
            }
        }

        val cooldownPowers = mutableListOf<CooldownPower>()
        val cooldownSection = extractArraySection(supportsSection, "cooldownPowers")
        if (cooldownSection.isNotBlank()) {
            for (entry in splitJsonArrayObjects(cooldownSection)) {
                val typeName = runCatching { JsonUtils.extractValue(entry, "type") }.getOrNull() ?: continue
                val type = runCatching { CooldownPowerType.valueOf(typeName) }.getOrNull() ?: continue
                val cooldownTurns =
                    runCatching { JsonUtils.extractValue(entry, "cooldownTurns").toInt() }
                        .getOrDefault(type.defaultCooldown)
                val startActive =
                    runCatching { JsonUtils.extractValue(entry, "startActive").toBoolean() }
                        .getOrDefault(true)
                cooldownPowers.add(
                    CooldownPower(type = type, cooldownTurns = cooldownTurns, startActive = startActive),
                )
            }
        }

        val fiefs = mutableListOf<de.egril.defender.model.SupportFief>()
        val fiefsSection = extractArraySection(supportsSection, "fiefs")
        if (fiefsSection.isNotBlank()) {
            for (entry in splitJsonArrayObjects(fiefsSection)) {
                val typeName = runCatching { JsonUtils.extractValue(entry, "type") }.getOrNull() ?: continue
                val type =
                    runCatching {
                        de.egril.defender.model.FiefType
                            .valueOf(typeName)
                    }.getOrNull() ?: continue
                val count = parseSupportCount(runCatching { JsonUtils.extractValue(entry, "count") }.getOrDefault(""))
                fiefs.add(
                    de.egril.defender.model
                        .SupportFief(type = type, count = count),
                )
            }
        }

        return LevelSupports(objects = objects, spells = spells, cooldownPowers = cooldownPowers, fiefs = fiefs)
    }

    internal fun serializeSupportCount(count: Int): String =
        if (isIndefiniteSupportCount(count)) {
            "\"INDEFINITELY\""
        } else {
            "$count"
        }

    internal fun parseSupportCount(rawCount: String): Int =
        if (rawCount == "INDEFINITELY") {
            INDEFINITE_SUPPORT_COUNT
        } else {
            rawCount.toIntOrNull() ?: 1
        }
}
