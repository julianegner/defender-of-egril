package de.egril.defender.ui.animations
import io.github.alexzhirkevich.compottie.Compottie

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/** Duration (ms) the mine-dig animation plays before stopping automatically. */
private const val MINE_DIG_ANIMATION_DURATION_MS = 5_000L

/**
 * Mine digging animation shown on the Dwarven Mine tile after the player performs a dig action.
 *
 * When [animate] is true the animation loops for up to [MINE_DIG_ANIMATION_DURATION_MS] ms (≈ 5 s),
 * then stops. It also stops immediately if the composable leaves the composition (e.g. the turn
 * ends and the effect is cleared, or a dragon spawns and replaces the mine tile).
 *
 * When [animate] is false, no additional visual is shown (the dig-outcome dialog already provides
 * feedback).
 */
@Composable
fun MineDigAnimation(animate: Boolean, modifier: Modifier = Modifier) {
    if (!animate) return

    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(MINE_DIG_ANIMATION_DURATION_MS)
        isVisible = false
    }

    if (isVisible) {
        LottieAnimation(
            animationType = AnimationType.MINE_DIG,
            modifier = modifier.fillMaxSize(),
            iterations = Compottie.IterateForever
        )
    }
    // No static fallback — the dig-outcome dialog already communicates the result.
}

