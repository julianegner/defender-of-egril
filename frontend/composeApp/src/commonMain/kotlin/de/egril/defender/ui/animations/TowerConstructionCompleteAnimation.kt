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
 * Construction complete sparkle overlay shown on a tower tile when it finishes building.
 * When [animate] is true, shows a Lottie gold radiating star-burst animation (plays once).
 * When [animate] is false, shows a static gold check-mark symbol.
 */
@Composable
fun TowerConstructionCompleteAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (animate) {
        AnimatedTowerConstructionComplete(modifier)
    } else {
        StaticTowerConstructionComplete(modifier)
    }
}

@Composable
private fun AnimatedTowerConstructionComplete(modifier: Modifier = Modifier) {
    LottieAnimation(
        animationType = AnimationType.TOWER_CONSTRUCTION_COMPLETE,
        modifier = modifier.fillMaxSize(),
        iterations = 1
    )
}

@Composable
private fun StaticTowerConstructionComplete(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // "\u2713" is a single-character symbol, not a translatable string
        Text(
            "\u2713",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFFFFD700),
            fontWeight = FontWeight.Bold
        )
    }
}
