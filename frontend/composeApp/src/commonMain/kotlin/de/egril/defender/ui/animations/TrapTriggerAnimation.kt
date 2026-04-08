package de.egril.defender.ui.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/**
 * Trap trigger overlay shown at the trap tile when it activates.
 * When [animate] is true, shows a Lottie orange snap-flash animation (plays once).
 * When [animate] is false, shows a static orange "!" symbol.
 */
@Composable
fun TrapTriggerAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedTrapTrigger(modifier)
    } else {
        StaticTrapTrigger(modifier)
    }
}

@Composable
private fun AnimatedTrapTrigger(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.TRAP_TRIGGER,
        modifier = modifier.fillMaxSize(),
        iterations = 1
    )
}

@Composable
private fun StaticTrapTrigger(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // "!" is a single-character symbol, not a translatable string
        Text(
            "!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFFFF6F00),
            fontWeight = FontWeight.ExtraBold
        )
    }
}
