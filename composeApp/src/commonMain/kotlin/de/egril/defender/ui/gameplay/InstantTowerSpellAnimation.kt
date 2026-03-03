package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import de.egril.defender.ui.animations.AnimationType
import de.egril.defender.ui.animations.LottieAnimation

/** Spell color (gold/yellow) used for Instant Tower effects */
val SpellInstantTowerColor = Color(0xFFFFD700)

/**
 * Animated wavering border overlay for tower buy buttons when Instant Tower spell is active.
 *
 * When [animate] is true, uses a Lottie animation of pulsing borders.
 * When [animate] is false, a static tri-colored border is drawn instead.
 */
@Composable
fun InstantTowerSpellAnimation(
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    if (animate) {
        LottieAnimation(
            animationType = AnimationType.INSTANT_TOWER_SPELL,
            modifier = modifier.fillMaxSize(),
            iterations = Int.MAX_VALUE,
            contentScale = ContentScale.FillBounds
        )
    } else {
        // Static fallback: three thin nested pill-shaped borders in gold, cyan and purple
        Canvas(modifier = modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeWidth = 2f
            val cornerRadius = h / 2f  // Match Material3 Button's fully-rounded (pill) shape
            val colors = listOf(SpellInstantTowerColor, Color.Cyan, Color(0xFFAA00FF))
            colors.forEachIndexed { i, color ->
                val inset = strokeWidth / 2f + i * (strokeWidth + 1f)
                drawRoundRect(
                    color = color.copy(alpha = 0.85f),
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(w - 2 * inset, h - 2 * inset),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius - inset),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}
