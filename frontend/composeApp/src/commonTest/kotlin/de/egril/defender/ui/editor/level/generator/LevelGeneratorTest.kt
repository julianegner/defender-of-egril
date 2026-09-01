package de.egril.defender.ui.editor.level.generator

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.AttackerType
import de.egril.defender.model.EnemyFaction
import de.egril.defender.model.isRealVillain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
                    primaryRoster = rostersForVillains(setOf(villain)).first,
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
    fun minionPoolUsesSelectedRosters() {
        val primaryOnly = LevelGenerator.minionPoolFor(GeneratorEnemyRoster.UNDEAD)
        assertEquals(GeneratorEnemyRoster.UNDEAD.types, primaryOnly)

        val withSecondary =
            LevelGenerator.minionPoolFor(GeneratorEnemyRoster.UNDEAD, GeneratorEnemyRoster.DEMONS)
        assertEquals(GeneratorEnemyRoster.UNDEAD.types + GeneratorEnemyRoster.DEMONS.types, withSecondary)
    }

    @Test
    fun levelWithoutVillainsOnlyUsesTheSelectedRosters() {
        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Roster Level",
                    villains = emptySet(),
                    primaryRoster = GeneratorEnemyRoster.WITCHES,
                    secondaryRoster = GeneratorEnemyRoster.PIRATES,
                    mapSource = GeneratorMapSource.EXISTING_MAP,
                    existingMap = existingMap(),
                    seed = 3,
                ),
            )

        val allowed = (GeneratorEnemyRoster.WITCHES.types + GeneratorEnemyRoster.PIRATES.types).toSet()
        assertTrue(result.level.enemySpawns.isNotEmpty())
        assertTrue(result.level.enemySpawns.all { it.attackerType in allowed })
    }

    @Test
    fun selectedRostersAreUsedEvenWithVillains() {
        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Roster Wins Over Villain",
                    villains = setOf(AttackerType.GAROKK),
                    primaryRoster = GeneratorEnemyRoster.PIRATES,
                    mapSource = GeneratorMapSource.EXISTING_MAP,
                    existingMap = existingMap(),
                    seed = 13,
                ),
            )

        val minions =
            result.level.enemySpawns
                .map { it.attackerType }
                .filter { !it.isRealVillain }
        assertTrue(minions.isNotEmpty())
        assertTrue(minions.all { it in GeneratorEnemyRoster.PIRATES.types })
    }

    @Test
    fun rostersArePresetFromTheSelectedVillains() {
        assertEquals(GeneratorEnemyRoster.HORDE to null, rostersForVillains(setOf(AttackerType.GAROKK)))
        assertEquals(GeneratorEnemyRoster.SPIDERS to null, rostersForVillains(setOf(AttackerType.ARAXXA)))
        assertEquals(
            GeneratorEnemyRoster.SPIDERS to GeneratorEnemyRoster.PIRATES,
            rostersForVillains(listOf(AttackerType.ARAXXA, AttackerType.CAPTAIN_RODERICH)),
        )
        // Without villains the default roster is kept.
        assertEquals(GeneratorEnemyRoster.HORDE to null, rostersForVillains(emptySet()))
    }

    @Test
    fun villainsSpawnAfterTheFirstThirdOfTheWavesAndNotAtTheEnd() {
        val difficulty = GeneratorDifficulty.MEDIUM
        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Early Villain",
                    difficulty = difficulty,
                    villains = setOf(AttackerType.GAROKK),
                    mapSource = GeneratorMapSource.EXISTING_MAP,
                    existingMap = existingMap(),
                    seed = 17,
                ),
            )

        val lastTurn = result.level.enemySpawns.maxOf { it.spawnTurn }
        val villainTurn =
            result.level.enemySpawns
                .first { it.attackerType == AttackerType.GAROKK }
                .spawnTurn
        // End of the first third of the waves, and well before the final wave.
        assertEquals(((difficulty.waveCount + 2) / 3) * 2 - 1, villainTurn)
        assertTrue(villainTurn < lastTurn)
    }

    @Test
    fun everySpawnTurnHasEnemies() {
        GeneratorDifficulty.entries.forEach { difficulty ->
            val result =
                LevelGenerator.generate(
                    LevelGeneratorConfig(
                        title = "Dense Waves",
                        difficulty = difficulty,
                        mapSource = GeneratorMapSource.EXISTING_MAP,
                        existingMap = existingMap(),
                        seed = 23,
                    ),
                )

            val turns =
                result.level.enemySpawns
                    .map { it.spawnTurn }
                    .distinct()
                    .sorted()
            assertEquals(1, turns.first())
            assertEquals((1..turns.last()).toList(), turns)
        }
    }

    @Test
    fun villainsUseTheirThemedRosterInsteadOfARandomMix() {
        assertEquals(GeneratorEnemyRoster.SPIDERS, AttackerType.ARAXXA.generatorRoster())
        assertEquals(GeneratorEnemyRoster.PIRATES, AttackerType.CAPTAIN_RODERICH.generatorRoster())
    }

    @Test
    fun spiderRosterContainsOnlySpiders() {
        assertEquals(listOf(AttackerType.SPIDERLING), GeneratorEnemyRoster.SPIDERS.types)

        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Spider Waves",
                    villains = setOf(AttackerType.ARAXXA),
                    primaryRoster = GeneratorEnemyRoster.SPIDERS,
                    mapSource = GeneratorMapSource.EXISTING_MAP,
                    existingMap = existingMap(),
                    seed = 5,
                ),
            )

        val minions =
            result.level.enemySpawns
                .map { it.attackerType }
                .filter { !it.isRealVillain }
        assertTrue(minions.isNotEmpty())
        assertTrue(minions.all { it == AttackerType.SPIDERLING })
    }

    @Test
    fun evilWizardBelongsToTheDemonsRoster() {
        assertTrue(AttackerType.EVIL_WIZARD in GeneratorEnemyRoster.DEMONS.types)
        assertFalse(AttackerType.EVIL_WIZARD in GeneratorEnemyRoster.WITCHES.types)
        assertEquals(listOf(AttackerType.RED_WITCH, AttackerType.GREEN_WITCH), GeneratorEnemyRoster.WITCHES.types)
    }

    @Test
    fun spiderVillainGetsASpiderWebMap() {
        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Web Lair",
                    villains = setOf(AttackerType.ARAXXA),
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    mapSize = GeneratedMapSize.MEDIUM,
                    seed = 31,
                ),
            )

        val map = assertNotNull(result.generatedMap)
        assertTrue(map.readyToUse)
        // A web has several spawn points around the target in the middle of the map.
        assertTrue(map.getSpawnPoints().size >= 3)
        val target = assertNotNull(map.getTargets().firstOrNull())
        assertEquals(map.width / 2, target.x)
        assertEquals(map.height / 2, target.y)
    }

    @Test
    fun generatedMapSizeCanBeAdjusted() {
        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Custom Size",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    mapSize = GeneratedMapSize.LARGE,
                    mapWidth = 27,
                    mapHeight = 19,
                    seed = 4,
                ),
            )

        val map = assertNotNull(result.generatedMap)
        assertEquals(27, map.width)
        assertEquals(19, map.height)
    }

    @Test
    fun sylvanasUsesTheWitchesRosterLikeSybilla() {
        assertEquals(
            AttackerType.GRAND_COVEN_MOTHER_SYBILLA.generatorRoster(),
            AttackerType.SYLVANAS_THE_MOLDING.generatorRoster(),
        )
        assertEquals(GeneratorEnemyRoster.WITCHES, AttackerType.SYLVANAS_THE_MOLDING.generatorRoster())
    }

    @Test
    fun covenTwinsAreNotSelectableAsStandaloneVillains() {
        assertFalse(AttackerType.HAGA.isSelectableGeneratorVillain)
        assertFalse(AttackerType.ZUSSA.isSelectableGeneratorVillain)
        assertTrue(AttackerType.GRAND_COVEN_MOTHER_SYBILLA.isSelectableGeneratorVillain)
        assertTrue(AttackerType.entries.filter { it.isSelectableGeneratorVillain }.all { it.isRealVillain })
    }

    @Test
    fun everySpawnTurnContainsSeveralUnits() {
        GeneratorDifficulty.entries.forEach { difficulty ->
            val result =
                LevelGenerator.generate(
                    LevelGeneratorConfig(
                        title = "Big Waves",
                        difficulty = difficulty,
                        mapSource = GeneratorMapSource.EXISTING_MAP,
                        existingMap = existingMap(),
                        seed = 77,
                    ),
                )

            val perTurn =
                result.level.enemySpawns
                    .groupBy { it.spawnTurn }
                    .mapValues { it.value.size }
            assertTrue(perTurn.values.all { it >= 4 }, "$difficulty has a turn with fewer than 4 units: $perTurn")
            // Later waves are bigger than the first ones.
            assertTrue(perTurn.getValue(perTurn.keys.max()) > perTurn.getValue(1))
        }
    }
}
