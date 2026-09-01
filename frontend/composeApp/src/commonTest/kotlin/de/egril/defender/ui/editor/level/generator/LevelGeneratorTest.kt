package de.egril.defender.ui.editor.level.generator

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.AttackerType
import de.egril.defender.model.EnemyFaction
import de.egril.defender.model.isRealVillain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LevelGeneratorTest {
    private fun existingMap(): EditorMap =
        EditorMap(
            id = "test_map",
            name = "Test Map",
            width = 6,
            height = 3,
            tiles =
                mapOf(
                    "0,1" to TileType.SPAWN_POINT,
                    "1,1" to TileType.PATH,
                    "2,1" to TileType.PATH,
                    "3,1" to TileType.PATH,
                    "4,1" to TileType.PATH,
                    "5,1" to TileType.TARGET,
                    "2,0" to TileType.BUILD_AREA,
                ),
            readyToUse = true,
        )

    @Test
    fun generatesLevelOnExistingMapWithoutGeneratingANewMap() {
        val map = existingMap()
        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "My Generated Level",
                    mapSource = GeneratorMapSource.EXISTING_MAP,
                    existingMap = map,
                    seed = 42,
                ),
            )

        assertNull(result.generatedMap)
        assertEquals(map.id, result.level.mapId)
        assertEquals("My Generated Level", result.level.title)
        assertTrue(result.level.id.startsWith("my_generated_level_"))
        assertTrue(result.level.enemySpawns.isNotEmpty())
        assertTrue(result.level.enemySpawns.all { it.spawnTurn >= 1 })
        assertTrue(result.level.availableTowers.isNotEmpty())
    }

    @Test
    fun generatesMapWithRequestedSize() {
        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Gigantic Level",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    mapSize = GeneratedMapSize.GIGANTIC,
                    seed = 7,
                ),
            )

        val generatedMap = assertNotNull(result.generatedMap)
        assertEquals(GeneratedMapSize.GIGANTIC.width, generatedMap.width)
        assertEquals(GeneratedMapSize.GIGANTIC.height, generatedMap.height)
        assertEquals(generatedMap.id, result.level.mapId)
        assertTrue(generatedMap.readyToUse)
        assertTrue(result.level.enemySpawns.all { it.spawnPoint in generatedMap.getSpawnPoints() })
    }

    @Test
    fun higherDifficultyProducesMoreEnemiesAndFewerResources() {
        fun generate(difficulty: GeneratorDifficulty) =
            LevelGenerator
                .generate(
                    LevelGeneratorConfig(
                        title = "Difficulty Test",
                        difficulty = difficulty,
                        mapSource = GeneratorMapSource.EXISTING_MAP,
                        existingMap = existingMap(),
                        seed = 1,
                    ),
                ).level

        val easy = generate(GeneratorDifficulty.EASY)
        val nightmare = generate(GeneratorDifficulty.NIGHTMARE)

        assertTrue(nightmare.enemySpawns.size > easy.enemySpawns.size)
        assertTrue(nightmare.startCoins < easy.startCoins)
        assertTrue(nightmare.startHealthPoints < easy.startHealthPoints)
    }

    @Test
    fun selectedVillainsAreSpawnedAndDefineTheMainEnemyType() {
        val villain = AttackerType.PRINCE_VALERIUS_THE_SOULREAPER
        assertTrue(villain.isRealVillain)
        assertEquals(EnemyFaction.UNDEAD, villain.faction)

        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Undead Level",
                    villains = setOf(villain),
                    mapSource = GeneratorMapSource.EXISTING_MAP,
                    existingMap = existingMap(),
                    seed = 5,
                ),
            )

        val spawnedTypes = result.level.enemySpawns.map { it.attackerType }
        assertTrue(villain in spawnedTypes)
        assertTrue(spawnedTypes.filter { !it.isRealVillain }.all { it.faction == EnemyFaction.UNDEAD })
    }

    @Test
    fun villainIsSpawnedOnlyOncePerLevel() {
        val villains = setOf(AttackerType.GAROKK, AttackerType.PRINCE_VALERIUS_THE_SOULREAPER)
        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Two Villains",
                    villains = villains,
                    mapSource = GeneratorMapSource.EXISTING_MAP,
                    existingMap = existingMap(),
                    seed = 11,
                ),
            )

        villains.forEach { villain ->
            assertEquals(1, result.level.enemySpawns.count { it.attackerType == villain })
        }
    }

    @Test
    fun minionPoolFallsBackToAllFactionsWithoutVillains() {
        val pool = LevelGenerator.minionPoolFor(emptyList())

        assertTrue(pool.isNotEmpty())
        assertTrue(pool.none { it.isRealVillain })
        assertTrue(pool.any { it.faction == EnemyFaction.HORDE })
        assertTrue(pool.any { it.faction == EnemyFaction.UNDEAD })
    }
}
