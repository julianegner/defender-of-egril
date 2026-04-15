package de.egril.defender.editor

/**
 * Defines which maps and levels are considered "official" content from the repository.
 * Official content is read-only in the editor - users must copy it to create a user version.
 */
object OfficialContent {

    /**
     * The editor level ID of the final level that triggers the credits screen after winning.
     */
    const val FINAL_LEVEL_ID = "the_final_stand"

    /**
     * Set of official map IDs from the repository
     */
    val OFFICIAL_MAP_IDS = setOf(
        "map_creek_valley",
        "map_dance",
        "map_ewhads_challenge",
        "map_fast_and_furious",
        "map_plains",
        "map_spiral",
        "map_straight",
        "map_the_creek",
        "map_the_cross",
        "map_the_fortress",
        "map_the_island",
        "map_the_maelstrom",
        "map_the_river",
        "map_the_rush",
        "map_tutorial",
        "map_winding_path",
        "map_woods"
    )
    
    /**
     * Set of official level IDs from the repository
     */
    val OFFICIAL_LEVEL_IDS = setOf(
        "creek_valley",
        "dark_magic_rises",
        "ewhads_challenge",
        "maelstrom",
        "mixed_forces",
        "the_creek",
        "the_cross",
        "the_dance",
        "the_fast_and_furious",
        "the_final_stand",
        "the_first_wave",
        "the_fortress_1_first_attacks",
        "the_fortress_2_orks_marching",
        "the_fortress_3_necromancer",
        "the_fortress_4_magic_assault",
        "the_fortress_5_mighty_forces",
        "the_island",
        "the_ork_invasion",
        "the_plains",
        "the_river",
        "the_rush",
        "the_spiral_challenge",
        "the_winding_path",
        "the_witches",
        "the_woods_first_incursion",
        "the_woods_full_assault",
        "welcome_to_defender_of_egril"
    )
    
    /**
     * Check if a map ID is official
     */
    fun isOfficialMap(mapId: String): Boolean = mapId in OFFICIAL_MAP_IDS
    
    /**
     * Check if a level ID is official
     */
    fun isOfficialLevel(levelId: String): Boolean = levelId in OFFICIAL_LEVEL_IDS
}
