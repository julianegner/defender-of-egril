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
    val OFFICIAL_MAP_IDS =
        setOf(
            "map_at_the_coast",
            "map_back_and_forth_path",
            "map_creek_valley",
            "map_curved_path",
            "map_dance",
            "map_demo",
            "map_ewhads_challenge",
            "map_fast_and_furious",
            "map_flotsam_city",
            "map_goblin_gorge",
            "map_hedges",
            "map_in_the_mountains",
            "map_laguna_islands",
            "map_obstacle_path",
            "map_plains",
            "map_portals",
            "map_spiral",
            "map_straight",
            "map_the_altars",
            "map_the_barricades",
            "map_the_city",
            "map_the_creek",
            "map_the_cross",
            "map_the_dark_path",
            "map_the_double_circle",
            "map_the_fortress",
            "map_the_frame",
            "map_the_gorge",
            "map_the_island",
            "map_the_labyrinth",
            "map_the_lighthouse",
            "map_the_lighthouse_island",
            "map_the_maelstrom",
            "map_the_prison",
            "map_the_river",
            "map_the_river_mouth",
            "map_the_river_mouth_2",
            "map_the_rush",
            "map_the_single_tower",
            "map_the_six_sided_fortress",
            "map_the_spiderweb",
            "map_the_swamp",
            "map_the_town_at_the_river",
            "map_the_volcano",
            "map_the_watch_gate",
            "map_tutorial",
            "map_winding_path",
            "map_woods",
            "the_maskmaster_5298_map",
        )

    /**
     * Set of official level IDs from the repository
     */
    val OFFICIAL_LEVEL_IDS =
        setOf(
            "creek_valley",
            "dark_magic_rises",
            "defend_the_city",
            "demo_demo",
            "ewhads_challenge",
            "flotsam_city",
            "gribnaks_ambush",
            "hold_the_gate",
            "islands_fall_dry",
            "maelstrom",
            "mixed_forces",
            "portals",
            "prison_break",
            "runes_and_altars",
            "sandbox",
            "shadows_over_the_path",
            "the_barricades",
            "the_creek",
            "the_cross",
            "the_dance",
            "the_fallen_shieldmaid",
            "the_fast_and_furious",
            "the_final_stand",
            "the_first_wave",
            "the_fortress_1_first_attacks",
            "the_fortress_2_orks_marching",
            "the_fortress_3_necromancer",
            "the_fortress_4_magic_assault",
            "the_fortress_5_mighty_forces",
            "the_frame",
            "the_giant_spider",
            "the_gorge",
            "the_hedges",
            "the_island",
            "the_labyrinth",
            "the_lighthouse_island",
            "the_dark_protector",
            "the_mountain_rift",
            "the_ork_invasion",
            "the_pirates_are_coming",
            "the_plains",
            "the_river",
            "the_rush",
            "the_spiral_challenge",
            "the_tower_of_the_hermit",
            "the_town_at_the_river",
            "the_volcano",
            "the_winding_path",
            "the_witches",
            "the_witches_swamp",
            "the_woods_first_incursion",
            "the_woods_full_assault",
            "welcome_to_defender_of_egril",
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
