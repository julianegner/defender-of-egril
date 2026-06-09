package de.egril.defender.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.ui.infopage.NewVersionInfo
import de.egril.defender.ui.settings.AppSettings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class NewVersionDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun newVersionDialogShowsShortcutHintsOnlyWhenEnabled() {
        AppSettings.resetToDefaults()
        try {
            currentLanguage.value = AppLocale.DEFAULT
            composeTestRule.setContent {
                NewVersionDialog(
                    info = NewVersionInfo(version = "9.9.9", releasePageUrl = "https://example.invalid/releases"),
                    onDismiss = {}
                )
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Go to Download", substring = true, ignoreCase = true).assertExists()
            composeTestRule.onNodeWithText("Close", substring = true, ignoreCase = true).assertExists()
            composeTestRule.onNodeWithText("Enter").assertDoesNotExist()
            composeTestRule.onNodeWithText("Esc").assertDoesNotExist()

            composeTestRule.runOnIdle {
                AppSettings.showButtonShortcutHints.value = true
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Go to Download", substring = true, ignoreCase = true).assertExists()
            composeTestRule.onNodeWithText("Close", substring = true, ignoreCase = true).assertExists()
            composeTestRule.onNodeWithText("Enter").assertExists()
            composeTestRule.onNodeWithText("Esc").assertExists()
        } finally {
            AppSettings.resetToDefaults()
        }
    }

    @Test
    fun newVersionDialogButtonsOpenAndCloseIt() {
        AppSettings.resetToDefaults()
        try {
            currentLanguage.value = AppLocale.DEFAULT
            var openedUrl: String? = null
            var dismissCount = 0
            val releaseInfo = NewVersionInfo(
                version = "9.9.9",
                releasePageUrl = "https://example.invalid/releases"
            )

            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalUriHandler provides object : UriHandler {
                        override fun openUri(uri: String) {
                            openedUrl = uri
                        }
                    }
                ) {
                    NewVersionDialog(
                        info = releaseInfo,
                        onDismiss = { dismissCount++ }
                    )
                }
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("newVersionDialog").assertExists()
            composeTestRule.onNodeWithText("Go to Download", substring = true, ignoreCase = true)
                .performClick()
            composeTestRule.waitForIdle()

            assertEquals(releaseInfo.releasePageUrl, openedUrl)
            assertEquals(1, dismissCount)

            openedUrl = null
            dismissCount = 0
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalUriHandler provides object : UriHandler {
                        override fun openUri(uri: String) {
                            openedUrl = uri
                        }
                    }
                ) {
                    NewVersionDialog(
                        info = releaseInfo,
                        onDismiss = { dismissCount++ }
                    )
                }
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("newVersionDialog").assertExists()
            composeTestRule.onNodeWithText("Close", substring = true, ignoreCase = true)
                .performClick()
            composeTestRule.waitForIdle()

            assertEquals(null, openedUrl)
            assertEquals(1, dismissCount)
        } finally {
            AppSettings.resetToDefaults()
        }
    }
}
