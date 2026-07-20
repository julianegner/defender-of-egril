package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.editor.EditorLevel
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.editor.level.LevelEditorView
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SandboxLevelEditorSupportsTabTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setDefaultLanguage() {
        currentLanguage.value = AppLocale.DEFAULT
    }

    @Test
    fun sandboxLevelCanOpenSupportsTabWithoutEnemySpawns() {
        composeTestRule.setContent {
            LevelEditorView(
                level =
                    EditorLevel(
                        id = "sandbox_level",
                        mapId = "map_30x8",
                        title = "Sandbox Level",
                        startCoins = 100,
                        startHealthPoints = 10,
                        enemySpawns = emptyList(),
                        availableTowers = setOf(DefenderType.SPIKE_TOWER),
                        isSandbox = true,
                    ),
                onSave = {},
                onCancel = {},
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Enemy Spawns", substring = true, ignoreCase = true)
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText("Supports", substring = true, ignoreCase = true)
            .assertExists()
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Placable Objects", substring = true, ignoreCase = true)
            .assertExists()
    }
}
