package de.egril.defender.ui.animations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/** Spell color (lila/purple) used for Double Tower Level effects */
val SpellDoubleLevelColor = Color(0xFFAB47BC)

/** Spell color (purple) used for Double Tower Reach effects */
val SpellDoubleReachColor = Color(0xFFAB47BC)

/**
 * Animated wavering border overlay for towers under the Double Tower Level spell.
 *
 * When [animate] is true, uses a Lottie animation of pulsing white/purple/red borders.
 * When [animate] is false, a static tri-colored border is drawn instead.
 */
@Composable
fun DoubleLevelSpellAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedDoubleLevelSpell(modifier)
    } else {
        StaticDoubleLevelSpell(modifier)
    }
}

@Composable
private fun AnimatedDoubleLevelSpell(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.DOUBLE_LEVEL_SPELL,
        modifier = modifier.fillMaxSize(),
        iterations = Int.MAX_VALUE
    )
}

@Composable
private fun StaticDoubleLevelSpell(modifier: Modifier = Modifier) {
    // Static fallback: three thin nested borders in white, purple and red
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2f
        val colors = listOf(Color.White, SpellDoubleLevelColor, Color.Red)
        colors.forEachIndexed { i, color ->
            val inset = strokeWidth / 2f + i * (strokeWidth + 1f)
            drawRect(
                color = color.copy(alpha = 0.85f),
                topLeft = Offset(inset, inset),
                size = Size(w - 2 * inset, h - 2 * inset),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}
