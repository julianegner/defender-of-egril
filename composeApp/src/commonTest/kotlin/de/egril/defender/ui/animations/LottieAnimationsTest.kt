package de.egril.defender.ui.animations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for Lottie animation integration
 */
class LottieAnimationsTest {
    
    @Test
    fun testAnimationTypeEnumExists() {
        // Verify that animation types are defined correctly
        val healingType = AnimationType.GREEN_WITCH_HEALING
        val damageType = AnimationType.BARRICADE_DAMAGE
        val freezeType = AnimationType.FREEZE_SPELL
        
        assertNotNull(healingType, "Green witch healing animation type should exist")
        assertNotNull(damageType, "Barricade damage animation type should exist")
        assertNotNull(freezeType, "Freeze spell animation type should exist")
    }
    
    @Test
    fun testAnimationTypeCount() {
        // Verify we have exactly 3 animation types as specified
        val animationTypes = AnimationType.values()
        assertEquals(3, animationTypes.size, "Should have exactly 3 animation types")
    }
}
