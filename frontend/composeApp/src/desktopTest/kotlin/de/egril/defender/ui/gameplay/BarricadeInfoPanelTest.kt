package de.egril.defender.ui.gameplay

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.model.Barricade
import de.egril.defender.model.Position
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Verifies that the barricade info panel distinguishes a plain barricade from one that
 * also serves as a tower base (HP >= 100) via its title and description (issue #627).
 */
class BarricadeInfoPanelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setDefaultLanguage() {
        currentLanguage.value = AppLocale.DEFAULT
    }

    private fun barricade(hp: Int) =
        Barricade(
            id = 1,
            position = Position(0, 0),
            healthPoints = mutableStateOf(hp),
            defenderId = 1,
        )

    @Test
    fun plainBarricadeShowsBarricadeTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                BarricadeInfoPanel(
                    position = Position(0, 0),
                    barricade = barricade(50),
                    onRemove = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Barricade").assertIsDisplayed()
    }

    @Test
    fun towerBaseBarricadeShowsTowerBaseTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                BarricadeInfoPanel(
                    position = Position(0, 0),
                    barricade = barricade(100),
                    onRemove = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Barricade - Tower Base").assertIsDisplayed()
    }
}
