package de.egril.defender.ui.icon

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

private val BoatWood = Color(0xFF8B5A2B)
private val BoatWoodDark = Color(0xFF4A2E17)
private val BoatSail = Color(0xFFF3EBD3)
private val NetFill = Color(0x99E8F4F4)
private val NetMesh = Color(0xFF5E6B6B)

private fun polygon(points: List<Offset>): Path =
    Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }

/**
 * A fishing boat with a net hanging from a boom off its side. Drawn with fractional coordinates so
 * it scales to any tile or button size.
 */
@Composable
fun FisherBoatIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        fun p(
            x: Float,
            y: Float,
        ) = Offset(x * w, y * h)

        val hull = polygon(listOf(p(0.10f, 0.56f), p(0.90f, 0.56f), p(0.75f, 0.80f), p(0.25f, 0.80f)))
        drawPath(hull, color = BoatWood)
        drawPath(hull, color = BoatWoodDark, style = Stroke(width = w * 0.03f))

        drawLine(BoatWoodDark, p(0.42f, 0.56f), p(0.42f, 0.14f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
        val sail = polygon(listOf(p(0.42f, 0.16f), p(0.42f, 0.54f), p(0.20f, 0.54f)))
        drawPath(sail, color = BoatSail)
        drawPath(sail, color = BoatWoodDark, style = Stroke(width = w * 0.015f))

        // Boom reaches out over the water from the right side of the hull.
        val boomStart = p(0.80f, 0.60f)
        val boomEnd = p(0.98f, 0.30f)
        drawLine(BoatWoodDark, boomStart, boomEnd, strokeWidth = w * 0.03f, cap = StrokeCap.Round)

        // Net hangs below the boom: the top edge follows the boom, the bottom edge drops into the water.
        val netBottomEnd = p(0.98f, 0.58f)
        val netBottomStart = p(0.82f, 0.74f)
        val net = polygon(listOf(boomStart, boomEnd, netBottomEnd, netBottomStart))
        drawPath(net, color = NetFill)
        drawPath(net, color = NetMesh, style = Stroke(width = w * 0.012f))
        for (i in 1..3) {
            val t = i / 4f
            val top = Offset(boomStart.x + (boomEnd.x - boomStart.x) * t, boomStart.y + (boomEnd.y - boomStart.y) * t)
            val bottom =
                Offset(
                    netBottomStart.x + (netBottomEnd.x - netBottomStart.x) * t,
                    netBottomStart.y + (netBottomEnd.y - netBottomStart.y) * t,
                )
            drawLine(NetMesh, top, bottom, strokeWidth = w * 0.01f)
        }
    }
}
