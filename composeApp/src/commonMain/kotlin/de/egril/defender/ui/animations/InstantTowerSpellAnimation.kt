package de.egril.defender.ui.animations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/** Spell color (purple) used for Instant Tower border */
val SpellInstantTowerColor = Color(0xFFAA00FF)

// Star gold → purple gradient endpoints
private val StarGold   = Color(1.0f, 0.843f, 0.0f,  1.0f)
private val StarPurple = Color(0.6f, 0.0f,   1.0f,  1.0f)

private const val StarAlpha = 0.88f
private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red   = a.red   + (b.red   - a.red)   * t,
    green = a.green + (b.green - a.green) * t,
    blue  = a.blue  + (b.blue  - a.blue)  * t,
    alpha = StarAlpha
)

/**
 * Rising-stars overlay for tower buy buttons when Instant Tower spell is active.
 *
 * When [animate] is true, uses a Lottie animation of 36 particle stars rising bottom→top
 * with a gold-to-purple color gradient.
 * When [animate] is false, a static star-field snapshot is drawn instead.
 *
 * The purple pill border is added separately in DefenderButtons via `.border()`.
 */
@Composable
fun InstantTowerSpellAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedInstantTowerSpell(modifier)
    } else {
        StaticInstantTowerSpell(modifier)
    }
}

@Composable
private fun AnimatedInstantTowerSpell(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.INSTANT_TOWER_SPELL,
        modifier = modifier.fillMaxSize().clip(RoundedCornerShape(percent = 50)),
        iterations = Int.MAX_VALUE,
        contentScale = ContentScale.FillBounds
    )
}

@Composable
private fun StaticInstantTowerSpell(modifier: Modifier = Modifier) {
    // Static fallback: 36 star dots scattered bottom (gold) to top (purple),
    // representing a mid-animation snapshot of the rising-star animation.
    Canvas(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(percent = 50))) {
        val w = size.width
        val h = size.height

        // Each entry: (relX, relY, radiusDp)
        // relY: 0.0 = top of button (purple), 1.0 = bottom (gold)
        val stars = listOf(
            // Bottom band (gold)
            Triple(0.10f, 0.92f, 3.5f), Triple(0.25f, 0.88f, 2.8f), Triple(0.40f, 0.94f, 4.0f),
            Triple(0.55f, 0.90f, 3.2f), Triple(0.70f, 0.86f, 2.5f), Triple(0.85f, 0.93f, 3.8f),
            Triple(0.18f, 0.82f, 2.5f), Triple(0.62f, 0.80f, 3.0f), Triple(0.92f, 0.78f, 2.2f),
            // Lower-middle (warm gold-purple)
            Triple(0.05f, 0.68f, 2.8f), Triple(0.30f, 0.65f, 3.2f), Triple(0.50f, 0.70f, 2.5f),
            Triple(0.72f, 0.72f, 3.5f), Triple(0.88f, 0.65f, 2.0f), Triple(0.15f, 0.60f, 3.0f),
            // Middle (mid-purple)
            Triple(0.42f, 0.52f, 2.8f), Triple(0.65f, 0.50f, 2.2f), Triple(0.25f, 0.48f, 3.5f),
            Triple(0.80f, 0.55f, 2.5f), Triple(0.08f, 0.42f, 2.0f), Triple(0.55f, 0.40f, 3.0f),
            Triple(0.35f, 0.45f, 2.2f), Triple(0.92f, 0.44f, 2.5f),
            // Upper-middle (more purple)
            Triple(0.35f, 0.32f, 2.5f), Triple(0.60f, 0.30f, 2.0f), Triple(0.15f, 0.28f, 2.8f),
            Triple(0.78f, 0.34f, 2.2f), Triple(0.48f, 0.24f, 2.5f), Triple(0.92f, 0.22f, 1.8f),
            // Top band (pure purple)
            Triple(0.22f, 0.14f, 2.0f), Triple(0.45f, 0.10f, 2.5f), Triple(0.68f, 0.16f, 1.8f),
            Triple(0.10f, 0.06f, 2.2f), Triple(0.82f, 0.12f, 2.0f), Triple(0.55f, 0.20f, 2.2f),
            Triple(0.35f, 0.05f, 1.8f)
        )

        stars.forEach { (relX, relY, radiusDp) ->
            // t: 0 at bottom (gold), 1 at top (purple)
            val t = 1f - relY
            drawCircle(
                color  = lerpColor(StarGold, StarPurple, t),
                radius = radiusDp.dp.toPx(),
                center = Offset(relX * w, relY * h)
            )
        }
    }
}
