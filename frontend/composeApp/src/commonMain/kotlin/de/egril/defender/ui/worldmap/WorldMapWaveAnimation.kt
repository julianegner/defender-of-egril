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

// Lottie timeline: 180 frames at 30fps = 6 seconds total cycle
private const val WAVE_TOTAL_FRAMES = 180f
private const val WAVE_CYCLE_MILLIS = 6000

// Default stroke width for wave marks (matches Lottie source data)
private const val DEFAULT_WAVE_STROKE_WIDTH = 3.5f

// Stroke color matching the Lottie data: dark navy ink
private val WAVE_STROKE_COLOR = Color(0.1f, 0.18f, 0.34f, 1.0f)

/**
 * Data for a single wave mark: a tilde shape at a given position with
 * animated opacity (fade in/hold/fade out) and slight horizontal drift.
 */
private data class WaveMark(
    /** Bezier control points for the tilde shape (in Lottie coords, relative to position) */
    val vertices: List<Offset>,
    val inTangents: List<Offset>,
    val outTangents: List<Offset>,
    /** Start position in Lottie coords */
    val startPos: Offset,
    /** End position (after drift) in Lottie coords */
    val endPos: Offset,
    /** Frame at which this wave becomes visible (opacity starts rising) */
    val visibleStartFrame: Float,
    /** Frame at which this wave reaches full opacity */
    val fadeInEndFrame: Float,
    /** Frame at which opacity starts decreasing */
    val fadeOutStartFrame: Float,
    /** Frame at which this wave becomes invisible */
    val visibleEndFrame: Float,
    /** Maximum opacity (0–1) */
    val maxOpacity: Float,
    /** Stroke width in Lottie units */
    val strokeWidth: Float,
)

private fun parseWaveMarks(json: String): List<WaveMark> {
    return try {
        val doc = Json.parseToJsonElement(json).jsonObject
        val layers = doc["layers"]?.jsonArray ?: return emptyList()
        layers.mapNotNull { elem ->
            val layer = elem.jsonObject
            val ks = layer["ks"]?.jsonObject ?: return@mapNotNull null
            val shapes = layer["shapes"]?.jsonArray ?: return@mapNotNull null
            val shapeGroup = shapes.firstOrNull()?.jsonObject ?: return@mapNotNull null
            val items = shapeGroup["it"]?.jsonArray ?: return@mapNotNull null

            // Parse shape (tilde curve)
            var vertices: List<Offset>? = null
            var inTangents: List<Offset>? = null
            var outTangents: List<Offset>? = null
            var strokeWidth = DEFAULT_WAVE_STROKE_WIDTH

            for (item in items) {
                val o = item.jsonObject
                when (o["ty"]?.jsonPrimitive?.content) {
                    "sh" -> {
                        val k = o["ks"]?.jsonObject?.get("k")?.jsonObject ?: continue
                        vertices = k["v"]?.jsonArray?.map { v ->
                            val arr = v.jsonArray
                            Offset(arr[0].jsonPrimitive.float, arr[1].jsonPrimitive.float)
                        }
                        inTangents = k["i"]?.jsonArray?.map { v ->
                            val arr = v.jsonArray
                            Offset(arr[0].jsonPrimitive.float, arr[1].jsonPrimitive.float)
                        }
                        outTangents = k["o"]?.jsonArray?.map { v ->
                            val arr = v.jsonArray
                            Offset(arr[0].jsonPrimitive.float, arr[1].jsonPrimitive.float)
                        }
                    }
                    "st" -> {
                        strokeWidth = o["w"]?.jsonObject?.get("k")?.jsonPrimitive?.float ?: DEFAULT_WAVE_STROKE_WIDTH
                    }
                }
            }

            val verts = vertices?.takeIf { it.size >= 2 } ?: return@mapNotNull null
            val inTan = inTangents ?: List(verts.size) { Offset.Zero }
            val outTan = outTangents ?: List(verts.size) { Offset.Zero }

            // Parse position keyframes
            val posKeys = ks["p"]?.jsonObject?.get("k")?.jsonArray ?: return@mapNotNull null
            val startPos = posKeys.firstOrNull()?.jsonObject?.get("s")?.jsonArray?.let {
                Offset(it[0].jsonPrimitive.float, it[1].jsonPrimitive.float)
            } ?: return@mapNotNull null

            // Find end position (the position after drift)
            val endPos = posKeys.lastOrNull { kf ->
                val s = kf.jsonObject["s"]?.jsonArray
                s != null && s[0].jsonPrimitive.float != startPos.x
            }?.jsonObject?.get("s")?.jsonArray?.let {
                Offset(it[0].jsonPrimitive.float, it[1].jsonPrimitive.float)
            } ?: startPos

            // Parse opacity keyframes: find fade-in start, peak, fade-out start, end
            val opKeys = ks["o"]?.jsonObject?.get("k")?.jsonArray ?: return@mapNotNull null
            var visibleStart = 0f
            var fadeInEnd = 0f
            var fadeOutStart = 0f
            var visibleEnd = WAVE_TOTAL_FRAMES
            var maxOpacity = 0.85f

            // Typical pattern: [0, 0, peak, peak, 0, 0] with times [t0, t1, t2, t3, t4, t5]
            // t1=visible start, t2=fade in complete, t3=fade out begins, t4=fully invisible
            if (opKeys.size >= 5) {
                visibleStart = opKeys[1].jsonObject["t"]?.jsonPrimitive?.float ?: 0f
                fadeInEnd = opKeys[2].jsonObject["t"]?.jsonPrimitive?.float ?: 0f
                fadeOutStart = opKeys[3].jsonObject["t"]?.jsonPrimitive?.float ?: 0f
                visibleEnd = opKeys[4].jsonObject["t"]?.jsonPrimitive?.float ?: WAVE_TOTAL_FRAMES
                maxOpacity = (opKeys[2].jsonObject["s"]?.jsonArray?.get(0)?.jsonPrimitive?.float
                    ?: 85f) / 100f
            }

            WaveMark(
                vertices = verts,
                inTangents = inTan,
                outTangents = outTan,
                startPos = startPos,
                endPos = endPos,
                visibleStartFrame = visibleStart,
                fadeInEndFrame = fadeInEnd,
                fadeOutStartFrame = fadeOutStart,
                visibleEndFrame = visibleEnd,
                maxOpacity = maxOpacity,
                strokeWidth = strokeWidth,
            )
        }
    } catch (e: Exception) {
        println("WorldMapWaveAnimation: failed to parse wave marks — ${e.message}")
        emptyList()
    }
}

