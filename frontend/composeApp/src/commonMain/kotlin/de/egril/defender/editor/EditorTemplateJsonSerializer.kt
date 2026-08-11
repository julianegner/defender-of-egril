package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.utils.JsonUtils

object EditorTemplateJsonSerializer {
    fun deserializeTemplateIndex(json: String): List<String> {
        val templatesSection = JsonUtils.extractJsonArrayForKey(json, "templates")
        if (templatesSection.isBlank()) return emptyList()
        return JsonUtils
            .splitJsonArray(templatesSection)
            .map { it.removeSurrounding("\"").trim() }
            .filter { it.isNotEmpty() }
    }

    fun deserializeSpawnTurnTemplate(json: String): SpawnTurnTemplateDefinition? {
        val dataJson = JsonUtils.extractDataSection(json)
        val id = JsonUtils.extractStringValue(dataJson, "id")
        val name = JsonUtils.extractStringValue(dataJson, "name")
        if (id.isBlank() || name.isBlank()) return null

        val description = JsonUtils.extractStringValue(dataJson, "description")
        val variantsSection = JsonUtils.extractJsonArrayForKey(dataJson, "variants")
        val variants =
            JsonUtils
                .splitJsonArray(variantsSection)
                .mapNotNull(::deserializeVariant)

        if (variants.isEmpty()) return null
        return SpawnTurnTemplateDefinition(
            id = id,
            name = name,
            description = description,
            variants = variants,
        )
    }

    fun deserializeMapTemplate(json: String): MapTemplateDefinition? {
        val dataJson = JsonUtils.extractDataSection(json)
        val id = JsonUtils.extractStringValue(dataJson, "id")
        val name = JsonUtils.extractStringValue(dataJson, "name")
        if (id.isBlank() || name.isBlank()) return null

        val layoutKindName = JsonUtils.extractStringValue(dataJson, "layoutKind")
        val layoutKind = runCatching { MapTemplateLayoutKind.valueOf(layoutKindName) }.getOrNull()
        if (layoutKind != null) {
            return MapTemplateDefinition(
                id = id,
                name = name,
                layoutKind = layoutKind,
            )
        }

        val templateMap = EditorJsonSerializer.deserializeMap(json) ?: return null
        return MapTemplateDefinition(
            id = id,
            name = name,
            templateMap = templateMap.copy(isOfficial = false),
        )
    }

    private fun deserializeVariant(json: String): SpawnTurnTemplateVariant? {
        val kindName = JsonUtils.extractStringValue(json, "kind")
        val kind = runCatching { EditorEnemyTemplateKind.valueOf(kindName) }.getOrNull() ?: return null
        val entriesSection = JsonUtils.extractJsonArrayForKey(json, "entries")
        val entries =
            JsonUtils
                .splitJsonArray(entriesSection)
                .mapNotNull(::deserializeEntry)

        if (entries.isEmpty()) return null
        return SpawnTurnTemplateVariant(kind = kind, entries = entries)
    }

    private fun deserializeEntry(json: String): SpawnTurnTemplateEntry? {
        val attackerTypeName = JsonUtils.extractStringValue(json, "attackerType")
        val attackerType = runCatching { AttackerType.valueOf(attackerTypeName) }.getOrNull() ?: return null
        val turnOffset = JsonUtils.extractNumericValue(json, "turnOffset").toIntOrNull() ?: return null
        val amount = JsonUtils.extractNumericValue(json, "amount").toIntOrNull() ?: 1
        val levelOffset = JsonUtils.extractNumericValue(json, "levelOffset").toIntOrNull() ?: 0
        return SpawnTurnTemplateEntry(
            attackerType = attackerType,
            turnOffset = turnOffset,
            amount = amount,
            levelOffset = levelOffset,
        )
    }
}
