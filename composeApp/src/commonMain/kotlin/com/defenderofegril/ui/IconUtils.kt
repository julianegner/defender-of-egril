package com.defenderofegril.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.emoji_explosion
import defender_of_egril.composeapp.generated.resources.emoji_heart
import defender_of_egril.composeapp.generated.resources.emoji_hole
import defender_of_egril.composeapp.generated.resources.emoji_lightning
import defender_of_egril.composeapp.generated.resources.emoji_money
import defender_of_egril.composeapp.generated.resources.emoji_pick
import defender_of_egril.composeapp.generated.resources.emoji_reload
import defender_of_egril.composeapp.generated.resources.emoji_sword
import defender_of_egril.composeapp.generated.resources.emoji_target
import defender_of_egril.composeapp.generated.resources.emoji_test_tube
import defender_of_egril.composeapp.generated.resources.emoji_timer
import org.jetbrains.compose.resources.painterResource

/**
 * Displays a lightning bolt emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+26A1)
 */
@Composable
fun LightningIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_lightning),
        contentDescription = "Lightning",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a timer/stopwatch emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+23F1)
 */
@Composable
fun TimerIcon(
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_timer),
        contentDescription = "Timer",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a crossed swords emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+2694)
 */
@Composable
fun SwordIcon(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_sword),
        contentDescription = "Sword",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a heart emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+2764)
 */
@Composable
fun HeartIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_heart),
        contentDescription = "Heart",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a reload/cycle emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F504)
 */
@Composable
fun ReloadIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_reload),
        contentDescription = "Reload",
        modifier = modifier.size(size)
    )
}

/**
 * Displays an explosion emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F4A5)
 */
@Composable
fun ExplosionIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_explosion),
        contentDescription = "Explosion",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a test tube emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F9EA)
 */
@Composable
fun TestTubeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_test_tube),
        contentDescription = "Test Tube",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a hole emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F573)
 */
@Composable
fun HoleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_hole),
        contentDescription = "Hole",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a target emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F3AF)
 */
@Composable
fun TargetIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_target),
        contentDescription = "Target",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a pick/pickaxe emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+26CF)
 */
@Composable
fun PickIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_pick),
        contentDescription = "Pick",
        modifier = modifier.size(size)
    )
}

/**
 * Displays a money bag emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F4B0)
 */
@Composable
fun MoneyIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    Image(
        painter = painterResource(Res.drawable.emoji_money),
        contentDescription = "Money",
        modifier = modifier.size(size)
    )
}
