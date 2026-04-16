package de.egril.defender.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.banner_dark
import defender_of_egril.composeapp.generated.resources.banner_light
import org.jetbrains.compose.resources.painterResource

/**
 * Image-based application banner that displays a pre-rendered PNG of the banner.
 * Uses [banner_light] in light mode and [banner_dark] in dark mode.
 *
 * The canvas-based [ApplicationBanner] composable is still used in the Sticker screen
 * and serves as the reference for generating the PNG assets.
 *
 * @param modifier Modifier applied to the [Image].
 */
@Composable
fun ApplicationBannerImage(
    modifier: Modifier = Modifier
) {
    val isDarkMode = AppSettings.isDarkMode.value
    val painter = painterResource(if (isDarkMode) Res.drawable.banner_dark else Res.drawable.banner_light)
    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier.fillMaxWidth(),
        contentScale = ContentScale.Fit
    )
}
