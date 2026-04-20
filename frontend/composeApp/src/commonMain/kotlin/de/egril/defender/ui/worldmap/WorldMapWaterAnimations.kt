package de.egril.defender.ui.worldmap

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.sin

/**
 * A river path defined as a sequence of normalized (0.0-1.0) coordinate points.
 * Points are ordered from source (inland) to mouth (coast/sea).
 */
private data class RiverPath(
    val points: List<Pair<Float, Float>>
)

/**
 * A coastal segment defined as normalized (0.0-1.0) coordinate points along the shore.
 */
private data class CoastalSegment(
    val points: List<Pair<Float, Float>>
)

// ── River path data ────────────────────────────────────────────────────────────
// Traced from the world_map_background.png (2048x1622 pixels).
// Each path flows from inland source toward the sea.

private val RIVER_PATHS = listOf(
    // River 1: Northern river – flows from central-north area westward to NW coast
    RiverPath(
        listOf(
            0.385f to 0.125f,
            0.350f to 0.130f,
            0.310f to 0.130f,
            0.270f to 0.125f,
            0.240f to 0.130f,
            0.210f to 0.145f,
            0.185f to 0.165f,
            0.165f to 0.190f,
            0.150f to 0.215f,
            0.138f to 0.245f,
            0.125f to 0.280f,
            0.115f to 0.310f,
            0.108f to 0.340f
        )
    ),
    // River 2: Upper-central river – flows from mountains westward, passes through lake area
    RiverPath(
        listOf(
            0.525f to 0.230f,
            0.500f to 0.235f,
            0.475f to 0.250f,
            0.450f to 0.265f,
            0.420f to 0.270f,
            0.390f to 0.275f,
            0.360f to 0.280f,
            0.330f to 0.275f,
            0.300f to 0.265f,
            0.270f to 0.255f,
            0.240f to 0.250f,
            0.215f to 0.260f,
            0.195f to 0.275f,
            0.175f to 0.295f,
            0.160f to 0.320f,
            0.148f to 0.340f
        )
    ),
    // River 3: Central river – flows from mid area southwest toward western coast
    RiverPath(
        listOf(
            0.460f to 0.300f,
            0.430f to 0.315f,
            0.400f to 0.325f,
            0.370f to 0.335f,
            0.340f to 0.345f,
            0.310f to 0.360f,
            0.280f to 0.375f,
            0.250f to 0.390f,
            0.220f to 0.400f,
            0.190f to 0.405f,
            0.160f to 0.400f,
            0.135f to 0.390f,
            0.115f to 0.380f
        )
    ),
    // River 4: Eastern mountain river – flows from NE mountains south-southeast toward east coast
    RiverPath(
        listOf(
            0.680f to 0.175f,
            0.700f to 0.200f,
            0.715f to 0.230f,
            0.725f to 0.260f,
            0.740f to 0.285f,
            0.758f to 0.310f,
            0.780f to 0.330f,
            0.800f to 0.345f,
            0.825f to 0.350f
        )
    ),
    // River 5: South-central river – flows from mid-area southward toward south coast
    RiverPath(
        listOf(
            0.420f to 0.525f,
            0.410f to 0.555f,
            0.395f to 0.585f,
            0.380f to 0.610f,
            0.370f to 0.640f,
            0.365f to 0.670f,
            0.370f to 0.700f,
            0.380f to 0.730f,
            0.395f to 0.755f,
            0.415f to 0.780f,
            0.430f to 0.800f
        )
    ),
    // River 6: Southeastern river – flows from central-east area toward SE coast
    RiverPath(
        listOf(
            0.570f to 0.460f,
            0.590f to 0.485f,
            0.610f to 0.510f,
            0.630f to 0.535f,
            0.650f to 0.560f,
            0.670f to 0.585f,
            0.690f to 0.610f,
            0.710f to 0.640f,
            0.730f to 0.665f
        )
    )
)

// ── Coastal/tidal segment data ─────────────────────────────────────────────────
// Points along the shoreline where tidal animation should appear.
// These trace the light-blue shallow water areas visible on the map.

