package de.egril.defender.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Custom color schemes for the app
 * Dark mode uses softer, less harsh colors for comfortable night viewing
 */
object AppTheme {
    
    /**
     * Light color scheme - standard Material 3 light theme
     */
    val lightColorScheme: ColorScheme = lightColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        
        secondary = Color(0xFF625B71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1D192B),
        
        tertiary = Color(0xFF7D5260),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8E4),
        onTertiaryContainer = Color(0xFF31111D),
        
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        
        background = Color(0xFFFFFBFE),
        onBackground = Color(0xFF1C1B1F),
        
        surface = Color(0xFFFFFBFE),
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0)
    )
    
    /**
     * Dark color scheme - custom softer colors for comfortable night viewing
     * Uses darker backgrounds and muted colors to reduce eye strain
     */
    val darkColorScheme: ColorScheme = darkColorScheme(
        primary = Color(0xFFB8A4D5),        // Softer purple (was 0xFFD0BCFF)
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        
        secondary = Color(0xFFCBC2DB),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        
        tertiary = Color(0xFFEFB8C8),
        onTertiary = Color(0xFF492532),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD8E4),
        
        error = Color(0xFFE57373),          // Softer red (was 0xFFF2B8B5)
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
        
        background = Color(0xFF0F0E11),     // Very dark background for comfort
        onBackground = Color(0xFFE3E2E6),   // Softer white (was 0xFFE6E1E5)
        
        surface = Color(0xFF1A1820),        // Dark but not pure black
        onSurface = Color(0xFFE3E2E6),      // Softer white
        surfaceVariant = Color(0xFF38353E), // Darker variant (was 0xFF49454F)
        onSurfaceVariant = Color(0xFFC4BFD0), // Softer text (was 0xFFCAC4D0)
        
        outline = Color(0xFF827A90),        // Softer outline (was 0xFF938F99)
        outlineVariant = Color(0xFF49454F)
    )

    /**
     * High-contrast light color scheme for improved readability.
     */
    val highContrastLightColorScheme: ColorScheme = lightColorScheme(
        primary = Color(0xFF000000),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFFFFF),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFF000000),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFFFFF),
        onSecondaryContainer = Color(0xFF000000),
        tertiary = Color(0xFF000000),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFFFFF),
        onTertiaryContainer = Color(0xFF000000),
        error = Color(0xFF7A0000),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410000),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF000000),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF000000),
        surfaceVariant = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFF000000),
        outline = Color(0xFF000000),
        outlineVariant = Color(0xFF000000)
    )

    /**
     * High-contrast dark color scheme for improved readability.
     */
    val highContrastDarkColorScheme: ColorScheme = darkColorScheme(
        primary = Color(0xFFFFFFFF),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF000000),
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFFFFFFFF),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF000000),
        onSecondaryContainer = Color(0xFFFFFFFF),
        tertiary = Color(0xFFFFFFFF),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFF000000),
        onTertiaryContainer = Color(0xFFFFFFFF),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF000000),
        errorContainer = Color(0xFF7A0000),
        onErrorContainer = Color(0xFFFFFFFF),
        background = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF000000),
        onSurfaceVariant = Color(0xFFFFFFFF),
        outline = Color(0xFFFFFFFF),
        outlineVariant = Color(0xFFFFFFFF)
    )
}
