package de.egril.defender.ui.editor.level

import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorLevel
import de.egril.defender.model.AttackerType
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

internal fun List<EditorEnemySpawn>.presentVillainSummary(nameProvider: (AttackerType) -> String): String =
    presentVillainTypes().joinToString(", ") { nameProvider(it) }

internal fun List<EditorLevel>.levelsUsingVillain(villainType: AttackerType): List<EditorLevel> =
    filter { level -> villainType in level.enemySpawns.presentVillainTypes() }

internal fun villainUsageEntries(levels: List<EditorLevel>): List<VillainUsageEntry> =
    AttackerType.entries
        .filter { it.isRealVillain }
        .map { villainType ->
            VillainUsageEntry(
                villainType = villainType,
                levels = levels.levelsUsingVillain(villainType),
            )
        }
