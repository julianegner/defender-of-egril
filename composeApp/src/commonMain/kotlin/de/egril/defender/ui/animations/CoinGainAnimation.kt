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
 * Coin gain animation overlay shown at the position of a defeated enemy.
 * When [animate] is true, shows a Lottie animation of rising golden circles (plays once).
 * When [animate] is false, shows a static gold "+[amount]" label.
 */
@Composable
fun CoinGainAnimation(amount: Int, animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedCoinGain(modifier)
    } else {
        StaticCoinGain(amount, modifier)
    }
}

@Composable
private fun AnimatedCoinGain(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.COIN_GAIN,
        modifier = modifier.fillMaxSize(),
        iterations = 1
    )
}

@Composable
private fun StaticCoinGain(amount: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // "+N" is a universal numeric notation (like damage values elsewhere in the UI)
        // and does not require localization.
        Text(
            "+$amount",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFFD700),
            fontWeight = FontWeight.Bold
        )
    }
}
