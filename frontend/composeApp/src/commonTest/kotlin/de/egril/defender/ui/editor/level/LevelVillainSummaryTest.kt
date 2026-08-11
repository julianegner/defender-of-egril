package de.egril.defender.ui.editor.level

import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.model.AttackerType
import kotlin.test.Test
import kotlin.test.assertEquals

class LevelVillainSummaryTest {
    @Test
    fun presentVillainTypesReturnsDistinctRealVillainsInSpawnOrder() {
        val spawns =
            listOf(
                EditorEnemySpawn(attackerType = AttackerType.GOBLIN, spawnTurn = 1),
                EditorEnemySpawn(attackerType = AttackerType.SILAS_THE_MASKMASTER, spawnTurn = 2),
                EditorEnemySpawn(attackerType = AttackerType.SILAS_THE_MASKMASTER, spawnTurn = 3),
                EditorEnemySpawn(attackerType = AttackerType.SILAS_MIRROR_IMAGE, spawnTurn = 3),
                EditorEnemySpawn(attackerType = AttackerType.ARAXXA, spawnTurn = 4),
            )

        assertEquals(
            listOf(AttackerType.SILAS_THE_MASKMASTER, AttackerType.ARAXXA),
            spawns.presentVillainTypes(),
        )
    }

    @Test
    fun presentVillainSummaryUsesProvidedNames() {
        val spawns =
            listOf(
                EditorEnemySpawn(attackerType = AttackerType.FALLEN_SHIELDMAIDEN_FREYA, spawnTurn = 1),
                EditorEnemySpawn(attackerType = AttackerType.GOBLIN, spawnTurn = 1),
                EditorEnemySpawn(attackerType = AttackerType.CAPTAIN_RODERICH, spawnTurn = 2),
            )

        assertEquals(
            "Freya, Roderich",
            spawns.presentVillainSummary { it.villainName ?: it.displayName },
        )
    }
}
