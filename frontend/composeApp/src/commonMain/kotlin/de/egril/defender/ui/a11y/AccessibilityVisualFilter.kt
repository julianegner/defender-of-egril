package de.egril.defender.ui.a11y

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

fun createAccessibilityColorMatrix(
    highContrastEnabled: Boolean,
    colorBlindPalette: ColorBlindPalette,
): ColorMatrix? {
    val contrastAdjusted = if (highContrastEnabled) 1.3f else 1f
    val matrix =
        when (colorBlindPalette) {
            ColorBlindPalette.OFF -> contrastMatrix(contrastAdjusted)
            ColorBlindPalette.DEUTERANOPIA ->
                ColorMatrix(
                    floatArrayOf(
                        0.625f * contrastAdjusted,
                        0.375f * contrastAdjusted,
                        0f,
                        0f,
                        0f,
                        0.700f * contrastAdjusted,
                        0.300f * contrastAdjusted,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.300f * contrastAdjusted,
                        0.700f * contrastAdjusted,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        1f,
                        0f,
                    ),
                )
            ColorBlindPalette.PROTANOPIA ->
                ColorMatrix(
                    floatArrayOf(
                        0.567f * contrastAdjusted,
                        0.433f * contrastAdjusted,
                        0f,
                        0f,
                        0f,
                        0.558f * contrastAdjusted,
                        0.442f * contrastAdjusted,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.242f * contrastAdjusted,
                        0.758f * contrastAdjusted,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        1f,
                        0f,
                    ),
                )
            ColorBlindPalette.TRITANOPIA ->
                ColorMatrix(
                    floatArrayOf(
                        0.950f * contrastAdjusted,
                        0.050f * contrastAdjusted,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.433f * contrastAdjusted,
                        0.567f * contrastAdjusted,
                        0f,
                        0f,
                        0f,
                        0.475f * contrastAdjusted,
                        0.525f * contrastAdjusted,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        1f,
                        0f,
                    ),
                )
        }

    return if (!highContrastEnabled && colorBlindPalette == ColorBlindPalette.OFF) {
        null
    } else {
        matrix
    }
}

private fun contrastMatrix(contrast: Float): ColorMatrix {
    val translation = 128f * (1f - contrast)
    return ColorMatrix(
        floatArrayOf(
            contrast,
            0f,
            0f,
            0f,
            translation,
            0f,
            contrast,
            0f,
            0f,
            translation,
            0f,
            0f,
            contrast,
            0f,
            translation,
            0f,
            0f,
            0f,
            1f,
            0f,
        ),
    )
}

fun Modifier.accessibilityVisualFilter(
    highContrastEnabled: Boolean,
    colorBlindPalette: ColorBlindPalette,
): Modifier {
    val matrix = createAccessibilityColorMatrix(highContrastEnabled, colorBlindPalette) ?: return this
    return this.drawWithContent {
        drawIntoCanvas { canvas ->
            canvas.saveLayer(
                Rect(0f, 0f, size.width, size.height),
                Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(matrix)
                },
            )
            this@drawWithContent.drawContent()
            canvas.restore()
        }
    }
}
