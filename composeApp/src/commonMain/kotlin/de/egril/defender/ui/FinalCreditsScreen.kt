package de.egril.defender.ui

import androidx.compose.animation.core.Animatable
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

/** Delay in milliseconds before auto-transitioning from the victory screen to credits. */
const val FINAL_CREDITS_TRANSITION_DELAY_MS = 5_000L

// ── Background image animation constants ─────────────────────────────────────
/** How long each image fades in (ms). */
private const val IMAGE_FADE_IN_MS = 1_200
/** How long each image stays fully visible (ms). */
private const val IMAGE_HOLD_MS = 5_000
/** How long each image fades out (ms). */
private const val IMAGE_FADE_OUT_MS = 1_200
/** Total lifetime of one image (ms). */
private const val IMAGE_TOTAL_MS = (IMAGE_FADE_IN_MS + IMAGE_HOLD_MS + IMAGE_FADE_OUT_MS).toLong()
/** How often a new image slot is started (ms). Keeps 1-3 images visible simultaneously. */
private const val IMAGE_SPAWN_INTERVAL_MS = 2_500L
/** Maximum number of images visible at the same time. */
private const val MAX_SIMULTANEOUS_IMAGES = 3

/**
 * Pre-defined display slots: (widthFraction, offsetXFraction, offsetYFraction).
 * widthFraction   – image width as fraction of screen width (0–1).
 * offsetXFraction – left edge as fraction of screen width.
 * offsetYFraction – top edge as fraction of screen height.
 * The sequence is cycled deterministically (no randomness).
 */
private data class ImageSlot(
    val widthFraction: Float,
    val offsetXFraction: Float,
    val offsetYFraction: Float
)

private val IMAGE_SLOTS = listOf(
    ImageSlot(0.45f, 0.05f, 0.08f),   // upper-left, large
    ImageSlot(0.30f, 0.62f, 0.04f),   // upper-right, medium
    ImageSlot(0.38f, 0.28f, 0.52f),   // center, medium
    ImageSlot(0.25f, 0.70f, 0.62f),   // lower-right, small
    ImageSlot(0.42f, 0.03f, 0.55f),   // lower-left, large
    ImageSlot(0.28f, 0.55f, 0.35f),   // center-right, small
    ImageSlot(0.40f, 0.15f, 0.22f),   // center-left, large
    ImageSlot(0.32f, 0.46f, 0.68f),   // lower-center, medium
    ImageSlot(0.35f, 0.60f, 0.15f),   // upper-right-mid, medium
    ImageSlot(0.22f, 0.08f, 0.35f),   // left-mid, small
)

/**
 * Maps a drawable resource name (without extension) to a [DrawableResource].
 * Returns null for unknown names.
 */
private fun drawableResourceByName(name: String): DrawableResource? = when (name) {
    "world_map_background" -> Res.drawable.world_map_background
    "dragon_destroying_mine" -> Res.drawable.dragon_destroying_mine
    "ewhad_message_background" -> Res.drawable.ewhad_message_background
    "story_message_background" -> Res.drawable.story_message_background
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

/** An active background image being animated. */
private class AnimatedBackgroundImage(
    val resource: DrawableResource,
    val slot: ImageSlot,
    val id: Int
) {
    val alpha = Animatable(0f)
}

/**
 * Final credits screen shown after winning "The Final Stand" (the last level).
 *
 * Two-layer display:
 * - Lower layer: game images that fade in one at a time at different sizes and positions,
 *   with 1–3 images visible simultaneously.
 * - Upper layer: scrolling credits text (developers, sound effects, background music).
 *
 * The display is fully deterministic – images and text always appear in the same order.
 * Clicking/tapping anywhere dismisses the credits and returns to the world map.
 */
@Composable
fun FinalCreditsScreen(
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Start auto-scrolling when the screen appears
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
            .clickable { coroutineScope.launch { onDismiss() } }
    ) {
        // ── Lower layer: animated background images ───────────────────────────
        CreditsAnimatedBackground()

        // ── Upper layer: scrolling credits text ───────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

/**
 * Lower layer: images fade in one by one at varying sizes and positions,
 * with 1–[MAX_SIMULTANEOUS_IMAGES] images visible at a time.
 * The sequence is deterministic (no randomness).
 */
@Composable
private fun CreditsAnimatedBackground() {
    val imageResources = remember {
        FinalCreditsData.backgroundImageNames.mapNotNull { drawableResourceByName(it) }
    }
    val activeImages = remember { mutableStateListOf<AnimatedBackgroundImage>() }
    val scope = rememberCoroutineScope()

    // Spawn images in a fixed order on a fixed schedule
    LaunchedEffect(imageResources) {
        var imageIndex = 0
        var slotIndex = 0
        var nextId = 0

        while (true) {
            if (activeImages.size < MAX_SIMULTANEOUS_IMAGES && imageResources.isNotEmpty()) {
                val resource = imageResources[imageIndex % imageResources.size]
                val slot = IMAGE_SLOTS[slotIndex % IMAGE_SLOTS.size]
                val display = AnimatedBackgroundImage(resource, slot, nextId++)

                activeImages.add(display)
                imageIndex++
                slotIndex++

                // Animate this image independently
                scope.launch {
                    display.alpha.animateTo(
                        0.25f,
                        animationSpec = tween(IMAGE_FADE_IN_MS, easing = LinearEasing)
                    )
                    delay(IMAGE_HOLD_MS.toLong())
                    display.alpha.animateTo(
                        0f,
                        animationSpec = tween(IMAGE_FADE_OUT_MS, easing = LinearEasing)
                    )
                    activeImages.remove(display)
                }
            }
            delay(IMAGE_SPAWN_INTERVAL_MS)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        activeImages.forEach { img ->
            val imageWidth = screenWidth * img.slot.widthFraction
            val offsetX = screenWidth * img.slot.offsetXFraction
            val offsetY = screenHeight * img.slot.offsetYFraction

            Image(
                painter = painterResource(img.resource),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .width(imageWidth)
                    .wrapContentHeight()
                    .alpha(img.alpha.value),
                contentScale = ContentScale.FillWidth
            )
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
    HorizontalDivider(
        color = CREDITS_SECTION_COLOR.copy(alpha = 0.4f),
        thickness = 1.dp,
        modifier = Modifier.fillMaxWidth(0.6f)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = title,
        color = CREDITS_SECTION_COLOR,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(
        color = CREDITS_SECTION_COLOR.copy(alpha = 0.4f),
        thickness = 1.dp,
        modifier = Modifier.fillMaxWidth(0.6f)
    )
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

