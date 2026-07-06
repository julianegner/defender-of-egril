package de.egril.defender.ui

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.egril.defender.model.AttackerType
import de.egril.defender.model.HexDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class EnemySpriteProviderTest {
    @Test
    fun spriteKeyHasSpritePrefix() {
        assertEquals("sprite_goblin", EnemySpriteProvider.spriteKey(AttackerType.GOBLIN))
        assertEquals("sprite_dragon", EnemySpriteProvider.spriteKey(AttackerType.DRAGON))
        assertEquals("sprite_skeleton", EnemySpriteProvider.spriteKey(AttackerType.SKELETON))
    }

    @Test
    fun spriteKeyUsesCanonicalNameForOrkAndEvilWizard() {
        // ORK file is sprite_orc.png; EVIL_WIZARD file is sprite_evil_mage.png
        assertEquals("sprite_orc", EnemySpriteProvider.spriteKey(AttackerType.ORK))
        assertEquals("sprite_evil_mage", EnemySpriteProvider.spriteKey(AttackerType.EVIL_WIZARD))
    }

    @Test
    fun frameBoundsCropDirectionalFramesFromThreeByThreeGrid() {
        // 3 columns × 3 rows of 60×60 frames → sheet is 180×180
        val frameW = 60
        val frameH = 60
        val sheetW = frameW * EnemySpriteProvider.GRID_COLS
        val sheetH = frameH * EnemySpriteProvider.GRID_ROWS

        // Row 0: SE (col 0), S unused, SW (col 2)
        assertEquals(IntOffset(0, 0) to IntSize(frameW, frameH), EnemySpriteProvider.frameBounds(sheetW, sheetH, HexDirection.SE))
        assertEquals(IntOffset(frameW * 2, 0) to IntSize(frameW, frameH), EnemySpriteProvider.frameBounds(sheetW, sheetH, HexDirection.SW))

        // Row 1: E (col 0), Center (col 1), W (col 2)
        assertEquals(IntOffset(0, frameH) to IntSize(frameW, frameH), EnemySpriteProvider.frameBounds(sheetW, sheetH, HexDirection.E))
        assertEquals(IntOffset(frameW, frameH) to IntSize(frameW, frameH), EnemySpriteProvider.frameBounds(sheetW, sheetH, null))
        assertEquals(IntOffset(frameW * 2, frameH) to IntSize(frameW, frameH), EnemySpriteProvider.frameBounds(sheetW, sheetH, HexDirection.W))

        // Row 2: NE (col 0), N unused, NW (col 2)
        assertEquals(IntOffset(0, frameH * 2) to IntSize(frameW, frameH), EnemySpriteProvider.frameBounds(sheetW, sheetH, HexDirection.NE))
        assertEquals(IntOffset(frameW * 2, frameH * 2) to IntSize(frameW, frameH), EnemySpriteProvider.frameBounds(sheetW, sheetH, HexDirection.NW))
    }

    @Test
    fun frameBoundsFallBackToWholeImageWhenTooSmall() {
        // Sheet too narrow to split into 3 columns (frameWidth = 2/3 = 0)
        val (offset, size) = EnemySpriteProvider.frameBounds(2, 10, HexDirection.SE)
        assertEquals(IntOffset.Zero, offset)
        assertEquals(IntSize(2, 10), size)
    }
}
