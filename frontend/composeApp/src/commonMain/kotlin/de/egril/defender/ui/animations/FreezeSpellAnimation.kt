package de.egril.defender.ui.animations
import io.github.alexzhirkevich.compottie.Compottie

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Freeze spell overlay for frozen enemies.
 * When [animate] is true, shows two staggered Lottie snowfall animations so the loop
 * boundary is invisible.
 * When [animate] is false, shows a static pattern of twelve white snowflakes.
 *
 * The optional [animationKey] parameter can be used to reset the stagger delay
 * (e.g., pass the attacker ID so the delay resets when an enemy is re-frozen).
 */
@Composable
fun FreezeSpellAnimation(animate: Boolean, modifier: Modifier = Modifier, animationKey: Any? = Unit) {
    if (animate) {
        AnimatedFreezeSpell(modifier, animationKey)
    } else {
        StaticFreezeSpell(modifier)
    }
}

@Composable
private fun AnimatedFreezeSpell(modifier: Modifier = Modifier, animationKey: Any? = Unit) {
    LottieAnimation(
        animationType = AnimationType.FREEZE_SPELL,
        modifier = modifier.fillMaxSize(),
        iterations = Compottie.IterateForever
    )
    // Delayed copy: starts after 3 s so that when one animation resets to frame 0
    // the other is mid-fall — no visible gap at the loop boundary.
    var showSecondAnimation by remember(animationKey) { mutableStateOf(false) }
    LaunchedEffect(animationKey) {
        delay(3000L)
        showSecondAnimation = true
    }
    if (showSecondAnimation) {
        LottieAnimation(
            animationType = AnimationType.FREEZE_SPELL,
            modifier = modifier.fillMaxSize(),
            iterations = Compottie.IterateForever
        )
    }
}

@Composable
private fun StaticFreezeSpell(modifier: Modifier = Modifier) {
    val snowflakeConfigs = remember {
        listOf(
            Triple(0.08f, 0.62f, 0.04f),   // far-left, lower-mid, small
            Triple(0.15f, 0.04f, 0.09f),   // left, near top, large
            Triple(0.25f, 0.42f, 0.07f),   // center-left, mid, medium
            Triple(0.32f, 0.83f, 0.04f),   // center-left, near bottom, small
            Triple(0.42f, 0.73f, 0.035f),  // center, lower-mid, tiny
            Triple(0.50f, 0.20f, 0.065f),  // center, upper, medium
            Triple(0.60f, 0.51f, 0.06f),   // center-right, mid, medium
            Triple(0.67f, 0.88f, 0.033f),  // center-right, near bottom, tiny
            Triple(0.75f, 0.18f, 0.08f),   // right, upper, large-medium
            Triple(0.82f, 0.54f, 0.072f),  // right, mid, medium-large
            Triple(0.88f, 0.85f, 0.045f),  // far-right, near bottom, small
            Triple(0.93f, 0.04f, 0.05f)    // far right, near top, small-medium
        )
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        snowflakeConfigs.forEach { (xFrac, yFrac, radiusFrac) ->
            drawSnowflake(
                center = Offset(xFrac * canvasWidth, yFrac * canvasHeight),
                radius = canvasWidth * radiusFrac,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

private fun DrawScope.drawSnowflake(center: Offset, radius: Float, color: Color) {
    val strokeWidth = radius * 0.15f
    for (i in 0 until 6) {
        val angle = i * PI.toFloat() / 3f
        val cosAngle = cos(angle)
        val sinAngle = sin(angle)
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x + cosAngle * radius, center.y + sinAngle * radius),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        val branchLength = radius * 0.3f
        val branchPos = radius * 0.65f
        val branchX = center.x + cosAngle * branchPos
        val branchY = center.y + sinAngle * branchPos
        val leftAngle = angle - PI.toFloat() / 6f
        drawLine(
            color = color,
            start = Offset(branchX, branchY),
            end = Offset(branchX + cos(leftAngle) * branchLength, branchY + sin(leftAngle) * branchLength),
            strokeWidth = strokeWidth * 0.6f,
            cap = StrokeCap.Round
        )
        val rightAngle = angle + PI.toFloat() / 6f
        drawLine(
            color = color,
            start = Offset(branchX, branchY),
            end = Offset(branchX + cos(rightAngle) * branchLength, branchY + sin(rightAngle) * branchLength),
            strokeWidth = strokeWidth * 0.6f,
            cap = StrokeCap.Round
        )
    }
}
