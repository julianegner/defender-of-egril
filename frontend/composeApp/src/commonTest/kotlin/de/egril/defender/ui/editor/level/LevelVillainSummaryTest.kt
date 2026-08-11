package de.egril.defender.ui.editor.level

import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorLevel
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
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

    @Test
    fun levelsUsingVillainReturnsOnlyLevelsContainingThatVillain() {
        val silasLevel =
            testLevel(
                id = "silas_level",
                title = "Silas Level",
                spawns =
                    listOf(
                        EditorEnemySpawn(attackerType = AttackerType.SILAS_THE_MASKMASTER, spawnTurn = 2),
                    ),
            )
        val araxxaLevel =
            testLevel(
                id = "araxxa_level",
                title = "Araxxa Level",
                spawns =
                    listOf(
                        EditorEnemySpawn(attackerType = AttackerType.ARAXXA, spawnTurn = 3),
                    ),
            )

        assertEquals(
            listOf(silasLevel),
            listOf(silasLevel, araxxaLevel).levelsUsingVillain(AttackerType.SILAS_THE_MASKMASTER),
        )
    }

    @Test
    fun villainUsageEntriesIncludesUnusedVillains() {
        val levels =
            listOf(
                testLevel(
                    id = "ewhad_level",
                    title = "Ewhad Level",
                    spawns = listOf(EditorEnemySpawn(attackerType = AttackerType.EWHAD, spawnTurn = 1)),
                ),
            )

        val entries = villainUsageEntries(levels)

        assertEquals(listOf("ewhad_level"), entries.first { it.villainType == AttackerType.EWHAD }.levels.map { it.id })
        assertEquals(emptyList(), entries.first { it.villainType == AttackerType.THE_KRAKEN }.levels)
    }

    private fun testLevel(
        id: String,
        title: String,
        spawns: List<EditorEnemySpawn>,
    ): EditorLevel =
        EditorLevel(
            id = id,
            mapId = "map",
            title = title,
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = spawns,
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
        )
}
