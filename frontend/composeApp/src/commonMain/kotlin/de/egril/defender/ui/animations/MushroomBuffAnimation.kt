package de.egril.defender.ui.animations

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/** Orange color used for mushroom buff visual effects */
val MushroomBuffColor = Color(0xFFFF8C00)

/**
 * Buff overlay shown on enemy units currently under a mushroom speed/level bonus.
 *
 * When [animate] is true, shows a pulsing orange glow around the tile.
 * When [animate] is false, shows a static orange border with a small mushroom icon.
 */
@Composable
fun MushroomBuffAnimation(
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    if (animate) {
        AnimatedMushroomBuff(modifier)
    } else {
        StaticMushroomBuff(modifier)
    }
}

@Composable
private fun AnimatedMushroomBuff(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mushroomBuff")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.75f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 700),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "mushroomBuffAlpha",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 3f
        val inset = strokeWidth / 2f
        drawRect(
            color = MushroomBuffColor.copy(alpha = alpha),
            topLeft = Offset(inset, inset),
            size = Size(size.width - 2 * inset, size.height - 2 * inset),
            style = Stroke(width = strokeWidth),
        )
    }
}

@Composable
private fun StaticMushroomBuff(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 2.5f
        val inset = strokeWidth / 2f
        drawRect(
            color = MushroomBuffColor.copy(alpha = 0.8f),
            topLeft = Offset(inset, inset),
            size = Size(size.width - 2 * inset, size.height - 2 * inset),
            style = Stroke(width = strokeWidth),
        )
    }
}
