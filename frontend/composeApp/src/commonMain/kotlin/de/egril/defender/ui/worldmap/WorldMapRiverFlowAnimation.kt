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

// Rendering thresholds
private const val MINIMUM_VISIBLE_PATH_LENGTH = 1f // paths shorter than 1px are invisible

// Highlight layer animation parameters
private const val HIGHLIGHT_SPEED_MULTIPLIER = 1.3f // faster than base flow

// Water appearance: subtle animated streaks that overlay the map's own blue rivers
// without adding a visible background color
private val WATER_FLOW_COLOR = Color(0.75f, 0.92f, 1.0f, 0.35f)
private val WATER_HIGHLIGHT_COLOR = Color(0.85f, 0.95f, 1.0f, 0.45f)

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
                        points =
                            k["v"]?.jsonArray?.map { v ->
                                val arr = v.jsonArray
                                Offset(arr[0].jsonPrimitive.float, arr[1].jsonPrimitive.float)
                            }
                    }
                    "st" ->
                        strokeWidth = o["w"]
                            ?.jsonObject
                            ?.get("k")
                            ?.jsonPrimitive
                            ?.float
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
 * Each river is rendered as short "snake" segments that travel along the path,
 * modeled after the water_flow.json Trim Paths approach. Two staggered snakes per river
 * create the illusion of a continuously flowing stream without adding any static coloring.
 *
 * The animation is only active when [AppSettings.enableWorldMapAnimations] is true.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun WorldMapRiverFlowAnimation(modifier: Modifier = Modifier) {
    if (!AppSettings.enableWorldMapAnimations.value) return

    var riverPaths by remember { mutableStateOf<List<RiverPath>>(emptyList()) }
    LaunchedEffect(Unit) {
        launch {
            val json =
                runCatching {
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
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = FLOW_CYCLE_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "riverFlowPhase",
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

            val path =
                Path().apply {
                    moveTo(scaledPts[0].x, scaledPts[0].y)
                    scaledPts.drop(1).forEach { lineTo(it.x, it.y) }
                }

            val totalLen = scaledPts.pathLength()
            if (totalLen < MINIMUM_VISIBLE_PATH_LENGTH) continue

            val sw = river.strokeWidth * scale

            // Snake segments: short visible dashes that travel along the path.
            // Each snake is ~15% of the path length with ~85% gap, creating
            // distinct traveling segments rather than a continuous fill.
            val snakeDash = totalLen * 0.15f
            val snakeGap = totalLen * 0.85f

            // --- Snake 1: primary flow segment ---
            val phase1 = flowPhase * (snakeDash + snakeGap)
            drawPath(
                path = path,
                color = WATER_FLOW_COLOR,
                style =
                    Stroke(
                        width = sw * 0.8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect =
                            PathEffect.dashPathEffect(
                                intervals = floatArrayOf(snakeDash, snakeGap),
                                phase = phase1,
                            ),
                    ),
            )

            // --- Snake 2: staggered second segment (offset by half cycle) ---
            val phase2 = ((flowPhase + 0.5f) % 1f) * (snakeDash + snakeGap)
            drawPath(
                path = path,
                color = WATER_FLOW_COLOR,
                style =
                    Stroke(
                        width = sw * 0.8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect =
                            PathEffect.dashPathEffect(
                                intervals = floatArrayOf(snakeDash, snakeGap),
                                phase = phase2,
                            ),
                    ),
            )

            // --- Highlight: a brighter, thinner, faster snake for sparkle ---
            val highlightDash = totalLen * 0.08f
            val highlightGap = totalLen * 0.92f
            val highlightPhase = ((flowPhase * HIGHLIGHT_SPEED_MULTIPLIER) % 1f) * (highlightDash + highlightGap)
            drawPath(
                path = path,
                color = WATER_HIGHLIGHT_COLOR,
                style =
                    Stroke(
                        width = sw * 0.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect =
                            PathEffect.dashPathEffect(
                                intervals = floatArrayOf(highlightDash, highlightGap),
                                phase = highlightPhase,
                            ),
                    ),
            )
        }
    }
}
