package de.egril.defender.ui.animations

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.launch

/**
 * Animation type enum for different game animations
 */
enum class AnimationType {
    GREEN_WITCH_HEALING,
    BARRICADE_DAMAGE,
    FREEZE_SPELL,
    DOUBLE_LEVEL_SPELL,
    INSTANT_TOWER_SPELL,
    FEAR_SPELL,
    BOMB_EXPLOSION,
    WATER_FLOW
}

/**
 * Loads a Lottie animation from resources and displays it
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun LottieAnimation(
    animationType: AnimationType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iterations: Int = 1, // Number of times to play the animation (use Int.MAX_VALUE for infinite loop)
    speed: Float = 1f,   // Playback speed multiplier (1f = normal, 2f = double speed)
    contentScale: ContentScale = ContentScale.Fit
) {
    // Get the animation file path based on type
    val animationPath = when (animationType) {
        AnimationType.GREEN_WITCH_HEALING -> "files/animations/green_witch_healing.json"
        AnimationType.BARRICADE_DAMAGE -> "files/animations/barricade_damage.json"
        AnimationType.FREEZE_SPELL -> "files/animations/freeze_spell.json"
        AnimationType.DOUBLE_LEVEL_SPELL -> "files/animations/double_level_spell.json"
        AnimationType.INSTANT_TOWER_SPELL -> "files/animations/instant_tower_spell.json"
        AnimationType.FEAR_SPELL -> "files/animations/fear_spell.json"
        AnimationType.BOMB_EXPLOSION -> "files/animations/bomb_explosion.json"
        AnimationType.WATER_FLOW -> "files/animations/water_flow.json"
    }
    
    // Load the animation JSON asynchronously
    var animationJson by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(animationPath) {
        launch {
            animationJson = runCatching {
                Res.readBytes(animationPath).decodeToString()
            }.getOrElse {
                // Return empty JSON if file not found
                "{}"
            }
        }
    }
    
    // Only show animation when JSON is loaded
    animationJson?.let { jsonString ->
        // Load the Lottie composition from JSON string
        val composition by rememberLottieComposition {
            LottieCompositionSpec.JsonString(jsonString)
        }
        
        // Animate the composition
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = iterations,
            isPlaying = true,
            speed = speed,
            restartOnPlay = true
        )
        
        // Create painter from composition
        val painter = rememberLottiePainter(
            composition = composition,
            progress = { progress }
        )
        
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painter,
                contentDescription = when (animationType) {
                    AnimationType.GREEN_WITCH_HEALING -> "Green witch healing animation"
                    AnimationType.BARRICADE_DAMAGE -> "Barricade damage animation"
                    AnimationType.FREEZE_SPELL -> "Freeze spell snowflakes animation"
                    AnimationType.DOUBLE_LEVEL_SPELL -> "Double tower level spell animation"
                    AnimationType.INSTANT_TOWER_SPELL -> "Instant tower spell animation"
                    AnimationType.FEAR_SPELL -> "Fear spell dark cloud animation"
                    AnimationType.BOMB_EXPLOSION -> "Bomb explosion animation"
                    AnimationType.WATER_FLOW -> "Water flow animation"
                },
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
