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
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import de.egril.defender.model.TileZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** JSON round trips for tile zones (maps) and event loops / zone actions (levels). */
class TileZoneSerializationTest {
    @Test
    fun mapTileZonesRoundTrip() {
        val zone =
            TileZone(
                id = "tide",
                name = "High tide",
                tiles = mapOf(Position(1, 0) to TileType.RIVER, Position(2, 0) to TileType.PATH),
                riverTiles = mapOf(Position(1, 0) to RiverTile(position = Position(1, 0), flowDirection = RiverFlow.SOUTH_WEST, flowSpeed = 2)),
            )
        val map =
            EditorMap(
                id = "zone_map",
                name = "Zone Map",
                width = 3,
                height = 2,
                tiles = mapOf("0,0" to TileType.SPAWN_POINT, "1,0" to TileType.PATH, "2,0" to TileType.RIVER, "2,1" to TileType.TARGET),
                riverTiles = mapOf("2,0" to RiverTile(position = Position(2, 0), flowDirection = RiverFlow.EAST, flowSpeed = 1)),
                tileZones = listOf(zone, TileZone(id = "empty")),
            )

        val loaded = EditorJsonSerializer.deserializeMap(EditorJsonSerializer.serializeMap(map))

        assertNotNull(loaded)
        assertEquals(map.tileZones, loaded.tileZones)
        assertEquals(map.tiles, loaded.tiles, "Zone tiles must not leak into the base map tiles")
        assertEquals(map.riverTiles, loaded.riverTiles, "Zone rivers must not leak into the base map rivers")
    }

    @Test
    fun eventLoopsAndZoneActionsRoundTrip() {
        val inner =
            EventLoop(
                steps = listOf(EventLoopStep(waitTurns = 1, actions = listOf(EventAction(type = EventActionType.GIVE_COINS, amount = 5)))),
                repeatCount = 3,
            )
        val loop =
            EventLoop(
                steps =
                    listOf(
                        EventLoopStep(
                            waitTurns = 2,
                            actions = listOf(EventAction(type = EventActionType.APPLY_TILE_ZONE, zoneId = "tide")),
                            messageKey = "event_msg_reinforcements",
                        ),
                        EventLoopStep(
                            waitTurns = 0,
                            actions = listOf(EventAction(type = EventActionType.REVERT_TILE_ZONE, zoneId = "tide")),
                            nestedLoop = inner,
                        ),
                    ),
                repeatCount = 0,
            )
        val events =
            LevelEvents(
                listOf(
                    LevelEvent(
                        id = "tides",
                        condition = EventCondition(type = EventConditionType.TURN_START, fromTurn = 3),
                        actions = listOf(EventAction(type = EventActionType.TOGGLE_TILE_ZONE, zoneId = "tide")),
                        messageKey = "event_msg_treasure",
                        loop = loop,
                    ),
                    LevelEvent(
                        id = "calm",
                        condition = EventCondition(type = EventConditionType.ENEMIES_KILLED, threshold = 10),
                        actions = listOf(EventAction(type = EventActionType.STOP_EVENT_LOOP, targetEventId = "tides")),
                    ),
                ),
            )
        val level =
            EditorLevel(
                id = "loop_level",
                mapId = "zone_map",
                title = "Loop Level",
                startCoins = 100,
                enemySpawns = listOf(EditorEnemySpawn(AttackerType.GOBLIN, 1, 1)),
                availableTowers = setOf(DefenderType.SPIKE_TOWER),
                events = events,
            )

        val loaded = EditorJsonSerializer.deserializeLevel(EditorJsonSerializer.serializeLevel(level))

        assertNotNull(loaded)
        assertEquals(events, loaded.events)
    }
}
