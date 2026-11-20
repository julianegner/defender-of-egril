package de.egril.defender.ui.settings

/**
 * Adjusts a sound volume slider based on the selected volume index.
 * 0=master, 1=effects, 2=worldmap music, 3=gameplay music.
 */
internal fun adjustSoundVolume(
    selectedIndex: Int,
    increase: Boolean,
) {
    val step = 0.1f
    when (selectedIndex) {
        0 -> {
            val newVolume =
                if (increase) {
                    (AppSettings.soundVolume.value + step).coerceAtMost(1f)
                } else {
                    (AppSettings.soundVolume.value - step).coerceAtLeast(0f)
                }
            AppSettings.saveSoundVolume(newVolume)
        }
        1 -> {
            val newVolume =
                if (increase) {
                    (AppSettings.effectsVolume.value + step).coerceAtMost(1f)
                } else {
                    (AppSettings.effectsVolume.value - step).coerceAtLeast(0f)
                }
            AppSettings.saveEffectsVolume(newVolume)
            de.egril.defender.audio.GlobalSoundManager
                .getInstance()
                ?.setVolume(newVolume)
        }
        2 -> {
            val newVolume =
                if (increase) {
                    (AppSettings.worldMapMusicVolume.value + step).coerceAtMost(1f)
                } else {
                    (AppSettings.worldMapMusicVolume.value - step).coerceAtLeast(0f)
                }
            AppSettings.saveWorldMapMusicVolume(newVolume)
            val currentMusic =
                de.egril.defender.audio.GlobalBackgroundMusicManager
                    .getCurrentMusic()
            if (currentMusic == de.egril.defender.audio.BackgroundMusic.WORLD_MAP) {
                de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                    de.egril.defender.audio.BackgroundMusic.WORLD_MAP,
                    loop = true,
                )
            }
        }
        3 -> {
            val newVolume =
                if (increase) {
                    (AppSettings.gameplayMusicVolume.value + step).coerceAtMost(1f)
                } else {
                    (AppSettings.gameplayMusicVolume.value - step).coerceAtLeast(0f)
                }
            AppSettings.saveGameplayMusicVolume(newVolume)
            val currentMusic =
                de.egril.defender.audio.GlobalBackgroundMusicManager
                    .getCurrentMusic()
            if (currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL ||
                currentMusic == de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH
            ) {
                de.egril.defender.audio.GlobalBackgroundMusicManager
                    .playMusic(currentMusic, loop = true)
            }
        }
    }
}

/**
 * Adjusts the header text size up or down by one step.
 */
internal fun adjustHeaderTextSize(increase: Boolean) {
    val current = AppSettings.headerTextSize.value
    val next =
        if (increase) {
            when (current) {
                HeaderTextSize.SMALL -> HeaderTextSize.MEDIUM
                HeaderTextSize.MEDIUM -> HeaderTextSize.LARGE
                HeaderTextSize.LARGE -> HeaderTextSize.LARGE
            }
        } else {
            when (current) {
                HeaderTextSize.LARGE -> HeaderTextSize.MEDIUM
                HeaderTextSize.MEDIUM -> HeaderTextSize.SMALL
                HeaderTextSize.SMALL -> HeaderTextSize.SMALL
            }
        }
    AppSettings.saveHeaderTextSize(next)
}

internal fun adjustFontSize(increase: Boolean) {
    val entries = FontSize.entries
    val currentIndex = entries.indexOf(AppSettings.fontSize.value)
    val nextIndex =
        if (increase) {
            (currentIndex + 1).coerceAtMost(entries.lastIndex)
        } else {
            (currentIndex - 1).coerceAtLeast(0)
        }
    AppSettings.saveFontSize(entries[nextIndex])
}

internal fun adjustA11ySlider(
    selectedIndex: Int,
    increase: Boolean,
) {
    when (selectedIndex) {
        0 -> adjustFontSize(increase)
        1 -> adjustHeaderTextSize(increase)
    }
}

internal fun adjustMousePointerSize(increase: Boolean) {
    val entries = MousePointerSize.entries
    val currentIndex = entries.indexOf(AppSettings.mousePointerSize.value)
    val nextIndex =
        if (increase) {
            (currentIndex + 1).coerceAtMost(entries.lastIndex)
        } else {
            (currentIndex - 1).coerceAtLeast(0)
        }
    AppSettings.saveMousePointerSize(entries[nextIndex])
}

internal fun adjustMousePointerSkinBrightness(increase: Boolean) {
    val step = 0.05f
    val next =
        if (increase) {
            AppSettings.mousePointerSkinBrightness.value + step
        } else {
            AppSettings.mousePointerSkinBrightness.value - step
        }
    AppSettings.saveMousePointerSkinBrightness(next)
}

internal fun adjustMousePointerSlider(
    selectedIndex: Int,
    increase: Boolean,
) {
    if (AppSettings.mousePointerSource.value != MousePointerSource.GAME) return
    when (selectedIndex) {
        0 -> adjustMousePointerSize(increase)
        1 -> adjustMousePointerSkinBrightness(increase)
    }
}
