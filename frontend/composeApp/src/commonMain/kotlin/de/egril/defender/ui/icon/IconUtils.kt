package de.egril.defender.ui.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.bomb
import defender_of_egril.composeapp.generated.resources.emoji_bed
import defender_of_egril.composeapp.generated.resources.emoji_coffee
import defender_of_egril.composeapp.generated.resources.emoji_explosion
import defender_of_egril.composeapp.generated.resources.emoji_hole
import defender_of_egril.composeapp.generated.resources.emoji_info
import defender_of_egril.composeapp.generated.resources.emoji_lightning
import defender_of_egril.composeapp.generated.resources.emoji_lock
import defender_of_egril.composeapp.generated.resources.emoji_money
import defender_of_egril.composeapp.generated.resources.emoji_pick
import defender_of_egril.composeapp.generated.resources.emoji_pushpin
import defender_of_egril.composeapp.generated.resources.emoji_reload
import defender_of_egril.composeapp.generated.resources.emoji_save
import defender_of_egril.composeapp.generated.resources.emoji_sword
import defender_of_egril.composeapp.generated.resources.emoji_target
import defender_of_egril.composeapp.generated.resources.emoji_test_tube
import defender_of_egril.composeapp.generated.resources.emoji_timer
import defender_of_egril.composeapp.generated.resources.emoji_tools
import defender_of_egril.composeapp.generated.resources.emoji_unlock
import defender_of_egril.composeapp.generated.resources.trap
import dev.vicart.compose.material.symbols.MaterialSymbol
import dev.vicart.compose.material.symbols.MaterialSymbols
import org.jetbrains.compose.resources.painterResource

/**
 * Displays a lightning bolt emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+26A1)
 */
@Composable
fun LightningIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_lightning),
        contentDescription = "Lightning",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a timer/stopwatch emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+23F1)
 */
@Composable
fun TimerIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 10.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_timer),
        contentDescription = "Timer",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a crossed swords emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+2694)
 */
@Composable
fun SwordIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 14.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_sword),
        contentDescription = "Sword",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a heart icon
 */
@Composable
fun HeartIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.FAVORITE,
        size = size,
        tint = Color.Red,
        modifier = modifier,
    )
}

/**
 * Displays a reload/cycle emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F504)
 */
@Composable
fun ReloadIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_reload),
        contentDescription = "Reload",
        modifier = modifier.size(size),
    )
}

/**
 * Displays an explosion emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F4A5)
 */
@Composable
fun ExplosionIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_explosion),
        contentDescription = "Explosion",
        modifier = modifier.size(size),
    )
}

/**
 * Displays the bomb image icon using Image for cross-platform compatibility
 */
@Composable
fun BombIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.bomb),
        contentDescription = "Bomb",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a test tube emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F9EA)
 */
@Composable
fun TestTubeIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_test_tube),
        contentDescription = "Test Tube",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a hole emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F573)
 */
@Composable
fun HoleIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_hole),
        contentDescription = "Hole",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a trap icon using the trap.png resource
 * Shows a detailed pit trap with grass and flowers
 */
@Composable
fun TrapIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
) {
    Image(
        painter = painterResource(Res.drawable.trap),
        contentDescription = "Trap",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a target emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F3AF)
 */
@Composable
fun TargetIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_target),
        contentDescription = "Target",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a pick/pickaxe emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+26CF)
 */
@Composable
fun PickIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_pick),
        contentDescription = "Pick",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a money/coin emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F4B0)
 */
@Composable
fun MoneyIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_money),
        contentDescription = "Money",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a coffee cup emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+2615)
 */
@Composable
fun CoffeeIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_coffee),
        contentDescription = "Coffee",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a bed emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F6CF)
 */
@Composable
fun BedIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_bed),
        contentDescription = "Bed",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a triangle up icon
 */
@Composable
fun TriangleUpIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.ARROW_DROP_UP,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a triangle right icon
 */
@Composable
fun TriangleRightIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.ARROW_RIGHT,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a triangle left icon
 */
@Composable
fun TriangleLeftIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.ARROW_LEFT,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a triangle down icon
 */
@Composable
fun TriangleDownIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.ARROW_DROP_DOWN,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a trash icon
 */
@Composable
fun TrashIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.DELETE,
        size = size,
        modifier = modifier,
    )
}

/**
 * Displays an info emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+2139)
 */
@Composable
fun InfoIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_info),
        contentDescription = "Info",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a pushpin emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F4CC)
 */
@Composable
fun PushpinIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_pushpin),
        contentDescription = "Pushpin",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a left arrow icon
 */
@Composable
fun LeftArrowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.ARROW_BACK,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays an up arrow icon
 */
@Composable
fun UpArrowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.ARROW_UPWARD,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a down arrow icon
 */
@Composable
fun DownArrowIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.ARROW_DOWNWARD,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a checkmark icon
 */
@Composable
fun CheckmarkIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.CHECK,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a resize icon using Material Symbols for cross-platform compatibility
 */
@Composable
fun ResizeIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.RESIZE,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a tools emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F6E0)
 */
@Composable
fun ToolsIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 24.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_tools),
        contentDescription = "Tools",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a lock emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F512)
 */
@Composable
fun LockIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_lock),
        contentDescription = "Lock",
        modifier = modifier.size(size),
    )
}

/**
 * Displays an unlock emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F513)
 */
@Composable
fun UnlockIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_unlock),
        contentDescription = "Unlock",
        modifier = modifier.size(size),
    )
}

/**
 * Displays a magnifying glass icon
 */
@Composable
fun MagnifyingGlassIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
    tint: Color? = null,
) {
    MaterialSymbol.Filled(
        icon = MaterialSymbols.SEARCH,
        size = size,
        tint = tint ?: LocalContentColor.current,
        modifier = modifier,
    )
}

/**
 * Displays a save emoji icon using Image for cross-platform compatibility
 * Source: Noto Emoji (U+1F4BE)
 */
@Composable
fun SaveIcon(
    modifier: Modifier = Modifier.Companion,
    size: Dp = 16.dp,
) {
    Image(
        painter = painterResource(Res.drawable.emoji_save),
        contentDescription = "Save",
        modifier = modifier.size(size),
    )
}
