package de.egril.defender.ui.gameplay

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private val WAAGH_METER_COLOR_STOPS =
    listOf(
        0.00f to Color(0xFF5A6B7C), // Cool stone (slate blue/gray).
        0.35f to Color(0xFF8C8D8A), // Warmed stone gray.
        0.70f to Color(0xFFFF5722), // Glowing orange.
        1.00f to Color(0xFFFFEA00), // Burning yellow ember.
    )

// Accepts values from 0.0f (0%) to 1.0f (100%).
internal fun getWaaghMeterColor(value: Float): Color {
    val clamped = value.coerceIn(0f, 1f)

    for (i in 0 until WAAGH_METER_COLOR_STOPS.size - 1) {
        val (startVal, startColor) = WAAGH_METER_COLOR_STOPS[i]
        val (endVal, endColor) = WAAGH_METER_COLOR_STOPS[i + 1]
        if (clamped in startVal..endVal) {
            val fraction = (clamped - startVal) / (endVal - startVal)
            return lerp(startColor, endColor, fraction)
        }
    }
    return WAAGH_METER_COLOR_STOPS.last().second
}

internal fun formatWaaghMeterPercent(points: Int): String = "${points.coerceIn(0, 100)}%"
