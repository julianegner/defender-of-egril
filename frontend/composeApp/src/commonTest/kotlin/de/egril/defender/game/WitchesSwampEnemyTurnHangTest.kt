package de.egril.defender.game

import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.RepositoryLoader
import de.egril.defender.model.GamePhase
import de.egril.defender.model.GameState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WitchesSwampEnemyTurnHangTest {
    // Loads the level and map straight from repository resources. On WASM the root-level
    // sequence.json is not readable in the test browser, so the sequence-based init cannot be used.
    @Test
    fun `witches swamp enemy turns complete through turn 6`() = runTest {
        val editorLevel = assertNotNull(RepositoryLoader.loadLevel("the_witches_swamp"))
        val editorMap = assertNotNull(RepositoryLoader.loadMap(editorLevel.mapId))
        val gameLevel = assertNotNull(EditorStorage.convertToGameLevel(editorLevel, 9100, editorMap))
        val state = GameState(level = gameLevel)
        state.initializePrePlacedElements()
        val engine = GameEngine(state)

        engine.startFirstPlayerTurn()
        repeat(6) { round ->
            val turn = round + 1
            engine.startEnemyTurn()
            val movements = engine.calculateEnemyTurnMovements()
            for (step in movements.allMovementSteps) {
                for ((id, pos) in step) engine.applyMovement(id, pos)
            }
            movements.attackersStoppedByBarricade.forEach { engine.attackBarricade(it.second, it.first) }
            engine.spawnEnemyTurnAttackers()
            for (step in engine.calculateNewlySpawnedMovements()) {
                for ((id, pos) in step) engine.applyMovement(id, pos)
            }
            engine.completeEnemyTurn()
            assertEquals(GamePhase.PLAYER_TURN, state.phase.value, "enemy turn $turn did not complete")
        }
    }
}
