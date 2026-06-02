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
        // Layers come in staggered groups of 3 for the snake animation; take one per group
        layers.filterIndexed { index, _ -> index % 3 == 0 }.mapNotNull { elem ->
            val layer = elem.jsonObject
            val shapes = layer["shapes"]?.jsonArray ?: return@mapNotNull null
            val shape = shapes.firstOrNull()?.jsonObject ?: return@mapNotNull null
            val items = shape["it"]?.jsonArray ?: return@mapNotNull null
            var points: List<Offset>? = null
            var strokeWidth = 5f
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
                    "st" -> strokeWidth = o["w"]?.jsonObject?.get("k")?.jsonPrimitive?.float ?: 5f
                }
            }
            val pts = points?.takeIf { it.size >= 2 } ?: return@mapNotNull null
            RiverPath(pts, strokeWidth)
        }
    } catch (_: Exception) {
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
 * Each river is rendered as:
 * - A faint semi-transparent blue stroke showing the river channel
 * - Three flowing "snake" dashes staggered by 1/3 phase each, giving the impression of
 *   water continuously flowing downstream
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
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "riverPhase",
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
            val sw = river.strokeWidth * scale
            // Each "snake" occupies 1/4 of the path length; the remaining 3/4 is gap.
            val dashLen = totalLen / 4f
            val gapLen = totalLen * 3f / 4f

            // Faint base channel so the river is always perceptible
            drawPath(
                path = path,
                color = Color(0.50f, 0.78f, 0.96f, 0.22f),
                style = Stroke(
                    width = sw * 1.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            // Three flowing snakes at 0, 1/3 and 2/3 phase offset
            for (snake in 0..2) {
                val p = ((phase + snake / 3f) % 1f) * totalLen
                drawPath(
                    path = path,
                    color = Color(0.62f, 0.87f, 1.0f, 0.82f),
                    style = Stroke(
                        width = sw,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(dashLen, gapLen),
                            phase = p,
                        ),
                    ),
                )
            }
        }
    }
}