private val COASTAL_SEGMENTS = listOf(
    // NW coast
    CoastalSegment(
        listOf(
            0.155f to 0.060f,
            0.135f to 0.085f,
            0.120f to 0.115f,
            0.108f to 0.150f,
            0.095f to 0.185f,
            0.085f to 0.220f,
            0.078f to 0.260f
        )
    ),
    // West coast
    CoastalSegment(
        listOf(
            0.078f to 0.260f,
            0.070f to 0.300f,
            0.065f to 0.340f,
            0.068f to 0.380f,
            0.072f to 0.420f,
            0.070f to 0.460f,
            0.065f to 0.500f,
            0.062f to 0.540f
        )
    ),
    // SW coast
    CoastalSegment(
        listOf(
            0.062f to 0.540f,
            0.068f to 0.580f,
            0.080f to 0.620f,
            0.095f to 0.660f,
            0.115f to 0.695f,
            0.140f to 0.720f,
            0.170f to 0.740f,
            0.200f to 0.755f,
            0.235f to 0.770f
        )
    ),
    // South coast
    CoastalSegment(
        listOf(
            0.235f to 0.770f,
            0.275f to 0.785f,
            0.320f to 0.810f,
            0.360f to 0.830f,
            0.400f to 0.845f,
            0.440f to 0.850f,
            0.480f to 0.845f,
            0.520f to 0.835f,
            0.560f to 0.820f,
            0.600f to 0.810f
        )
    ),
    // SE coast (near the bay/peninsula)
    CoastalSegment(
        listOf(
            0.600f to 0.810f,
            0.640f to 0.790f,
            0.680f to 0.770f,
            0.720f to 0.745f,
            0.755f to 0.715f,
            0.785f to 0.690f,
            0.810f to 0.665f,
            0.835f to 0.640f
        )
    ),
    // East coast
    CoastalSegment(
        listOf(
            0.835f to 0.640f,
            0.855f to 0.600f,
            0.870f to 0.560f,
            0.880f to 0.520f,
            0.890f to 0.480f,
            0.895f to 0.440f,
            0.900f to 0.400f,
            0.898f to 0.360f,
            0.892f to 0.320f
        )
    ),
    // NE coast
    CoastalSegment(
        listOf(
            0.892f to 0.320f,
            0.885f to 0.280f,
            0.875f to 0.240f,
            0.862f to 0.200f,
            0.845f to 0.165f,
            0.825f to 0.135f,
            0.800f to 0.110f,
            0.770f to 0.090f,
            0.735f to 0.075f
        )
    ),
    // North coast
    CoastalSegment(
        listOf(
            0.735f to 0.075f,
            0.695f to 0.062f,
            0.650f to 0.052f,
            0.600f to 0.048f,
            0.550f to 0.050f,
            0.500f to 0.048f,
            0.450f to 0.042f,
            0.400f to 0.038f,
            0.350f to 0.040f,
            0.300f to 0.045f,
            0.250f to 0.048f,
            0.200f to 0.055f,
            0.155f to 0.060f
        )
    ),
    // Small island (bottom-left)
    CoastalSegment(
        listOf(
            0.055f to 0.800f,
            0.065f to 0.790f,
            0.075f to 0.795f,
            0.080f to 0.810f,
            0.075f to 0.825f,
            0.062f to 0.830f,
            0.050f to 0.820f,
            0.048f to 0.810f,
            0.055f to 0.800f
        )
    )
)

// ── Animation constants ────────────────────────────────────────────────────────

private const val RIVER_PARTICLE_COUNT = 6
private const val RIVER_CYCLE_DURATION_MS = 4000
private const val TIDAL_CYCLE_DURATION_MS = 6000
private val RIVER_PARTICLE_COLOR = Color(0x60ADD8E6) // Semi-transparent light blue
private val TIDAL_COLOR = Color(0x2887CEEB)           // Very transparent sky blue

/**
 * Canvas overlay that draws animated water effects on the world map.
 * Includes river flow particles and tidal shore breathing.
 *
 * Only rendered when both enableAnimations and enableWorldMapAnimations settings are ON.
 */
