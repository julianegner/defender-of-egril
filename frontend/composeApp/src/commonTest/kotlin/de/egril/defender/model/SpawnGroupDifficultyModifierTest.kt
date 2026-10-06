package de.egril.defender.model

import de.egril.defender.ui.settings.DifficultyLevel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [DifficultyModifiers.applySpawnGroupsModifier] (issue #694): difficulty scaling of spawn
 * loops must raise enemy levels while preserving loop structure and single-identity (boss / named /
 * first-iteration-only) units.
 */
class SpawnGroupDifficultyModifierTest {
    private val group =
        SpawnGroup(
            groupId = "wave",
            repeatMode = SpawnRepeatMode.CONDITION,
            condition = SpawnCondition.UNIT_ALIVE,
            targetUnitId = "boss",
            turns =
                listOf(
                    SpawnGroupTurn(
                        1,
                        listOf(
                            SpawnGroupSpawn(AttackerType.EWHAD, level = 1, unitId = "boss", firstIterationOnly = true),
                            SpawnGroupSpawn(AttackerType.GOBLIN, count = 2, level = 1),
                        ),
                    ),
                    SpawnGroupTurn(2, listOf(SpawnGroupSpawn(AttackerType.SKELETON, level = 3))),
                ),
        )

    @Test
    fun mediumLeavesGroupsUnchanged() {
        assertEquals(listOf(group), DifficultyModifiers.applySpawnGroupsModifier(listOf(group), DifficultyLevel.MEDIUM))
    }

    @Test
    fun hardDoublesEveryLevelWithoutChangingStructure() {
        val result = DifficultyModifiers.applySpawnGroupsModifier(listOf(group), DifficultyLevel.HARD).single()

        // Same turn/spawn structure, only levels doubled.
        assertEquals(2, result.turns.size)
        assertEquals(2, result.turns[0].spawns.size)
        val boss = result.turns[0].spawns[0]
        assertEquals(AttackerType.EWHAD, boss.attackerType)
        assertEquals(2, boss.level)
        assertEquals("boss", boss.unitId) // identity preserved
        assertEquals(2, result.turns[0].spawns[1].level)
        assertEquals(6, result.turns[1].spawns[0].level)
    }

    @Test
    fun nightmareTriplesRegularEnemiesButKeepsNamedBossSingle() {
        val result = DifficultyModifiers.applySpawnGroupsModifier(listOf(group), DifficultyLevel.NIGHTMARE).single()

        val turn1 = result.turns[0].spawns
        // Boss (named + firstIterationOnly + Ewhad) stays a single entry at 5x level.
        val bossEntries = turn1.filter { it.attackerType == AttackerType.EWHAD }
        assertEquals(1, bossEntries.size)
        assertEquals(5, bossEntries[0].level)
        assertEquals("boss", bossEntries[0].unitId)

        // Regular goblin is tripled with 3x/2x/1x levels (count preserved per entry).
        val goblins = turn1.filter { it.attackerType == AttackerType.GOBLIN }
        assertEquals(listOf(3, 2, 1), goblins.map { it.level })
        goblins.forEach { assertEquals(2, it.count) }

        // Skeleton on turn 2 (base level 3) tripled to 9/6/3.
        val skeletons = result.turns[1].spawns.filter { it.attackerType == AttackerType.SKELETON }
        assertEquals(listOf(9, 6, 3), skeletons.map { it.level })
    }

    @Test
    fun nightmareKeepsUnitlessFirstIterationOnlyEntrySingle() {
        val minibossGroup =
            SpawnGroup(
                groupId = "mini",
                repeatMode = SpawnRepeatMode.COUNT,
                repeatCount = 2,
                turns = listOf(SpawnGroupTurn(1, listOf(SpawnGroupSpawn(AttackerType.ORK, level = 2, firstIterationOnly = true)))),
            )
        val result = DifficultyModifiers.applySpawnGroupsModifier(listOf(minibossGroup), DifficultyLevel.NIGHTMARE).single()
        val entries = result.turns[0].spawns
        assertEquals(1, entries.size) // firstIterationOnly → not duplicated
        assertEquals(6, entries[0].level) // non-Ewhad once-spawn → 3x
    }
}
