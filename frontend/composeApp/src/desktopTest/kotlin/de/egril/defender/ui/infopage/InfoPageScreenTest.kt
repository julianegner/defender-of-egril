package de.egril.defender.ui.infopage

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hyperether.resources.stringResource
import de.egril.defender.ui.ScreenshotTestUtils
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.info_tab_streamer_info
import defender_of_egril.composeapp.generated.resources.streamer_info_copyable_notice_text
import org.junit.Rule
import org.junit.Test

class InfoPageScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun infoPageRendersAndCapturesScreenshot() {
        composeTestRule.setContent {
            InfoPageScreen(
                onBack = {},
            )
        }

        composeTestRule.waitForIdle()

        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "info-page-screen",
            width = 1200,
            height = 800,
        )
    }

    @Test
    fun infoPageCanOpenStreamerInfoTab() {
        var tabLabel = ""
        var noticeText = ""

        composeTestRule.setContent {
            tabLabel = stringResource(Res.string.info_tab_streamer_info)
            noticeText = stringResource(Res.string.streamer_info_copyable_notice_text)

            InfoPageScreen(
                onBack = {},
                initialTab = InfoTab.STREAMER_INFO,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText(tabLabel).assertCountEquals(2)
        composeTestRule.onAllNodesWithText(tabLabel).assertCountEquals(2)
        composeTestRule.onNodeWithText(noticeText).assertExists()
    }
}
