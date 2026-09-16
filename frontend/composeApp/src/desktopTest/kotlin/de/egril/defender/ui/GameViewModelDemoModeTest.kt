package de.egril.defender.ui

import de.egril.defender.editor.EditorStorage
import de.egril.defender.game.DemoMode
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameMessage
import de.egril.defender.model.GameMessageType
import de.egril.defender.model.GameState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameViewModelDemoModeTest {
    @Test
    fun `demodemo level stays official when loaded from storage`() {
        EditorStorage.ensureInitialized()

        val level = assertNotNull(EditorStorage.getLevel(DemoMode.DEMO_DEMO_LEVEL_ID))

        assertTrue(level.isOfficial)
    }

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
            assertTrue(state.barricades.count { it.canSupportTower() } >= 20, "Runtime demo should initialize tower-base barricades")

            viewModel.stopDemoMode()
        }

    @Test
    fun `demodemo level data keeps initial towers linked to tower bases`() {
        EditorStorage.ensureInitialized()

        val editorLevel = assertNotNull(EditorStorage.getLevel(DemoMode.DEMO_DEMO_LEVEL_ID))
        val gameLevel = assertNotNull(EditorStorage.convertToGameLevel(editorLevel, 9001))
        val state = GameState(level = gameLevel)

        state.initializePrePlacedElements()

        assertTrue(state.defenders.isNotEmpty())
        assertTrue(state.defenders.all { it.towerBaseBarricadeId.value != null })
        assertTrue(state.barricades.count { it.hasTower() } == state.defenders.size)
    }

    @Test
    fun `demo mode auto dismisses villain messages after four seconds`() =
        runBlocking {
            val viewModel = GameViewModel()
            viewModel.navigateToWorldMap()
            assertTrue(viewModel.applyWorldMapCheatCode("demodemo"))
            val state = assertNotNull(viewModel.gameState.first())

            try {
                state.pendingMessages.add(
                    GameMessage(
                        type = GameMessageType.VILLAIN_ENTERS,
                        name = AttackerType.SNOTLING_BOSS.name,
                    ),
                )
                GameViewModel::class.java
                    .getDeclaredMethod("surfaceNextPendingMessageIfIdle")
                    .apply { isAccessible = true }
                    .invoke(viewModel)

                assertEquals(GameMessageType.VILLAIN_ENTERS, viewModel.pendingGameMessage.first()?.type)

                delay(DemoMode.VILLAIN_MESSAGE_DISMISS_DELAY_MS + 750L)

                assertNull(viewModel.pendingGameMessage.first())
            } finally {
                viewModel.stopDemoMode()
            }
        }
}
