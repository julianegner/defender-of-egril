package de.egril.defender.ui

/**
 * Static data for the final credits screen.
 *
 * This file must be updated when:
 * - New developers commit code to the repository (add to [developers])
 * - Other contributors should be credited (add to [contributors])
 * - New sound files are added with credits in the sounds README files (add to [soundEffectsCredits] or [backgroundMusicCredits])
 * - New software / tools are used (add to [softwareCredits])
 * - New special-thanks entries are needed (add to [specialThanks])
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
     * Update this list when new developers make commits using a real email address
     * (not a GitHub no-reply address).
     *
     * Excluded automatically by [FinalCreditsDataTest]:
     * - Accounts whose email ends with "@users.noreply.github.com"
     * - The copilot-swe-agent bot
     *
     * Developers who commit with a GitHub no-reply email but want to be credited should
     * be added here manually.
     */
    val developers: List<String> = listOf(
        "Julian Egner"
    )

    data class ContributorEntry(
        val name: String,
        val contribution: String
    )

    /**
     * Other contributors who helped the project in any capacity
     * (testing, bug reports, design feedback, etc.) but are not
     * part of the core development team.
     * Each entry carries the contributor's name and a short description
     * of their contribution, which is displayed on the same line.
     * Add entries here when new contributors should be credited.
     */
    val contributors: List<ContributorEntry> = listOf(
        ContributorEntry("Kathrin Kläs-Dickhof", "Auto-attack function")
    )

    data class SoundCreditEntry(
        val author: String,
        val description: String
    )

    data class SoftwareCreditEntry(
        val name: String,
        val description: String
    )

    data class SpecialThanksEntry(
        val name: String,
        val reason: String
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
        SoundCreditEntry("David Fesliyan", "Beautiful Memories (fesliyanstudios.com)"),
        SoundCreditEntry("Pixabay", "Mystic Fantasy Orchestral Music (pixabay.com)")
    )

    /**
     * Third-party software and tools used to create the game's assets.
     * Update when new tools or services are used.
     * See also: composeResources/drawable/README.MD
     */
    val softwareCredits: List<SoftwareCreditEntry> = listOf(
        SoftwareCreditEntry(
            "Black Forest Labs – FLUX.1-dev",
            "AI image generation (huggingface.co/black-forest-labs/FLUX.1-dev)"
        ),
        SoftwareCreditEntry(
            "Black Forest Labs – FLUX.2-dev",
            "AI image generation (huggingface.co/black-forest-labs/FLUX.2-dev)"
        )
    )

    /**
     * Individuals and projects deserving special recognition.
     * Update when new acknowledgements are warranted.
     */
    val specialThanks: List<SpecialThanksEntry> = listOf(
        SpecialThanksEntry(
            "Amit Patel – Red Blob Games (redblobgames.com)",
            "Invaluable explanations of hex maps, range & pathfinding algorithms,\nand mapgen4 which is the foundation of our world map."
        )
    )

    /**
     * Names of drawable resources (without file extension) to display as animated background images.
     * Excludes files with "emoji_" or "tile_" prefixes, and excludes example_map_cutout.png
     * (which is outdated).
     * Update when new drawable images (without those prefixes) are added.
     * The order here determines the fixed display order (no randomness).
     * Always starts with the world map image.
     */
    val backgroundImageNames: List<String> = listOf(
        "world_map_background",
        "dragon_destroying_mine",
        "ewhad_message_background",
        "story_message_background",
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
        "gate",
        "barricade",
        "trap",
        "bomb",
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

    /**
     * Image names that are intentionally excluded from the background display.
     * Tested by [FinalCreditsDataTest] to ensure all non-emoji, non-tile drawables
     * are either listed in [backgroundImageNames] or in this exclusion list.
     */
    val backgroundImageExclusions: Set<String> = setOf(
        "example_map_cutout",  // outdated, not representative of current game state
        "ic_menu_compass",     // UI icon, not a game scene image
        "black_shield",        // UI icon, not a game scene image
        "black_shield2"        // UI icon, not a game scene image
    )
}
