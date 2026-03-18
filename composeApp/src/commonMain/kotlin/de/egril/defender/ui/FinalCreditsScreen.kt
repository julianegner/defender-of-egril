package de.egril.defender.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val CREDITS_BACKGROUND_COLOR = Color(0xFF1A1A1A)
private val CREDITS_TITLE_COLOR = Color(0xFFFFD700)
private val CREDITS_SECTION_COLOR = Color(0xFFFFA500)
private val CREDITS_TEXT_COLOR = Color(0xFFE0E0E0)
private val CREDITS_SUB_TEXT_COLOR = Color(0xFFAAAAAA)

private const val SCROLL_DURATION_MS = 60_000
private const val IMAGE_BACKGROUND_ALPHA = 0.18f
private const val IMAGES_PER_ROW = 4

/** Delay in milliseconds before auto-transitioning from the victory screen to credits. */
const val FINAL_CREDITS_TRANSITION_DELAY_MS = 5_000L

/**
 * Maps a drawable resource name (without extension) to a [DrawableResource].
 * Returns null for unknown names.
 */
private fun drawableResourceByName(name: String): DrawableResource? = when (name) {
    "world_map_background" -> Res.drawable.world_map_background
    "dragon_destroying_mine" -> Res.drawable.dragon_destroying_mine
    "ewhad_message_background" -> Res.drawable.ewhad_message_background
    "story_message_background" -> Res.drawable.story_message_background
    "example_map_cutout" -> Res.drawable.example_map_cutout
    "location_fortress" -> Res.drawable.location_fortress
    "location_round_tower" -> Res.drawable.location_round_tower
    "location_square_tower" -> Res.drawable.location_square_tower
    "location_forest" -> Res.drawable.location_forest
    "location_village" -> Res.drawable.location_village
    "location_city" -> Res.drawable.location_city
    "location_prison" -> Res.drawable.location_prison
    "location_prison2" -> Res.drawable.location_prison2
    "location_dance" -> Res.drawable.location_dance
    "location_cross" -> Res.drawable.location_cross
    "location_scroll" -> Res.drawable.location_scroll
    "ic_menu_compass" -> Res.drawable.ic_menu_compass
    "gate" -> Res.drawable.gate
    "barricade" -> Res.drawable.barricade
    "trap" -> Res.drawable.trap
    "bomb" -> Res.drawable.bomb
    "black_shield" -> Res.drawable.black_shield
    "black_shield2" -> Res.drawable.black_shield2
    "dig_outcome_gold" -> Res.drawable.dig_outcome_gold
    "dig_outcome_diamond" -> Res.drawable.dig_outcome_diamond
    "dig_outcome_gem_blue" -> Res.drawable.dig_outcome_gem_blue
    "dig_outcome_gem_green" -> Res.drawable.dig_outcome_gem_green
    "dig_outcome_gem_red" -> Res.drawable.dig_outcome_gem_red
    "dig_outcome_brass" -> Res.drawable.dig_outcome_brass
    "dig_outcome_silver" -> Res.drawable.dig_outcome_silver
    "dig_outcome_dragon" -> Res.drawable.dig_outcome_dragon
    "dig_outcome_rubble" -> Res.drawable.dig_outcome_rubble
    else -> null
}

/**
 * Final credits screen shown after winning "The Final Stand" (the last level).
 *
 * Two-layer display:
 * - Lower layer: a mosaic of game images in a fixed order, shown at low opacity.
 * - Upper layer: scrolling credits text (developers, sound effects, background music).
 *
 * The whole display is deterministic – images and text appear in the same order every time.
 * Clicking/tapping anywhere dismisses the credits and returns to the world map.
 */
@Composable
fun FinalCreditsScreen(
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Start scrolling automatically when the composable is first shown
    LaunchedEffect(Unit) {
        delay(500)
        scrollState.animateScrollTo(
            value = scrollState.maxValue,
            animationSpec = tween(durationMillis = SCROLL_DURATION_MS, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CREDITS_BACKGROUND_COLOR)
            .clickable {
                coroutineScope.launch { onDismiss() }
            }
    ) {
        // ── Lower layer: background image mosaic ──────────────────────────────
        CreditsBackgroundImages()

        // ── Upper layer: scrolling credits text ───────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Extra space at the top so text starts below the screen
            Spacer(modifier = Modifier.height(600.dp))

            CreditsTitle()

            Spacer(modifier = Modifier.height(48.dp))

            CreditsDeveloperSection()

            Spacer(modifier = Modifier.height(48.dp))

            CreditsSoundEffectsSection()

            Spacer(modifier = Modifier.height(48.dp))

            CreditsBackgroundMusicSection()

            Spacer(modifier = Modifier.height(64.dp))

            CreditsThankYou()

            // Extra space at the bottom so last line scrolls fully off screen
            Spacer(modifier = Modifier.height(600.dp))
        }

        // Hint at the bottom
        Text(
            text = stringResource(Res.string.credits_click_to_skip),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .alpha(0.5f),
            color = CREDITS_TEXT_COLOR,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun CreditsBackgroundImages() {
    val imageResources = FinalCreditsData.backgroundImageNames
        .mapNotNull { drawableResourceByName(it) }

    val rows = imageResources.chunked(IMAGES_PER_ROW)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .alpha(IMAGE_BACKGROUND_ALPHA),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { rowImages ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowImages.forEach { resource ->
                    Image(
                        painter = painterResource(resource),
                        contentDescription = null,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )
                }
                // Fill remaining cells in the last row if it is not full
                repeat(IMAGES_PER_ROW - rowImages.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CreditsTitle() {
    Text(
        text = stringResource(Res.string.credits_game_title),
        color = CREDITS_TITLE_COLOR,
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(Res.string.credits_title),
        color = CREDITS_SUB_TEXT_COLOR,
        fontSize = 18.sp,
        fontStyle = FontStyle.Italic,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun CreditsSectionHeader(title: String) {
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = CREDITS_SECTION_COLOR.copy(alpha = 0.4f), thickness = 1.dp, modifier = Modifier.fillMaxWidth(0.6f))
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = title,
        color = CREDITS_SECTION_COLOR,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = CREDITS_SECTION_COLOR.copy(alpha = 0.4f), thickness = 1.dp, modifier = Modifier.fillMaxWidth(0.6f))
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun CreditsDeveloperSection() {
    CreditsSectionHeader(title = stringResource(Res.string.credits_section_developers))

    FinalCreditsData.developers.forEach { name ->
        Text(
            text = name,
            color = CREDITS_TEXT_COLOR,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun CreditsSoundEffectsSection() {
    CreditsSectionHeader(title = stringResource(Res.string.credits_section_sound_effects))

    FinalCreditsData.soundEffectsCredits.forEach { entry ->
        Text(
            text = entry.author,
            color = CREDITS_TEXT_COLOR,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text(
            text = entry.description,
            color = CREDITS_SUB_TEXT_COLOR,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun CreditsBackgroundMusicSection() {
    CreditsSectionHeader(title = stringResource(Res.string.credits_section_background_music))

    FinalCreditsData.backgroundMusicCredits.forEach { entry ->
        Text(
            text = entry.author,
            color = CREDITS_TEXT_COLOR,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text(
            text = entry.description,
            color = CREDITS_SUB_TEXT_COLOR,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun CreditsThankYou() {
    Text(
        text = stringResource(Res.string.credits_thank_you),
        color = CREDITS_TITLE_COLOR,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}
