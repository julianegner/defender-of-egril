package de.egril.defender.ui.editor.level

import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorLevel
import de.egril.defender.model.AttackerType
import de.egril.defender.model.arrivalCompanions
import de.egril.defender.model.isRealVillain

internal data class VillainUsageEntry(
    val villainType: AttackerType,
    val levels: List<EditorLevel>,
)

internal fun List<EditorEnemySpawn>.presentVillainTypes(): List<AttackerType> =
    asSequence()
        .map { it.attackerType }
        .filter { it.isRealVillain }
        .distinct()
        .toList()

internal fun List<EditorEnemySpawn>.presentVillainSummary(nameProvider: (AttackerType) -> String): String = presentVillainTypes().joinToString(", ") { nameProvider(it) }

internal fun List<EditorLevel>.levelsUsingVillain(villainType: AttackerType): List<EditorLevel> = filter { level -> villainType in level.configuredSpawnEntries().presentVillainTypes() }

internal fun villainUsageEntries(
    levels: List<EditorLevel>,
    onlyUnusedVillains: Boolean = false,
): List<VillainUsageEntry> {
    val entries =
        AttackerType.entries
            .filter { it.isRealVillain }
            .map { villainType ->
                VillainUsageEntry(
                    villainType = villainType,
                    levels = levels.levelsUsingVillain(villainType),
                )
            }
    if (!onlyUnusedVillains) return entries
    val usedVillains = entries.filter { it.levels.isNotEmpty() }.map { it.villainType }.toSet()
    // Companions such as Haga and Zussa count as used while their summoner (Sybilla) is used.
    return entries.filter { entry ->
        entry.levels.isEmpty() &&
            AttackerType.entries.none { parent -> parent in usedVillains && entry.villainType in parent.arrivalCompanions }
    }
}
