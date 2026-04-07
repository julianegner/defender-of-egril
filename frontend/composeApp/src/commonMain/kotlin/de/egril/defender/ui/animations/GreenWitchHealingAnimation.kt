package de.egril.defender.ui.animations
import io.github.alexzhirkevich.compottie.Compottie

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
 * Healing overlay for enemies affected by the Green Witch healing ability.
 * When [animate] is true, shows a Lottie animation of green healing effects.
 * When [animate] is false, shows three static green "+" symbols.
 */
@Composable
fun GreenWitchHealingAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedGreenWitchHealing(modifier)
    } else {
        StaticGreenWitchHealing(modifier)
    }
}

@Composable
private fun AnimatedGreenWitchHealing(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.GREEN_WITCH_HEALING,
        modifier = modifier.fillMaxSize(),
        iterations = Compottie.IterateForever
    )
}

@Composable
private fun StaticGreenWitchHealing(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Large + symbol at center
        Text(
            "+",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold
        )
        // Medium + symbol - offset left and higher
        Text(
            "+",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(x = (-12).dp, y = (-8).dp)
        )
        // Small + symbol - offset right and even higher
        Text(
            "+",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(x = 12.dp, y = (-16).dp)
        )
    }
}
