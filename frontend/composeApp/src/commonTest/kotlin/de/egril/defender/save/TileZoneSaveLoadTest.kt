package de.egril.defender.save

import de.egril.defender.model.ActiveEventLoop
import de.egril.defender.model.AttackerType
import de.egril.defender.model.EventLoopFrame
import de.egril.defender.model.GamePhase
import de.egril.defender.model.Position
import de.egril.defender.model.Raft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Save/load round trips for tile zones, event loops, raft health and submerged units. */
class TileZoneSaveLoadTest {
    private fun savedGame(
        activeTileZoneIds: List<String> = emptyList(),
        activeEventLoops: List<ActiveEventLoop> = emptyList(),
        rafts: List<SavedRaft> = emptyList(),
        attackers: List<SavedAttacker> = emptyList(),
    ) = SavedGame(
        id = "zone-save",
        timestamp = 1L,
        levelId = 3,
        levelName = "Tide Level",
        turnNumber = 9,
        coins = 50,
        healthPoints = 7,
        phase = GamePhase.PLAYER_TURN,
        defenders = emptyList(),
        attackers = attackers,
        nextDefenderId = 1,
        nextAttackerId = 3,
        currentWaveIndex = 0,
        spawnCounter = 0,
        attackersToSpawn = emptyList(),
        fieldEffects = emptyList(),
        traps = emptyList(),
        rafts = rafts,
        activeTileZoneIds = activeTileZoneIds,
        activeEventLoops = activeEventLoops,
    )

    @Test
    fun zoneAndLoopStateRoundTrip() {
        val original =
            savedGame(
                activeTileZoneIds = listOf("tide", "new_river"),
                activeEventLoops =
                    listOf(
                        ActiveEventLoop(
                            eventId = "tides",
                            frames =
                                listOf(
                                    EventLoopFrame(stepIndex = 1, turnsRemaining = 0, iterationsDone = 2, stepExecuted = true),
                                    EventLoopFrame(stepIndex = 0, turnsRemaining = 3, iterationsDone = 1, stepExecuted = false),
                                ),
                        ),
                        ActiveEventLoop(eventId = "other", frames = listOf(EventLoopFrame(stepIndex = 0, turnsRemaining = 1))),
                    ),
                rafts = listOf(SavedRaft(id = 1, defenderId = 2, position = Position(3, 4), healthPoints = 110)),
                attackers =
                    listOf(
                        SavedAttacker(1, AttackerType.TROLL, Position(2, 0), 1, 90, false, isSubmerged = true),
                        SavedAttacker(2, AttackerType.GOBLIN, Position(5, 0), 1, 20, false),
                    ),
            )

        val loaded = SaveJsonSerializer.deserializeSavedGame(SaveJsonSerializer.serializeSavedGame(original))

        assertNotNull(loaded)
        assertEquals(original.activeTileZoneIds, loaded.activeTileZoneIds)
        assertEquals(original.activeEventLoops, loaded.activeEventLoops)
        assertEquals(original.rafts, loaded.rafts)
        assertTrue(loaded.attackers.first { it.id == 1 }.isSubmerged)
        assertFalse(loaded.attackers.first { it.id == 2 }.isSubmerged)
        assertEquals(7, loaded.healthPoints, "Raft health must not be confused with the player's health")
    }

    @Test
    fun oldSavesLoadWithDefaults() {
        val json =
            SaveJsonSerializer
                .serializeSavedGame(savedGame(rafts = listOf(SavedRaft(id = 1, defenderId = 2, position = Position(3, 4)))))
                .replace(Regex(""",\s*"healthPoints": 150"""), "")
                .replace(Regex(""""activeTileZoneIds": \[[^\]]*\],\s*"""), "")
                .replace(Regex(""""activeEventLoops": \[[^\]]*\],\s*"""), "")

        val loaded = SaveJsonSerializer.deserializeSavedGame(json)

        assertNotNull(loaded)
        assertEquals(Raft.RAFT_MAX_HEALTH, loaded.rafts.single().healthPoints)
        assertTrue(loaded.activeTileZoneIds.isEmpty())
        assertTrue(loaded.activeEventLoops.isEmpty())
    }
}
