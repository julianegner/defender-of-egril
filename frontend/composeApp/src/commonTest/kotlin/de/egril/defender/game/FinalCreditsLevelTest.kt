package de.egril.defender.game

import de.egril.defender.editor.OfficialContent
import de.egril.defender.model.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests that the final credits screen is triggered only after winning "the_final_stand"
 * level, not after the last level in the world levels list.
 *
 * Regression test for the bug where credits were shown after "the_tower_of_the_hermit"
 * because isLastLevel was computed as the last item in the world levels list.
 */
class FinalCreditsLevelTest {

    /** Mirrors the isLastLevel computation in GameViewModel.onLevelComplete(). */
    private fun isLastLevel(worldLevels: List<WorldLevel>, levelId: Int): Boolean {
        return worldLevels.firstOrNull { it.level.id == levelId }
            ?.level?.editorLevelId == OfficialContent.FINAL_LEVEL_ID
    }

    private fun makeLevel(id: Int, editorLevelId: String?): Level {
        return Level(
            id = id,
            name = "Level $id",
            pathCells = emptySet(),
            attackerWaves = emptyList(),
            editorLevelId = editorLevelId
        )
    }

    /**
     * Simulates the real sequence where "the_final_stand" is not the last level:
     * the sequence ends with levels that come AFTER the_final_stand
     * (e.g., the_tower_of_the_hermit).
     */
    @Test
    fun testFinalStandIsLastLevelWhenItIsTheCurrentLevel() {
        val worldLevels = listOf(
            WorldLevel(makeLevel(1, "the_first_wave")),
            WorldLevel(makeLevel(2, "the_final_stand")),
            WorldLevel(makeLevel(3, "the_tower_of_the_hermit"))
        )

        assertTrue(
            isLastLevel(worldLevels, levelId = 2),
            "Winning 'the_final_stand' should be treated as winning the final level"
        )
    }

    @Test
    fun testTowerOfTheHermitIsNotTheLastLevel() {
        val worldLevels = listOf(
            WorldLevel(makeLevel(1, "the_first_wave")),
            WorldLevel(makeLevel(2, "the_final_stand")),
            WorldLevel(makeLevel(3, "the_tower_of_the_hermit"))
        )

        assertFalse(
            isLastLevel(worldLevels, levelId = 3),
            "Winning 'the_tower_of_the_hermit' (last in list) must NOT trigger credits"
        )
    }

    @Test
    fun testNonFinalLevelBeforeFinalStandIsNotLastLevel() {
        val worldLevels = listOf(
            WorldLevel(makeLevel(1, "the_first_wave")),
            WorldLevel(makeLevel(2, "the_final_stand")),
            WorldLevel(makeLevel(3, "the_tower_of_the_hermit"))
        )

        assertFalse(
            isLastLevel(worldLevels, levelId = 1),
            "A regular level before the_final_stand should not trigger credits"
        )
    }
}
