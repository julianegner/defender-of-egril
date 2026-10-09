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
                    previewRegenerating = true,
                    previewError = "preview error",
                    hasPreviewPainter = true,
                ),
            )

        assertFalse(openedState.generationRunning)
        assertEquals(true, openedState.generationSuccess)
        assertNull(openedState.generationError)
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
