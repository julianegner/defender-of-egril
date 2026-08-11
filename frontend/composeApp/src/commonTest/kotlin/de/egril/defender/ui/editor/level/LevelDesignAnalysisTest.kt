package de.egril.defender.ui.editor.level

import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorEnemyTemplateKind
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.SpawnTurnTemplateDefinition
import de.egril.defender.editor.SpawnTurnTemplateEntry
import de.egril.defender.editor.SpawnTurnTemplateVariant
import de.egril.defender.editor.TileType
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelDesignAnalysisTest {
    @Test
    fun analyzeLevelDesignFlagsCountersArrivalsAndVillainTiming() {
        val map = straightTestMap()
        val level =
            testLevel(
                spawns =
                    listOf(
                        EditorEnemySpawn(AttackerType.GOBLIN, spawnTurn = 1),
                        EditorEnemySpawn(AttackerType.RED_DEMON, spawnTurn = 2),
                        EditorEnemySpawn(AttackerType.BLUE_DEMON, spawnTurn = 3),
                        EditorEnemySpawn(AttackerType.EWHAD, spawnTurn = 4),
                        EditorEnemySpawn(AttackerType.PIRATE, spawnTurn = 6),
                    ),
                towers = setOf(DefenderType.SPIKE_TOWER),
            )

        val summary = analyzeLevelDesign(level, map)

        assertEquals(4, summary.firstVillainTurn)
        assertEquals(TimingBand.EARLY, summary.villainTimingBand)
        assertEquals(3, summary.turnPreviews.first { it.turn == 1 }.earliestArrivalTurn)
        assertTrue(DefenderType.WIZARD_TOWER in summary.missingCounters)
        assertTrue(DefenderType.ALCHEMY_TOWER in summary.missingCounters)
        assertTrue(DefenderType.BOW_TOWER in summary.missingCounters)
    }

    @Test
    fun applySpawnTurnTemplateAppendsNewTurnsAfterCurrentMaximum() {
        val map = straightTestMap()
        val existing = mutableListOf(EditorEnemySpawn(AttackerType.GOBLIN, spawnTurn = 2))
        val template =
            SpawnTurnTemplateDefinition(
                id = "mixed_push",
                name = "Mixed push",
                description = "",
                variants =
                    listOf(
                        SpawnTurnTemplateVariant(
                            kind = EditorEnemyTemplateKind.HORDE,
                            entries =
                                listOf(
                                    SpawnTurnTemplateEntry(AttackerType.GOBLIN, turnOffset = 0, amount = 2),
                                    SpawnTurnTemplateEntry(AttackerType.ORK, turnOffset = 0, amount = 1),
                                    SpawnTurnTemplateEntry(AttackerType.ROBOTIC_GOBLIN, turnOffset = 1, amount = 2),
                                ),
                        ),
                    ),
            )

        val (updated, newMaxTurn) =
            applySpawnTurnTemplate(
                existing,
                maxTurnNumber = 2,
                map = map,
                template = template,
                enemyKind = EditorEnemyTemplateKind.HORDE,
                baseLevel = 2,
            )

        assertEquals(6, updated.size)
        assertEquals(4, newMaxTurn)
        assertEquals(listOf(2, 3, 3, 3, 4, 4), updated.map { it.spawnTurn }.sorted())
        assertEquals(listOf(2, 2, 2, 2, 2), updated.drop(1).map { it.level })
    }

    @Test
    fun createFocusedPlaytestLevelTrimsEarlierWavesAndAddsBufferCoins() {
        val level =
            testLevel(
                coins = 100,
                spawns =
                    listOf(
                        EditorEnemySpawn(AttackerType.GOBLIN, spawnTurn = 1),
                        EditorEnemySpawn(AttackerType.ORK, spawnTurn = 2),
                        EditorEnemySpawn(AttackerType.EWHAD, spawnTurn = 4),
                        EditorEnemySpawn(AttackerType.GOBLIN, spawnTurn = 5),
                    ),
            )
        val summary = analyzeLevelDesign(level, straightTestMap())

        val playtest = createFocusedPlaytestLevel(level, summary, FocusedPlaytestType.FIRST_VILLAIN)

        assertEquals(listOf(1, 3, 4), playtest.enemySpawns.map { it.spawnTurn })
        assertEquals(102, playtest.startCoins)
        assertTrue(playtest.title.contains("first villain"))
    }

    private fun straightTestMap(): EditorMap {
        val tiles = mutableMapOf<String, TileType>()
        for (x in 0..6) {
            tiles["$x,2"] = TileType.PATH
        }
        tiles["0,2"] = TileType.SPAWN_POINT
        tiles["7,2"] = TileType.TARGET
        return EditorMap(
            id = "test_map",
            name = "Test Map",
            width = 8,
            height = 5,
            tiles = tiles,
        )
    }

    private fun testLevel(
        spawns: List<EditorEnemySpawn>,
        towers: Set<DefenderType> = setOf(DefenderType.SPIKE_TOWER, DefenderType.SPEAR_TOWER),
        coins: Int = 100,
    ): EditorLevel =
        EditorLevel(
            id = "test_level",
            mapId = "test_map",
            title = "Test Level",
            startCoins = coins,
            startHealthPoints = 10,
            enemySpawns = spawns,
            availableTowers = towers,
        )
}
