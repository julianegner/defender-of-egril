package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Dp
import de.egril.defender.ui.icon.wunjoRunePath

@Composable
internal fun RuneCountIcon(
    size: Dp,
    color: Color,
) {
    Canvas(modifier = Modifier.size(size)) {
        val runeRadius = this.size.minDimension * 0.32f
        drawPath(
            path = wunjoRunePath(this.size.width / 2f, this.size.height / 2f, runeRadius),
            color = color,
            style =
                Stroke(
                    width = this.size.minDimension * 0.09f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
        )
    }
}
