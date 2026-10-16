package de.egril.defender.ui.hexagon

import de.egril.defender.editor.TileType
import de.egril.defender.game.TileZoneSystem
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.model.TileZone
import kotlin.test.Test
import kotlin.test.assertEquals

class HexagonMinimapTileZoneTest {
    @Test
    fun minimapUsesCurrentTileZoneState() {
        val zonePosition = Position(1, 0)
        val level =
            Level(
                id = 1,
                name = "Zone test",
                gridWidth = 3,
                gridHeight = 1,
                startPositions = listOf(Position(0, 0)),
                targetPositions = listOf(Position(2, 0)),
                pathCells = setOf(zonePosition),
                attackerWaves = emptyList(),
                tileZones = listOf(TileZone(id = "flood", tiles = mapOf(zonePosition to TileType.RIVER))),
            )
        val gameState = GameState(level)
        val minimapBaseType = TileType.PATH

        assertEquals(minimapBaseType, resolveMinimapTileType(minimapBaseType, zonePosition, gameState))

        val zoneSystem = TileZoneSystem(gameState)
        zoneSystem.applyZone("flood")
        assertEquals(TileType.RIVER, resolveMinimapTileType(minimapBaseType, zonePosition, gameState))

        zoneSystem.revertZone("flood")
        assertEquals(minimapBaseType, resolveMinimapTileType(minimapBaseType, zonePosition, gameState))
    }
}
