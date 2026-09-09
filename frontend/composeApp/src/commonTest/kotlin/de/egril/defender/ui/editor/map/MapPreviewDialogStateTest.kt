package de.egril.defender.ui.editor.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapPreviewDialogStateTest {
    @Test
    fun openingDialogKeepsSuccessfulStateWhenPreviewAlreadyLoaded() {
        val openedState =
            stateForOpeningMapPreviewDialog(
                MapPreviewDialogState(
                    generationRunning = true,
                    generationSuccess = false,
                    generationError = "old error",
                    generationStep = "compressing",
                    compressedSizeKb = 42L,
                    generationWasRegenerated = true,
                    previewRegenerating = true,
                    previewError = "preview error",
                    hasPreviewPainter = true,
                ),
            )

        assertFalse(openedState.generationRunning)
        assertEquals(true, openedState.generationSuccess)
        assertNull(openedState.generationError)
        assertEquals("", openedState.generationStep)
        assertEquals(0L, openedState.compressedSizeKb)
        assertFalse(openedState.generationWasRegenerated)
        assertFalse(openedState.previewRegenerating)
        assertNull(openedState.previewError)
        assertTrue(openedState.hasPreviewPainter)
    }

    @Test
    fun openingDialogWithoutPreviewLeavesSuccessUnsetAndRequiresDiskLoad() {
        val openedState =
            stateForOpeningMapPreviewDialog(
                MapPreviewDialogState(
                    generationSuccess = false,
                    generationError = "old error",
                    hasPreviewPainter = false,
                ),
            )

        assertNull(openedState.generationSuccess)
        assertNull(openedState.generationError)
        assertTrue(shouldLoadMapPreviewFromDisk(hasPreviewPainter = false))
        assertFalse(shouldLoadMapPreviewFromDisk(hasPreviewPainter = true))
    }
}
