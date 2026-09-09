package de.egril.defender.model

import de.egril.defender.editor.InitialBridge
import de.egril.defender.editor.InitialData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InitialBridgeSetupTest {
    @Test
    fun initializePrePlacedElementsAddsConfiguredBridges() {
        val level =
            Level(
                id = 1,
                name = "Bridge Init",
                pathCells = setOf(Position(0, 0), Position(1, 0), Position(3, 0), Position(4, 0)),
                attackerWaves = emptyList(),
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(4, 0)),
                riverTiles = mapOf(Position(2, 0) to RiverTile(Position(2, 0))),
                initialData =
                    InitialData(
                        bridges =
                            listOf(
                                InitialBridge(
                                    position = Position(2, 0),
                                    type = BridgeType.STONE,
                                    healthPoints = 123,
                                    isIndestructible = true,
                                ),
                            ),
                    ),
            )

        val state = GameState(level)
        state.initializePrePlacedElements()

        assertEquals(1, state.bridges.size)
        val bridge = state.bridges.first()
        assertEquals(BridgeType.STONE, bridge.type)
        assertEquals(123, bridge.currentHealth.value)
        assertTrue(bridge.isIndestructible)
        assertTrue(state.isBridgeAt(Position(2, 0)))
    }
}
