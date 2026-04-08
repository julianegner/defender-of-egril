package de.egril.defender.audio

import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for Android sound manager factory
 */
class SoundManagerAndroidTest {
    
    @Test
    fun `createSoundManager returns FileSoundManager instance`() {
        val soundManager = createSoundManager()
        assertNotNull(soundManager)
        assertTrue(soundManager is FileSoundManager)
    }
}