/**
 * Draws animated ocean wave marks (tilde shapes) over the world map.
 *
 * Wave positions and timing are loaded from [world_map_waves.json] which uses Lottie
 * coordinates (2048×1622). Each wave fades in, drifts slightly, and fades out on a
 * staggered 6-second loop, creating the appearance of rolling ocean waves as seen
 * from above.
 *
 * The animation is only active when [AppSettings.enableAnimations] is true.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun WorldMapWaveAnimation(modifier: Modifier = Modifier) {
    if (!AppSettings.enableAnimations.value) return

    var waveMarks by remember { mutableStateOf<List<WaveMark>>(emptyList()) }
    LaunchedEffect(Unit) {
        launch {
            val json = runCatching {
                Res.readBytes("files/animations/world_map_waves.json").decodeToString()
            }.getOrElse { return@launch }
            waveMarks = parseWaveMarks(json)
        }
    }
    if (waveMarks.isEmpty()) return

    val infiniteTransition = rememberInfiniteTransition(label = "waveAnimation")

    // Single phase that drives the entire 6-second cycle (0..1 maps to 0..180 frames)
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WAVE_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )

    Canvas(modifier = modifier) {
        val lottieW = 2048f
        val lottieH = 1622f
        // ContentScale.Fit: uniform scale, centred (same as background image)
        val scale = minOf(size.width / lottieW, size.height / lottieH)
        val dx = (size.width - lottieW * scale) / 2f
        val dy = (size.height - lottieH * scale) / 2f

        val currentFrame = phase * WAVE_TOTAL_FRAMES

        for (wave in waveMarks) {
            // Compute opacity based on current frame and wave's keyframe timing
            val opacity = when {
                currentFrame < wave.visibleStartFrame -> 0f
                currentFrame < wave.fadeInEndFrame -> {
                    val fadeRange = wave.fadeInEndFrame - wave.visibleStartFrame
                    if (fadeRange > 0f) {
                        ((currentFrame - wave.visibleStartFrame) / fadeRange) * wave.maxOpacity
                    } else wave.maxOpacity
                }
                currentFrame < wave.fadeOutStartFrame -> wave.maxOpacity
                currentFrame < wave.visibleEndFrame -> {
                    val fadeRange = wave.visibleEndFrame - wave.fadeOutStartFrame
                    if (fadeRange > 0f) {
                        (1f - (currentFrame - wave.fadeOutStartFrame) / fadeRange) * wave.maxOpacity
                    } else 0f
                }
                else -> 0f
            }

            if (opacity <= 0.01f) continue

            // Interpolate position (drift)
            val posProgress = when {
                currentFrame < wave.visibleStartFrame -> 0f
                currentFrame > wave.visibleEndFrame -> 1f
                else -> {
                    val range = wave.visibleEndFrame - wave.visibleStartFrame
                    if (range > 0f) (currentFrame - wave.visibleStartFrame) / range else 0f
                }
            }
            val posX = wave.startPos.x + (wave.endPos.x - wave.startPos.x) * posProgress
            val posY = wave.startPos.y + (wave.endPos.y - wave.startPos.y) * posProgress

            // Build the bezier path for the tilde shape
            val path = Path().apply {
                val v0 = wave.vertices[0]
                val sx = (posX + v0.x) * scale + dx
                val sy = (posY + v0.y) * scale + dy
                moveTo(sx, sy)

                for (i in 1 until wave.vertices.size) {
                    val prev = wave.vertices[i - 1]
                    val curr = wave.vertices[i]
                    val cp1x = (posX + prev.x + wave.outTangents[i - 1].x) * scale + dx
                    val cp1y = (posY + prev.y + wave.outTangents[i - 1].y) * scale + dy
                    val cp2x = (posX + curr.x + wave.inTangents[i].x) * scale + dx
                    val cp2y = (posY + curr.y + wave.inTangents[i].y) * scale + dy
                    val ex = (posX + curr.x) * scale + dx
                    val ey = (posY + curr.y) * scale + dy
                    cubicTo(cp1x, cp1y, cp2x, cp2y, ex, ey)
                }
            }

            val color = WAVE_STROKE_COLOR.copy(alpha = opacity)
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = wave.strokeWidth * scale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}
