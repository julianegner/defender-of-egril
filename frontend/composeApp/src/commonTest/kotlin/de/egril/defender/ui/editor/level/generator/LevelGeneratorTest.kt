package de.egril.defender.ui.editor.level.generator

import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.AttackerType
import de.egril.defender.model.EnemyFaction
import de.egril.defender.model.Position
import de.egril.defender.model.SpawnPointType
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.model.isRealVillain
import de.egril.defender.model.hexDistanceTo
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
    fun spiderVillainGetsASpiderThemedMap() {
        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Web Lair",
                    villains = setOf(AttackerType.ARAXXA),
                    primaryRoster = GeneratorEnemyRoster.SPIDERS,
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    landSpawnCount = 3,
                    mapDescription = "spider web",
                    seed = 31,
                ),
            )

        val map = assertNotNull(result.generatedMap)
        assertTrue(map.readyToUse)
        // Spider-themed layouts still keep the "many lanes around one objective" feel.
        assertTrue(map.getSpawnPoints().size >= 3)
        assertEquals(1, map.getTargets().size)
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
    fun riverCrossingMapsStayReadyToUse() {
        val map =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "River Crossing Test",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    mapDescription = "river crossing with water spawn",
                    seed = 41,
                ),
            )
                .generatedMap

        assertNotNull(map)
        assertTrue(map.readyToUse)
        assertTrue(map.getSpawnPoints().isNotEmpty())
        assertTrue(map.getTargets().isNotEmpty())
    }

    @Test
    fun generatedSpawnsStayWithinNearbySpawnCapacity() {
        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Spawn Cap",
                    difficulty = GeneratorDifficulty.NIGHTMARE,
                    mapSource = GeneratorMapSource.EXISTING_MAP,
                    existingMap = existingMap(),
                    seed = 99,
                ),
            )

        val maxPerTurn =
            result.level.enemySpawns
                .groupBy { it.spawnTurn }
                .values
                .maxOfOrNull { it.size } ?: 0
        assertTrue(maxPerTurn <= 6)
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
            assertTrue(perTurn.values.all { it in 4..6 }, "$difficulty has an implausible spawn density: $perTurn")
        }
    }

    @Test
    fun mapDescriptionCanRequestRiverLikeLayouts() {
        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "River Request",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    waterSpawnCount = 1,
                    mapDescription = "river map with islands and water enemies",
                    seed = 1234,
                ),
            )

        val map = assertNotNull(result.generatedMap)
        assertTrue(map.readyToUse)
        assertTrue(map.getRiverCells().isNotEmpty())
        assertTrue(map.getSpawnPoints().any { map.getSpawnPointType(it) == SpawnPointType.WATER })
    }

    @Test
    fun generatedMapsAreNoLongerLockedToOneShape() {
        val uniqueTileLayouts =
            (1..8)
                .map { seed ->
                    LevelGenerator
                        .generate(
                            LevelGeneratorConfig(
                                title = "Variation",
                                mapSource = GeneratorMapSource.GENERATED_MAP,
                                mapDescription = "straight battlefield",
                                seed = seed,
                            ),
                        ).generatedMap
                        ?.tiles
                }.toSet()

        assertTrue(uniqueTileLayouts.size >= 2)
    }

    @Test
    fun differentDescriptionsYieldDifferentProceduralShaping() {
        val riverMap =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "River Variant",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    mapDescription = "river with islands",
                    seed = 7,
                ),
            ).generatedMap

        val spiderMap =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Spider Variant",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    mapDescription = "spider web with rings",
                    seed = 7,
                ),
            ).generatedMap

        assertNotNull(riverMap)
        assertNotNull(spiderMap)
        assertTrue(riverMap.tiles != spiderMap.tiles)
    }

    @Test
    fun generatedMapsUsePathAsTheDefaultTerrain() {
        val map =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Path Default",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    mapDescription = "dry plain",
                    waterLevel = 0f,
                    requirePath = false,
                    seed = 202,
                ),
            ).generatedMap

        val generatedMap = assertNotNull(map)
        val pathTiles = generatedMap.tiles.count { it.value == TileType.PATH }
        val buildTiles = generatedMap.tiles.count { it.value == TileType.BUILD_AREA }
        assertTrue(pathTiles > 0)
        assertTrue(pathTiles > buildTiles, "Default terrain should be path-dominant")
    }

    @Test
    fun minimumPathWidthParameterExpandsGeneratedPathCorridors() {
        val narrow =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Narrow Paths",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    mapWidth = 30,
                    mapHeight = 20,
                    landSpawnCount = 1,
                    targetCount = 1,
                    waterLevel = 0f,
                    minPathWidth = 1,
                    requirePath = true,
                    seed = 404,
                ),
            ).generatedMap
        val wide =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Wide Paths",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    mapWidth = 30,
                    mapHeight = 20,
                    landSpawnCount = 1,
                    targetCount = 1,
                    waterLevel = 0f,
                    minPathWidth = 3,
                    requirePath = true,
                    seed = 404,
                ),
            ).generatedMap

        val narrowMap = assertNotNull(narrow)
        val wideMap = assertNotNull(wide)
        assertTrue(wideMap.getPathCells().size > narrowMap.getPathCells().size, "Wider path setting should create larger corridors")
    }

    @Test
    fun generatedTargetsStayFarAwayFromSpawnPoints() {
        val map =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Long March",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    mapWidth = 40,
                    mapHeight = 28,
                    landSpawnCount = 3,
                    targetCount = 2,
                    waterLevel = 0.5f,
                    requirePath = true,
                    seed = 1337,
                ),
            ).generatedMap

        val generatedMap = assertNotNull(map)
        val spawnPoints = generatedMap.getSpawnPoints()
        val targets = generatedMap.getTargets()
        val minimumDistance =
            spawnPoints.minOf { spawn ->
                targets.minOf { target -> spawn.hexDistanceTo(target) }
            }
        assertTrue(minimumDistance >= 10, "Spawn points and targets should be far apart for long playable paths")
    }

    @Test
    fun spawnAndTargetCountsAreClampedToAtLeastOne() {
        val map =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Minimum Counts",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    landSpawnCount = 0,
                    targetCount = 0,
                    seed = 912,
                ),
            ).generatedMap

        val generatedMap = assertNotNull(map)
        assertTrue(generatedMap.getSpawnPoints().isNotEmpty())
        assertTrue(generatedMap.getTargets().isNotEmpty())
    }

    @Test
    fun requestedWaterSpawnCountCreatesDistinctSpawnTypes() {
        val map =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Mixed Spawn Types",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    landSpawnCount = 3,
                    waterSpawnCount = 1,
                    targetCount = 1,
                    waterLevel = 0.8f,
                    seed = 581,
                ),
            ).generatedMap

        val generatedMap = assertNotNull(map)
        val spawnPoints = generatedMap.getSpawnPoints()
        val waterSpawns = spawnPoints.count { generatedMap.getSpawnPointType(it) == SpawnPointType.WATER }
        val landSpawns = spawnPoints.count { generatedMap.getSpawnPointType(it) == SpawnPointType.LAND }
        assertTrue(waterSpawns >= 1, "At least one spawn should be marked as WATER")
        assertTrue(landSpawns >= 1, "At least one spawn should be marked as LAND")
    }

    @Test
    fun waterOnlySpawnMapsUseOnlyWaterCompatibleEnemyTypes() {
        val result =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Water Only",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    landSpawnCount = 0,
                    waterSpawnCount = 2,
                    targetCount = 1,
                    primaryRoster = GeneratorEnemyRoster.HORDE,
                    waterLevel = 0.8f,
                    seed = 919,
                ),
            )

        val generatedMap = assertNotNull(result.generatedMap)
        val spawnPoints = generatedMap.getSpawnPoints()
        assertTrue(spawnPoints.isNotEmpty())
        assertTrue(spawnPoints.all { generatedMap.getSpawnPointType(it) == SpawnPointType.WATER })
        assertTrue(result.level.enemySpawns.isNotEmpty())
        assertTrue(result.level.enemySpawns.all { it.attackerType.canSpawnOnWater })
    }

    @Test
    fun spawnTargetPathsAreLongWhenMeasuredOnAllowedTiles() {
        val map =
            LevelGenerator.generate(
                LevelGeneratorConfig(
                    title = "Long Traversable Paths",
                    mapSource = GeneratorMapSource.GENERATED_MAP,
                    mapWidth = 40,
                    mapHeight = 28,
                    landSpawnCount = 2,
                    targetCount = 2,
                    waterSpawnCount = 1,
                    waterLevel = 0.65f,
                    requirePath = true,
                    seed = 777,
                ),
            ).generatedMap

        val generatedMap = assertNotNull(map)
        val targets = generatedMap.getTargets()
        val pathLengths =
            generatedMap.getSpawnPoints().mapNotNull { spawn ->
                targets.mapNotNull { target ->
                    shortestAllowedPathLength(generatedMap, spawn, target)
                }.maxOrNull()
            }

        assertTrue(pathLengths.isNotEmpty())
        assertTrue(pathLengths.minOrNull() ?: 0 >= 10, "Spawn-to-target traversable paths should stay long")
    }

    private fun shortestAllowedPathLength(
        map: EditorMap,
        start: Position,
        end: Position,
    ): Int? {
        val allowRiverTiles = map.getSpawnPointType(start) == SpawnPointType.WATER
        val queue = ArrayDeque<Pair<Position, Int>>()
        val visited = mutableSetOf(start)
        queue.add(start to 0)

        while (queue.isNotEmpty()) {
            val (current, distance) = queue.removeFirst()
            if (current == end) return distance

            current.getHexNeighbors()
                .filter { it.x in 0 until map.width && it.y in 0 until map.height && it !in visited }
                .forEach { neighbor ->
                    val type = map.getTileType(neighbor.x, neighbor.y)
                    val traversable =
                        type == TileType.PATH ||
                            type == TileType.SPAWN_POINT ||
                            type == TileType.TARGET ||
                            (allowRiverTiles && type == TileType.RIVER)
                    if (traversable) {
                        visited += neighbor
                        queue.add(neighbor to distance + 1)
                    }
                }
        }
        return null
    }
}
