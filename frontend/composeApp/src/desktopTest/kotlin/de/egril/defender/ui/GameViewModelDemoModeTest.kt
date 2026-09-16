package de.egril.defender.ui

import de.egril.defender.editor.EditorStorage
import de.egril.defender.game.DemoMode
import de.egril.defender.model.AttackerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameViewModelDemoModeTest {
    @Test
    fun `demodemo setup uses level file tower bases`() {
        EditorStorage.ensureInitialized()

        val setup = DemoMode.createDemoLevelSetup(0, DemoMode.Scenario.DEMO_DEMO)
        val level = assertNotNull(setup).level
        val initialData = level.getEffectiveInitialData()

        assertEquals("demo_demo", level.editorLevelId)
        assertEquals("map_demo", level.mapId)
        assertEquals(100, level.healthPoints)
        assertTrue(setup.initialTowers.isNotEmpty())
        assertTrue(setup.initialTowers.all { it.onTowerBase })
        assertTrue(initialData.defenders.isEmpty(), "Demo runtime level should start without pre-placed defenders")
        assertTrue(initialData.barricades.count { it.supportsTower } >= 20, "Demo level should provide many tower-base barricades")
        val spawnTypes = level.directSpawnPlan.orEmpty().map { it.attackerType }.toSet()
        assertEquals(
            setOf(
                AttackerType.GOBLIN,
                AttackerType.SKELETON,
                AttackerType.ORK,
                AttackerType.SNOTLING_BOSS,
                AttackerType.EVIL_WIZARD,
                AttackerType.GREEN_WITCH,
                AttackerType.BARON_RATTERZAHN,
                AttackerType.OGRE,
                AttackerType.ZOMBIE,
                AttackerType.BLUE_DEMON,
                AttackerType.RED_DEMON,
                AttackerType.RED_WITCH,
                AttackerType.FALLEN_SHIELDMAIDEN_FREYA,
                AttackerType.PRINCE_VALERIUS_THE_SOULREAPER,
                AttackerType.MORGUK_BONEWHISPER,
            ),
            spawnTypes,
        )
    }

    @Test
    fun `demodemo cheat starts map demo level`() =
        runBlocking {
            val viewModel = GameViewModel()
            viewModel.navigateToWorldMap()

            val result = viewModel.applyWorldMapCheatCode("demodemo")
            val state = assertNotNull(viewModel.gameState.first())

            assertTrue(result)
            assertTrue(viewModel.isDemoMode.first())
            assertEquals(Screen.GamePlay(state.level.id), viewModel.currentScreen.first())
            assertEquals("demo_demo", state.level.editorLevelId)
            assertEquals("map_demo", state.level.mapId)
            assertEquals(100, state.healthPoints.value)
            assertTrue(state.defenders.isEmpty(), "Towers should still be placed by the automated player")
            assertTrue(state.level.getEffectiveInitialData().barricades.count { it.canSupportTower() } >= 20)

            viewModel.stopDemoMode()
        }
}
