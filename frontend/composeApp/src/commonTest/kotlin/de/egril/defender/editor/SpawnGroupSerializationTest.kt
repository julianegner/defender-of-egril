package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.Position
import de.egril.defender.model.SpawnCondition
import de.egril.defender.model.SpawnGroup
import de.egril.defender.model.SpawnGroupSpawn
import de.egril.defender.model.SpawnGroupTurn
import de.egril.defender.model.SpawnRepeatMode
import de.egril.defender.model.SpawnSequenceEntry
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
    private fun baseLevel(groups: List<SpawnSequenceEntry>?): EditorLevel =
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
                    entries =
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
                    entries =
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
                    entries = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.RED_WITCH)))),
                ),
            )

        val json = EditorJsonSerializer.serializeLevel(baseLevel(groups))
        val restored = EditorJsonSerializer.deserializeLevel(json)

        assertNotNull(restored)
        val restoredGroups = restored.spawnGroups?.filterIsInstance<SpawnGroup>()
        assertNotNull(restoredGroups)
        assertEquals(3, restoredGroups.size)

        val first = restoredGroups[0]
        assertEquals("wave_1_basic", first.groupId)
        assertEquals(SpawnRepeatMode.COUNT, first.repeatMode)
        assertEquals(3, first.repeatCount)
        assertEquals(2, first.directTurns.size)
        assertEquals(1, first.directTurns[0].turnOffset)
        assertEquals(AttackerType.GOBLIN, first.directTurns[0].spawns[0].attackerType)
        assertEquals(2, first.directTurns[0].spawns[0].count)
        assertEquals(Position(0, 2), first.directTurns[0].spawns[0].spawnPoint)
        assertEquals(2, first.directTurns[1].spawns[0].level)

        val boss = restoredGroups[1]
        assertEquals(SpawnRepeatMode.CONDITION, boss.repeatMode)
        assertEquals(SpawnCondition.UNIT_ALIVE, boss.condition)
        assertEquals("ewhad_boss", boss.targetUnitId)
        assertEquals("ewhad_boss", boss.directTurns[0].spawns[0].unitId)
        assertTrue(boss.directTurns[0].spawns[0].firstIterationOnly)
        assertTrue(boss.directTurns[1].spawns.isEmpty())
        assertEquals(3, boss.directTurns[2].spawns[0].count)

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
                        entries =
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
        assertEquals(6, level.toLevelInfoEnemiesLevelData(0).enemyTypeCounts[AttackerType.GOBLIN])
        assertEquals(3, level.toLevelInfoEnemiesLevelData(0).enemyTypeCounts[AttackerType.ORK])
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
                    entries =
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
                    entries = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.GOBLIN)))),
                ),
                SpawnGroup(
                    groupId = "zero-offset",
                    repeatMode = SpawnRepeatMode.COUNT,
                    entries = listOf(SpawnGroupTurn(0, listOf(SpawnGroupSpawn(AttackerType.GOBLIN)))),
                ),
                SpawnGroup(
                    groupId = "zero-count",
                    repeatMode = SpawnRepeatMode.COUNT,
                    entries = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.GOBLIN, count = 0)))),
                ),
                SpawnGroup(
                    groupId = "condition-without-target",
                    repeatMode = SpawnRepeatMode.CONDITION,
                    condition = SpawnCondition.UNIT_ALIVE,
                    entries = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.GOBLIN)))),
                ),
            )

        invalidGroups.forEach { group ->
            val json = EditorJsonSerializer.serializeLevel(baseLevel(listOf(group)))
            assertNull(EditorJsonSerializer.deserializeLevel(json), group.groupId)
        }
    }

    @Test
    fun nestedLoopsAndMixedTopLevelEntriesRoundTrip() {
        val sequence: List<SpawnSequenceEntry> =
            listOf(
                SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.GOBLIN))),
                SpawnGroup(
                    groupId = "outer",
                    repeatMode = SpawnRepeatMode.COUNT,
                    repeatCount = 2,
                    entries =
                        listOf(
                            SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.ORK, unitId = "captain", firstIterationOnly = true))),
                            SpawnGroup(
                                groupId = "inner",
                                repeatMode = SpawnRepeatMode.CONDITION,
                                condition = SpawnCondition.UNIT_ALIVE,
                                targetUnitId = "captain",
                                entries = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.SKELETON, count = 2)))),
                            ),
                            SpawnGroupTurn(1, emptyList()),
                        ),
                ),
                SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.OGRE))),
            )

        val json = EditorJsonSerializer.serializeLevel(baseLevel(sequence).copy(enemySpawns = emptyList()))
        val restored = EditorJsonSerializer.deserializeLevel(json)

        assertNotNull(restored)
        assertEquals(sequence, restored.spawnGroups)
    }

    @Test
    fun legacyGroupsWithTurnsArrayStillLoad() {
        val json = EditorJsonSerializer.serializeLevel(baseLevel(null))
        val legacyGroups =
            """"spawnGroups": [{"groupId": "legacy", "repeatMode": "COUNT", "repeatCount": 2, "turns": [{"turnOffset": 1, "spawns": [{"attackerType": "GOBLIN", "count": 1, "level": 1}]}]}],"""
        val legacyJson = json.replaceFirst("\"enemySpawns\"", "$legacyGroups\n  \"enemySpawns\"")
        val restored = EditorJsonSerializer.deserializeLevel(legacyJson)

        assertNotNull(restored)
        val group = restored.spawnGroups?.single() as SpawnGroup
        assertEquals("legacy", group.groupId)
        assertEquals(2, group.repeatCount)
        assertEquals(AttackerType.GOBLIN, group.directTurns.single().spawns.single().attackerType)
    }

    @Test
    fun stopSpawnLoopActionRoundTrips() {
        val stop = EventAction(type = EventActionType.STOP_SPAWN_LOOP, spawnLoopId = "endless")
        val events = LevelEvents(listOf(LevelEvent("stop", EventCondition(EventConditionType.TURN_START, fromTurn = 5), actions = listOf(stop))))
        val level =
            baseLevel(listOf(SpawnGroup("endless", SpawnRepeatMode.INFINITE, entries = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.GOBLIN)))))))
                .copy(enemySpawns = emptyList(), events = events)
        val loaded = assertNotNull(EditorJsonSerializer.deserializeLevel(EditorJsonSerializer.serializeLevel(level)))
        assertEquals(events, loaded.events)
        assertTrue(loaded.isReadyToPlay())
        assertFalse(loaded.copy(events = LevelEvents()).isReadyToPlay())
    }
}
