package de.egril.defender.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import de.egril.defender.ui.ScreenshotTestUtils
import org.junit.Rule
import org.junit.Test

class SpeechBubbleScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    private fun bubbles() {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(24.dp)) {
            Box(modifier = Modifier.padding(8.dp)) {
                SpeechBubble(pointer = SpeechBubblePointer.UP, pointerOffset = 20.dp) {
                    Text("The remaining enemies aren't mighty enough to threaten your stronghold.")
                }
            }
            Box(modifier = Modifier.padding(8.dp)) {
                SpeechBubble(pointer = SpeechBubblePointer.DOWN) {
                    Text("Pointer down")
                }
            }
            Box(modifier = Modifier.padding(8.dp)) {
                SpeechBubble(pointer = SpeechBubblePointer.LEFT) {
                    Text("Pointer left")
                }
            }
            Box(modifier = Modifier.padding(8.dp)) {
                SpeechBubble(pointer = SpeechBubblePointer.RIGHT) {
                    Text("Pointer right")
                }
            }
        }
    }

    @Test
    fun renderLight() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) { bubbles() }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().assertExists()
        composeTestRule.onNodeWithText("Pointer down").assertExists()
        composeTestRule.onNodeWithText("Pointer left").assertExists()
        composeTestRule.onNodeWithText("Pointer right").assertExists()
        ScreenshotTestUtils.captureScreenshot(composeTestRule, "speech_bubble_light")
    }

    @Test
    fun renderDark() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) { bubbles() }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().assertExists()
        composeTestRule.onNodeWithText("Pointer down").assertExists()
        ScreenshotTestUtils.captureScreenshot(composeTestRule, "speech_bubble_dark")
    }

    @Test
    fun closeButtonDismissesBubble() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                var visible by remember { mutableStateOf(true) }
                if (visible) {
                    SpeechBubble(
                        pointer = SpeechBubblePointer.UP,
                        onClose = { visible = false },
                        closeContentDescription = "Close",
                    ) {
                        Text("Closable bubble")
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Closable bubble").assertExists()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Closable bubble").assertDoesNotExist()
    }
}
