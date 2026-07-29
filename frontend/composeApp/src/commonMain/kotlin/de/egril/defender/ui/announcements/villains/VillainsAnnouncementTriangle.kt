package de.egril.defender.ui.announcements.villains

import de.egril.defender.ui.settings.AppSettings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material3.*
import androidx.compose.ui.zIndex
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.banner_villains_light
import defender_of_egril.composeapp.generated.resources.banner_villains_dark

// Defines a right-angled triangle shape matching /_|
val RightTriangleShape = GenericShape { size, _ ->
    moveTo(size.width, 0f)            // Top-right corner (tip)
    lineTo(size.width, size.height)   // Bottom-right corner (right angle)
    lineTo(0f, size.height)           // Bottom-left corner
    close()                           // Draws line back to top-right
}

@Composable
fun VillainsAnnouncementTriangle(
    modifier: Modifier = Modifier,
    zIndex: Float = 999f,
    onClick: () -> Unit = {},
) {
    val isDarkMode = AppSettings.isDarkMode.value

    val painter = if (isDarkMode)
            painterResource(Res.drawable.banner_villains_light)
        else
            painterResource(Res.drawable.banner_villains_dark)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().zIndex(zIndex),
        contentAlignment = Alignment.BottomEnd,
    ) {
        // Responsive size: larger fraction on wide (desktop) screens, smaller on mobile.
        // Wide screens use 67% of the smaller dimension; narrow/mobile screens use 37%.
        val isWideScreen = maxWidth > 800.dp
        val triangleSize = if (isWideScreen) {
            (minOf(maxWidth, maxHeight) * 0.67f).coerceIn(420.dp, 750.dp)
        } else {
            (minOf(maxWidth, maxHeight) * 0.37f).coerceIn(150.dp, 240.dp)
        }
        // Push the image partially off-screen at the bottom so the text sits over the visible part.
        val verticalOffset = triangleSize * 0.26f

        Image(
            painter = painter,
            contentDescription = stringResource(Res.string.villains_announcement_content_description),
            modifier = Modifier
                .size(triangleSize)
                .offset(y = verticalOffset)
                .clip(RightTriangleShape)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(role = Role.Button) { onClick() },
            )
        Box(
            modifier = Modifier
                .background(if (isDarkMode) Color.White else Color.Black)
        ) {
            Text(
                stringResource(Res.string.villains_announcement_coming_soon),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .padding(bottom = 12.dp, end = 12.dp)
            )
        }
    }
}
