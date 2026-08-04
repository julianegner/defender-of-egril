package de.egril.defender.ui.announcements.villains

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hyperether.resources.stringResource
import de.egril.defender.ui.isMobileWebBrowser
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.utils.isPlatformMobile
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.banner_villains_dark
import defender_of_egril.composeapp.generated.resources.banner_villains_light
import org.jetbrains.compose.resources.painterResource

// Defines a right-angled triangle shape matching /_|
val RightTriangleShape =
    GenericShape { size, _ ->
        moveTo(size.width, -40f) // Top-right corner (tip)
        lineTo(size.width, size.height) // Bottom-right corner (right angle)
        lineTo(-40f, size.height) // Bottom-left corner
        close() // Draws line back to top-right
    }

val AnnouncementLabelShape =
    GenericShape { size, _ ->
        // cut the left side diagonally
        // moveTo(size.width * 0.22f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }

@Composable
fun VillainsAnnouncementTriangle(
    modifier: Modifier = Modifier,
    zIndex: Float = 999f,
    onClick: () -> Unit = {},
) {
    val isDarkMode = AppSettings.isDarkMode.value
    val isMobileLike = isPlatformMobile || isMobileWebBrowser()

    val painter =
        if (isDarkMode) {
            painterResource(Res.drawable.banner_villains_light)
        } else {
            painterResource(Res.drawable.banner_villains_dark)
        }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().zIndex(zIndex),
        contentAlignment = Alignment.BottomEnd,
    ) {
        val minSide = minOf(maxWidth, maxHeight)

        val triangleSize =
            if (isMobileLike) {
                (minSide.value * 0.408f).dp.coerceIn(168.dp, 336.dp) // +20%
            } else {
                (minSide.value * 0.744f).dp.coerceIn(384.dp, 984.dp) // +20%
            }

        // Text always derives from triangle size, so it stays smaller.
        val textPadding = (triangleSize.value * 0.06f).dp.coerceIn(8.dp, 32.dp)
        val textMaxWidth = (triangleSize.value * 0.64f).dp
        val labelRightShift = (triangleSize.value * 0.05f).dp + (if (isMobileLike) 10.dp else 0.dp)
        val labelBottomShift = 18.dp
        val textSize = ((triangleSize.value - (textPadding.value * 2f)) * 0.055f).coerceIn(5.5f, 21f).sp
        val verticalOffset = (triangleSize.value * 0.25f).dp

        val windowInsetCompensation = 16.dp // matches MenuScreens BoxWithConstraints padding
        Image(
            painter = painter,
            contentDescription = stringResource(Res.string.villains_announcement_content_description),
            modifier =
                Modifier
                    .size(triangleSize)
                    .offset(x = windowInsetCompensation, y = windowInsetCompensation + verticalOffset)
                    .clip(RightTriangleShape)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(role = Role.Button) { onClick() },
        )

        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = labelRightShift, labelBottomShift)
                    .zIndex(1f)
                    .width(textMaxWidth)
                    .clip(AnnouncementLabelShape)
                    .background(if (isDarkMode) Color.White else Color.Black)
                    .padding(
                        start = 0.dp, // (textPadding.value * 0.7f).dp,
                        end = textPadding,
                        top = (textPadding.value * 0.4f).dp,
                        bottom = textPadding,
                    ).pointerHoverIcon(PointerIcon.Hand)
                    .clickable(role = Role.Button) { onClick() },
        ) {
            Text(
                stringResource(Res.string.villains_announcement_coming_soon),
                fontSize = textSize,
                lineHeight = (textSize.value * 1.1f).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
