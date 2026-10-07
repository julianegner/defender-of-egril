package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorSpawnLoop
import de.egril.defender.editor.EditorSpawnTimeline
import de.egril.defender.editor.spawnTimeline
import de.egril.defender.editor.withSpawnTimeline
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.SpawnCondition
import de.egril.defender.model.SpawnGroup
import de.egril.defender.model.SpawnGroupTurn
import de.egril.defender.model.SpawnRepeatMode
import de.egril.defender.ui.editor.level.LevelEditorView
import de.egril.defender.ui.editor.level.enemies.SpawnLoopOptionsDialog
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SpawnLoopEditorTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun useEnglish() {
        currentLanguage.value = AppLocale.DEFAULT
    }

    private fun level() =
        EditorLevel(
            id = "spawn_loop_ui_test",
            mapId = "map_30x8",
            title = "Loop UI",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, spawnTurn = 2)),
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
        )

    private fun loopLevel(
        loops: List<EditorSpawnLoop>,
        spawns: List<EditorEnemySpawn> = listOf(EditorEnemySpawn(AttackerType.ORK, spawnTurn = 2)),
        maxTurn: Int = 3,
    ) = level().withSpawnTimeline(EditorSpawnTimeline(spawns, maxTurn, loops))

    @Test
    fun createsLoopFromTurnEditsItWithUndoAndCopiesIt() {
        var saved: EditorLevel? = null
        rule.setContent { LevelEditorView(level(), { saved = it }, {}, { _, _ -> }) }
        rule.onNodeWithText("Enemy Spawns").performClick()
        rule.onAllNodesWithText("Create loop")[1].performClick()

        // The loop header is collapsed by default.
        rule.onNodeWithText("Loop loop_1").assertExists()
        rule.onNodeWithText("Total iterations (at least 1)").assertDoesNotExist()
        rule.onNodeWithText("Loop loop_1").performClick()
        rule.onNodeWithText("Total iterations (at least 1)").performTextReplacement("3")
        rule.onNodeWithText("Save Level").performClick()
        val sequence = assertNotNull(saved?.spawnGroups)
        assertEquals(SpawnGroupTurn(1), sequence[0])
        val loop = sequence[1] as SpawnGroup
        assertEquals(3, loop.repeatCount)
        assertEquals(AttackerType.GOBLIN, loop.directTurns.single().spawns.single().attackerType)

        rule.onNodeWithText("Undo").performClick()
        rule.onNodeWithText("Copy loop").performClick()
        rule.onNodeWithText("Save Level").performClick()
        val copied = assertNotNull(saved?.spawnGroups).filterIsInstance<SpawnGroup>()
        assertEquals(listOf(2, 2), copied.map { it.repeatCount })
        assertEquals(2, copied.map { it.groupId }.distinct().size)
    }

    @Test
    fun nestedLoopsShowCollapsedHeadersAndSaveUnchanged() {
        val nested =
            loopLevel(
                loops = listOf(EditorSpawnLoop("outer", 1, 3, repeatCount = 2), EditorSpawnLoop("inner", 2, 3, repeatCount = 3)),
            )
        var saved: EditorLevel? = null
        rule.setContent { LevelEditorView(nested, { saved = it }, {}, { _, _ -> }) }
        rule.onNodeWithText("Enemy Spawns").performClick()
        rule.onNodeWithTag("spawn_loop_header_outer").assertExists()
        rule.onNodeWithTag("spawn_loop_header_inner").assertExists()
        rule.onNodeWithText("Total iterations (at least 1)").assertDoesNotExist()
        rule.onNodeWithText("Save Level").performClick()
        assertEquals(nested.spawnTimeline(), assertNotNull(saved).spawnTimeline())
    }

    @Test
    fun invalidRepeatCountBlocksSaving() {
        var saved: EditorLevel? = null
        rule.setContent { LevelEditorView(loopLevel(listOf(EditorSpawnLoop("wave", 1, 3))), { saved = it }, {}, { _, _ -> }) }
        rule.onNodeWithText("Enemy Spawns").performClick()
        rule.onNodeWithText("Loop wave").performClick()
        rule.onNodeWithText("Total iterations (at least 1)").performTextReplacement("invalid")
        rule.onNodeWithText("Save Level").assertIsNotEnabled()
        rule.onNodeWithText("Save As New").assertIsNotEnabled()
        rule.onNodeWithText("The total iteration count must be a positive whole number.").assertExists()
        assertNull(saved)
    }

    @Test
    fun loopOptionsDialogPersistsUnitIdAndFirstIterationFlag() {
        val spawn = EditorEnemySpawn(AttackerType.GOBLIN, spawnTurn = 1, unitId = "scout", firstIterationOnly = true)
        var applied: Pair<String?, Boolean>? = null
        rule.setContent { SpawnLoopOptionsDialog(spawn, setOf("other"), {}, { id, first -> applied = id to first }) }
        rule.onNodeWithText("Unit ID (optional, unique)").performTextReplacement("leader")
        rule.onNode(isToggleable()).assertIsOn()
        rule.onNodeWithText("Save").performClick()
        assertEquals("leader" to true, applied)
    }

    @Test
    fun villainLoopOptionsAreFixedToItsNameAndFirstIteration() {
        val spawn = EditorEnemySpawn(AttackerType.EWHAD, spawnTurn = 1, unitId = "boss", firstIterationOnly = false)
        var applied: Pair<String?, Boolean>? = null
        rule.setContent { SpawnLoopOptionsDialog(spawn, emptySet(), {}, { id, first -> applied = id to first }) }
        rule.onNodeWithText("Ewhad").assertIsNotEnabled()
        rule.onNode(isToggleable()).assertIsOn().assertIsNotEnabled()
        rule.onNodeWithText("Save").performClick()
        assertEquals("Ewhad" to true, applied)
    }

    @Test
    fun conditionAndInfiniteModesAreEditableAndSaved() {
        val boss = EditorEnemySpawn(AttackerType.EWHAD, spawnTurn = 1)
        var saved: EditorLevel? = null
        rule.setContent {
            LevelEditorView(loopLevel(listOf(EditorSpawnLoop("boss_loop", 1, 1)), listOf(boss), maxTurn = 1), { saved = it }, {}, { _, _ -> })
        }
        rule.onNodeWithText("Enemy Spawns").performClick()
        rule.onNodeWithText("Loop boss_loop").performClick()
        rule.onNodeWithText("While unit alive").performClick()
        rule.onNodeWithText("Save Level").assertIsNotEnabled()
        rule.onNodeWithText("Target unit ID").performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithText("Ewhad (Ewhad)").performClick()
        rule.onNodeWithText("Save Level").performClick()
        val condition = assertNotNull(saved?.spawnGroups).single() as SpawnGroup
        assertEquals(SpawnRepeatMode.CONDITION, condition.repeatMode)
        assertEquals(SpawnCondition.UNIT_ALIVE, condition.condition)
        assertEquals("Ewhad", condition.targetUnitId)
        assertEquals("Ewhad", condition.directTurns.single().spawns.single().unitId)
        rule.onNodeWithText("Infinite").performClick()
        // An infinite loop without a "Stop spawn loop" event makes the level invalid.
        rule.onNodeWithText("Save Level").assertIsNotEnabled()
        rule.onNodeWithTag("enemy_spawns_tab_issue_dot").performClick()
        rule.onAllNodesWithText("Every infinite loop must be stopped by an event", substring = true).onFirst().assertExists()
        rule.onNodeWithTag("events_tab_issue_dot").performClick()
        rule.onNodeWithText("These infinite spawn loops are never stopped: boss_loop", substring = true).assertExists()
    }

    @Test
    fun infiniteLoopStoppedByEventIsValidAndSelectableInEvents() {
        val stop = EventAction(type = EventActionType.STOP_SPAWN_LOOP, spawnLoopId = "endless")
        val events = LevelEvents(listOf(LevelEvent("stop", EventCondition(EventConditionType.TURN_START, fromTurn = 5), actions = listOf(stop))))
        val level =
            loopLevel(listOf(EditorSpawnLoop("endless", 1, 3, repeatMode = SpawnRepeatMode.INFINITE)))
                .copy(events = events)
        var saved: EditorLevel? = null
        rule.setContent { LevelEditorView(level, { saved = it }, {}, { _, _ -> }) }
        rule.onNodeWithTag("enemy_spawns_tab_issue_dot").assertDoesNotExist()
        rule.onNodeWithTag("events_tab_issue_dot").assertDoesNotExist()
        rule.onNodeWithText("Save Level").performClick()
        assertEquals(SpawnRepeatMode.INFINITE, (saved?.spawnGroups?.single() as SpawnGroup).repeatMode)
        assertEquals(events, saved?.events)

        rule.onNodeWithText("Events").performClick()
        rule.onNodeWithText("Stop spawn loop: endless", substring = true).performClick()
        rule.onNodeWithText("Spawn loop").performSemanticsAction(SemanticsActions.OnClick)
        rule.onAllNodesWithText("endless (Infinite)").assertCountEquals(2) // selected value and menu entry
    }
}
