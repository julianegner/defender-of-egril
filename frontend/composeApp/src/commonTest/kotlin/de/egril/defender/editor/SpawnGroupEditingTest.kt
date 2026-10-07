package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.EventLoop
import de.egril.defender.model.EventLoopStep
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpawnGroupEditingTest {
    private val enemy = SpawnGroupSpawn(AttackerType.GOBLIN)

    private fun group(
        id: String = "wave",
        entries: List<SpawnSequenceEntry> = listOf(SpawnGroupTurn(1, listOf(enemy))),
    ) = SpawnGroup(id, SpawnRepeatMode.COUNT, entries = entries)

    private fun spawn(
        type: AttackerType,
        turn: Int,
        unitId: String? = null,
    ) = EditorEnemySpawn(type, 1, turn, unitId = unitId)

    /** Turns 1..6: a normal turn, an outer loop (2..5) with a nested loop (3..4), and a normal turn. */
    private val nestedTimeline =
        EditorSpawnTimeline(
            spawns =
                listOf(
                    spawn(AttackerType.GOBLIN, 1),
                    spawn(AttackerType.ORK, 2),
                    spawn(AttackerType.SKELETON, 3),
                    spawn(AttackerType.OGRE, 5),
                    spawn(AttackerType.GOBLIN, 6),
                ),
            maxTurn = 6,
            loops =
                listOf(
                    EditorSpawnLoop("outer", 2, 5, repeatCount = 2),
                    EditorSpawnLoop("inner", 3, 4, repeatCount = 3),
                ),
        )

    @Test
    fun timelineWithMixedAndNestedLoopsBuildsSequenceAndRoundTrips() {
        val sequence = assertNotNull(buildSpawnSequence(nestedTimeline))
        assertEquals(3, sequence.size)
        assertTrue(sequence[0] is SpawnGroupTurn)
        val outer = sequence[1] as SpawnGroup
        assertEquals("outer", outer.groupId)
        assertEquals(3, outer.entries.size)
        val inner = outer.entries[1] as SpawnGroup
        assertEquals("inner", inner.groupId)
        assertEquals(listOf(1, 2), inner.directTurns.map { it.turnOffset })
        assertTrue(sequence[2] is SpawnGroupTurn)

        assertEquals(nestedTimeline, spawnTimelineFromSequence(sequence))
    }

    @Test
    fun flatteningExpandsNestedLoopsInOrder() {
        val flat = assertNotNull(flattenSpawnSequence(assertNotNull(buildSpawnSequence(nestedTimeline))))
        assertEquals(
            listOf(1, 2, 3, 5, 7, 9, 10, 11, 13, 15, 17, 18),
            flat.spawns.map { it.spawnTurn },
        )
        assertEquals(18, flat.maxTurn)
    }

    @Test
    fun flatteningHonorsCountsAndFirstIterationOnly() {
        val first =
            group(
                entries =
                    listOf(
                        SpawnGroupTurn(1, listOf(enemy.copy(count = 2), enemy.copy(attackerType = AttackerType.ORK, firstIterationOnly = true))),
                        SpawnGroupTurn(3),
                    ),
            ).copy(repeatCount = 2)
        val flat = assertNotNull(flattenSpawnSequence(listOf(first, SpawnGroupTurn(2, listOf(enemy)))))
        assertEquals(listOf(1, 1, 1, 4, 4, 8), flat.spawns.map { it.spawnTurn })
        assertEquals(1, flat.spawns.count { it.attackerType == AttackerType.ORK })
        assertEquals(8, flat.maxTurn)
    }

    @Test
    fun dynamicAndOversizedPlansCannotBeFlattened() {
        assertNull(flattenSpawnSequence(listOf(group().copy(repeatMode = SpawnRepeatMode.INFINITE))))
        assertNull(flattenSpawnSequence(listOf(group().copy(repeatCount = Int.MAX_VALUE))))
        assertNull(flattenSpawnSequence(listOf(group(entries = listOf(SpawnGroupTurn(1, listOf(enemy.copy(count = Int.MAX_VALUE))))))))
        assertNotNull(flattenSpawnSequence(listOf(group().copy(repeatMode = SpawnRepeatMode.INFINITE)), sampleDynamicLoops = true))
    }

    @Test
    fun levelWithoutLoopsIsStoredFlatAndWithLoopsAsSequence() {
        val level =
            EditorLevel(
                id = "loop_editor_test",
                mapId = "test",
                title = "Loop test",
                enemySpawns = emptyList(),
                availableTowers = setOf(DefenderType.SPIKE_TOWER),
                startCoins = 100,
                startHealthPoints = 10,
            )
        val flat = level.withSpawnTimeline(nestedTimeline.copy(loops = emptyList(), spawns = listOf(spawn(AttackerType.ORK, 2, "named"))))
        assertNull(flat.spawnGroups)
        assertNull(flat.enemySpawns.single().unitId)

        val looped = level.withSpawnTimeline(nestedTimeline)
        assertTrue(looped.enemySpawns.isEmpty())
        assertNotNull(looped.spawnGroups)
        val restored = assertNotNull(EditorJsonSerializer.deserializeLevel(EditorJsonSerializer.serializeLevel(looped)))
        assertEquals(nestedTimeline, restored.spawnTimeline())
        assertTrue(restored.isReadyToPlay())
    }

    @Test
    fun createAddTurnAndDeleteTurnKeepLoopsAligned() {
        val base = EditorSpawnTimeline(listOf(spawn(AttackerType.GOBLIN, 1), spawn(AttackerType.ORK, 3)), maxTurn = 3)
        val withLoop = base.createLoop(2)
        assertEquals(listOf(EditorSpawnLoop("loop_1", 2, 2)), withLoop.loops)

        val extended = withLoop.addTurnToLoop("loop_1")
        assertEquals(4, extended.maxTurn)
        assertEquals(EditorSpawnLoop("loop_1", 2, 3), extended.loops.single())
        assertEquals(4, extended.spawns.single { it.attackerType == AttackerType.ORK }.spawnTurn)

        val shrunk = extended.deleteTurn(2)
        assertEquals(EditorSpawnLoop("loop_1", 2, 2), shrunk.loops.single())
        assertEquals(3, shrunk.maxTurn)
        assertTrue(shrunk.deleteTurn(2).loops.isEmpty())
        assertTrue(withLoop.unwrapLoop("loop_1").loops.isEmpty())
    }

    @Test
    fun copyLoopDuplicatesContentWithUniqueIdsAndDeleteRemovesIt() {
        val original =
            EditorSpawnTimeline(
                spawns =
                    listOf(
                        spawn(AttackerType.GOBLIN, 1),
                        spawn(AttackerType.ORK, 2, unitId = "boss"),
                        spawn(AttackerType.SKELETON, 3),
                    ),
                maxTurn = 3,
                loops =
                    listOf(
                        EditorSpawnLoop(
                            "loop_1",
                            2,
                            2,
                            repeatMode = SpawnRepeatMode.CONDITION,
                            condition = SpawnCondition.UNIT_ALIVE,
                            targetUnitId = "boss",
                        ),
                    ),
            )
        val copied = original.copyLoop("loop_1")
        assertEquals(4, copied.maxTurn)
        val copy = copied.loops.single { it.id != "loop_1" }
        assertEquals(3, copy.startTurn)
        assertEquals(3, copy.endTurn)
        assertEquals("boss_2", copy.targetUnitId)
        assertEquals("boss_2", copied.spawns.single { it.spawnTurn == 3 }.unitId)
        assertEquals(4, copied.spawns.single { it.attackerType == AttackerType.SKELETON }.spawnTurn)
        assertTrue(spawnTimelineIssues(copied).isEmpty())

        assertEquals(original, copied.deleteLoopWithTurns(copy.id))
    }

    @Test
    fun copyingAnOuterLoopCopiesNestedLoops() {
        val copied = nestedTimeline.copyLoop("outer")
        assertEquals(10, copied.maxTurn)
        assertEquals(4, copied.loops.size)
        assertEquals(listOf(2 to 5, 3 to 4, 6 to 9, 7 to 8), copied.loops.map { it.startTurn to it.endTurn })
        assertTrue(spawnTimelineIssues(copied).isEmpty())
    }

    @Test
    fun resizeRejectsPartialOverlapsAndStructureIssuesAreReported() {
        assertNull(nestedTimeline.resizeLoop("inner", 3, 6))
        assertNotNull(nestedTimeline.resizeLoop("outer", 2, 6))
        assertNull(nestedTimeline.resizeLoop("outer", 0, 5))

        val overlapping = nestedTimeline.copy(loops = listOf(EditorSpawnLoop("a", 1, 3), EditorSpawnLoop("b", 2, 4)))
        assertEquals(setOf(SpawnGroupIssue.STRUCTURE), spawnTimelineIssues(overlapping))
        val duplicateIds = nestedTimeline.copy(loops = listOf(EditorSpawnLoop("a", 1, 2), EditorSpawnLoop("a", 3, 4)))
        assertTrue(SpawnGroupIssue.GROUP_ID in spawnTimelineIssues(duplicateIds))
        assertTrue(spawnTimelineIssues(nestedTimeline.copy(loops = emptyList())).isEmpty())
    }

    @Test
    fun conditionCanReferenceUnitDefinedBeforeOrInsideTheLoop() {
        val bossTurn = SpawnGroupTurn(3, listOf(enemy.copy(unitId = "boss", firstIterationOnly = true)))
        val loop =
            group("adds").copy(
                repeatMode = SpawnRepeatMode.CONDITION,
                condition = SpawnCondition.UNIT_ALIVE,
                targetUnitId = "boss",
            )
        assertTrue(spawnGroupIssues(listOf(bossTurn, loop)).isEmpty())
        assertTrue(SpawnGroupIssue.CONDITION_TARGET in spawnGroupIssues(listOf(loop, bossTurn)))
        assertTrue(spawnGroupIssues(listOf(loop.copy(entries = listOf(group("inner", listOf(bossTurn)))))).isEmpty())
    }

    @Test
    fun rejectsInvalidOffsetsCountsReferencesAndDuplicateIds() {
        val broken =
            group(
                entries =
                    listOf(
                        SpawnGroupTurn(0, listOf(enemy.copy(count = 0, level = 0, unitId = "boss"))),
                        SpawnGroupTurn(0, listOf(enemy.copy(unitId = "boss"))),
                    ),
            ).copy(repeatCount = 0)
        val issues = spawnGroupIssues(listOf(broken, broken))
        assertTrue(SpawnGroupIssue.GROUP_ID in issues)
        assertTrue(SpawnGroupIssue.TURNS in issues)
        assertTrue(SpawnGroupIssue.REPEAT_COUNT in issues)
        assertTrue(SpawnGroupIssue.SPAWN_VALUES in issues)
        assertTrue(SpawnGroupIssue.UNIT_ID in issues)
        assertTrue(SpawnGroupIssue.TURNS in spawnGroupIssues(listOf(group(entries = emptyList()))))
    }

    @Test
    fun infiniteLoopMayBeFollowedByFurtherTurnsAndLoops() {
        // Infinite loops are always ended by an event, so turns and loops after them are reachable.
        val infinite = group("endless").copy(repeatMode = SpawnRepeatMode.INFINITE)
        assertTrue(spawnGroupIssues(listOf(infinite, group("after"))).isEmpty())
        assertTrue(spawnGroupIssues(listOf(group("outer", listOf(infinite, SpawnGroupTurn(1, listOf(enemy)))))).isEmpty())
        assertTrue(spawnGroupIssues(listOf(group("outer", listOf(infinite)), SpawnGroupTurn(1, listOf(enemy))), stoppedLoopIds = setOf("endless")).isEmpty())
    }

    @Test
    fun villainsRemainUniqueAndOnlySpawnOnceInLoops() {
        val boss = enemy.copy(attackerType = AttackerType.EWHAD)
        val loop = group(entries = listOf(SpawnGroupTurn(1, listOf(boss)))).copy(repeatCount = 2)
        // Villains always spawn only in the first iteration, even without the explicit flag.
        assertTrue(spawnGroupIssues(listOf(loop)).isEmpty())
        val valid = loop.copy(entries = listOf(SpawnGroupTurn(1, listOf(boss.copy(firstIterationOnly = true)))))
        assertTrue(spawnGroupIssues(listOf(valid)).isEmpty())
        assertTrue(SpawnGroupIssue.VILLAIN in spawnGroupIssues(listOf(valid, valid.copy(groupId = "second"))))
        // A repeating outer loop would respawn the villain even though the inner loop runs once.
        val outer = group("outer", listOf(group("inner", listOf(SpawnGroupTurn(1, listOf(boss)))))).copy(repeatCount = 2)
        assertTrue(SpawnGroupIssue.VILLAIN in spawnGroupIssues(listOf(outer)))
    }

    @Test
    fun remappingSwapsPositionsSimultaneouslyWithoutChangingLoopMetadata() {
        val a = Position(0, 1)
        val b = Position(5, 1)
        val original =
            group(
                entries =
                    listOf(
                        SpawnGroupTurn(
                            2,
                            listOf(
                                enemy.copy(spawnPoint = a, unitId = "boss", firstIterationOnly = true),
                                enemy.copy(spawnPoint = b),
                                enemy,
                            ),
                        ),
                    ),
            ).copy(repeatMode = SpawnRepeatMode.CONDITION, condition = SpawnCondition.UNIT_ALIVE, targetUnitId = "boss")
        val remapped = remapSpawnGroupPoints(listOf(original), mapOf(a to b, b to a)).single() as SpawnGroup
        assertEquals(listOf(b, a, null), remapped.directTurns.single().spawns.map { it.spawnPoint })
        assertEquals(original, remapSpawnGroupPoints(listOf(remapped), mapOf(a to b, b to a)).single())
    }

    @Test
    fun mapValidationAllowsRandomCompatiblePointsButRejectsInvalidAssignments() {
        val map = EditorMap("points", width = 8, height = 5, tiles = mapOf("0,2" to TileType.SPAWN_POINT))
        assertTrue(spawnGroupIssues(listOf(group()), map).isEmpty())
        val invalid = group(entries = listOf(SpawnGroupTurn(1, listOf(enemy.copy(spawnPoint = Position(7, 4))))))
        assertTrue(SpawnGroupIssue.SPAWN_POINT in spawnGroupIssues(listOf(invalid), map))
        val fixed = remapSpawnGroupPoints(listOf(invalid), mapOf(Position(7, 4) to Position(0, 2)))
        assertTrue(spawnGroupIssues(fixed, map).isEmpty())
        assertTrue(SpawnGroupIssue.SPAWN_POINT in spawnGroupIssues(fixed, map.copy(tiles = emptyMap())))
    }

    @Test
    fun villainsDefaultToFirstIterationOnlyWhenPlacedInLoops() {
        assertTrue(EditorEnemySpawn(AttackerType.EWHAD, spawnTurn = 1).firstIterationOnly)
        assertTrue(!EditorEnemySpawn(AttackerType.GOBLIN, spawnTurn = 1).firstIterationOnly)

        val timeline =
            EditorSpawnTimeline(listOf(EditorEnemySpawn(AttackerType.EWHAD, spawnTurn = 1), EditorEnemySpawn(AttackerType.GOBLIN, spawnTurn = 1)), 1)
                .createLoop(1)
        val sequence = assertNotNull(buildSpawnSequence(timeline))
        val spawns = (sequence.single() as SpawnGroup).directTurns.single().spawns
        assertTrue(spawns.single { it.attackerType == AttackerType.EWHAD }.firstIterationOnly)
        assertTrue(!spawns.single { it.attackerType == AttackerType.GOBLIN }.firstIterationOnly)
        assertTrue(spawnTimelineIssues(timeline).isEmpty())
    }

    @Test
    fun villainsAlwaysSpawnInFirstIterationOnly() {
        val forcedOff = EditorEnemySpawn(AttackerType.EWHAD, spawnTurn = 1, firstIterationOnly = false)
        assertTrue(forcedOff.spawnsInFirstIterationOnly)
        val timeline = EditorSpawnTimeline(listOf(forcedOff), 1).createLoop(1)
        val sequence = assertNotNull(buildSpawnSequence(timeline))
        assertTrue((sequence.single() as SpawnGroup).directTurns.single().spawns.single().firstIterationOnly)
        assertTrue(spawnTimelineIssues(timeline).isEmpty())
        assertTrue(SpawnGroupSpawn(AttackerType.EWHAD, firstIterationOnly = false).spawnsInFirstIterationOnly)
    }

    @Test
    fun villainsUseTheirNameAsLoopUnitId() {
        assertEquals("Ewhad", EditorEnemySpawn(AttackerType.EWHAD, spawnTurn = 1, unitId = "boss").loopUnitId)
        assertEquals(
            "Grand Runemaster Vaelen",
            EditorEnemySpawn(AttackerType.GRAND_RUNEMASTER_VAELEN, spawnTurn = 1, unitId = "boss").loopUnitId,
        )
        assertEquals("scout", EditorEnemySpawn(AttackerType.GOBLIN, spawnTurn = 1, unitId = "scout").loopUnitId)

        // Older data with a custom villain id is migrated, and conditions follow the rename.
        val legacy =
            listOf(
                SpawnGroup(
                    groupId = "boss_loop",
                    repeatMode = SpawnRepeatMode.CONDITION,
                    condition = SpawnCondition.UNIT_ALIVE,
                    targetUnitId = "boss",
                    entries = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.EWHAD, unitId = "boss", firstIterationOnly = true)))),
                ),
            )
        val timeline = spawnTimelineFromSequence(legacy)
        assertEquals("Ewhad", timeline.loops.single().targetUnitId)
        assertEquals("Ewhad", timeline.spawns.single().unitId)
        assertTrue(spawnTimelineIssues(timeline).isEmpty())
        val rebuilt = assertNotNull(buildSpawnSequence(timeline)).single() as SpawnGroup
        assertEquals("Ewhad", rebuilt.targetUnitId)
        assertEquals("Ewhad", rebuilt.directTurns.single().spawns.single().unitId)
    }

    @Test
    fun infiniteLoopsMustBeStoppedByAnEvent() {
        val infinite = SpawnGroup("endless", SpawnRepeatMode.INFINITE, entries = listOf(SpawnGroupTurn(1, listOf(enemy))))
        val stop = EventAction(type = EventActionType.STOP_SPAWN_LOOP, spawnLoopId = "endless")
        val stopEvents = LevelEvents(listOf(LevelEvent("stop", EventCondition(EventConditionType.TURN_START), actions = listOf(stop))))

        assertTrue(SpawnGroupIssue.INFINITE_NOT_STOPPED in spawnGroupIssues(listOf(infinite), stoppedLoopIds = emptySet()))
        assertTrue(spawnGroupIssues(listOf(infinite), stoppedLoopIds = stopEvents.stoppedSpawnLoopIds()).isEmpty())
        assertEquals(listOf("endless"), unstoppedInfiniteSpawnLoops(listOf(infinite), LevelEvents()))
        assertTrue(unstoppedInfiniteSpawnLoops(listOf(infinite), stopEvents).isEmpty())

        // A stop inside an event loop counts as well; stops of unknown loops are reported.
        val loopStop =
            LevelEvents(
                listOf(
                    LevelEvent(
                        "loop",
                        EventCondition(EventConditionType.TURN_START),
                        loop = EventLoop(steps = listOf(EventLoopStep(actions = listOf(stop, stop.copy(spawnLoopId = "gone"))))),
                    ),
                ),
            )
        assertTrue(unstoppedInfiniteSpawnLoops(listOf(infinite), loopStop).isEmpty())
        assertEquals(1, invalidSpawnLoopStopCount(loopStop, setOf("endless")))

        val timeline = EditorSpawnTimeline(listOf(EditorEnemySpawn(AttackerType.GOBLIN, spawnTurn = 1)), 1).createLoop(1)
        val loopId = timeline.loops.single().id
        val infiniteTimeline = timeline.copy(loops = timeline.loops.map { it.copy(repeatMode = SpawnRepeatMode.INFINITE) })
        assertTrue(SpawnGroupIssue.INFINITE_NOT_STOPPED in spawnTimelineIssues(infiniteTimeline, stoppedLoopIds = emptySet()))
        assertTrue(spawnTimelineIssues(infiniteTimeline, stoppedLoopIds = setOf(loopId)).isEmpty())

        val level =
            EditorLevel(id = "l", mapId = "m", title = "t", startCoins = 10, startHealthPoints = 10, enemySpawns = emptyList(), availableTowers = setOf(DefenderType.SPIKE_TOWER))
                .withSpawnTimeline(infiniteTimeline)
        assertTrue(!level.isReadyToPlay())
        val stopping = EventAction(type = EventActionType.STOP_SPAWN_LOOP, spawnLoopId = loopId)
        assertTrue(level.copy(events = LevelEvents(listOf(LevelEvent("s", EventCondition(EventConditionType.TURN_START), actions = listOf(stopping))))).isReadyToPlay())
    }
}
