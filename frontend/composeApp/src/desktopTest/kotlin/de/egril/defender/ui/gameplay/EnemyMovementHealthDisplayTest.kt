package de.egril.defender.ui.gameplay

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import org.junit.Rule
import org.junit.Test

class EnemyMovementHealthDisplayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun enteringAnEmptyTileShowsActualHealthImmediately() {
        val attacker =
            Attacker(
                id = 1,
                type = AttackerType.GOBLIN,
                position = mutableStateOf(Position(0, 0)),
            )
        val occupant = mutableStateOf<Attacker?>(null)
        composeTestRule.setContent {
            Text(rememberDisplayedEnemyHealth(occupant.value).value.toString())
        }

        composeTestRule.runOnIdle { occupant.value = attacker }
        composeTestRule.onNodeWithText(attacker.type.health.toString()).assertExists()
        composeTestRule.onNodeWithText("0").assertDoesNotExist()

        // Health changes on the same tile are still controlled by the delayed hit animation.
        composeTestRule.runOnIdle { attacker.currentHealth.value = 12 }
        composeTestRule.onNodeWithText(attacker.type.health.toString()).assertExists()

        composeTestRule.runOnIdle { occupant.value = null }
        composeTestRule.runOnIdle {
            attacker.position.value = Position(1, 0)
            occupant.value = attacker
        }
        composeTestRule.onNodeWithText("12").assertExists()
        composeTestRule.onNodeWithText("0").assertDoesNotExist()
    }
}
