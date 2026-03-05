package de.egril.defender.ui.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Damage overlay for barricades taking damage.
 * When [animate] is true, shows a Lottie damage animation.
 * When [animate] is false, shows three static red "-" symbols.
 */
@Composable
fun BarricadeDamageAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedBarricadeDamage(modifier)
    } else {
        StaticBarricadeDamage(modifier)
    }
}

@Composable
private fun AnimatedBarricadeDamage(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.BARRICADE_DAMAGE,
        modifier = modifier.fillMaxSize(),
        iterations = Int.MAX_VALUE
    )
}

@Composable
private fun StaticBarricadeDamage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Large - symbol at center
        Text(
            "-",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.Red,
            fontWeight = FontWeight.Bold
        )
        // Medium - symbol - offset left and higher
        Text(
            "-",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Red,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(x = (-10).dp, y = (-12).dp)
        )
        // Small - symbol - offset right and higher
        Text(
            "-",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Red,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(x = 8.dp, y = (-15).dp)
        )
    }
}
