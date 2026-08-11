package de.egril.defender.editor

import de.egril.defender.model.AttackerType

enum class EditorEnemyTemplateKind {
    MIXED,
    HORDE,
    UNDEAD,
    DARK_MAGIC,
    DEMONIC,
    PIRATES,
    VILLAINS,
}

data class SpawnTurnTemplateEntry(
    val attackerType: AttackerType,
    val turnOffset: Int,
    val amount: Int = 1,
    val levelOffset: Int = 0,
)

data class SpawnTurnTemplateVariant(
    val kind: EditorEnemyTemplateKind,
    val entries: List<SpawnTurnTemplateEntry>,
)

data class SpawnTurnTemplateDefinition(
    val id: String,
    val name: String,
    val description: String,
    val variants: List<SpawnTurnTemplateVariant>,
) {
    fun supportedKinds(): List<EditorEnemyTemplateKind> = variants.map { it.kind }

    fun variantFor(kind: EditorEnemyTemplateKind): SpawnTurnTemplateVariant? =
        variants.firstOrNull { it.kind == kind }
            ?: variants.firstOrNull { it.kind == EditorEnemyTemplateKind.MIXED }
            ?: variants.firstOrNull()
}

data class MapTemplateDefinition(
    val id: String,
    val name: String,
    val layoutKind: MapTemplateLayoutKind? = null,
    val templateMap: EditorMap? = null,
)

enum class MapTemplateLayoutKind {
    STRAIGHT_APPROACH,
    SPLIT_LANES,
    RIVER_CROSSING,
    SPIRAL_SIEGE,
}