@Composable
fun BoxScope.WorldMapWaterAnimationsOverlay(
    imageAspectRatio: Float,
    containerWidth: Int,
    containerHeight: Int
) {
    // Calculate actual image bounds within container (matching ImageWorldMapView layout)
    val containerAspectRatio = containerWidth.toFloat() / containerHeight.toFloat().coerceAtLeast(1f)

    val imageWidth: Float
    val imageHeight: Float
    val imageOffsetX: Float
    val imageOffsetY: Float

    if (containerAspectRatio > imageAspectRatio) {
        imageHeight = containerHeight.toFloat()
        imageWidth = imageHeight * imageAspectRatio
        imageOffsetX = (containerWidth - imageWidth) / 2f
        imageOffsetY = 0f
    } else {
        imageWidth = containerWidth.toFloat()
        imageHeight = imageWidth / imageAspectRatio
        imageOffsetX = 0f
        imageOffsetY = (containerHeight - imageHeight) / 2f
    }

    // ── Infinite transition for all animations ─────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "worldmap_water")

    // River flow progress: 0 → 1 over the cycle, repeating
    val riverProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(RIVER_CYCLE_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "river_flow"
    )

    // Tidal breathing: 0 → 1 → 0 (ping-pong)
    val tidalPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(TIDAL_CYCLE_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tidal_phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // ── River flow particles ───────────────────────────────────────
        for (river in RIVER_PATHS) {
            val pts = river.points
            if (pts.size < 2) continue

            // Total path length in screen pixels
            val segmentLengths = mutableListOf<Float>()
            var totalLength = 0f
            for (i in 0 until pts.size - 1) {
                val ax = imageOffsetX + pts[i].first * imageWidth
                val ay = imageOffsetY + pts[i].second * imageHeight
                val bx = imageOffsetX + pts[i + 1].first * imageWidth
                val by = imageOffsetY + pts[i + 1].second * imageHeight
                val dx = bx - ax
                val dy = by - ay
                val len = kotlin.math.sqrt(dx * dx + dy * dy)
                segmentLengths.add(len)
                totalLength += len
            }
            if (totalLength <= 0f) continue

            // Draw several particles spread along the river
            for (p in 0 until RIVER_PARTICLE_COUNT) {
                val particleT = ((riverProgress + p.toFloat() / RIVER_PARTICLE_COUNT) % 1f)
                val targetDist = particleT * totalLength
                var accumulated = 0f
                for (i in 0 until segmentLengths.size) {
                    val segLen = segmentLengths[i]
                    if (accumulated + segLen >= targetDist || i == segmentLengths.size - 1) {
                        val localT = if (segLen > 0f) ((targetDist - accumulated) / segLen).coerceIn(0f, 1f) else 0f
                        val ax = imageOffsetX + pts[i].first * imageWidth
                        val ay = imageOffsetY + pts[i].second * imageHeight
                        val bx = imageOffsetX + pts[i + 1].first * imageWidth
                        val by = imageOffsetY + pts[i + 1].second * imageHeight
                        val cx = ax + (bx - ax) * localT
                        val cy = ay + (by - ay) * localT

                        // Fade particles near the edges of the path
                        val fadeFactor = if (particleT < 0.1f) particleT / 0.1f
                            else if (particleT > 0.9f) (1f - particleT) / 0.1f
                            else 1f

                        drawCircle(
                            color = RIVER_PARTICLE_COLOR.copy(alpha = RIVER_PARTICLE_COLOR.alpha * fadeFactor),
                            radius = 3f,
                            center = Offset(cx, cy)
                        )
                        // Small trailing line for motion effect
                        if (i > 0 || localT > 0.05f) {
                            val trailLen = 8f
                            val prevDist = (targetDist - trailLen).coerceAtLeast(0f)
                            var accum2 = 0f
                            for (j in 0 until segmentLengths.size) {
                                val sLen = segmentLengths[j]
                                if (accum2 + sLen >= prevDist || j == segmentLengths.size - 1) {
                                    val lt = if (sLen > 0f) ((prevDist - accum2) / sLen).coerceIn(0f, 1f) else 0f
                                    val tx = imageOffsetX + pts[j].first * imageWidth
                                    val ty = imageOffsetY + pts[j].second * imageHeight
                                    val ux = imageOffsetX + pts[j + 1].first * imageWidth
                                    val uy = imageOffsetY + pts[j + 1].second * imageHeight
                                    val trailX = tx + (ux - tx) * lt
                                    val trailY = ty + (uy - ty) * lt
                                    drawLine(
                                        color = RIVER_PARTICLE_COLOR.copy(alpha = RIVER_PARTICLE_COLOR.alpha * fadeFactor * 0.5f),
                                        start = Offset(trailX, trailY),
                                        end = Offset(cx, cy),
                                        strokeWidth = 2f,
                                        cap = StrokeCap.Round
                                    )
                                    break
                                }
                                accum2 += sLen
                            }
                        }
                        break
                    }
                    accumulated += segLen
                }
            }
        }

        // ── Tidal shore animation ──────────────────────────────────────
        // Draw breathing circles along the coastline that expand/contract
        val phaseRadians = tidalPhase * kotlin.math.PI.toFloat()
        val tidalRadius = 6f + 5f * sin(phaseRadians)
        val tidalAlpha = 0.12f + 0.08f * sin(phaseRadians)

        for (segment in COASTAL_SEGMENTS) {
            for (pt in segment.points) {
                val sx = imageOffsetX + pt.first * imageWidth
                val sy = imageOffsetY + pt.second * imageHeight
                drawCircle(
                    color = TIDAL_COLOR.copy(alpha = tidalAlpha),
                    radius = tidalRadius,
                    center = Offset(sx, sy)
                )
            }
            // Also draw between points for smoother coverage
            val pts = segment.points
            for (i in 0 until pts.size - 1) {
                val ax = imageOffsetX + pts[i].first * imageWidth
                val ay = imageOffsetY + pts[i].second * imageHeight
                val bx = imageOffsetX + pts[i + 1].first * imageWidth
                val by = imageOffsetY + pts[i + 1].second * imageHeight
                val midX = (ax + bx) / 2f
                val midY = (ay + by) / 2f
                drawCircle(
                    color = TIDAL_COLOR.copy(alpha = tidalAlpha * 0.7f),
                    radius = tidalRadius * 0.8f,
                    center = Offset(midX, midY)
                )
            }
        }
    }
}
