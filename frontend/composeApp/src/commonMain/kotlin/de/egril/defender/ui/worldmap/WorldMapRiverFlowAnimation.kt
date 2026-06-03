package de.egril.defender.ui.worldmap

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.math.sqrt

// Default stroke width used when the Lottie layer doesn't declare one; value is in
// Lottie coordinate units (0–2048 x 0–1622). Actual river widths range from 4 to 12.
private const val DEFAULT_LOTTIE_STROKE_WIDTH = 6f

// Animation timing
private const val FLOW_CYCLE_MILLIS = 3500
private const val SHIMMER_CYCLE_MILLIS = 5000

// Water appearance: multiple layered strokes for a richer look
private val WATER_BASE_COLOR = Color(0.35f, 0.65f, 0.88f, 0.35f)
private val WATER_MIDTONE_COLOR = Color(0.45f, 0.75f, 0.95f, 0.50f)
private val WATER_HIGHLIGHT_COLOR = Color(0.70f, 0.90f, 1.0f, 0.75f)
private val WATER_SHIMMER_COLOR = Color(0.85f, 0.95f, 1.0f, 0.60f)

private data class RiverPath(
    val points: List<Offset>,
    val strokeWidth: Float,
)

private fun List<Offset>.pathLength(): Float {
    var len = 0f
    for (i in 1 until size) {
        val dx = this[i].x - this[i - 1].x
        val dy = this[i].y - this[i - 1].y
        len += sqrt(dx * dx + dy * dy)
    }
    return len
}

private fun parseRiverPaths(json: String): List<RiverPath> {
    return try {
        val doc = Json.parseToJsonElement(json).jsonObject
        val layers = doc["layers"]?.jsonArray ?: return emptyList()
        // Each layer is one river path with shape and stroke data.
        layers.mapNotNull { elem ->
            val layer = elem.jsonObject
            val shapes = layer["shapes"]?.jsonArray ?: return@mapNotNull null
            val shape = shapes.firstOrNull()?.jsonObject ?: return@mapNotNull null
            val items = shape["it"]?.jsonArray ?: return@mapNotNull null
            var points: List<Offset>? = null
            var strokeWidth = DEFAULT_LOTTIE_STROKE_WIDTH
            for (item in items) {
                val o = item.jsonObject
                when (o["ty"]?.jsonPrimitive?.content) {
                    "sh" -> {
                        val k = o["ks"]?.jsonObject?.get("k")?.jsonObject ?: continue
                        points = k["v"]?.jsonArray?.map { v ->
                            val arr = v.jsonArray
                            Offset(arr[0].jsonPrimitive.float, arr[1].jsonPrimitive.float)
                        }
                    }
                    "st" -> strokeWidth = o["w"]?.jsonObject?.get("k")?.jsonPrimitive?.float
                        ?: DEFAULT_LOTTIE_STROKE_WIDTH
                }
            }
            val pts = points?.takeIf { it.size >= 2 } ?: return@mapNotNull null
            RiverPath(pts, strokeWidth)
        }
    } catch (e: Exception) {
        println("WorldMapRiverFlowAnimation: failed to parse river paths — ${e.message}")
        emptyList()
    }
}

/**
 * Draws an animated river-flow effect over the world map background.
 *
 * River paths are loaded from [world_map_rivers.json] which uses Lottie coordinates (2048×1622).
 * Paths are scaled to the actual canvas size using the same ContentScale.Fit logic as the
 * background image, so they line up pixel-for-pixel with the rivers on the map.
 *
 * Each river is rendered with multiple layered strokes to simulate flowing water:
 * 1. A wide, semi-transparent base stroke (the river bed/channel)
 * 2. A mid-tone flowing layer with long dashes that scroll continuously
 * 3. Brighter highlight dashes (shorter, faster) giving a sparkle/current effect
 * 4. A subtle shimmer layer with a secondary animation speed
 *
 * The combination of these layers with different dash lengths and animation speeds
 * produces the appearance of continuously flowing water rather than simple colored lines.
 *
 * The animation is only active when [AppSettings.enableAnimations] is true.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun WorldMapRiverFlowAnimation(modifier: Modifier = Modifier) {
    if (!AppSettings.enableAnimations.value) return

    var riverPaths by remember { mutableStateOf<List<RiverPath>>(emptyList()) }
    LaunchedEffect(Unit) {
        launch {
            val json = runCatching {
                Res.readBytes("files/animations/world_map_rivers.json").decodeToString()
            }.getOrElse { return@launch }
            riverPaths = parseRiverPaths(json)
        }
    }
    if (riverPaths.isEmpty()) return

    val infiniteTransition = rememberInfiniteTransition(label = "riverFlow")

    // Primary flow phase – drives the main current
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = FLOW_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "riverFlowPhase",
    )

    // Secondary shimmer phase – slightly slower, creates visual depth
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "riverShimmerPhase",
    )

    Canvas(modifier = modifier) {
        val lottieW = 2048f
        val lottieH = 1622f
        // ContentScale.Fit: uniform scale, centred
        val scale = minOf(size.width / lottieW, size.height / lottieH)
        val dx = (size.width - lottieW * scale) / 2f
        val dy = (size.height - lottieH * scale) / 2f

        for (river in riverPaths) {
            val scaledPts = river.points.map { Offset(it.x * scale + dx, it.y * scale + dy) }

            val path = Path().apply {
                moveTo(scaledPts[0].x, scaledPts[0].y)
                scaledPts.drop(1).forEach { lineTo(it.x, it.y) }
            }

            val totalLen = scaledPts.pathLength()
            if (totalLen < 1f) continue

            val sw = river.strokeWidth * scale

            // --- Layer 1: Wide base channel (always visible, gives body to the river) ---
            drawPath(
                path = path,
                color = WATER_BASE_COLOR,
                style = Stroke(
                    width = sw * 1.6f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            // --- Layer 2: Mid-tone flowing current (long dashes, smooth scroll) ---
            // Dashes cover ~40% of the path with 10% gaps, creating a continuous feel
            val midDash = totalLen * 0.4f
            val midGap = totalLen * 0.1f
            val midPhaseOffset = flowPhase * (midDash + midGap)
            drawPath(
                path = path,
                color = WATER_MIDTONE_COLOR,
                style = Stroke(
                    width = sw * 1.2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(midDash, midGap),
                        phase = midPhaseOffset,
                    ),
                ),
            )

            // --- Layer 3: Highlight streaks (shorter, faster dashes for current) ---
            // Two staggered highlight dashes give a rippling effect
            val highlightDash = totalLen * 0.15f
            val highlightGap = totalLen * 0.18f
            for (streak in 0..1) {
                val p = ((flowPhase * 1.3f + streak / 2f) % 1f) * (highlightDash + highlightGap) * 3f
                drawPath(
                    path = path,
                    color = WATER_HIGHLIGHT_COLOR,
                    style = Stroke(
                        width = sw * 0.7f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(highlightDash, highlightGap),
                            phase = p,
                        ),
                    ),
                )
            }

            // --- Layer 4: Shimmer/sparkle (very short bright flashes at different speed) ---
            val shimmerDash = totalLen * 0.05f
            val shimmerGap = totalLen * 0.45f
            val shimmerOffset = shimmerPhase * (shimmerDash + shimmerGap) * 2f
            drawPath(
                path = path,
                color = WATER_SHIMMER_COLOR,
                style = Stroke(
                    width = sw * 0.4f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(shimmerDash, shimmerGap),
                        phase = shimmerOffset,
                    ),
                ),
            )
        }
    }
}
