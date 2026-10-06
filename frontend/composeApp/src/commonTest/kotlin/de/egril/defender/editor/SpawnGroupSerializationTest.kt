package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import de.egril.defender.model.SpawnCondition
import de.egril.defender.model.SpawnGroup
import de.egril.defender.model.SpawnGroupSpawn
import de.egril.defender.model.SpawnGroupTurn
import de.egril.defender.model.SpawnRepeatMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * JSON round-trip tests for spawn groups (issue #694) in [EditorJsonSerializer], including backward
 * compatibility with level files that predate spawn groups.
 */
class SpawnGroupSerializationTest {
    private fun baseLevel(groups: List<SpawnGroup>?): EditorLevel =
        EditorLevel(
            id = "spawn_group_level",
            mapId = "test_map",
            title = "Spawn Group Level",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
            spawnGroups = groups,
        )

    @Test
    fun roundTripsAllRepeatModesAndSpawnFields() {
        val groups =
            listOf(
                SpawnGroup(
                    groupId = "wave_1_basic",
                    repeatMode = SpawnRepeatMode.COUNT,
                    repeatCount = 3,
                    turns =
                        listOf(
                            SpawnGroupTurn(
                                1,
                                listOf(SpawnGroupSpawn(AttackerType.GOBLIN, count = 2, level = 1, spawnPoint = Position(0, 2))),
                            ),
                            SpawnGroupTurn(2, listOf(SpawnGroupSpawn(AttackerType.ORK, count = 1, level = 2))),
                        ),
                ),
                SpawnGroup(
                    groupId = "boss_phase_ewhad",
                    repeatMode = SpawnRepeatMode.CONDITION,
                    condition = SpawnCondition.UNIT_ALIVE,
                    targetUnitId = "ewhad_boss",
                    turns =
                        listOf(
                            SpawnGroupTurn(
                                1,
                                listOf(SpawnGroupSpawn(AttackerType.EWHAD, unitId = "ewhad_boss", firstIterationOnly = true)),
                            ),
                            SpawnGroupTurn(2, emptyList()),
                            SpawnGroupTurn(3, listOf(SpawnGroupSpawn(AttackerType.SKELETON, count = 3))),
                        ),
                ),
                SpawnGroup(
                    groupId = "endless_onslaught",
                    repeatMode = SpawnRepeatMode.INFINITE,
                    turns = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.RED_WITCH)))),
                ),
            )

        val json = EditorJsonSerializer.serializeLevel(baseLevel(groups))
        val restored = EditorJsonSerializer.deserializeLevel(json)

        assertNotNull(restored)
        val restoredGroups = restored.spawnGroups
        assertNotNull(restoredGroups)
        assertEquals(3, restoredGroups.size)

        val first = restoredGroups[0]
        assertEquals("wave_1_basic", first.groupId)
        assertEquals(SpawnRepeatMode.COUNT, first.repeatMode)
        assertEquals(3, first.repeatCount)
        assertEquals(2, first.turns.size)
        assertEquals(1, first.turns[0].turnOffset)
        assertEquals(AttackerType.GOBLIN, first.turns[0].spawns[0].attackerType)
        assertEquals(2, first.turns[0].spawns[0].count)
        assertEquals(Position(0, 2), first.turns[0].spawns[0].spawnPoint)
        assertEquals(2, first.turns[1].spawns[0].level)

        val boss = restoredGroups[1]
        assertEquals(SpawnRepeatMode.CONDITION, boss.repeatMode)
        assertEquals(SpawnCondition.UNIT_ALIVE, boss.condition)
        assertEquals("ewhad_boss", boss.targetUnitId)
        assertEquals("ewhad_boss", boss.turns[0].spawns[0].unitId)
        assertTrue(boss.turns[0].spawns[0].firstIterationOnly)
        assertTrue(boss.turns[1].spawns.isEmpty())
        assertEquals(3, boss.turns[2].spawns[0].count)

        val endless = restoredGroups[2]
        assertEquals(SpawnRepeatMode.INFINITE, endless.repeatMode)
    }

    @Test
    fun levelsWithoutSpawnGroupsStayBackwardCompatible() {
        val json = EditorJsonSerializer.serializeLevel(baseLevel(groups = null))
        assertFalse(json.contains("spawnGroups"))
        val restored = EditorJsonSerializer.deserializeLevel(json)
        assertNotNull(restored)
        assertNull(restored.spawnGroups)
        assertEquals(1, restored.enemySpawns.size)
    }

    @Test
    fun groupOnlyLevelsAreReadyAndReportConfiguredSpawnCount() {
        val level =
            baseLevel(
                listOf(
                    SpawnGroup(
                        groupId = "wave",
                        repeatMode = SpawnRepeatMode.COUNT,
                        repeatCount = 3,
                        turns =
                            listOf(
                                SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.GOBLIN, count = 2))),
                                SpawnGroupTurn(2, listOf(SpawnGroupSpawn(AttackerType.ORK))),
                            ),
                    ),
                ),
            ).copy(enemySpawns = emptyList())

        assertTrue(level.hasConfiguredSpawns())
        assertTrue(level.isReadyToPlay())
        assertEquals(3L, level.configuredSpawnCount())
        assertEquals(2, level.toLevelInfoEnemiesLevelData(0).enemyTypeCounts[AttackerType.GOBLIN])
    }

    @Test
    fun emptySpawnGroupsArePreservedAndOverrideFlatSpawns() {
        val json = EditorJsonSerializer.serializeLevel(baseLevel(groups = emptyList()))

        assertTrue(json.contains("\"spawnGroups\": ["))
        val restored = EditorJsonSerializer.deserializeLevel(json)

        assertNotNull(restored)
        assertEquals(emptyList(), restored.spawnGroups)
        assertFalse(restored.hasConfiguredSpawns())
        assertEquals(0L, restored.configuredSpawnCount())
    }

    @Test
    fun spawnGroupIdentifiersRoundTripEscapedJsonCharacters() {
        val unitId = "boss\\phase\"one"
        val groups =
            listOf(
                SpawnGroup(
                    groupId = "phase\\\"one",
                    repeatMode = SpawnRepeatMode.CONDITION,
                    condition = SpawnCondition.UNIT_ALIVE,
                    targetUnitId = unitId,
                    turns =
                        listOf(
                            SpawnGroupTurn(
                                1,
                                listOf(
                                    SpawnGroupSpawn(
                                        AttackerType.EWHAD,
                                        unitId = unitId,
                                        firstIterationOnly = true,
                                    ),
                                ),
                            ),
                        ),
                ),
            )

        val restored = EditorJsonSerializer.deserializeLevel(EditorJsonSerializer.serializeLevel(baseLevel(groups)))

        assertNotNull(restored)
        assertEquals(groups, restored.spawnGroups)
    }

    @Test
    fun malformedSpawnGroupBoundariesRejectTheLevel() {
        val invalidGroups =
            listOf(
                SpawnGroup(
                    groupId = "zero-repeat",
                    repeatMode = SpawnRepeatMode.COUNT,
                    repeatCount = 0,
                    turns = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.GOBLIN)))),
                ),
                SpawnGroup(
                    groupId = "zero-offset",
                    repeatMode = SpawnRepeatMode.COUNT,
                    turns = listOf(SpawnGroupTurn(0, listOf(SpawnGroupSpawn(AttackerType.GOBLIN)))),
                ),
                SpawnGroup(
                    groupId = "zero-count",
                    repeatMode = SpawnRepeatMode.COUNT,
                    turns = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.GOBLIN, count = 0)))),
                ),
                SpawnGroup(
                    groupId = "condition-without-target",
                    repeatMode = SpawnRepeatMode.CONDITION,
                    condition = SpawnCondition.UNIT_ALIVE,
                    turns = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.GOBLIN)))),
                ),
            )

        invalidGroups.forEach { group ->
            val json = EditorJsonSerializer.serializeLevel(baseLevel(listOf(group)))
            assertNull(EditorJsonSerializer.deserializeLevel(json), group.groupId)
        }
    }
}
