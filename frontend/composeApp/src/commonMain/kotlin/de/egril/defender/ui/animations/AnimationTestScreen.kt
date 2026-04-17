package de.egril.defender.ui.animations
import io.github.alexzhirkevich.compottie.Compottie
import de.egril.defender.ui.animations.BallistaAttackTestPreview
import de.egril.defender.ui.animations.BowAttackTestPreview
import de.egril.defender.ui.animations.SpearAttackTestPreview
import de.egril.defender.ui.animations.PikeAttackTestPreview
import de.egril.defender.ui.animations.WizardAttackTestPreview
import de.egril.defender.ui.animations.AlchemyAttackTestPreview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.animation_test_animation_label
import defender_of_egril.composeapp.generated.resources.animation_test_background_label
import defender_of_egril.composeapp.generated.resources.animation_test_replay
import defender_of_egril.composeapp.generated.resources.animation_test_title
import defender_of_egril.composeapp.generated.resources.back

/**
 * Background preset for the animation test screen.
 * Covers the most relevant tile and card backgrounds used across the game.
 */
data class AnimationBackground(val label: String, val color: Color)

private val ANIMATION_BACKGROUNDS = listOf(
    AnimationBackground("Light mode surface",     Color(0xFFFAFAFA)),
    AnimationBackground("Dark mode surface",      Color(0xFF121212)),
    AnimationBackground("Enemy tile (red)",        Color(0xFFF44336)),
    AnimationBackground("Tower tile (blue)",       Color(0xFF2196F3)),
    AnimationBackground("Path tile (cream)",       Color(0xFFFFF8DC)),
    AnimationBackground("Build area (green)",      Color(0xFFA5D6A7)),
    AnimationBackground("Non-playable (gray)",     Color(0xFFE0E0E0)),
    AnimationBackground("Dark path (brown)",       Color(0xFF3E3528)),
    AnimationBackground("Dark build area (green)", Color(0xFF3D6B2C)),
    AnimationBackground("Dark non-playable",       Color(0xFF2C2C2C)),
    AnimationBackground("Enemy card (light red)",  Color(0xFFFFEBEE)),
    AnimationBackground("Enemy card (dark red)",   Color(0xFF4A2C2C)),
    AnimationBackground("Trap (brown)",            Color(0xFF8B4513)),
    AnimationBackground("River (blue)",            Color(0xFF4A90E2)),
    AnimationBackground("Yellow / selected",       Color(0xFFFFEB3B)),
    AnimationBackground("Black",                   Color(0xFF000000)),
    AnimationBackground("White",                   Color(0xFFFFFFFF)),
)

/**
 * Developer cheat-code screen: lets you pick any animation and any background colour and watch
 * it play, without having to trigger the real in-game event that would normally cause the animation.
 *
 * Access via the worldmap cheat code **"animation"** (click the worldmap title to open the cheat
 * dialog).
 */
@Composable
fun AnimationTestScreen(onBack: () -> Unit) {
    var selectedAnimation by remember { mutableStateOf(AnimationType.ENEMY_DEATH) }
    var selectedBackground by remember { mutableStateOf(ANIMATION_BACKGROUNDS[0]) }
    var animExpanded by remember { mutableStateOf(false) }
    var bgExpanded by remember { mutableStateOf(false) }
    // Key used to restart the animation when it is changed
    var animationKey by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Top bar ────────────────────────────────────────────────────────────────
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = onBack) {
                    Text(stringResource(Res.string.back))
                }

                Text(
                    text = stringResource(Res.string.animation_test_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Controls row ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animation picker
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.animation_test_animation_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                AnimationDropdown(
                    selected = selectedAnimation,
                    expanded = animExpanded,
                    onExpandedChange = { animExpanded = it },
                    onSelect = { type ->
                        selectedAnimation = type
                        animationKey++   // restart the animation
                        animExpanded = false
                    }
                )
            }

            // Background picker
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.animation_test_background_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                BackgroundDropdown(
                    selected = selectedBackground,
                    expanded = bgExpanded,
                    onExpandedChange = { bgExpanded = it },
                    onSelect = { bg ->
                        selectedBackground = bg
                        bgExpanded = false
                    }
                )
            }

            // Replay button
            OutlinedButton(onClick = { animationKey++ }) {
                Text(stringResource(Res.string.animation_test_replay))
            }
        }

        HorizontalDivider()

        // ── Preview area ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(selectedBackground.color),
            contentAlignment = Alignment.Center
        ) {
            // Use `key` to force a full recomposition (and thus restart) when the animation changes
            key(animationKey, selectedAnimation) {
                if (selectedAnimation == AnimationType.BALLISTA_ATTACK) {
                    // Special canvas-based preview for ballista (spans multiple tiles in gameplay)
                    BallistaAttackTestPreview(modifier = Modifier.fillMaxSize())
                } else if (selectedAnimation == AnimationType.BOW_ATTACK) {
                    // Special canvas-based preview for bow arrow volley
                    BowAttackTestPreview(modifier = Modifier.fillMaxSize())
                } else if (selectedAnimation == AnimationType.SPEAR_ATTACK) {
                    // Special canvas-based preview for spear throw
                    SpearAttackTestPreview(modifier = Modifier.fillMaxSize())
                } else if (selectedAnimation == AnimationType.PIKE_ATTACK) {
                    // Special canvas-based preview for pike extend
                    PikeAttackTestPreview(modifier = Modifier.fillMaxSize())
                } else if (selectedAnimation == AnimationType.WIZARD_ATTACK) {
                    // Special canvas-based preview for wizard fireball
                    WizardAttackTestPreview(modifier = Modifier.fillMaxSize())
                } else if (selectedAnimation == AnimationType.ALCHEMY_ATTACK) {
                    // Special canvas-based preview for alchemy acid vial
                    AlchemyAttackTestPreview(modifier = Modifier.fillMaxSize())
                } else {
                    val isLooping = selectedAnimation in LOOPING_ANIMATIONS
                    LottieAnimation(
                        animationType = selectedAnimation,
                        modifier = Modifier.size(200.dp),
                        iterations = if (isLooping) Compottie.IterateForever else 1
                    )
                }
            }
        }
    }
}

