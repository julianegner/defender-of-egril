package de.egril.defender.ui.infopage

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.audio_background_music_streamer_notice_intro
import defender_of_egril.composeapp.generated.resources.streamer_info_copyable_notice_label
import defender_of_egril.composeapp.generated.resources.streamer_info_copyable_notice_text
import defender_of_egril.composeapp.generated.resources.streamer_info_title
import org.junit.Rule
import org.junit.Test

class StreamerInfoTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun audioLicensesShowsStreamerNoticeAboveBackgroundMusicList() {
        var introText = ""
        var noticeText = ""

        composeTestRule.setContent {
            introText = stringResource(Res.string.audio_background_music_streamer_notice_intro)
            noticeText = stringResource(Res.string.streamer_info_copyable_notice_text)
            AudioLicensesInfo()
        }

        composeTestRule.onNodeWithText(introText).assertExists()
        composeTestRule.onNodeWithText(noticeText).assertExists()
    }

    @Test
    fun streamerInfoTabShowsCopyableNotice() {
        var titleText = ""
        var labelText = ""
        var noticeText = ""

        composeTestRule.setContent {
            titleText = stringResource(Res.string.streamer_info_title)
            labelText = stringResource(Res.string.streamer_info_copyable_notice_label)
            noticeText = stringResource(Res.string.streamer_info_copyable_notice_text)
            StreamerInfo()
        }

        composeTestRule.onNodeWithText(titleText).assertExists()
        composeTestRule.onNodeWithText(labelText).assertExists()
        composeTestRule.onNodeWithText(noticeText).assertExists()
    }
}
