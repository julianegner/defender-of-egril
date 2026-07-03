package de.egril.defender.ui

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.egril.defender.model.AttackerType
import de.egril.defender.model.HexDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class EnemySpriteProviderTest {
    @Test
    fun spriteKeyIsLowercasedTypeName() {
        assertEquals("goblin", EnemySpriteProvider.spriteKey(AttackerType.GOBLIN))
        assertEquals("evil_wizard", EnemySpriteProvider.spriteKey(AttackerType.EVIL_WIZARD))
        assertEquals("dragon", EnemySpriteProvider.spriteKey(AttackerType.DRAGON))
    }

    @Test
    fun frameBoundsCropEachDirectionInOrder() {
        // 6 frames of 64x64 laid out horizontally.
        val width = 64 * EnemySpriteProvider.DIRECTION_COUNT
        val height = 64
        HexDirection.entries.forEachIndexed { index, direction ->
            val (offset, size) = EnemySpriteProvider.frameBounds(width, height, direction)
            assertEquals(IntOffset(64 * index, 0), offset)
            assertEquals(IntSize(64, 64), size)
        }
    }

    @Test
    fun frameBoundsFallBackToWholeImageWhenTooNarrow() {
        val (offset, size) = EnemySpriteProvider.frameBounds(4, 10, HexDirection.SE)
        assertEquals(IntOffset.Zero, offset)
        assertEquals(IntSize(4, 10), size)
    }
}
