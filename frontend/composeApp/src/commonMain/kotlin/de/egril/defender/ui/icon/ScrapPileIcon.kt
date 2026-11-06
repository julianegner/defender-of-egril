package de.egril.defender.ui.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ScrapPileIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val cX = size.toPx() / 2f
        val cY = size.toPx() / 2f
        val r = size.toPx() * 0.18f
        drawCircle(color = Color(0xFF6F7680), radius = r, center = Offset(cX - r * 1.1f, cY + r * 0.8f))
        drawCircle(color = Color(0xFF9A4E2F), radius = r * 0.85f, center = Offset(cX, cY + r * 0.5f))
        drawCircle(color = Color(0xFF7A8088), radius = r * 0.75f, center = Offset(cX + r * 1.1f, cY + r * 0.2f))
        drawCircle(color = Color(0xFF4A4F55), radius = r * 0.65f, center = Offset(cX - r * 0.3f, cY - r * 0.3f))
        drawCircle(color = Color(0xFFAAAFAF), radius = r * 0.18f, center = Offset(cX - r * 1.1f, cY + r * 0.8f))
        drawCircle(color = Color(0xFFAAAFAF), radius = r * 0.14f, center = Offset(cX + r * 1.1f, cY + r * 0.2f))
    }
}
