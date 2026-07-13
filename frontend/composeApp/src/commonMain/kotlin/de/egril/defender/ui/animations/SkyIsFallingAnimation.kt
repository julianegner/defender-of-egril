package de.egril.defender.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Total duration of the falling-meteor sweep, in milliseconds. */
private const val SKY_IS_FALLING_DURATION_MS = 1500

/** Number of meteors that rain across the map. */
private const val METEOR_COUNT = 26

/** Fiery colours used for the meteor heads and trails. */
private val MeteorHeadColor = Color(0xFFFFF3B0)
private val MeteorCoreColor = Color(0xFFFF7043)
private val MeteorTrailColor = Color(0xFFD84315)

private data class Meteor(
    val startXFraction: Float,
    val startDelay: Float,
    val activeSpan: Float,
    val driftFraction: Float,
    val headRadiusFraction: Float,
    val trailLengthFraction: Float,
)

/**
 * Full-map "Sky is Falling" overlay: a shower of glowing meteors/comets that fall diagonally
 * across every tile of the map when the power is activated.
 *
 * Rendered in the [de.egril.defender.ui.hexagon.HexagonalMapView] overlay slot so it spans the
 * entire (scaled) map. Each change of [triggerKey] (incremented when the power is used) restarts
 * the animation. When [animate] is false, nothing is drawn (transient decorative effect only).
 */
@Composable
fun SkyIsFallingAnimation(
    triggerKey: Int,
    contentSize: IntSize,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!animate || triggerKey <= 0 || contentSize.width <= 0 || contentSize.height <= 0) return

    val meteors =
        remember(triggerKey) {
            val random = Random(triggerKey * 92821 + 7)
            List(METEOR_COUNT) { index ->
                Meteor(
                    // Spread evenly across the width, with a little jitter, so all tiles are covered.
                    startXFraction = (index + random.nextFloat()) / METEOR_COUNT - 0.15f,
                    startDelay = random.nextFloat() * 0.55f,
                    activeSpan = 0.35f + random.nextFloat() * 0.2f,
                    driftFraction = 0.12f + random.nextFloat() * 0.12f,
                    headRadiusFraction = 0.006f + random.nextFloat() * 0.006f,
                    trailLengthFraction = 0.12f + random.nextFloat() * 0.12f,
                )
            }
        }

    val progress = remember(triggerKey) { Animatable(0f) }
    var visible by remember(triggerKey) { mutableStateOf(true) }

    LaunchedEffect(triggerKey) {
        visible = true
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = SKY_IS_FALLING_DURATION_MS))
        visible = false
    }

    if (!visible) return

    val density = LocalDensity.current.density
    val widthDp = (contentSize.width / density).dp
    val heightDp = (contentSize.height / density).dp

    Canvas(modifier = modifier.size(widthDp, heightDp)) {
        val p = progress.value
        val minDim = max(size.minDimension, 1f)
        meteors.forEach { meteor ->
            drawMeteor(meteor, p, minDim)
        }
    }
}

private fun DrawScope.drawMeteor(
    meteor: Meteor,
    globalProgress: Float,
    minDim: Float,
) {
    val local = (globalProgress - meteor.startDelay) / meteor.activeSpan
    if (local <= 0f || local >= 1f) return

    val margin = size.height * 0.25f
    val headX = meteor.startXFraction * size.width + local * size.width * meteor.driftFraction
    val headY = -margin + local * (size.height + 2f * margin)
    val head = Offset(headX, headY)

    // Trail points up and to the left (opposite the fall direction).
    val trailLength = minDim * meteor.trailLengthFraction
    val trailDx = size.width * meteor.driftFraction
    val trailDy = size.height + 2f * margin
    val trailMag = max(sqrt(trailDx * trailDx + trailDy * trailDy), 1f)
    val tail =
        Offset(
            head.x - trailDx / trailMag * trailLength,
            head.y - trailDy / trailMag * trailLength,
        )

    // Fade in/out over the meteor's active span for a soft appearance.
    val fade = sin(local * PI.toFloat())
    val headRadius = minDim * meteor.headRadiusFraction

    // Glowing trail.
    drawLine(
        brush =
            Brush.linearGradient(
                colors = listOf(Color.Transparent, MeteorTrailColor.copy(alpha = 0.85f * fade)),
                start = tail,
                end = head,
            ),
        start = tail,
        end = head,
        strokeWidth = headRadius * 1.4f,
        cap = StrokeCap.Round,
    )

    // Soft glow around the head.
    drawCircle(
        color = MeteorCoreColor.copy(alpha = 0.5f * fade),
        radius = headRadius * 2.4f,
        center = head,
    )
    // Bright core.
    drawCircle(
        color = MeteorHeadColor.copy(alpha = fade),
        radius = headRadius,
        center = head,
    )
}