// Animation types that should loop rather than play once
private val LOOPING_ANIMATIONS = setOf(
    AnimationType.TOWER_READY_PULSE,
    AnimationType.WATER_FLOW,
    AnimationType.WIZARD_IDLE,
    AnimationType.ALCHEMY_IDLE,
    AnimationType.MINE_DIG
)

// ── Dropdown components ──────────────────────────────────────────────────────

@Composable
private fun AnimationDropdown(
    selected: AnimationType,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (AnimationType) -> Unit
) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable { onExpandedChange(!expanded) },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected.displayName(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (expanded) "▲" else "▼",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            AnimationType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName()) },
                    onClick = { onSelect(type) }
                )
            }
        }
    }
}

@Composable
private fun BackgroundDropdown(
    selected: AnimationBackground,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (AnimationBackground) -> Unit
) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable { onExpandedChange(!expanded) },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Small color swatch
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(selected.color, RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            )
            Text(
                text = selected.label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (expanded) "▲" else "▼",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            ANIMATION_BACKGROUNDS.forEach { bg ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(bg.color, RoundedCornerShape(3.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
                            )
                            Text(bg.label)
                        }
                    },
                    onClick = { onSelect(bg) }
                )
            }
        }
    }
}

/** Human-readable label for each [AnimationType]. */
private fun AnimationType.displayName(): String = when (this) {
    AnimationType.GREEN_WITCH_HEALING         -> "Green Witch Healing"
    AnimationType.BARRICADE_DAMAGE            -> "Barricade Damage"
    AnimationType.FREEZE_SPELL               -> "Freeze Spell"
    AnimationType.DOUBLE_LEVEL_SPELL         -> "Double Level Spell"
    AnimationType.INSTANT_TOWER_SPELL        -> "Instant Tower Spell"
    AnimationType.FEAR_SPELL                 -> "Fear Spell"
    AnimationType.BOMB_EXPLOSION             -> "Bomb Explosion"
    AnimationType.WATER_FLOW                 -> "Water Flow"
    AnimationType.ENEMY_DEATH                -> "Enemy Death"
    AnimationType.TOWER_READY_PULSE          -> "Tower Ready Pulse"
    AnimationType.COIN_GAIN                  -> "Coin Gain"
    AnimationType.TOWER_ATTACK_IMPACT        -> "Tower Attack Impact"
    AnimationType.TOWER_CONSTRUCTION_COMPLETE -> "Tower Construction Complete"
    AnimationType.ENEMY_SPAWN                -> "Enemy Spawn"
    AnimationType.TRAP_TRIGGER               -> "Trap Trigger"
    AnimationType.ENEMY_MOVE                 -> "Enemy Move"
    AnimationType.DRAGON_LEVEL_UP            -> "Dragon Level Up"
    AnimationType.DRAGON_LEVEL_DOWN          -> "Dragon Level Down"
    AnimationType.WIZARD_IDLE                -> "Wizard Idle"
    AnimationType.ALCHEMY_IDLE               -> "Alchemy Idle"
    AnimationType.MINE_DIG                   -> "Mine Dig"
    AnimationType.ARROW_ATTACK               -> "Arrow Attack"
    AnimationType.DRAGON_TARGET              -> "Dragon Target (Mine)"
    AnimationType.BALLISTA_ATTACK            -> "Ballista Attack (Rock)"
    AnimationType.BOW_ATTACK                 -> "Bow Attack (Arrow Volley)"
    AnimationType.SPEAR_ATTACK               -> "Spear Attack (Spear Throw)"
    AnimationType.PIKE_ATTACK                -> "Pike Attack (Pike Extend)"
    AnimationType.WIZARD_ATTACK              -> "Wizard Attack (Fireball)"
    AnimationType.ALCHEMY_ATTACK             -> "Alchemy Attack (Acid Vial)"
}
