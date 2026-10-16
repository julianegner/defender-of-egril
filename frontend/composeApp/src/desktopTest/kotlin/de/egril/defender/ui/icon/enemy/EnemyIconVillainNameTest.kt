package de.egril.defender.ui.icon.enemy

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Verifies that villains display their (short) name where regular enemies show their
 * health points, and that regular enemies still show their health number.
 */
class EnemyIconVillainNameTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setDefaultLanguage() {
        currentLanguage.value = AppLocale.DEFAULT
    }

    @Test
    fun villainShowsShortNameInsteadOfHealth() {
        val garokk =
            Attacker(
                id = 1,
                type = AttackerType.GAROKK,
                position = mutableStateOf(Position(0, 0)),
            )

        composeTestRule.setContent {
            EnemyIcon(attacker = garokk, modifier = Modifier.size(64.dp))
        }

        // The villain's short name is displayed; the raw health number is not.
        composeTestRule.onNodeWithText("Garokk").assertIsDisplayed()
        composeTestRule.onNodeWithText("${garokk.currentHealth.value}").assertDoesNotExist()
    }

    @Test
    fun regularEnemyShowsHealthNumber() {
        val goblin =
            Attacker(
                id = 2,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(0, 0)),
            )

        composeTestRule.setContent {
            EnemyIcon(attacker = goblin, modifier = Modifier.size(64.dp))
        }

        composeTestRule.onNodeWithText("${goblin.currentHealth.value}").assertIsDisplayed()
    }
}
