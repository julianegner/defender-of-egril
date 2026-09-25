package de.egril.defender.ui.infopage

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class StreamerInfoTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun audioLicensesShowsStreamerNoticeAboveBackgroundMusicList() {
        composeTestRule.setContent {
            AudioLicensesInfo()
        }

        composeTestRule.onNodeWithText("The following notice applies to the Fesliyan Studios background music tracks used in the game:").assertExists()
        composeTestRule.onNodeWithText("Fesliyan Studios policy: https://www.fesliyanstudios.com/policy", substring = true).assertExists()
    }

    @Test
    fun streamerInfoTabShowsCopyableNotice() {
        composeTestRule.setContent {
            StreamerInfo()
        }

        composeTestRule.onNodeWithText("Infos for Streamers").assertExists()
        composeTestRule.onNodeWithText("Copyable notice").assertExists()
        composeTestRule.onNodeWithText("fair use\" gameplay footage.", substring = true).assertExists()
    }
}
