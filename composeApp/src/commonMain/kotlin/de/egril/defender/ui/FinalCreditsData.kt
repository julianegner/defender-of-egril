package de.egril.defender.ui

/**
 * Static data for the final credits screen.
 *
 * This file must be updated when:
 * - New developers commit code to the repository (add to [developers])
 * - New sound files are added with credits in the sounds README files (add to [soundEffectsCredits] or [backgroundMusicCredits])
 *
 * Tests in FinalCreditsDataTest validate that this data stays in sync with:
 * - Git history (for developers)
 * - composeResources/files/sounds/README.md (for sound effects)
 * - composeResources/files/sounds/background/README.md (for background music)
 *
 * Names of drawable images shown in the background are validated against
 * the actual files in composeResources/drawable (excluding emoji_* and tile_*).
 */
object FinalCreditsData {

    /**
     * Human developers who contributed code to the project.
     * Update this list when new developers make commits.
     * Bot accounts (e.g. names ending in "[bot]") are excluded by convention.
     */
    val developers: List<String> = listOf(
        "Julian Egner"
    )

    data class SoundCreditEntry(
        val author: String,
        val description: String
    )

    /**
     * Sound effects credits sourced from composeResources/files/sounds/README.md.
     * Update when new sounds are added.
     */
    val soundEffectsCredits: List<SoundCreditEntry> = listOf(
        SoundCreditEntry("Christopherderp", "swords-clash-w-swing (attack_melee)"),
        SoundCreditEntry("SonoFxAudio", "arrow_loose02 (attack_ranged)"),
        SoundCreditEntry("Quickmusik", "warrior-bass-t (attack_ballista)"),
        SoundCreditEntry("Robinhood76", "giant-fireball-blow (attack_area)"),
        SoundCreditEntry("spookymodem", "acid-bubbling (attack_lasting)"),
        SoundCreditEntry("Ali_6868", "knight-footstep (enemy_spawn, enemy_move)"),
        SoundCreditEntry("PaladinVII", "DeathSFX (enemy_destroyed)"),
        SoundCreditEntry("ryanconway", "pickaxe-mining-stone (mine_dig)"),
        SoundCreditEntry("Paul Sinnett", "coin-clink (mine_coin, tower_sold)"),
        SoundCreditEntry("wavecal22", "wood-misc (mine_trap)"),
        SoundCreditEntry("NearTheAtmoshphere", "beast (mine_dragon)"),
        SoundCreditEntry("TheBuilder15", "trap-switch (trap_trigger)"),
        SoundCreditEntry("joseppujol", "wounded-man-scream (life_lost)"),
        SoundCreditEntry("_stubb", "growl (dragon_eat)"),
        SoundCreditEntry("mokasza", "level-up (tower_upgraded)"),
        SoundCreditEntry("Porphyr", "battle-horn (battle_start)"),
        SoundCreditEntry("LilMati", "ticking-timer (bomb_ticking)"),
        SoundCreditEntry("sangnamsa", "impact (bomb_exploding)")
    )

    /**
     * Background music credits sourced from composeResources/files/sounds/background/README.md.
     * Update when new music files are added.
     */
    val backgroundMusicCredits: List<SoundCreditEntry> = listOf(
        SoundCreditEntry("David Fesliyan", "Fantasy Ambience (fesliyanstudios.com)"),
        SoundCreditEntry("David Fesliyan", "The Dark Castle (fesliyanstudios.com)"),
        SoundCreditEntry("Pixabay", "Mystic Fantasy Orchestral Music (pixabay.com)")
    )

    /**
     * Names of drawable resources (without file extension) to display as background images.
     * Excludes files with "emoji_" or "tile_" prefixes.
     * Update when new drawable images (without those prefixes) are added.
     * The order here determines the fixed display order (no randomness).
     */
    val backgroundImageNames: List<String> = listOf(
        "world_map_background",
        "dragon_destroying_mine",
        "ewhad_message_background",
        "story_message_background",
        "example_map_cutout",
        "location_fortress",
        "location_round_tower",
        "location_square_tower",
        "location_forest",
        "location_village",
        "location_city",
        "location_prison",
        "location_prison2",
        "location_dance",
        "location_cross",
        "location_scroll",
        "ic_menu_compass",
        "gate",
        "barricade",
        "trap",
        "bomb",
        "black_shield",
        "black_shield2",
        "dig_outcome_gold",
        "dig_outcome_diamond",
        "dig_outcome_gem_blue",
        "dig_outcome_gem_green",
        "dig_outcome_gem_red",
        "dig_outcome_brass",
        "dig_outcome_silver",
        "dig_outcome_dragon",
        "dig_outcome_rubble"
    )
}
