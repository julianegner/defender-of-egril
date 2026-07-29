package de.egril.defender.ui.announcements.villains

import de.egril.defender.ui.settings.AppSettings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material3.*
import androidx.compose.ui.zIndex
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
    triangleSize: Dp = 100.dp,
    zIndex: Float = 999f,
) {
    val isDarkMode = AppSettings.isDarkMode.value

    val painter = if (isDarkMode)
            painterResource(Res.drawable.banner_villains_light)
        else
            painterResource(Res.drawable.banner_villains_dark)

    Box(
        modifier = modifier.fillMaxSize().zIndex(zIndex),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Image(
            painter = painter,
            contentDescription = "Announcement for upcoming Villain feature",
            modifier = Modifier
                .clip(RightTriangleShape)
            )
        Text("Coming soon: Version 1.1: Villains ",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.background,
        )
    }
}
