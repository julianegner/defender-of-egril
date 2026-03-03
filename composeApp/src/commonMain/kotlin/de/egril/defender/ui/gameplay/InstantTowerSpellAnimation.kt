package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import de.egril.defender.ui.animations.AnimationType
import de.egril.defender.ui.animations.LottieAnimation

/** Spell color (purple) used for Instant Tower border */
val SpellInstantTowerColor = Color(0xFFAA00FF)

/** Semi-transparent purple glow used as Instant Tower fill overlay */
private val SpellGlowColor = Color(0x50990ECC)

/** Pink sparkle dots color for Instant Tower static overlay */
private val SpellSparklePinkColor = Color(0xFFDD99FF)

/** Gold sparkle accent color for Instant Tower static overlay */
private val SpellSparkleGoldColor = Color(0xFFFFD700)

/**
 * Purple glow overlay for tower buy buttons when Instant Tower spell is active.
 *
 * When [animate] is true, uses a Lottie animation with pulsing purple glow and sparkle dots.
 * When [animate] is false, a static purple sparkle overlay is drawn instead.
 *
 * The border around the button is added separately in DefenderButtons via Compose's
 * `.border()` modifier, so this composable only draws the interior glow/sparkle fill.
 */
@Composable
fun InstantTowerSpellAnimation(
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    if (animate) {
        LottieAnimation(
            animationType = AnimationType.INSTANT_TOWER_SPELL,
            modifier = modifier.fillMaxSize().clip(RoundedCornerShape(percent = 50)),
            iterations = Int.MAX_VALUE,
            contentScale = ContentScale.FillBounds
        )
    } else {
        // Static fallback: purple glow fill + sparkle dots (inspired by magical sparkle aesthetic)
        Canvas(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(percent = 50))) {
            val w = size.width
            val h = size.height
            val r = h / 2f  // Pill corner radius

            // Semi-transparent purple glow fill over the whole button
            drawRoundRect(
                color = SpellGlowColor,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(r)
            )

            // Sparkle dots — fixed positions within the pill area
            val sparkleRadius = 2.5.dp.toPx()
            listOf(
                Offset(w * 0.15f, h * 0.30f),
                Offset(w * 0.28f, h * 0.72f),
                Offset(w * 0.50f, h * 0.20f),
                Offset(w * 0.55f, h * 0.78f),
                Offset(w * 0.72f, h * 0.32f),
                Offset(w * 0.85f, h * 0.65f),
            ).forEach { pos ->
                drawCircle(color = SpellSparklePinkColor.copy(alpha = 0.85f), radius = sparkleRadius, center = pos)
            }

            // Small bright gold accent dots
            listOf(
                Offset(w * 0.38f, h * 0.50f),
                Offset(w * 0.62f, h * 0.50f),
            ).forEach { pos ->
                drawCircle(color = SpellSparkleGoldColor.copy(alpha = 0.7f), radius = 1.8.dp.toPx(), center = pos)
            }
        }
    }
}
