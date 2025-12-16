package de.egril.defender.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.ui.settings.DualLabelSwitch
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the DualLabelSwitch component.
 */
class DualLabelSwitchTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testDualLabelSwitchShowsBothLabels() {
        val state = mutableStateOf(false)
        
        composeTestRule.setContent {
            DualLabelSwitch(
                state = state,
                leftText = "Left Label",
                rightText = "Right Label",
                onCheckedChange = { }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Both labels should be visible
        composeTestRule.onNodeWithText("Left Label")
            .assertExists()
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Right Label")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun testDualLabelSwitchToggles() {
        val state = mutableStateOf(false)
        var changedValue: Boolean? = null
        
        composeTestRule.setContent {
            DualLabelSwitch(
                state = state,
                leftText = "Off State",
                rightText = "On State",
                onCheckedChange = { changedValue = it }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Click the switch
        composeTestRule.onNode(hasClickAction())
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify state changed and callback was invoked
        assert(state.value) { "State should be true after toggle" }
        assert(changedValue == true) { "Callback should have been invoked with true" }
    }
    
    @Test
    fun testDualLabelSwitchWithSpecificLabels() {
        val state = mutableStateOf(true)
        
        composeTestRule.setContent {
            DualLabelSwitch(
                state = state,
                leftText = "Image Map View",
                rightText = "Level Cards View",
                onCheckedChange = { }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Verify both specific labels are shown
        composeTestRule.onNodeWithText("Image Map View")
            .assertExists()
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Level Cards View")
            .assertExists()
            .assertIsDisplayed()
    }
}
