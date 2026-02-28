package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

/** Spell color (lila/purple) used for Double Tower Level effects */
val SpellDoubleLevelColor = Color(0xFFAB47BC)

/**
 * Animated wavering border overlay for towers under the Double Tower Level spell.
 *
 * When [animate] is true, the border pulses and cycles through white, purple and red.
 * When [animate] is false, a static tri-colored border is drawn instead.
 */
@Composable
fun DoubleLevelSpellAnimation(
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    if (animate) {
        var tick by remember { mutableStateOf(0L) }
        LaunchedEffect(Unit) {
            while (isActive) {
                delay(40)
                tick++
            }
        }
        Canvas(modifier = modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val phase = (tick * 0.06f) % (2f * PI.toFloat())

            // Three animated arcs that shift color over time
            val colors = listOf(Color.White, SpellDoubleLevelColor, Color.Red)
            colors.forEachIndexed { i, color ->
                val offset = i * 2f * PI.toFloat() / 3f
                // Stroke width pulses between 2 and 5 px
                val strokeWidth = 2f + 3f * (0.5f + 0.5f * sin(phase + offset))
                val inset = strokeWidth / 2f
                drawRect(
                    color = color.copy(alpha = 0.7f + 0.3f * sin(phase + offset)),
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(w - 2 * inset, h - 2 * inset),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    } else {
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
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(w - 2 * inset, h - 2 * inset),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}
