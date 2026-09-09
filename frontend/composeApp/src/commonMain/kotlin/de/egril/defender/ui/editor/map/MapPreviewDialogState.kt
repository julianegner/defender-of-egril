package de.egril.defender.ui.editor.map

internal data class MapPreviewDialogState(
    val generationRunning: Boolean = false,
    val generationSuccess: Boolean? = null,
    val generationError: String? = null,
    val generationStep: String = "",
    val compressedSizeKb: Long = 0L,
    val generationWasRegenerated: Boolean = false,
    val previewRegenerating: Boolean = false,
    val previewError: String? = null,
    val hasPreviewPainter: Boolean = false,
)

internal fun stateForOpeningMapPreviewDialog(state: MapPreviewDialogState): MapPreviewDialogState =
    state.copy(
        generationRunning = false,
        generationSuccess = if (state.hasPreviewPainter) true else null,
        generationError = null,
        generationStep = "",
        compressedSizeKb = 0L,
        generationWasRegenerated = false,
        previewRegenerating = false,
        previewError = null,
    )

internal fun shouldLoadMapPreviewFromDisk(hasPreviewPainter: Boolean): Boolean = !hasPreviewPainter
