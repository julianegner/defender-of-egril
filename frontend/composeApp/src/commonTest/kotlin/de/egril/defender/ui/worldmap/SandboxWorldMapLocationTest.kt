package de.egril.defender.ui.worldmap

import de.egril.defender.model.Level
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.WorldLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SandboxWorldMapLocationTest {
    @Test
    fun sandboxLevelsAreGroupedAtTheLowerRightWithAnImageMarker() {
        val location =
            createSandboxWorldMapLocation(
                listOf(
                    worldLevel(id = "regular", isSandbox = false),
                    worldLevel(id = "sandbox_one", isSandbox = true),
                    worldLevel(id = "sandbox_two", isSandbox = true),
                ),
            )

        assertEquals("sandbox_levels", location?.id)
        assertEquals(listOf("sandbox_one", "sandbox_two"), location?.levelIds)
        assertEquals(0.93f, location?.x)
        assertEquals(0.92f, location?.y)
        assertEquals("sandbox", location?.locationData?.nameKey)
        assertEquals("scroll", location?.locationData?.iconResourceName)
    }

    @Test
    fun noSandboxLocationIsCreatedWithoutSandboxLevels() {
        assertNull(createSandboxWorldMapLocation(listOf(worldLevel(id = "regular", isSandbox = false))))
    }

    private fun worldLevel(
        id: String,
        isSandbox: Boolean,
    ) = WorldLevel(
        level =
            Level(
                id = 1,
                name = id,
                pathCells = emptySet(),
                attackerWaves = emptyList(),
                editorLevelId = id,
                isSandbox = isSandbox,
            ),
        status = LevelStatus.UNLOCKED,
    )
}
