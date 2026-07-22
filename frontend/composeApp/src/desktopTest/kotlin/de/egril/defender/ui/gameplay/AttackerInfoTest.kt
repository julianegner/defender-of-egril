package de.egril.defender.ui.gameplay

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AttackerInfoTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setDefaultLanguage() {
        currentLanguage.value = AppLocale.DEFAULT
    }

    @Test
    fun ewhadShowsVillainDescription() {
        val attacker =
            Attacker(
                id = 1,
                type = AttackerType.EWHAD,
                position = mutableStateOf(Position(0, 0)),
            )

        composeTestRule.setContent {
            MaterialTheme {
                AttackerInfo(attacker = attacker)
            }
        }

        composeTestRule
            .onNodeWithText("He summons increasingly powerful Demons every turn", substring = true)
            .assertIsDisplayed()
    }
}
