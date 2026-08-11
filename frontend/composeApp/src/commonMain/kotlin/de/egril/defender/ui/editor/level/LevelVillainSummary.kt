package de.egril.defender.ui.editor.level

import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.model.AttackerType
import de.egril.defender.model.isRealVillain

internal fun List<EditorEnemySpawn>.presentVillainTypes(): List<AttackerType> =
    asSequence()
        .map { it.attackerType }
        .filter { it.isRealVillain }
        .distinct()
        .toList()

internal fun List<EditorEnemySpawn>.presentVillainSummary(nameProvider: (AttackerType) -> String): String =
    presentVillainTypes().joinToString(", ") { nameProvider(it) }
