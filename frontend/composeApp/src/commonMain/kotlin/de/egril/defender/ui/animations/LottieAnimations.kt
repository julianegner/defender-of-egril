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
import io.github.alexzhirkevich.compottie.Compottie
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
    WATER_FLOW,
    ENEMY_DEATH,
    TOWER_READY_PULSE,
    COIN_GAIN,
    TOWER_ATTACK_IMPACT,
    TOWER_CONSTRUCTION_COMPLETE,
    ENEMY_SPAWN,
    TRAP_TRIGGER,
    ENEMY_MOVE,
    DRAGON_LEVEL_UP,
    DRAGON_LEVEL_DOWN,
    WIZARD_IDLE,
    ALCHEMY_IDLE,
    MINE_DIG,
    ARROW_ATTACK,
    DRAGON_TARGET
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
    iterations: Int = 1, // Number of times to play the animation (use Compottie.IterateForever for infinite loop)
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
        AnimationType.ENEMY_DEATH -> "files/animations/enemy_death.json"
        AnimationType.TOWER_READY_PULSE -> "files/animations/tower_ready_pulse.json"
        AnimationType.COIN_GAIN -> "files/animations/coin_gain.json"
        AnimationType.TOWER_ATTACK_IMPACT -> "files/animations/tower_attack_impact.json"
        AnimationType.TOWER_CONSTRUCTION_COMPLETE -> "files/animations/tower_construction_complete.json"
        AnimationType.ENEMY_SPAWN -> "files/animations/enemy_spawn.json"
        AnimationType.TRAP_TRIGGER -> "files/animations/trap_trigger.json"
        AnimationType.ENEMY_MOVE -> "files/animations/enemy_move.json"
        AnimationType.DRAGON_LEVEL_UP -> "files/animations/dragon_level_up.json"
        AnimationType.DRAGON_LEVEL_DOWN -> "files/animations/dragon_level_down.json"
        AnimationType.WIZARD_IDLE -> "files/animations/wizard_idle.json"
        AnimationType.ALCHEMY_IDLE -> "files/animations/alchemy_idle.json"
        AnimationType.MINE_DIG -> "files/animations/mine_dig.json"
        AnimationType.ARROW_ATTACK -> "files/animations/arrow_attack.json"
        AnimationType.DRAGON_TARGET -> "files/animations/dragon_target.json"
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
                    AnimationType.ENEMY_DEATH -> "Enemy death animation"
                    AnimationType.TOWER_READY_PULSE -> "Tower ready pulse animation"
                    AnimationType.COIN_GAIN -> "Coin gain animation"
                    AnimationType.TOWER_ATTACK_IMPACT -> "Tower attack impact animation"
                    AnimationType.TOWER_CONSTRUCTION_COMPLETE -> "Tower construction complete animation"
                    AnimationType.ENEMY_SPAWN -> "Enemy spawn portal animation"
                    AnimationType.TRAP_TRIGGER -> "Trap trigger animation"
                    AnimationType.ENEMY_MOVE -> "Enemy movement trail animation"
                    AnimationType.DRAGON_LEVEL_UP -> "Dragon level up animation"
                    AnimationType.DRAGON_LEVEL_DOWN -> "Dragon level down animation"
                    AnimationType.WIZARD_IDLE -> "Wizard tower idle sparkle animation"
                    AnimationType.ALCHEMY_IDLE -> "Alchemy tower idle bubble animation"
                    AnimationType.MINE_DIG -> "Dwarven mine digging animation"
                    AnimationType.ARROW_ATTACK -> "Arrow attack projectile animation"
                    AnimationType.DRAGON_TARGET -> "Dragon targeting mine animation"
                },
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
